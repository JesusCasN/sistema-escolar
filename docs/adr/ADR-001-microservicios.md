# ADR-001: Arquitectura de microservicios

**Estado:** aceptada · **Fecha:** 2026-09-09

## Contexto

El sistema tiene dominios con ciclos de vida y cargas muy distintas: mensajería (tráfico
constante, sensible a latencia), moderación (dependiente de un LLM, latencia variable),
académico (picos al final de bimestre), pagos (requisitos de integridad más estrictos).
Además, el proyecto es un portafolio cuyo objetivo explícito es demostrar diseño de
sistemas distribuidos.

## Decisión

Siete microservicios (gateway, identity, messaging, moderation, academics, payments,
notifications) comunicados de forma síncrona vía REST (a través del gateway) y asíncrona
vía eventos Kafka. Cada servicio es dueño exclusivo de su base de datos.

## Consecuencias

- (+) Aislamiento de fallas: la caída de moderation no tumba messaging (circuit breaker
  con política documentada por spec).
- (+) Cada servicio puede evolucionar versión de framework y esquema de datos por separado.
- (−) Complejidad operativa: se mitiga con Docker Compose para local y contratos
  explícitos (OpenAPI + JSON Schema) para toda comunicación.
- (−) Consistencia eventual entre servicios: aceptada y documentada por flujo.
