# CE-U18 Functional Evidence Manifest — Batch test history drill-down

**Task:** CE-U18 / Task Master **#93** — Batch test history sampleResults expand + async-only full-test path  
**Slice:** `ce-u18-batch-test-history` (`feat/ce-u18-batch-test-history`)  
**Worktree:** `D:/working/DGE-ce-u18-batch-test-history`  
**BDD:** [docs/behavior/ce-u18-batch-test-history.md](../../../docs/behavior/ce-u18-batch-test-history.md) (`ready`)  
**Date:** 2026-07-17  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS** (3/3)

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U18-batch-test-history.spec.ts` — BDD-CE-U18-BTH-001/002 | **passed** |
| `CE-U18-batch-test-history.spec.ts` — BDD-CE-U18-BTH-003 | **passed** |
| `CE-U18-batch-test-history.spec.ts` — BDD-CE-U18-BTH-004/005/006 | **passed** |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/CE-U18-batch-test-history.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 3 passed (24.1s)
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/ce-u18-stage6-e2e/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| BTH-001 Expand sampleResults | Preview runs → expand history row → `[data-testid=batch-history-sample-results]` shows Sample results + ≥2 data-set ids + Succeeded/Failed |
| BTH-002 Open data set | Click `[data-testid=batch-history-open-data-set]` → `testingTab=dataSets` + Data sets tab selected + `tr.is-selected-row` |
| BTH-003 Unmatched feedback | Route-injected missing `dataSetExternalId` → Open data set → still `testingTab=dataSets` + English warning toast (`Could not find data set`) |
| BTH-004 Async-only run | Confirm Full test → `POST .../batch-tests/run` (202); zero `POST .../previews/batch-test` |
| BTH-005 Completed no sync | After async completion / dialog close: still zero sync batch posts; no sync batch-success toast |
| BTH-006 No sync UI entry | Action rail shows **Full test** only; no Batch test generate / sync batch controls |

### Fixture notes

- Draft template via `prepareDraftTemplateWithCleanBinding` + two `E2E CE-U18` test data sets (`E2E-` prefix; global teardown cleans)
- FOL catalog not required (demo master / RETAIL seed via `assertDemoCatalogSeeded`)
- History seeded with `POST .../batch-tests/run` when top row lacks ≥2 `sampleResults`

## Artifacts added / updated

- `frontend/e2e/CE-U18-batch-test-history.spec.ts` (new)
- `frontend/e2e/helpers/template-testing-api.ts` (`BatchTestHistorySampleResult` + `sampleResults` on summary)
- `frontend/e2e/evidence/CE-U18-manifest.md` (this file)
- `docs/plan/evidence/ce-u18-stage6-e2e/CE-U18-stage6-e2e-manifest.md`

## Notes for e2e-uiux-reviewer (stage 7)

1. Dual-brand @1920: Preview runs history expand + Sample results nested table + Open data set.
2. English-first: Sample results / Open data set / Open preview / Could not find data set / Full test.
3. Confirm expand panel scroll does not break bank OA layout with multiple samples.
4. No merge / MAIN doc-sync from stage 6.
