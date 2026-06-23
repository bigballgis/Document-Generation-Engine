---
id: DOC-ARCH-M6-TASK-SHEET.MD
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

# M6 Task Sheet (Wave 6: Lifecycle Governance)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Template lifecycle state machine, publish gate, and lifecycle audit chain.

## Baseline

- **Scope:** Wave 6
- **Priority:** P0
- **Exit criteria:** Full lifecycle journey in UI/API with publish gate and audit evidence.

## Dependencies

- E01 partial
- M4 preview artifacts

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M6-T01 | P0 | Template release governance | State transitions through publish | Deterministic guards per role | Done |
| M6-T02 | P0 | Template release governance | Test/approval opinion forms + evidence confirmation | Warning summary viewed flag in audit | Done |
| M6-T03 | P0 | Template release governance | Publish gate checklist aggregation | Blockers prevent publish | Done |
| M6-T04 | P0 | Template release governance | Stop/restore/deprecate + impact preview | Fail-closed; audit trail | Done |
| M6-T05 | P1 | Collaboration | In-app todos + timeout escalation visibility | No auto-approve; admin escalation only | Done |

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
