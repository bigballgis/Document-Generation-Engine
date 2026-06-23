---
id: DOC-ARCH-E03-TASK-SHEET.MD
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

# E03 Task Sheet (API Management Completion)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Full API management product flows for admins and caller contract view.

## Baseline

- **Scope:** Epic E03 鈥?maps to P6
- **Priority:** P0
- **Exit criteria:** Policy lifecycle operable without backend-only workarounds.

## Dependencies

- E02 partial

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E03-T01 | P0 | API management | All five policy domains in UI | Impact preview before save | Done |
| E03-T02 | P0 | API management | Credential admin flows | Secret shown once; expiry reminders | Done |
| E03-T03 | P0 | Contract publication | Caller-facing contract page | Non-sensitive; version diff computed in UI | Done |

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
