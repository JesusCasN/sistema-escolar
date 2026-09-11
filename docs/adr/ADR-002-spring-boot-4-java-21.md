# ADR-002: Spring Boot 4 + Java 21

**Estado:** aceptada · **Fecha:** 2026-09-09

## Contexto

El proyecto busca demostrar dominio de tecnología reciente y de uso real en la industria.
Spring Boot 4 (Framework 7) es la generación actual; Java 21 es LTS con virtual threads.

## Decisión

- **Java 21 LTS** en todos los servicios, con virtual threads habilitados donde el perfil
  de carga lo justifique (I/O-bound: notifications, clientes HTTP salientes). Ver ADR-004.
- **Spring Boot 4** como base. Se aprovechan features propias de la versión: versionado
  de API nativo (`@ApiVersion`) en el gateway y clientes HTTP declarativos.
- **Excepción permitida:** si una dependencia clave (p. ej. la línea estable de Spring AI)
  aún no soporta Boot 4, ese servicio arranca en Boot 3.5.x y se migra después. La
  independencia de versiones entre servicios es una ventaja deliberada de esta arquitectura
  (ver ADR-001) y quedará registrada aquí con fecha de migración.

## Consecuencias

- (+) Stack demostrable como "actual" ante cualquier revisor técnico.
- (−) Menos material de referencia ante bugs de versiones nuevas; se mitiga fijando
  versiones exactas y documentando los tropiezos abajo.

## Diferencias con Boot 3 que ya nos costaron tiempo

Se documentan aquí porque no son obvias y cuestan horas de depuración. Todas se
descubrieron implementando el servicio `identity`.

### Las autoconfiguraciones se separaron en módulos por tecnología

En Boot 3, `spring-boot-autoconfigure` traía todas las autoconfiguraciones. En Boot 4
viven en artefactos independientes (`spring-boot-jdbc`, `spring-boot-hibernate`,
`spring-boot-jpa`, `spring-boot-flyway`…).

Consecuencia práctica: agregar solo la librería de un tercero **no basta**. Con
`flyway-core` en el classpath pero sin `org.springframework.boot:spring-boot-flyway`,
Flyway existe pero nunca se ejecuta — y **no emite ningún log**, así que el síntoma es
un `Schema validation: missing table` de Hibernate, que apunta al lugar equivocado.

Lo mismo pasó con Kafka: `org.springframework.kafka:spring-kafka` aporta la librería,
pero el `KafkaTemplate` lo publica `spring-boot-kafka`. Sin ese módulo el arranque falla
con `No qualifying bean of type 'org.springframework.kafka.core.KafkaTemplate'`.

**Regla práctica adoptada:** preferir siempre el *starter* de Boot
(`spring-boot-starter-kafka`) sobre la librería suelta. El starter trae la librería más su
módulo de autoconfiguración y deja las versiones en manos del parent. Solo se declara la
librería suelta cuando no existe starter para ella.

**Cómo se diagnostica:** si un bean que Boot "debería" crear no existe, o si una
tecnología no hace nada y no aparece en el log, revisar primero si falta su módulo de
autoconfiguración — no la configuración del `application.yml`. La ausencia de logs es la
pista: una tecnología mal configurada se queja; una sin autoconfiguración calla.

### Jackson 3: cambió el paquete raíz

Boot 4 usa **Jackson 3**, cuyo paquete es `tools.jackson` (antes `com.fasterxml.jackson`).
El bean del `ObjectMapper` que publica Boot es el de Jackson 3.

Detalle que confunde: Jackson 2 sigue apareciendo en el classpath porque lo arrastran
librerías de terceros (en este proyecto, springdoc/swagger-core). Está como librería pero
**no como bean**, así que inyectar `com.fasterxml.jackson.databind.ObjectMapper` compila y
falla al arrancar.

Al migrar código:

- `com.fasterxml.jackson.databind` → `tools.jackson.databind`
- `com.fasterxml.jackson.core` → `tools.jackson.core`
- Las **anotaciones** (`@JsonProperty`, `@JsonIgnore`…) siguen en
  `com.fasterxml.jackson.annotation`: Jackson 3 las reutiliza y ese import no se toca.
- Las excepciones ahora son *unchecked* y cuelgan de `tools.jackson.core.JacksonException`;
  `JsonProcessingException` desaparece con ese nombre.

### Los starters de test también son modulares

Ya no se declara `spring-boot-starter-test` directamente. Se declaran los starters de la
tecnología bajo prueba (`spring-boot-starter-webmvc-test`, `-data-jpa-test`,
`-security-test`) y cada uno arrastra el núcleo (JUnit 5, AssertJ, Mockito).

### Boot 4 ya no administra las versiones de Testcontainers

Hay que importar su BOM explícitamente en `dependencyManagement`, o Maven falla con
`'dependencies.dependency.version' ... is missing`.
