---
id: DOC-ARCH-M9-TASK-SHEET.MD
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

# M9 Task Sheet (Wave 9: Dependency Scan Recovery)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Restore online/intranet dependency scan execution and frontend audit loop.

## Baseline

- **Scope:** Wave 9
- **Priority:** P1
- **Exit criteria:** SBOM + SCA evidence captured; frontend audit executable.

## Dependencies

- M8 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M9-T01 | P0 | Platform | Backend SBOM generation (CycloneDX) | Artifact archived per run | Not Started |
| M9-T02 | P0 | Platform | Frontend SBOM + intranet SCA | See m9-t02-closure-plan.md | Not Started |
| M9-T03 | P1 | Platform | Renew/close security exceptions with metadata | Owner + expiry on any residual | Not Started |

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
