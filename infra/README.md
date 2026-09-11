# Infraestructura local

Levanta todo lo que los servicios necesitan para correr en tu máquina:

```bash
docker compose up -d
docker compose ps        # espera a que todos digan (healthy)
```

| Servicio | Puerto en el host | Notas |
|---|---|---|
| PostgreSQL (con pgvector) | **5433** | Una base por servicio; ver `initdb/` |
| Kafka (KRaft, sin ZooKeeper) | 9092 | |
| Redis | 6379 | |
| MinIO | 9000 (API) · 9001 (consola) | Usuario y contraseña: `escuela` / `escuela-local` |
| Ollama | 11434 | Tras el primer arranque: `docker exec se-ollama ollama pull llama3.2:3b` |

Las credenciales de este archivo son **solo para desarrollo local**. En staging y
producción la configuración sensible va por variables de entorno y Secrets de
Kubernetes, nunca en el repositorio (ver ADR-003).

## Bases de datos

Los scripts de `initdb/` se ejecutan **una sola vez**, cuando el volumen de datos se
inicializa por primera vez. Si los modificas, hay que recrear el volumen:

```bash
docker compose down -v && docker compose up -d
```

Verifica que las bases existan:

```bash
docker exec se-postgres psql -U escuela -d escuela -c "\l"
```

## Problemas conocidos del entorno

Estos tres nos costaron tiempo durante el desarrollo. Si clonas el proyecto y algo
no arranca, empieza por aquí.

### PostgreSQL en el puerto 5433, no en el 5432

Muchas máquinas de desarrollo tienen PostgreSQL instalado de forma nativa ocupando el
5432. En Windows el conflicto es traicionero: el servicio nativo escucha en
`127.0.0.1:5432` y Docker en `0.0.0.0:5432`, así que **ambos conviven sin que Docker
reporte error**, pero las conexiones a `localhost` llegan al nativo. El síntoma es
`FATAL: password authentication failed for user "escuela"` — un servidor contestó,
solo que no es el nuestro.

Por eso el contenedor se publica en el **5433**. Para comprobar qué hay en cada puerto:

```powershell
Get-Service | Where-Object {$_.Name -like "*postgre*"}   # Windows
netstat -ano | findstr :5432
```

### Bind mounts de archivos sueltos en Docker Desktop para Windows

Montar un archivo individual (`./init-db.sql:/ruta/init-db.sql`) falla de forma
intermitente con `mkdir /run/desktop/mnt/host/d: file exists`. Por eso se monta el
**directorio** `initdb/` completo, que además es la forma convencional de usar
`docker-entrypoint-initdb.d`.

Si el error persiste, reinicia Docker Desktop; si aún así, cierra Docker y ejecuta
`wsl --shutdown` antes de volver a abrirlo.

### Imágenes de Bitnami

El catálogo gratuito de Bitnami salió de Docker Hub en agosto de 2025, así que
`bitnami/kafka` ya no resuelve. Se usa la imagen oficial `apache/kafka` en modo KRaft.
Ojo al migrar: las variables de Bitnami llevaban el prefijo `KAFKA_CFG_` y las de la
imagen oficial **no**.
