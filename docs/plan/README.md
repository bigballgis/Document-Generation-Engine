# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** **Done** (2026-06-23);
**P17** per-domain API policy governance **Done** (2026-06-25; Wave 3); **P19**
verifiability/publish-gate **Done** (2026-06-25); **P20** i18n **Done** (2026-06-25).
**P14** confirmed large domains **Done** (2026-06-27). **P15** Kubernetes deployment **Done**
(2026-06-27; T01–T10). **P18** structured authoring **Done** (2026-06-28; T01–T10).
**Active formal phase / program:** **None** (2026-07-09). **CODE-QUALITY program Done** (CQ-01A…CQ-08; ArchUnit **11/11**); see [program entry](code-quality-program.md) and [task sheet](detail/CODE-QUALITY-code-hygiene.md). **CORE-FORTRESS program Done** (F1–F8; 2026-07-09; `mvn verify` **1154**). **Prior:** **CORE-FORTRESS F7 Done** (authoring UX; Vitest **894**; E2E **12/12**); **F6 Done** (frontend kernel refactor; composable **73** Vitest).

**Delivery focus note (2026-07-10):** **LR-A5 Done** (`lrp-a5-adr-closeout`; merge `cc9e5f6`; **ADR-0041 Accepted**; 0042/0043 remain Proposed) — **Wave LR-A Done** (Word/XSD/LO24 residuals deferred out of wave exit) — [LRP-A detail](detail/LRP-A-rendering-trust-hardening.md); [LRP program](launch-readiness-program.md). Formal phase remains **None**. **Sibling:** CDP Wave CD-2 **partial** (T01/T01b Done, merge `1930842`; T02–T12 Not Started) — [CDP program](competitiveness-deepening-program.md). **LRP:** LR-A1–A7 Done; do not start LR-C9 for deferred residuals.

**Ad-hoc slice (2026-07-10):** **[MGMT-UI-DEFECTS](detail/MGMT-UI-defects.md)** — Round 2 / P0 **Done**; **Round 3 / P1 depth governance Done** (`mgmt-ui-p1`; merge `180bffb`; worktree removed).

**Formal phase P23:** **[P23 Demo typography & layout excellence](detail/P23-demo-typography-layout-excellence.md)** — **Done** (2026-07-08; T01–T16; bank-grade Word output for all demo packages).
**Formal phase P22:** **Done** (2026-07-04) — rendering engine + demo scaffolds; [P22 detail](detail/P22-demo-expansion-rendering-fidelity.md).
**P21 Done** (2026-06-30; T01–T11 + X01–X06 + X02; backend **553**,
frontend **511** Vitest; **AUD-B10 resolved** via P12-AUD-B10; **AUD-M02 resolved** via P12-AUD-M02). Latest gates: backend `mvn verify` BUILD SUCCESS + frontend **643** Vitest (2026-07-03, P12-TEMPLATE-TESTING-OVERHAUL). See
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
| P12 | Deferred enhancements | [detail/P12-deferred-enhancements.md](detail/P12-deferred-enhancements.md) · [API package access (Done)](detail/P12-api-package-access-invocation-records.md) | **Not Started** — slices **P12-TEMPLATE-TESTING-OVERHAUL Done**, **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03) |
| P13 | Identity & group administration | [detail/P13-identity-group-administration.md](detail/P13-identity-group-administration.md) | Done |
| P14 | Confirmed large domains | [detail/P14-confirmed-large-domains.md](detail/P14-confirmed-large-domains.md) | Done (2026-06-27) |
| P15 | Kubernetes deployment & container hardening | [detail/P15-kubernetes-deployment-container-hardening.md](detail/P15-kubernetes-deployment-container-hardening.md) | Done (2026-06-27; T01–T10) |
| P16 | Template & version lifecycle governance completeness | [detail/P16-lifecycle-version-governance.md](detail/P16-lifecycle-version-governance.md) | Done (2026-06-23) |
| P17 | Per-domain API policy governance | [detail/P17-api-policy-domain-governance.md](detail/P17-api-policy-domain-governance.md) | Done (2026-06-25; Wave 3) |
| P18 | Structured authoring & rendering-fidelity engine | [detail/P18-structured-authoring-fidelity-engine.md](detail/P18-structured-authoring-fidelity-engine.md) | Done (2026-06-28; T01–T10) |
| P19 | Template verifiability, publish gate & decision forms | [detail/P19-verifiability-publish-gate.md](detail/P19-verifiability-publish-gate.md) | Done (2026-06-25; T01–T10) |
| P20 | i18n multi-locale readiness & UI upgradeability | [detail/P20-i18n-ui-upgradeability.md](detail/P20-i18n-ui-upgradeability.md) | Done (2026-06-25) |
| P21 | Role-journey frontend redesign & business-friendly terminology | [detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md) | Done (2026-06-30; T01–T11 + X01–X06 + X02; AUD-B10 resolved P12-AUD-B10 Done 2026-07-01; AUD-M02 resolved P12-AUD-M02 Done 2026-07-01) |
| P22 | Demo expansion & rendering fidelity | [detail/P22-demo-expansion-rendering-fidelity.md](detail/P22-demo-expansion-rendering-fidelity.md) | **Done** (2026-07-04; engine + scaffolds; typography → P23) |
| P23 | Demo document typography & layout excellence | [detail/P23-demo-typography-layout-excellence.md](detail/P23-demo-typography-layout-excellence.md) | **Done** (2026-07-08; T01–T16; POI + E2E + human checklist template) |

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

**In-flight work:**
- **CDP Wave CD-2 In Progress** (2026-07-10) — **partial** (CD-E2E-T01/T01b **Done**; T02–T12 Not Started); **CD-0 Done** — [program](./competitiveness-deepening-program.md); [detail](detail/CDP-e2e-full-chain-evidence.md); merge `1930842`
- **LRP Wave LR-A Done** (2026-07-10) — A1–A7 Done; **ADR-0041 Accepted**; 0042/0043 remain Proposed; Word/XSD/LO24/0042-Accepted/0043-Accepted residuals deferred out of wave exit — [program](./launch-readiness-program.md); [detail](detail/LRP-A-rendering-trust-hardening.md); [corpus](./pagination-delta-corpus.md)
- **CODE-QUALITY Done** (2026-07-09) — code hygiene & structural consistency ([program](code-quality-program.md); [detail](detail/CODE-QUALITY-code-hygiene.md))
- **CORE-FORTRESS Done** (2026-07-09; F1–F8 — [program roadmap](detail/CORE-FORTRESS-program-roadmap.md))
- **P23 Done** (2026-07-08) — demo typography & layout excellence ([plan](detail/P23-demo-typography-layout-excellence.md); T01–T16; human reviewer sign-off operational follow-up)

**P21 Done** — closure evidence in [detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md).

**Open backlog (non-active slices):**
- **SOR-0…7** — consolidated system optimization review (2026-07-03; 52 tasks, all Not Started) — see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)
- **OPT-D / M9** — architecture/security debt (Redisson, QueryDSL expansion, intranet SCA) — see [optimization-plan.md](./optimization-plan.md)
- **OPT-G6 (partial)** — brand rename Done via P12-UIUX-DEEP-REFACTOR A1; remaining aria-labels/locale-aware dates deferred
- ~~**Phase B** — multi-revision master history API~~ → **Done (P2-T06, 2026-07-01)** — see [P2 detail](detail/P2-master-management.md) § P2-T06 and [catalog-navigation-ux.md](../product/catalog-navigation-ux.md)

## Optimization backlogs

| Backlog | Lens |
| --- | --- |
| **[code-quality-program.md](./code-quality-program.md)** | **CODE-QUALITY program Done (2026-07-09):** behavior-preserving hygiene — module boundaries, god-class extraction, DRY demo/PDF/E2E infra (`CQ-*`) |
| **[system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)** | **Consolidated optimization review (SOR-0…7, 2026-07-04 closeout)** — open hygiene items superseded by **CODE-QUALITY** where duplicated |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [launch-readiness-program.md](./launch-readiness-program.md) | Launch-readiness deep optimization — production pitfalls + usability deepening (LR-A…E; sibling program; LR-C1/C4 Done via F7) |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [spotbugs-exclusion-ratchet.md](./spotbugs-exclusion-ratchet.md) | SpotBugs exclude.xml ratchet (SOR-A05): baseline `<Match>` count, banned patterns, EI slice cadence |
| [coverage-ratchet-plan.md](./coverage-ratchet-plan.md) | JaCoCo / Vitest bundle floors (SOR-C02) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
