# Agent & AI scaffolding index

Short map for humans and parent agents. Full narrative: [README.md](README.md)
(**AI agent delivery system**). Details under `.cursor/`.

## Supervisor mode (default)

Stay in **one main chat**. Speak the goal in natural language — the parent agent
**auto-maps** intent to deliver / multitask / deploy-queue / verify-done and spawns
`Task` specialists. You do **not** need to type slash commands every time.

| You say (examples) | Parent does |
| --- | --- |
| 「把 F7 做完」「修这个 bug」「按管线交付」「自动执行后续」 | **deliver** → `delivery-orchestrator` (**stage −1 Batch Recommendation** then one leaf). Task flake → **retry** (≤3); still unavailable → **GP/inline under contract**. Forbid GP: `禁止降级` / `no-gp-fallback`. Early GP: `允许降级` / `allow-gp-fallback` |
| 「这两个切片并行」「同时改前后端」 | **Refuse fan-out by default** → serial queue. Only `force-parallel` / `强制并行` → legacy multitask (≤2 writers) |
| 「部署一下」「队列状态」「重启栈」 | **deploy-queue** → `build-deploy-agent` |
| 「验收一下」「算不算 Done」 | **verify-done** → `verifier` |

**Default (2026-07-16):** single-lane serial on this Docker host — at most one CE/delivery
leaf In Progress. See CE plan §9.2.

Optional shortcuts: `/deliver` (preferred), `/multitask-slices` (legacy opt-in),
`/deploy-queue`, `/verify-done` under `.cursor/commands/`.

Native Cursor parallel primitives are **opt-in only** — see
`.cursor/skills/cursor-native-parallel/SKILL.md`.

## First read (progressive disclosure)

Keep the live index **thin**. Do **not** dump closed program bodies into context by default.

1. [docs/README.md](docs/README.md) delivery focus + sole-active
2. Active Task Master queue head (`.taskmaster/tasks/tasks.json`)
3. [module-map.md](docs/architecture/module-map.md) for package/dir placement (prefer over whole-repo grep)
4. Owning behavior + detail for the **current** leaf only
5. Closed programs → [docs/plan/ARCHIVE.md](docs/plan/ARCHIVE.md) (open detail only when named)

Soft size budgets: [quality-gate-threshold-baseline](docs/architecture/quality-gate-threshold-baseline.md)
+ [ai-scale-docs-conventions](docs/behavior/ai-scale-docs-conventions.md) — prefer peel leaves when exceeded.

## Pipeline (−1, then 0–14)

See `.cursor/skills/delivery-pipeline/SKILL.md` and `delivery-orchestrator`.

- **Stage −1 — Batch Recommendation** (mandatory on deliver): skill
  [delivery-batch-recommend](.cursor/skills/delivery-batch-recommend/SKILL.md);
  behavior [delivery-batch-recommend.md](docs/behavior/delivery-batch-recommend.md).
  Decide `merge` | `solo` | `split` from repo facts so related work shares **one**
  worktree / one evidence run. **Not** multi-writer parallel.
- **Delivery lane (`full` | `light`):** skill
  [lightweight-delivery-lane](.cursor/skills/lightweight-delivery-lane/SKILL.md);
  behavior [lightweight-delivery-lane.md](docs/behavior/lightweight-delivery-lane.md).
  `light` may skip E2E+Docker (stages 5–7, 10) **only** when BDD proves no UI/runtime
  acceptance surface. **Does not** weaken full product leaves; worktree still mandatory.
- **Specialist runtime (retry then GP):** Task flake / missing enum —
  [specialist-runtime-fallback](.cursor/skills/specialist-runtime-fallback/SKILL.md);
  behavior [specialist-runtime-fallback.md](docs/behavior/specialist-runtime-fallback.md).
  Retry named specialist (≤3) → **GP/inline under contract**. Forbid: `禁止降级` /
  `no-gp-fallback`. Early opt-in: `允许降级` / `allow-gp-fallback`. Emit `runtime_routing`.
- Stages **0–13** as before; optional stage **14** = `verifier`.

## Agents (18)

**Canonical model (all specialists):** `cursor-grok-4.5-high-fast`  
Tiers below are **pipeline roles only** — model pin is identical. `inherit` forbidden.

| Tier | Agents |
| --- | --- |
| Governance | delivery-orchestrator, plan-orchestrator, architecture-reviewer, code-quality-reviewer (`is_background`), integration-merger, post-task-commit-review |
| Delivery | behavior-spec-author, doc-keeper, backend-engineer, frontend-engineer, rendering-engineer, e2e-*, post-task-doc-sync |
| Execution | worktree-router, build-deploy-agent, deploy-engineer (rollback), **verifier** |

See `.cursor/agents/MODEL-STRATEGY.md`.

## Built-in (Cursor Task — no project `.md`)

Project routing primarily uses:

- `explore` — deep read-only audit
- `bugbot` — defect-oriented review

Other built-ins may appear in the live Task enum (e.g. `generalPurpose`, `shell`,
`cursor-guide`, `ci-investigator`, `security-review`, `best-of-n-runner`). They are
**not** substitutes for the 18 project specialists. See
[MODEL-STRATEGY.md](.cursor/agents/MODEL-STRATEGY.md) and
[specialist-runtime-fallback](.cursor/skills/specialist-runtime-fallback/SKILL.md).

**Accuracy note:** `.cursor/agents/*.md` defines **18** specialists the pipeline names.
Whether Cursor injects those names into the current session’s `Task` enum is runtime —
if missing → retry (≤3) then **GP under contract** (unless `禁止降级`).

## MCP (Cursor)

`.cursor/mcp.json`: `task-master-ai`, `docgen-postgres` (local Docker only), `fetch`.

## Docker

Always queue on this single host:

```powershell
# Windows / pwsh
.\scripts\docker-deploy-queue.ps1
.\scripts\docker-deploy-queue.ps1 -SkipBuild
.\scripts\docker-deploy-queue.ps1 -Status
```

```bash
# Linux (PowerShell Core)
pwsh ./scripts/docker-deploy-queue.ps1
pwsh ./scripts/docker-deploy-queue.ps1 -SkipBuild
pwsh ./scripts/docker-deploy-queue.ps1 -Status
```

Never invent a second compose project or port offsets.

## Cursor Cloud specific instructions

Dev environment for this codebase runs **natively** (no Docker in the Cloud VM). System
deps are baked into the VM snapshot: **Temurin JDK 25** (default `java`), **Maven 3.8.7**,
**PostgreSQL 16**, **Redis 7**. Standard gate/run commands live in `README.md` and
`frontend/package.json`; only the non-obvious cloud caveats are captured here.

- **JDK 25 is required, not 21.** `README.md` prerequisites say JDK 21, but `backend/pom.xml`
  pins `maven.compiler.release=25`. The backend will not compile on JDK 21. Use JDK 25.
- **Start data services each session (they do not auto-start):**
  `sudo service postgresql start` and `sudo service redis-server start`.
  The `docgen` DB + `docgen`/`docgen_local_pwd` role already exist in the snapshot; Flyway
  migrations + seed users (`10000001`…`/ChangeMe123!`) persist in the Postgres data dir.
- **MinIO and Kafka are NOT installed.** Run the backend with `STORAGE_PROVIDER=filesystem`
  so object storage does not require MinIO (default is `minio`). Kafka is optional
  (`ASYNC_TRANSPORT` defaults to `in-process`), so no action needed.
  **LibreOffice is not installed** → DOCX→PDF conversion is unavailable; login, catalog,
  CRUD and DOCX assembly work without it.
- **Run backend (dev):** from `backend/`,
  `STORAGE_PROVIDER=filesystem JWT_SECRET=local-dev-only-change-me-please-32bytes-min mvn -B -ntp -DskipTests spring-boot:run`.
  Serves on `:8080`; health `GET /healthz`, readiness `GET /readyz` (503 until Postgres UP).
- **Run frontend (dev):** from `frontend/`, `pnpm dev` → `http://127.0.0.1:5173`. Vite proxies
  `/api` → `http://localhost:8080` (override via `VITE_BACKEND_URL`). Use `:5173` for dev, not
  the Docker `:4173` acceptance path.
- **Quality gates** (see `README.md` § Quality gates): backend `mvn -B -ntp -f backend/pom.xml verify`
  (heavy — full JaCoCo/veraPDF; the `verapdf`/`libreoffice` gates may need `-Ddocgen.verapdf.skip=true`
  / soffice absent). Frontend `pnpm -C frontend {lint,type-check,test,build}` all pass out of the box.
- The `docker-deploy*.ps1` / `pwsh` queue flows require Docker + PowerShell, which are **not**
  present in the Cloud VM; they are for the single Docker acceptance host only.

