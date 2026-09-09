package com.jesuscastillo.escuela.identity.service;

/**
 * Servicio de tokens de invitación (SPEC-001).
 * <p>
 * Funcionalidad:
 * Generar el token de un solo uso que se envía al usuario invitado y calcular
 * el hash con el que ese token se persiste. El token en claro nunca se guarda
 * en base de datos: solo viaja en el correo de invitación.
 */
public interface InvitationTokenService {

    /**
     * Genera un token de invitación criptográficamente aleatorio (256 bits).
     *
     * @return token en claro, codificado en Base64 URL-safe (43 caracteres)
     */
    String nuevoToken();

    /**
     * Calcula el hash SHA-256 con el que se persiste un token de invitación.
     *
     * @param token token en claro
     * @return hash en hexadecimal (64 caracteres)
     */
    String hash(String token);
}
