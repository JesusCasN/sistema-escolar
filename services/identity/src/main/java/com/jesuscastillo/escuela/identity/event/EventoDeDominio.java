package com.jesuscastillo.escuela.identity.event;

import java.util.UUID;

/**
 * Contrato común de los eventos de dominio que publica este servicio.
 * <p>
 * Cada evento sabe cómo se llama y en qué versión está, en lugar de que quien lo registra
 * tenga que acordarse de pasar esos datos sueltos. Así el tipo y la versión viajan pegados
 * al payload y es imposible desalinearlos.
 * <p>
 * Toda implementación corresponde 1:1 con su esquema en {@code contracts/events}: si uno
 * cambia, el otro cambia en el mismo commit.
 */
public interface EventoDeDominio {

    /** Tipo lógico del evento, p. ej. {@code usuario.activado}. Da nombre al tópico. */
    String tipo();

    /** Versión del esquema declarado en {@code contracts/events}. */
    int version();

    /** Entidad que originó el evento. Se usa como clave del mensaje en Kafka. */
    UUID aggregateId();
}
