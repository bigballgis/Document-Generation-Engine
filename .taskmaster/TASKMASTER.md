# Task Master — Cursor Integration Guide

Task Master (`task-master-ai`) is the **machine-readable task source** for new/active work
(ADR-0053, amended by ADR-0055). Parent agents use it via **Cursor MCP** only.

## Canonical MCP

Configure in **`.cursor/mcp.json`** (sole project MCP path — ADR-0055):

```json
{
  "mcpServers": {
    "task-master-ai": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "task-master-ai"],
      "env": { "TASK_MASTER_TOOLS": "core" }
    }
  }
}
```

Do **not** use a root `.mcp.json` or Claude Code MCP paths.

## Essential paths

| Path | Role |
| --- | --- |
| `.taskmaster/tasks/tasks.json` | Active task SoT (#2 in conflict order) |
| `.taskmaster/config.json` | Models / defaults |
| `.taskmaster/state.json` | Runtime tag/state |
| `.taskmaster/templates/` | PRD / parse templates |
| `docs/plan/` | Phase history + live programs (SoT #3) |

## Cursor workflow

1. Parent classifies intent → `delivery-orchestrator` (or named specialist).
2. `plan-orchestrator` syncs Task Master status with plan/ledger when a slice moves.
3. Prefer MCP tools (`get_tasks`, `next_task`, `set_task_status`, …) over inventing task IDs.
4. Formal `docs/plan/` phases remain **None** unless explicitly activated — do not invent P-phases.

## CLI (optional)

When MCP is unavailable, `npx task-master-ai` / global `task-master` CLI may list or update
tasks. Prefer MCP from Cursor agents.

## Forbidden

- Dual-agent / Claude Code parent narrative
- Writing `taskmaster.mdc` / `dev_workflow.mdc` into `.cursor/rules/` (conflicts with constitutions)
- Treating Task Master `done` as product Done without gates + doc-sync + commit-review

## Related

- [AGENTS.md](../AGENTS.md) — agent index
- [ADR-0053](../docs/adr/documentation-governance/0053-task-master-ai-adoption.md)
- [ADR-0055](../docs/adr/documentation-governance/0055-cursor-sole-parent-agent.md)
- `.cursor/skills/delivery-pipeline/SKILL.md`
