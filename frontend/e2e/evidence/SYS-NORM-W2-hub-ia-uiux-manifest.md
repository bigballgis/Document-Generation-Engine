# SYS-NORM-W2 UIUX Evidence Manifest — Template (+ Master) Package Hub IA

**Task:** SYS-NORM Wave 2 / Task Master **#146** — Package Hub IA (Version-lines-primary, Properties drawer, API model A redirect, Master parity)  
**Slice:** `sys-norm-hub-ia` (`feat/sys-norm-hub-ia`)  
**Worktree:** `D:/working/DGE-sys-norm-hub-ia`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-21  
**Viewport:** 1440×900 (standard)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand evidence complete)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | Template Package Hub — Version lines primary, no secondary tabs, fluid | `/templates/:id` (DEMO-RETAIL-LETTER) | REDBC + GREENBC |
| 2 | Template Properties right drawer + overview summary | Hub → Properties open | REDBC + GREENBC |
| 3 | API package settings shell (from hub API settings) | `/api/packages/:id/settings` + interim banner | REDBC + GREENBC |
| 4 | Master Package Hub — Revision lines + Properties drawer parity | `/masters/:id` (Demo Retail Letterhead) | REDBC + GREENBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `SYS-NORM-W2-hub-ia.spec.ts` | **PASS** (handoff) |
| Stage 7 evidence: `SYS-NORM-W2-hub-ia-uiux-evidence.spec.ts` | **4/4 passed** (~54s) |
| Temp screenshot copy | **N/A** — `sys-norm-w2-*.png` absent under `%LOCALAPPDATA%\Temp\cursor\screenshots`; re-captured via Playwright |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/SYS-NORM-W2-hub-ia-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 4 passed (54.3s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-template-hub-version-lines-redbc-1440x900.png` | REDBC | Template hub Version lines |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 1c | `01c-version-lines-card-redbc-crop.png` | REDBC | Version lines card crop |
| 2 | `01-template-hub-version-lines-greenbc-1440x900.png` | GREENBC | Template hub dual-brand |
| 2b | `01b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 2c | `01c-version-lines-card-greenbc-crop.png` | GREENBC | Version lines card crop |
| 3 | `02-template-hub-properties-drawer-redbc-1440x900.png` | REDBC | Properties drawer open over hub |
| 3b | `02b-properties-drawer-redbc-crop.png` | REDBC | Drawer crop |
| 4 | `02-template-hub-properties-drawer-greenbc-1440x900.png` | GREENBC | Properties drawer dual-brand |
| 4b | `02b-properties-drawer-greenbc-crop.png` | GREENBC | Drawer crop |
| 5 | `03-api-package-settings-redbc-1440x900.png` | REDBC | API package settings shell |
| 5b | `03b-api-settings-interim-banner-redbc-crop.png` | REDBC | Interim banner crop |
| 6 | `03-api-package-settings-greenbc-1440x900.png` | GREENBC | API settings dual-brand |
| 6b | `03b-api-settings-interim-banner-greenbc-crop.png` | GREENBC | Interim banner crop |
| 7 | `04-master-hub-properties-redbc-1440x900.png` | REDBC | Master hub + Properties drawer |
| 7b | `04b-master-properties-drawer-redbc-crop.png` | REDBC | Master drawer crop |
| 8 | `04-master-hub-properties-greenbc-1440x900.png` | GREENBC | Master hub dual-brand |
| 8b | `04b-master-properties-drawer-greenbc-crop.png` | GREENBC | Master drawer crop |

Path prefix: `frontend/e2e/evidence/SYS-NORM-W2/screenshots/` (**18** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Version-lines-primary hub (no Overview/Dependencies/External access tabs) | ✅ | 01 REDBC/GREENBC + Stage 6 W2-001/002 |
| Fluid hub layout (`--fluid`, no `__inner`) | ✅ | Spec class assert on `template-package-hub` |
| Properties right drawer overlays hub (Version lines remain) | ✅ | 02 full + 02b crop |
| API settings navigates to package settings shell (model A) | ✅ | 03 + interim banner 03b |
| Master hub Properties + revision lines parity | ✅ | 04 + 04b |
| Dual-brand REDBC + GREENBC | ✅ | 01–04 pairs; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01b Red Bank / Green Bank crops |
| No horizontal page overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| English-first copy | ✅ | Version lines / Properties / API settings / Revision lines |
| No raw UUID as primary entity label | ✅ | Group RETAIL / master name readable in drawer |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Version lines **Last updated by** truncates at 1440 (`Template Aut…`). Prefer full identity at standard viewport or tooltip on ellipsis for scannability. | OA density / hub Version lines table — not a Wave 2 IA blocker |
| 🟡 Suggestion | Template Properties drawer footer (**Save approval matrix mode**) can sit near the fold / feel clipped when overview content is long; ensure sticky footer or internal scroll so the primary save action stays discoverable. | OA drawer / `template-properties-drawer` |
| 🟡 Suggestion | API package settings on DEMO draft shows empty/error panels (“Unable to load route summary”, “API policy was not found”) beside the intentional interim banner — honest for unpublished packages, but visual weight of EP empty/error icons competes with the Wave-3-later messaging; consider quieter empty states for unpublished packages. | OA hierarchy / `api-package-settings-*` — Wave 3 panel fill expected |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Prior Ask-mode review screenshots lived only under temp (`sys-norm-w2-*.png`) and were **gone** at persist time; this run re-captured durable evidence via `SYS-NORM-W2-hub-ia-uiux-evidence.spec.ts`.
2. Helpers: `SYS_NORM_W2_VIEWPORT` + `captureSysNormW2Screenshot` / `captureSysNormW2LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
3. Spec: `frontend/e2e/SYS-NORM-W2-hub-ia-uiux-evidence.spec.ts`.
4. API settings empty/error panels are consistent with package-level API model A shell before Wave 3 ops dashboard — not treated as Critical for Hub IA slice.

## Stage 7 gate

**PASS** — ready for Stage 8 architecture-reviewer (no UIUX blockers for merge of #146).
