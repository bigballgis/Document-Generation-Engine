# Document Generation Platform



Enterprise low-code document generation for bank correspondence.

Plan history and live programs: [docs/plan/master-plan.md](docs/plan/master-plan.md),

[docs/plan/execution-sync-ledger.md](docs/plan/execution-sync-ledger.md).

Active/new tasks: [.taskmaster/tasks/tasks.json](.taskmaster/tasks/tasks.json) (ADR-0053).



## Repository layout



text

backend/          Java 25 + Spring Boot 4 (Maven)

frontend/         Vue 3 + TypeScript + Vite + Element Plus

docs/             Requirements, ADRs, OpenAPI v1, plan layer

.cursor/          Cursor agents, skills, rules, hooks (AI delivery system)

AGENTS.md         Short index of the agent system

docker-compose.yml Local PostgreSQL, Redis, Kafka, MinIO





## AI agent delivery system



This repo is built for **supervisor-mode** AI development: you stay in **one main Cursor

chat**; the parent agent routes work through specialists via the Task tool. Do not open a

new chat per pipeline stage.



| Resource | Path |

| --- | --- |

| Index | [AGENTS.md](AGENTS.md) |

| Agents (18) | [.cursor/agents/](.cursor/agents/) |

| Model pins | [.cursor/agents/MODEL-STRATEGY.md](.cursor/agents/MODEL-STRATEGY.md) |

| Pipeline stages 0–14 | [.cursor/skills/delivery-pipeline/SKILL.md](.cursor/skills/delivery-pipeline/SKILL.md) |

| Routing rules | [.cursor/rules/subagent-routing-mandate.mdc](.cursor/rules/subagent-routing-mandate.mdc) |

| Orchestration constitution | [.cursor/rules/delivery-orchestration-constitution.mdc](.cursor/rules/delivery-orchestration-constitution.mdc) |

| MCP (Cursor) | [.cursor/mcp.json](.cursor/mcp.json) |

| Slash commands | [.cursor/commands/](.cursor/commands/) (/deliver, /multitask-slices, /deploy-queue, /verify-done) |

| Native parallel | [.cursor/skills/cursor-native-parallel/SKILL.md](.cursor/skills/cursor-native-parallel/SKILL.md) |



### How to use it



1. In the **main** Cursor Agent chat, state the goal in natural language (Chinese or English),

   or name a Task Master / plan task id. **You do not need to type slash commands.**

2. The parent auto-maps intent and runs the matching workflow:

   - delivery / 做功能 / 修 bug → delivery-orchestrator (full pipeline)

   - parallel / 并行切片 → worktree-router + capped writers + deploy queue

   - deploy / 部署 / 队列 → build-deploy-agent + docker-deploy-queue.ps1

   - verify / 验收 / 是否 Done → verifier

3. You review summaries and unblock only when asked (unclear BDD, ADR conflicts,

   no-commit / secrets, etc.).



Optional shortcuts (same workflows): /deliver, /multitask-slices, /deploy-queue,

/verify-done — see [.cursor/commands/](.cursor/commands/).



### Canonical pipeline (0–13)



text

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





### Model policy (no inherit)



**All project specialists pin cursor-grok-4.5-high-fast.** Role tiers (Governance / Delivery / Execution) describe pipeline responsibility only — not different models. See [.cursor/agents/MODEL-STRATEGY.md](.cursor/agents/MODEL-STRATEGY.md).



Built-in Cursor types (no project file): explore, bugbot.



### Parallel work & Docker



- Prefer Cursor **/multitask** + **/worktree** (or Agents Window) for parallel writers; still run worktree-router for naming (../DGE-<slice>, feat/<slice>).

- Cap concurrent writers (≤3). After isolated green → integration-merger, then doc-sync/commit on **main**.

- This machine has **one** Docker acceptance stack — always use the deploy queue (or /deploy-queue):

```powershell
.\scripts\docker-deploy-queue.ps1 -Status
.\scripts\docker-deploy-queue.ps1
.\scripts\docker-deploy-queue.ps1 -SkipBuild
```

Linux / cloud agents:

```bash
pwsh ./scripts/docker-deploy-queue.ps1 -Status
pwsh ./scripts/docker-deploy-queue.ps1
pwsh ./scripts/docker-deploy-queue.ps1 -SkipBuild
```





### MCP (project)



Configured in [.cursor/mcp.json](.cursor/mcp.json):



| Server | Use |

| --- | --- |

| task-master-ai | Active task list (core tools) |

| docgen-postgres | Local Docker Postgres (dev only; compose must be up) |

| fetch | http://localhost:8080/healthz, OpenAPI, etc. |



Reload the Cursor window after changing agents or MCP config.



## Prerequisites



| Tool | Version |

| --- | --- |

| JDK | 21 (compile target; newer JDK OK) |

| Maven | 3.9+ |

| Node.js | 20+ |

| pnpm | 9+ (corepack enable then corepack prepare pnpm@9.15.0 --activate) |

| Docker Desktop | For local dependencies (optional until integration tests) |



## Quick start



### Docker-only validation (required for manual testing)



Compile on your machine (Maven / pnpm use local caches), run in Docker. Prefer the

**queue** wrapper on this single Docker host:



powershell

copy .env.example .env   # if .env does not exist

.\scripts\docker-deploy-queue.ps1





This acquires a deploy mutex, then runs local mvn package + pnpm build, then builds

slim images that **only copy** backend/target/*.jar and frontend/dist — no dependency

download inside Docker build.



| Service | URL |

| --- | --- |

| Management UI | http://localhost:4173 |

| Backend health | http://localhost:8080/healthz |

| PDF conversion | LibreOffice headless (embedded in docgen-backend image) |

| Login | 10000001 / ChangeMe123! |



Restart without recompiling: .\scripts\docker-deploy-queue.ps1 -SkipBuild.  

Queue status: .\scripts\docker-deploy-queue.ps1 -Status.



The backend image includes LibreOffice for DOCX→PDF acceptance testing (LIBREOFFICE_CONVERSION_MODE=cli).

No separate LibreOffice sidecar is required for the queued deploy.



### 1. Environment



powershell

copy .env.example .env





### 2. Local dependencies (Docker)



powershell

docker compose up -d docgen-postgres docgen-redis docgen-kafka docgen-minio





Optional LibreOffice sidecar (split deployment / docker-exec mode only — not used by docker-deploy.ps1):



powershell

docker compose --profile rendering up -d docgen-libreoffice

# Set LIBREOFFICE_CONVERSION_MODE=docker-exec and mount Docker socket into backend.





### 3. Backend (optional local dev only)



powershell

cd backend

mvn -B -ntp verify

mvn spring-boot:run





Health: http://localhost:8080/healthz and http://localhost:8080/readyz



Optional runtime integration (see .env.example):



- IDEMPOTENCY_CACHE=redis — Redis + DB dual-write (default for non-test profiles)

- ASYNC_TRANSPORT=kafka — publish async batch tasks to Kafka (generation.async-batch-task.v1); default is in-process @Async



### 4. Frontend (optional local dev only)



powershell

cd frontend

pnpm install

pnpm dev





App: http://localhost:5173 — **not** used for acceptance testing; use Docker UI on port 4173.



### 5. Sign in (P1)



Start backend and frontend, then sign in with a seeded management account:



| Username | Password | Role |

| --- | --- | --- |

| 10000001 | ChangeMe123! | GLOBAL_ADMIN |

| 10000002 | ChangeMe123! | GROUP_ADMIN |

| 10000003 | ChangeMe123! | TEMPLATE_AUTHOR |



Management auth API: POST /api/management/v1/auth/login, GET /api/management/v1/auth/session,

POST /api/management/v1/auth/logout. Details: [P1 plan](docs/plan/detail/P1-login-session.md).



## Quality gates



powershell

# Backend

mvn -B -ntp -f backend/pom.xml verify



# Frontend

pnpm -C frontend lint

pnpm -C frontend type-check

pnpm -C frontend test

pnpm -C frontend build





## Documentation entry



Start at [docs/README.md](docs/README.md) and [docs/plan/master-plan.md](docs/plan/master-plan.md).

AI delivery system: [AGENTS.md](AGENTS.md) and [.cursor/agents/](.cursor/agents/).



Kubernetes deployment (P15): [deploy/README.md](deploy/README.md) — chart validate via

.\scripts\helm-validate.ps1; CI blocking gates via .\scripts\ci-k8s-manifest-gates.ps1

([deploy/ci-k8s-gates.md](deploy/ci-k8s-gates.md)); health probes in [deploy/k8s-health-probes.md](deploy/k8s-health-probes.md);

blue-green cutover in [deploy/blue-green-runbook.md](deploy/blue-green-runbook.md).



## Active phase



**P14** confirmed large domains **Done** (2026-06-27) — all three vertical slices complete:

**P14-T01** clause/content module lifecycle (T01a–T01e; backend **469**; frontend **224**; architecture re-review **PASS**);

**P14-T02** collaboration to-dos + timeout escalation (T02a–T02d; E2E **3/3**; backend **481**; frontend **235**);

**P14-T03** template export/import (T03a–T03c; OpenAPI contract; E2E **2/2**; backend **481**; frontend **235+**).

**Active formal phase:** **None** (2026-07-09+). **CORE-FORTRESS program Done** (F1–F8; 2026-07-09). **CODE-QUALITY Done** (2026-07-09). **P23 Done** (2026-07-08). **Delivery focus note (2026-07-21):** **#147** SYS-NORM Wave 3 (`sys-norm-external-ops`) → **Done** (MAIN merge `18a9e3b2`; feature tip `f21dda5e`; worktree **REMOVED**; Batch **solo** closed; BDD **ready** [sys-norm-external-ops.md](docs/behavior/sys-norm-external-ops.md) **BDD-SYS-NORM-W3-001…018**; Gates: FE lint/type-check/test/build **PASS** (**1643**); Stage 5+10 **DEPLOY_OK**; E2E **10/10**; UIUX **PASS**; arch **PASS_WITH_FINDINGS** `merge_go=yes`; **sole-active cleared**; next Wave 4 `sys-norm-test-artifacts` **Not Started** — not activated; do **not** flip **#3b/#5a GO**; CE-O02 Deferred; no RTL; do **not** claim go-live / SYS-NORM program Done). Prior **#146** SYS-NORM Wave 2 → **Done** (`5d77db80` / `992f6822`). Prior **#145** SYS-NORM Wave 1 → **Done** (`7a62be44` / `f1594f2a` / `ce2cb9f0`; N18 deferred). Prior **#144** Published template test artifacts → **Done** (`ac36ecbc` / `6bc74ff1`). Prior **#143** SYS-NORM Wave 0 (`sys-norm-charter`) → **Done** (MAIN merge `f8e898ad`; feature `28d4abe1`; worktree **REMOVED**; Batch **split** closed; Wave 0 complete — program SoT + ADR-0070/0071 **Accepted**; Gates: docs-only **N/A**; do **not** mark **#53** Done). Prior **#142** Demo expand Wave B (`bank-letter-demo-expand`) → **Done** (MAIN merge `288ce98f`; worktree **REMOVED**; Batch **solo** closed; Wave B complete — **7** families; registry **20/20**; generate **20/20**; Gates: `mvn verify` **GREEN 2340**; docker **DEPLOY_OK**; arch **merge_go**; FE E2E **N/A**; vitest baseline **RED** unrelated; evidence `docs/plan/evidence/bank-letter-demo-expand/`; do **not** reopen P22/P23; do **not** flip **#3b/#5a GO**; CE-O02 Deferred; no RTL; do **not** claim go-live). Prior **#141** Demo refresh Wave A (`bank-letter-demo-refresh`) → **Done** (MAIN merge `aa88170f`; feature tip `5ae9575a`; worktree **REMOVED**; Batch **split** closed; Wave A complete — import/publish/generate **13/13**; same-release publish supersede + `DEACTIVATE_VERSION` audit; Gates: `mvn verify` **GREEN 2312**; docker **DEPLOY_OK**; arch **PASS_WITH_NOTES** `merge_go=true`; FE/E2E **N/A**; evidence `docs/plan/evidence/bank-letter-demo-refresh/`). Prior **#140** CD-3 / CD-HARD-T06 → **Done** (MAIN merge `b933965c`; worktree **REMOVED**; Batch **solo** closed; Wave **CD-3 Done** T01–T06; E2E **2/2**; UIUX **PASS_WITH_NOTES**; arch **PASS_WITH_NOTES** `merge_go`; Stage 5 **DEPLOY_OK**; residual **OUT (LR-C8)** AUDIT_ADMIN tour→dashboard Forbidden; **B3 CE-O02** remains product-blocked (`5bc1fe2d` — **not** Done). Prior **#139** PD-7 → **Done** (MAIN FF merge `b966874a`; worktree **REMOVED**; Batch **solo** closed; **OUT of IBL-E**; **pursue docs Done** — [ADR-0069](docs/adr/rendering-authoring/0069-licensed-font-embedding-pursue.md) + [procurement checklist](docs/operations/licensed-font-embedding-procurement.md); BDD **not-applicable** [pd7-licensed-font-embedding.md](docs/behavior/pd7-licensed-font-embedding.md); Gates: docs-only; FE/E2E/UIUX **N/A**; **Accepted ≠ embedding Done** — still cannot ship without licensed assets; **sole-active cleared**; **#119** remains **Blocked**; formal phase **None**; do **not** flip **#3b/#5a GO**; do **not** claim embedding / **IBL program Done** / go-live; next recommend **B3 CE-O02**). Prior **#138** PD-6 → **Done** (MAIN FF merge `73ab03cb`; F6 opt-in production re-issue; `mvn verify` **GREEN 2309**). Prior **#134** IBL-E7 → **Done** (MAIN merge `37239d68` / feature tip `68abc7c3`; worktree **REMOVED**; Batch **solo** closed; **F15 closed by descope**; ADR-0068 **Accepted**; spike **DESCOPE**; inventory probe **GREEN**; architecture **merge_go**; BDD **not-applicable**; `frontend_ui_in_scope=false`; **Wave IBL-E → Done** (E1–E7 all Done)). Prior **#132** IBL-E5 → **Done** (MAIN merge `688f9e58` / feature tip `20ead1ce`; worktree removed; F27 closed). Prior **#131** IBL-E4 → **Done** (MAIN merge `4d810395` / feature tip `212c6be9`; F27 document-brand half closed). Prior **#130** IBL-E3 → **Done** (MAIN merge `233342d3` / feature tip `e81a6bac`; F26 closed). Prior **#129** IBL-E2 → **Done** (MAIN merge 81a1ca29 / 6a96e9ab; F25 closed). Prior **#128** IBL-E1 → **Done** (MAIN merge `4c712b03` / feature tip `9decf19c`; worktree removed; F24 closed; ADR-0062 + locale-variant model/UI; Gates: `mvn verify` **GREEN 2202**; FE **1571**/build **GREEN**; E2E **4/4**; UIUX **PASS** Critical=0; arch `merge_go`; Stage 5+10 **DEPLOY_OK**). Prior Out-of-band FE bug leaf `nav-missing-icons` → **Done** (MAIN merge `137a115f` / feature tip `47784667`; worktree removed; shell nav icons for asset-library / legal-holds; FE+E2E+UIUX+arch+Stage 5/10 green). Prior **#127** IBL-D5 → **Done** (MAIN merge `6f672271` / feature tip `2e56787e`; worktree removed; F23 legalhold half closed — create/enforce/block depth: 61 legalhold tests / +44 vs baseline 17; Playwright 9/162 residual **OUT**; BDD **not-applicable**; Batch **solo** closed; Gates: `mvn verify` **GREEN 2188**; arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 SkipBuild health **DEPLOY_OK**; FE/E2E **N/A**; **Wave IBL-D → Done** D1–D5 all Done; Wave **IBL-C** remains **Done** C1+C2+C3; Wave **IBL-B** stays **In Progress** B1–B6 Done / B7 Blocked residual — skip B7; do **not** claim Wave B / IBL program Done; Wave **IBL-A** remains **Done**; **sole-active cleared**; next queue residual (not activated) **#119** IBL-B7 still **Blocked** **OR** Wave E **BLOCKED** (PD-*) — do **not** activate; do **not** invent Word evidence; formal phase **None**; do **not** flip **#3b/#5a GO**; do **not** invent Word/pixel; do **not** claim go-live. Prior **#124** IBL-D2 → **Done** (MAIN merge `21be3a99` / feature tip `4fd0c5da`; worktree removed; F21 closed — opt-in `-Plibreoffice-ci` fail-not-skip when `soffice` absent; default optional LO skip retained; BDD **not-applicable**; default `mvn verify` **GREEN 2138**/14 skipped; `-Plibreoffice-ci` fail-closed without soffice (**expected FAIL**); arch **PASS_WITH_NOTES** (after excluding demo docx); Stage 10 ForceRebuild **DEPLOY_OK**; FE/E2E **N/A**; **Wave IBL-D stays In Progress** D1+D2 Done / D3–D5 Not Started — do **not** mark Wave D Done; sole-active cleared; next **#125** IBL-D3 not activated; residual: company CI wiring `-Plibreoffice-ci` may remain; soffice-positive GREEN not proven on this host); Prior **#123** IBL-D1 → **Done** (MAIN merge `1a686938` / feature tip `f399489c`; worktree removed; F20 closed — opt-in TC+Flyway lane; default H2 retained; BDD **not-applicable**; default `mvn verify` **GREEN 2133**; `-Ptestcontainers,dev-fast test` **GREEN** (Flyway **68** on PG16); arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 ForceRebuild **DEPLOY_OK**; FE/E2E **N/A**);; Prior **#122** IBL-C3 → **Done** (MAIN merge `bdfc285d` / feature tip `dbfff086`; F19 closed — en/zh + multi-currency themes honest **SYNTHETIC**; LO residual → **IBL-D2** / **F21**; **Wave IBL-C → Done** C1+C2+C3); Prior **#121** IBL-C2 → **Done** (MAIN merge `1d357e4d` / includes `767b4ceb`+`abf564dc`; worktree removed; F18); Prior **#120** IBL-C1 → **Done** (MAIN merge `55909dd2` / includes `d2492fc4`+`5d89b606`; prior tip `c77418aa`; worktree removed; F17 layout-metric closed — `PAGE_COUNT`/`TEXT_POSITION`; `PIXEL_*` still rejected; LO **SKIPPED** honestly; `mvn verify` **2125** GREEN; arch **PASS_WITH_NOTES**; ForceRebuild **DEPLOY_OK**; FE/E2E **N/A**); Prior **#118** IBL-B6 → **Done** (MAIN merge `8722f4f1` / includes `8e8c62e6` freeze docs; also brought previously local `e2f0a505` B5 docs; prior remote tip `1666312b`; worktree removed; F16 closed — ADR-0060 + ops freeze; docs-only; arch **PASS_WITH_NOTES** `merge_go=true`; deploy/FE/E2E **N/A**); Prior **#117** IBL-B5 → **Done** (`1666312b` / `d7459405`+`fbb40429`); Wave **IBL-C** → **Done** (C1+C2+C3; F19 honesty SYNTHETIC; LO residual → D2/F21); Wave **IBL-B** → **In Progress** (B1–B6 Done; B7 Blocked; sole-active cleared); Prior **#116** IBL-B4 → **Done** (`610eb0fa` / `d6b389d1`+`d2e8a1c9`); Prior **#115** IBL-B3 → **Done** (`3710811a` / `c81054b0`+`e0102ddb`); Wave **IBL-A** remains **Done** (A1–A6); Program IBL stays registered; prior **#114** IBL-B2 → **Done** (`29d022b6` / `3dd1aa60`+`36a9821c`; F11 / DEF-LRP-D6-001 **CLOSED**); do **not** flip **#3b/#5a GO**; do **not** claim go-live / IBL program Done; prior **#113** IBL-B1 → **Done** (`a33da272` / `44237c99`); prior **#111** IBL-A5 → **Done** (`e5217a80` / `99e8c1a7`); prior **#110** IBL-A4 → **Done** (MAIN merge `ddf6601c` / feature tip `6edb41cd`; worktree removed; `/contract` per-field schemas + breaking-change gate); prior **#109** IBL-A3 → **Done** (MAIN merge `779b1979` / feature tip `f09326ca`; worktree removed; international `SPELL_AMOUNT` en/USD); prior **#108** IBL-A2 → **Done** (MAIN merge `e3000479` / feature tip `89584242`; worktree removed; ISO-currency `FORMAT_AMOUNT`); prior **#107** IBL-A1 → **Done** (MAIN merge `f0a2b6fe` / feature tip `4bda5f2d`; worktree removed; runtime/preview fail-closed `VARIABLE_VALIDATION_FAILED`); prior **#137** PRR-P2 audit hygiene → **Done** (MAIN merge `baaf16cc` / feature tip `09cf85ce`; worktree removed; listAll removed; `components/template`→`templates`; knip unused exports **31→0**); BDD **not-applicable**; prior **#136** PRR-D01c → **Done** (`a872c15b` / `8c52ee67`); audit Wave D residuals **D01A+#135+#136** closed; prior **#135** D01B → **Done** (`6e776232` / `1ada6b41`); prior **#104** D01A → **Done** (`f1f79d14`); prior **#103** PRR-C01 → **Done** (merge 3513ab92 / tip 7a1a1bb1; Path **X**); ADR-0042/0043 **Accepted**; checklist **#3b** → **CONDITIONAL** (≠ GO); prior **#105** PRR-B02 → **Done** (`50448016`); **#102** PRR-B01 → **Done** (`40e264e7`); **#101** PRR-A04 → **Done** (`5b705f56`); Wave A **#98+#99+#100** → **Done** (`4197770f`); **sole-active delivery leaf:** cleared (prior IBL-D3 **#125** Done `6b9b6487` / `0242084e`; F22 partial; Wave IBL-D In Progress D1–D3 Done / D4–D5 Not Started; next **#126** not activated; **#119** B7 remains Blocked); do **not** flip **#3b GO** / **#5a GO** (#5a stays **CONDITIONAL**); do **not** mark Wave A / IBL program Done. **CORE-EXCELLENCE (CE)** — **#77** CE-G05 → **Done** (merge `c3f6a288` / feature `744b628a`; worktree removed); **P3 queue empty** (#96/#97/#80/#77 all Done); umbrella **#53** remains **in-progress** (program registry only — **do not** claim program Done); **#80** CE-E03 → **Done** (merge `f1f02554` / feature `86e4ff10`; worktree removed); **#97** CE-U19 → **Done** (merge `e4679421` / feature `90a9e5cd`; worktree removed); **#96** CE-U17 → **Done** (merge `4fc2dbdb` / feature `d3293db1`; worktree removed) — do **not** claim program Done; **P2 CE continuum complete** — **#95** CE-U21 → **Done** (merge `8d8f6f6d` / feature `292fbc35`; worktree removed); **#94** CE-U20 → **Done** (merge `b9327a11` / feature `b4b0b420`; worktree removed); **#93** CE-U18 → **Done** (merge `05e9f8e1` / feature `e05407f2`; worktree removed); **#92** CE-U16 → **Done** (merge `5d683c40` / feature `1a3d0f20`; worktree removed); **#91** CE-U15 → **Done** (merge `b2968052`; closeout `ed8a15e6`; worktree removed); **#90** CE-U14 → **Done** (merge `05e845e4` / feature `09e0d251`); **#75** CE-G04 → **Done** (merge tip `42745ea5` / feature `b47ea896`; worktree removed); **#81** CE-O01 → **Done** (merge `e081bcfa`) — do **not** reopen; **#79** CE-E02 → **Done** (merge `5bd3611e`) — do **not** reopen; **#78** CE-E01 → **Done** (merge `6ae57974`); **#76** CE-G06 → **Done** (merge `d8636232`); **#71** CE-C06 → **Done** (merge `35f6f47d`); **#89** CE-U13 → **Done** (merge `ccdfacda`); **#74** CE-G03 → **Done** (merge `50c1a524`); **#62 CE-K06** → **Done** (K06a `485a7f3e` + K06b `a689ca87` + K06c tip `76297d08`); **#69/#70/#88** → **Done** (`c7be8305` / `405f7cea` / `7734366e`); Wave 0 **#61/#86/#87** → **Done**; umbrella **#53** (program registry only); prior waves **#60/#84/#85** and **#59/#68/#83** **Done**; **Batch 4 remains Done** — **#73 CE-G02** (merge `2ea74018`) + **#58 CE-K02** (merge `2f6792eb`); Batch 1–3 Done; formal phase **None**; **not** go-live. **#50** Vitest 3.2.7 → **Done** (merge `6c8fff7d`; GHSA-5xrq-8626-4rwp **CLOSED**; ADR-0029). **Prior:** **BOOT-4-1-UPGRADE → Done** (slice `boot-4-1-upgrade`; merge `993c287`; feature tip `e9bf43c`; Task Master **#51**; **Java 25** + Spring Boot **4.1.0**; ADR-0028 amended; `mvn verify` GREEN 1357/0/0/7; DEPLOY_OK_WITH_NOTES healthz 200). **no sole-active formal phase**. Overall checklist **CONDITIONAL** (**#3b** Path X; **#10** / **#5a**) — **not GO** / **not** go-live. **Prior:** **DEPS-SECURITY-REFRESH → Done** (slice `deps-security-refresh`; merge `08c7d56`; feature tip `cb28237`; Task Master **#49**; Boot **3.3.13** + baseline-safe Maven/pnpm upgrades; Vitest Critical residual closed by **#50**). **Prior:** **SLIM-KNIP-SCAN → Done** (slice `slim-knip-scan`; merge `ea7db64`; feature tip `ea7db64`; Task Master **#48**; Knip ^6.26 tooling + baseline evidence + Wave-1 orphan deletes). **Prior:** **OPS-PASTE-BINDING-SEAM → Done** (slice `ops-paste-binding-seam`; merge `f1f00da`; feature tip `f1f00da`; Task Master **#47**; checklist #5b → **GO**; **CD-HARD-T05 → Done**; ledger seam closed). **Prior:** **OPS-AD-GROUP-STUB-CLOSE → Done** (slice `ops-ad-group-stub-close`; merge `4e51a1b`; feature tip `4e51a1b`; Task Master **#46**; checklist #5a → **CONDITIONAL** residual — real LDAP/AD + company directory evidence still missing; LAB ≠ production AD; **not GO**; **not** a blocking NO-GO). **Prior:** **OPS-KAFKA-COMPANY-REGISTRY → Done** (slice `ops-kafka-company-registry`; merge `e54d03c`; feature tip `e54d03c`; Task Master **#45**; checklist #10 → **CONDITIONAL** residual — operator must supply company-approved `KAFKA_IMAGE`; not GO without company registry evidence; **not** a blocking NO-GO). **Prior:** **OPS-JWT-SECRET → Done** (slice `ops-jwt-secret-no-default`; merge `587cd9a`; feature tip `283233e`; Task Master #44; checklist #9 → **GO**). Prior blocking **#3b** → **CONDITIONAL** (Path X; merge `3513ab92`); overall **CONDITIONAL** — **not** go-live. **Prior:** **LR-E2 → Done** (slice `lrp-e2-launch-checklist`; merge `ae39fbb`; feature tip `57e9133`; Task Master #43; BDD not-applicable; checklist overall **NO-GO**). **Wave LR-E → Done** (E1+E2 docs exit gate — **not** production go-live). **LRP planned waves A–E → Done**. Formal phase remains **None**; **no sole-active**. Do **not** claim production go-live. Do **not** activate CD-3. Do **not** touch `DGE-audit-governance`. Active delivery: sole-active cleared (prior IBL-D3 **#125** Done `6b9b6487` / `0242084e`; F22 partial; Wave IBL-D In Progress D1–D3 Done / D4–D5 Not Started; next **#126** not activated; **#119** B7 Blocked); overall **CONDITIONAL** (#3b Path X; #10/#5a) — **not** go-live. **Prior:** **LR-E1 → Done** (slice `lrp-e1-sse-proxy-e2e`; merge `575d0aa`; Task Master #42; BDD not-applicable; closes CD-PIT-12 browser proof; Playwright 2/2; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; DEPLOY_OK; evidence `frontend/e2e/evidence/LRP-E1-sse-manifest.md`). **Prior:** **Wave LR-D → Done** (D1–D7 all Done; merge tip `218dcf1`). **In-flight programs (not formal phases):** **CDP** (planned waves CD-0/CD-2/CD-3 **Done**; #140 `b933965c`; sole-active cleared; **not** go-live); **LRP** (planned waves A–E **Done** — C1–C13 merge tip `bf9cbeb`; D1–D7 merge tip `218dcf1`; E1+E2 merge tip `ae39fbb`; checklist **CONDITIONAL**).

MVP P0–P11 complete;

**P13** Done (2026-06-23); **P14** Done (2026-06-27); **P15** Done (2026-06-27; T01–T10);

**P17** Done (2026-06-25); **P18** Done (2026-06-28); **P19** Done (2026-06-25);

**P20** Done (2026-06-25); **P21** Done (2026-06-30). P12 (deferred enhancements) is the non-active catch-all.

Outstanding: external deployment validation (E05-T06), intranet SCA (M9-T02).

