# Convenciones del proyecto

Este archivo define las reglas de ingeniería del proyecto. Todo contribuidor las respeta.

## Qué es este proyecto

Plataforma de comunicación escuela-familia (primaria/secundaria): los maestros nunca exponen
sus datos personales, los mensajes respetan horarios laborales, una capa de IA modera el trato,
y la gestión académica y de pagos vive en el mismo sistema.

## Reglas de trabajo (spec-driven)

1. **Nada se programa sin spec.** Cada feature nace como `docs/specs/NNN-nombre.md`
   (usar la plantilla `000-plantilla-spec.md`). La spec se aprueba antes de codear.
2. **Contract-first.** El contrato REST vive en `contracts/openapi/<servicio>.yaml` y los
   eventos Kafka en `contracts/events/*.schema.json`. Se actualizan ANTES de implementar.
   El Swagger que genera springdoc en runtime debe coincidir con el contrato.
3. **Decisiones de arquitectura** → ADR en `docs/adr/` (formato: contexto, decisión, consecuencias).
4. Si un cambio altera la arquitectura, actualizar el diagrama en `docs/diagrams/` y el README.

## Convenciones de código

- **Idioma**: código e identificadores en inglés; documentación, specs y ADRs en español.
- **Paquetes**: `com.jesuscastillo.escuela.<servicio>` con estructura por capa técnica
  (la convención más común en la industria). Todos los servicios usan el mismo árbol:

  ```
  com.jesuscastillo.escuela.<servicio>
  ├── client/                 clientes HTTP salientes hacia otros servicios + interceptores
  ├── config/                 beans, @ConfigurationProperties, seguridad
  ├── controller/             REST; depende de las interfaces de service, nunca de los Impl
  ├── dto/                    records request/response de la API
  ├── entity/                 entidades JPA y enums de dominio
  ├── event/                  payloads de eventos como records, uno por evento y version
  │   └── <origen>/           agrupados por servicio que los emite cuando se consumen varios
  ├── exception/              excepciones de negocio + handler RFC 7807
  ├── mapper/                 MapStruct
  ├── messaging/
  │   ├── listener/           consumers de Kafka (@KafkaListener), idempotentes por eventId
  │   └── publisher/          publicadores hacia Kafka (incluye el publicador del outbox)
  ├── repository/             Spring Data
  └── service/                interfaces de negocio
      └── impl/               implementaciones @Service con sufijo Impl
  ```

  Reglas que importan de este árbol:
  - **`messaging/` no es `service/`.** Publicar y consumir eventos es infraestructura,
    no lógica de negocio. Un `@KafkaListener` traduce el mensaje y delega en un service;
    no contiene reglas.
  - **Los eventos son records tipados en `event/`, no `Map<String,Object>`.** El record es
    el contrato en código y debe corresponder 1:1 con su esquema en `contracts/events/`.
  - **Sin prefijo `I` en las interfaces.** Es `UsuarioService` / `UsuarioServiceImpl`, no
    `IUsuarioService`: en Java el nombre limpio es la interfaz.
  - **Configuración de Kafka en YAML, no en clases.** Solo se escribe una `@Configuration`
    cuando la propiedad no alcanza (error handler, DLT, deserializador con `trusted.packages`).
- **Servicios de negocio**: se declaran como interfaz + implementación. La interfaz
  documenta el contrato de negocio ("Funcionalidad:"); la implementación documenta el flujo
  paso a paso ("Flujo: 1... 2... 3..."), vive en `service/impl` con sufijo `Impl`, inyecta
  por constructor (`@RequiredArgsConstructor`) y marca sus métodos con `@Transactional`
  cuando tocan base de datos. Los controllers dependen de la interfaz, nunca del `Impl`.
- **Cuándo NO va interfaz.** Una interfaz existe para que **alguien dependa de ella**: para
  poder sustituir la implementación, para simularla en pruebas, o para separar el contrato
  de la mecánica. Un componente que nadie inyecta no la necesita. El publicador del outbox
  es el ejemplo: lo invoca el planificador de Spring, no hay un segundo publicador posible
  y ningún test lo simula — una interfaz ahí sería un archivo que nadie usa. En cambio
  `NotificadorInvitaciones` sí la lleva, porque el servicio depende de ella y va a haber
  varias implementaciones (log, correo, push).
- **Comentarios**: un comentario responde **por qué**, nunca *qué* ni *acuérdate de*.
  - Sí: la restricción que no se ve en el código, la decisión y su alternativa descartada,
    la trampa conocida. Ejemplo real: *"Se corta la pasada para no romper el orden de los
    eventos del agregado."*
  - No: repetir lo que la línea ya dice, notas dirigidas a una persona, anécdotas de cómo
    se descubrió algo, ni recordatorios de tareas.
  - El trabajo pendiente **no vive en comentarios**. Lo que es de producto va al checklist
    de *Estado* del README del servicio y a su spec; lo que es operativo y personal va a
    `NOTAS.local.md`, que git ignora.
  - Un `TODO` solo se acepta si referencia una spec o un issue. Un `TODO` suelto es un
    comentario que nadie va a volver a leer.
- **Logging**: `@Slf4j` (Logback, el default de Spring Boot). Nivel `info` para hitos del
  flujo, `warn` para casos borde manejados, `error` solo para fallas reales.
- **DTOs**: records de Java. Mapeo con MapStruct. Nunca exponer entidades JPA en la API.
- **Errores**: formato RFC 7807 (`application/problem+json`) en todos los servicios.
- **Config**: cero secretos en el repo o en el config repo — solo variables de entorno.
- **Java 21** con virtual threads habilitados donde aplique; documentar el porqué en el servicio.

## Dependencias y versiones

Cada una de estas reglas nació de un problema real que costó horas. Los tropiezos
concretos están documentados en `docs/adr/ADR-002-spring-boot-4-java-21.md`.

1. **Nunca se declara `<version>` de una dependencia que administre el parent de Spring
   Boot.** Si Maven se queja de que falta la versión, la respuesta correcta es importar el
   BOM de ese proyecto en `dependencyManagement`, no clavar un número a mano.
2. **Siempre el *starter* de Boot, nunca la librería suelta.** `spring-boot-starter-kafka`,
   no `org.springframework.kafka:spring-kafka`. En Boot 4 las autoconfiguraciones viven en
   módulos por tecnología y el starter garantiza que venga el par completo (librería +
   autoconfiguración). Solo se declara la librería suelta cuando no existe starter.
3. **Antes de agregar una dependencia nueva**, verificar en Maven Central que exista una
   versión compatible con la línea de Boot que usamos. Si no la hay, ese servicio se queda
   en la versión anterior de Boot y se registra en un ADR.
4. **Jackson 3**: el paquete es `tools.jackson`, no `com.fasterxml.jackson`. Las
   anotaciones son la excepción: siguen en `com.fasterxml.jackson.annotation`.
5. **Java 21 LTS, sin excepciones.** El parent fija `maven.compiler.release=21`, así que el
   compilador ya rechaza cualquier API posterior. Lo que eso NO cubre es la JVM con la que
   se construye y se ejecuta: para eso está el enforcer.
6. **Una dependencia nueva se justifica en el PR**: qué resuelve y por qué no alcanza lo
   que ya está en el classpath.

### Verificación mecánica

Las reglas escritas se olvidan; las que rompen el build, no. Todos los `pom.xml` llevan:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <!-- El parent de Spring Boot no administra este plugin: la version va explicita. -->
    <version>3.6.3</version>
    <executions>
        <execution>
            <id>reglas-del-proyecto</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <!-- Compilar con release 21 no impide CONSTRUIR con una JVM mas
                         nueva, y esa diferencia ya costo una sesion de depuracion.
                         Aqui se corta de tajo, con mensaje explicito. -->
                    <requireJavaVersion>
                        <version>[21,22)</version>
                        <message>Este proyecto se construye con Java 21 LTS. Ver ADR-002.</message>
                    </requireJavaVersion>
                    <requireMavenVersion>
                        <version>[3.9,)</version>
                    </requireMavenVersion>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Patrones de diseño y SOLID

El criterio es **el patrón lo pide el dominio, no el CV**. Un patrón que no resuelve un
problema real de este sistema es deuda disfrazada de arquitectura, y un revisor con
experiencia lo detecta más rápido que la ausencia del patrón.

Patrones adoptados y por qué el dominio los exige:

| Patrón | Dónde | Por qué aquí sí |
|---|---|---|
| Transactional Outbox | todos los productores de eventos | La escritura de negocio y el evento tienen que ser atómicos; sin esto se pierden eventos o se emiten de más |
| Idempotent Consumer | todos los listeners | La entrega es "al menos una vez": el consumidor deduplica por `eventId` |
| Strategy | canal de notificación (correo, push, SMS) | Habrá varias implementaciones reales conviviendo, elegidas en runtime |
| Chain of Responsibility | moderación de mensajes | La revisión es una secuencia de reglas independientes (tono, horario, datos personales) que crece con el producto |
| Adapter / Ports & Adapters | proveedor de IA | El moderador se define como interfaz; Ollama en local y un proveedor externo en la nube son intercambiables |
| Saga (coreografía) | pagos de colegiaturas | El cobro cruza varios servicios sin transacción distribuida |

SOLID, con el ejemplo concreto del proyecto:

- **S** — `InvitationTokenService` genera y hashea tokens; `OutboxService` registra eventos;
  `OutboxPublisher` los envía. Tres responsabilidades, tres clases.
- **O** — agregar un canal de notificación no debe tocar `InvitacionServiceImpl`.
- **L** — cualquier `NotificadorInvitaciones` debe poder sustituir a otro sin que el service
  cambie su comportamiento ni sus expectativas de error.
- **I** — interfaces chicas por caso de uso (`UsuarioService`, `InvitacionService`) en lugar
  de un `IdentityService` que lo haga todo.
- **D** — el negocio depende de la interfaz `NotificadorInvitaciones`; quién manda el correo
  se decide por configuración. Es la razón por la que existe `LogNotificadorInvitaciones`
  en desarrollo.

**Cuándo NO aplicar un patrón:** con dos casos y sin un tercero a la vista, un `if` explícito
gana. Si el `if` empieza a crecer o llega la tercera variante, ahí se refactoriza a Strategy
y se registra la decisión en un ADR.

## Validación de entrada

- La **normalización** vive en el DTO, no en el service: los records llevan constructor
  compacto que recorta espacios y baja a minúsculas antes de que corra la validación.
  Jackson construye el record por su constructor canónico, así que `@Email` ya recibe el
  valor limpio. Sin esto, un correo copiado de una hoja de cálculo con un espacio al final
  se rechaza con 422 aunque sea válido.
- Ser **consistente en la permisividad**: si se normalizan mayúsculas, también espacios.
  Aceptar una variación y rechazar la otra confunde a quien consume la API.
- Las reglas que no se pueden expresar con anotaciones (p. ej. "la contraseña es obligatoria
  solo cuando el método es PASSWORD") van en el service y salen como 422 vía el handler.

## Tests

- JUnit 5. Unitarios para lógica de dominio; integración con **Testcontainers**
  (Postgres/Kafka reales) para repositorios, consumers y controllers.
- Naming: `metodo_condicion_resultadoEsperado` (p. ej. `crearMensaje_fueraDeHorario_seEncola`).
- Un feature no está terminado sin al menos 1 test de integración.

## Git

- Commits convencionales: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
  El sujeto va en minúsculas, en imperativo y **sin acentos**, para que ninguna
  codificación ensucie el subject. El cuerpo explica el *por qué*, igual que un comentario.

### Ramas

**Nada se commitea directo a `main`.** Toda unidad de trabajo nace en una rama, incluso un
`docs:` de una línea. No es ceremonia: es lo que garantiza que el CI corra **antes** de que
`main` lo reciba. Una regla con excepciones obliga a decidir caso por caso qué es "trivial",
y esa decisión siempre se toma a favor de la prisa.

El prefijo de la rama es el mismo vocabulario de los commits:

| Prefijo | Para qué |
|---|---|
| `feat/` | funcionalidad nueva |
| `fix/` | corrección de un defecto |
| `test/` | pruebas de código que ya existe |
| `refactor/` | cambio interno sin cambio de comportamiento |
| `docs/` | documentación, specs, ADRs, README |
| `chore/` | build, dependencias, CI, estructura |

Nomenclatura: `<tipo>/NNN-nombre-corto` cuando el trabajo pertenece a una spec, donde NNN
es su número; `<tipo>/nombre-corto` cuando no pertenece a ninguna, como el README de la
raíz. Ejemplos: `feat/001-authorization-server`, `test/001-tests-integracion`,
`chore/ci-cachea-imagenes`.

**Una rama es una unidad revertible**, no un tipo de cambio. Si una entrega trae varios
commits de tipos distintos, van todos en la misma rama y el prefijo lo pone el tipo
dominante. Lo que no cabe junto es lo que se revertiría por separado: eso es otra rama.

### Secuencia

```bash
git switch main && git pull               # 1. partir de main actualizado
git switch -c test/001-tests-integracion  # 2. rama con su prefijo
# ... commits pequeños y convencionales ...
./mvnw -pl services/identity verify       # 3. verde en local ANTES de empujar
git push -u origin test/001-tests-integracion
gh pr create --fill                       # 4. PR
```

5. **El CI verde es requisito, no sugerencia.** Si está rojo, se arregla en la rama.
6. Se integra solo cuando se cumple la *Definition of Done* de la sección siguiente.
7. Merge y limpieza en un paso:

```bash
gh pr merge --merge --delete-branch
```

### Merge commit, siempre

`--merge` y nunca squash ni rebase. El squash fundiría en un commit los mensajes
granulares que cuesta trabajo escribir, y el rebase borra la frontera de la entrega. El
merge commit conserva las dos cosas: los commits individuales y el dato de qué commits
formaban una entrega. Eso es lo que hace que revertirla completa sea un comando:

```bash
git revert -m 1 <sha-del-merge>
```

Conviene dejar en la configuración del repositorio en GitHub **solo** *Create a merge
commit* habilitado. Con los tres botones disponibles, tarde o temprano se aprieta el que no
era y el historial queda mezclado.

**Si `main` avanzó mientras la rama estaba abierta**, se pone al día con `git rebase main`
antes de integrar, para que el PR se revise contra el `main` de hoy y el CI pruebe lo que
de verdad se va a mergear. Reescribir la propia rama y hacer `push --force-with-lease` es
correcto; reescribir `main` no lo es nunca.

## Definition of Done

1. Spec aprobada y contrato actualizado
2. Tests unitarios + integración pasando en CI
3. Sin secretos, sin warnings nuevos
4. README/diagramas actualizados si cambió la arquitectura
