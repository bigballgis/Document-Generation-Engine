---
id: DOC-ARCH-M14-TASK-SHEET.MD
type: Architecture View
status: Accepted
sourceOfTruth: false
owners:
  - architecture
  - implementation
dependsOn:
  - docs/plan/master-plan.md
  - docs/PROJECT-STATUS-RESET.md
  - docs/architecture/implementation-task-plan.md
---

# M14 Task Sheet (Wave 14: Batch Transport & Async Acceptance)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

HTTP parity for batch endpoints and 202 async-accepted projection.

## Baseline

- **Scope:** Wave 14
- **Priority:** P0
- **Exit criteria:** All nine OpenAPI ops have transport-level parity including batch.

## Dependencies

- M13 Done
- M3 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M14-T01 | P0 | API adapters | Batch generate HTTP handlers | Ordered items[] in response | Done |
| M14-T02 | P0 | API adapters | 202 async-accepted projection model | task.queryPath in accept response | Done |
| M14-T03 | P0 | API adapters | Batch transport integration tests | Sync fail-all + async partial scenarios | Done |

## Gate commands (when implementation exists)

- Backend: `mvn -B -ntp -f backend/pom.xml verify`
- Frontend: `pnpm -C frontend lint` / `type-check` / `test` / `build`

## Evidence

| Evidence slot | Status |
| --- | --- |
| Unit / integration tests | Done |
| Contract / OpenAPI conformance | Done |
| Quality gate logs | Done |
| Plan status sync | Done |
