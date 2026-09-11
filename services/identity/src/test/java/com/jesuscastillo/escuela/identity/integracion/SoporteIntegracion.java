package com.jesuscastillo.escuela.identity.integracion;

import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base de las pruebas de integración: Postgres y Kafka reales, aplicación real.
 * <p>
 * Aquí no sirve una base en memoria. El esquema lo manda Flyway y {@code ddl-auto:
 * validate} lo verifica contra el motor de verdad; con H2 se estarían probando dos
 * dialectos distintos al de producción. Lo mismo del lado de la mensajería: el outbox
 * solo demuestra algo si un broker confirma la escritura.
 * <p>
 * Las pruebas entran por HTTP, así que corren en otro hilo que la prueba: ni el
 * {@code @Transactional} de Spring revierte lo que escriben, ni sirve compartir estado
 * por el EntityManager. Por eso la limpieza es un TRUNCATE.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// En Boot 4 los modulos de prueba estan separados y el TestRestTemplate ya no se
// registra solo: hay que pedirlo.
@AutoConfigureTestRestTemplate
@Import(CapturaInvitaciones.Config.class)
@ActiveProfiles("test")
abstract class SoporteIntegracion {

    protected static final String ADMIN_USUARIO = "direccion";
    protected static final String ADMIN_PASSWORD = "clave-solo-de-pruebas";

    /**
     * Argon2id cuesta ~100 ms: se calcula una vez y no en cada resolución de la propiedad.
     */
    private static final String ADMIN_PASSWORD_HASH =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(ADMIN_PASSWORD);

    // Las mismas imagenes que infra/docker-compose.yml: se prueba contra los motores que
    // ya corren en local, y no hay que bajar nada nuevo.
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"));

    @ServiceConnection
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:4.1.2"));

    // Se arrancan a mano y no con @Container porque así viven una sola vez para todas las
    // clases de prueba, igual que el contexto de Spring que Boot cachea entre ellas.
    // Arrancar Kafka por clase multiplicaria el tiempo del build sin comprar nada.
    static {
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void credencialDeAdministracion(DynamicPropertyRegistry registro) {
        // El prefijo {noop} no funciona aqui: al existir un bean PasswordEncoder, Boot lo
        // usa tal cual y la cadena se interpreta como un hash. Se registra el hash real de
        // una contrasena propia de las pruebas, para no arrastrar la de desarrollo.
        registro.add("spring.security.user.password", () -> ADMIN_PASSWORD_HASH);
    }

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected CapturaInvitaciones invitaciones;

    @BeforeEach
    void limpiarEstado() {
        jdbc.execute("TRUNCATE TABLE outbox_event, invitacion, usuario RESTART IDENTITY CASCADE");
        invitaciones.limpiar();
    }

    /** Cliente autenticado como dirección, que es lo único que abre /admin. */
    protected TestRestTemplate comoDireccion() {
        return rest.withBasicAuth(ADMIN_USUARIO, ADMIN_PASSWORD);
    }

    protected ResponseEntity<UsuarioResponse> alta(CrearUsuarioRequest request) {
        return comoDireccion().postForEntity("/admin/usuarios", request, UsuarioResponse.class);
    }
}
