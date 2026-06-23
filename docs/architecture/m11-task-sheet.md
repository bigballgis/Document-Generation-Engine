---
id: DOC-ARCH-M11-TASK-SHEET.MD
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

# M11 Task Sheet (Wave 11: Intranet Security Baseline)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Permanent intranet-executable security baseline without public internet dependency.

## Baseline

- **Scope:** Wave 11
- **Priority:** P2
- **Exit criteria:** Continuous intranet gate cadence documented and passing.

## Dependencies

- M10 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M11-T01 | P0 | Platform | Lock intranet SBOM + SCA workflow | Reproducible in corp network | Not Started |
| M11-T02 | P0 | Platform | Close M10 renewed exceptions | Approval trace + timestamp | Not Started |

## Gate commands (when implementation exists)

- Backend: `mvn -B -ntp -f backend/pom.xml verify`
- Frontend: `pnpm -C frontend lint` / `type-check` / `test` / `build`

## Evidence

| Evidence slot | Status |
| --- | --- |
| Unit / integration tests | Not Started |
| Contract / OpenAPI conformance | Not Started |
| Quality gate logs | Not Started |
| Plan status sync | Not Started |
