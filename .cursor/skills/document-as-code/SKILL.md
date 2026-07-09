---
name: document-as-code
description: Keep this project's documentation as the durable, rebuildable source of truth. Use when changing requirements, PRD, domain model, permissions, API contracts, ADRs, architecture, or the plan layer; when separating confirmed facts from pending questions; or when reconciling drift between code and docs.
---

# Document as Code

Documentation is the first-class asset. Code is a replaceable realization. The
system must remain rebuildable from documentation alone.

## Workflow

```
- [ ] 1. Read the owning source-of-truth document(s) before changing anything
- [ ] 2. Update documentation BEFORE / WITH the implementation change
- [ ] 3. Keep confirmed requirements separate from pending questions
- [ ] 4. Update docs/README.md index + cross-links in the same change
- [ ] 5. Record durable decisions as ADRs under docs/adr/
- [ ] 6. Re-check no doc contradicts another (resolve conflicts explicitly)
```

## Source-of-truth order (resolve conflicts in this order)

1. Latest explicit user confirmation
2. `.taskmaster/tasks/tasks.json` (active work since 2026-07-05; ADR-0053)
3. `docs/plan/` (master-plan, detail, execution-sync-ledger)
4. `docs/requirements/requirements-plan.md`
5. `docs/product/PRD.md`
6. `docs/domain/domain-model.md`
7. `docs/security/permission-matrix.md`
8. ADRs under `docs/adr/`

If two documents disagree, do NOT silently choose. Mark the conflict as an open
question and confirm with the user; preserve the latest confirmed decision.

## Hard rules

- Never promote assumptions, recommendations, or common patterns into confirmed requirements.
- Never edit accepted ADR decisions to reflect implementation progress. ADR status
  records a decision, not task completion.
- Completion claims must reflect real, durable, verifiable behavior — not demo/in-memory/mock.
- Every active document is reachable from `docs/README.md`.
- Prefer small, reviewable, diff-friendly changes.

## Project status baseline

Execution truth: `.taskmaster/tasks/tasks.json` for new/active work; `docs/plan/` for
P0–P23 history and live LRP/CDP/SOR programs; `docs/plan/execution-sync-ledger.md` for
evidence. See `docs/PROJECT-STATUS-RESET.md`. Post-task sync is mandatory before Done
(`.cursor/skills/post-task-doc-sync/SKILL.md`).
