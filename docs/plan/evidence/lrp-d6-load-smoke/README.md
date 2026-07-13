# LR-D6 Load Smoke Evidence

Ledger-linked evidence for slice `lrp-d6-load-smoke` (Task Master #37).

## Convention

| Artifact | Role |
| --- | --- |
| `results-<yyyyMMddTHHmmssZ>.json` | Machine-readable run summary (p95/p99, error rate, pool rejections, SSE terminals) |
| `results-<yyyyMMddTHHmmssZ>.md` | Human-readable mirror |
| `latest-summary.json` / `latest-summary.md` | Pointers to the most recent harness run |
| `TRIAGE-pdf-422.md` | Named defect `DEF-LRP-D6-001` for Scenario A PDF 422s |

Placeholders filled by the harness (override via env/property):

- **date / measuredAt** — ISO-8601 UTC from the run
- **stackVersion** — `DOCGEN_LOAD_SMOKE_STACK_VERSION` (default `<stack-version-placeholder>`)
- **hardwareNote** — `DOCGEN_LOAD_SMOKE_HARDWARE_NOTE` (default `<hardware-note-placeholder>`)

## How to produce

See harness README:

`backend/src/test/java/com/bank/docgen/runtime/loadsmoke/README.md`

```powershell
mvn -B -ntp -f backend/pom.xml -Pdev-fast test `
  -Dtest=LoadSmokeDockerHarnessTest `
  -Ddocgen.loadSmoke=true `
  "-Ddocgen.loadSmoke.stackVersion=$(git rev-parse --short HEAD)" `
  "-Ddocgen.loadSmoke.hardwareNote=Windows host; Docker Desktop acceptance 8080/4173"
```

Runs are **not** produced by normal `mvn verify` (flag-gated).

Opt-in Docker settings (preview≥5 + smoke access-account): tracked
`docker-compose.load-smoke.override.yml.example` — pass as an extra `-f` (not used by
`docker-deploy.ps1` by default).
