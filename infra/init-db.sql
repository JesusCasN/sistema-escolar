-- Una base por servicio (dueño exclusivo, ver ADR-001).
-- En local todas viven en la misma instancia de Postgres por simplicidad.
CREATE DATABASE identity;
CREATE DATABASE messaging;
CREATE DATABASE moderation;
CREATE DATABASE academics;
CREATE DATABASE payments;

-- pgvector solo donde se usa (RAG / embeddings de moderación)
\connect moderation
CREATE EXTENSION IF NOT EXISTS vector;
