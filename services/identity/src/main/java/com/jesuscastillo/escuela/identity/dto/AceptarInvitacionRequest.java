package com.jesuscastillo.escuela.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Activación de una cuenta — {@code AceptarInvitacionRequest} del contrato.
 *
 * @param metodo   PASSKEY (el ceremonial WebAuthn se completa después) o PASSWORD
 * @param password obligatoria solo cuando el método es PASSWORD; mínimo 12 caracteres
 */
public record AceptarInvitacionRequest(

        @NotNull(message = "el metodo de activacion es obligatorio")
        MetodoActivacion metodo,

        @Size(min = 12, max = 128, message = "la contrasena debe tener al menos 12 caracteres")
        String password
) {

    public enum MetodoActivacion {
        PASSKEY,
        PASSWORD
    }

    /** El contrato exige contraseña únicamente cuando el método es PASSWORD. */
    public boolean requierePassword() {
        return metodo == MetodoActivacion.PASSWORD;
    }
}
