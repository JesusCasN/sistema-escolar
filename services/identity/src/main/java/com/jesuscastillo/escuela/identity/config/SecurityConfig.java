package com.jesuscastillo.escuela.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad del servicio.
 * <p>
 * PROVISIONAL: la cadena de filtros de esta entrega protege los endpoints de
 * administración con autenticación básica, únicamente para poder probar el flujo de
 * alta en local. En la siguiente entrega se sustituye por el Authorization Server
 * (OAuth2/OIDC) y la validación de JWT con los claims {@code role} y {@code schoolId},
 * tal como exige la SPEC-001.
 * <p>
 * Los endpoints de invitación son públicos por diseño: quien llega con la liga del
 * correo todavía no tiene cuenta activa.
 */
@Configuration
public class SecurityConfig {

    /**
     * Argon2id para el hash de contraseñas (SPEC-001). Se usan los parámetros
     * recomendados por Spring Security, que ya contemplan salt y coste de memoria.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // API sin sesión: no hay formulario ni cookie que proteger con CSRF.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/invitaciones/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
