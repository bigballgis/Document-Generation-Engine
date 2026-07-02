# Security evidence — SBOM & intranet SCA

CycloneDX SBOM and intranet SCA submission evidence for **M9**. Generated binaries are **not committed** — they live under `artifacts/` (gitignored).

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

## SBOM outputs

| File | Source |
| --- | --- |
| `artifacts/sbom/frontend-cyclonedx.json` | `pnpm -C frontend sbom` / `@cyclonedx/cyclonedx-npm` |
| `artifacts/sbom/backend-cyclonedx.json` | `mvn -f backend/pom.xml -Psbom package -DskipTests` |

## Status (2026-07-02)

| Step | In-repo | Org gate |
| --- | --- | --- |
| SBOM generation | **Done** | — |
| Submission bundle + runbook | **Done** | — |
| Intranet SCA upload | — | **Pending owner** |
| Remediation + re-gates | — | After scan |

**M9-T02 is not Done** until intranet submission is recorded in [sca-execution-log.md](./sca-execution-log.md).
