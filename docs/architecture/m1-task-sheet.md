---
id: DOC-ARCH-M1-TASK-SHEET.MD
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

# M1 Task Sheet (Wave 0-1: Foundation & Contract Discovery)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Establish implementation skeleton, shared conventions, and deliver read-only contract visibility endpoints.

## Baseline

- **Scope:** Wave 0 + Wave 1
- **Priority:** P0
- **Exit criteria:** Module map documented; contract + versions endpoints match OpenAPI with auth/audit tests.

## Dependencies

- P0 in master plan
- OpenAPI v1 contract + examples

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M1-T01 | P0 | Platform | Backend/frontend repo skeleton, compose file, health endpoints | Apps boot; README documents local setup | Done |
| M1-T02 | P0 | Shared kernel | Unified error envelope + metadata mapping | Matches OpenAPI error schema in tests | Done |
| M1-T03 | P0 | Authorization | Credential header validation skeleton, fail-closed | 401/403 paths tested | Done |
| M1-T04 | P0 | Contract publication | GET contract + GET versions read models | Schema-valid responses; group-scoped filtering | Done |
| M1-T05 | P0 | Audit | Security audit summary on contract access | Non-sensitive audit fields only | Done |
| M1-T06 | P1 | API adapters | OpenAPI contract validation test suite | CI fails on schema drift | Done |

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
