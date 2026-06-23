---
id: DOC-ARCH-M4-TASK-SHEET.MD
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

# M4 Task Sheet (Wave 4: Download Security)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Secure document download with secondary authorization, expiry, and audit.

## Baseline

- **Scope:** Wave 4
- **Priority:** P0
- **Exit criteria:** Download endpoint enforces template-level re-auth and expiry; audit complete.

## Dependencies

- M2 or M3 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M4-T01 | P0 | API adapters | GET download with documentId resolution to template | 403/404/410 cases tested | Done |
| M4-T02 | P0 | Authorization | Secondary auth: credential + AD Group + template | Fail-closed; no version re-check at download | Done |
| M4-T03 | P0 | Artifact storage | Presigned/stream download from MinIO | Multiple downloads within 15min window | Done |
| M4-T04 | P1 | Audit | Download audit summary | No full URL in audit/log UI | Done |

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
