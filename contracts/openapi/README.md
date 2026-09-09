# Contratos REST (contract-first)

Un archivo OpenAPI 3.1 por servicio: `<servicio>.yaml`.

Regla: el contrato se actualiza **antes** de implementar (ver CONTRIBUTING.md). El Swagger que
genera springdoc en runtime debe coincidir con lo declarado aquí; cualquier divergencia
es un bug del código, no del contrato.

Los archivos aparecen a partir de la Fase 1, uno por spec implementada.
