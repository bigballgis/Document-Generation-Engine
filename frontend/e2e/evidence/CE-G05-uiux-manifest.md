# CE-G05 UIUX Evidence Manifest — annual review + clause FTS / where-used

**Task:** CE-G05 — Template annual review due/complete + content-module FULL_TEXT search + Where used  
**Slice:** `ce-g05-annual-review-fts` (`feat/ce-g05-annual-review-fts`)  
**Worktree:** `D:/working/DGE-ce-g05-annual-review-fts`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1440×900 (desktop-first skill standard)  
**Stack:** Docker FE `http://127.0.0.1:4173` + API `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `CE-G05-annual-review-fts.spec.ts` | **17/17 passed** |
| Stage 7 evidence: `CE-G05-annual-review-fts-uiux-evidence.spec.ts` | **1/1 passed** (~20.1s) |
| Embedded axe (wcag2a/aa + 2.1) on annual partition / overview / FTS / where-used @REDBC | **0 critical** (asserted in evidence spec) |
| `a11y-smoke.spec.ts` @ Docker | **9/9 passed** |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-G05-annual-review-fts-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# evidence 1/1 (~20.1s); a11y 9/9 (~24.5s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-annual-review-due-partition-redbc-1440x900.png` | REDBC | Dashboard My tasks — Annual review due partition |
| 1b | `01b-annual-review-partition-crop-redbc-1440x900.png` | REDBC | Annual review partition crop |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 1d | `01d-annual-review-row-crop-redbc.png` | REDBC | Due row + Open |
| 2 | `02-template-overview-annual-review-redbc-1440x900.png` | REDBC | Template overview — nextReviewDue + Complete CTA |
| 2b | `02b-overview-summary-crop-redbc.png` | REDBC | Summary grid crop |
| 2c | `02c-complete-review-cta-crop-redbc.png` | REDBC | Complete annual review primary (red) |
| 3 | `03-content-modules-full-text-redbc-1440x900.png` | REDBC | Standard clauses — Full text (body) hit |
| 3b | `03b-search-mode-full-text-crop-redbc.png` | REDBC | Search mode select crop |
| 4 | `04-where-used-tab-redbc-1440x900.png` | REDBC | Where used tab + EntityLinkCell row |
| 4b | `04b-where-used-panel-crop-redbc.png` | REDBC | Where-used panel crop |
| 5 | `05-annual-review-due-partition-greenbc-1440x900.png` | GREENBC | Dashboard Annual review due |
| 5b | `05b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 5c | `05c-annual-review-partition-crop-greenbc-1440x900.png` | GREENBC | Partition crop |
| 6 | `06-content-modules-full-text-greenbc-1440x900.png` | GREENBC | FULL_TEXT catalog |
| 7 | `07-where-used-tab-greenbc-1440x900.png` | GREENBC | Where used tab |
| 7b | `07b-where-used-panel-crop-greenbc.png` | GREENBC | Where-used panel crop |
| 8 | `08-template-overview-annual-review-greenbc-1440x900.png` | GREENBC | Overview + Complete CTA |
| 8b | `08b-complete-review-cta-crop-greenbc.png` | GREENBC | Complete annual review primary (teal) |

Path prefix: `frontend/e2e/evidence/CE-G05/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Annual review due partition (author-workflow, not collab queue) | ✅ | 01 / 01b / 05 / 05c |
| Overview nextReviewDue + Complete annual review CTA | ✅ | 02 / 02b / 02c / 08 / 08b |
| REDBC primary CTA / header | ✅ | 01c, 02c (`#DB0011`) |
| GREENBC logo + primary CTA switch | ✅ | 05b, 08b (`#00847F`) |
| FULL_TEXT search mode + catalog fluid layout | ✅ | 03 / 03b / 06 — `AppPageLayout fluid` |
| Entity names as links (no raw UUID primary) | ✅ | 03, 04 / 04b, 07 — EntityLinkCell |
| Where-used table (name + externalId subtitle) | ✅ | 04 / 04b / 07 / 07b |
| No horizontal overflow @1440 | ✅ | Spec assert + full viewport shots |
| a11y smoke (critical axe) | ✅ | 9/9 |
| English-first copy | ✅ | Annual review / Full text (body) / Where used |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 | Content-module detail tab ink for “Where used” reads as default EP blue underline rather than brand-primary accent | Detail tabs on content-module workspace — token polish; not a blocker |
| 🟡 | Dashboard annual-review Item / What-to-do cells truncate at OA density (ellipsis) | `template-annual-review` partition — expected dense table; Open remains reachable |
| — | No 🔴 Critical | — |

## Notes

1. Helpers: `CE_G05_VIEWPORT` 1440×900 + `captureCeG05Screenshot` / `captureCeG05LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-G05-annual-review-fts-uiux-evidence.spec.ts`.
3. Surfaces: dashboard partition `data-partition-id=template-annual-review`, `TemplateDetailOverviewTab.vue`, `ContentModuleListView.vue` (search mode), `ContentModuleWhereUsedPanel.vue`.
4. No merge / no new deploy performed (stage 7 handoff only).
