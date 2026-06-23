---
id: DOC-ARCH-E05-TASK-SHEET.MD
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

# E05 Task Sheet (Enterprise Integration Hardening)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Replace transitional seams with production adapters for persistence, cache, messaging, storage, directory, secrets.

## Baseline

- **Scope:** Epic E05 鈥?maps to P0, P7, P9
- **Priority:** P0
- **Exit criteria:** Core paths use real adapters; external env evidence tracked separately.

## Dependencies

- P0 compose

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| E05-T01 | P0 | Infrastructure | PostgreSQL repositories (no in-memory primary store) | Flyway migrations; integration tests | Done |
| E05-T02 | P0 | Infrastructure | Redis cache + idempotency | Fail-closed when Redis unavailable | Done |
| E05-T03 | P0 | Infrastructure | MinIO artifact storage | Upload/download round-trip | Done |
| E05-T04 | P0 | Infrastructure | Kafka async publisher + consumer | Retry + DLT configured | Done |
| E05-T05 | P0 | Authorization | Enterprise AD Group resolver adapter | 503 fail-closed without cache | Done |
| E05-T06 | P1 | Platform | External validation evidence ledger | See e05-external-validation-evidence.md | Not Started |

## Gate commands

- Backend: `mvn -B -ntp -f backend/pom.xml verify`
- Frontend: `pnpm -C frontend lint` / `type-check` / `test` / `build`

## Evidence

| Evidence slot | Status |
| --- | --- |
| Unit / integration tests | Done |
| Contract / OpenAPI conformance | Done |
| Quality gate logs | Done (2026-06-23) |
| Plan status sync | Done |
| External env validation | Not Started (E05-T06) |
