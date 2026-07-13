# Security evidence — SBOM & intranet SCA

CycloneDX SBOM and intranet SCA submission evidence for **M9**. Generated binaries are **not committed** — they live under `artifacts/` (gitignored).

Related in-repo hygiene (does **not** close the org SCA gate): Task Master **#49** / slice `deps-security-refresh` → **Done** (merge `08c7d56`) — [behavior note](../../behavior/deps-security-refresh.md); evidence [deps-security-refresh/](../deps-security-refresh/README.md); critical/high policy in [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md).

## Quick start

```powershell
# 1. Generate SBOM
.\scripts\generate-sbom.ps1

# 2. Prepare org submission bundle (manifest + checksums + checklist)
.\scripts\prepare-sca-submission-bundle.ps1
```

## Documents

| Doc | Purpose |
| --- | --- |
| [intranet-sca-submission-runbook.md](./intranet-sca-submission-runbook.md) | M9-T02 Step 2–5 procedure |
| [sca-execution-log.md](./sca-execution-log.md) | Record each intranet submission cycle |
| [m9-t02-closure-plan.md](../../architecture/m9-t02-closure-plan.md) | Task steps & Done definition |
| [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md) | Critical/high block + #49 baseline upgrade constraints |
| [deps-security-refresh.md](../../behavior/deps-security-refresh.md) | Task Master #49 ops-hygiene slice **Done** (`bdd_readiness: not-applicable`) |

## SBOM outputs

| File | Source |
| --- | --- |
| `artifacts/sbom/frontend-cyclonedx.json` | `pnpm -C frontend sbom` / `@cyclonedx/cyclonedx-npm` |
| `artifacts/sbom/backend-cyclonedx.json` | `mvn -f backend/pom.xml -Psbom package -DskipTests` |

## Status (2026-07-13)

| Step | In-repo | Org gate |
| --- | --- | --- |
| SBOM generation | **Done** (2026-07-02) | — |
| Submission bundle + runbook | **Done** (2026-07-02) | — |
| Intranet SCA upload | — | **Pending owner** (M9-T02) |
| Remediation + re-gates | **In-repo Done** — Task Master **#49** (merge `08c7d56`); SBOM regenerated; Vitest Critical exception → **#50** | Org triage still after upload; #49 does **not** mark M9-T02 Done |

**M9-T02 is not Done** until intranet submission is recorded in [sca-execution-log.md](./sca-execution-log.md).
