package com.jesuscastillo.escuela.identity.service;

import java.util.UUID;

/**
 * Registro de eventos de dominio mediante el patrón transactional outbox (SPEC-001).
 * <p>
 * Funcionalidad:
 * Guardar el evento en la misma transacción que el cambio de estado que lo origina.
 * Un publicador aparte lo envía a Kafka después. Así, si Kafka está caído, la
 * operación de negocio NO se revierte y el evento no se pierde.
 */
public interface OutboxService {

    /**
     * Registra un evento pendiente de publicación.
     *
     * @param tipo        tipo lógico del evento, p. ej. {@code usuario.activado}
     * @param version     versión del esquema del evento (ver contracts/events)
     * @param aggregateId identificador de la entidad que originó el evento
     * @param payload     cuerpo del evento; se serializa a JSON
     */
    void registrar(String tipo, int version, UUID aggregateId, Object payload);
}
