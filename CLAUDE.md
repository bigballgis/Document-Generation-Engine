# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Context

**Active formal phase:** **None** (2026-07-09+). **CODE-QUALITY program Done** (CQ-01A…CQ-08; ArchUnit **11/11**; gates **GREEN**). **CORE-FORTRESS program Done** (F1–F8). **LR-A4 Done** (2026-07-10; merge `a523a09`). **CDP CD-E2E-T01/T01b Done** (2026-07-10; merge `1930842`); Wave CD-2 remains **In Progress** (T02–T12 Not Started). **Prior:** **P23-DEMO-TYPOGRAPHY-LAYOUT-EXCELLENCE Done** (2026-07-08). See [docs/plan/execution-sync-ledger.md](docs/plan/execution-sync-ledger.md).

**Task source (2026-07-05):** `.taskmaster/tasks/tasks.json` (task-master-ai v0.43.1) is the source of truth for **new/active work** going forward. `docs/plan/` is the **frozen archive** for P0–P22 history + live record for LRP/CDP/SOR active programs. Project history is fully traceable from zero via `docs/plan/` (P0–P22, 23 phases, 30 detail files, 6 cross-cutting programs) and `.taskmaster/` (new work from 2026-07-05). See ADR-0053.

This is a **dual-agent project** — workflow automation is orchestrated by Cursor rules (`.cursor/rules/*.mdc`, `.cursor/agents/*.md`) and **Claude Code should operate as the parent orchestrator**, delegating to Cursor's specialist subagents via the Task tool.

**Please read ALL guide files under `.cursor/` before starting any delivery work.** Key rules:
- `.cursor/rules/strategic-direction-autonomy-constitution.mdc` — **when direction is clear, proceed autonomously; no end-of-turn confirmation loops**
- `.cursor/rules/subagent-routing-mandate.mdc` — the routing table for parent agents
- `.cursor/rules/delivery-orchestration-constitution.mdc` — the full pipeline order
- `.cursor/rules/worktree-and-deploy-queue-constitution.mdc` — worktree isolation + single Docker queue
- `.cursor/rules/tdd-bdd-delivery-constitution.mdc` — test-first gates
- `.cursor/rules/document-as-code-constitution.mdc` — docs-first behavior updates
- `.cursor/agents/MODEL-STRATEGY.md` — all specialists `grok-4.5-fast-xhigh` (`inherit` forbidden)

## Quick Commands (host compile, Docker runtime)

```powershell
# Backend (Java 21 + Spring Boot 3)
cd backend
mvn -B -ntp -f backend/pom.xml -Pdev-fast test                    # TDD inner loop
mvn -B -ntp -f backend/pom.xml verify                              # Full quality gate (Checkstyle + PMD + SpotBugs + JaCoCo)
mvn -B -ntp -f backend/pom.xml package -Dmaven.test.skip=true     # Compile only (for Docker)
mvn spring-boot:run                                               # Local dev (requires local PostgreSQL)

# Frontend (Vue 3 + TypeScript + Vite)
pnpm -C frontend test --run                                      # TDD inner loop
pnpm -C frontend lint && pnpm -C frontend type-check && pnpm -C frontend test && pnpm -C frontend build  # Full quality gates
pnpm -C frontend build                                           # Compile only (for Docker)

# Both stacks (p0 gate)
.\scripts\p0-gate.ps1                                            # Mechanical debounce for auto mode

# Docker-only validation (required for manual testing) — single host, queued
.\scripts\docker-deploy-queue.ps1                            # Mutex + full compile + deploy
.\scripts\docker-deploy-queue.ps1 -SkipBuild                 # Restart containers only
.\scripts\docker-deploy-queue.ps1 -Status                    # Show lock / queue
curl -f http://localhost:8080/healthz                        # Backend health
curl -f http://localhost:4173                                # Management UI

# E2E docker acceptance (tests at http://localhost:4173)
pnpm -C frontend test:e2e:docker                             # Playwright smoke (8 specs in CI)

# Seed FOL demo
$env:DOCGEN_IMPORT_FOL_DEMO='true'; .\scripts\docker-deploy-queue.ps1
```

**Important:** Manual acceptance testing runs in Docker containers. Compilation happens on the host (local Maven/pnpm using `~/.m2` and `node_modules`), then artifacts are copied into runtime images — **no native Maven/npm inside Docker build.**

## Architecture Overview

### Module Boundaries (backend)

File: `docs/architecture/module-boundaries.md`

| Module | Owns | Does Not Own |
|--------|------|--------------|
| `apimgmt` | API credentials, policy, AD Group authorization | Template content, lifecycle authorization |
| `audit` | Security + lifecycle + generation audit records | Sensitive plaintext, business variable raw values |
| `authoring` | Structured content docx assembly, fidelity engine | API credential secrets |
| `authorization` (management) | Login/session auth, role-based route access | Runtime API authorization |
| `master` | Master DOCX assets, anchors, review state | Template release governance |
| `contentmodule` | Content module lifecycle (T01a–T01e) | Template composition |
| `collaboration` | To-dos + timeout escalation | Template lifecycle |
| `template` | Template structure, variables, rules | API credential lifecycle |
| `runtime` | Public generation API (sync/async/batch), idempotency | Rendering engine internals |
| `rendering` | DOCX → PDF, preview generation | API authorization |
| `infrastructure` | Spring configuration, i18n, resilience, tracing | Business logic |
| `sharedkernel` | Error envelopes, metadata, health, security primitives | Business workflow |

**Rendering is isolated** from lifecycle/authorization/API-governance (`com.bank.docgen.rendering.*`). API management (`apimgmt`) and template composition (`authoring` + `template`) remain separate capabilities.

### Frontend Structure

| Concern | Location |
|---------|----------|
| Views | `src/views/<domain>/` — dashboard, masters, templates, contentModules, api, audit, identity, collaboration |
| Shared components | `src/components/common/` — `AppDataTable`, `AppPageLayout`, `WorkspaceTabShell`, etc. |
| Layout shell | `src/components/layout/ManagementShell.vue` (brand switcher, nav, breadcrumb) |
| Pinia stores | `src/stores/` — `app`, `session`, `templates`, `masters`, `identity`, `audit`, `collaboration`, `contentModules` |
| Composables | `src/composables/` — `useCapabilities`, `useWorkflowTasks`, `useLocaleFormatters`, etc. |
| API modules | `src/api/` — axios instance `http.ts`, error parsing `errorEnvelope.ts` |
| Theme tokens | `src/theme/tokens.ts`, `src/config/brands.ts`, `src/styles/global.scss` |
| i18n | `src/i18n/locales/en.ts` (base) + `zh-CN.ts` (lazy) |
| Types | `src/types/` — `session.ts` (envelope), `contract.ts` |

**Complexity hotspots** to watch: `useTemplateDetailController.ts`, `DashboardView.vue`, `TemplateAuthoringBindingsPanel.vue`, large tab views. Extract instead of inline.

## Quality Gates (must be GREEN before Done)

### Backend

```
mvn -B -ntp -f backend/pom.xml verify
```

Enforced floors (pom.xml ratchet):
- **JaCoCo LINE ≥ 0.70 / BRANCH ≥ 0.45**
- **Checkstyle: 0 violations**
- **PMD: 0 violations** (priority ≤ 5)
- **SpotBugs: 0 bugs** (Medium threshold)
- **Custom coverage walls**: security-critical/core domain lines ≥ 90%

### Frontend

```
pnpm -C frontend lint && type-check && test && build
```

Coverage floors (vitest.config.ts): lines 22 / functions 32 / branches 55.

## Delivery Pipeline (parent agent: you)

All behavior-changing work routes through **one orchestrator** with fixed stage numbers
(see `.cursor/skills/delivery-pipeline/SKILL.md`):

```
0. Placement       → worktree-router (MANDATORY — ../DGE-<slice-id> + move_agent_to_root)
1. Behavior spec   → behavior-spec-author
2. Plan            → plan-orchestrator (+ Task Master when applicable)
3. Docs-first      → doc-keeper
4. Implement       → backend-engineer | frontend-engineer | rendering-engineer [feature worktree]
5. E2E stack prep  → build-deploy-agent (docker-deploy-queue)
6. E2E functional  → e2e-test-engineer
7. E2E UIUX        → e2e-uiux-reviewer
8. Architecture    → architecture-reviewer
9. Code quality    → code-quality-reviewer (optional)
10. Deploy evidence → build-deploy-agent (queue)
11. Integrate      → integration-merger (MANDATORY — merge + remove worktree)
12. Doc sync       → post-task-doc-sync (MAIN only, after merge)
13. Commit gate    → post-task-commit-review
Done
```

**Non-negotiable:**
- **Every delivery session:** new worktree before writes; never implement on MAIN
- No code before behavior spec (BDD)
- No frontend Done without E2E functional + UIUX evidence
- No parallel Docker acceptance stacks — queue only
- Isolated slices: merge + worktree cleanup at stage 11; doc-sync/commit on **MAIN**
- No Done before post-task doc sync + post-task commit review (honor `no-commit`)
- Use `Task` for implementation — never inline multi-file delivery
- **Models:** all specialists pin `grok-4.5-fast-xhigh`; **avoid `glm-5.2-*`**; **`inherit` forbidden** — see `.cursor/agents/MODEL-STRATEGY.md`
- **Supervisor mode:** user stays in one main session; parent autonomously spawns Task subagents
- **MCP:** `.cursor/mcp.json` — task-master-ai, local Postgres (dev), fetch (healthz/OpenAPI)

## Git Workflow Rules

Prefer **small, reviewable documentation changes** with clear commit messages:
- `docs: update requirements`
- `docs: add PRD section`
- `docs: add/update ADR`
- `docs: sync execution ledger`
- `chore: update hooks.json`

**Task-level automation** is enabled when files are allocated to a non-doc Task ID. Auto commit/push only after green required gates. Explicitly opt out with `no-commit`, `draft`, `plan-only`, or equivalent.

**Commit scope:** One coherent task slice per commit (code + docs from same change set). **Never mix unrelated Task IDs.**

See `docs/git-workflow.md` for full details.

## Automation Mode Guardrails (Claude Code + Cursor Dual-Stack)

This project is configured for **dual-agent orchestration** — Cursor owns rules and specialist subagents; Claude Code owns the parent agent orchestration and user communication. **Here is how to use both together effectively:**

### Parent Agent Contract (Claude Code)

Your role is the **orchestrator**: classify requests, invoke the right `Task(subagent_type=...)`, synthesize outputs, and communicate with the user. **Do NOT write production code directly.** Follow `.cursor/rules/subagent-routing-mandate.mdc` before any file writes.

**Mandatory first action when responding to a delivery request:**
1. Classify the request
2. **Invoke `Task(subagent_type=delivery-orchestrator)`** for multi-step/behavior-changing work
3. If user *explicitly* opts out (e.g., "不要子 agent"), work inline

**Exception: single mechanical edit** (no behavior/API/permission/plan-code change, single typo) — you may work inline.

### Auto Commit/Push Authorization

Auto-level task automation is authorized for non-doc Task IDs. After green gates:
- If Task ID was allocated to non-code Task → **Auto stage, commit, push** (unless user opted out with `no-commit`, `no-push`, `draft`)
- If no Task ID was allocated → **Normal** git review, user confirms before commit/push

Cursor's:
```
.cursor/hooks/chain-commit-review.sh
```
hooks `post-task-doc-sync` subagents and triggers post-task-commit-review.

### Recurring Tasks Schedule (setup scope)

If you need to run periodic jobs (monitoring, cleanup, validation), use the `CronCreate` tool:

```powershell
# Example: check container health every 5 minutes
CronCreate -cron "*/5 * * * *" -prompt "Check container health and report status" -recurring $true

# Example: regenerate demo binding configs nightly
CronCreate -cron "0 3 * * *" -prompt "Regenerate demo binding configs from master" -durable $true
```

Store scheduled tasks in `.claude/scheduled_tasks.json`.

### In-Session Permission Whitelisting

Prefer allowing read-only git/fs tools in `.claude/settings.json` to reduce repeated permission prompts for Grep/Read/Glob-style work. Do not weaken write/deploy permissions.

## Documentation Source-of-Truth Order

When in doubt about a behavior/architecture decision, follow this order:

```
1. Latest explicit user confirmation (direct conversation)
2. .taskmaster/tasks/tasks.json (active + pending tasks — new work, task-master-ai)
3. docs/plan/master-plan.md (phase-level history; P0–P22 frozen archive)
4. docs/plan/detail/<phase>.md (task-level history; frozen for Done phases,
   live for LRP/CDP/SOR active programs)
5. docs/domain/domain-model.md
6. docs/security/permission-matrix.md
7. docs/product/PRD.md
8. docs/requirements/requirements-plan.md
9. Acceptance ADRs (docs/adr/)
10. Plan/tech/task sheets (docs/architecture/*task-sheet*.md)
```

**Task source:** `.taskmaster/tasks/tasks.json` (task-master-ai) for new/active work; `docs/plan/` is the frozen archive for P0–P22 history + live record for LRP/CDP/SOR programs.

**Never silently pick a side on conflicts** — surface them and confirm.

## Getting Started After Help/Init

1. **Confirm active phase** — Run `Task(subagent_type=plan-orchestrator)` with prompt: "Show current master-plan.md status and active phase."
2. **Classify request** — Use `.cursor/rules/subagent-routing-mandate.mdc` routing table.
3. **Invoke specialist** — Always via `Task` tool, specify `subagent_type`.
4. **Adhere to gates** — Green gates before committing. Check `.cursor/rules/tdd-bdd-delivery-constitution.mdc`.
5. **Sync docs** — Run doc sync after green gates; use post-task-commit-review before Done.

## Additional References

- **Read more:** `docs/plan/master-plan.md`, `docs/plan/execution-sync-ledger.md`, `docs/plan/README.md`
- **Backend details:** `backend/README.md`
- **Docker deployment (queued):** `.\scripts\docker-deploy-queue.ps1`, `docker-compose.prod.yml`
- **Worktree isolation:** `.cursor/skills/worktree-isolation/SKILL.md`, agents `worktree-router` / `integration-merger`
- **Model strategy:** `.cursor/agents/MODEL-STRATEGY.md` (fleet-wide `grok-4.5-fast-xhigh`; `inherit` forbidden)

- **Frontend i18n:** `.cursor/skills/i18n-english-first/SKILL.md`
- **Frontend OA design:** `.cursor/skills/frontend-oa-design/SKILL.md`
- **TDD delivery loop:** `.cursor/skills/tdd-feature-delivery/SKILL.md`
- **Plan orchestration:** `.cursor/agents/plan-orchestrator.md`
- **Architecture reviewer:** `.cursor/agents/architecture-reviewer.md`
- **All Cursor rules:** `.cursor/rules/*.mdc` (must read all before committing any code change)



<!-- cloude-code-toolbox:mcp-skills-awareness-begin -->

### MCP & Skills awareness (Cloude Code ToolBox)

_Last synced: 2026-07-04T16:55:32.927Z._

- **Full report:** `.claude/cloude-code-toolbox-mcp-skills-awareness.md` in this workspace (auto-overwritten on each scan). Use it as ground truth for configured servers and skill folders.
- **MCP:** For **live tools** in Claude Code, enable the matching server via `/mcp`. Servers are configured in `~/.claude.json` (user) and `.mcp.json` (project).
- **When the user’s task matches a server** (e.g. Confluence work and a **Confluence** / **Atlassian** MCP is listed), **prefer that server id** and plan on tool use—not only file search.
- **Skills:** Folders below contain `SKILL.md`; attach or cite paths in chat when relevant.

#### Workspace MCP

- `d:\working\Document Generation Engine\.mcp.json` _(workspace: Document Generation Engine)_ — _file missing_

_No active workspace servers in mcp.json._

#### User MCP

- `C:\Users\孙豫龙\.claude.json` — _servers defined_

| Server id | Kind | Detail |
|-----------|------|--------|
| zai-mcp-server | stdio | npx -y @z_ai/mcp-server |
| web-search-prime | http | https://open.bigmodel.cn/api/mcp/web_search_prime/mcp |
| web-reader | http | https://open.bigmodel.cn/api/mcp/web_reader/mcp |
| zread | http | https://open.bigmodel.cn/api/mcp/zread/mcp |

#### Project skills

- **docker-deployment** — `d:\working\Document Generation Engine\.cursor\skills\docker-deployment` — Automated Docker build and deployment workflow for the platform. Use to build images, bring up dependency + app stacks with healthcheck gating, verify health, capture deployment evidence, and define rollback — only after

- **document-as-code** — `d:\working\Document Generation Engine\.cursor\skills\document-as-code` — Keep this project's documentation as the durable, rebuildable source of truth. Use when changing requirements, PRD, domain model, permissions, API contracts, ADRs, architecture, or the plan layer; when separating confirm

- **e2e-frontend-testing** — `d:\working\Document Generation Engine\.cursor\skills\e2e-frontend-testing` — Automated frontend end-to-end testing workflow with Playwright for the management UI. Use to encode BDD acceptance scenarios as user-journey tests (functional) and to drive UIUX visual/responsive/accessibility evidence f

- **frontend-oa-design** — `d:\working\Document Generation Engine\.cursor\skills\frontend-oa-design` — Locked bank OA visual + interaction design system for the management UI. Use whenever building or changing any user-facing Vue surface — shell, navigation, tables, forms, dialogs, states, theming — to keep an advanced, p

- **i18n-english-first** — `d:\working\Document Generation Engine\.cursor\skills\i18n-english-first` — Internationalization convention for this platform — the system supports multiple languages with English as the primary/base language. Use when adding or changing any user-facing string, error message, API messageKey, or 

- **plan-status-tracking** — `d:\working\Document Generation Engine\.cursor\skills\plan-status-tracking` — Maintain the layered project plan (overall master plan + per-phase detailed plans) and track completion status from zero. Use when planning work, updating phase/task status, selecting the next phase, or reconciling the p

- **post-task-commit-review** — `d:\working\Document Generation Engine\.cursor\skills\post-task-commit-review` — Mandatory workflow after post-task-doc-sync — review the full change set, escalate to architecture-reviewer/explore subagents when needed, then stage, commit, and push before claiming Done. Skips push only when the user 

- **post-task-doc-sync** — `d:\working\Document Generation Engine\.cursor\skills\post-task-doc-sync` — Mandatory workflow after every behavior-changing task completes and gates pass — sync plan layer, execution ledger, epic/milestone sheets, and indexes before claiming Done.

- **tdd-feature-delivery** — `d:\working\Document Generation Engine\.cursor\skills\tdd-feature-delivery` — Test-first, behavior-driven delivery loop for backend (Java 21 + Spring Boot) and frontend (Vue 3 + TS) slices in this project. Use when implementing any feature slice, fixing a bug with a regression test, or before clai

#### User skills

- **microsoft-foundry** — `C:\Users\孙豫龙\.agents\skills\microsoft-foundry` — Deploy, evaluate, and manage Foundry agents end-to-end: Docker build, ACR push, hosted/prompt agent create, container start, batch eval, continuous eval, prompt optimizer workflows, agent.yaml, dataset curation from trac

<!-- cloude-code-toolbox:mcp-skills-awareness-end -->

## Task Master AI Instructions
**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
