package com.jesuscastillo.escuela.identity.dto;

import com.jesuscastillo.escuela.identity.entity.Rol;

import java.time.Instant;

/**
 * Datos mínimos de una invitación vigente — {@code InvitacionResponse} del contrato.
 * <p>
 * Este endpoint es público (lo consulta quien trae la liga del correo), así que
 * expone únicamente lo necesario para pintar la pantalla de activación: nunca
 * el correo ni el identificador del usuario.
 */
public record InvitacionResponse(String nombre, Rol rol, Instant expiraEn) {
}
