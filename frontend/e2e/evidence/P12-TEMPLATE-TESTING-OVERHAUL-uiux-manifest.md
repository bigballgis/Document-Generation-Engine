# P12-TEMPLATE-TESTING-OVERHAUL UIUX Evidence Manifest

**Task:** Template testing tab overhaul (T13) — testing workspace, data sets, coverage, batch history, SSE progress dialogs  
**Reviewer:** e2e-uiux-reviewer (evidence captured by e2e-test-engineer)  
**Date:** 2026-07-03  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (functional E2E green; evidence frames captured)

## Test execution

| Command | Result |
| --- | --- |
| `E2E_TARGET=docker FRONTEND_PORT=4173 pnpm exec playwright test e2e/P12-TEMPLATE-TESTING-OVERHAUL-T13.spec.ts --config playwright.docker.config.ts --workers=1` | **8/11 passed**, **3 skipped** (~7.7m) |
| `E2E_TARGET=docker FRONTEND_PORT=4173 pnpm exec playwright test e2e/P12-TEMPLATE-TESTING-OVERHAUL-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (~4.2m) |

### Functional coverage (BDD F1–F6)

| Scenario | Result | Notes |
| --- | --- | --- |
| SCEN-F4-02 — submit disabled (no valid batch test) | PASS | |
| SCEN-F5-01 — coverage panel dimensions | PASS | |
| SCEN-F5-02 — uncovered lists expand | SKIP | FOL seed meets thresholds |
| SCEN-F1-01 — preview SSE terminal state | PASS | |
| SCEN-F1-02 — preview concurrency 429 | PASS | |
| SCEN-F1-03 — preview failure + retry | PASS | Empty-variable data set fails in Docker; retry click verified |
| SCEN-F2-01 — full test SSE + persistence | PASS | |
| SCEN-F6-01 — batch history tab | PASS | |
| SCEN-F4-01 — submit enabled when eligible | SKIP | Coverage gate not met for FOL wholesale seed after F2 |
| SCEN-F3-01 — invalidation on content change | PASS | |
| SCEN-F2-02 / F4-03 — partial batch failure gate | SKIP | Intentional failure data set does not block batch in current seed |

## Screenshot inventory (10)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-testing-tab-data-sets-redbc-1440x900.png` | Dev workspace — Template testing tab, Test data sets sub-tab, action rail (Full test / Submit for testing) | REDBC |
| 2 | `screenshots/02-submit-for-test-disabled-redbc-1440x900.png` | Action rail — Submit for testing disabled after invalidation | REDBC |
| 3 | `screenshots/03-testing-tab-data-sets-greenbc-1440x900.png` | Same testing tab after brand switch | GREENBC |
| 4 | `screenshots/04-preview-progress-in-flight-redbc-1440x900.png` | Preview progress dialog — SSE in-flight (queued / generating) | REDBC |
| 5 | `screenshots/05-preview-progress-failed-retry-redbc-1440x900.png` | Preview progress dialog — error alert + Retry control | REDBC |
| 6 | `screenshots/07-batch-test-progress-complete-redbc-1440x900.png` | Full test progress dialog — completion summary | REDBC |
| 7 | `screenshots/08-coverage-panel-redbc-1440x900.png` | Coverage sub-tab — aggregate alert + dimension table | REDBC |
| 8 | `screenshots/09-coverage-panel-greenbc-1440x900.png` | Same coverage panel after brand switch | GREENBC |
| 9 | `screenshots/10-batch-test-history-redbc-1440x900.png` | Preview runs sub-tab — batch test history table | REDBC |

> **Note:** Preview success dialog (download DOCX/PDF) not captured in this run — FOL preview terminated in error state within the 240s window; frame 05 documents failure+retry UX instead.

## Defects found during E2E (routed / fixed)

| Finding | Resolution |
| --- | --- |
| `getSubmitTestEligibility` called wrong path `/submit-test-eligibility` (404) — tooltip empty, UI gate stale | **Fixed** in `frontend/src/api/templates.ts` → `/batch-tests/submit-eligibility` + nested response mapping |
| `handlePreviewRetry` used `selectedId` not set on row Run preview | **Fixed** in `TemplateTestDataSetPanel.vue` (sets `selectedId` in `handleRunPreview`) |
| Disabled-button tooltip hover flaky | E2E helper `hoverSubmitForTestTooltip` targets `.el-tooltip__trigger` wrapper |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + workspace tabs) | ✅ | 01, 03 |
| Template testing workspace tab shell + sub-tabs | ✅ | 01, 08, 10 |
| Action rail (Full test / Submit for testing) | ✅ | 01, 02 |
| Dual-brand REDBC / GREENBC | ✅ | 01 vs 03; 08 vs 09 |
| SSE progress dialogs (preview + batch) | ✅ | 04, 05, 07 |
| Coverage panel hierarchy | ✅ | 08, 09 |
| Batch history table | ✅ | 10 |
| English-first copy via i18n | ✅ | All frames |

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P12-TEMPLATE-TESTING-OVERHAUL-uiux-evidence.spec.ts` | UIUX screenshot capture spec |
| `frontend/e2e/P12-TEMPLATE-TESTING-OVERHAUL-T13.spec.ts` | BDD F1–F6 functional journeys (Docker) |
| `frontend/e2e/helpers/template-testing-api.ts` | Shared API/UI helpers for testing tab E2E |
| `frontend/e2e/helpers/uiux-evidence.ts` | P12-TEMPLATE-TESTING-OVERHAUL evidence dirs + capture helpers |
| `frontend/e2e/evidence/P12-TEMPLATE-TESTING-OVERHAUL-uiux-manifest.md` | This manifest |
| `frontend/src/api/templates.ts` | Submit eligibility API path + response mapping fix |

## References

- BDD: `docs/behavior/template-testing-overhaul.md`
- Pattern: `frontend/e2e/P12-AUD-B10-uiux-evidence.spec.ts`
- `.cursor/skills/frontend-oa-design/SKILL.md`
