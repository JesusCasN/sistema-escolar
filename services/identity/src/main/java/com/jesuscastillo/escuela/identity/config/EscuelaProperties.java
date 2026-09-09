package com.jesuscastillo.escuela.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.UUID;

/**
 * Configuración de negocio del servicio de identidad (prefijo {@code escuela.identidad}).
 * <p>
 * Se enlaza desde {@code application.yml} y, a partir de la entrega del Config Server,
 * desde el repositorio de configuración centralizada (ADR-003).
 *
 * @param schoolId  identificador de la escuela; en el MVP hay una sola (SPEC-001)
 * @param invitacion parámetros de las invitaciones de un solo uso
 */
@ConfigurationProperties(prefix = "escuela.identidad")
public record EscuelaProperties(UUID schoolId, Invitacion invitacion) {

    /**
     * @param vigencia    cuánto dura una invitación antes de expirar
     * @param urlBase     base para armar la liga que se envía al invitado
     */
    public record Invitacion(Duration vigencia, String urlBase) {
    }
}
