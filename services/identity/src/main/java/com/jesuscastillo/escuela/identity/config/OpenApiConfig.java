package com.jesuscastillo.escuela.identity.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos del contrato que springdoc publica en runtime.
 * <p>
 * Lo que sale de aquí es el REFLEJO del código. La fuente de verdad es
 * {@code contracts/openapi/identity.yaml}, escrito a mano antes de implementar
 * (ver CONTRIBUTING.md). Si los dos difieren, el que está mal es el código.
 * <p>
 * El esquema de seguridad declarado es PROVISIONAL: hoy es autenticación básica contra
 * un usuario en memoria, solo para poder probar en local. Cuando entre el Authorization
 * Server se sustituye por OAuth2 con Authorization Code + PKCE, y Swagger UI podrá
 * obtener el token por sí solo.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Identity API",
                version = "v1",
                description = """
                        Alta de usuarios por invitacion de un solo uso, activacion con \
                        contrasena o passkey, y emision de tokens OAuth2/OIDC para la \
                        plataforma de comunicacion escuela-familia."""),
        servers = @Server(url = "http://localhost:8081", description = "Local"))
@SecurityScheme(
        name = OpenApiConfig.ESQUEMA_BASICO,
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "PROVISIONAL. Se reemplaza por OAuth2/OIDC en la siguiente entrega.")
public class OpenApiConfig {

    /** Nombre del esquema de seguridad, referenciado por los controllers protegidos. */
    public static final String ESQUEMA_BASICO = "basicAuth";
}
