# Resource Catalogue — Docker Compose

Runs the full resource-catalogue stack locally or on a server.

## Services

| Service | Image | Port |
|---------|-------|------|
| `backend` | `docker.madgik.di.uoa.gr/resource-catalogue:<tag>` | 8080 (internal) |
| `postgres` | `pgvector/pgvector:pg17` | internal |
| `elasticsearch` | `docker.elastic.co/elasticsearch/elasticsearch:9.4.1` | internal |
| `redis` | `redis:8` | internal |

All services communicate over the internal `rc-net` bridge network. The application is not exposed externally — place a reverse proxy in front if needed.

## Prerequisites

- Docker with the Compose plugin
- Access to `docker.madgik.di.uoa.gr` (or a locally built image — see below)

## Setup

### 1. Environment variables

```bash
cp .env.example .env
```

Edit `.env` and set passwords for the credentials shared across services:

```
DB_PASSWORD=<password>          # app user password
POSTGRES_PASSWORD=<password>    # postgres superuser password
ES_PASSWORD=<password>
REDIS_PASSWORD=<password>
```

### 2. Application config

```bash
cp config/application.properties.example config/application.properties
```

Edit `config/application.properties` and fill in all deployment-specific settings: OAuth credentials, node identity, admins, redirect URLs, resource ID prefixes, service endpoints, API tokens, etc. The only `${VAR}` placeholders in the file are `${DB_USER}`, `${DB_PASSWORD}`, `${DB_NAME}`, `${REDIS_PASSWORD}`, and `${ES_PASSWORD}` — these are shared with the other compose services and resolved from `.env`.

This file is mounted as a Docker secret and is never exposed as an environment variable.

### 3. PID config (optional)

```bash
cp config/pid.yaml.example config/pid.yaml
```

Edit `config/pid.yaml` to set the PID issuer URL, credentials (`user`, `user-index`, `password`), and cert paths for each resource type. Place the referenced PEM files in `config/pid_certs/<resource-type>/`. The entire `config/` directory is mounted at `/rc/config` inside the container.

If PID support is not needed, leave `pid.yaml` as-is with empty credentials.

## Running

### With the Makefile (from project root)

```bash
# Pull image and start (runs in the foreground)
make compose

# Stop and remove containers
make compose-down
```

### With a locally built image

```bash
# Build image first
make docker-build

# Then start with the local build
docker compose -f compose/compose.yaml up
```

### Directly with Docker Compose (from this directory)

```bash
docker compose up -d
docker compose down
```

### Running the JAR locally (dev workflow)

`make run` (from project root) runs the Spring Boot JAR directly on the host. It sources `compose/.env` first (so `${DB_NAME}`, `${DB_PASSWORD}`, etc. are resolved), then by default loads `compose/config/application.properties` and `compose/config/pid.yaml` as Spring additional-location config. Override with `CONFIG=...` to point at a different set of files.

Start from the example and edit it to suit your local setup:

```bash
cp config/application.properties.example /path/to/local-application.properties
```

The example file uses Docker service names as hostnames (`postgres`, `elasticsearch`, `redis`). These only resolve inside the Docker network — update them to point at your actual running instances:

```properties
registry.datasource.url=jdbc:postgresql://localhost:5432/${DB_NAME}
registry.elasticsearch.uris=http://localhost:9200
spring.data.redis.host=localhost
```

Then run:

```bash
make run CONFIG=file:/path/to/local-application.properties,file:/path/to/local-pid.yaml
```

**If you use the compose stack as your infra (postgres, elasticsearch, redis),** those services are not exposed to the host by default. Add port mappings to the relevant services in `compose.yaml` before starting the stack:

```yaml
postgres:
  ports:
    - "127.0.0.1:5432:5432"

elasticsearch:
  ports:
    - "127.0.0.1:9200:9200"

redis:
  ports:
    - "127.0.0.1:6379:6379"
```

Start only the infra services, then run the JAR separately:

```bash
docker compose -f compose/compose.yaml up -d postgres elasticsearch redis
make run CONFIG=file:/path/to/local-application.properties,file:/path/to/local-pid.yaml
```

## Directory structure

```
compose/
├── compose.yaml
├── .env                                # local credentials (gitignored)
├── .env.example                        # template — copy to .env
├── config/
│   ├── application.properties          # Spring config with real values (gitignored)
│   ├── application.properties.example  # template — copy to application.properties
│   ├── pid.yaml                        # PID issuer config (gitignored)
│   ├── pid.yaml.example                # template — copy to pid.yaml
│   └── pid_certs/                      # PEM certificates for PID auth
└── postgres/
    └── init/
        └── 01-init-db.sh               # Creates app DB user and extensions on first run
```

## Notes

- Postgres data, Elasticsearch indices, and Redis snapshots are persisted in named Docker volumes (`rc-postgres-data`, `rc-elastic-data`, `rc-redis-data`). To reset state: `docker compose down -v`.
- The postgres init script runs only once (on a fresh volume). To re-run it, remove the `rc-postgres-data` volume first.
- `application.properties` is mounted as a Docker secret (file-based, not an environment variable). Its contents are not visible via `docker inspect`.
