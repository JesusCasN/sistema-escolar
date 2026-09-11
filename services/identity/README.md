# identity

Servicio de identidad: alta de usuarios por invitación (sin auto-registro), activación
con passkey o contraseña, y emisión de tokens OAuth2/OIDC con Spring Authorization Server.

- Spec: [`docs/specs/001-identity.md`](../../docs/specs/001-identity.md)
- Contrato: [`contracts/openapi/identity.yaml`](../../contracts/openapi/identity.yaml)
- Eventos: [`contracts/events/`](../../contracts/events/)
- Puerto local: **8081** · Base: `identity` (Postgres en el **5433**) · Swagger: `/swagger-ui.html`

## Correr en local

Requiere la infraestructura del repo levantada:

```bash
cd infra && docker compose up -d
cd .. && ./mvnw -pl services/identity spring-boot:run
```

El wrapper vive en la raíz del repositorio, así que no hace falta tener Maven instalado.

## Probar el flujo completo

La forma rápida es la colección de Postman:
[`docs/postman/identity.postman_collection.json`](../../docs/postman/identity.postman_collection.json).
Trae los 6 endpoints implementados, sus casos de error y las dos pruebas de seguridad del
flujo de invitaciones.

El alta la hace dirección. Mientras no exista el Authorization Server, los endpoints de
administración se protegen con autenticación básica: usuario `direccion`, contraseña
`cambiar-en-desarrollo`. En el `application.yml` no está en claro, está su hash Argon2id;
sirve únicamente contra `localhost` y desaparece con la Entrega 3.

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
lo envía a Kafka en la siguiente pasada (cada 5 s por defecto). Para comprobarlo:

```bash
docker exec se-postgres psql -U escuela -d identity \
  -c "select type, count(*) total, count(published_at) publicados from outbox_event group by type;"
```

## Pruebas

```bash
./mvnw -pl services/identity test      # unitarios, sin Docker
./mvnw -pl services/identity verify    # + integración con Testcontainers
```

Las de integración (`*IT`) levantan Postgres y Kafka en contenedores, con las mismas
imágenes de `infra/docker-compose.yml`, y no necesitan que la infraestructura local esté
arriba: Testcontainers publica puertos aleatorios. Solo hace falta que Docker esté
corriendo.

`OutboxKafkaIT` consume el mensaje real del tópico y lo valida contra
[`contracts/events/usuario.activado.v1.schema.json`](../../contracts/events/usuario.activado.v1.schema.json),
el archivo del contrato y no una copia. El esquema declara `additionalProperties: false`,
así que un campo de más en el payload rompe la prueba.

## Decisiones de diseño

- **El token de invitación nunca se persiste**: en base de datos vive su hash SHA-256.
- **Transactional outbox**: el evento se guarda en la misma transacción que el cambio de
  estado, así una caída de Kafka no revierte la activación ni pierde el evento. La entrega
  es "al menos una vez", por eso los consumidores deben ser idempotentes usando `eventId`.
- **Los eventos son records tipados** en el paquete `event/`, no mapas: cada uno aporta su
  propio tipo y versión, y corresponde 1:1 con su esquema en `contracts/events`. Si el
  esquema cambia y el record no, el compilador lo detecta.
- **Publicar es infraestructura**: el publicador del outbox vive en `messaging/publisher`,
  no en `service/impl`. En `service` solo hay reglas de negocio.
- **Contraseñas con Argon2id**; una cuenta activada con passkey no guarda contraseña.
- El endpoint público de validación devuelve solo el primer nombre y el rol: no revela
  correo ni identificador.

## Estado

- [x] Entrega 1 — esqueleto: entidades, migración V1, repositorios, errores RFC 7807
- [x] Entrega 2 — flujo de invitaciones (alta, validación, activación), outbox → Kafka
- Entrega 3:
  - [x] Pruebas de integración con Testcontainers del flujo existente
  - [ ] Authorization Server (OAuth2/OIDC)
  - [ ] Passkeys (WebAuthn)
  - [ ] Endpoint `/me`
