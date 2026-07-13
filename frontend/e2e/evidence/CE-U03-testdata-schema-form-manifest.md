# CE-U03 — Schema-driven test data form E2E Manifest

**Slice:** `ce-u03-testdata-schema-form` (CE-U03 / Task Master #55 residual)  
**Stage:** 6 — e2e-test-engineer (functional)  
**Date:** 2026-07-14  
**Placement:** ISOLATED `D:/working/DGE-ce-u03-testdata-schema-form` / `feat/ce-u03-testdata-schema-form`  
**BDD readiness:** `ready` ([docs/behavior/ce-u03-testdata-schema-form.md](../../../docs/behavior/ce-u03-testdata-schema-form.md))  
**Spec:** `frontend/e2e/CE-U03-testdata-schema-form.spec.ts`  
**Helper:** `frontend/e2e/helpers/ce-u03-testdata-schema-api.ts`  
**Verdict:** **PASS** (9/9)

## Environment

| Item | Value |
| --- | --- |
| UI | `http://127.0.0.1:4173` |
| API / healthz | `http://127.0.0.1:8080` **UP** |
| Stage 5 | **DEPLOY_OK_WITH_NOTES** (backend docker healthcheck wget note; app UP) |
| Fixture | Compact VariableSchema (TEXT/AMOUNT/ENUM/BOOLEAN + COMPUTED) + large ≥12 TEXT schema |
| Role | Template Author (`10000003`); Tester deny path (`10000005`) |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'; $env:E2E_SKIP_CATALOG_CLEANUP='true'
pnpm -C frontend exec playwright test e2e/CE-U03-testdata-schema-form.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Result:** **9 passed** (~27.8s) — 2026-07-14

HTML report: `frontend/playwright-report/docker/`

## Scenario mapping (BDD-CE-U03-TESTDATA-SCHEMA-001)

| Test | BDD | Result |
| --- | --- | --- |
| Create dialog schema form; skip compute; no Sample hardcode | S1 / S5 / S8 | **PASS** |
| Generate from schema fills defaults + boolean placeholder | S4 | **PASS** |
| Required empty blocks Save (no API) | S3 | **PASS** |
| Type mismatch via Advanced JSON blocks Save | S2 | **PASS** |
| Invalid JSON blocks Save | S7 | **PASS** |
| Backend `fieldErrors` map into dialog summary (route stub) | S9 | **PASS** |
| Skeleton → edit field → save creates row | Save-flow (primary journey) | **PASS** |
| ≥12 variables expands Advanced JSON; JSON→form sync | S6 | **PASS** |
| TEMPLATE_TESTER create denied (API fail-closed) | S10 | **PASS** |

## Artifacts

| Path | Role |
| --- | --- |
| `frontend/e2e/CE-U03-testdata-schema-form.spec.ts` | Journeys + assertions |
| `frontend/e2e/helpers/ce-u03-testdata-schema-api.ts` | Draft template + VariableSchema fixtures |
| `frontend/e2e/evidence/CE-U03-testdata-schema-form-manifest.md` | This manifest |
| `frontend/playwright-report/docker/` | Playwright HTML report |

## Notes / non-blockers

- Canonical smoke script `pnpm test:e2e:docker` does **not** include this slice-specific file (same pattern as LRP/CDP slice specs). Acceptance for CE-U03 is the docker-config command above.
- S9 uses a route stub to force `fieldErrors` (client also validates; cannot reliably reach live backend `REQUIRED` without bypassing UI).
- No product code changes in this Stage 6 handoff.
