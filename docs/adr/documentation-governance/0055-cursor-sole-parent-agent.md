---
id: ADR-0055
title: Cursor as Sole Parent Agent and Canonical MCP Path
status: Accepted
date: 2026-07-14
deciders: architecture, user
related:
  - docs/adr/documentation-governance/0053-task-master-ai-adoption.md
  - AGENTS.md
  - .cursor/mcp.json
  - .taskmaster/TASKMASTER.md
amends:
  - ADR-0053 (MCP path + parent-agent tooling; Task Master remains active)
---

# ADR-0055 — Cursor as Sole Parent Agent and Canonical MCP Path

## Context

ADR-0053 adopted task-master-ai as the task source for new/active work and described a
dual-agent operating model (Claude Code as parent orchestrator + Cursor specialists),
with MCP registered in both `~/.claude.json` / root `.mcp.json` and a guide at
`.taskmaster/CLAUDE.md`.

The project has since standardized on **Cursor supervisor mode**: one main Cursor chat,
`delivery-orchestrator` + specialist agents under `.cursor/agents/`, constitutions under
`.cursor/rules/`, and skills under `.cursor/skills/`. Root `CLAUDE.md`, the `.claude/`
tree (settings, hooks with `--no-verify` auto-commit, `commands/tm/*`), and the duplicate
root `.mcp.json` are leftover dual-stack scaffolding that drift from the live delivery
pipeline and create conflicting agent entrypoints.

## Decision

1. **Cursor is the sole parent agent.** Specialist work routes through
   `.cursor/agents/` via the Task tool and the delivery pipeline
   (`.cursor/skills/delivery-pipeline/SKILL.md`). Do not document or restore a Claude Code
   dual-agent parent role.
2. **Canonical MCP path is `.cursor/mcp.json` only.** Root `.mcp.json` is removed.
   Task Master continues via the Cursor MCP `task-master-ai` server (`TASK_MASTER_TOOLS=core`).
3. **Claude Code tooling is removed from the repo:** delete `.claude/`, root `CLAUDE.md`,
   and dual-agent narrative. Root agent entry is **`AGENTS.md`** (+ always-applied
   `.cursor/rules`).
4. **Task Master guide is Cursorized:** `.taskmaster/CLAUDE.md` → `.taskmaster/TASKMASTER.md`
   (Cursor MCP only; no Claude Code sections).
5. **ADR-0053 decision body is not rewritten.** This ADR amends the *tooling / parent-agent*
   and *MCP path* aspects of ADR-0053 while keeping Task Master as the active task source
   for new work and `docs/plan/` as the historical + live-program record.

## Consequences

- Single agent entry (`AGENTS.md`) and single MCP config reduce onboarding and drift risk.
- `scripts/validate-doc-structure.ps1` may assert absence of dual-agent leftovers.
- Historical ADR-0053 text may still mention Claude paths; readers must follow **this ADR**
  for current tooling. Do not silently edit Accepted ADR-0053 decision sections.
- Task Master workflows remain available through Cursor MCP and `.taskmaster/TASKMASTER.md`.

## Evidence / acceptance

- No tracked `.claude/` tree, root `CLAUDE.md`, or root `.mcp.json`.
- `.cursor/mcp.json` present; `.taskmaster/TASKMASTER.md` present.
- `AGENTS.md` describes Cursor-only supervisor mode.
