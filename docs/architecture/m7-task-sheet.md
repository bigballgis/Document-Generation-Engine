---
id: DOC-ARCH-M7-TASK-SHEET.MD
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

# M7 Task Sheet (Wave 7: Runtime E2E Integration)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Close adapter-to-HTTP gap with full request flow integration tests.

## Baseline

- **Scope:** Wave 7
- **Priority:** P0
- **Exit criteria:** All nine OpenAPI operations callable end-to-end over HTTP.

## Dependencies

- M2鈥揗4 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M7-T01 | P0 | API adapters | Controller wiring for all runtime ops | RestAssured/contract E2E green | Done |
| M7-T02 | P0 | API adapters | Negative matrix 401/403/404/409/410/422 | Stable error.code per scenario | Done |
| M7-T03 | P0 | Generation orchestration | Trace + audit metadata completeness | traceId/auditId on all responses | Done |

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
