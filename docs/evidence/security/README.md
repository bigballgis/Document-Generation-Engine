# Security evidence — SBOM generation

CycloneDX SBOM artifacts for intranet SCA submission (M9). **Generated files are not committed** — they live under `artifacts/sbom/` (gitignored).

## Generate

From repository root:

```powershell
.\scripts\generate-sbom.ps1
```

Or frontend only:

```powershell
pnpm -C frontend sbom
```

## Outputs

| File | Source |
| --- | --- |
| `artifacts/sbom/frontend-cyclonedx.json` | `@cyclonedx/cyclonedx-npm` (pnpm project; `--ignore-npm-errors` for npm ls under pnpm) |
| `artifacts/sbom/backend-cyclonedx.json` | `mvn -f backend/pom.xml -Psbom package -DskipTests` → `backend/target/bom.json` |

## Org gate (not in-repo)

Submit generated JSON to approved intranet SCA per [m9-t02-closure-plan.md](../architecture/m9-t02-closure-plan.md) Step 2. **M9-T02 remains Not Started** until org submission completes.
