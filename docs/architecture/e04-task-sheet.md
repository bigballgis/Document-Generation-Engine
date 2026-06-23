---
id: DOC-ARCH-E04-TASK-SHEET.MD
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

# E04 Task Sheet (Audit & Governance Console)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Audit search, filter, export with role scope and masking.

## Baseline

- **Scope:** Epic E04 鈥?maps to P8
- **Priority:** P1
- **Exit criteria:** Audit roles use complete console within permission boundaries.

## Dependencies

- E02
- E03

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E04-T01 | P0 | Audit | Audit query API + scope filters | GROUP_ADMIN cannot see other groups | Done |
| E04-T02 | P0 | Audit | Masked export | No sensitive plaintext in export | Done |
| E04-T03 | P0 | Audit | Audit console UI | English i18n; time window filters | Done |

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
