# SPEC-NNN: <Nombre del feature>

**Servicio(s):** <messaging | moderation | ...>
**Estado:** borrador | aprobada | implementada
**Rama:** feat/NNN-nombre-corto

## Problema

Qué se resuelve y para quién (1-2 párrafos, en lenguaje de negocio).

## Solución propuesta

Cómo funciona a alto nivel. Diagramas de secuencia en Mermaid si el flujo cruza servicios.

## Contrato

- Endpoints nuevos/modificados → actualizar `contracts/openapi/<servicio>.yaml`
- Eventos nuevos/modificados → actualizar `contracts/events/`
- Resumen aquí: método, ruta, request/response principales, códigos de error.

## Casos borde

Lista explícita: qué pasa si el dato no existe, llega duplicado, llega fuera de horario,
falla el servicio del que dependo (¿fail-open o fail-closed y por qué?), etc.

## Criterios de aceptación

- [ ] Dado X, cuando Y, entonces Z
- [ ] ...

## Fuera de alcance

Qué NO cubre esta spec (para evitar scope creep).
