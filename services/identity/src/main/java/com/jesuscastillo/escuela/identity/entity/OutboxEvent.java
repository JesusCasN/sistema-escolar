package com.jesuscastillo.escuela.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Patrón transactional outbox: el evento se guarda en la MISMA transacción que el
 * cambio de estado del agregado, y un publicador lo envía a Kafka después.
 * Así la activación de un usuario nunca se pierde ni se revierte por un Kafka caído.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private UUID id;

    /** Tipo lógico del evento, p. ej. "usuario.activado". */
    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false)
    private int version;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    /** Sobre completo del evento serializado como JSON (ver contracts/events/). */
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public static OutboxEvent de(String type, int version, UUID aggregateId, String payload) {
        OutboxEvent e = new OutboxEvent();
        e.id = UUID.randomUUID();
        e.type = type;
        e.version = version;
        e.aggregateId = aggregateId;
        e.payload = payload;
        e.createdAt = Instant.now();
        return e;
    }

    public void marcarPublicado() {
        this.publishedAt = Instant.now();
    }
}
