# Plan Layer Index

Execution truth for this project lives here.

**Current baseline:** P0–P11 re-earned **Done** (2026-06-23); **P13** **Done** (2026-06-23);
**P17** per-domain API policy governance **Done** (2026-06-25; Wave 3); **P19**
verifiability/publish-gate **Done** (2026-06-25); **P20** i18n **Done** (2026-06-25).
**P14** confirmed large domains **Done** (2026-06-27). **P15** Kubernetes deployment **Done**
(2026-06-27; T01–T10). **P18** structured authoring **Done** (2026-06-28; T01–T10).
**Delivery focus note (2026-07-17):** **#101** PRR-A04 → **Done** (merge `5b705f56` / feature `834ca1a6`; worktree removed; BDD **ready** [prod-library-export-streaming.md](../behavior/prod-library-export-streaming.md); FE/E2E N/A); prior **PRR Wave A** **#98+#99+#100** → **Done** (`4197770f`); **sole-active cleared**; **next queue head (not activated):** **#102** PRR-B01 Wave B prod contract/LDAP (**pending**); then **#103** PRR-C01 / **#104** PRR-D01; do **not** flip checklist **#3b**. **CORE-EXCELLENCE (CE)** — **#77** CE-G05 → **Done** (merge `c3f6a288` / feature `744b628a`; worktree removed; BDD **ready** [ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md); FE+E2E); **P3 queue empty** (#96/#97/#80/#77 all Done); umbrella **#53** remains **in-progress** (program registry only — **do not** claim program Done); **#80** CE-E03 → **Done** (merge `f1f02554` / feature `86e4ff10`; worktree removed; BDD **ready** [ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md); API-first FE/E2E N/A); **#97** CE-U19 → **Done** (merge `e4679421` / feature `90a9e5cd`; worktree removed); **#96** CE-U17 → **Done** (merge `4fc2dbdb` / feature `d3293db1`; worktree removed) — do **not** claim program Done; **P2 CE continuum complete** — **#95** CE-U21 → **Done** (merge `8d8f6f6d` / feature `292fbc35`; worktree removed); **#94** CE-U20 → **Done** (merge `b9327a11` / feature `b4b0b420`; worktree removed); **#93** CE-U18 → **Done** (merge `05e9f8e1` / feature `e05407f2`; worktree removed); **#92** CE-U16 → **Done** (merge `5d683c40` / feature `1a3d0f20`; worktree removed); **#91** CE-U15 → **Done** (merge `b2968052`; closeout `ed8a15e6`; worktree removed); **#90** CE-U14 → **Done** (merge `05e845e4` / feature `09e0d251`); **#50** Vitest 3.2.7 → **Done** (`6c8fff7d`; GHSA-5xrq-8626-4rwp **CLOSED**); **#75** CE-G04 → **Done** (merge tip `42745ea5` / feature `b47ea896`; worktree removed); **#81** CE-O01 → **Done** (merge `e081bcfa`; worktree removed); **#79** CE-E02 → **Done** (merge `5bd3611e`) — do **not** reopen; **#78** CE-E01 → **Done** (merge `6ae57974`) — do **not** reopen; **#76** CE-G06 → **Done** (merge `d8636232`) — do **not** reopen; **#71** CE-C06 → **Done** (merge `35f6f47d`) — do **not** reopen; **#89** CE-U13 → **Done** (merge `ccdfacda`); **#74** CE-G03 → **Done** (merge `50c1a524`); **#62** CE-K06 → **Done** (K06a `485a7f3e` + K06b `a689ca87` + K06c tip `76297d08`); **#69** CE-C04 / **#70** CE-C05 / **#88** CE-U06 → **Done** (merges `c7be8305` / `405f7cea` / `7734366e`); Wave 0 #61/#86/#87 **Done**; umbrella **#53** remains **in-progress** (program registry only); [core-excellence-program-2026-07.md](core-excellence-program-2026-07.md). Prior waves closed (#60/#84/#85; #59/#68/#83). **Batch 4 remains Done**. Batch 1–3 Done. Formal phase remains **None**; **not** go-live; do **not** activate CD-3; do **not** invent a formal P-phase. Overall checklist remains **NO-GO** (blocking **#3b**; #5a/#10 **CONDITIONAL**). **Scaffold hygiene (2026-07-14):** slice `cursor-scaffold-hygiene` — Cursor-only agent/docs; ADR-0055; see ledger. **Prior:** **BOOT-4-1-UPGRADE → Done** (Task Master **#51**; merge `993c287`). **CDP Wave CD-2 Done** (no CDP wave In Progress). **LRP A–E Done**.

**Active formal phase / program:** **None** (2026-07-09). **CODE-QUALITY program Done** (CQ-01A…CQ-08; ArchUnit **11/11**); see [program entry](code-quality-program.md) and [task sheet](detail/CODE-QUALITY-code-hygiene.md). **CORE-FORTRESS program Done** (F1–F8; 2026-07-09). **CORE-EXCELLENCE** is the active delivery program (not a formal P-phase). **LRP waves A–E → Done**. **CDP Wave CD-2 → Done** (T01–T13; **no CDP wave In Progress**). Historical LR-C/D/E and CD-E2E slice evidence: [execution-sync-ledger.md](./execution-sync-ledger.md) · [LRP program](launch-readiness-program.md) · [CDP program](competitiveness-deepening-program.md).

**Ad-hoc slice (2026-07-16):** **ORCH-AGENT-ENUM-DOCS → Done** — align AGENTS /
MODEL-STRATEGY / inline-agent wording with retry-first runtime policy. Formal phase
**None**. **#81** CE-O01 is **Done** (`e081bcfa`).

**Ad-hoc slice (2026-07-16):** **ORCH-SPECIALIST-RETRY-THEN-GP → Done** — retry ≤3
then GP under contract (`orch-specialist-retry-then-gp`;
[specialist-runtime-fallback.md](../behavior/specialist-runtime-fallback.md);
[orch-specialist-fallback-governance.md](./orch-specialist-fallback-governance.md)).
Forbid: `禁止降级` / `no-gp-fallback`. Early opt-in: `允许降级` / `allow-gp-fallback`.
Formal phase **None**. Placement **MAIN** (`main-only`). Committed with CE-G04 closeout.

**Ad-hoc slice (2026-07-16):** **ORCH-SPECIALIST-RETRY-ONLY → superseded** by
retry-then-GP policy above.

**Ad-hoc slice (2026-07-16):** **ORCH-SPECIALIST-FALLBACK → superseded** (historical).

**Ad-hoc slice (2026-07-16):** **ORCH-BATCH-RECOMMEND → Done** — pre-0 Batch Recommendation
governance (`orch-batch-recommend`; skill + constitution + BDD
[delivery-batch-recommend.md](../behavior/delivery-batch-recommend.md);
note [orch-batch-recommend-governance.md](./orch-batch-recommend-governance.md)). Formal phase
remains **None**. **#81** CE-O01 → **Done** (`e081bcfa`; not activated by this governance slice). Do **not** invent a P-phase.

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
| **[core-excellence-program-2026-07.md](./core-excellence-program-2026-07.md)** | **Core Excellence（CE-K/U/C/G/E/O）** — Task Master **#53–#97**; **#77** CE-G05 **Done** (`c3f6a288` / `744b628a`); **P3 queue empty**; umbrella **#53** in-progress (not program Done); **host sole-active cleared** (#101 PRR-A04 Done `5b705f56`; Wave A `4197770f`; next **#102** PRR-B01 not activated); **Batch 1–4 Done**; Wave 0 #61/#86/#87 **Done**; formal phase **None** (optimization backlog, not a P-phase) |
| **[orch-batch-recommend-governance.md](./orch-batch-recommend-governance.md)** | **Agent governance (2026-07-16)** — pre-0 Batch Recommendation (`merge`\|`solo`\|`split`); not a P-phase; #81 CE-O01 later **Done** |
| **[comprehensive-optimization-roadmap.md](./comprehensive-optimization-roadmap.md)** | **Unified prioritized map (COR-0…6): docs sync, API contract, template workflow, frontend UX, performance, E2E, P14–P20 — start here (2026-06-23)** |
| [launch-readiness-program.md](./launch-readiness-program.md) | Launch-readiness deep optimization — production pitfalls + usability deepening (LR-A…E; sibling program; **Wave LR-C Done**; **Wave LR-D Done** — D1–D7; merge tip `218dcf1`; **Wave LR-E Done** — E1 merge `575d0aa` #42; E2 merge `ae39fbb` #43; docs gate; checklist **NO-GO**; **LRP planned waves → Done**) |
| [optimization-plan.md](./optimization-plan.md) | Technical debt detail: quality gates, coverage, backend architecture/security/performance (OPT-A…G) |
| [spotbugs-exclusion-ratchet.md](./spotbugs-exclusion-ratchet.md) | SpotBugs exclude.xml ratchet (SOR-A05): baseline `<Match>` count, banned patterns, EI slice cadence |
| [coverage-ratchet-plan.md](./coverage-ratchet-plan.md) | JaCoCo / Vitest bundle floors (SOR-C02) |
| [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); verify Done claims against comprehensive roadmap |
