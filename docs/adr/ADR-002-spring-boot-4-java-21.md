# ADR-002: Spring Boot 4 + Java 21

**Estado:** aceptada · **Fecha:** 2026-09-09

## Contexto

El proyecto busca demostrar dominio de tecnología reciente y de uso real en la industria.
Spring Boot 4 (Framework 7) es la generación actual; Java 21 es LTS con virtual threads.

## Decisión

- **Java 21 LTS** en todos los servicios, con virtual threads habilitados donde el perfil
  de carga lo justifique (I/O-bound: notifications, clientes HTTP salientes).
- **Spring Boot 4** como base. Se aprovechan features propias de la versión: versionado
  de API nativo (`@ApiVersion`) en el gateway y clientes HTTP declarativos.
- **Excepción permitida:** si una dependencia clave (p. ej. la línea estable de Spring AI)
  aún no soporta Boot 4, ese servicio arranca en Boot 3.5.x y se migra después. La
  independencia de versiones entre servicios es una ventaja deliberada de esta arquitectura
  (ver ADR-001) y quedará registrada aquí con fecha de migración.

## Consecuencias

- (+) Stack demostrable como "actual" ante cualquier revisor técnico.
- (−) Menos material de referencia ante bugs de versiones nuevas; se mitiga fijando
  versiones exactas y documentando workarounds en el ADR correspondiente.
