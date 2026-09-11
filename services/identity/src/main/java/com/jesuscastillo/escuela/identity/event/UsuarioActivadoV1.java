package com.jesuscastillo.escuela.identity.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.jesuscastillo.escuela.identity.entity.TipoVinculo;
import com.jesuscastillo.escuela.identity.entity.Usuario;

import java.util.List;
import java.util.UUID;

/**
 * Evento {@code usuario.activado.v1}: el usuario aceptó su invitación y su cuenta pasó
 * a ACTIVO.
 * <p>
 * Consumidores previstos: messaging, que arma los directorios de conversación, y
 * notifications. Corresponde 1:1 con
 * {@code contracts/events/usuario.activado.v1.schema.json}.
 * <p>
 * Los métodos de {@link EventoDeDominio} se excluyen del JSON porque pertenecen al sobre
 * del evento, no al payload, y el esquema declara {@code additionalProperties: false}.
 */
@JsonIgnoreProperties({"tipo", "version", "aggregateId"})
public record UsuarioActivadoV1(
        UUID userId,
        UUID schoolId,
        Rol rol,
        String nombre,
        List<VinculoRef> vinculos
) implements EventoDeDominio {

    /** Hijos (TUTOR) o grupos (MAESTRO) al momento de la activación. */
    public record VinculoRef(TipoVinculo tipo, UUID id) {
    }

    public static UsuarioActivadoV1 de(Usuario usuario) {
        return new UsuarioActivadoV1(
                usuario.getId(),
                usuario.getSchoolId(),
                usuario.getRol(),
                usuario.getNombre(),
                usuario.getVinculos().stream()
                        .map(v -> new VinculoRef(v.getTipo(), v.getRefId()))
                        .toList());
    }

    @Override
    public String tipo() {
        return "usuario.activado";
    }

    @Override
    public int version() {
        return 1;
    }

    @Override
    public UUID aggregateId() {
        return userId;
    }
}
