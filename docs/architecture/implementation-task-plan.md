---
id: DOC-ARCH-IMPLEMENTATION-TASK-PLAN
type: Architecture View
status: Accepted
sourceOfTruth: true
sourceOfTruthScope: Technical wave ordering and milestone mapping only — not product behavior.
owners:
  - architecture
  - implementation
dependsOn:
  - docs/plan/master-plan.md
  - docs/api/openapi-v1.yaml
  - docs/architecture/module-boundaries.md
  - docs/PROJECT-STATUS-RESET.md
related:
  - docs/architecture/tdd-delivery-workflow.md
  - docs/architecture/quality-gate-threshold-baseline.md
---

# Implementation Task Plan

> **Sync status (2026-06-23):** Waves 0–7 and 12–14 re-earned Done (P0–P11).
> M9 in progress (local SBOM; intranet SCA pending). M10–M11 not started.
> Mirror: [execution-sync-ledger.md](../plan/execution-sync-ledger.md).

## Purpose

Maps confirmed OpenAPI v1 and architecture boundaries onto **technical delivery
waves**. Product behavior remains in PRD/domain/API docs.

## OpenAPI v1 runtime scope (nine operations)

| Operation | Path pattern |
| --- | --- |
| getTemplateApiContract | `GET .../templates/{templateId}/contract` |
| listCallableVersions | `GET .../templates/{templateId}/versions` |
| generateDocumentByVersion | `POST .../versions/{releaseVersion}/generate` |
| generateDocumentByDefaultRoute | `POST .../default/generate` |
| batchGenerateByVersion | `POST .../versions/{releaseVersion}/batch-generate` |
| batchGenerateByDefaultRoute | `POST .../default/batch-generate` |
| getAsyncTask | `GET .../tasks/{taskId}` |
| cancelAsyncTask | `POST .../tasks/{taskId}/cancel` |
| downloadDocument | `GET .../documents/{documentId}/download` |

## Repository layout (target)

```text
backend/          Java 21 + Spring Boot 3, module-first packages
frontend/         Vue 3 + TS + Vite + Element Plus
docker-compose.yml
docs/
scripts/          validation and gate automation (to be added in P0)
```

## End-to-end production chain (phase view)

Aligns with [master plan](../plan/master-plan.md):

| Chain step | Primary phase | Milestone |
| --- | --- | --- |
| Scaffold & gates | P0 | M1 (partial) |
| Login & session | P1 | — |
| Master & template UI | P2–P3 | E01 |
| Render & preview | P4 | E01 |
| Lifecycle | P5 | M6, E02 |
| API management | P6 | M5, E03 |
| Runtime API | P7 | M2–M4, M7, M12–M14 |
| Audit | P8 | E04 |
| Hardening | P9 | M8–M11, E07 |

## Delivery waves

| Wave | Goal | Milestone | Status |
| --- | --- | --- | --- |
| 0 | Module skeleton, error envelope, tracing, contract tests, security baseline | M1 | Done |
| 1 | Contract discovery APIs (contract + versions) | M1 | Done |
| 2 | Sync generation (explicit + default route) | M2 | Done |
| 3 | Batch + async task query/cancel | M3 | Done |
| 4 | Secure download + hardening | M4 | Done |
| 5 | API management plane | M5 | Done |
| 6 | Template lifecycle governance backend | M6 | Done |
| 7 | Runtime HTTP integration & E2E | M7 | Done |
| 8 | Security remediation gates | M8 | Done (local verify gates) |
| 9 | Network-enabled dependency scan recovery | M9 | In Progress |
| 10 | Deferred security closure | M10 | Not Started |
| 11 | Intranet-first security baseline | M11 | Not Started |
| 12 | Runtime endpoint adapter completion | M12 | Done |
| 13 | Runtime HTTP transport wiring | M13 | Done |
| 14 | Batch transport + async acceptance | M14 | Done |

## Thin vertical slice (implement first)

Within waves, prioritize:

1. Wave 0 + 1 — foundation and contract read APIs
2. Management login (P1 — not an OpenAPI v1 op)
3. Master + template minimum (E01 — management APIs TBD in P2/P3)
4. Wave 2 — **sync DOCX generation only** for one published template
5. Lifecycle through publish (M6 / E02) before batch/async breadth

## Quality gates (must pass before milestone Done)

**Backend**

```bash
mvn -B -ntp -f backend/pom.xml verify
```

**Frontend**

```bash
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build
```

Coverage: changed lines ≥ 85%; security-critical modules ≥ 90%.

## TDD workflow

Every task: behavior spec → failing test → implement → refactor → gates → update
task sheet status. See [tdd-delivery-workflow.md](./tdd-delivery-workflow.md).

## Milestone task sheets

| Milestone | Sheet |
| --- | --- |
| M1 | [m1-task-sheet.md](./m1-task-sheet.md) |
| M2 | [m2-task-sheet.md](./m2-task-sheet.md) |
| M3 | [m3-task-sheet.md](./m3-task-sheet.md) |
| M4 | [m4-task-sheet.md](./m4-task-sheet.md) |
| M5 | [m5-task-sheet.md](./m5-task-sheet.md) |
| M6 | [m6-task-sheet.md](./m6-task-sheet.md) |
| M7 | [m7-task-sheet.md](./m7-task-sheet.md) |
| M8 | [m8-task-sheet.md](./m8-task-sheet.md) |
| M9 | [m9-task-sheet.md](./m9-task-sheet.md) |
| M10 | [m10-task-sheet.md](./m10-task-sheet.md) |
| M11 | [m11-task-sheet.md](./m11-task-sheet.md) |
| M12 | [m12-task-sheet.md](./m12-task-sheet.md) |
| M13 | [m13-task-sheet.md](./m13-task-sheet.md) |
| M14 | [m14-task-sheet.md](./m14-task-sheet.md) |

## Epic task sheets

| Epic | Sheet |
| --- | --- |
| E01 | [e01-task-sheet.md](./e01-task-sheet.md) |
| E02 | [e02-task-sheet.md](./e02-task-sheet.md) |
| E03 | [e03-task-sheet.md](./e03-task-sheet.md) |
| E04 | [e04-task-sheet.md](./e04-task-sheet.md) |
| E05 | [e05-task-sheet.md](./e05-task-sheet.md) |
| E06 | [e06-task-sheet.md](./e06-task-sheet.md) |
| E07 | [e07-task-sheet.md](./e07-task-sheet.md) |

## Pending confirmations

None blocking wave 0 start. Numeric SLO targets will be set after first load-test cycle.
