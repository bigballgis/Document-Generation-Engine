# Master Plan

**Baseline:** Project restart from zero — 2026-06-23  
**Active formal phase / program:** **None** (2026-07-09+). **Delivery focus note (2026-07-18):** **#102** PRR-B01 true production contract (Wave B item 5) → **Done** (MAIN merge `40e264e7`; worktree removed; BDD **ready** [prod-true-prod-contract.md](../behavior/prod-true-prod-contract.md) (**BDD-PRR-B01-TPC**); `frontend_ui_in_scope=false`; gates: `mvn verify` GREEN, arch **PASS_WITH_NOTES**, **DEPLOY_OK** LAB stack). **Sole-active cleared**. Formal phase remains **None**; **not** go-live; do **not** flip checklist **#3b** / **#5a GO** (#5a path text → `docker-compose.lab.yml`; verdict stays **CONDITIONAL**). Prior **#101** PRR-A04 → **Done** (`5b705f56`); PRR Wave A **#98+#99+#100** → **Done** (`4197770f`). **Serial queue:** **#105** LDAP CONDITIONAL (**next head, not activated**) → **#103** PRR-C01 ADR-0042/0043 → **#104** PRR-D01 ops hardening. Prior: **#50** Vitest 3.2.7 upgrade → **Done** (merge `6c8fff7d`; worktree removed; BDD **not-applicable** tooling; GHSA-5xrq-8626-4rwp **CLOSED**; ADR-0029 Vitest **3.x**). **CORE-EXCELLENCE (CE)** — delivery program, not a formal P-phase — **#77** CE-G05 → **Done** (merge `c3f6a288` / feature `744b628a`; worktree removed; BDD **ready** [ce-g05-annual-review-fts.md](../behavior/ce-g05-annual-review-fts.md); FE+E2E); **P3 queue empty** (#96/#97/#80/#77 all Done); umbrella **#53** remains **in-progress** (program registry only — **do not** claim program Done; **not** a delivery leaf); **#80** CE-E03 → **Done** (merge `f1f02554` / feature `86e4ff10`; worktree removed; BDD **ready** [ce-e03-full-library-export.md](../behavior/ce-e03-full-library-export.md); API-first FE/E2E N/A); **#97** CE-U19 → **Done** (merge `e4679421` / feature `90a9e5cd`; worktree removed); **#96** CE-U17 → **Done** (merge `4fc2dbdb` / feature `d3293db1`; worktree removed) — do **not** claim program Done. **P2 CE continuum complete** — **#95** CE-U21 → **Done** (merge `8d8f6f6d` / feature `292fbc35`; worktree removed); **#94** CE-U20 → **Done** (merge `b9327a11` / feature `b4b0b420`; worktree removed). **#93** CE-U18 → **Done** (merge `05e9f8e1` / feature `e05407f2`; worktree removed). **#92** CE-U16 → **Done** (merge `5d683c40` / feature `1a3d0f20`; worktree removed). **#91** CE-U15 → **Done** (merge `b2968052`; closeout `ed8a15e6`; worktree removed). **#90** CE-U14 → **Done** (merge `05e845e4` / feature `09e0d251`). **#75** CE-G04 → **Done** (merge tip `42745ea5` / feature `b47ea896`; worktree removed). **#81** CE-O01 → **Done** (merge `e081bcfa`; worktree removed). **#79** CE-E02 → **Done** (merge `5bd3611e`) — do **not** reopen. **#78** CE-E01 → **Done** (merge `6ae57974`) — do **not** reopen. **#76** CE-G06 → **Done** (merge `d8636232`) — do **not** reopen. **#71** CE-C06 → **Done** (merge `35f6f47d`) — do **not** reopen. **#89** CE-U13 → **Done** (merge `ccdfacda`). **#74** CE-G03 → **Done** (merge `50c1a524`). **#63** CE-K08 → **Done** (`35f57503` on main). **#62** CE-K06 → **Done** (K06a `485a7f3e` + K06b `a689ca87` + K06c tip `76297d08`). **#69** CE-C04 → **Done** (`c7be8305`). **#70** CE-C05 → **Done** (`405f7cea`). **#88** CE-U06 → **Done** (`7734366e`). Wave 0 closed — **#61 CE-K05** + **#86 CE-U11** + **#87 CE-U12** → **Done** (merges `51a58f12` / `513d776c` / tip `34353b75`); umbrella **#53** remains **in-progress** (program registry only — not a delivery leaf). Prior wave closed: **#60 CE-K04** + **#84 CE-U09** + **#85 CE-U10** → **Done**. Prior wave closed: **#59 CE-K03** / **#68 CE-C03** / **#83 CE-U08** → **Done**. **Batch 4 remains Done**. Formal phase remains **None**; **not** go-live; do **not** activate CD-3; do **not** activate CE-O02. Overall checklist remains **NO-GO** (blocking: **#3b**).
**P14** confirmed large domains **Done** (2026-06-27).
**P15** Kubernetes deployment **Done** (2026-06-27; T01–T10).
**P18** structured authoring **Done** (2026-06-28; T01–T10).
UX-A…UX-F interaction/upgradeability tasks (same plan) run as optimization waves
against the existing implementation. **UX Wave A** (role gating + half-built interactions)
re-earned Done on 2026-06-23; **UX Wave B** (workbenches + polish) Done (2026-06-23).
**P22 Done** (2026-07-03) — **P22-DEMO-EXPANSION** closed (T01–T15; gates **GREEN**). **P12 catch-all → Not Started** (slice **P12-API-PACKAGE-ACCESS-INVOCATION Done** 2026-07-03).
**P16–P20 (deep-review functional gaps, 2026-06-23):** P16 lifecycle governance **Done**;
P17 per-domain API policy **Done** (2026-06-25; Wave 3; T01–T09 + COR-F18);
P18 structured authoring **Done** (2026-06-28; T01–T10); **P19 verifiability/publish-gate Done**
(2026-06-25); P20 i18n **Done** (2026-06-25).
**P21 role-journey frontend redesign & business-friendly terminology — Done**
(activated 2026-06-29; closed 2026-06-30; T01–T11 + X01–X06 + **X02** governance close; backend **553**, frontend **511** Vitest; **AUD-B10 resolved** via **P12-AUD-B10 Done** 2026-07-01; **AUD-M02 resolved** via **P12-AUD-M02 Done** 2026-07-01).

## Product goal

Build an enterprise low-code document generation platform for bank correspondence:
template lifecycle, dynamic generation API, API authorization, DOCX/PDF output with
optional dynamic encryption. v1 does not provide a customer-facing generation portal;
upstream systems invoke the dynamic API.

## Delivery strategy

1. **Thin vertical slice first** — one end-to-end path before breadth.
2. **Test-first** — behavior spec → failing test → implementation → green gates.
3. **Document-as-code** — update docs with or before behavior changes.
4. **English-first i18n** — all user-facing strings via message keys.

## Phase roadmap

| Phase | Name | Depends on | Exit criteria (summary) | Status |
| --- | --- | --- | --- | --- |
| P0 | Foundation & guardrails | — | Runnable skeleton, docker-compose, CI gates, OpenAPI contract test harness | Done |
| P1 | Login & session | P0 | Real local auth, seeded users, role/group session, login-first shell entry | Done |
| P2 | Master management | P1 | Upload DOCX master, anchor catalog, lightweight review, group isolation | Done |
| P3 | Template authoring | P2 | Create template from approved master, variables, structured content, rules | Done |
| P4 | Rendering & preview | P3 | DOCX/PDF render, fidelity warnings/blockers, preview records | Done |
| P5 | Lifecycle governance | P4 | Test → approve → publish with audit; **thin-slice** publish gate checklist (live gates → P19) | Done |
| P6 | API management | P5 | Credentials, AD Group policy, output/batch/encryption/default route | Done |
| P7 | Runtime dynamic API | P5, P6 | OpenAPI v1 operations callable with auth, idempotency, sync/async/batch | Done |
| P8 | Audit & contract visibility | P5, P6, P7 | Role-scoped audit console; caller contract view | Done |
| P9 | Production readiness | P0–P8 | Security scans, observability, deployment evidence, release gates | Done |
| P10 | Runtime document download | P9 | Secure download with secondary auth and 15-minute expiry | Done |
| P11 | Batch & async generation | P10 | Sync batch, async task query/cancel | Done |
| P12 | Deferred enhancements | P0–P11 | Catch-all; slice **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03 — T01–T12; Playwright **10/10**); prior **P12-TEMPLATE-TESTING-OVERHAUL Done** (2026-07-03) | Not Started |
| P13 | Identity & group administration | P1 | Global/group admins manage users & groups via management API + UI, with fail-closed escalation protection, audit, and green gates | Done |
| P14 | Confirmed large domains | P2–P8 | Clause/content modules, collaboration to-dos + timeout escalation, template export/import (UX-G) | Done (2026-06-27; T01–T03 + E2E/UIUX/architecture gates; backend **481**, frontend **235+**) |
| P15 | Kubernetes deployment & container hardening | P9 | Distroless non-root read-only containers, Helm/manifests for app workloads, ConfigMap/Secret, NGINX Ingress + cert-manager TLS, default-deny NetworkPolicy, HPA (CPU/mem + custom), /healthz+/readyz probes, blue-green + manual approval/rollback, CI manifest validation (ADR-0030) | Done (2026-06-27; T01–T10) |
| P16 | Template & version lifecycle governance completeness | P5 | Stop/restore/deprecate template + version deactivate/restore, recovery/deprecate impact preview, reason + secondary confirm, logical-delete only, audit (gap G1) | Done (2026-06-23) |
| P17 | Per-domain API policy governance | P6 | Config-domain save (AD group/output/batch/encryption/default-route), impact preview (hard-block vs warning), policyVersion lineage, rollback, default-route governance, API_POLICY_UPDATED audit (gap G2) | Done (2026-06-25; T01–T09 + COR-F18; 308 backend tests) |
| P18 | Structured authoring & rendering-fidelity engine | P3, P4 | Controlled node matrix, master style catalog + limited direct format, table component, seal/QR/attachment nodes, controlled numbering, Word/HTML paste cleaning, publish-locked renderProfile, fidelity blockers/warnings (gap G3) | Done (2026-06-28; P18-T01–T10) |
| P19 | Template verifiability, publish gate & decision forms | P3, P4, P5 | Multi-sample coverage thresholds, batch test, change-diff, preview comparison, live publish-gate checklist, controlled test/approval opinion forms + risk prompts + exception markers (gaps G4, G5) | Done (2026-06-25; T01–T10) |
| P20 | i18n multi-locale readiness & UI upgradeability | P1 | Locale registry/switcher/fallback + html lang, config-driven brand theming, environment selector (gap G6, i18n constitution) | Done (2026-06-25) |
| P21 | Role-journey frontend redesign & business-friendly terminology | P13, P14, P19, P20 | Hybrid IA (single task hub authoritative + behavior-typed "my to-dos" entries + per-role `RoleJourneyTimeline`), task-hub queue partitioning + restored fields + inline open actions, 6 collaboration-trigger backend completeness (+ `RESOLVED`), foreign-bank non-IT persona business-friendly L1 copy (en/zh, keys stable); **plus code-grounded audit remediation** — permission single-source/fail-closed, template-detail bug fixes (focus/tab, stale id), APPROVAL dual-substate, i18n parity, a11y focus ring, capability/route/OpenAPI drift; ADR Batch B / COR-T11 not violated | Done (2026-06-30; T01–T11 + X01–X06 + X02; backend **553**, frontend **511** Vitest; **AUD-B10 resolved** P12-AUD-B10 Done 2026-07-01; **AUD-M02 resolved** P12-AUD-M02 Done 2026-07-01) |
| P22 | Demo expansion & rendering fidelity | P3, P4, P18 | Structured content DOCX writer, dual page numbering, eight `deploy/demo-*` **scaffolds**, `import-all-demos.ps1` (R1–R5 engine + structure; **content/typography excellence → P23**) | **Done** (2026-07-04; T01–T15; gates **GREEN**; T05–T11 scaffold carry-forward) |
| P23 | Demo document typography & layout excellence | P22, P18, LRP LR-A2 | Bank-grade master styles/fonts/margins/headers-footers; rich structured bindings (not placeholder text); all 8+ demos at foreign-bank-letter grade; POI + E2E + human typography checklist; taskmaster 4–8 | **Done** (2026-07-08; T01–T16; gates **GREEN**; human reviewer sign-off template published — execution pending) |
| CORE-FORTRESS F1 | Rendering core correctness | P23, P22, P18 | Unified structured→DOCX engine; fail-closed module/image refs | **Done** (2026-07-09) |
| CORE-FORTRESS F2 | Runtime lightweight | F1 | Publish fidelity cache; lifecycle bulk; idempotency hash cache | **Done** (2026-07-09) |
| CORE-FORTRESS F3 | Node matrix + expression engine | F2 | Rich conditionals; matrix validation hardening | **Done** (2026-07-09) |
| CORE-FORTRESS F4 | Production rendering hardening | F3 | LO pool, fonts, pagination | **Done** (2026-07-09; targeted **23/23**; full `mvn verify` env caveat) |
| CORE-FORTRESS F5 | Async durability + security depth | F4 | Kafka retry/DLT depth; fail-closed security seams | **Done** (2026-07-09; F5 targeted **31/31**; full `mvn verify` env caveat) |
| CORE-FORTRESS F6 | Frontend kernel refactor | F5 | Composable decomposition; zero behavior change; Bank OA lock | **Done** (2026-07-09; controller **243** lines; composable **73** Vitest; F6-T08 E2E **9/9**) |
| CORE-FORTRESS F7 | Authoring UX (dirty guard, side-by-side preview) | F5, F6 | `useDirtyGuard`; side-by-side preview; LR-C1/C4 | **Done** (2026-07-09; Vitest **894**; E2E **12/12**) |
| CORE-FORTRESS F8 | Observability, SLO, DR, evidence bundle | F1–F7 | Micrometer SLOs; deep readiness; DR playbook; evidence bundle | **Done** (2026-07-09; `mvn verify` **1154**; arch review PASS) |
| CODE-QUALITY | Code hygiene & structural consistency | CORE-FORTRESS F1–F8 | Module boundary decoupling; god-class extraction; DRY demo/PDF/E2E infra; naming normalization — zero behavior change | **Done** (2026-07-09; CQ-01A…CQ-08; ArchUnit **11/11**) |

> **CODE-QUALITY program (closed):** [code-quality-program.md](./code-quality-program.md) — **Done** (2026-07-09); detail [detail/CODE-QUALITY-code-hygiene.md](./detail/CODE-QUALITY-code-hygiene.md). BDD **`not-applicable`** (refactor only).
>
> **CORE-FORTRESS program:** [detail/CORE-FORTRESS-program-roadmap.md](./detail/CORE-FORTRESS-program-roadmap.md) — **Done** (F1–F8; 2026-07-09).
>
> **P23 vs P22:** **P22 Done** — closed rendering **engine** and deploy **scaffold**. **P23 Done** (2026-07-08) — bank-grade Word typography/layout for all demo packages; pairs with **LR-A2** (font baseline **Done**) and unblocks **LR-A7** (pagination corpus). Detail: [detail/P23-demo-typography-layout-excellence.md](./detail/P23-demo-typography-layout-excellence.md).
>
> **P22 vs P12:** **P22 Done** (2026-07-04). Closes P18/P4 rendering fidelity **engine** gap per [demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md).
> **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03 — T01–T12; commit `2e61dc3`). Do not reopen
> P18/P21 phase status. Detail:
> [detail/P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md),
> [detail/P12-api-package-access-invocation-records.md](./detail/P12-api-package-access-invocation-records.md),
> [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md).
>
> **P22 (Done 2026-07-04):** rendering engine + demo scaffolds — typography/content excellence → **P23**.
>
> **CDP (delivery focus):** [competitiveness-deepening-program.md](./competitiveness-deepening-program.md) — Wave **CD-0 Done**; Wave **CD-2 Done** (2026-07-11; T01–T13, merges `1930842` / `6821f45` / `895f16e` / `3aed175` / `c62b1a1` / `1eb230b` / `55a6ab6` / `b16e52a` / `6e3f825` / `f12b193` / `b2b0899`); CD-3 **Not Started** (recommend only); task prefix **`CD-*`** only.
> **LRP:** [launch-readiness-program.md](./launch-readiness-program.md) — Waves LR-A…LR-E; task prefix **`LR-*`** only; **Wave LR-A Done** (2026-07-10; A1–A7; merge `cc9e5f6`); **LR-A5 Done** (`lrp-a5-adr-closeout`; **ADR-0041 Accepted**; 0042/0043 remain Proposed); Word/XSD/LO24 residuals deferred — do not invent Word numbers; **Wave LR-C Done** (2026-07-11 — C1–C13; merge tip `bf9cbeb`); **Wave LR-D Done** (2026-07-12 — D1–D7; merge tip `218dcf1`); **Wave LR-E Done** (2026-07-12 — E1 merge `575d0aa` #42; E2 merge `ae39fbb` #43; docs gate; checklist **NO-GO** — **not** production go-live); **LRP planned waves → Done**; **no sole-active LRP wave**; **OPS-PASTE-BINDING-SEAM → Done** (Task Master **#47**; merge `f1f00da`; checklist #5b → **GO**; CD-HARD-T05 Done — not an LRP wave; **no sole-active**). **LR-D4 Done** (`218dcf1`; feature tip `670a683`; Task Master #41; ADR-0049 Accepted; mvn verify GREEN 1321; DEPLOY_OK 2026-07-12T04:18:13+08:00). **LR-D3 Done** (`ba5ea2e`; #40); **LR-D5 Done** (`5b13476`; #38; NFR proposals pending — **not** confirmed SLOs); **LR-D6 Done** (`56383eb`; #37); **LR-D7 Done** (`c94a356`; #36); **LR-D1 Done** (`20b2a76`; #35); **LR-D2 Done** (`3d78bc5`; #39). Do **not** claim production go-live. Do **not** activate CD-3. Do **not** touch `DGE-audit-governance`.
## Thin vertical slice (MVP chain)

Target before expanding breadth:

```text
Login → upload & approve master → create template → configure variables/content
  → test generate → submit test/approval → publish
  → configure minimal API policy → sync generate DOCX via runtime API
```

Phases involved: **P0 → P1 → P2 → P3 → P4 → P5 → P6 → P7 (minimal sync path)**.

## Deferred / post-MVP enhancements

| Area | Notes |
| --- | --- |
| P6-T04 | Management caller contract page | Done |
| P5-D03 | Publish gate checklist UI | Done |
| P3-T04/T05 deep UI | Rule configurator + binding validation UI (test data CRUD deferred) | Done (thin slice) |
| P4-T05 | Preview comparison panel + comparisonSummary | Done (thin slice) |
| P7-T06 | DOCX + PDF encryption execution (LibreOffice prod / PDFBox test conversion) | Done (thin slice) |
| Idempotency cache seam | Redis + DB dual-write | Done |
| E06-T05 | Playwright login a11y smoke | Done |
| Kafka async worker | Kafka transport + DLT + EmbeddedKafka test | Done (thin slice) |
| Test data set CRUD | P3-T05 full backend + UI | Done |
| Batch PDF/encryption | DocumentGenerationEngine batch path | Done |
| LibreOffice docker-exec | `conversion-mode=docker-exec` | Done |

## Epic cross-reference

| Epic | Maps to phases | Task sheet |
| --- | --- | --- |
| E01 Master & template authoring | P2, P3, P4 | [e01-task-sheet.md](../architecture/e01-task-sheet.md) |
| E02 Lifecycle workflow | P5 | [e02-task-sheet.md](../architecture/e02-task-sheet.md) |
| E03 API management | P6 | [e03-task-sheet.md](../architecture/e03-task-sheet.md) |
| E04 Audit console | P8 | [e04-task-sheet.md](../architecture/e04-task-sheet.md) |
| E05 Enterprise integration | P0, P7, P9 | [e05-task-sheet.md](../architecture/e05-task-sheet.md) |
| E06 Management UI finish | P1, P5, P6, P8 | [e06-task-sheet.md](../architecture/e06-task-sheet.md) |
| E07 Production readiness | P9 | [e07-task-sheet.md](../architecture/e07-task-sheet.md) |
| E11 Role-journey UI | P1, P6 | [e11-role-journey-ui-continuation-plan.md](../architecture/e11-role-journey-ui-continuation-plan.md) |
| E12 Frontend role-operation UI | P1, E11 | [e12-phase1-task-sheet.md](../architecture/e12-phase1-task-sheet.md), [e12-phase3-task-sheet.md](../architecture/e12-phase3-task-sheet.md) |
| E13 Identity & group administration | P13 | [detail/P13-identity-group-administration.md](./detail/P13-identity-group-administration.md) (mirror in [execution-sync-ledger.md](./execution-sync-ledger.md)) |

## Milestone cross-reference (technical waves)

| Milestone | Scope | Task sheet |
| --- | --- | --- |
| M1 | Wave 0–1: foundation + contract discovery | [m1-task-sheet.md](../architecture/m1-task-sheet.md) |
| M2 | Wave 2: sync generation | [m2-task-sheet.md](../architecture/m2-task-sheet.md) |
| M3 | Wave 3: batch & async | [m3-task-sheet.md](../architecture/m3-task-sheet.md) |
| M4 | Wave 4: download security | [m4-task-sheet.md](../architecture/m4-task-sheet.md) |
| M5 | Wave 5: API management backend | [m5-task-sheet.md](../architecture/m5-task-sheet.md) |
| M6 | Wave 6: lifecycle governance backend | [m6-task-sheet.md](../architecture/m6-task-sheet.md) |
| M7 | Wave 7: runtime integration | [m7-task-sheet.md](../architecture/m7-task-sheet.md) |
| M8–M11 | Security & dependency gates | m8–m11 task sheets |
| M12–M14 | Runtime adapter & transport closure | m12–m14 task sheets |

## Active phase rule

- Only one phase row may be `In Progress`.
- Activate the next phase only when the prior phase exit criteria are met and marked `Done`.
