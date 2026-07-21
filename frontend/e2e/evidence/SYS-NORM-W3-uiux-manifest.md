# SYS-NORM-W3-UIUX Evidence Manifest — External services ops

**Task:** SYS-NORM Wave 3 / Task Master **#147** — External services dashboard + invocations + package settings  
**Slice:** `sys-norm-external-ops` (`feat/sys-norm-external-ops`)  
**Worktree:** `D:/working/DGE-sys-norm-external-ops`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-21  
**Viewport:** 1440×900 (standard)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (nginx SPA fix redeployed)  
**Verdict:** **PASS** (Critical = 0; dual-brand evidence complete; suggestions non-blocking)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | External services overview — readiness + ops sample cards (not catalog) | `/api/policies` | REDBC + GREENBC |
| 2 | Invocation records — filters + table (EntityLink packages) | `/api/invocations` | REDBC + GREENBC |
| 3 | Invocation summary drawer (summary-only, no variables) | Invocations → Open summary | REDBC |
| 4 | Package API settings complete (no interim banner) | `/api/packages/:id/settings` | REDBC + GREENBC |
| 5 | Nav membership — overview + invocation records (+ icons); no settings catalog | Shell sidebar | REDBC + GREENBC |
| 6 | Forbidden fail-closed (template author → `/api/policies`) | Unified no-access + Reference | REDBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `SYS-NORM-W3-external-ops.spec.ts` | **10/10 passed** (handoff) |
| Stage 7 evidence: `SYS-NORM-W3-uiux-evidence.spec.ts` | **6/6 passed** (~68s wall in combined run) |
| `a11y-smoke.spec.ts` (combined run) | **8/9** — #6 submit-gate fixture `POST /templates` **500** (env flake, not W3 surface) |
| `a11y-smoke.spec.ts` (retry alone) | **9/9 passed** (28.0s) |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/SYS-NORM-W3-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# evidence 6/6 PASS; a11y #6 fixture 500 once

pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 9 passed (28.0s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-dashboard-ops-redbc-1440x900.png` | REDBC | Overview readiness + ops cards |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 1c | `01c-readiness-summary-redbc-crop.png` | REDBC | Readiness summary crop |
| 1d | `01d-ops-summary-redbc-crop.png` | REDBC | Ops sample crop |
| 2 | `01-dashboard-ops-greenbc-1440x900.png` | GREENBC | Overview dual-brand |
| 2b | `01b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 2c | `01c-readiness-summary-greenbc-crop.png` | GREENBC | Readiness dual-brand |
| 2d | `01d-ops-summary-greenbc-crop.png` | GREENBC | Ops dual-brand |
| 3 | `02-invocations-page-redbc-1440x900.png` | REDBC | Invocations page |
| 3b | `02b-invocations-filters-redbc-crop.png` | REDBC | Filters crop |
| 3c | `02c-invocations-table-redbc-crop.png` | REDBC | Table + EntityLink + tooltip |
| 4 | `02-invocations-page-greenbc-1440x900.png` | GREENBC | Invocations dual-brand |
| 4b | `02b-invocations-filters-greenbc-crop.png` | GREENBC | Filters dual-brand |
| 4c | `02c-invocations-table-greenbc-crop.png` | GREENBC | Table dual-brand |
| 5 | `03-invocation-drawer-redbc-1440x900.png` | REDBC | Drawer over table |
| 5b | `03b-invocation-drawer-redbc-crop.png` | REDBC | Summary-only drawer crop |
| 6 | `04-package-settings-redbc-1440x900.png` | REDBC | Completed API settings |
| 6b | `04b-package-settings-panel-redbc-crop.png` | REDBC | Settings panel crop |
| 6c | `04c-brand-header-redbc-crop.png` | REDBC | Header crop |
| 7 | `04-package-settings-greenbc-1440x900.png` | GREENBC | Settings dual-brand |
| 7b | `04b-package-settings-panel-greenbc-crop.png` | GREENBC | Panel crop |
| 7c | `04c-brand-header-greenbc-crop.png` | GREENBC | Header crop |
| 8 | `05-nav-external-services-redbc-1440x900.png` | REDBC | Shell + External services nav |
| 8b | `05b-nav-external-services-redbc-crop.png` | REDBC | Overview + Invocation records icons |
| 9 | `05-nav-external-services-greenbc-1440x900.png` | GREENBC | Nav dual-brand |
| 9b | `05b-nav-external-services-greenbc-crop.png` | GREENBC | Nav crop |
| 10 | `06-forbidden-policies-redbc-1440x900.png` | REDBC | Access denied + Reference (no leak) |

Path prefix: `frontend/e2e/evidence/SYS-NORM-W3/screenshots/` (**27** files on disk)  
Spec: `frontend/e2e/SYS-NORM-W3-uiux-evidence.spec.ts`  
Helpers: `SYS_NORM_W3_VIEWPORT` + `captureSysNormW3Screenshot` in `frontend/e2e/helpers/uiux-evidence.ts`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Fluid layout (`--fluid`, no `__inner`) | ✅ | Spec assert on overview / invocations / settings |
| Dashboard = ops readiness (not published-packages catalog) | ✅ | 01; no “Published packages” heading; no alerts pagination |
| Invocations separate page + filter control types | ✅ | 02; status `el-select`; package `AppSearchSelect` |
| Package EntityLink readable (name + externalId); no UUID primary | ✅ | 02c — human names + DEMO-/CORP- subtitles |
| Settings complete; no interim / under-construction shell | ✅ | 04; interim banner count 0 |
| Dual-brand REDBC + GREENBC | ✅ | 01–05 pairs; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01b / 04c Red Bank ↔ Green Bank |
| Nav: overview + invocations only (+ icons); settings deep-link | ✅ | 05b; no API settings nav item |
| Forbidden unified no-access + Reference; no ops data leak | ✅ | 06 — Access denied; readiness/ops absent |
| No horizontal page overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | Retry **9/9** |
| English-first copy | ✅ | External services overview / Invocation records / Open summary / API settings |
| Drawer summary-only (no variables / parameters) | ✅ | 03b + spec assert |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Invocations **Actions** column clips i18n `Open summary` to `Open summary …` (CSS ellipsis). Widen Actions min-width or use icon+tooltip so the full label remains scannable at 1440. | OA overflow / `ApiInvocationsView` Actions — not a merge blocker |
| 🟡 Suggestion | Invocations filter row wraps: **Created before** + Apply/Clear sit alone on a second row at 1440 while Status/Package/Request ID/Created after fill row 1. Consider `CatalogFilterToolbar` or denser filter grid so apply actions stay visually grouped. | OA density / filter rhythm — `api-invocations-filters` |
| 🟡 Suggestion | Status `SUCCEEDED` / drawer Outcome `SUCCESS` render as plain text (no shared status badge token). Optional alignment with OA semantic badges used elsewhere. | OA status vocabulary — polish only |
| 🟢 Nice to have | Request ID column truncates with hover tooltip (works); optional monospace + copy affordance for ops users. | Invocations table Request ID |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Wave 3 settings **must not** show the Wave 2 interim banner — confirmed absent on completed settings surface (supersedes P13/W2 interim evidence).
2. Hard-refresh SPA for `/api/invocations` verified in UIUX via `page.goto('/api/invocations')` (nginx fix live).
3. Combined a11y+evidence run once hit backend `INTERNAL_ERROR` on unrelated submit-gate fixture prep; retry of a11y alone was green — treat as infra flake, not W3 UIUX regression.
4. Functional Stage 6 PNGs under `evidence/SYS-NORM-W3/*.png` remain complementary; Stage 7 dual-brand set lives in `screenshots/`.

## Stage 7 gate

**PASS** — ready for Stage 8 architecture-reviewer (no UIUX blockers for merge of #147).
