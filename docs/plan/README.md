# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** (identity & group
administration) completed **Done** (2026-06-23). Active phase: **none — no single active
phase slice**; P12 is the non-active deferred-enhancements catch-all. See
[execution-sync-ledger.md](./execution-sync-ledger.md).

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
| P12 | Deferred enhancements | [master-plan.md](./master-plan.md) (catch-all, non-active) | Not Started |
| P13 | Identity & group administration | [detail/P13-identity-group-administration.md](detail/P13-identity-group-administration.md) | Done |
| P14 | Confirmed large domains | [detail/P14-confirmed-large-domains.md](detail/P14-confirmed-large-domains.md) | Not Started |
| P15 | Kubernetes deployment & container hardening | [detail/P15-kubernetes-deployment-container-hardening.md](detail/P15-kubernetes-deployment-container-hardening.md) | Not Started |
| P16 | Template & version lifecycle governance completeness | [detail/P16-lifecycle-version-governance.md](detail/P16-lifecycle-version-governance.md) | Not Started |
| P17 | Per-domain API policy governance | [detail/P17-api-policy-domain-governance.md](detail/P17-api-policy-domain-governance.md) | Not Started |
| P18 | Structured authoring & rendering-fidelity engine | [detail/P18-structured-authoring-fidelity-engine.md](detail/P18-structured-authoring-fidelity-engine.md) | Not Started |
| P19 | Template verifiability, publish gate & decision forms | [detail/P19-verifiability-publish-gate.md](detail/P19-verifiability-publish-gate.md) | Not Started |
| P20 | i18n multi-locale readiness & UI upgradeability | [detail/P20-i18n-ui-upgradeability.md](detail/P20-i18n-ui-upgradeability.md) | In Progress (T06 open) |

## First delivery target (thin vertical slice) — achieved

Login → upload approved master → create template → test → approve → publish →
call runtime API → receive DOCX. Mapped to **P0–P7 (minimal sync path)** — Done.

## Next focus

P13 (identity & group administration) is **Done** (2026-06-23) — GLOBAL/GROUP admins manage
users and business groups through the management API + UI, with fail-closed privilege-escalation
protection, audit, and green quality gates (local account store is the authorization authority,
ADR 0036). See [detail/P13-identity-group-administration.md](detail/P13-identity-group-administration.md).
There is currently **no single active phase slice**. P12 (deferred enhancements) remains a
non-active catch-all; P14 (confirmed large domains) is the next planned large slice. **P16–P20**
were added 2026-06-23 from a deep functional-gap
review — lifecycle/version governance (P16), per-domain API policy governance (P17),
structured authoring & fidelity engine (P18), verifiability/publish-gate + decision forms
(P19), and i18n multi-locale + UI upgradeability (P20); all `Not Started`.

## Optimization backlogs

| Backlog | Lens |
| --- | --- |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
