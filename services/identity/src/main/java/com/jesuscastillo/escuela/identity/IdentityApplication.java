package com.jesuscastillo.escuela.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Servicio de identidad: alta de usuarios por invitación, activación con passkey o
 * contraseña, y emisión de tokens OAuth2/OIDC.
 * <p>
 * {@code @EnableScheduling} habilita el publicador del outbox hacia Kafka.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
