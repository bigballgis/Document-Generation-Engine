# Agent & AI scaffolding index

Short map for humans and parent agents. Full narrative: [README.md](README.md)
(**AI agent delivery system**). Details under `.cursor/`.

## Supervisor mode (default)

Stay in **one main chat**. Speak the goal in natural language — the parent agent
**auto-maps** intent to deliver / multitask / deploy-queue / verify-done and spawns
`Task` specialists. You do **not** need to type slash commands every time.

| You say (examples) | Parent does |
| --- | --- |
| 「把 F7 做完」「修这个 bug」「按管线交付」「自动执行后续」 | **deliver** → `delivery-orchestrator` (**stage −1 Batch Recommendation** then one leaf). Task flake → **retry** (≤3); still unavailable → **BLOCKED** (recovery hints). GP downgrade **only** if you say `允许降级` / `allow-gp-fallback` |
| 「这两个切片并行」「同时改前后端」 | **Refuse fan-out by default** → serial queue. Only `force-parallel` / `强制并行` → legacy multitask (≤2 writers) |
| 「部署一下」「队列状态」「重启栈」 | **deploy-queue** → `build-deploy-agent` |
| 「验收一下」「算不算 Done」 | **verify-done** → `verifier` |

**Default (2026-07-16):** single-lane serial on this Docker host — at most one CE/delivery
leaf In Progress. See CE plan §9.2.

Optional shortcuts: `/deliver` (preferred), `/multitask-slices` (legacy opt-in),
`/deploy-queue`, `/verify-done` under `.cursor/commands/`.

Native Cursor parallel primitives are **opt-in only** — see
`.cursor/skills/cursor-native-parallel/SKILL.md`.

## Pipeline (−1, then 0–14)

See `.cursor/skills/delivery-pipeline/SKILL.md` and `delivery-orchestrator`.

- **Stage −1 — Batch Recommendation** (mandatory on deliver): skill
  [delivery-batch-recommend](.cursor/skills/delivery-batch-recommend/SKILL.md);
  behavior [delivery-batch-recommend.md](docs/behavior/delivery-batch-recommend.md).
  Decide `merge` | `solo` | `split` from repo facts so related work shares **one**
  worktree / one evidence run. **Not** multi-writer parallel.
- **Specialist runtime (retry first):** Task flake / missing enum —
  [specialist-runtime-fallback](.cursor/skills/specialist-runtime-fallback/SKILL.md);
  behavior [specialist-runtime-fallback.md](docs/behavior/specialist-runtime-fallback.md).
  Retry named specialist → **BLOCKED** (no auto GP). Opt-in: `允许降级` / `allow-gp-fallback`.
  Emit `runtime_routing`.
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
if missing → retry (≤3) then **BLOCKED** (no auto GP).

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

