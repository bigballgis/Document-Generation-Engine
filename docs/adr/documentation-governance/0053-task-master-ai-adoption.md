---
id: ADR-0053
title: task-master-ai Adoption as New Task Source (Scoped)
status: Accepted
date: 2026-07-05
deciders: architecture, user
related:
  - docs/plan/master-plan.md
  - docs/plan/README.md
  - docs/plan/execution-sync-ledger.md
  - CLAUDE.md
---

# ADR-0053 — task-master-ai Adoption as New Task Source (Scoped)

## Context

The existing plan layer under `docs/plan/` has carried the project from P0 to P22 (23 formal
phases, 30 detail files, 6 cross-cutting programs: CDP/LRP/SOR/COR/OPT/UX). It is durable and
fully traceable from zero, but it has accumulated structural debt that makes **new** task
management harder:

- **No unified task schema** — each detail file invents its own column set
  (`ID|Task|Owner|Depends on|Status` vs `ID|Task|Acceptance|Status` vs
  `ID|Pri|Title|Evidence|Acceptance|Status|Maps`, etc.).
- **4-value status enum** (`Not Started / In Progress / Blocked / Done`) that does not map 1:1
  to any modern task tool's lifecycle.
- **No machine-readable dependency graph** — dependencies are free-text columns, so "next task"
  must be inferred by reading prose.
- **No AI-native breakdown** — `expand` / `analyze-complexity` / `parse-prd` style tooling is
  unavailable; every breakdown is hand-written.
- **22 governance files** (CLAUDE.md, 4 `.cursor/rules` constitutions, 5 agents, 4 SKILLs,
  scripts, READMEs) hard-couple to `docs/plan/*` as the write surface, so any in-place schema
  change would ripple across the whole governance surface in one shot.

`task-master-ai@0.43.1` is now installed globally. It provides a fixed JSON task schema,
dependency graph, status lifecycle (`pending / in-progress / review / done / deferred / cancelled`),
AI-driven PRD parsing, complexity analysis, and subtask expansion. It can run on
`claude-code/sonnet` with **no API key** (via the local Claude Code CLI), which fits this
dual-agent project's existing model strategy.

A full "big-bang" migration — reparenting all 200+ historical tasks into `.taskmaster/tasks.json`
and rewriting the 22 governance files — was considered and rejected because:

1. The 4-value `Blocked` status has no clean home in task-master's 6-value enum.
2. Each of the 30 detail files would need a per-file schema mapping (no unified source schema
   exists to automate from).
3. It would touch 30 already-`Done` historical detail files, risking the very traceability
   we want to preserve.
4. The `plan-orchestrator` Cursor agent and 4 `.cursor/rules` constitutions are load-bearing
   for the live LRP/CDP/SOR programs; rewriting them mid-flight destabilizes active work.
5. `task-master init` defaults to installing `taskmaster.mdc` + `dev_workflow.mdc` into
   `.cursor/rules/`, which directly conflicts with the existing four constitutions
   (`subagent-routing-mandate`, `delivery-orchestration-constitution`, `tdd-bdd-delivery-constitution`,
   `document-as-code-constitution`) — two orchestration rule sets in one directory would
   produce ambiguous routing.

## Decision

Adopt **scoped B**: establish `task-master-ai` as the task source for **new and active work**
going forward, while freezing `docs/plan/` as the **historical archive** (P0–P22) and **live
record** for the currently-running LRP/CDP/SOR programs.

Concretely:

1. **MCP registration** — `task-master-ai` is registered in `~/.claude.json` (user MCP) and
   `.mcp.json` (project MCP), `TASK_MASTER_TOOLS=core` (7 tools, low context footprint).
2. **Project init** — `task-master init --rules=claude` scaffolds `.taskmaster/`. The
   `--rules=claude` flag (not `cursor`) avoids writing `taskmaster.mdc`/`dev_workflow.mdc`
   into `.cursor/rules/`; the guide lands in `.taskmaster/CLAUDE.md` instead, so the existing
   four constitutions remain the sole orchestration rules.
3. **Model** — main + research set to `claude-code/sonnet` (no API key); fallback cleared.
4. **Source-of-truth order** — CLAUDE.md updated: `.taskmaster/tasks/tasks.json` is #2 for
   new/active work; `docs/plan/master-plan.md` and `docs/plan/detail/<phase>.md` slide to
   #3/#4 as frozen archive + live program record.
5. **Traceability preserved** — project history remains fully traceable from zero:
   - P0–P22 (2026-06-23 → 2026-07-03): `docs/plan/master-plan.md` + `docs/plan/detail/P*.md`
     + `docs/plan/execution-sync-ledger.md` — **frozen, read-only archive**.
   - LRP/CDP/SOR active programs: continue to live in their `docs/plan/<program>*.md` files
     until each program closes; new programs created after 2026-07-05 go into
     `.taskmaster/tasks/tasks.json` instead.
   - New work from 2026-07-05: `.taskmaster/tasks/tasks.json`.
6. **Gitignore** — `.taskmaster/tasks/tasks.json` IS committed (it is the source of truth);
   only `.taskmaster/.env`, `logs/`, and `report*.json` are ignored.

## Consequences

### Positive

- New work gets a standard schema, dependency graph, and AI breakdown (`parse-prd`,
  `expand`, `analyze-complexity`) that the markdown layer never had.
- Zero API-key cost — runs on the existing Claude Code CLI.
- `docs/plan/` historical archive is **not** modified — full P0–P22 traceability is preserved
  exactly as it was on 2026-07-03.
- No conflict with the four `.cursor/rules` constitutions or the `plan-orchestrator` agent;
  they continue to own `docs/plan/` untouched.
- Migration is incremental and reversible — if task-master proves a poor fit, the
  `.taskmaster/` directory can be deleted with zero impact on the historical archive.

### Negative / accepted trade-offs

- **Two task systems coexist** until LRP/CDP/SOR close and migrate. CLAUDE.md's
  source-of-truth order governs which is authoritative for a given question.
- **Two status enums coexist** (4-value in archive, 6-value in task-master) with **no
  cross-mapping** — each system only manages tasks in its own domain.
- **`plan-orchestrator` agent does not yet read `.taskmaster/`** — it still routes through
  `docs/plan/`. New-work planning via task-master is currently a parent-agent (Claude Code)
  responsibility, exercised through the `task-master` CLI or the MCP tools. Folding
  `.taskmaster/` into `plan-orchestrator` is a future architecture change (separate ADR).
- **`task-master init`'s default `cursor` rules were deliberately not installed.** If a future
  contributor runs `task-master rules add cursor`, they will create the constitution conflict
  this ADR avoided. The `--rules=claude` choice is documented here as the canonical setting.

## Migration of active programs (deferred, separate PRs)

The LRP, CDP, and SOR programs have remaining open tasks in `docs/plan/<program>*.md`. They
will be migrated into `.taskmaster/tasks/tasks.json` **per program, in dedicated PRs**, as
each program nears closure — not in this adoption change. This keeps the active-work
destabilization risk bounded.

## Verification

- `/mcp` shows `task-master-ai: ✔ Connected` (7 core tools).
- `task-master models` shows main = `claude-code/sonnet`, research = `claude-code/sonnet`,
  fallback = cleared, no API key required.
- `.taskmaster/config.json`, `.taskmaster/state.json`, `.taskmaster/CLAUDE.md` exist and are
  tracked by git; `.taskmaster/.env` is gitignored.
- `grep -n "tasks.json" CLAUDE.md` hits the source-of-truth order and project-context lines.
- `docs/plan/` tree is byte-identical to its pre-adoption state (`git status docs/plan/` clean).
- `git check-ignore .taskmaster/.env` returns a hit.
