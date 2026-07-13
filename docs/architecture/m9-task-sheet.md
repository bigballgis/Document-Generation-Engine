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

> **Sync status (2026-07-13):** Status mirrored in [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Related delivery focus **DEPS-SECURITY-REFRESH** (Task Master **#49**) → **Done** (merge `08c7d56`) for in-repo Critical/High remediation + exception under [quality-gate-threshold-baseline.md](./quality-gate-threshold-baseline.md); **M9-T02 remains In Progress** until org SCA upload is evidenced. Re-earned per [PROJECT-STATUS-RESET.md](../PROJECT-STATUS-RESET.md).

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
| M9-T01 | P0 | Platform | Backend SBOM generation (CycloneDX) | `.\scripts\generate-sbom.ps1 -BackendOnly` → `artifacts/sbom/backend-cyclonedx.json` | Done (in-repo 2026-07-02) |
| M9-T02 | P0 | Platform | Frontend SBOM + intranet SCA | Step 1 Done; org SCA Steps 2–4 pending — [m9-t02-closure-plan.md](./m9-t02-closure-plan.md); related in-repo remediation **Done** under Task Master **#49** (`deps-security-refresh`, merge `08c7d56`) without closing org gate | In Progress |
| M9-T03 | P1 | Platform | Renew/close security exceptions with metadata | Owner + expiry on any residual; **#49** documented Vitest Critical exception (cleanup **#50**) using this metadata pattern without marking M9-T03 Done | Not Started |

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
