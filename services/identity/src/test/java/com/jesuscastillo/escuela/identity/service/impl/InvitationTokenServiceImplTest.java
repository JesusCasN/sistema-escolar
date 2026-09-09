package com.jesuscastillo.escuela.identity.service.impl;

import com.jesuscastillo.escuela.identity.service.InvitationTokenService;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvitationTokenServiceImplTest {

    private final InvitationTokenService service = new InvitationTokenServiceImpl();

    @Test
    void nuevoToken_generaTokensUnicosYUrlSafe() {
        Set<String> tokens = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            String token = service.nuevoToken();
            assertThat(token).matches("[A-Za-z0-9_-]{43}");
            tokens.add(token);
        }
        assertThat(tokens).hasSize(1_000);
    }

    @Test
    void hash_mismoToken_produceMismoHashHexDe64() {
        String token = service.nuevoToken();
        assertThat(service.hash(token))
                .isEqualTo(service.hash(token))
                .matches("[0-9a-f]{64}");
    }

    @Test
    void hash_tokensDistintos_producenHashesDistintos() {
        assertThat(service.hash("token-a")).isNotEqualTo(service.hash("token-b"));
    }

    @Test
    void hash_tokenVacio_lanzaExcepcion() {
        assertThatThrownBy(() -> service.hash("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
