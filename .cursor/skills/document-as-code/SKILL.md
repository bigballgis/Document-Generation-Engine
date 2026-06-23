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
2. `docs/requirements/requirements-plan.md`
3. `docs/product/PRD.md`
4. `docs/domain/domain-model.md`
5. `docs/security/permission-matrix.md`
6. ADRs under `docs/adr/`

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

The project restarted from zero (see `docs/PROJECT-STATUS-RESET.md`). P0–P11
re-earned Done; execution truth lives in `docs/plan/` and
`docs/plan/execution-sync-ledger.md`. Post-task sync is mandatory before Done
(`.cursor/skills/post-task-doc-sync/SKILL.md`).
