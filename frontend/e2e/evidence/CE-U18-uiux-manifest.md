# CE-U18 UIUX Evidence Manifest — Batch test history sampleResults expand

**Task:** CE-U18 / Task Master **#93** — Batch test history expand + Sample results + async-only Full test rail  
**Slice:** `ce-u18-batch-test-history` (`feat/ce-u18-batch-test-history`)  
**Worktree:** `D:/working/DGE-ce-u18-batch-test-history`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 DEPLOY_OK)  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U18-batch-test-history.spec.ts` | **3/3 passed** (see `CE-U18-manifest.md`) |
| Stage 7 evidence: `CE-U18-batch-test-history-uiux-evidence.spec.ts` | **2/2 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (expanded Sample results REDBC/GREENBC + after Open data set) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U18-batch-test-history-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 11 passed (52.8s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-batch-history-expanded-redbc-1920x1080.png` | REDBC | Preview runs — history row expanded + Sample results + Full test rail |
| 1b | `01b-batch-test-history-panel-crop-redbc-1920x1080.png` | REDBC | `.batch-test-history` panel crop |
| 1c | `01c-sample-results-expand-crop-redbc-1920x1080.png` | REDBC | `[data-testid=batch-history-sample-results]` crop |
| 1d | `01d-full-test-action-rail-crop-redbc-1920x1080.png` | REDBC | `.workspace-tab-shell__actions` — Full test primary |
| 1e | `01e-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-batch-history-expanded-greenbc-1920x1080.png` | GREENBC | Expanded history dual-brand |
| 2b | `02b-batch-test-history-panel-crop-greenbc-1920x1080.png` | GREENBC | History panel crop |
| 2c | `02c-sample-results-expand-crop-greenbc-1920x1080.png` | GREENBC | Sample results crop |
| 2d | `02d-full-test-action-rail-crop-greenbc-1920x1080.png` | GREENBC | Full test teal primary |
| 2e | `02e-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-open-data-set-data-sets-tab-redbc-1920x1080.png` | REDBC | After Open data set → `testingTab=dataSets` |
| 3b | `03b-data-sets-panel-crop-redbc-1920x1080.png` | REDBC | `.test-data-set-panel` crop |
| 3c | `03c-full-test-rail-after-open-redbc-1920x1080.png` | REDBC | Full test still on action rail |
| 4 | `04-open-data-set-data-sets-tab-greenbc-1920x1080.png` | GREENBC | Data sets tab dual-brand |
| 4b | `04b-data-sets-panel-crop-greenbc-1920x1080.png` | GREENBC | Data sets panel crop |
| 4c | `04c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/CE-U18/screenshots/` (**16** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02, 03–04 |
| Logo / brand header switch | ✅ | 01e Red Bank; 02e / 04c Green Bank |
| BTH-001 Sample results expand | ✅ | 01 / 01c / 02 / 02c — heading + ≥2 data-set ids + Succeeded + Open data set |
| Nested sample table density / no overflow | ✅ | 01c / 02c; spec `assertNoViewportOverflow` + samples box width ≤ 1920 |
| BTH-002 Open data set → Data sets tab | ✅ | 03 / 04 — Test data sets selected; panel visible |
| BTH-006 Full test action rail (async-only path) | ✅ | 01d / 02d / 03c — **Full test** only; no Batch test generate / sync batch |
| Full test remains coherent after path unify | ✅ | Primary on `.workspace-tab-shell__actions` REDBC red / GREENBC teal |
| No horizontal page overflow @1920 | ✅ | Spec `assertNoViewportOverflow` on expand + post-open surfaces |
| a11y smoke (critical axe) | ✅ | 9/9 + inline critical axe on changed surfaces = 0 |
| English-first copy | ✅ | Sample results / Open data set / Full test / Test run history / Preview runs |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Parent history **Readiness** badge truncates (`Readiness checks pas…`) at `width="140"` while 1920 has spare horizontal space | `BatchTestHistoryPanel.vue` readiness column — OA no-clip polish; not in Sample results expand |
| 🟡 Suggestion | Parent history **Results** cell can look sparse vs nested Sample results (counts template `{succeeded} / {total} passed`); confirm numeric fields always bind when `sampleResults` present | `BatchTestHistoryPanel.vue` counts column — secondary to expand drill-down |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. **Sample results** nested table uses bank OA density (compact th/td, scroll `max-height: 16rem`) — clean at 1920 with ≥2 rows; Open data set link primary brand-colored.
2. Data set ids (`TDS-…`) are short external codes — not raw UUID primary labels; Open data set navigates without leaking forbidden entity detail.
3. **Submit for testing** remains secondary/disabled on Draft rail beside **Full test** — hierarchy intact after async-only path unify (BTH-006).
4. Helpers: `CE_U18_VIEWPORT` 1920×1080 + `captureCeU18Screenshot` / `captureCeU18LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
5. Spec: `frontend/e2e/CE-U18-batch-test-history-uiux-evidence.spec.ts`.
6. No merge / no new deploy / no product Done claim (stage 7 handoff only).

## Next

**Stage 8 — `architecture-reviewer`**
