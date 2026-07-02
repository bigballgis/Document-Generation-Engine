# M9-T02 Closure Plan (Frontend Dependency Security)

> **Sync status (2026-07-02):** Re-aligned — split **in-repo automation** vs **org intranet SCA gate**. Mirror: [execution-sync-ledger.md](../plan/execution-sync-ledger.md).

## Task

Close frontend dependency security baseline: SBOM generation + intranet SCA + remediation loop.

## Scope split (document-as-code)

| Layer | In-repo deliverable | Done when |
| --- | --- | --- |
| **A. SBOM generation** | CycloneDX SBOM from `frontend/` (pnpm lockfile) in CI or script | Reproducible artifact path documented; sample SBOM archived in repo or CI artifact |
| **B. Intranet SCA submission** | Procedure + owner + ticket reference | Org tool accepts submission; high/critical triaged |
| **C. Remediation loop** | Dependency bumps or documented exceptions with expiry | `pnpm lint/type-check/test/build` green after remediation |

**M9-T02 is not Done** until **B** completes — org gate cannot be faked in-repo (constitution: no demo-only Done).

## Steps

| Step | Action | Status |
| --- | --- | --- |
| 1 | Generate frontend CycloneDX SBOM (script or CI job) | Not Started |
| 2 | Submit to approved intranet SCA | Not Started (org gate) |
| 3 | Remediate or exception-track high/critical | Not Started |
| 4 | Re-run `pnpm -C frontend lint/type-check/test/build` | Not Started |

## Related

- [m9-task-sheet.md](./m9-task-sheet.md) — M9-T01 backend SBOM (parallel track)
- Tech stack guardrails — company-approved repositories only; no ad-hoc dependency switches
