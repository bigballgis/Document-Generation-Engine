---
id: DOC-ARCH-M5-TASK-SHEET.MD
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

# M5 Task Sheet (Wave 5: API Management)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Template-level API policy management plane with policyVersion and impact preview.

## Baseline

- **Scope:** Wave 5
- **Priority:** P0
- **Exit criteria:** Admins manage all policy domains through product flows with audit.

## Dependencies

- M1 Done
- E03 alignment

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M5-T01 | P0 | API management | Policy aggregate + per-domain save | policyVersion increments; API_POLICY_UPDATED audit | Done |
| M5-T02 | P0 | API management | Credential lifecycle CRUD + one-time secret | Fingerprint only after create/rotate | Done |
| M5-T03 | P0 | API management | AD Group authorization config + cache invalidation | Immediate effect; 5min cache rule | Done |
| M5-T04 | P0 | API management | Default route target change + impact preview | Audit + contract reflects target version | Done |
| M5-T05 | P1 | API management | Management UI for policy domains | English i18n; hard block vs warning UX | Done |

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
