# CE-G03 — Test data PII governance E2E Manifest

**Slice:** `ce-g03-testdata-pii` (CE-G03 / Task Master #74)  
**Stage:** 6 — e2e-test-engineer (functional)  
**Date:** 2026-07-16  
**Placement:** ISOLATED `D:/working/DGE-ce-g03-testdata-pii` / `feat/ce-g03-testdata-pii`  
**BDD readiness:** `ready` ([docs/behavior/ce-g03-testdata-pii.md](../../../docs/behavior/ce-g03-testdata-pii.md))  
**Spec:** `frontend/e2e/CE-G03-testdata-pii.spec.ts`  
**Helper:** `frontend/e2e/helpers/ce-g03-testdata-pii-api.ts`  
**Verdict:** **PASS** (5/5)

## Environment

| Item | Value |
| --- | --- |
| UI | `http://127.0.0.1:4173` |
| API / healthz | `http://127.0.0.1:8080` **UP** |
| Stage 5 | Deployed sole project `documentgenerationengine` (no redeploy this stage) |
| Fixture | Draft template: `customerName`=`PERSONAL_NAME`, `amount`=`NONE` |
| Role | Template Author (`10000003`) |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'; $env:E2E_SKIP_CATALOG_CLEANUP='true'
pnpm -C frontend exec playwright test e2e/CE-G03-testdata-pii.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Result:** **5 passed** (~15.4s) — 2026-07-16

HTML report: `frontend/playwright-report/docker/`

## Scenario mapping

| Test | BDD | Result |
| --- | --- | --- |
| Create dialog shows PII badge on marked fields; handling group visible | BDD-CE-G03-012 | **PASS** |
| Strip `piiHandling` on wire → 422; dialog stays open; no silent success | BDD-CE-G03-013 (+ G03-C9) | **PASS** |
| EXPLICIT path: dialog → reason + secondary required → 201 with audit fields | BDD-CE-G03-014 | **PASS** |
| SYNTHETIC recommended path saves with `piiHandling=SYNTHETIC` | BDD-CE-G03-006 (UI) | **PASS** |
| API create without `piiHandling` → 422 `testDataSetPiiHandlingRequired` | BDD-CE-G03-008 / 011 | **PASS** |

## Artifacts

| Path | Role |
| --- | --- |
| `frontend/e2e/CE-G03-testdata-pii.spec.ts` | Journeys + assertions |
| `frontend/e2e/helpers/ce-g03-testdata-pii-api.ts` | Draft template + PII VariableSchema fixture |
| `frontend/e2e/evidence/CE-G03-testdata-pii-manifest.md` | This manifest |
| `frontend/playwright-report/docker/` | Playwright HTML report |

## Notes / non-blockers

- Canonical smoke script `pnpm test:e2e:docker` does **not** include this slice-specific file (same pattern as CE-U03). Acceptance for CE-G03 is the docker-config command above.
- BDD-013: UI defaults handling to `SYNTHETIC` (cannot deselect radios). E2E exercises the BDD-allowed path «发出后映射 422» by stripping `piiHandling` on the request; client-side clear-gate remains covered by Vitest (`TemplateTestDataSetEditDialog.test.ts`).
- No product code changes in this Stage 6 handoff.
- Out of scope / parked: G04, G06, U13, C06.
- **No commit** (orchestrator constraint).
