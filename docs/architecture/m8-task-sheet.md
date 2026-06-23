---
id: DOC-ARCH-M8-TASK-SHEET.MD
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

# M8 Task Sheet (Wave 8: Security Remediation Gates)

> **Sync status (2026-06-23):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

## Purpose

Dependency remediation inventory and enforceable static/security release gates.

## Baseline

- **Scope:** Wave 8
- **Priority:** P1
- **Exit criteria:** High/critical findings remediated or exception-tracked; gates executable.

## Dependencies

- M7 Done

## Task board

| Task ID | Priority | Module | Scope | Acceptance criteria | Status |
| --- | --- | --- | --- | --- | --- |
| M8-T01 | P0 | Platform | Checkstyle + SpotBugs + PMD baseline | mvn verify includes all three | Done |
| M8-T02 | P0 | Platform | JaCoCo thresholds enforced | 85%/90% gates in pom | Done |
| M8-T03 | P1 | Platform | Dependency advisory inventory + disposition | No untracked high/critical | Done |

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
