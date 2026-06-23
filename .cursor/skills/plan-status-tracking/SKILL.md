---
name: plan-status-tracking
description: Maintain the layered project plan (overall master plan + per-phase detailed plans) and track completion status from zero. Use when planning work, updating phase/task status, selecting the next phase, or reconciling the plan with repository reality.
---

# Plan Status Tracking

The plan is layered: an overall master plan at phase granularity, and detailed
per-phase plans with task/design decomposition.

## Files

- `docs/plan/README.md` — plan layer index and rules.
- `docs/plan/master-plan.md` — phases P0..P11 (overall plan).
- `docs/plan/detail/<phase>.md` — detailed tasks and design for one phase.
- `docs/plan/execution-sync-ledger.md` — epic/milestone mirror + gate evidence.

## Status vocabulary (only these)

- `Not Started`
- `In Progress`
- `Blocked`
- `Done`

`Done` requires real, durable, verifiable behavior with green gates — never
demo/in-memory/mock-only claims.

## Rules

- The project starts from zero. All prior wave/epic closure claims are historical
  and void until re-earned (see `docs/PROJECT-STATUS-RESET.md`).
- Exactly one phase is `In Progress` at a time.
- Classify each new request against the active phase before task planning. If it
  doesn't match, update the plan and confirm priority with the user first.
- Keep the master plan at phase granularity; push task detail into `docs/plan/detail/`.
- When repository reality diverges from the plan, refresh the plan before broad new work.
- After any status change, run post-task doc sync (`.cursor/skills/post-task-doc-sync/SKILL.md`).
- Update both master and detailed plan when a phase or task status changes.

Update checklist:

- [ ] Master plan phase status updated
- [ ] Detailed plan task status updated
- [ ] execution-sync-ledger.md updated
- [ ] Post-task doc sync completed
- [ ] Single active phase invariant holds
- [ ] Next phase selected if current completed
