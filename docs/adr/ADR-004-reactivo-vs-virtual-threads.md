# ADR-004: Modelo de concurrencia — imperativo con virtual threads, reactivo donde aporta

**Estado:** aceptada · **Fecha:** 2026-09-09

## Contexto

Spring ofrece dos modelos: el stack servlet imperativo (Spring MVC) y el stack reactivo
(WebFlux + Project Reactor). Durante años la recomendación de moda fue "reactivo para todo
lo que escale". Dos cosas cambiaron ese cálculo:

1. **Virtual threads (Java 21, Project Loom).** Un hilo virtual bloqueado no retiene un hilo
   del sistema operativo, así que el código imperativo bloqueante escala en concurrencia
   de forma comparable al reactivo para cargas dominadas por I/O — que es exactamente el
   perfil de este sistema (consultas a base de datos, llamadas HTTP, publicación a Kafka).
2. **El costo real del reactivo es de mantenimiento.** Stack traces inservibles, depuración
   difícil, y la exigencia de que TODA la cadena sea no bloqueante: una sola llamada
   bloqueante dentro de un flujo reactivo (por ejemplo JPA) tumba el beneficio y además
   introduce bugs difíciles de diagnosticar.

Un factor decisivo para este proyecto: la persistencia usa **JPA/Hibernate, que es
bloqueante**. Ir full reactivo obligaría a migrar a R2DBC, perdiendo JPA, y a reescribir
el modelo de datos. Eso es costo alto sin beneficio medible al volumen de una escuela.

## Decisión

**Imperativo (Spring MVC) con virtual threads habilitados como modelo por defecto** en todos
los servicios:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**Reactivo de forma quirúrgica**, solo donde el modelo aporta algo que el imperativo no da:

| Caso | Por qué ahí sí |
|---|---|
| Feed de mensajes en vivo (`messaging`) | Server-Sent Events: la conexión queda abierta minutos u horas. Es streaming con backpressure, justo para lo que sirve el modelo reactivo. Se implementa con `SseEmitter` (nativo de MVC) o devolviendo un `Flux<ServerSentEvent<>>` desde el controller |
| Respuesta del modelo en `moderation` | Spring AI expone una API de streaming; consumir tokens conforme llegan evita esperar la respuesta completa |

Ambos casos son de **transporte**, no de persistencia: la base de datos se sigue tocando de
forma bloqueante sobre un hilo virtual, sin mezclar JPA dentro de una cadena reactiva.

## Consecuencias

- (+) Código legible y depurable; el equipo (y el autor en una entrevista) puede explicar
  cada línea.
- (+) Se conserva JPA, Flyway y todo el ecosistema bloqueante maduro.
- (+) Escalabilidad de concurrencia comparable al reactivo para este perfil de carga.
- (+) Se demuestra dominio de AMBOS modelos y, sobre todo, **criterio para elegir**: usar
  reactivo donde aporta en vez de por moda.
- (−) Si en el futuro apareciera una carga con miles de conexiones concurrentes por
  instancia y presión de memoria, habría que reevaluar. Este ADR se revisaría entonces con
  números de carga reales (ver la prueba con k6 en la Fase 5), no por intuición.

## Trampa conocida

Con virtual threads hay que evitar `synchronized` alrededor de operaciones bloqueantes
largas (puede fijar el hilo virtual a su portador — *pinning*). Se usa `ReentrantLock` en
esos casos. Queda como regla en `CONTRIBUTING.md`.
