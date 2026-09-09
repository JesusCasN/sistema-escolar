package com.jesuscastillo.escuela.identity.dto;

import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Rol;

import java.time.Instant;
import java.util.UUID;

/**
 * Usuario tal como lo expone la API — {@code UsuarioResponse} del contrato.
 *
 * @param invitacionExpiraEn solo tiene valor mientras hay una invitación vigente
 */
public record UsuarioResponse(
        UUID id,
        String nombre,
        String correo,
        Rol rol,
        EstadoUsuario estado,
        Instant invitacionExpiraEn
) {
}
