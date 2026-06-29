# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** **Done** (2026-06-23);
**P17** per-domain API policy governance **Done** (2026-06-25; Wave 3); **P19**
verifiability/publish-gate **Done** (2026-06-25); **P20** i18n **Done** (2026-06-25).
**P14** confirmed large domains **Done** (2026-06-27). **Active phase:** **P15**
(Kubernetes deployment & container hardening, **In Progress** — **P15-T01 Done**; **P15-T02 Done** re-earned; **P15-T03 Done** re-earned; **P15-T04 Done** re-earned; **P15-T05 Done** re-earned; **P15-T06 Done** re-earned; **P15-T07 Done** re-earned; **P15-T08 Done** re-earned; **P15-T09 Done** re-earned; **P15-T10** open).
P12 is the non-active deferred-enhancements catch-all. **P18 Not Started**
(sequence P14 → P15 → P18). See
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
| P14 | Confirmed large domains | [detail/P14-confirmed-large-domains.md](detail/P14-confirmed-large-domains.md) | Done (2026-06-27) |
| P15 | Kubernetes deployment & container hardening | [detail/P15-kubernetes-deployment-container-hardening.md](detail/P15-kubernetes-deployment-container-hardening.md) | In Progress (2026-06-27; P15-T01 + T02 + T03 + T04 + T05 + T06 + T07 + T08 Done) |
| P16 | Template & version lifecycle governance completeness | [detail/P16-lifecycle-version-governance.md](detail/P16-lifecycle-version-governance.md) | Not Started |
| P17 | Per-domain API policy governance | [detail/P17-api-policy-domain-governance.md](detail/P17-api-policy-domain-governance.md) | Done (2026-06-25; Wave 3) |
| P18 | Structured authoring & rendering-fidelity engine | [detail/P18-structured-authoring-fidelity-engine.md](detail/P18-structured-authoring-fidelity-engine.md) | Not Started |
| P19 | Template verifiability, publish gate & decision forms | [detail/P19-verifiability-publish-gate.md](detail/P19-verifiability-publish-gate.md) | Done (2026-06-25; T01–T10) |
| P20 | i18n multi-locale readiness & UI upgradeability | [detail/P20-i18n-ui-upgradeability.md](detail/P20-i18n-ui-upgradeability.md) | Done (2026-06-25) |

## First delivery target (thin vertical slice) — achieved

Login → upload approved master → create template → test → approve → publish →
call runtime API → receive DOCX. Mapped to **P0–P7 (minimal sync path)** — Done.

## Next focus

**P14 Done** (2026-06-27) — confirmed large domains all three vertical slices complete:
**P14-T01** clause/content module (T01a–T01e; backend **469**; frontend **224**; architecture re-review **PASS**);
**P14-T02** collaboration to-dos + timeout escalation (T02a–T02d; E2E **3/3**; backend **481**; frontend **235**);
**P14-T03** template export/import (T03a–T03c; OpenAPI contract; E2E **2/2**; backend **481**; frontend **235+**).
See [detail/P14-confirmed-large-domains.md](detail/P14-confirmed-large-domains.md).

**Active phase:** **P15** (Kubernetes deployment & container hardening, **In Progress**).
**P15-T01** container hardening — **Done** (T01a–T01c, 2026-06-27). **P15-T02** Helm chart scaffold — **Done** (T02a–T02c, re-earned 2026-06-27). **P15-T03** configuration & secrets — **Done** (T03a–T03c, re-earned 2026-06-27). **P15-T04** Service + Ingress + cert-manager TLS — **Done** (T04a–T04c, re-earned 2026-06-27). **P15-T05** HPA (CPU/memory + custom metrics) — **Done** (T05a–T05b, re-earned 2026-06-27). **P15-T06** default-deny NetworkPolicy + explicit allow — **Done** (T06a–T06b, re-earned 2026-06-27). **P15-T07** health probes (`/healthz` + `/readyz`) — **Done** (T07a–T07c, re-earned 2026-06-27). **P15-T08** blue-green release — **Done** (T08a–T08c, re-earned 2026-06-27). **P15-T09** CI manifest validation — **Done** (T09a–T09b, re-earned 2026-06-27). **Next:** **P15-T10** deployment docs + phase close. **P18** (structured authoring &
rendering-fidelity engine) **Not Started** — queued after P15. P12 (deferred enhancements)
remains a non-active catch-all.

## Optimization backlogs

| Backlog | Lens |
| --- | --- |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
