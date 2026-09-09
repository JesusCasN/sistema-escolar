# identity

Servicio de identidad: alta de usuarios por invitación (sin auto-registro), activación
con passkey o contraseña, y emisión de tokens OAuth2/OIDC con Spring Authorization Server.

- Spec: [`docs/specs/001-identity.md`](../../docs/specs/001-identity.md)
- Contrato: [`contracts/openapi/identity.yaml`](../../contracts/openapi/identity.yaml)
- Puerto local: **8081** · Base: `identity` (Postgres) · Swagger: `/swagger-ui.html`

## Correr en local

Requiere la infraestructura del repo levantada:

```bash
cd ../../infra && docker compose up -d
cd ../services/identity && mvn spring-boot:run
```

## Probar el flujo de alta de punta a punta

El alta la hace dirección; mientras no exista el Authorization Server, los endpoints de
administración se protegen con autenticación básica (usuario `direccion`, contraseña en
`application.yml`, sobreescribible con `DEV_ADMIN_PASSWORD`).

```bash
# 1. Alta de un maestro
curl -u direccion:cambiar-en-desarrollo -X POST http://localhost:8081/admin/usuarios \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ana Ruiz","correo":"ana@escuela.mx","rol":"MAESTRO"}'

# 2. La liga de invitación aparece en el log del servicio. Copia el token y validalo:
curl http://localhost:8081/invitaciones/{token}

# 3. Activa la cuenta
curl -X POST http://localhost:8081/invitaciones/{token}/aceptar \
  -H "Content-Type: application/json" \
  -d '{"metodo":"PASSWORD","password":"contrasena-larga-123"}'
```

Al activarse se registra `usuario.activado.v1` en la tabla `outbox_event` y el publicador
lo envía a Kafka en la siguiente pasada (cada 5 s por defecto).

## Decisiones de diseño

- **El token de invitación nunca se persiste**: en base de datos vive su hash SHA-256.
- **Transactional outbox**: el evento se guarda en la misma transacción que el cambio de
  estado, así una caída de Kafka no revierte la activación ni pierde el evento. La entrega
  es "al menos una vez", por eso los consumidores deben ser idempotentes usando `eventId`.
- **Contraseñas con Argon2id**; una cuenta activada con passkey no guarda contraseña.
- El endpoint público de validación devuelve solo el primer nombre y el rol: no revela
  correo ni identificador.

## Estado

- [x] Entrega 1 — esqueleto: entidades, migración V1, repositorios, errores RFC 7807
- [x] Entrega 2 — flujo de invitaciones (alta, validación, activación), outbox → Kafka
- [ ] Entrega 3 — Authorization Server, passkeys (WebAuthn), endpoint `/me` y pruebas
      de integración con Testcontainers
