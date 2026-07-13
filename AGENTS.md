# Agent & AI scaffolding index

Short map for humans and parent agents. Full narrative: [README.md](README.md)
(**AI agent delivery system**). Details under `.cursor/`.

## Supervisor mode (default)

Stay in **one main chat**. Speak the goal in natural language — the parent agent
**auto-maps** intent to deliver / multitask / deploy-queue / verify-done and spawns
`Task` specialists. You do **not** need to type slash commands every time.

| You say (examples) | Parent does |
| --- | --- |
| 「把 F7 做完」「修这个 bug」「按管线交付」 | **deliver** → `delivery-orchestrator` |
| 「这两个切片并行」「同时改前后端」 | **multitask-slices** → worktree-router + ≤3 writers |
| 「部署一下」「队列状态」「重启栈」 | **deploy-queue** → `build-deploy-agent` |
| 「验收一下」「算不算 Done」 | **verify-done** → `verifier` |

Optional shortcuts (same workflows): `/deliver`, `/multitask-slices`, `/deploy-queue`,
`/verify-done` under `.cursor/commands/`.

Native Cursor: `/multitask`, `/worktree`, `/best-of-n`, Agents Window — see
`.cursor/skills/cursor-native-parallel/SKILL.md`.

## Pipeline (0–14)

See `.cursor/skills/delivery-pipeline/SKILL.md` and `delivery-orchestrator`.
Optional stage **14** = `verifier`.

## Agents (18)

**Canonical model (all specialists):** `grok-4.5-fast-xhigh`  
Tiers below are **pipeline roles only** — model pin is identical. `inherit` forbidden.

| Tier | Agents |
| --- | --- |
| Governance | delivery-orchestrator, plan-orchestrator, architecture-reviewer, code-quality-reviewer (`is_background`), integration-merger, post-task-commit-review |
| Delivery | behavior-spec-author, doc-keeper, backend-engineer, frontend-engineer, rendering-engineer, e2e-*, post-task-doc-sync |
| Execution | worktree-router, build-deploy-agent, deploy-engineer (rollback), **verifier** |

See `.cursor/agents/MODEL-STRATEGY.md`.

## Built-in (no project file)

- `explore` — deep read-only audit
- `bugbot` — defect-oriented review

## MCP (Cursor)

`.cursor/mcp.json`: `task-master-ai`, `docgen-postgres` (local Docker only), `fetch`.

## Docker

Always `.\scripts\docker-deploy-queue.ps1` on this single host.
