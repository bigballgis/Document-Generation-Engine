---
id: DOC-ARCH-E12-PHASE1-TASK-SHEET.MD
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

# E12 Phase 1 Task Sheet

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

E12 frontend role-operation journey 鈥?phase 1 tasks.

## Baseline

- **Scope:** Epic E12
- **Priority:** P1
- **Exit criteria:** Phase 1 role journeys executable in UI with tests.

## Dependencies

- E11 partial
- P1 login

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E12-T01 | P0 | Frontend | GLOBAL_ADMIN governance home page | Lands after login; i18n keys | Done |
| E12-T02 | P0 | Frontend | GROUP_ADMIN governance home page | Scoped to authorized groups | Done |
| E12-T03 | P0 | Frontend | TEMPLATE_AUTHOR authoring home | Template list/create entry | Done |
| E12-T04 | P0 | Frontend | Shared shell components extraction | Theme tokens only; no hardcoded brand | Done |
| E12-T05 | P0 | Frontend | Playwright smoke: 3 role landings | CI job defined | Done |

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
