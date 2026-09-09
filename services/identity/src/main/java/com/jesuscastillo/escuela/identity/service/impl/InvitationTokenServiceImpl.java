package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.service.InvitationTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Implementación del servicio de tokens de invitación.
 * <p>
 * Flujo:
 * 1. {@link #nuevoToken()} genera 32 bytes con {@link SecureRandom} y los codifica
 *    en Base64 URL-safe, para que el token pueda viajar en una liga sin escapar.
 * 2. {@link #hash(String)} aplica SHA-256 y devuelve el resultado en hexadecimal.
 * 3. El servicio de invitaciones persiste ÚNICAMENTE el hash; el token en claro
 *    se entrega al usuario y no se puede recuperar después.
 */
@Service
@Slf4j
public class InvitationTokenServiceImpl implements InvitationTokenService {

    /** 32 bytes = 256 bits de entropía. */
    private static final int TOKEN_BYTES = 32;

    private static final String ALGORITMO_HASH = "SHA-256";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nuevoToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String token) {

        if (token == null || token.isBlank()) {
            log.warn("Invitacion: se intento hashear un token nulo o vacio");
            throw new IllegalArgumentException("El token no puede ser nulo ni vacio");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO_HASH);
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ALGORITMO_HASH + " no disponible en esta JVM", e);
        }
    }
}
