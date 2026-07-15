# Documentation Index

**Project baseline:** Restart from zero (2026-06-23); **P0–P11, P13–P21 Done**; **P12 Not Started**
(catch-all idle; slices **P12-TEMPLATE-TESTING-OVERHAUL Done**, **P12-API-PACKAGE-ACCESS-INVOCATION Done** 2026-07-03).

**Delivery focus:** **CORE-EXCELLENCE (CE)** — **#62 CE-K06** → **Done** (K06a/`485a7f3e` + K06b/`a689ca87` + K06c tip/`76297d08`); **#69 CE-C04** / **#70 CE-C05** / **#88 CE-U06** → **Done** (`c7be8305` / `405f7cea` / `7734366e`); Wave 0 #61/#86/#87 **Done**; umbrella **#53** remains **in-progress**; plan [core-excellence-program-2026-07.md](./plan/core-excellence-program-2026-07.md). Prior waves (#60/#84/#85; #59/#68/#83) **Done**. **Batch 4 remains Done**. Batch 1–3 Done. Formal phase remains **None**; **not** go-live. Overall checklist remains **NO-GO** (blocking: **#3b**; #5a/#10 **CONDITIONAL**). Do **not** activate CD-3. Do **not** invent a formal P-phase.

**Active formal program:** **None** (2026-07-09+). **CODE-QUALITY Done** — CQ-01A…CQ-08. **CORE-FORTRESS Done** — F1–F8. **CDP Wave CD-2 Done** (T01–T13; merge tip `b2b0899`; **no CDP wave In Progress**; CD-3 Not Started). **LRP waves A–E → Done**. See [CORE-EXCELLENCE](./plan/core-excellence-program-2026-07.md) · [CODE-QUALITY](./plan/code-quality-program.md) · [LRP](./plan/launch-readiness-program.md) · [CDP](./plan/competitiveness-deepening-program.md) · [execution-sync-ledger](./plan/execution-sync-ledger.md).

**P22 Done** (2026-07-04) — rendering engine + demo scaffolds; [P22 detail](./plan/detail/P22-demo-expansion-rendering-fidelity.md).

**P2-T06 Done** (Phase B master revision history, 2026-07-01). **P21 Done**. Latest gates: backend
`mvn verify` BUILD SUCCESS; frontend **643** Vitest (2026-07-03). See [PROJECT-STATUS-RESET.md](./PROJECT-STATUS-RESET.md)
and [plan/execution-sync-ledger.md](./plan/execution-sync-ledger.md).

## Start here

| Order | Document | Purpose |
| --- | --- | --- |
| 1 | [Master plan](./plan/master-plan.md) | Overall phase roadmap and status |
| 2 | [Plan layer index](./plan/README.md) | Detailed plans per phase (P0–P23) |
| 2a | **[CORE-EXCELLENCE (CE)](./plan/core-excellence-program-2026-07.md)** | **Delivery focus** — **#62** CE-K06 **Done** (a/b/c; tip `76297d08`); **#69/#70/#88 Done**; Wave 0 #61/#86/#87 **Done**; umbrella **#53**; not a formal P-phase |
| 2a′ | **[Competitiveness Deepening Program (CDP)](./plan/competitiveness-deepening-program.md)** | **CD-2 Done** (T01–T13); **no wave In Progress**; CD-3 Not Started; E2E matrix + pitfall registry (`CD-*`) |
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
| **[CORE-EXCELLENCE (CE)](./plan/core-excellence-program-2026-07.md)** | **Delivery focus** — **#62** CE-K06 **Done** (`76297d08`); **#69/#70/#88 Done**; Wave 0 #61/#86/#87 **Done**; prior #60/#84/#85 **Done**; Batch 4 remains Done; umbrella **#53**; not a formal P-phase |
| **[Competitiveness Deepening Program (CDP)](./plan/competitiveness-deepening-program.md)** | **CD-2 Done** — doc truth, E2E golden paths, pitfall registry (`CD-*`); CD-3 Not Started |
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
| **[Launch readiness checklist (LR-E2)](./operations/launch-readiness-checklist.md)** | **LR-E2 Done** — evidence-linked go/no-go / conditional rows + verdict template; snapshot **NO-GO** (2026-07-12); **#9 GO** / **#10 CONDITIONAL** (Kafka `KAFKA_IMAGE`); Wave LR-E docs gate **Done** — **not** a production go-live claim; companion [launch-readiness-gate.md](./plan/launch-readiness-gate.md) |
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
| **[Kafka image — company registry / fail-closed KAFKA_IMAGE](./behavior/ops-kafka-company-registry.md)** | **ready** (2026-07-12) · slice **Done** (`e54d03c`; Task Master **#45**) | **BDD-OPS-KAFKA-REGISTRY-001** — checklist **#10 → CONDITIONAL** (fail-closed `${KAFKA_IMAGE:?…}`; Hub example LOCAL/DEV ONLY; operator must supply company-approved coords — **do not** invent registry hostname; not GO without company pull evidence); overall checklist still **NO-GO**; **not** go-live |
| **[AD Group resolver — prod refuse config stub](./behavior/ops-ad-group-stub-close.md)** | **ready** (2026-07-12) · slice **Done** (`4e51a1b`; Task Master **#46**) | **BDD-OPS-AD-GROUP-STUB-001** S1–S4 — checklist **#5a → CONDITIONAL** (honest-bound config stub + fail-closed prod; LAB ONLY ≠ production AD; real LDAP/AD + company directory evidence still missing — **not GO**); **[ADR-0054 Accepted](./adr/authorization-security/0054-ad-group-resolver-production-boundary.md)**; company LDAP/AD **UNKNOWN**; overall checklist still **NO-GO**; **not** go-live |
| **[Paste cleaning ↔ binding / publish fail-closed](./behavior/ops-paste-binding-seam.md)** | **ready** (2026-07-12) · slice **Done** (`f1f00da`; Task Master **#47**) | **BDD-OPS-PASTE-BINDING-001** S1–S6 — checklist **#5b → GO** (wire path; ADR-0019 SoT: object + absolute → BLOCKED; residue on Accept; `computeBindingStatus` + PublishGate fail-closed; **CD-HARD-T05 Done**; **no** edit-time-only ADR escape); LR-A4 orthogonal; overall checklist still **NO-GO** (#3b); **not** go-live |
| **[Knip dead-code scan tooling](./behavior/slim-knip-scan.md)** | **not-applicable** (2026-07-12) · slice **Done** (`ea7db64`; Task Master **#48**) | Frontend Knip ^6.26 + evidence under [evidence/slim-knip-scan](./evidence/slim-knip-scan/README.md); Wave-1 orphan-file delete Done (unused files → 0); residual unused exports/types informational — **no** product actor journey; overall checklist still **NO-GO** (#3b); **not** go-live |
| **[Spring Boot 4.1.0 + Java 25 platform upgrade](./behavior/boot-4-1-upgrade.md)** | **not-applicable** (2026-07-13) · Task Master **#51** · slice **Done** (`993c287`; tip `e9bf43c`) | Platform/ops baseline — Boot parent **3.3.x → 4.1.0** + Java **21 → 25**; [ADR-0028](./adr/technology-stack/0028-backend-platform-stack-baseline.md) amended; `mvn verify` **GREEN** 1357/0/0/7; healthz **200** Boot 4.1.0 + Java 25.0.3; **no** product UI/API journey; formal phase **None**; **not** go-live; do **not** activate CD-3 |
| **[Dependency security refresh](./behavior/deps-security-refresh.md)** | **not-applicable** (2026-07-13) · Task Master **#49** · slice **Done** (`08c7d56`) | Maven + pnpm CVE/hygiene audit + baseline-safe upgrades (Boot **3.3.13**, ShedLock **6.x**, no major Vue/Vite without ADR); Vitest Critical exception → **#50** expires 2026-10-13; gates green — **no** product actor journey; does **not** close M9-T02; formal phase **None**; **not** go-live; do **not** activate CD-3 |
| **[API ops discoverability](./behavior/api-ops-discoverability.md)** | **ready** (2026-07-14) · slice `api-ops-discoverability` | **BDD-API-OPS-DISCOVERABILITY-001** SCEN-AOD-01…15 — C10 frontend alignment (`PENDING_RELEASE` External access); Overview readiness summary (SCEN-ALERT-04); alerts extend MISSING_AD_GROUP to PENDING_RELEASE; published vs runtime callable; **not** independent API catalog / #3b / Boot 4.1 |
| **[CE-K07 golden corpus skeleton](./behavior/ce-k07-golden-corpus-skeleton.md)** | **ready** (2026-07-14) · Task Master **#54** · slice **Done** (`e8f996a0`; tip `91455ca3`) | **BDD-CE-K07-001…019** — `golden-corpus/` ≥8 主题包骨架；ACTIVE 最小样本 `nested-clauses` + `encrypted-pdf`；DOCX 关键路径 + PDF 文本断言（禁像素）；接入 `mvn verify` **GREEN** 1379/0/0/7；K01–K06/G02 后续充实 PLACEHOLDER；formal phase **None**; **not** go-live |
| **[CE-C01+C02 runtime contract strictness](./behavior/ce-c01-c02-contract-strictness.md)** | **ready** (2026-07-14) · Task Master **#56** · slice **Done** (`da08f3fe`; tip `c942da13`) | **BDD-CE-C01** / **BDD-CE-C02** — `context` whitelist + runtime-only `FAIL_ON_UNKNOWN` → `REQUEST_BODY_INVALID`; management DTOs unchanged; C03–C06 out of scope; formal phase **None**; **not** go-live |
| **[CE-U03 test data schema-driven form](./behavior/ce-u03-testdata-schema-form.md)** | **ready** (2026-07-14) · Task Master **#55** · slice **Done** (`22bb391f`; tip `0565e1ae`) | **BDD-CE-U03-TESTDATA-SCHEMA-001** S1–S18 — VariableSchema dynamic form + skeleton + collapsible JSON; `TestDataSetService` fieldErrors on save; skip COMPUTED/computeExpression (K03 weak coupling); E2E 9/9 + UIUX PASS_WITH_NOTES; formal phase **None**; **not** go-live |
| **[CE-U02 block sort / copy / validate scroll](./behavior/ce-u02-block-sort-copy-scroll.md)** | **ready** (2026-07-14) · Task Master **#65** · slice **Done** (`50b7d04d`; tip `a9c98f0f`) | **BDD-CE-U02-BS-01…BS-05** — same-layer drag reorder, block copy, client validation scroll-into-view; E2E 2/2 + UIUX 2/2 @1920 dual-brand; frontend gates GREEN (1211 Vitest); formal phase **None**; **not** go-live |
| **[CE-U05 fidelity viewed persistence + fix path](./behavior/ce-u05-fidelity-viewed-persist.md)** | **ready** (2026-07-14) · Task Master **#66** · slice **Done** (`12741d69`) | **BDD-CE-U05-FVP-001…004** — per-warning viewed persistence; publish gate `FIDELITY_WARNINGS_VIEWED`; human-readable warnings + Edit binding deep link; E2E 4/4 + UIUX PASS_WITH_NOTES; formal phase **None**; **not** go-live |
| **[CE-U04 inline PDF preview (pdf.js)](./behavior/ce-u04-inline-pdf-preview.md)** | **ready** (2026-07-14) · Task Master **#67** · slice **Done** (`feat/ce-u04-inline-pdf-preview`) | **BDD-CE-U04-IPP-001…004** — in-app PDF in `AuthoringPreviewPane` / preview tab; page nav; nginx `.mjs` worker MIME; E2E **3/3** + UIUX **PASS_WITH_NOTES**; CE-G02 watermark soft-dep; formal phase **None**; **not** go-live |
| **[CE-U07 clause outdated bump](./behavior/ce-u07-clause-outdated-bump.md)** | **ready** (2026-07-15) · Task Master **#82** · slice **Done** (`fde9342a`) | **BDD-CE-U07-COB-001…004** — out-of-date badge + one-click bump + dashboard author todo deep link; E2E **3/3** + UIUX **PASS_WITH_NOTES**; formal phase **None**; **not** go-live |
| **[CE-U08 content-module review loop](./behavior/ce-u08-content-module-review-loop.md)** | **ready** (2026-07-15) · Task Master **#83** · slice **Done** (`dd94c25d`) | **BDD-CE-U08-CMRL-001…007** — Dashboard CM pending/rework todos; versions `rejectionReason`; lifecycle `el-timeline` review history (master-aligned); FE GREEN; E2E 3/3; UIUX PASS_WITH_NOTES dual-brand @1920; Flyway V59; formal phase **None**; **not** go-live |
| **[CE-U09 master review reachability](./behavior/ce-u09-master-review-reachability.md)** | **ready** (2026-07-15) · Task Master **#84** · slice **Done** (`2af22254`) | **BDD-CE-U09-MRR-001…007** — Hub Submit/Approve/Reject for current revision; Dashboard `?workspaceTab=approval` deep link; E2E **4/4** + UIUX **PASS** dual-brand @1920; formal phase **None**; **not** go-live |
| **[CE-U10 sharedGroupCodes config UI](./behavior/ce-u10-shared-group-codes-ui.md)** | **ready** (2026-07-15) · Task Master **#85** · slice **Done** (`10aa5c70`; tip `1e4df8a8`) | **BDD-CE-U10-SGC-001…007** — create/settings Share to groups; detail summary; PUT shared-group-codes; E2E **6/6** + UIUX **2/2 PASS** dual-brand @1920; formal phase **None**; **not** go-live |
| **[CE-U06 master anchor visual context](./behavior/ce-u06-master-anchor-context.md)** | **ready** (2026-07-15) · Task Master **#88** · slice **Done** (`7734366e`) | **BDD-CE-U06-MAC-001…009** — revision workspace DOCX overview as anchor position highlight list (no full render) + editable `displayLabel`; historical/PENDING_REVIEW read-only; formal phase **None**; **not** go-live |
| **[CE-K04 semantic change diff + release A/B](./behavior/ce-k04-semantic-change-diff.md)** | **ready** (2026-07-15) · Task Master **#60** · slice **Done** (`d95e4bfd`; tip `f871e0d3`) | **BDD-CE-K04-SCD-001…009** — sentence-level CONTENT semantic diff; `computeBetween`; release A/B API + FE multi-select compare; approval/publish human-readable summaries; formal phase **None**; **not** go-live |
| **[CE-U01 nested editor](./behavior/ce-u01-nested-editor.md)** | **ready** (2026-07-14) · Task Master **#64** · slice **Done** | **BDD-CE-U01-NE-01…NE-05** — recursive nested blocks (max depth 3); path-based mutations; undo/redo compatible |
| **[CE-G01 self-approval block](./behavior/ce-g01-self-approval-block.md)** | **ready** (2026-07-14) · Task Master **#72** · slice **Done** (`c187a230`) | **BDD-CE-G01-001…022** — `SelfApprovalGuard` fail-closed; admin exception + audit; Flyway **V56** |
| **[CE-K01 release-bundle pinning](./behavior/ce-k01-release-bundle-pinning.md)** | **ready** (2026-07-14) · Task Master **#57** · slice **Done** | **BDD-CE-K01-001…022** — publish pins master revision + SHA-256 + pin metadata; runtime reads pinned storage_key; delete protection 409; Flyway **V57** + backfill `PINNED_RETROACTIVELY`; `mvn verify` **GREEN**; formal phase **None**; **not** go-live |
| **[CE-G02 SPECIMEN watermark](./behavior/ce-g02-specimen-watermark.md)** | **ready** (2026-07-15) · Task Master **#73** · slice **Done** (`2ea74018`) | **BDD-CE-G02-DOCX/PDF/RT/GOLD/X/OUT** — preview/test-generate DOCX header+footer + PDF diagonal `SPECIMEN`; runtime formal path zero watermark; golden `06-specimen-watermark` → ACTIVE; `mvn verify` **GREEN** 1492/0/0/8; formal phase **None**; **not** go-live |
| **[CE-K02 master style authority](./behavior/ce-k02-master-style-authority.md)** | **ready** (2026-07-15) · Task Master **#58** · slice **Done** (`2f6792eb`) | **BDD-CE-K02-001…018** — parse styles.xml → per-revision catalog; styleRef > master style > docDefaults; no Calibri hardcode; `MASTER_STYLE_FALLBACK`; golden `dual-font-master` ACTIVE; post-rebase `mvn verify` **GREEN** 1501/0/0/8; Flyway v58; formal phase **None**; **not** go-live |
| **[CE-K03 variable compute engine](./behavior/ce-k03-variable-compute-engine.md)** | **ready** (2026-07-15) · Task Master **#59** · slice **Done** (`06cd58ec`) | **BDD-CE-K03-001…030** — whitelist DSL evaluate before DOCX; FORMAT_*/SPELL_AMOUNT; `VARIABLE_COMPUTE_FAILED`; author validate/evaluate APIs; [ADR-0056](./adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md); formal phase **None**; **not** go-live |
| **[CE-K06 rendering fidelity (a/b/c Done)](./behavior/ce-k06-rendering-fidelity.md)** | **ready** (2026-07-15) · Task Master **#62** · slice **Done** (tip `76297d08`) | **BDD-CE-K06a-001…006** (`485a7f3e`); **BDD-CE-K06b-001…009** (`a689ca87`); **BDD-CE-K06c-001…008** (feat `4418efe6` + fix `76297d08`) — tblHeader; ZXing `qrBarcodeRef`; `attachmentListRef` numbered list; PDF stamp via `renderProfile`; writer-unsupported empty; goldens `02`/`09`/`10`; formal phase **None**; **not** go-live |
| **[CE-C03 fidelityWarnings contract](./behavior/ce-c03-fidelity-warnings-contract.md)** | **ready** (2026-07-15) · Task Master **#68** · slice **Done** (`49fa0a70`) | **BDD-CE-C03-001…010** — batch/task full `FidelityWarning[]`; sync stream header summary documented; OpenAPI/contract align; `mvn verify` **GREEN** 1515/0/8; formal phase **None**; **not** go-live |
| **[CE-C04 credential expires_at persist + expose](./behavior/ce-c04-credential-expires.md)** | **ready** (2026-07-15) · Task Master **#69** · slice **Done** (`c7be8305`) | **BDD-CE-C04-001…012** — Flyway `expires_at` + issue/rotate write; `RuntimeCredentialSummaryView.expiresAt` / `EXPIRING_SOON`; formal phase **None**; **not** go-live |
| **[CE-C05 originalBatchId retry lineage](./behavior/ce-c05-original-batch-id.md)** | **ready** (2026-07-15) · Task Master **#70** · slice **Done** (`405f7cea`) | **BDD-CE-C05-001…012** — optional `originalBatchId`; same-credential `BATCH_ROOT` validation; `404 ORIGINAL_BATCH_NOT_FOUND`; response echo + audit association; E2E N/A; formal phase **None**; **not** go-live |
| **[Management invocation history](./behavior/management-invocation-history.md)** | **ready** | Management-plane invocation history / records journey |
| **[API access cross-package alerts](./behavior/api-access-cross-package-alerts.md)** | **ready** | Cross-package API access alert surfaces |
| **[Cursor scaffold hygiene](./behavior/cursor-scaffold-hygiene.md)** | **not-applicable** (2026-07-14) | Ops/docs agent scaffold — Cursor-only; no product E2E |

## Evidence & acceptance artifacts

| Document | Purpose |
| --- | --- |
| [LR-A7 pagination measurement](./evidence/lrp-a7-pagination/README.md) | Docker PDF page-count corpus (2026-07-10 / merge `abf2048`); Word baseline n/a on host; slim JSON + README (PDFs untracked under `.tmp/`) |
| [Knip dead-code scan](./evidence/slim-knip-scan/README.md) | Frontend Knip 6.26 baseline (2026-07-12) — unused files/exports/deps; `pnpm -C frontend knip` / `.\scripts\knip-scan.ps1` |
| [Dependency security refresh](./evidence/deps-security-refresh/README.md) | Task #49 Maven audit + SBOM regen (2026-07-13; merge `08c7d56`); frontend audit [operations/deps-security-refresh-frontend-audit.md](./operations/deps-security-refresh-frontend-audit.md) |
| [Slim Wave 1b unused exports](./evidence/slim-hygiene/README.md) | Knip unused exports **93→0**; duplicate exports **1→0**; 22 OpenAPI/contract types retained (merge `b7cbc07`) |
| [Slim Wave 2 backend god-class](./evidence/slim-backend/README.md) | AuditRecorder/AuditQuery/VersionLine/Master extracts; `mvn verify` GREEN (merge `6dd76b3`) |
| [Slim R-backend residual](./evidence/slim-r-backend/README.md) | ApiManagement 572→230; TemplateLifecycle 553→312; package-private supports; `mvn verify` GREEN |
| [Slim R2-backend residual](./evidence/slim-r2-backend/README.md) | PublishGateService 429→181; CheckItem + Checklist supports; `mvn verify` GREEN |
| [Slim R3-backend residual](./evidence/slim-r3-backend/README.md) | InvocationRecord 468→287; BatchGeneration 511→311; package-private supports; `mvn verify` GREEN |
| [Slim R4-backend](./evidence/slim-r4-backend/README.md) | TemplateService 466→341; ContractAssembly 451→59; RuntimeGenerationAudit 446→260; package-private supports; `mvn verify` GREEN |
| [Slim R5-frontend](./evidence/slim-r5-frontend/README.md) | DevWorkspace 416→236; NotificationBell 400→201; useDashboardJourney 387→142; four FE gates GREEN |
| [Slim R5-backend](./evidence/slim-r5-backend/README.md) | MasterDocumentService 492→340; ManagementAuditRecorder 492→349; 8 package-private supports; `mvn verify` GREEN |
| [Slim R6-frontend](./evidence/slim-r6-frontend/README.md) | Lifecycle/composables + mid-tier panels &lt;300; FE gates GREEN |
| [Slim R6-backend](./evidence/slim-r6-backend/README.md) | TemplateController split + Collaboration/Runtime/VersionLine peels; `mvn verify` GREEN |
| [Slim R6-render](./evidence/slim-r6-render/README.md) | WriteSession/DocxAssembler/ConditionEvaluator peels under soft warn; `mvn verify` GREEN |
| [Slim R7-frontend](./evidence/slim-r7-frontend/README.md) | MUST Vue mid-tier &lt;260; SHOULD peels; FE gates GREEN |
| [Slim R7-backend](./evidence/slim-r7-backend/README.md) | All Java ≥300 hotspots &lt;300; 8 supports; `mvn verify` GREEN |
| [Slim R8-backend](./evidence/slim-r8-backend/README.md) | Near-line services &lt;260; entities skipped; `mvn verify` GREEN |
| [Slim R8-frontend](./evidence/slim-r8-frontend/README.md) | Near-line Vue/TS &lt;260; FE gates GREEN |
| [Slim R9-frontend](./evidence/slim-r9-frontend/README.md) | Near-250 panels/composables &lt;240; FE gates GREEN |
| [Slim R9-backend](./evidence/slim-r9-backend/README.md) | ApiManagement split + near-250 peels; `mvn verify` GREEN |
| [Slim R10-backend](./evidence/slim-r10-backend/README.md) | Near-240 services &lt;220; `mvn verify` GREEN |
| [Slim R10-frontend](./evidence/slim-r10-frontend/README.md) | stores/api/types barrels + DetailViewBody peel; FE gates GREEN |
| [Slim R11-backend](./evidence/slim-r11-backend/README.md) | Near-220 services &lt;200; `mvn verify` GREEN |
| [Slim R12-frontend](./evidence/slim-r12-frontend/README.md) | Near-190 panels/api/stores peels; FE gates GREEN |
| [Slim R12-backend](./evidence/slim-r12-backend/README.md) | Near-200 services peels; `mvn verify` GREEN |
| [Slim R11-frontend](./evidence/slim-r11-frontend/README.md) | Near-210 panels &lt;200; stores &lt;250; FE gates GREEN |
| [Slim Wave 3 frontend SFC](./evidence/slim-frontend/README.md) | BindingsPanel 1249→170 orchestrator; ManagementShell 671→311 (merge `2cf7cb9`) |
| [Slim R-frontend residual](./evidence/slim-r-frontend/README.md) | CSC Editor 864→177; AuditConsole 588→155 |
| [Slim R2-frontend residual](./evidence/slim-r2-frontend/README.md) | TemplateDetailView 577→388 orchestrator + detail shells (merge `02b299c`) |
| [Slim R3-hubs](./evidence/slim-r3-hubs/README.md) | TemplatePackageHub 501→145; MasterPackageHub 508→112 (merge `23ec86b`) |
| [Slim R3-detail](./evidence/slim-r3-detail/README.md) | ContentModuleDetail 477→110; TemplateList 416→107; MasterRevisionDetail 458→145 (merge `536ea6d`) |
| [Slim R4-composables](./evidence/slim-r4-composables/README.md) | CommandPalette 549→264; CSC editor 551→305; BindingsPanel composable 462→155 |
| [Slim R4-panels](./evidence/slim-r4-panels/README.md) | Clause/VersionLines/DecisionDialog panel peels (merge `2573bc1`) |
| [Slim Wave 4 test DRY](./evidence/slim-tests/README.md) | Platform/VersionLine/Dashboard fixtures DRY; net **−1033** LOC; gates GREEN (merge `b7e279e`) |
| [Slim R-tests residual](./evidence/slim-r-tests/README.md) | AuditQueryServiceTest 746→318; shared `AuditQueryServiceTestSupport`; `mvn verify` GREEN |
| [Demo typography review checklist](./evidence/demo-typography-review-checklist.md) | Human reviewer checklist — fonts, styles, margins, headers/footers, tables, signatures (**P23-T16**; ≥2 CORP + ≥2 RETAIL mandatory samples) |
| [Fundraising demo summary](./evidence/fundraising-demo-summary.md) | 13-template evidence matrix — generate script, E2E, POI tests, manifest paths (**P23-T16**) |
| [Security evidence index](./evidence/security/README.md) | SCA runbook and execution logs |

## ADRs

| Document | Purpose |
| --- | --- |
| [ADR index](./adr/README.md) | Decision records (Accepted = decision, not task done) |
| [ADR-0054 AD Group resolver production boundary](./adr/authorization-security/0054-ad-group-resolver-production-boundary.md) | **Accepted** (2026-07-12) — config stub local/dev/test only; acceptance/production directory SPI **or** startup fail-closed; LDAP/AD coords UNKNOWN; does not supersede ADR-0010 |

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
2. [`.taskmaster/tasks/tasks.json`](../.taskmaster/tasks/tasks.json) (active work)  
3. [`docs/plan/`](./plan/)  
4. [Requirements plan](./requirements/requirements-plan.md)  
5. [PRD](./product/PRD.md)  
6. [Domain model](./domain/domain-model.md)  
7. [Permission matrix](./security/permission-matrix.md)  
8. [ADRs](./adr/)

## Status vocabulary (project / epic / task)

`Not Started` | `In Progress` | `Blocked` | `Done`

Prior wave/epic closure claims are **void** until re-earned with real behavior and green gates.
