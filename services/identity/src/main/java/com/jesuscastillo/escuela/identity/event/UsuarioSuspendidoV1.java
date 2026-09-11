package com.jesuscastillo.escuela.identity.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.jesuscastillo.escuela.identity.entity.Usuario;

import java.util.UUID;

/**
 * Evento {@code usuario.suspendido.v1}: dirección dio de baja lógica una cuenta.
 * <p>
 * Consumidores previstos: messaging, que cierra las conversaciones activas del usuario,
 * y notifications. Corresponde 1:1 con
 * {@code contracts/events/usuario.suspendido.v1.schema.json}.
 * <p>
 * {@code motivo} es opcional en el esquema, por eso se omite del JSON cuando viene nulo:
 * un {@code "motivo": null} no pasaría la validación de tipo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"tipo", "version", "aggregateId"})
public record UsuarioSuspendidoV1(
        UUID userId,
        UUID schoolId,
        String motivo
) implements EventoDeDominio {

    /** El motivo es opcional en el esquema y hoy no se captura en ninguna pantalla. */
    public static UsuarioSuspendidoV1 de(Usuario usuario) {
        return new UsuarioSuspendidoV1(usuario.getId(), usuario.getSchoolId(), null);
    }

    @Override
    @JsonIgnore
    public String tipo() {
        return "usuario.suspendido";
    }

    @Override
    @JsonIgnore
    public int version() {
        return 1;
    }

    @Override
    @JsonIgnore
    public UUID aggregateId() {
        return userId;
    }
}
