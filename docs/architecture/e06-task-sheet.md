---
id: DOC-ARCH-E06-TASK-SHEET.MD
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

# E06 Task Sheet (Management UI Product Finish)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Login-first OA management shell with dual-brand theming and grouped navigation.

## Baseline

- **Scope:** Epic E06 鈥?maps to P1, P5, P6, P8 UI
- **Priority:** P1
- **Exit criteria:** UI presents coherent product shell, not a workbench stub.

## Dependencies

- P1 login

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E06-T01 | P0 | Frontend shell | Global header + side nav (4 OA groups) | REDBC/GREENBC theme switch | Done |
| E06-T02 | P0 | Frontend shell | Role-aware landing pages | Forbidden route unified UX | Done |
| E06-T03 | P0 | Frontend shell | Lifecycle governance surfaces | End-to-end journey in shell | Done |
| E06-T04 | P0 | Frontend shell | API governance surfaces | Wired to backend session auth | Done |
| E06-T05 | P1 | Frontend shell | Accessibility + responsive baseline | Vitest/Playwright smoke | Done |

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
