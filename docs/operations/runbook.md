# Production Runbook (v1)

## Release gate

Run the automated release gate before tagging a release candidate:

```powershell
./scripts/release-gate.ps1
```

Evidence is written to `artifacts/release-gate/<timestamp>/`.

## Local production profile

Build backend JAR first, then start the prod compose profile:

```powershell
mvn -B -ntp -f backend/pom.xml package -DskipTests
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d --build
```

- Backend health: `http://localhost:8080/healthz`
- Frontend: `http://localhost:4173`
- Prometheus metrics (prod profile): `http://localhost:8080/actuator/prometheus`

## Observability

- **Structured logs:** `prod` Spring profile emits JSON logs via Logstash encoder (`logback-spring.xml`).
- **Trace propagation:** `X-Trace-Id` request header is echoed on responses and bound to MDC `traceId` for log correlation.
- **Metrics:** Actuator exposes `health`, `info`, `metrics`, and `prometheus` in prod profile.

## Required environment variables (production)

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Management JWT signing (min 32 bytes) |
| `POSTGRES_*` | Database connection |
| `MINIO_*` | Object storage |
| `APP_ENVIRONMENT` | Runtime environment label |

## Rollback

1. Stop prod profile containers: `docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod down`
2. Deploy previous image tag.
3. Flyway migrations are forward-only; rollback requires a new migration if schema changed.
