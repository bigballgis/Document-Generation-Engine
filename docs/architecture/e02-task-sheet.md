---
id: DOC-ARCH-E02-TASK-SHEET.MD
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

# E02 Task Sheet (Template Lifecycle Workflow)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

End-to-end lifecycle productization with audit evidence.

## Baseline

- **Scope:** Epic E02 鈥?maps to P5
- **Priority:** P0
- **Exit criteria:** Lifecycle completable in UI/API with audit.

## Dependencies

- E01 partial

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E02-T01 | P0 | Template release governance | Submit test + test decision | Role guards; state machine correct | Done |
| E02-T02 | P0 | Template release governance | Submit approval + approval decision | Evidence summaries required | Done |
| E02-T03 | P0 | Template release governance | Publish + semver release version | API contract generated at publish | Done |
| E02-T04 | P0 | Template release governance | Import/export governance | Import restarts at draft in prod | Done |
| E02-T05 | P1 | Collaboration | Todos + timeout escalation | No proxy approval | Done |

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
