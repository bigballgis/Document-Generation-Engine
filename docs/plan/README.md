# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** **Done** (2026-06-23);
**P17** per-domain API policy governance **Done** (2026-06-25; Wave 3); **P19**
verifiability/publish-gate **Done** (2026-06-25); **P20** i18n **Done** (2026-06-25).
**P14** confirmed large domains **Done** (2026-06-27). **P15** Kubernetes deployment **Done**
(2026-06-27; T01–T10). **P18** structured authoring **Done** (2026-06-28; T01–T10).
**Active formal phase: P21** (activated 2026-06-29; **sub-phase D cluster ④ Done** — T11; **all four role clusters complete**; phase **wrap-up** — next **P21-X03** P0 or **P21-X01/X05/X06**). P12 remains the deferred-enhancements
catch-all (non-active). See [execution-sync-ledger.md](./execution-sync-ledger.md).

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
| P15 | Kubernetes deployment & container hardening | [detail/P15-kubernetes-deployment-container-hardening.md](detail/P15-kubernetes-deployment-container-hardening.md) | Done (2026-06-27; T01–T10) |
| P16 | Template & version lifecycle governance completeness | [detail/P16-lifecycle-version-governance.md](detail/P16-lifecycle-version-governance.md) | Done (2026-06-23) |
| P17 | Per-domain API policy governance | [detail/P17-api-policy-domain-governance.md](detail/P17-api-policy-domain-governance.md) | Done (2026-06-25; Wave 3) |
| P18 | Structured authoring & rendering-fidelity engine | [detail/P18-structured-authoring-fidelity-engine.md](detail/P18-structured-authoring-fidelity-engine.md) | Done (2026-06-28; T01–T10) |
| P19 | Template verifiability, publish gate & decision forms | [detail/P19-verifiability-publish-gate.md](detail/P19-verifiability-publish-gate.md) | Done (2026-06-25; T01–T10) |
| P20 | i18n multi-locale readiness & UI upgradeability | [detail/P20-i18n-ui-upgradeability.md](detail/P20-i18n-ui-upgradeability.md) | Done (2026-06-25) |
| P21 | Role-journey frontend redesign & business-friendly terminology | [detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md) | In Progress (2026-06-29; clusters ①→④ Done; wrap-up — next P21-X) |

## First delivery target (thin vertical slice) — achieved

Login → upload approved master → create template → test → approve → publish →
call runtime API → receive DOCX. Mapped to **P0–P7 (minimal sync path)** — Done.

## Next focus

**User sequence P14 → P15 → P18 — complete** (2026-06-28).

| Phase | Summary | Evidence |
| --- | --- | --- |
| P14 | Content modules, collaboration, export/import | E2E/UIUX green; see [P14 detail](detail/P14-confirmed-large-domains.md) |
| P15 | K8s Helm, probes, blue-green, CI gates | [deploy/README.md](../deploy/README.md); helm-validate green |
| P18 | Structured authoring + fidelity engine + UI | `mvn verify` **524** tests; Vitest **250**; Playwright P18-T10 **5/5** + UIUX **1/1** |

**Active phase: P21** — role-journey frontend redesign & business-friendly terminology
(activated 2026-06-29; **sub-phase D cluster ④ Done** — T11; **all four role clusters complete**; phase **wrap-up** — next **P21-X03** (P0 permission fail-closed) or **P21-X01/X05/X06**) — see
[detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md)

**Open backlog (non-active):**

- **P12** — deferred enhancements (pick one slice when activating)
- **OPT-D / M9** — architecture/security debt (Redisson, QueryDSL expansion, intranet SCA) — see [optimization-plan.md](./optimization-plan.md)
- **Phase B** — multi-revision master history API — see [catalog-navigation-ux.md](../product/catalog-navigation-ux.md)

## Optimization backlogs

| Backlog | Lens |
| --- | --- |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
