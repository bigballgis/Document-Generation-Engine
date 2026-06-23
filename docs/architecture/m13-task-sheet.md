---
id: DOC-ARCH-M13-TASK-SHEET.MD
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

# M13 Task Sheet (Wave 13: Runtime HTTP Transport)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Wire adapters to HTTP controllers with OpenAPI-conformant transport mapping.

## Baseline

- **Scope:** Wave 13
- **Priority:** P0
- **Exit criteria:** HTTP integration tests pass for discovery, generate, task, download.

## Dependencies

- M12 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M13-T01 | P0 | API adapters | HTTP controllers + header/path mapping | Status codes match OpenAPI | Done |
| M13-T02 | P0 | API adapters | Sync stream header metadata | Required headers on file responses | Done |
| M13-T03 | P0 | API adapters | Transport negative-path regression suite | Full matrix automated | Done |

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
