# Document Generation Platform

Enterprise low-code document generation for bank correspondence. **P0–P11 Done**
(re-earned 2026-06-23). See [docs/plan/master-plan.md](docs/plan/master-plan.md)
and [docs/plan/execution-sync-ledger.md](docs/plan/execution-sync-ledger.md).

## Repository layout

```text
backend/          Java 21 + Spring Boot 3 (Maven)
frontend/         Vue 3 + TypeScript + Vite + Element Plus
docs/             Requirements, ADRs, OpenAPI v1, plan layer
docker-compose.yml Local PostgreSQL, Redis, Kafka, MinIO
```

## Prerequisites

| Tool | Version |
| --- | --- |
| JDK | 21 (compile target; newer JDK OK) |
| Maven | 3.9+ |
| Node.js | 20+ |
| pnpm | 9+ (`corepack enable` then `corepack prepare pnpm@9.15.0 --activate`) |
| Docker Desktop | For local dependencies (optional until integration tests) |

## Quick start

### 1. Environment

```powershell
copy .env.example .env
```

### 2. Local dependencies (Docker)

```powershell
docker compose up -d docgen-postgres docgen-redis docgen-kafka docgen-minio
```

LibreOffice sidecar (PDF rendering, later phases):

```powershell
docker compose --profile rendering up -d docgen-libreoffice
```

### 3. Backend

```powershell
cd backend
mvn -B -ntp verify
mvn spring-boot:run
```

Health: `http://localhost:8080/healthz` and `http://localhost:8080/readyz`

Optional runtime integration (see `.env.example`):

- `IDEMPOTENCY_CACHE=redis` — Redis + DB dual-write (default for non-test profiles)
- `ASYNC_TRANSPORT=kafka` — publish async batch tasks to Kafka (`generation.async-batch-task.v1`); default is in-process `@Async`

### 4. Frontend

```powershell
cd frontend
pnpm install
pnpm dev
```

App: `http://localhost:5173`

### 5. Sign in (P1)

Start backend and frontend, then sign in with a seeded management account:

| Username | Password | Role |
| --- | --- | --- |
| 10000001 | ChangeMe123! | GLOBAL_ADMIN |
| 10000002 | ChangeMe123! | GROUP_ADMIN |
| 10000003 | ChangeMe123! | TEMPLATE_AUTHOR |

Management auth API: `POST /api/management/v1/auth/login`, `GET /api/management/v1/auth/session`,
`POST /api/management/v1/auth/logout`. Details: [P1 plan](docs/plan/detail/P1-login-session.md).

## Quality gates

```powershell
# Backend
mvn -B -ntp -f backend/pom.xml verify

# Frontend
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build
```

## Documentation entry

Start at [docs/README.md](docs/README.md) and [docs/plan/master-plan.md](docs/plan/master-plan.md).

## Active phase

**P12 — Deferred enhancements** (no single active slice). MVP vertical slice P0–P11
is complete with green local quality gates. Outstanding: external deployment validation
(E05-T06), intranet SCA (M9-T02).
