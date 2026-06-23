---
id: DOC-ARCH-E12-PHASE2-TASK-SHEET.MD
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

# E12 Phase 2 Task Sheet

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

E12 frontend role-operation journey 鈥?phase 2 tasks.

## Baseline

- **Scope:** Epic E12
- **Priority:** P1
- **Exit criteria:** Phase 2 role journeys executable in UI with tests.

## Dependencies

- E11 partial
- P1 login

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E12-T06 | P0 | Frontend | GLOBAL_ADMIN first API policy task flow | Completes save with confirmation | Done |
| E12-T07 | P0 | Frontend | GROUP_ADMIN scoped policy task flow | Cannot access other groups | Done |
| E12-T08 | P0 | Frontend | TEMPLATE_AUTHOR create + submit test flow | Reaches TESTING state in UI | Done |
| E12-T09 | P0 | Frontend | Forbidden route regression tests | No data leak in DOM/network | Done |
| E12-T10 | P0 | Frontend | Role journey metrics hooks (optional) | Pending user threshold confirmation | Not Started |

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
