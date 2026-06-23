# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23). Active phase: **P12**
(deferred enhancements). See [execution-sync-ledger.md](./execution-sync-ledger.md).

## Layer model

```text
docs/plan/master-plan.md              ← Overall plan (phase granularity)
    └── docs/plan/detail/P*.md        ← Detailed tasks & design per phase
docs/plan/execution-sync-ledger.md    ← Epic/milestone mirror + evidence
docs/architecture/orchestration-high-level-plan.md   ← Epic ordering (reference)
docs/architecture/implementation-task-plan.md        ← Technical waves (reference)
docs/architecture/m*-task-sheet.md                   ← Milestone task decomposition
docs/architecture/e*-task-sheet.md                   ← Epic task decomposition
```

## Rules

1. Exactly **one phase** may be `In Progress` at a time.
2. Status vocabulary: `Not Started` | `In Progress` | `Blocked` | `Done`.
3. Prior completion claims were void at reset; re-earned status is recorded in
   [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md) and this layer.
4. `Done` requires real behavior + green gates — never demo/in-memory/mock-only.
5. Behavior-changing work needs a behavior spec before task decomposition.
6. **Post-task doc sync is mandatory** before claiming Done (see
   `.cursor/agents/post-task-doc-sync.md`).

## Phase overview

| Phase | Name | Detailed plan | Status |
| --- | --- | --- | --- |
| P0 | Foundation & guardrails | [detail/P0-foundation.md](detail/P0-foundation.md) | Done |
| P1 | Login & session | [detail/P1-login-session.md](detail/P1-login-session.md) | Done |
| P2 | Master document management | [detail/P2-master-management.md](detail/P2-master-management.md) | Done |
| P3 | Template authoring | [detail/P3-template-authoring.md](detail/P3-template-authoring.md) | Done |
| P4 | Rendering & preview | [detail/P4-rendering-preview.md](detail/P4-rendering-preview.md) | Done |
| P5 | Lifecycle governance | [detail/P5-lifecycle-governance.md](detail/P5-lifecycle-governance.md) | Done |
| P6 | API management | [detail/P6-api-management.md](detail/P6-api-management.md) | Done |
| P7 | Runtime dynamic API | [detail/P7-runtime-api.md](detail/P7-runtime-api.md) | Done |
| P8 | Audit & contract visibility | [detail/P8-audit-contract.md](detail/P8-audit-contract.md) | Done |
| P9 | Production readiness | [detail/P9-production-readiness.md](detail/P9-production-readiness.md) | Done |
| P10 | Runtime document download | [detail/P10-runtime-download.md](detail/P10-runtime-download.md) | Done |
| P11 | Batch & async generation | [detail/P11-batch-async.md](detail/P11-batch-async.md) | Done |

## First delivery target (thin vertical slice) — achieved

Login → upload approved master → create template → test → approve → publish →
call runtime API → receive DOCX. Mapped to **P0–P7 (minimal sync path)** — Done.

## Next focus (P12)

Deferred/post-MVP enhancements without a single active slice. See
[master-plan.md](./master-plan.md) deferred section and outstanding items in
[execution-sync-ledger.md](./execution-sync-ledger.md).
