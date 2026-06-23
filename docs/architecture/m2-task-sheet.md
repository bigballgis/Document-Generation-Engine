---
id: DOC-ARCH-M2-TASK-SHEET.MD
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

# M2 Task Sheet (Wave 2: Synchronous Generation)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Deliver sync generation for explicit version and default route paths including idempotency and output delivery modes.

## Baseline

- **Scope:** Wave 2
- **Priority:** P0
- **Exit criteria:** Sync DOCX/PDF generation with idempotency replay and download URL mode passes integration tests.

## Dependencies

- M1 Done
- Published template exists (E02/E01)

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M2-T01 | P0 | Generation orchestration | Request validation (variables, output, encryption) | 400/422 field errors per OpenAPI | Done |
| M2-T02 | P0 | Generation orchestration | Idempotency key handling + replay/conflict | metadata.idempotencyStatus tested | Done |
| M2-T03 | P0 | Rendering worker | Sync DOCX render job execution | Binary stream response with required headers | Done |
| M2-T04 | P0 | Rendering worker | PDF output + fidelity warnings in JSON path | result.fidelityWarnings[] populated when warnings exist | Done |
| M2-T05 | P0 | API adapters | SYNC_DOWNLOAD_URL delivery + 15min expiry | download.url + download.expiresAt in response | Done |
| M2-T06 | P1 | Encryption | Dynamic encryption parameter validation + execution | ENCRYPTION_* errors; no password in logs | Done |

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
