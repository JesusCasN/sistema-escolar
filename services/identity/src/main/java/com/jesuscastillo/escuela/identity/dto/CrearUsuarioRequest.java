package com.jesuscastillo.escuela.identity.dto;

import com.jesuscastillo.escuela.identity.entity.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Alta de usuario por parte de dirección — {@code CrearUsuarioRequest} del contrato.
 * <p>
 * No lleva credenciales: el sistema genera una invitación de un solo uso y es el
 * propio usuario quien define su credencial al aceptarla (SPEC-001).
 *
 * @param nombre   nombre completo, máximo 120 caracteres
 * @param correo   correo institucional o personal al que se envía la invitación
 * @param rol      rol con el que se da de alta
 * @param hijosIds alumnos vinculados; solo aplica cuando el rol es TUTOR
 */
public record CrearUsuarioRequest(

        @NotBlank(message = "el nombre es obligatorio")
        @Size(max = 120, message = "el nombre no puede exceder 120 caracteres")
        String nombre,

        @NotBlank(message = "el correo es obligatorio")
        @Email(message = "el correo no tiene un formato valido")
        @Size(max = 254, message = "el correo no puede exceder 254 caracteres")
        String correo,

        @NotNull(message = "el rol es obligatorio")
        Rol rol,

        List<UUID> hijosIds
) {
}
