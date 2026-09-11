package com.jesuscastillo.escuela.identity.service;

import com.jesuscastillo.escuela.identity.event.EventoDeDominio;

/**
 * Registro de eventos de dominio mediante el patrón transactional outbox (SPEC-001).
 * <p>
 * Funcionalidad:
 * Guardar el evento en la misma transacción que el cambio de estado que lo origina.
 * Un publicador aparte lo envía a Kafka después. Así, si Kafka está caído, la operación
 * de negocio NO se revierte y el evento no se pierde.
 */
public interface OutboxService {

    /**
     * Registra un evento pendiente de publicación.
     * <p>
     * El evento aporta su propio tipo, versión y agregado, de modo que quien lo registra
     * no puede equivocarse al pasarlos sueltos.
     *
     * @param evento evento de dominio; su contenido se serializa como payload del sobre
     */
    void registrar(EventoDeDominio evento);
}
