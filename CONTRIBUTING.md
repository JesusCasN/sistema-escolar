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
  (la convención más común en la industria):
  `controller` (REST), `service` (interfaces de negocio) + `service/impl`
  (implementaciones anotadas con `@Service`), `repository` (Spring Data), `entity`
  (JPA y enums), `dto` (records request/response), `config` (beans y seguridad),
  `exception` (excepciones y handler RFC 7807).
- **Servicios**: se declaran como interfaz + implementación. La interfaz documenta el
  contrato de negocio ("Funcionalidad:"); la implementación documenta el flujo paso a paso
  ("Flujo: 1... 2... 3..."), vive en `service/impl` con sufijo `Impl`, inyecta por
  constructor (`@RequiredArgsConstructor`) y marca sus métodos con `@Transactional`
  cuando tocan base de datos. Los controllers dependen de la interfaz, nunca del `Impl`.
- **Logging**: `@Slf4j` (Logback, el default de Spring Boot). Nivel `info` para hitos del
  flujo, `warn` para casos borde manejados, `error` solo para fallas reales.
- **DTOs**: records de Java. Mapeo con MapStruct. Nunca exponer entidades JPA en la API.
- **Errores**: formato RFC 7807 (`application/problem+json`) en todos los servicios.
- **Config**: cero secretos en el repo o en el config repo — solo variables de entorno.
- **Java 21** con virtual threads habilitados donde aplique; documentar el porqué en el servicio.

## Tests

- JUnit 5. Unitarios para lógica de dominio; integración con **Testcontainers**
  (Postgres/Kafka reales) para repositorios, consumers y controllers.
- Naming: `metodo_condicion_resultadoEsperado` (p. ej. `crearMensaje_fueraDeHorario_seEncola`).
- Un feature no está terminado sin al menos 1 test de integración.

## Git

- Rama por feature: `feat/NNN-nombre-corto` (NNN = número de spec). Merge a `main` vía PR.
- Commits convencionales: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
- El CI debe estar verde antes de mergear.

## Definition of Done

1. Spec aprobada y contrato actualizado
2. Tests unitarios + integración pasando en CI
3. Sin secretos, sin warnings nuevos
4. README/diagramas actualizados si cambió la arquitectura
