# M9-T02 Closure Plan (Frontend Dependency Security)

> **Sync status (2026-07-13):** Re-aligned — split **in-repo automation** vs **org intranet SCA gate**. Related delivery focus **DEPS-SECURITY-REFRESH** (Task Master **#49** / slice `deps-security-refresh`) → **Done** (merge `08c7d56`) for in-repo Step 3 remediation + exception; **M9-T02 remains In Progress** until org SCA Steps 2–4 complete. Mirror: [execution-sync-ledger.md](../plan/execution-sync-ledger.md).

## Task

Close frontend dependency security baseline: SBOM generation + intranet SCA + remediation loop.

## Scope split (document-as-code)

| Layer | In-repo deliverable | Done when |
| --- | --- | --- |
| **A. SBOM generation** | CycloneDX SBOM from `frontend/` (pnpm lockfile) in CI or script | Reproducible artifact path documented; sample SBOM archived in repo or CI artifact |
| **B. Intranet SCA submission** | Procedure + bundle script + execution log; org upload + ticket | Org tool accepts submission; high/critical triaged; [sca-execution-log.md](../../evidence/security/sca-execution-log.md) updated |
| **C. Remediation loop** | Dependency bumps or documented exceptions with expiry | `pnpm lint/type-check/test/build` green after remediation |

**M9-T02 is not Done** until **B** completes — org gate cannot be faked in-repo (constitution: no demo-only Done).

## Steps

| Step | Action | Status |
| --- | --- | --- |
| 1 | Generate frontend CycloneDX SBOM (script or CI job) | **Done** (2026-07-02) — `pnpm -C frontend sbom` / `.\scripts\generate-sbom.ps1` |
| 1b | Generate backend CycloneDX SBOM | **Done** (2026-07-02) — `mvn -f backend/pom.xml -Psbom package -DskipTests` via same script |
| 2 | Submit to approved intranet SCA | **Procedure ready** (2026-07-02) — [intranet-sca-submission-runbook.md](../../evidence/security/intranet-sca-submission-runbook.md) + `prepare-sca-submission-bundle.ps1`; **org upload pending owner** |
| 3 | Remediate or exception-track high/critical | **In-repo Done (related)** — Task Master **#49** Done (2026-07-13; merge `08c7d56`) + **#50** Vitest 3.2.7 Done (2026-07-17; merge `6c8fff7d`; GHSA-5xrq-8626-4rwp **CLOSED**); **does not** mark M9-T02 Done |
| 4 | Re-run `pnpm -C frontend lint/type-check/test/build` | **In-repo Done** after #49 — org SCA triage still pending after upload |

## Related

- [m9-task-sheet.md](./m9-task-sheet.md) — M9-T01 backend SBOM (parallel track); M9-T03 exception metadata (Not Started — #49 documented exception without closing M9-T03)
- [deps-security-refresh.md](../behavior/deps-security-refresh.md) — Task Master **#49** in-repo hygiene **Done** (`bdd_readiness: not-applicable`)
- [quality-gate-threshold-baseline.md](./quality-gate-threshold-baseline.md) — block critical/high in changed deps; Boot **3.3.x** / ShedLock **6.x** / no major Vue·Vite jump without ADR
- [docs/evidence/security/README.md](../evidence/security/README.md) — SBOM paths + SCA evidence index
- SOR-C05 (Done) — CI SBOM/SCA automation wiring **feeds** this plan; #49 does **not** reopen SOR-C05 ([system-optimization-review-2026-07.md](../plan/system-optimization-review-2026-07.md) §3 / §10)
- Tech stack guardrails — company-approved repositories only; no ad-hoc dependency switches
