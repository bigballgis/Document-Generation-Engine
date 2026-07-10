# Document Generation Platform

Enterprise low-code document generation for bank correspondence.
Plan history and live programs: [docs/plan/master-plan.md](docs/plan/master-plan.md),
[docs/plan/execution-sync-ledger.md](docs/plan/execution-sync-ledger.md).
Active/new tasks: [`.taskmaster/tasks/tasks.json`](.taskmaster/tasks/tasks.json) (ADR-0053).

## Repository layout

```text
backend/          Java 21 + Spring Boot 3 (Maven)
frontend/         Vue 3 + TypeScript + Vite + Element Plus
docs/             Requirements, ADRs, OpenAPI v1, plan layer
.cursor/          Cursor agents, skills, rules, hooks (AI delivery system)
AGENTS.md         Short index of the agent system
docker-compose.yml Local PostgreSQL, Redis, Kafka, MinIO
```

## AI agent delivery system

This repo is built for **supervisor-mode** AI development: you stay in **one main Cursor
chat**; the parent agent routes work through specialists via the Task tool. Do not open a
new chat per pipeline stage.

| Resource | Path |
| --- | --- |
| Index | [AGENTS.md](AGENTS.md) |
| Agents (17) | [`.cursor/agents/`](.cursor/agents/) |
| Model pins | [`.cursor/agents/MODEL-STRATEGY.md`](.cursor/agents/MODEL-STRATEGY.md) |
| Pipeline stages 0–13 | [`.cursor/skills/delivery-pipeline/SKILL.md`](.cursor/skills/delivery-pipeline/SKILL.md) |
| Routing rules | [`.cursor/rules/subagent-routing-mandate.mdc`](.cursor/rules/subagent-routing-mandate.mdc) |
| Orchestration constitution | [`.cursor/rules/delivery-orchestration-constitution.mdc`](.cursor/rules/delivery-orchestration-constitution.mdc) |
| MCP (Cursor) | [`.cursor/mcp.json`](.cursor/mcp.json) |
| Slash commands | [`.cursor/commands/`](.cursor/commands/) (`/deliver`, `/multitask-slices`, `/deploy-queue`, `/verify-done`) |
| Native parallel | [`.cursor/skills/cursor-native-parallel/SKILL.md`](.cursor/skills/cursor-native-parallel/SKILL.md) |

### How to use it

1. In the **main** Cursor Agent chat, state the goal in natural language (Chinese or English),
   or name a Task Master / plan task id. **You do not need to type slash commands.**
2. The parent auto-maps intent and runs the matching workflow:
   - delivery / 做功能 / 修 bug → `delivery-orchestrator` (full pipeline)
   - parallel / 并行切片 → worktree-router + capped writers + deploy queue
   - deploy / 部署 / 队列 → `build-deploy-agent` + `docker-deploy-queue.ps1`
   - verify / 验收 / 是否 Done → `verifier`
3. You review summaries and unblock only when asked (unclear BDD, ADR conflicts,
   `no-commit` / secrets, etc.).

Optional shortcuts (same workflows): `/deliver`, `/multitask-slices`, `/deploy-queue`,
`/verify-done` — see [`.cursor/commands/`](.cursor/commands/).

### Canonical pipeline (0–13)

```text
0  worktree-router          → MAIN vs isolated worktree
1  behavior-spec-author     → BDD (skip only if not-applicable)
2  plan-orchestrator        → plan phase + Task Master
3  doc-keeper               → docs-first when SoT changes
4  engineers                → backend | frontend | rendering (TDD)
5  build-deploy-agent       → queued stack prep for E2E (:4173 / :8080)
6  e2e-test-engineer        → Playwright functional (frontend)
7  e2e-uiux-reviewer        → UIUX evidence (frontend)
8  architecture-reviewer    → boundaries / ADR / fail-closed
9  code-quality-reviewer    → optional hygiene
10 build-deploy-agent       → queued deploy evidence
11 integration-merger       → merge + remove worktree (if isolated)
12 post-task-doc-sync       → plan / ledger / Task Master (on main)
13 post-task-commit-review  → review → commit → push (honor no-commit / no-push)
14 verifier (optional)      → independent PASS/FAIL before handoff
```

### Model policy (no `inherit`)

**All project specialists pin `grok-4.5-fast-xhigh`.** Role tiers (Governance / Delivery / Execution) describe pipeline responsibility only — not different models. See [`.cursor/agents/MODEL-STRATEGY.md`](.cursor/agents/MODEL-STRATEGY.md).

Built-in Cursor types (no project file): `explore`, `bugbot`.

### Parallel work & Docker

- Prefer Cursor **`/multitask`** + **`/worktree`** (or Agents Window) for parallel writers; still run `worktree-router` for naming (`../DGE-<slice>`, `feat/<slice>`).
- Cap concurrent writers (≤3). After isolated green → `integration-merger`, then doc-sync/commit on **main**.
- This machine has **one** Docker acceptance stack — always use the deploy queue (or `/deploy-queue`):

```powershell
.\scripts\docker-deploy-queue.ps1 -Status
.\scripts\docker-deploy-queue.ps1
.\scripts\docker-deploy-queue.ps1 -SkipBuild
```

### MCP (project)

Configured in [`.cursor/mcp.json`](.cursor/mcp.json):

| Server | Use |
| --- | --- |
| `task-master-ai` | Active task list (core tools) |
| `docgen-postgres` | Local Docker Postgres (dev only; compose must be up) |
| `fetch` | `http://localhost:8080/healthz`, OpenAPI, etc. |

Reload the Cursor window after changing agents or MCP config.

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

Compile on your machine (Maven / pnpm use local caches), run in Docker. Prefer the
**queue** wrapper on this single Docker host:

```powershell
copy .env.example .env   # if .env does not exist
.\scripts\docker-deploy-queue.ps1
```

This acquires a deploy mutex, then runs local `mvn package` + `pnpm build`, then builds
slim images that **only copy** `backend/target/*.jar` and `frontend/dist` — no dependency
download inside Docker build.

| Service | URL |
| --- | --- |
| Management UI | http://localhost:4173 |
| Backend health | http://localhost:8080/healthz |
| PDF conversion | LibreOffice headless (embedded in `docgen-backend` image) |
| Login | `10000001` / `ChangeMe123!` |

Restart without recompiling: `.\scripts\docker-deploy-queue.ps1 -SkipBuild`.  
Queue status: `.\scripts\docker-deploy-queue.ps1 -Status`.

The backend image includes LibreOffice for DOCX→PDF acceptance testing (`LIBREOFFICE_CONVERSION_MODE=cli`).
No separate LibreOffice sidecar is required for the queued deploy.

### 1. Environment

```powershell
copy .env.example .env
```

### 2. Local dependencies (Docker)

```powershell
docker compose up -d docgen-postgres docgen-redis docgen-kafka docgen-minio
```

Optional LibreOffice sidecar (split deployment / `docker-exec` mode only — not used by `docker-deploy.ps1`):

```powershell
docker compose --profile rendering up -d docgen-libreoffice
# Set LIBREOFFICE_CONVERSION_MODE=docker-exec and mount Docker socket into backend.
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
AI delivery system: [AGENTS.md](AGENTS.md) and [`.cursor/agents/`](.cursor/agents/).

Kubernetes deployment (P15): [deploy/README.md](deploy/README.md) — chart validate via
`.\scripts\helm-validate.ps1`; CI blocking gates via `.\scripts\ci-k8s-manifest-gates.ps1`
([deploy/ci-k8s-gates.md](deploy/ci-k8s-gates.md)); health probes in [deploy/k8s-health-probes.md](deploy/k8s-health-probes.md);
blue-green cutover in [deploy/blue-green-runbook.md](deploy/blue-green-runbook.md).

## Active phase

**P14** confirmed large domains **Done** (2026-06-27) — all three vertical slices complete:
**P14-T01** clause/content module lifecycle (T01a–T01e; backend **469**; frontend **224**; architecture re-review **PASS**);
**P14-T02** collaboration to-dos + timeout escalation (T02a–T02d; E2E **3/3**; backend **481**; frontend **235**);
**P14-T03** template export/import (T03a–T03c; OpenAPI contract; E2E **2/2**; backend **481**; frontend **235+**).
**Active formal phase:** **None** (2026-07-09+). **CORE-FORTRESS program Done** (F1–F8; 2026-07-09). **CODE-QUALITY Done** (2026-07-09). **P23 Done** (2026-07-08). **Delivery focus note (2026-07-10):** **LR-C9 → Done** (`lrp-c9-load-error-panel`; merge `0013615`; unified list states / role-aware empty CTAs) — no LRP slice currently `In Progress`; see [docs/plan/detail/LRP-C-usability-deepening.md](docs/plan/detail/LRP-C-usability-deepening.md). **Prior:** Wave LR-A **Done** (A1–A7; merge `cc9e5f6`; **ADR-0041 Accepted**; Word/XSD/LO24 deferred). CDP Wave CD-2 **In Progress** (**partial** — CD-E2E-T01/T01b **Done**; merge `1930842`). **In-flight programs (not formal phases):** **CDP** CD-2; **LRP** (Wave LR-C partial — C1/C4/C9 Done).
MVP P0–P11 complete;
**P13** Done (2026-06-23); **P14** Done (2026-06-27); **P15** Done (2026-06-27; T01–T10);
**P17** Done (2026-06-25); **P18** Done (2026-06-28); **P19** Done (2026-06-25);
**P20** Done (2026-06-25); **P21** Done (2026-06-30). P12 (deferred enhancements) is the non-active catch-all.
Outstanding: external deployment validation (E05-T06), intranet SCA (M9-T02).
