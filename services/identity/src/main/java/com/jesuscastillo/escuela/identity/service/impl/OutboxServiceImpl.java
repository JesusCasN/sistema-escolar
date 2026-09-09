package com.jesuscastillo.escuela.identity.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesuscastillo.escuela.identity.entity.OutboxEvent;
import com.jesuscastillo.escuela.identity.repository.OutboxRepository;
import com.jesuscastillo.escuela.identity.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementación del registro de eventos en el outbox.
 * <p>
 * Flujo:
 * 1. Arma el sobre común del evento (eventId, occurredAt, type, version, payload),
 *    igual que los esquemas de contracts/events.
 * 2. Lo serializa a JSON.
 * 3. Lo guarda en la tabla outbox_event con {@code Propagation.MANDATORY}: este
 *    método SIEMPRE debe correr dentro de la transacción de negocio que lo invoca.
 *    Si alguien lo llama fuera de una transacción, falla de inmediato en vez de
 *    romper la garantía de atomicidad de forma silenciosa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipo, int version, UUID aggregateId, Object payload) {

        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventId", UUID.randomUUID());
        sobre.put("occurredAt", Instant.now());
        sobre.put("type", tipo);
        sobre.put("version", version);
        sobre.put("payload", payload);

        String json;
        try {
            json = objectMapper.writeValueAsString(sobre);
        } catch (JsonProcessingException e) {
            // Un evento que no se puede serializar es un error de programacion:
            // se deja tronar la transaccion de negocio en vez de perder el evento.
            throw new IllegalStateException("No se pudo serializar el evento " + tipo, e);
        }

        outboxRepository.save(OutboxEvent.de(tipo, version, aggregateId, json));
        log.debug("Evento {} v{} registrado en outbox para agregado={}", tipo, version, aggregateId);
    }
}
