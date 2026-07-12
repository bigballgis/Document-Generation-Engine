# Documentation Index

**Project baseline:** Restart from zero (2026-06-23); **P0–P11, P13–P21 Done**; **P12 Not Started**
(catch-all idle; slices **P12-TEMPLATE-TESTING-OVERHAUL Done**, **P12-API-PACKAGE-ACCESS-INVOCATION Done** 2026-07-03).

**Active work in this documentation track:** **None** (sole-active cleared). **[LR-E2 Done](./plan/launch-readiness-program.md)** (slice `lrp-e2-launch-checklist`; merge `ae39fbb`; Task Master #43; BDD not-applicable; artifact [launch-readiness-checklist.md](./operations/launch-readiness-checklist.md); overall snapshot **NO-GO** — not a production go-live claim). **[Wave LR-E Done](./plan/launch-readiness-program.md)** (E1+E2 docs exit gate — **not** production go-live). **LRP planned waves A–E → Done**. Formal phase remains **None**. Do **not** claim production go-live. Do **not** activate CD-3. Do **not** touch `DGE-audit-governance`. Recommend next (notes only): **pause**. **Prior:** **[LR-E1 Done](./behavior/lrp-e1-sse-proxy-e2e.md)** (slice `lrp-e1-sse-proxy-e2e`; merge `575d0aa`; Task Master #42; evidence [LRP-E1-sse-manifest.md](../frontend/e2e/evidence/LRP-E1-sse-manifest.md)). **Prior:** **[Wave LR-D Done](./plan/detail/LRP-D-ops-observability.md)** (2026-07-12 — D1–D7 all Done; merge tip `218dcf1`). **[Wave LR-C Done](./plan/detail/LRP-C-usability-deepening.md)** (2026-07-11 — C1–C13; merge tip `bf9cbeb`). **[CDP Wave CD-2 Done](./plan/competitiveness-deepening-program.md)** (2026-07-11 — T01–T13; merge tip `b2b0899`).

**Active formal program:** **None** (2026-07-09+). **CODE-QUALITY Done** — CQ-01A…CQ-08; ArchUnit **11/11**; gates **GREEN**. **CORE-FORTRESS program Done** — F1–F8 complete. **LR-A4 Done** (2026-07-10; merge `a523a09`). **CDP golden path T01 Done** (2026-07-10; merge `1930842`). See [CODE-QUALITY program](./plan/code-quality-program.md) · [LRP](./plan/launch-readiness-program.md) · [CDP](./plan/competitiveness-deepening-program.md).

**P22 Done** (2026-07-04) — rendering engine + demo scaffolds; [P22 detail](./plan/detail/P22-demo-expansion-rendering-fidelity.md).

**P2-T06 Done** (Phase B master revision history, 2026-07-01). **P21 Done**. Latest gates: backend
`mvn verify` BUILD SUCCESS; frontend **643** Vitest (2026-07-03). See [PROJECT-STATUS-RESET.md](./PROJECT-STATUS-RESET.md)
and [plan/execution-sync-ledger.md](./plan/execution-sync-ledger.md).

## Start here

| Order | Document | Purpose |
| --- | --- | --- |
| 1 | [Master plan](./plan/master-plan.md) | Overall phase roadmap and status |
| 2 | [Plan layer index](./plan/README.md) | Detailed plans per phase (P0–P23) |
| 2a | **[Competitiveness Deepening Program (CDP)](./plan/competitiveness-deepening-program.md)** | **Program In Progress** — CD-2 **Done** (T01–T13); CD-3 Not Started; E2E matrix + pitfall registry ( **`CD-*` tasks** ) |
| 2b | [Execution sync ledger](./plan/execution-sync-ledger.md) | Epic/milestone mirror + gate evidence + **transitional seams index** |
| 2c | **[Deployment guide](../deploy/README.md)** | **Canonical** install/upgrade/cutover/rollback — Docker Compose + Kubernetes (ADR-0030) |
| 3 | [Orchestration high-level plan](./architecture/orchestration-high-level-plan.md) | Epic ordering and active epic rules |
| 4 | [Implementation task plan](./architecture/implementation-task-plan.md) | Technical waves M1–M14 |
| 5 | [Requirements plan](./requirements/requirements-plan.md) | Confirmed requirements + pending questions |
| 6 | [PRD](./product/PRD.md) | Product behavior and scope |

## Core product & domain

| Document | Purpose |
| --- | --- |
| [Requirements plan](./requirements/requirements-plan.md) | Raw confirmed requirements |
| [Demo expansion behavior spec](./requirements/demo-expansion-behavior-spec.md) | BDD-DEMO-EXP — rendering fidelity, dual page numbers, eight bank letter demos (**P22 Done**) |
| [Demo typography & layout behavior spec](./requirements/demo-typography-layout-behavior-spec.md) | BDD-DEMO-TYP-001…020 — bank-grade Word styles, rich bindings, font baseline, POI/E2E acceptance (**P23**; ready 2026-07-08) |
| [Non-functional requirements](./requirements/non-functional-requirements.md) | Quality, security, reliability constraints |
| [PRD](./product/PRD.md) | Product-facing behavior |
| [Usability review](./product/usability-review.md) | UX baselines, role journeys, open UX questions |
| [Authoring & rendering first principles](./product/authoring-rendering-first-principles-review.md) | Core product philosophy — fidelity over API breadth |
| [Catalog navigation UX](./product/catalog-navigation-ux.md) | Master/template package catalog IA + hybrid behavior-typed navigation (P21) |
| [Business terminology guide](./product/business-terminology-guide.md) | Business-friendly L1 label SSOT for non-IT bank users (P21) |
| [Domain model](./domain/domain-model.md) | Objects, states, invariants |
| [Permission matrix](./security/permission-matrix.md) | Roles, groups, authorization |

## Plan layer (execution truth)

| Document | Purpose |
| --- | --- |
| [Plan index](./plan/README.md) | Layer rules and phase links |
| [Execution sync ledger](./plan/execution-sync-ledger.md) | Epic/milestone ↔ phase mapping + gate evidence + transitional seams index |
| [Master plan](./plan/master-plan.md) | P0–P23 phases — see phase detail plans for status |
| [System optimization review 2026-07](./plan/system-optimization-review-2026-07.md) | **Consolidated optimization program** (SOR-0…7, 2026-07-04 closeout) — actionable backlog complete except P22-blocked A02/A03 |
| [SpotBugs exclusion ratchet](./plan/spotbugs-exclusion-ratchet.md) | Ongoing EI_EXPOSE_REP reduction plan (SOR-A05 slice 1+; post-SOR program) |
| [Comprehensive optimization roadmap](./plan/comprehensive-optimization-roadmap.md) | **Unified execution map** — docs, API contract, template workflow, frontend UX, performance, E2E (COR-0…6, 2026-06-23) |
| **[Competitiveness Deepening Program (CDP)](./plan/competitiveness-deepening-program.md)** | **Launch readiness** — doc truth, E2E golden paths, pitfall registry (`CD-*` tasks) |
| **[Code Quality Program (CODE-QUALITY)](./plan/code-quality-program.md)** | **Done** (2026-07-09) — behavior-preserving hygiene: module boundaries, god-class extraction, DRY infra (`CQ-*` all slices) |
| [Launch Readiness & Deep-Optimization Program (LRP)](./plan/launch-readiness-program.md) | Production pitfalls + usability deepening — Waves LR-A…LR-E (`LR-*` tasks; sibling program; LR-C1/C4 Done via F7) |
| [P12 Deferred enhancements](./plan/detail/P12-deferred-enhancements.md) | Catch-all slice registry (testing overhaul, API package, UIUX refactor — Done slices) |
| [P12 API package access](./plan/detail/P12-api-package-access-invocation-records.md) | Package-first API access + invocation records (**Done** 2026-07-03) |
| [Optimization plan & backlog](./plan/optimization-plan.md) | Technical debt detail (OPT-A…G): gates, coverage, backend architecture |
| [UX & upgradeability optimization plan](./plan/ux-upgradeability-optimization-plan.md) | Historical UX waves (UX-A…G); cross-check against comprehensive roadmap |
| [P0 Foundation](./plan/detail/P0-foundation.md) | Scaffold, compose, gates |
| [P1 Login & session](./plan/detail/P1-login-session.md) | Local auth, role landing |
| [P2 Master management](./plan/detail/P2-master-management.md) | DOCX master, anchors, review |
| [P3 Template authoring](./plan/detail/P3-template-authoring.md) | Wizard, variables, content |
| [P4 Rendering & preview](./plan/detail/P4-rendering-preview.md) | DOCX/PDF, fidelity |
| [P5 Lifecycle governance](./plan/detail/P5-lifecycle-governance.md) | Test → approve → publish |
| [P6 API management](./plan/detail/P6-api-management.md) | Policy, credentials, AD Group |
| [P7 Runtime API](./plan/detail/P7-runtime-api.md) | OpenAPI v1 operations |
| [P8 Audit & contract](./plan/detail/P8-audit-contract.md) | Audit console, caller view |
| [P9 Production readiness](./plan/detail/P9-production-readiness.md) | Gates, observability, deploy |
| [P10 Runtime download](./plan/detail/P10-runtime-download.md) | Secure document download |
| [P11 Batch & async generation](./plan/detail/P11-batch-async.md) | Sync batch, async task query/cancel |
| [P13 Identity & group administration](./plan/detail/P13-identity-group-administration.md) | User + group management plane (Done 2026-06-23) |
| [P14 Confirmed large domains](./plan/detail/P14-confirmed-large-domains.md) | Content modules, collaboration, export/import (Done 2026-06-27) |
| [P15 Kubernetes deployment](./plan/detail/P15-kubernetes-deployment-container-hardening.md) | Helm, hardening, blue-green, CI gates (Done 2026-06-27; ADR-0030) |
| [P16 Lifecycle/version governance](./plan/detail/P16-lifecycle-version-governance.md) | Stop/restore/deprecate, version deactivate (Done 2026-06-23) |
| [P17 Per-domain API policy](./plan/detail/P17-api-policy-domain-governance.md) | Domain save, impact preview, rollback (Done 2026-06-25) |
| [P18 Structured authoring & fidelity](./plan/detail/P18-structured-authoring-fidelity-engine.md) | Controlled editor, paste cleaning, renderProfile (Done 2026-06-28) |
| [P19 Verifiability & publish gate](./plan/detail/P19-verifiability-publish-gate.md) | Batch test, coverage, live publish gate, decision forms (Done 2026-06-25) |
| [P20 i18n & UI upgradeability](./plan/detail/P20-i18n-ui-upgradeability.md) | Locale registry, brand theming, zh-CN primary journey (Done 2026-06-25) |
| [P21 Role-journey frontend redesign](./plan/detail/P21-role-journey-frontend-redesign.md) | Hybrid IA, behavior-typed to-dos, per-role journeys, business-friendly terminology (**Done** 2026-06-30) |
| [P22 Demo expansion & rendering fidelity](./plan/detail/P22-demo-expansion-rendering-fidelity.md) | Structured content DOCX writer, dual page numbers, eight demo **scaffolds** (**Done** 2026-07-04) |
| [P23 Demo typography & layout excellence](./plan/detail/P23-demo-typography-layout-excellence.md) | Bank-grade styles, rich bindings, font baseline, POI/E2E acceptance (**Done** 2026-07-08; T01–T16) |
| **[Deployment guide](../deploy/README.md)** | **Canonical operator guide** — prerequisites, install/upgrade/cutover/rollback/secrets; indexes all `deploy/*.md` topic docs |

## Architecture

| Document | Purpose |
| --- | --- |
| [Architecture index](./architecture/README.md) | Architecture views entry |
| [System context](./architecture/system-context.md) | External actors and boundaries |
| [Module boundaries](./architecture/module-boundaries.md) | Bounded modules |
| [Runtime view](./architecture/runtime-view.md) | Deployment and components |
| [Data & storage view](./architecture/data-storage-view.md) | Persistence and retention |
| [Async messaging view](./architecture/async-messaging-view.md) | Kafka boundaries |
| [Security view](./architecture/security-view.md) | Auth, audit, fail-closed |
| [Technology stack decisions](./architecture/technology-stack-decisions.md) | ADR sync ledger |
| [TDD delivery workflow](./architecture/tdd-delivery-workflow.md) | Mandatory delivery loop |
| [Quality gate baseline](./architecture/quality-gate-threshold-baseline.md) | Threshold defaults |
| [AI development guide](./architecture/ai-development-guide.md) | Reading paths for implementers |
| [UX entity display constitution](./architecture/ux-entity-display-constitution.md) | Entity columns, links, filters, fluid vs contained layout (**Phases 0–3 Done**, Phase 4 In Progress — 2026-07-08) |
| [Frontend entity display skill](../.cursor/skills/frontend-entity-display/SKILL.md) | Implementer workflow for `EntityLinkCell`, filters, `layoutVariant` |

## Orchestration & milestones

| Document | Purpose |
| --- | --- |
| [Orchestration plan](./architecture/orchestration-high-level-plan.md) | Epic backlog E01–E12 |
| [Implementation task plan](./architecture/implementation-task-plan.md) | Waves 0–14 |
| [M1–M14 task sheets](./architecture/m1-task-sheet.md) | Milestone decomposition (see m2–m14 siblings) |
| [E01–E07 task sheets](./architecture/e01-task-sheet.md) | Epic decomposition (see e02–e07 siblings) |
| [E11 role-journey plan](./architecture/e11-role-journey-ui-continuation-plan.md) | Post-login navigation |
| [E12 development plan](./architecture/e12-frontend-role-journey-development-plan.md) | Role-operation UI |

## API

| Document | Purpose |
| --- | --- |
| [API index](./api/README.md) | Contract maintenance |
| [OpenAPI v1](./api/openapi-v1.yaml) | Formal runtime contract |
| [Contract outline](./api/contract-outline.md) | Narrative companion |
| [Examples](./api/examples/README.md) | Request/response examples |

## Deployment & operations

| Document | Purpose |
| --- | --- |
| **[Deployment guide](../deploy/README.md)** | **Start here for operators** — Docker Compose + Kubernetes install, upgrade, blue-green cutover, rollback, secrets (ADR-0030 / P15) |
| [Container hardening](../deploy/container-hardening.md) | Non-root, read-only root FS, minimal base images |
| [Helm chart README](../deploy/helm/docgen/README.md) | Chart values, lint/template, per-env profiles |
| [Production runbook](./operations/runbook.md) | Release gate, local prod compose profile, observability + **LR-D3 draft alert response sections** |
| **[Launch readiness checklist (LR-E2)](./operations/launch-readiness-checklist.md)** | **LR-E2 Done** — evidence-linked go/no-go / conditional rows + verdict template; snapshot **NO-GO** (2026-07-12); Wave LR-E docs gate **Done** — **not** a production go-live claim; companion [launch-readiness-gate.md](./plan/launch-readiness-gate.md) |
| **[Backup & restore runbook](./operations/backup-restore-runbook.md)** | **LR-D2** — pg/MinIO backup + confirmation-gated scratch restore; Flyway forward-only + blue-green cross-link; drill evidence **EXECUTED** 2026-07-12 (scratch scope only — do **not** claim production ADR-0030 RPO/RTO compliance) |
| [ADR-0030 Operational Platform Baseline](./adr/operations/0030-operational-platform-baseline.md) | Accepted CD, hardening, backup, and observability decisions |
| [ADR-0048 Audit Data Retention & Archival](./adr/operations/0048-audit-data-retention-policy.md) | **Accepted** (2026-07-11) — Tier-1 management 90d / runtime 365d hard delete; Tier-2 archival deferred (LR-D1) |

## Behavior specifications (`docs/behavior/`)

| Document | Status | Purpose |
| --- | --- | --- |
| **[MVP golden path (browser)](./behavior/mvp-golden-path-browser.md)** | **ready** | CD-BDD-T01 → **CD-E2E-T01 Done** (2026-07-10; Docker 1/1) |
| [Tester decision journey](./behavior/tester-decision-journey.md) | **ready** | CD-BDD-T02 → CD-E2E-T02/T03 |
| [Approver decision journey](./behavior/approver-decision-journey.md) | **ready** | CD-BDD-T03 → CD-E2E-T04 |
| [Team lead publish journey](./behavior/team-lead-publish-journey.md) | **ready** | CD-BDD-T04 → CD-E2E-T05 |
| [Master designer lifecycle](./behavior/master-designer-lifecycle.md) | **ready** | CD-BDD-T05 → **CD-E2E-T06 Done** (2026-07-10; merge `3aed175`) |
| [API policy edit-save journey](./behavior/api-policy-edit-save-journey.md) | **ready** | CD-BDD-T06 → CD-E2E-T07 |
| **[Preview success + artifact download](./behavior/preview-success-artifact-download-journey.md)** | **ready** | CD-BDD-T08 → **CD-E2E-T08** (closes P12 T13 preview-success manifest gap; CD-PIT-08 final-path download) |
| [Preview comparison journey](./behavior/preview-comparison-journey.md) | **ready** | CD-BDD-T07 → CD-E2E-T09 |
| **[Fidelity viewed confirmation](./behavior/fidelity-viewed-confirmation-journey.md)** | **ready** | **CD-E2E-T10** — BDD-CDP-FID-001…004 (Pass/Approve/Publish fail-closed) |
| [Audit admin query journey](./behavior/audit-admin-query-journey.md) | **ready** | CD-BDD-T08 → CD-E2E-T11 |
| **[zh-CN + dual-brand golden screenshots](./behavior/zh-cn-dual-brand-golden-screenshots.md)** | **ready** | **CD-E2E-T12** — BDD-CDP-I18N-001/002（≥3 key surfaces zh-CN；REDBC+GREENBC @1920） |
| [Template testing overhaul](./behavior/template-testing-overhaul.md) | **Done** (P12 2026-07-03) | P12 template testing tab |
| [API package access & invocation records](./behavior/api-package-access-and-invocation-records.md) | **Done** (P12 2026-07-03) | Package-first API access |
| [Session renewal & revocation](./behavior/session-renewal-revocation.md) | **ready** (LR-B6 delivered 2026-07-04) | BDD-LRP-SESSION-001 → LR-B6 **Done** (sliding renewal, 30 min TTL, 8 h absolute cap, Redis revocation fail-closed; policy confirmed 2026-07-04; implementation deviations in spec §14.1) |
| **[CORE-FORTRESS F1 rendering correctness](./behavior/core-fortress-f1-rendering-correctness.md)** | **Done** (2026-07-09) | Unified renderer, fail-closed refs — **F1 closed** |
| **[CORE-FORTRESS F2 runtime lightweight](./behavior/core-fortress-f2-runtime-lightweight.md)** | **Done** (2026-07-09) | Publish fidelity cache, bulk lifecycle, idempotency — **F2 closed** |
| **[CORE-FORTRESS F3 node matrix + expression engine](./behavior/core-fortress-f3-node-matrix-expression.md)** | **Done** (2026-07-09) | Shared `ConditionExpressionEvaluator`; matrix validation hardening — **F3 closed** |
| **[CORE-FORTRESS F4 production rendering hardening](./behavior/core-fortress-f4-production-rendering-hardening.md)** | **Done** (2026-07-09) | LO pool parallel regression, config evidence, pagination corpus schema — **F4 closed** (Docker measurements + full `mvn verify` env follow-up) |
| **[CORE-FORTRESS F5 async durability + security depth](./behavior/core-fortress-f5-async-durability-security.md)** | **Done** (2026-07-09) | Stale reclaim, Kafka DLT, payload scrub, 429 audit, credential rotation — **F5 closed** (full `mvn verify` env follow-up) |
| **[CORE-FORTRESS F6 frontend kernel refactor](./behavior/core-fortress-f6-frontend-kernel-refactor.md)** | **Done** (2026-07-09) | Composable decomposition — controller **243** lines; **73** composable Vitest; F6-T08 E2E env blocker documented |
| **[CORE-FORTRESS F7 authoring UX](./behavior/core-fortress-f7-authoring-ux.md)** | **Done** (2026-07-09) | Dirty guard + side-by-side preview; LR-C1/C4 mirrored Done — Vitest **894**; E2E **12/12** |
| **[LR-C2 structured editor local draft recovery](./behavior/lrp-c2-structured-editor-local-draft-recovery.md)** | **ready** (2026-07-11) | BDD-LRP-C2-DRAFT-001 — debounced localStorage draft; Restore/Discard banner; clear-on-save; LR-C1 interplay; C3 storage separation |
| **[LR-C3 editor undo/redo](./behavior/lrp-c3-editor-undo-redo.md)** | **ready** (2026-07-11) | BDD-LRP-C3-UNDO-001 — structure-level snapshot history (cap 50); Ctrl/Cmd+Z/Y; toolbar; draft/history separation; dirty interplay |
| **[LR-C5 catalog server-side pagination/filter](./behavior/lrp-c5-catalog-pagination.md)** | **ready** (2026-07-11) | BDD-LRP-C5-CATALOG-001 — templates/masters/content-modules `PageView`; filters/search; COR-F09 group-first row sort; ≥500 p95 under 1s |
| **[LR-C6 global command palette](./behavior/lrp-c6-command-palette.md)** | **ready** (2026-07-11) | BDD-LRP-C6-PALETTE-001 — Ctrl/Cmd+K; C5 `search` + `visibleRoutes`; keyboard nav; authz fail-closed; no new backend endpoint |
| **[LR-C7 in-app notification center](./behavior/lrp-c7-notification-center.md)** | **ready** (2026-07-11) | BDD-LRP-C7-NOTIFY-001 — shell bell + unread badge; P14 work-item projection + per-user read marker; 30s poll; deep-link `/dashboard?queue={QUEUE}#tasks-section`; no email/IM/SSE |
| **[LR-C8 role onboarding tour](./behavior/lrp-c8-role-onboarding-tour.md)** | **ready** (2026-07-11) | BDD-LRP-C8-TOUR-001 — role-aware `el-tour` (no new dep); first-login + skip/dismiss local marker; help-menu replay; reuses P21 `roleJourneyDefinitions` |
| **[CORE-FORTRESS F8 observability / SLO / DR](./behavior/core-fortress-f8-observability-slo-dr.md)** | **Done** (2026-07-09) | Micrometer SLOs; deep readiness; DR playbook; evidence bundle — `mvn verify` **1154** |
| **[LR-A3 master DOCX upload validation](./behavior/lrp-a3-master-docx-upload-validation.md)** | **ready** (2026-07-10) | BDD-LRP-A3-UPLOAD-001 — ZIP magic + OPC probe + 50MB/60MB limits; virus scan pending Q |
| **[LR-A4 fail-closed unsupported nodes](./behavior/lrp-a4-fail-closed-unsupported-nodes.md)** | **ready** (2026-07-10) | BDD-LRP-A4-FAIL-CLOSED-001 — publish-gate hard-block for `qrBarcodeRef`/`attachmentListRef`; no silent omit; writers deferred |
| **[LR-D1 audit data retention & archival](./behavior/lrp-d1-audit-retention.md)** | **ready** (2026-07-11) | BDD-LRP-D1-001…010 — management 90d / runtime 365d hard delete; ShedLock; `AUDIT_RETENTION_PURGE`; **[ADR-0048 Accepted](./adr/operations/0048-audit-data-retention-policy.md)** |
| **[LR-D7 durable security audit events](./behavior/lrp-d7-durable-security-audit.md)** | **ready** (2026-07-11) — slice **Done** (`c94a356`) | BDD-LRP-D7-001…010 — login / forbidden-route / download grant+deny → `management_audit_event`; fail-safe login; matrix §13.3; ledger 「Security forbidden-route audit」**closed**; joins ADR-0048 90d |
| **[LR-D6 load smoke baseline](./behavior/lrp-d6-load-smoke.md)** | **not-applicable** (2026-07-12) — slice **Done** (`56383eb`) | Measurement harness + evidence — ≥20 concurrent sync gen (p95/error/pool) + ≥5 SSE previews; DEF-LRP-D6-001 triage; fed **LR-D5 Done** (`5b13476`; proposals pending confirmation) / D3 pending |
| **[LR-D2 backup/restore runbook + drill](./behavior/lrp-d2-backup-restore.md)** | **not-applicable** (2026-07-12) | Ops docs + timed Docker/local Postgres+MinIO drill evidence; ADR-0030 RPO/RTO rehearsal; no product UI/API behavior — feeds LR-E2 checklist |
| **[LR-D3 metrics & alerting as code](./behavior/lrp-d3-metrics-alerting.md)** | **not-applicable** (2026-07-12) — slice **Done** (`ba5ea2e`) | Observability instrumentation + Prometheus/Grafana as code; scrape + rule-lint acceptance; draft thresholds from D6/D5 only — no product UI/API behavior |
| **[LR-D4 trace propagation](./behavior/lrp-d4-trace-propagation.md)** | **not-applicable** (2026-07-12) — slice **Done** (`218dcf1`) | Internal observability plumbing — request→MDC→async→Kafka→consumer MDC; **[ADR-0049 Accepted](./adr/operations/0049-distributed-trace-propagation.md)**; `MdcTaskDecorator` + Kafka `X-Trace-Id`; Scenario A/B; no UI / no Zipkin-Tempo backend |
| **[LR-E1 SSE-through-proxy incremental E2E](./behavior/lrp-e1-sse-proxy-e2e.md)** | **not-applicable** (2026-07-12) · **Done** merge `575d0aa` | Test-only evidence for **LR-B3** — Playwright on Docker 4173: ≥2 incremental SSE arrival timestamps (maxGapMs≈1864) + ≥60 s idle heartbeat survival through nginx; closes CD-PIT-12 browser proof; [manifest](../frontend/e2e/evidence/LRP-E1-sse-manifest.md) |
| **[JWT_SECRET explicit provision — no compose default](./behavior/ops-jwt-secret-no-default.md)** | **ready** (2026-07-12) · slice **Done** (`587cd9a`) | **BDD-OPS-JWT-SECRET-001** — checklist **#9 → GO** (closes LR-B6 🟡#4); compose `:?` + `ProductionSecretGuard`; overall checklist still **NO-GO**; **not** go-live |

## Evidence & acceptance artifacts

| Document | Purpose |
| --- | --- |
| [LR-A7 pagination measurement](./evidence/lrp-a7-pagination/README.md) | Docker PDF page-count corpus (2026-07-10 / merge `abf2048`); Word baseline n/a on host; slim JSON + README (PDFs untracked under `.tmp/`) |
| [Demo typography review checklist](./evidence/demo-typography-review-checklist.md) | Human reviewer checklist — fonts, styles, margins, headers/footers, tables, signatures (**P23-T16**; ≥2 CORP + ≥2 RETAIL mandatory samples) |
| [Fundraising demo summary](./evidence/fundraising-demo-summary.md) | 13-template evidence matrix — generate script, E2E, POI tests, manifest paths (**P23-T16**) |
| [Security evidence index](./evidence/security/README.md) | SCA runbook and execution logs |

## ADRs

| Document | Purpose |
| --- | --- |
| [ADR index](./adr/README.md) | Decision records (Accepted = decision, not task done) |

## Governance & constitution

| Document | Purpose |
| --- | --- |
| [Document as software charter](./document-as-software.md) | Operating philosophy |
| [Documentation architecture](./documentation-architecture.md) | Knowledge model |
| [Governance](./governance.md) | Update rules and anti-drift gates |
| [Git workflow](./git-workflow.md) | Version control workflow |
| [PROJECT-STATUS-RESET](./PROJECT-STATUS-RESET.md) | Zero baseline declaration |

## Project agent tooling (`.cursor/`)

| Asset | Purpose |
| --- | --- |
| `.cursor/agents/delivery-orchestrator.md` | Single-entry pipeline scheduler / router |
| `.cursor/agents/behavior-spec-author.md` | BDD behavior spec (Given/When/Then) gate |
| `.cursor/agents/plan-orchestrator.md` | Plan layer maintenance |
| `.cursor/agents/doc-keeper.md` | Documentation-as-code guardian |
| `.cursor/agents/backend-engineer.md` | Backend TDD implementer |
| `.cursor/agents/frontend-engineer.md` | Frontend TDD implementer (bank OA style lock) |
| `.cursor/agents/e2e-test-engineer.md` | Playwright functional E2E journeys |
| `.cursor/agents/e2e-uiux-reviewer.md` | Visual/responsive/a11y/brand UIUX evidence |
| `.cursor/agents/deploy-engineer.md` | Automated Docker build/deploy/rollback |
| `.cursor/agents/architecture-reviewer.md` | Read-only architecture review |
| `.cursor/agents/post-task-doc-sync.md` | Mandatory end-of-task documentation sync |
| `.cursor/agents/post-task-commit-review.md` | Mandatory end-of-task commit review and commit |
| `.cursor/skills/document-as-code/` | Doc workflow skill |
| `.cursor/skills/tdd-feature-delivery/` | TDD loop skill |
| `.cursor/skills/frontend-oa-design/` | Bank OA design-system lock skill |
| `.cursor/skills/e2e-frontend-testing/` | Playwright E2E (functional + UIUX) workflow |
| `.cursor/skills/docker-deployment/` | Docker build/deploy/rollback workflow |
| `.cursor/skills/post-task-doc-sync/` | Post-task doc sync workflow |
| `.cursor/skills/post-task-commit-review/` | Post-task commit review workflow |
| `.cursor/skills/i18n-english-first/` | English-first i18n skill |
| `.cursor/skills/plan-status-tracking/` | Plan status skill |
| `.cursor/rules/*.mdc` | Project constitutions (always apply) |
| `.cursor/rules/strategic-direction-autonomy-constitution.mdc` | **When direction is clear, proceed autonomously — no permission polling** (always apply) |
| `.cursor/rules/subagent-routing-mandate.mdc` | **Parent agent must delegate via Task/subagent_type** (always apply) |
| `.cursor/hooks.json` | Auto-chain commit review after doc-sync subagent |

## Source-of-truth order (on conflict)

1. Latest explicit user confirmation  
2. [Requirements plan](./requirements/requirements-plan.md)  
3. [PRD](./product/PRD.md)  
4. [Domain model](./domain/domain-model.md)  
5. [Permission matrix](./security/permission-matrix.md)  
6. [ADRs](./adr/)

## Status vocabulary (project / epic / task)

`Not Started` | `In Progress` | `Blocked` | `Done`

Prior wave/epic closure claims are **void** until re-earned with real behavior and green gates.
