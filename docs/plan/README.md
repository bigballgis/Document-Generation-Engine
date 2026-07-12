# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** **Done** (2026-06-23);
**P17** per-domain API policy governance **Done** (2026-06-25; Wave 3); **P19**
verifiability/publish-gate **Done** (2026-06-25); **P20** i18n **Done** (2026-06-25).
**P14** confirmed large domains **Done** (2026-06-27). **P15** Kubernetes deployment **Done**
(2026-06-27; T01–T10). **P18** structured authoring **Done** (2026-06-28; T01–T10).
**Active formal phase / program:** **None** (2026-07-09). **CODE-QUALITY program Done** (CQ-01A…CQ-08; ArchUnit **11/11**); see [program entry](code-quality-program.md) and [task sheet](detail/CODE-QUALITY-code-hygiene.md). **CORE-FORTRESS program Done** (F1–F8; 2026-07-09; `mvn verify` **1154**). **Prior:** **CORE-FORTRESS F7 Done** (authoring UX; Vitest **894**; E2E **12/12**); **F6 Done** (frontend kernel refactor; composable **73** Vitest).

**Delivery focus note (2026-07-12):** **LR-E2 → Done** (slice `lrp-e2-launch-checklist`; merge `ae39fbb`; feature tip `57e9133`; Task Master #43; BDD **not-applicable**; [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md); overall **NO-GO**). **Wave LR-E → Done** (E1+E2 docs exit gate — **not** production go-live). **LRP planned waves A–E → Done**; **no sole-active**. Formal phase remains **None**. Do **not** claim production go-live. Do **not** activate CD-3. Do **not** touch `DGE-audit-governance`. Recommend next (notes only): **pause**. **Prior:** **LR-E1 → Done** (slice `lrp-e1-sse-proxy-e2e`; merge `575d0aa`; Task Master #42; closes CD-PIT-12 browser proof; Playwright 2/2; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; DEPLOY_OK). **Prior:** **Wave LR-D → Done** (D1–D7 all Done; merge tip `218dcf1`). **LR-D4 → Done** (slice `lrp-d4-trace-propagation`; merge `218dcf1`; feature tip `670a683`; Task Master #41; BDD not-applicable; ADR-0049 Accepted; `MdcTaskDecorator` + Kafka `X-Trace-Id`; Scenario A/B; mvn verify GREEN 1321; architecture PASS_WITH_NOTES; DEPLOY_OK 2026-07-12T04:18:13+08:00 healthz 200; X-Trace-Id echo OK). **LR-D3 → Done** (merge `ba5ea2e`; Task Master #40). **LR-D2 → Done** (merge `3d78bc5`; Task Master #39). **LR-D5 → Done** (merge `5b13476`; Task Master #38; NFR proposals pending confirmation — **not** confirmed SLOs). **LR-D6 → Done** (merge `56383eb`; Task Master #37). **LR-D7 → Done** (merge `c94a356`; Task Master #36; seam closed). **LR-D1 → Done** (merge `20b2a76`; Task Master #35; ADR-0048 Accepted; V54). **Prior:** **Wave LR-C → Done** (C1–C13; merge tip `bf9cbeb`). **Prior:** **LR-C8 → Done** (merge `bf9cbeb`; Task Master #34). **Prior:** **LR-C7 → Done** (slice `lrp-c7-notification-center`; merge `879108c`; Task Master #33; E2E 5/5; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES). **Prior:** **LR-C5 → Done** (slice `lrp-c5-catalog-pagination`; merge `5543a33`; feature `cfab79e`; Task Master #31; E2E 6/6; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES; OPT-F4 residual closed). **Prior:** **LR-C3 → Done** (slice `lrp-c3-editor-undo-redo`; merge `0cf553b`; Task Master #30; E2E 7/7; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES). **Prior:** **LR-C2 → Done** (slice `lrp-c2-local-draft-recovery`; merge `12a6a7e`; Task Master #29; E2E 4/4; UIUX PASS_WITH_NOTES; architecture PASS_WITH_NOTES). **Prior:** **LR-C13 → Done** (slice `lrp-c13-frontend-eng-debt`; merge `45addd6`; feature `5c1d23c`; Task Master #28; OPT-G4/G5 closed; coverage floors 80/55/80/80). **Prior:** **LR-C12 → Done** (slice `lrp-c12-keyboard-a11y`; merge `0357a16`; feature `e207e28`; Task Master #27). **CDP Wave CD-2 → Done** (T01/T01b + T02–T13; merges `1930842` / `6821f45` / `895f16e` / `3aed175` / `c62b1a1` / `1eb230b` / `55a6ab6` / `b16e52a` / `6e3f825` / `f12b193` / `b2b0899`; all matrix manifests PASS; **no sole-active CDP slice**). **Prior:** **LR-C11 → Done** (slice `lrp-c11-api-error-i18n`; merge `44fcf40`; `api.error` catalog parity **159/159** + parity Vitest; Task Master #26). **Prior:** **LR-C10 → Done** (slice `lrp-c10-upload-ux`; merge `bdaf95d`). **Prior:** **CD-E2E-T13 → Done** (slice `cdp-e2e-t13-materialize`; merge `b2b0899`). **Prior:** **CD-E2E-T12 → Done** (slice `cdp-e2e-t12-i18n-brands`; merge `f12b193`; zh-CN + REDBC/GREENBC dual-brand golden screenshots @1920; BDD-CDP-I18N-001/002). **Prior:** **CD-E2E-T11 → Done** (slice `cdp-e2e-t11-audit`; merge `6e3f825`; Audit admin query/filter + export; BDD-CDP-AUDIT-001/002). **Prior:** **CD-E2E-T10 → Done** (slice `cdp-e2e-t10-fidelity`; merge `b16e52a`; Fidelity «viewed» confirmation; BDD-CDP-FID-001…004). **Prior:** **CD-E2E-T09 → Done** (slice `cdp-e2e-t09-compare`; merge `55a6ab6`; Preview vs final comparison; BDD-CDP-CMP-001). **Prior:** **CD-E2E-T07 → Done** (slice `cdp-e2e-t07-api-policy`; merge `1eb230b`). **Prior:** **CD-E2E-T08 → Done** (slice `cdp-e2e-t08-preview`; merge `c62b1a1`). **Prior:** **CD-E2E-T06 → Done** (slice `cdp-e2e-t06-master`; merge `3aed175`). **Prior:** **CD-E2E-T05 → Done** (slice `cdp-e2e-t05-publish`; merge `895f16e`). **Prior:** **LR-C9 → Done** (`lrp-c9-load-error-panel`; merge `0013615`) — [LRP-C detail](detail/LRP-C-usability-deepening.md); [LRP program](launch-readiness-program.md). **Prior:** Wave LR-A **Done** (A1–A7; merge `cc9e5f6`; **ADR-0041 Accepted**; Word/XSD/LO24 residuals deferred). CDP program: [competitiveness-deepening-program.md](competitiveness-deepening-program.md).

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
- **CDP Wave CD-2 Done** (2026-07-11) — CD-E2E-T01–T13 Done, merges `1930842` / `6821f45` / `895f16e` / `3aed175` / `c62b1a1` / `1eb230b` / `55a6ab6` / `b16e52a` / `6e3f825` / `f12b193` / `b2b0899`; **CD-0 Done** — [program](./competitiveness-deepening-program.md); [detail](detail/CDP-e2e-full-chain-evidence.md)
- **LRP Wave LR-A Done** (2026-07-10) — A1–A7 Done; **ADR-0041 Accepted**; 0042/0043 remain Proposed; Word/XSD/LO24/0042-Accepted/0043-Accepted residuals deferred out of wave exit — [program](./launch-readiness-program.md); [detail](detail/LRP-A-rendering-trust-hardening.md); [corpus](./pagination-delta-corpus.md)
- **CODE-QUALITY Done** (2026-07-09) — code hygiene & structural consistency ([program](code-quality-program.md); [detail](detail/CODE-QUALITY-code-hygiene.md))
- **CORE-FORTRESS Done** (2026-07-09; F1–F8 — [program roadmap](detail/CORE-FORTRESS-program-roadmap.md))
- **P23 Done** (2026-07-08) — demo typography & layout excellence ([plan](detail/P23-demo-typography-layout-excellence.md); T01–T16; human reviewer sign-off operational follow-up)

**P21 Done** — closure evidence in [detail/P21-role-journey-frontend-redesign.md](detail/P21-role-journey-frontend-redesign.md).

**Open backlog (non-active slices):**
- **SOR-0…7** — consolidated system optimization review (2026-07-03; 52 tasks, all Not Started) — see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)
- **OPT-D / M9** — architecture/security debt (Redisson, QueryDSL expansion, intranet SCA) — see [optimization-plan.md](./optimization-plan.md)
- **OPT-G6 (partial)** — brand rename Done via P12; `logoSlotLabel` proper-noun exempt (LR-C11); locale dates + catalog residual **closed by LR-C11** (merge `44fcf40`); **aria-label sweep remains**
- **OPT-G7 Done** — `api.error` frontend/backend parity **159/159** + Vitest (LR-C11 Done 2026-07-11)
- ~~**Phase B** — multi-revision master history API~~ → **Done (P2-T06, 2026-07-01)** — see [P2 detail](detail/P2-master-management.md) § P2-T06 and [catalog-navigation-ux.md](../product/catalog-navigation-ux.md)

## Optimization backlogs

| Backlog | Lens |
| --- | --- |
| **[code-quality-program.md](./code-quality-program.md)** | **CODE-QUALITY program Done (2026-07-09):** behavior-preserving hygiene — module boundaries, god-class extraction, DRY demo/PDF/E2E infra (`CQ-*`) |
| **[system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md)** | **Consolidated optimization review (SOR-0…7, 2026-07-04 closeout)** — open hygiene items superseded by **CODE-QUALITY** where duplicated |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [launch-readiness-program.md](./launch-readiness-program.md) | Launch-readiness deep optimization — production pitfalls + usability deepening (LR-A…E; sibling program; **Wave LR-C Done**; **Wave LR-D Done** — D1–D7; merge tip `218dcf1`; **Wave LR-E Done** — E1 merge `575d0aa` #42; E2 merge `ae39fbb` #43; docs gate; checklist **NO-GO**; **LRP planned waves → Done**) |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [spotbugs-exclusion-ratchet.md](./spotbugs-exclusion-ratchet.md) | SpotBugs exclude.xml ratchet (SOR-A05): baseline `<Match>` count, banned patterns, EI slice cadence |
| [coverage-ratchet-plan.md](./coverage-ratchet-plan.md) | JaCoCo / Vitest bundle floors (SOR-C02) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
