-- SPEC-001: esquema inicial del servicio identity

CREATE TABLE usuario (
    id              UUID PRIMARY KEY,
    school_id       UUID         NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    correo          VARCHAR(254) NOT NULL UNIQUE,
    rol             VARCHAR(20)  NOT NULL CHECK (rol IN ('DIRECCION','MAESTRO','TUTOR','ALUMNO')),
    estado          VARCHAR(20)  NOT NULL CHECK (estado IN ('PENDIENTE','ACTIVO','SUSPENDIDO')),
    password_hash   VARCHAR(255),
    creado_en       TIMESTAMPTZ  NOT NULL,
    actualizado_en  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_usuario_rol_estado ON usuario (rol, estado);

CREATE TABLE usuario_vinculo (
    usuario_id UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo       VARCHAR(20) NOT NULL CHECK (tipo IN ('HIJO','GRUPO')),
    ref_id     UUID        NOT NULL
);

CREATE INDEX idx_usuario_vinculo_usuario ON usuario_vinculo (usuario_id);

CREATE TABLE invitacion (
    id         UUID PRIMARY KEY,
    usuario_id UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_en  TIMESTAMPTZ NOT NULL,
    usada_en   TIMESTAMPTZ,
    invalidada BOOLEAN     NOT NULL DEFAULT FALSE,
    creada_en  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_invitacion_usuario ON invitacion (usuario_id);

-- Patrón transactional outbox (SPEC-001, caso borde "Kafka caído")
CREATE TABLE outbox_event (
    id           UUID PRIMARY KEY,
    type         VARCHAR(100) NOT NULL,
    version      INT          NOT NULL,
    aggregate_id UUID         NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_pendientes ON outbox_event (created_at) WHERE published_at IS NULL;
