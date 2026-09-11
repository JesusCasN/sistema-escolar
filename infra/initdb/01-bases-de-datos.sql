-- Una base por servicio (dueño exclusivo, ver ADR-001).
-- En local todas viven en la misma instancia de Postgres por simplicidad.
--
-- Este directorio se monta completo en /docker-entrypoint-initdb.d y Postgres
-- ejecuta los scripts en orden alfabetico, solo la PRIMERA vez que inicializa
-- el volumen de datos. Si cambias algo aqui, hay que recrear el volumen:
--   docker compose down -v && docker compose up -d

CREATE DATABASE identity;
CREATE DATABASE messaging;
CREATE DATABASE moderation;
CREATE DATABASE academics;
CREATE DATABASE payments;

-- pgvector solo donde se usa (RAG y embeddings del servicio de moderacion)
\connect moderation
CREATE EXTENSION IF NOT EXISTS vector;
