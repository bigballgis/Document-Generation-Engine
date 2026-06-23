---
id: DOC-ARCH-M3-TASK-SHEET.MD
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

# M3 Task Sheet (Wave 3: Batch & Async Lifecycle)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Batch generation and async task query/cancel with state model and partial-success semantics.

## Baseline

- **Scope:** Wave 3
- **Priority:** P0
- **Exit criteria:** Batch and async paths pass contract and state-transition tests.

## Dependencies

- M2 Done
- Kafka async adapter (E05)

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M3-T01 | P0 | Generation orchestration | Batch item validation + itemId uniqueness | 400 ITEM_ID_DUPLICATED on duplicates | Done |
| M3-T02 | P0 | Generation orchestration | Sync batch all-or-nothing failure | No files on partial validation failure | Done |
| M3-T03 | P0 | Generation orchestration | Async batch accept + PARTIAL_SUCCEEDED | 202 accept; 200 with per-item status | Done |
| M3-T04 | P0 | Generation orchestration | Task query + full replay on idempotency hit | getAsyncTask returns complete state object | Done |
| M3-T05 | P0 | Generation orchestration | Task cancel rules + CANCELLED terminal state | 409 when cancellation not allowed | Done |

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
