# ADR-003: Configuración centralizada con Spring Cloud Config + Bus

**Estado:** aceptada · **Fecha:** 2026-09-09

## Contexto

Siete servicios × varios ambientes = configuración dispersa. Alternativas evaluadas:
archivos por servicio (no escala), ConfigMaps de Kubernetes (nativo pero sin historial
propio ni refresh coordinado fuera de K8s), Spring Cloud Config Server con backend Git.

## Decisión

**Spring Cloud Config Server** leyendo del repo `sistema-escolar-config`, con
**Spring Cloud Bus sobre Kafka** para refresh dinámico (`@RefreshScope`): un cambio de
configuración (p. ej. la ventana de horario de mensajes) se aplica a todos los servicios
sin redeploy vía `POST /actuator/busrefresh`.

Reglas:

1. **Cero secretos en el repo de config** (aunque sea privado). Solo config no sensible:
   URLs, timeouts, ventanas, flags. Secretos → variables de entorno (local/compose) y
   Secrets de Kubernetes (k8s). Mismo principio que una bóveda de credenciales corporativa.
2. El historial de Git funge como auditoría de cambios de configuración.

## Consecuencias

- (+) Refresh dinámico demostrable; auditoría de cambios gratis; un solo mecanismo para
  compose y K8s.
- (−) El config-server es un punto de dependencia al arranque; se mitiga con
  `spring.config.import: optional:configserver:` + valores default embebidos para local.
- Alternativa K8s-nativa (ConfigMaps) documentada y descartada por las razones de arriba;
  reconsiderar si el sistema viviera solo en K8s.
