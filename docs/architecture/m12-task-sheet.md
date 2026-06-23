---
id: DOC-ARCH-M12-TASK-SHEET.MD
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

# M12 Task Sheet (Wave 12: Runtime Endpoint Adapters)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Complete adapter-level orchestration before HTTP transport wiring.

## Baseline

- **Scope:** Wave 12
- **Priority:** P0
- **Exit criteria:** Adapter tests cover positive + negative paths for all runtime ops.

## Dependencies

- M7 partial

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M12-T01 | P0 | API adapters | Discovery adapter flows | Adapter-level tests green | Done |
| M12-T02 | P0 | API adapters | Generate adapter flows (sync/async accept) | Idempotency-safe adapter tests | Done |
| M12-T03 | P0 | API adapters | Task + download adapter flows | Secondary auth at adapter boundary | Done |

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
