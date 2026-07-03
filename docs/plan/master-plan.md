# Master Plan

**Baseline:** Project restart from zero — 2026-06-23  
**Active formal phase:** **P22** (2026-07-03 — **P22-DEMO-EXPANSION In Progress**; partial progress `6f9c76a`; T01/T02 largely done, T03/T04 partial; gates **RED**). **Prior:** **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03 — T01–T12; Playwright **10/10** + UIUX **PASS**; `2e61dc3`). **P12-TEMPLATE-TESTING-OVERHAUL Done** (2026-07-03). **P21 Done** (2026-06-30; AUD-B10 **resolved** via P12-AUD-B10; AUD-M02 **resolved** via P12-AUD-M02). User sequence P14 → P15 → P18 **complete**; **P21 role-journey frontend redesign Done** (2026-06-30; T01–T11 + X01–X06 + X02).
**P14** confirmed large domains **Done** (2026-06-27).
**P15** Kubernetes deployment **Done** (2026-06-27; T01–T10).
**P18** structured authoring **Done** (2026-06-28; T01–T10).
UX-A…UX-F interaction/upgradeability tasks (same plan) run as optimization waves
against the existing implementation. **UX Wave A** (role gating + half-built interactions)
re-earned Done on 2026-06-23; **UX Wave B** (workbenches + polish) Done (2026-06-23).
**P22 In Progress** (2026-07-03) — **P22-DEMO-EXPANSION** (partial `6f9c76a`: T01/T02 largely done, T03/T04 partial; T05–T15 Not Started; gates **RED**). **P12 catch-all → Not Started** (slice **P12-API-PACKAGE-ACCESS-INVOCATION Done** 2026-07-03).
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
| P22 | Demo expansion & rendering fidelity | P3, P4, P18 | Structured content DOCX fidelity (P18 rendering gap), dual page numbering, per-demo footers, eight bank letter `deploy/demo-*` packages, `import-all-demos.ps1` (R1–R5; BDD-DEMO-EXP-001…015) | **In Progress** (2026-07-03; partial `6f9c76a` — T01/T02 **In Progress**, T03/T04 partial; T05–T15 **Not Started**; gates **RED**) |

> **P22 vs P12:** **P22** is the sole formal phase **In Progress** (2026-07-03). Closes P18/P4
> rendering fidelity gap per [demo-expansion-behavior-spec.md](../requirements/demo-expansion-behavior-spec.md).
> **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03 — T01–T12; commit `2e61dc3`). Do not reopen
> P18/P21 phase status. Detail:
> [detail/P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md),
> [detail/P12-api-package-access-invocation-records.md](./detail/P12-api-package-access-invocation-records.md),
> [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md).
>
> **CDP (this session):** [competitiveness-deepening-program.md](./competitiveness-deepening-program.md) — Wave CD-0…CD-3; task prefix **`CD-*`** only.
>
> **P22 (other session):** formal phase may show In Progress in this file for repo accounting; implementation tracked in [P22 detail](./detail/P22-demo-expansion-rendering-fidelity.md). **Do not** execute P22 from CDP docs.

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
