# Contratos de eventos Kafka

Un JSON Schema por evento, versionado en el nombre: `mensaje.creado.v1.schema.json`.

Reglas:

1. Los eventos son contratos igual que las APIs: cambiar el payload sin versionar rompe
   consumidores silenciosamente.
2. Cambios compatibles (agregar campo opcional) → misma versión. Cambios incompatibles →
   nueva versión del evento y periodo de convivencia documentado en la spec.
3. Todo evento lleva sobre común: `eventId` (UUID), `occurredAt` (ISO-8601), `type`,
   `version`, `payload`.
