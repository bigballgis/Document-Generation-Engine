# PRR-D01c Functional Evidence Manifest — Dashboard summary API

**Task:** PRR-D01c / Task Master **#136** — Dashboard Overview stops unbounded masters/templates fetch-all  
**Slice:** `prod-dashboard-summary-api` (`feat/prod-dashboard-summary-api`)  
**BDD:** [docs/behavior/prod-dashboard-summary-api.md](../../../docs/behavior/prod-dashboard-summary-api.md) (`ready`)  
**Date:** 2026-07-18  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (Stage 5 **DEPLOY_OK**)  
**Worktree tip (pre-E2E commit):** `dafa1a81`  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `PRR-D01C-dashboard-summary-api.spec.ts` — BDD-PRR-D01C-001/010 | **passed** |
| `PRR-D01C-dashboard-summary-api.spec.ts` — BDD-PRR-D01C-006 smoke (Tasks) | **passed** |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-001 (regression) | **passed** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test `
  e2e/PRR-D01C-dashboard-summary-api.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 2 passed (12.8s)

pnpm -C frontend exec playwright test `
  e2e/CE-U14-dashboard-lifecycle-todos.spec.ts `
  --config playwright.docker.config.ts --workers=1 -g "BDD-CE-U14-DLT-001"
# 1 passed (19.2s)
```

**Totals this stage:** **3 passed** / 0 failed (2 new leaf + 1 CE-U14 smoke)

**HTML report:** `frontend/playwright-report/docker/`  
**Network capture:** `frontend/e2e/evidence/PRR-D01C-network.json`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| D01C-001 / D01C-010 Overview bounded first paint | GLOBAL_ADMIN Overview reload → `GET /api/management/v1/dashboard/summary` **200**; stats section **Catalog & workflow snapshot** visible; catalog card counts match summary API (`catalogMasters=45`, `catalogTemplates=515`) |
| No unbounded fetch-all | Network capture: `unboundedCatalogListUrls=[]`; no unfiltered `/masters` or `/templates` list GETs; templates list URLs empty on Overview |
| D01C-C6 status-filtered masters (allowed) | Capture shows only `status=PENDING_REVIEW|DRAFT|REJECTED` masters list pages (workflow candidates), not full-catalog merge |
| D01C-006 CE-U14 smoke | Tester `?queue=TEST#tasks-section` loads Tasks; no unfiltered catalog list GETs |
| CE-U14 DLT-001 regression | TEST queue Open still deep-links to testing decision surface |

## Dual-brand

Deferred to **e2e-uiux-reviewer** (functional stage does not require dual-brand per project E2E skill).

## Artifacts

- `frontend/e2e/PRR-D01C-dashboard-summary-api.spec.ts` (new)
- `frontend/e2e/evidence/PRR-D01C-manifest.md` (this file)
- `frontend/e2e/evidence/PRR-D01C-network.json`
