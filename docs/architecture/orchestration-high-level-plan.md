---
id: DOC-ARCH-ORCHESTRATION-HIGH-LEVEL-PLAN
type: Architecture View
status: Accepted
sourceOfTruth: true
sourceOfTruthScope: Epic ordering, active epic selection, and delivery focus only — not product behavior.
orchestrationSourceOfTruth: true
owners:
  - architecture
  - orchestration
dependsOn:
  - docs/plan/master-plan.md
  - docs/PROJECT-STATUS-RESET.md
  - docs/product/PRD.md
  - docs/requirements/requirements-plan.md
---

# Orchestration High-Level Plan

> **Sync status (2026-06-23):** P0–P11 re-earned Done. Epic status mirrored in
> [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Reset history:
> [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

This document defines **epic-level delivery order** for the fullstack orchestrator.
It does not define product behavior — that remains in requirements, PRD, domain
model, permissions, API docs, and ADRs.

## Where to start

1. [Master plan](../plan/master-plan.md) — phase status and exit criteria
2. Active epic task sheet (below)
3. Source-of-truth product/domain/API documents for the behavior being built

## Delivery assessment (current repository)

| Area | Status |
| --- | --- |
| Requirements, PRD, domain, permissions, OpenAPI v1 | Documented — preserved |
| Accepted ADRs & architecture views | Documented — preserved |
| Backend (`backend/`) | **Present** — Spring Boot 4, module-first packages |
| Frontend (`frontend/`) | **Present** — Vue 3 + TS management UI |
| Infrastructure (compose, CI scripts) | **Present** — docker-compose, gate scripts |
| Quality gates (2026-06-23) | Backend `mvn verify` + frontend lint/type-check/test/build green |

## Epic backlog

| Epic | Priority | Active | Scope summary | Status | Depends on |
| --- | --- | --- | --- | --- | --- |
| E01 | P0 | No | Master upload/review, template authoring core, structured editing, preview entry | Done | PRD, domain |
| E02 | P0 | No | Lifecycle: test → approve → publish → stop/restore/deprecate | Done | E01 |
| E03 | P0 | No | API management: credentials, AD Group, output/batch/encryption/default route | Done | E02 |
| E04 | P1 | No | Audit console: search, filter, masked export, role scope | Done | E02, E03 |
| E05 | P0 | No | Enterprise adapters: PostgreSQL, Redis, Kafka, MinIO, AD directory, secrets | Done (thin slice) | P0 foundation |
| E06 | P1 | No | Management UI product shell: login-first, OA layout, dual-brand | Done | E01–E05 slices |
| E07 | P2 | No | Production readiness: gates, observability, deployment evidence | Done (local gates) | E01–E06 |
| E11 | P1 | No | Role-journey navigation after login (GLOBAL/GROUP/TEMPLATE_AUTHOR) | Done | P1 login |
| E12 | P1 | No | Frontend role-operation journey UI continuation | **Done** (2026-06-30; P21 + UX Wave A/B) — see [e12-phase3-task-sheet.md](../architecture/e12-phase3-task-sheet.md) | E11 |

## Active epic rule

- Exactly **one** epic may be `Active = Yes` (status `In Progress`).
- **Current active epic:** none — MVP epics re-earned Done.
- **In-flight (non-epic):** **Published template test artifacts** TM **#144** → **Done** (`ac36ecbc` / `6bc74ff1`; sole-active **cleared**; [published-template-test-artifacts.md](../plan/detail/published-template-test-artifacts.md)). **Bank-letter demo refresh/expand** Wave A **#141** + Wave B **#142** → **Done** (`aa88170f` / `288ce98f`; registry **20/20**; sole-active **cleared**; [bank-letter-demo-refresh.md](../plan/detail/bank-letter-demo-refresh.md)). **IBL** Wave **IBL-A Done** (A1–A6 **#107–#112 Done**; A6 tip `5cacff2a`); Wave **IBL-B** **In Progress** (**#113** IBL-B1 **Done** `a33da272` / `44237c99`; **#114** IBL-B2 **Done** `29d022b6` / `3dd1aa60`+`36a9821c`; **#115** IBL-B3 **Done** `3710811a` / `c81054b0`+`e0102ddb`; **#116** IBL-B4 **Done** `610eb0fa` / `d6b389d1`+`d2e8a1c9`; **#117** IBL-B5 **Done** `1666312b` / `d7459405`+`fbb40429`; **#118** IBL-B6 **Done** `8722f4f1` / `8e8c62e6`; B7 Blocked); Wave **IBL-E** **Done** (E1–E7 **#128–#134 Done** — E7 `37239d68` / `68abc7c3`; F15 descope / ADR-0068; sole-active cleared); Wave **IBL-D** **Done** (**#123–#127** D1–D5 **Done** — D5 `6f672271` / `2e56787e`; F23 legalhold half closed; Playwright residual OUT); Wave **IBL-C** **Done** (**#120–#122** C1–C3 **Done** — C3 `bdfc285d` / `dbfff086`; F19 honesty SYNTHETIC; LO residual → D2/F21; [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md)); **CDP** planned waves **Done** (CD-0/CD-2/CD-3; #140 `b933965c`; sole-active cleared; **not** go-live; [competitiveness-deepening-program.md](../plan/competitiveness-deepening-program.md)); **P22** Done; **LRP** planned waves A–E **Done** ([launch-readiness-program.md](../plan/launch-readiness-program.md); checklist **CONDITIONAL** — **not** go-live; Word Path E / XSD/LO24 residuals deferred).

## Recommended activation sequence

For the agreed **thin vertical slice**:

```text
P0 scaffold → P1 login (E06/E11 start) → E01 (P2–P4) → E02 (P5)
  → E03 (P6) → E05 adapters alongside → minimal P7 sync API → E04/E07 later
```

## Epic exit criteria (summary)

### E01 — Master & template authoring

User can upload/approve a master, create a template, configure variables and
structured content, and trigger test generation with preview evidence.

### E02 — Lifecycle workflow

Template moves through test, approval, and publish in UI/API with audit trail
and publish gate checklist.

### E03 — API management

Admin manages full template-level API policy lifecycle with impact preview and
`policyVersion` audit.

### E04 — Audit console

Audit roles search/export within authorization boundaries; summaries are non-sensitive.

### E05 — Enterprise integration

No in-memory-only production paths for persistence, cache, messaging, storage,
directory, or secrets.

### E06 — Management UI

Coherent login-first OA product shell — not a workbench stub.

### E07 — Production readiness

Release gates executable with captured evidence.

## Cross-cutting governance

| ID | Rule |
| --- | --- |
| G01 | Document-as-code: docs before/with code; see `.cursor/rules/document-as-code-constitution.mdc` |
| G02 | English-first i18n: message keys for all user-facing strings |
| G03 | BDD + TDD: behavior spec → failing test → implement → green gates |
| G04 | Fail-closed authorization and sensitive-data handling per ADR 0020 |

## Status vocabulary

`Not Started` | `In Progress` | `Blocked` | `Done`

Do not use: Planned, Partially complete, In progress (legacy) — normalize to the above.

## Refresh triggers

Update this file when: user reprioritizes epics, a phase completes, or repository
reality diverges from stated order.
