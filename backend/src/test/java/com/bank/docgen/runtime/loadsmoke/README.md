# LR-D6 Load Smoke Harness

Flag-gated JUnit harness (no k6 / no APM). Exercises the **Docker acceptance stack** only.

## Flag (mandatory)

| Name | Value | Effect |
| --- | --- | --- |
| `docgen.loadSmoke` | `true` | Enables `LoadSmokeDockerHarnessTest` |
| *(unset / false)* | — | Harness **never** runs in normal `mvn verify` |

## Run against Docker

Prerequisites:

1. Acceptance stack healthy: `http://localhost:8080/healthz`
2. FOL (or chosen) demo **imported + published** with runtime credential at
   `.tmp/credentials/CORP-FOL-OFFER.json` (or override path). Publish via
   `deploy/publish-all-demos.ps1` (or FOL-only publish) after FOL seed
   (`DOCGEN_IMPORT_FOL_DEMO=true` on deploy).
3. **Access account mapping:** default `X-Access-Account` is `lrp-d6-load-smoke`.
   It must resolve to **CORP_API** for FOL. Seed `application.yml` maps
   `svc-caller` / `e2e-runtime-caller` only — use the opt-in compose override
   below (or set `docgen.loadSmoke.accessAccount=svc-caller`).
4. For Scenario B (≥5 **parallel** SSE streams): Docker must allow
   `docgen.preview.max-concurrent >= 5` (default is **3**).

### Opt-in compose override (does **not** affect normal deploy)

Tracked file: `docker-compose.load-smoke.override.yml.example`  
`scripts/docker-deploy.ps1` / `docker-deploy-queue.ps1` **do not** include it.

```powershell
# After images exist — restart backend only with smoke settings:
docker compose `
  -f docker-compose.yml `
  -f docker-compose.prod.yml `
  -f docker-compose.load-smoke.override.yml.example `
  --profile prod up -d --no-deps docgen-backend
```

Equivalent env (before a queued deploy):

```powershell
$env:SPRING_APPLICATION_JSON = '{"docgen":{"preview":{"max-concurrent":8},"ad-group-resolver":{"account-groups":{"lrp-d6-load-smoke":["RETAIL_API","CORP_API"],"svc-caller":["RETAIL_API","CORP_API"],"e2e-runtime-caller":["RETAIL_API","CORP_API"]}}}}'
# then scripts/docker-deploy-queue.ps1
```

### Maven command

```powershell
mvn -B -ntp -f backend/pom.xml -Pdev-fast test `
  -Dtest=LoadSmokeDockerHarnessTest `
  -Ddocgen.loadSmoke=true `
  "-Ddocgen.loadSmoke.stackVersion=$(git rev-parse --short HEAD)" `
  "-Ddocgen.loadSmoke.hardwareNote=Windows host; Docker Desktop acceptance 8080/4173"
```

Optional overrides (system property **or** env):

| Property | Env | Default |
| --- | --- | --- |
| `docgen.loadSmoke.baseUrl` | `DOCGEN_LOAD_SMOKE_BASE_URL` | `http://localhost:8080` |
| `docgen.loadSmoke.environment` | `DOCGEN_LOAD_SMOKE_ENVIRONMENT` | `dev` |
| `docgen.loadSmoke.templateExternalId` | `DOCGEN_LOAD_SMOKE_TEMPLATE_EXTERNAL_ID` | `CORP-FOL-OFFER` |
| `docgen.loadSmoke.credentialFile` | `DOCGEN_LOAD_SMOKE_CREDENTIAL_FILE` | `.tmp/credentials/CORP-FOL-OFFER.json` |
| `docgen.loadSmoke.variablesFile` | `DOCGEN_LOAD_SMOKE_VARIABLES_FILE` | `deploy/demo-fol/config/fol-demo-test-variables.json` |
| `docgen.loadSmoke.evidenceDir` | `DOCGEN_LOAD_SMOKE_EVIDENCE_DIR` | `docs/plan/evidence/lrp-d6-load-smoke` |
| `docgen.loadSmoke.mgmtUser` | `DOCGEN_LOAD_SMOKE_MGMT_USER` | `10000003` (template author) |
| `docgen.loadSmoke.mgmtPassword` | `DOCGEN_LOAD_SMOKE_MGMT_PASSWORD` | seed password |
| `docgen.loadSmoke.accessAccount` | `DOCGEN_LOAD_SMOKE_ACCESS_ACCOUNT` | `lrp-d6-load-smoke` |
| `docgen.loadSmoke.syncConcurrency` | `DOCGEN_LOAD_SMOKE_SYNC_CONCURRENCY` | `20` |
| `docgen.loadSmoke.sseConcurrency` | `DOCGEN_LOAD_SMOKE_SSE_CONCURRENCY` | `5` |
| `docgen.loadSmoke.hardwareNote` | `DOCGEN_LOAD_SMOKE_HARDWARE_NOTE` | placeholder |
| `docgen.loadSmoke.stackVersion` | `DOCGEN_LOAD_SMOKE_STACK_VERSION` | placeholder |

Variables JSON may include a UTF-8 BOM (PowerShell `Set-Content -Encoding UTF8`); the harness strips it.

## Scenarios

- **A** — ≥20 concurrent sync generations, alternating DOCX/PDF via
  `POST /api/{env}/v1/templates/{externalId}/default/generate`
- **B** — ≥5 parallel `async-preview` + SSE `progress-stream`; asserts every started
  stream receives terminal event `completed` or `failed` (no silent drop)

## Evidence / triage

Machine-readable + Markdown under `docs/plan/evidence/lrp-d6-load-smoke/`.

Named defect for concurrent PDF 422s: [TRIAGE-pdf-422.md](../../../../docs/plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) (`DEF-LRP-D6-001`).

Do **not** tune product thresholds to pass — record observed p95/p99/error/pool/stream outcomes.
