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

### Docker-only validation (required for manual testing)

Compile on your machine (Maven / pnpm use local caches), run in Docker:

```powershell
copy .env.example .env   # if .env does not exist
.\scripts\docker-deploy.ps1
```

This runs local `mvn package` + `pnpm build`, then builds slim images that **only copy**
`backend/target/*.jar` and `frontend/dist` — no dependency download inside Docker build.

| Service | URL |
| --- | --- |
| Management UI | http://localhost:4173 |
| Backend health | http://localhost:8080/healthz |
| Login | `10000001` / `ChangeMe123!` |

Restart without recompiling: `.\scripts\docker-deploy.ps1 -SkipBuild`.

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

### 3. Backend (optional local dev only)

```powershell
cd backend
mvn -B -ntp verify
mvn spring-boot:run
```

Health: `http://localhost:8080/healthz` and `http://localhost:8080/readyz`

Optional runtime integration (see `.env.example`):

- `IDEMPOTENCY_CACHE=redis` — Redis + DB dual-write (default for non-test profiles)
- `ASYNC_TRANSPORT=kafka` — publish async batch tasks to Kafka (`generation.async-batch-task.v1`); default is in-process `@Async`

### 4. Frontend (optional local dev only)

```powershell
cd frontend
pnpm install
pnpm dev
```

App: `http://localhost:5173` — **not** used for acceptance testing; use Docker UI on port 4173.

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

Kubernetes deployment (P15): [deploy/README.md](deploy/README.md) — chart validate via
`.\scripts\helm-validate.ps1`; CI blocking gates via `.\scripts\ci-k8s-manifest-gates.ps1`
([deploy/ci-k8s-gates.md](deploy/ci-k8s-gates.md)); health probes in [deploy/k8s-health-probes.md](deploy/k8s-health-probes.md);
blue-green cutover in [deploy/blue-green-runbook.md](deploy/blue-green-runbook.md).

## Active phase

**P14** confirmed large domains **Done** (2026-06-27) — all three vertical slices complete:
**P14-T01** clause/content module lifecycle (T01a–T01e; backend **469**; frontend **224**; architecture re-review **PASS**);
**P14-T02** collaboration to-dos + timeout escalation (T02a–T02d; E2E **3/3**; backend **481**; frontend **235**);
**P14-T03** template export/import (T03a–T03c; OpenAPI contract; E2E **2/2**; backend **481**; frontend **235+**).
**Active phase:** **P21** (**In Progress**). **P21-X01 Done** (2026-06-30) — full-system L1 terminology sweep; AUD-Q05 resolved; frontend gates green (**511** Vitest). **P21-X06 Done** (2026-06-30) — i18n parity AUD-Q04 (~368 zh-CN keys, `collectLeafKeys` + parity test). **P21-X05 Done** (2026-06-30) — UI/a11y AUD-Q01..Q03 (`--color-primary`, nav/breadcrumb `:focus-visible`, brand wordmark i18n, token cleanup). **P21-X03 Done** (2026-06-30) — permission fail-closed + unified route guard; AUD-P01..P05 + AUD-B04 resolved. **All four role clusters complete** — phase **wrap-up**; next **P21-X02** only.
MVP P0–P11 complete;
**P13** Done (2026-06-23); **P14** Done (2026-06-27); **P15** Done (2026-06-27; T01–T10);
**P17** Done (2026-06-25); **P18** Done (2026-06-28); **P19** Done (2026-06-25);
**P20** Done (2026-06-25). P12 (deferred enhancements) is the non-active catch-all.
Outstanding: external deployment validation (E05-T06), intranet SCA (M9-T02).
