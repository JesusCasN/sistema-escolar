package com.jesuscastillo.escuela.identity.integracion;

import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest;
import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest.MetodoActivacion;
import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.InvitacionResponse;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.EstadoUsuario;
import com.jesuscastillo.escuela.identity.entity.Rol;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flujo de alta por invitación y activación, de punta a punta: HTTP real contra Postgres
 * real, con el esquema que puso Flyway.
 * <p>
 * Lo que estas pruebas cubren y las unitarias no pueden: que la migración V1 y las
 * entidades JPA concuerden (con {@code ddl-auto: validate} el contexto ni siquiera
 * arranca si no), que la cadena de filtros deje pasar lo público y detenga lo demás, y
 * que el hash que queda guardado en la columna sea Argon2id de verdad.
 */
@DisplayName("Alta por invitacion y activacion")
class AltaYActivacionIT extends SoporteIntegracion {

    private static final String CORREO = "ana.lopez@example.com";
    private static final String NOMBRE = "Ana Lopez Mendez";
    private static final String CLAVE_NUEVA = "clave-de-la-tutora-2026";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void alta_correoNuevo_creaUsuarioPendienteYEmiteInvitacion() {

        ResponseEntity<UsuarioResponse> respuesta = alta(tutora());

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UsuarioResponse cuerpo = respuesta.getBody();
        assertThat(cuerpo).isNotNull();
        assertThat(cuerpo.correo()).isEqualTo(CORREO);
        assertThat(cuerpo.estado()).isEqualTo(EstadoUsuario.PENDIENTE);
        assertThat(cuerpo.invitacionExpiraEn())
                .isBetween(Instant.now().plus(Duration.ofHours(71)),
                        Instant.now().plus(Duration.ofHours(73)));

        assertThat(estadoEnBase(CORREO)).isEqualTo("PENDIENTE");
        assertThat(passwordHashEnBase(CORREO)).isNull();

        // De la invitacion solo se persiste el hash del token; el token en claro existe
        // una sola vez, en el notificador.
        Map<String, Object> invitacion = invitacionDe(cuerpo.id());
        assertThat((String) invitacion.get("token_hash")).hasSize(64);
        assertThat(invitacion.get("usada_en")).isNull();
        assertThat(invitacion.get("invalidada")).isEqualTo(false);

        assertThat(invitaciones.tokenDe(CORREO)).hasSize(43);
    }

    @Test
    void alta_sinCredenciales_devuelve401() {

        ResponseEntity<String> respuesta =
                rest.postForEntity("/admin/usuarios", tutora(), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(cuantosUsuarios()).isZero();
    }

    @Test
    void validar_invitacionVigente_devuelveSoloPrimerNombre() {

        alta(tutora());
        String token = invitaciones.tokenDe(CORREO);

        ResponseEntity<InvitacionResponse> respuesta =
                rest.getForEntity("/invitaciones/{token}", InvitacionResponse.class, token);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        // Endpoint publico: no revela el nombre completo ni el correo.
        assertThat(respuesta.getBody().nombre()).isEqualTo("Ana");
        assertThat(respuesta.getBody().rol()).isEqualTo(Rol.TUTOR);

        // Validar no consume la invitacion.
        assertThat(estadoEnBase(CORREO)).isEqualTo("PENDIENTE");
    }

    @Test
    void validar_tokenInexistente_devuelve404() {

        ResponseEntity<String> respuesta = rest.getForEntity(
                "/invitaciones/{token}", String.class, "token-que-nadie-emitio");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void aceptar_metodoPassword_activaConHashArgon2id() {

        alta(tutora());
        String token = invitaciones.tokenDe(CORREO);

        ResponseEntity<UsuarioResponse> respuesta = aceptar(
                token, new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, CLAVE_NUEVA));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoUsuario.ACTIVO);

        assertThat(estadoEnBase(CORREO)).isEqualTo("ACTIVO");

        String hash = passwordHashEnBase(CORREO);
        assertThat(hash).startsWith("$argon2id$v=19$m=16384,t=2,p=1$");
        assertThat(passwordEncoder.matches(CLAVE_NUEVA, hash)).isTrue();
        assertThat(hash).doesNotContain(CLAVE_NUEVA);

        assertThat(invitacionDe(respuesta.getBody().id()).get("usada_en")).isNotNull();
    }

    @Test
    void aceptar_metodoPasskey_noGuardaContrasena() {

        alta(tutora());
        String token = invitaciones.tokenDe(CORREO);

        ResponseEntity<UsuarioResponse> respuesta = aceptar(
                token, new AceptarInvitacionRequest(MetodoActivacion.PASSKEY, null));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(estadoEnBase(CORREO)).isEqualTo("ACTIVO");
        // El ceremonial WebAuthn se completa despues; la cuenta queda activa sin
        // credencial de contrasena.
        assertThat(passwordHashEnBase(CORREO)).isNull();
    }

    @Test
    void aceptar_metodoPasswordSinContrasena_devuelve422() {

        alta(tutora());
        String token = invitaciones.tokenDe(CORREO);

        ResponseEntity<String> respuesta = rest.postForEntity(
                "/invitaciones/{token}/aceptar",
                new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, null),
                String.class, token);

        // La regla no cabe en una anotacion de Jakarta: vive en el service y sale como
        // 422 por el handler RFC 7807. En Spring Framework 7 el 422 se llama
        // UNPROCESSABLE_CONTENT; UNPROCESSABLE_ENTITY sigue existiendo pero deprecado, y
        // son dos constantes distintas del mismo enum.
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(respuesta.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(estadoEnBase(CORREO)).isEqualTo("PENDIENTE");
    }

    @Test
    void aceptar_segundaVez_devuelve410() {

        alta(tutora());
        String token = invitaciones.tokenDe(CORREO);
        aceptar(token, new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, CLAVE_NUEVA));

        ResponseEntity<String> repetida = rest.postForEntity(
                "/invitaciones/{token}/aceptar",
                new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, "otra-clave-distinta"),
                String.class, token);

        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(repetida.getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        // El segundo intento no reescribio la credencial del primero.
        assertThat(passwordEncoder.matches(CLAVE_NUEVA, passwordHashEnBase(CORREO))).isTrue();
    }

    @Test
    void reinvitar_usuarioPendiente_invalidaElTokenAnterior() {

        ResponseEntity<UsuarioResponse> creado = alta(tutora());
        String tokenViejo = invitaciones.tokenDe(CORREO);

        comoDireccion().postForEntity("/admin/usuarios/{id}/reinvitar", null,
                UsuarioResponse.class, creado.getBody().id());
        String tokenNuevo = invitaciones.tokenDe(CORREO);

        assertThat(tokenNuevo).isNotEqualTo(tokenViejo);

        ResponseEntity<String> conElViejo = rest.getForEntity(
                "/invitaciones/{token}", String.class, tokenViejo);
        assertThat(conElViejo.getStatusCode()).isEqualTo(HttpStatus.GONE);

        assertThat(rest.getForEntity("/invitaciones/{token}", String.class, tokenNuevo)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static CrearUsuarioRequest tutora() {
        return new CrearUsuarioRequest(NOMBRE, CORREO, Rol.TUTOR, List.of(UUID.randomUUID()));
    }

    private ResponseEntity<UsuarioResponse> aceptar(String token, AceptarInvitacionRequest peticion) {
        return rest.postForEntity("/invitaciones/{token}/aceptar", peticion,
                UsuarioResponse.class, token);
    }

    private String estadoEnBase(String correo) {
        return jdbc.queryForObject(
                "SELECT estado FROM usuario WHERE correo = ?", String.class, correo);
    }

    private String passwordHashEnBase(String correo) {
        return jdbc.queryForObject(
                "SELECT password_hash FROM usuario WHERE correo = ?", String.class, correo);
    }

    private Map<String, Object> invitacionDe(UUID usuarioId) {
        return jdbc.queryForMap(
                "SELECT token_hash, usada_en, invalidada FROM invitacion"
                        + " WHERE usuario_id = ? AND invalidada = FALSE", usuarioId);
    }

    private int cuantosUsuarios() {
        return jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class);
    }
}
