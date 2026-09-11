package com.jesuscastillo.escuela.identity.integracion;

import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest;
import com.jesuscastillo.escuela.identity.dto.AceptarInvitacionRequest.MetodoActivacion;
import com.jesuscastillo.escuela.identity.dto.CrearUsuarioRequest;
import com.jesuscastillo.escuela.identity.dto.UsuarioResponse;
import com.jesuscastillo.escuela.identity.entity.Rol;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * El evento {@code usuario.activado.v1} saliendo de verdad hacia Kafka.
 * <p>
 * Esta es la prueba que exige el criterio de aceptación de la SPEC-001. Cubre lo que
 * ninguna prueba unitaria puede: que el {@code ObjectMapper} de Jackson 3 serialice el
 * sobre exactamente como lo declara el contrato, que el planificador del outbox dispare
 * solo, y que el broker confirme antes de que la fila se marque como publicada.
 * <p>
 * El JSON se valida contra {@code contracts/events/usuario.activado.v1.schema.json}, el
 * archivo real y no una copia: el contrato manda sobre el código también aquí. El esquema
 * declara {@code additionalProperties: false}, así que cualquier campo de más en el
 * payload — los del sobre colándose desde {@code EventoDeDominio}, por ejemplo — revienta
 * la prueba.
 */
@DisplayName("Outbox hacia Kafka")
class OutboxKafkaIT extends SoporteIntegracion {

    /** El publicador arma el tópico como escuela.identity.<tipo>. */
    private static final String TOPICO = "escuela.identity.usuario.activado";

    private static final Schema ESQUEMA = esquemaDeUsuarioActivado();

    private KafkaConsumer<String, String> consumidor;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void crearTopico() throws InterruptedException {
        try (Admin admin = Admin.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of(new NewTopic(TOPICO, 1, (short) 1))).all().get();
        } catch (ExecutionException e) {
            // Se crea explicitamente para no depender de la autocreacion del broker, que
            // es una propiedad que cualquiera puede apagar. Si ya existe, no hay nada que
            // hacer: las clases de prueba comparten el contenedor.
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw new IllegalStateException("No se pudo crear el topico " + TOPICO, e);
            }
        }
    }

    @BeforeEach
    void abrirConsumidor() {
        consumidor = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                // Grupo nuevo en cada prueba: se lee el topico desde el principio y no
                // se hereda el avance de la prueba anterior.
                ConsumerConfig.GROUP_ID_CONFIG, "prueba-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
        consumidor.subscribe(List.of(TOPICO));
    }

    @AfterEach
    void cerrarConsumidor() {
        if (consumidor != null) {
            consumidor.close();
        }
    }

    @Test
    void activacion_porHttp_publicaEventoQueCumpleElEsquemaDelContrato() {

        UUID hijoUno = UUID.randomUUID();
        UUID hijoDos = UUID.randomUUID();
        UsuarioResponse tutora = altaYActivacion("carmen.diaz@example.com",
                "Carmen Diaz Ruiz", List.of(hijoUno, hijoDos));

        ConsumerRecord<String, String> registro = esperarEventoDe(tutora.id());

        // La clave es el agregado: manda todos los eventos de un usuario a la misma
        // particion, que es lo unico que garantiza su orden.
        assertThat(registro.key()).isEqualTo(tutora.id().toString());

        List<Error> errores = ESQUEMA.validate(registro.value(), InputFormat.JSON);
        assertThat(errores)
                .describedAs("El evento no cumple usuario.activado.v1.schema.json: %s",
                        errores.stream().map(Error::getMessage).toList())
                .isEmpty();

        JsonNode sobre = objectMapper.readTree(registro.value());
        assertThat(sobre.propertyNames())
                .containsExactlyInAnyOrder("eventId", "occurredAt", "type", "version", "payload");
        assertThat(sobre.get("type").asString()).isEqualTo("usuario.activado");
        assertThat(sobre.get("version").asInt()).isEqualTo(1);

        JsonNode payload = sobre.get("payload");
        // Redundante con el additionalProperties del esquema, y a proposito: si alguien
        // afloja el contrato, el fallo sigue diciendo cual es la clave que sobra.
        assertThat(payload.propertyNames())
                .containsExactlyInAnyOrder("userId", "schoolId", "rol", "nombre", "vinculos");
        assertThat(payload.get("userId").asString()).isEqualTo(tutora.id().toString());
        assertThat(payload.get("rol").asString()).isEqualTo("TUTOR");
        assertThat(payload.get("nombre").asString()).isEqualTo("Carmen Diaz Ruiz");

        JsonNode vinculos = payload.get("vinculos");
        assertThat(vinculos.size()).isEqualTo(2);
        assertThat(vinculos.values().stream().map(v -> v.get("tipo").asString()).toList())
                .containsOnly("HIJO");
        assertThat(vinculos.values().stream().map(v -> v.get("id").asString()).toList())
                .containsExactlyInAnyOrder(hijoUno.toString(), hijoDos.toString());
    }

    @Test
    void activacion_porHttp_marcaElEventoComoPublicadoEnElOutbox() {

        UsuarioResponse maestro = altaYActivacion("luis.perez@example.com",
                "Luis Perez", List.of());

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT type, version, aggregate_id FROM outbox_event");
        assertThat(fila.get("type")).isEqualTo("usuario.activado");
        assertThat(fila.get("version")).isEqualTo(1);
        assertThat(fila.get("aggregate_id")).isEqualTo(maestro.id());

        // Lo marca el planificador real, no una llamada a mano desde la prueba.
        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertThat(pendientesEnOutbox())
                        .describedAs("eventos sin publicar").isZero());
    }

    @Test
    void aceptar_dosVeces_publicaUnSoloEvento() {

        String correo = "rosa.mena@example.com";
        altaYActivacion(correo, "Rosa Mena", List.of());

        rest.postForEntity("/invitaciones/{token}/aceptar",
                new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, "otra-clave-distinta"),
                String.class, invitaciones.tokenDe(correo));

        // El 410 corta antes de tocar el outbox: un consumidor no recibe la activacion
        // dos veces por reintentar la peticion.
        assertThat(eventosEnOutbox()).isEqualTo(1);
    }

    /** Alta por el endpoint de administración y activación con contraseña. */
    private UsuarioResponse altaYActivacion(String correo, String nombre, List<UUID> hijos) {

        UsuarioResponse creado = alta(
                new CrearUsuarioRequest(nombre, correo, Rol.TUTOR, hijos)).getBody();
        assertThat(creado).isNotNull();

        rest.postForEntity("/invitaciones/{token}/aceptar",
                new AceptarInvitacionRequest(MetodoActivacion.PASSWORD, "clave-de-la-activacion"),
                UsuarioResponse.class, invitaciones.tokenDe(correo));

        return creado;
    }

    private ConsumerRecord<String, String> esperarEventoDe(UUID usuarioId) {

        String clave = usuarioId.toString();
        List<ConsumerRecord<String, String>> recibidos = new ArrayList<>();

        // pollInSameThread es obligatorio: KafkaConsumer no admite acceso desde varios
        // hilos, y Awaitility evalua en un hilo propio si no se le dice lo contrario.
        await().atMost(Duration.ofSeconds(20))
                .pollInSameThread()
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    consumidor.poll(Duration.ofMillis(500)).records(TOPICO).forEach(recibidos::add);
                    assertThat(recibidos)
                            .describedAs("mensajes en %s", TOPICO)
                            .anyMatch(registro -> clave.equals(registro.key()));
                });

        return recibidos.stream()
                .filter(registro -> clave.equals(registro.key()))
                .findFirst()
                .orElseThrow();
    }

    private int pendientesEnOutbox() {
        return jdbc.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE published_at IS NULL", Integer.class);
    }

    private int eventosEnOutbox() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class);
    }

    private static Schema esquemaDeUsuarioActivado() {

        // La ruta la inyecta failsafe; el valor por omision cubre lanzar la prueba desde
        // el IDE, donde el directorio de trabajo es el del modulo.
        Path ruta = Path.of(System.getProperty("contratos.dir", "../../contracts"))
                .resolve("events/usuario.activado.v1.schema.json")
                .toAbsolutePath().normalize();

        SchemaRegistry registro = SchemaRegistry.withDefaultDialect(
                SpecificationVersion.DRAFT_2020_12,
                constructor -> constructor.schemaRegistryConfig(
                        // Sin esto, "format": "uuid" y "date-time" quedan como anotacion
                        // y no se verifican: el draft 2020-12 los declara opcionales.
                        SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build()));

        try (InputStream entrada = Files.newInputStream(ruta)) {
            return registro.getSchema(entrada);
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontro el contrato en " + ruta, e);
        }
    }
}
