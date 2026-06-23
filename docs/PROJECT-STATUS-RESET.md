# Project Status Reset

**Effective date:** 2026-06-23

## Decision

The project **restarted from zero** on 2026-06-23. All prior wave, epic, milestone,
and task-sheet completion claims were **historical and void** until re-earned with
real, durable, verifiable behavior and green quality gates.

## Re-earned progress (2026-06-23)

Phases **P0–P11** have been re-earned **Done** with implementation in `backend/`
and `frontend/`, green quality gates, and updated plan detail docs.

| Evidence | Location |
| --- | --- |
| Phase status | [docs/plan/master-plan.md](./plan/master-plan.md) |
| Task-level detail | [docs/plan/detail/](./plan/detail/) |
| Epic/milestone mirror | [docs/plan/execution-sync-ledger.md](./plan/execution-sync-ledger.md) |
| Gate logs | Backend `mvn verify`; frontend lint/type-check/test/build (2026-06-23) |

**Active phase:** P12 (deferred enhancements).  
**Still open:** external deployment validation (E05-T06), intranet SCA (M9-T02), M10–M11.

## What is preserved

| Category | Status |
| --- | --- |
| Confirmed requirements (`docs/requirements/`, `docs/product/PRD.md`) | Preserved |
| Domain model, permission matrix, API contract (OpenAPI v1) | Preserved |
| Accepted ADRs under `docs/adr/` | Preserved — ADR status records a **decision**, not task completion |
| Architecture views (module boundaries, runtime, storage, security) | Preserved as design baseline |

## What was reset (2026-06-23)

| Category | Baseline at reset |
| --- | --- |
| Epic / wave / milestone / task completion | Void until re-earned |
| Closure evidence, gate logs, in-repo done snapshots | Empty until re-captured |
| Active epic / active phase | None until explicitly activated |

## Execution truth

- **Overall plan:** `docs/plan/master-plan.md`
- **Detailed plans:** `docs/plan/detail/<phase>.md`
- **Sync ledger:** `docs/plan/execution-sync-ledger.md`
- **Epic ordering reference:** `docs/architecture/orchestration-high-level-plan.md`
- **Technical wave reference:** `docs/architecture/implementation-task-plan.md`

## Status vocabulary (only these)

- `Not Started` — no meaningful implementation work yet
- `In Progress` — active delivery focus (only one phase/epic at a time)
- `Blocked` — cannot proceed until dependency, decision, or environment is resolved
- `Done` — exit criteria met with real behavior + green gates + updated docs

## Re-earning Done

A task or phase may be marked `Done` only when:

1. Behavior is durable (not demo/in-memory/mock-only).
2. Required tests and quality gates pass.
3. Owning documentation and plan status are updated in the same change set.
4. Post-task doc sync completes (see `.cursor/rules/post-task-doc-sync-constitution.mdc`).
