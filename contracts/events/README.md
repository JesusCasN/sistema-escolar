# Contratos de eventos Kafka

Un JSON Schema por evento, versionado en el nombre: `mensaje.creado.v1.schema.json`.

Reglas:

1. Los eventos son contratos igual que las APIs: cambiar el payload sin versionar rompe
   consumidores silenciosamente.
2. Cambios compatibles (agregar campo opcional) → misma versión. Cambios incompatibles →
   nueva versión del evento y periodo de convivencia documentado en la spec.
3. Todo evento lleva sobre común: `eventId` (UUID), `occurredAt` (ISO-8601), `type`,
   `version`, `payload`.

## Tópicos

El nombre del tópico se arma así:

```
escuela.<servicio-productor>.<tipo-de-evento>
```

Existen hoy:

| Tópico | Productor | Consumidores previstos |
|---|---|---|
| `escuela.identity.usuario.activado` | identity | messaging, notifications |
| `escuela.identity.usuario.suspendido` | identity | messaging, notifications |

Cuatro decisiones detrás de ese esquema:

- **Un tópico por tipo de evento**, no uno por servicio. Así cada consumidor se suscribe
  solo a lo que le importa, en lugar de leer todo y filtrar.
- **El servicio productor va en el nombre.** Leyendo el tópico sabes de dónde viene el
  dato y a quién reclamarle si viene mal.
- **La versión NO va en el tópico**, va en el sobre. Cuando conviven `v1` y `v2` viajan
  por el mismo tópico y cada consumidor atiende la que entiende. Meter la versión en el
  nombre obligaría a los consumidores a resuscribirse en cada cambio.
- **La clave del mensaje es el `aggregateId`.** Kafka garantiza orden dentro de una
  partición, y la clave decide la partición: así todos los eventos de un mismo usuario
  llegan en el orden en que ocurrieron. Sin clave, una suspensión podría procesarse antes
  que la activación.

### Reintentos y mensajes muertos

Cuando un servicio empiece a consumir, su tópico de mensajes irrecuperables se llama
igual con el sufijo `.DLT`. Un mensaje que falla se reintenta con backoff y, si sigue
fallando, termina ahí para revisión manual — nunca se descarta ni se reintenta infinito.

### Idempotencia

La entrega es "al menos una vez": un mismo evento puede llegar dos veces. Todo consumidor
descarta los `eventId` que ya procesó. Eso no es opcional.
