package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.entity.OutboxEvent;
import com.jesuscastillo.escuela.identity.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publicador del outbox hacia Kafka.
 * <p>
 * Flujo:
 * 1. Cada cierto intervalo lee los eventos que siguen sin publicar, en orden de creación.
 * 2. Publica cada uno usando el identificador del agregado como clave del mensaje, para
 *    que todos los eventos de un mismo usuario caigan en la misma partición y conserven
 *    su orden.
 * 3. Marca como publicado solo lo que Kafka confirmó. Si un envío falla, el evento se
 *    queda pendiente y se reintenta en la siguiente pasada.
 * <p>
 * La entrega es "al menos una vez": un corte justo entre la confirmación de Kafka y el
 * commit de la transacción puede reenviar un evento. Por eso los consumidores deben ser
 * idempotentes usando el {@code eventId} del sobre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    /** Prefijo de los tópicos: usuario.activado -> escuela.identity.usuario.activado */
    private static final String PREFIJO_TOPICO = "escuela.identity.";

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${escuela.identidad.outbox.intervalo-ms:5000}")
    @Transactional
    public void publicarPendientes() {

        List<OutboxEvent> pendientes = outboxRepository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
        if (pendientes.isEmpty()) {
            return;
        }

        log.debug("Outbox: {} eventos pendientes de publicar", pendientes.size());
        int publicados = 0;

        for (OutboxEvent evento : pendientes) {
            try {
                kafkaTemplate.send(
                        PREFIJO_TOPICO + evento.getType(),
                        evento.getAggregateId().toString(),
                        evento.getPayload()
                ).get();

                evento.marcarPublicado();
                publicados++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Outbox: publicacion interrumpida, se reintentara en la siguiente pasada");
                break;
            } catch (Exception e) {
                // Se corta la pasada para no romper el orden de los eventos del agregado.
                log.error("Outbox: fallo al publicar evento id={} tipo={}; se reintentara",
                        evento.getId(), evento.getType(), e);
                break;
            }
        }

        if (publicados > 0) {
            log.info("Outbox: {} eventos publicados", publicados);
        }
    }
}
