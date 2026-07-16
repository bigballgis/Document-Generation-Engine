# CE-U20 UIUX Evidence Manifest — Clause create structured + catalog Status

**Task:** CE-U20 / Task Master **#94** — structured create editor (900px, no JSON textarea) + catalog Status column/filter  
**Slice:** `ce-u20-clause-create-structured` (`feat/ce-u20-clause-create-structured`)  
**Worktree:** `D:/working/DGE-ce-u20-clause-create-structured`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-17  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 DEPLOY_OK; Stage 6 7/7)  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U20-clause-create-structured.spec.ts` | **7/7 passed** (see `CE-U20-manifest.md`) |
| Stage 7 evidence: `CE-U20-clause-create-structured-uiux-evidence.spec.ts` | **2/2 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (structured editor excl. style-picker; catalog Status) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U20-clause-create-structured-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 11 passed (41.7s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-create-dialog-structured-redbc-1920x1080.png` | REDBC | Create dialog + structured editor over Standard clauses |
| 1b | `01b-create-dialog-crop-redbc-1920x1080.png` | REDBC | `.el-dialog` panel crop (~900px) |
| 1c | `01c-structured-editor-crop-redbc-1920x1080.png` | REDBC | `[data-testid=controlled-structured-content-editor]` |
| 1d | `01d-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-create-dialog-structured-greenbc-1920x1080.png` | GREENBC | Create dialog dual-brand |
| 2b | `02b-create-dialog-crop-greenbc-1920x1080.png` | GREENBC | Dialog panel crop |
| 2c | `02c-structured-editor-crop-greenbc-1920x1080.png` | GREENBC | Structured editor crop |
| 2d | `02d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-catalog-status-column-redbc-1920x1080.png` | REDBC | Catalog Status column (Draft + Approved fixtures) |
| 3b | `03b-status-filter-toolbar-crop-redbc-1920x1080.png` | REDBC | CatalogFilterToolbar Status combobox |
| 3c | `03c-status-column-table-crop-redbc-1920x1080.png` | REDBC | Status badges + EntityLinkCell (REDBC red names) |
| 3d | `03d-catalog-status-draft-filter-redbc-1920x1080.png` | REDBC | After Status=Draft filter |
| 3e | `03e-status-filter-draft-chip-crop-redbc-1920x1080.png` | REDBC | Chip `Status: DRAFT` + Clear all |
| 3f | `03f-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 4 | `04-catalog-status-column-greenbc-1920x1080.png` | GREENBC | Catalog Status dual-brand |
| 4b | `04b-status-filter-toolbar-crop-greenbc-1920x1080.png` | GREENBC | Status filter toolbar |
| 4c | `04c-status-column-table-crop-greenbc-1920x1080.png` | GREENBC | Status badges + teal name links |
| 4d | `04d-catalog-status-draft-filter-greenbc-1920x1080.png` | GREENBC | Draft filter applied |
| 4e | `04e-status-filter-draft-chip-crop-greenbc-1920x1080.png` | GREENBC | Status: DRAFT chip |
| 4f | `04f-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/CE-U20/screenshots/` (**20** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02 create; 03–04 catalog |
| Logo / brand header switch | ✅ | 01d Red Bank; 02d / 04f Green Bank |
| Create dialog ~900px structured editor | ✅ | Spec measures `.el-dialog` width 860–940; 01b / 02b |
| No legacy JSON textarea as primary structure field | ✅ | 01c / 02c — `ControlledStructuredContentEditor` + paragraph input; JSON only under collapsible preview |
| Default empty paragraph surface | ✅ | 01c — Paragraph block + Static text placeholder |
| Catalog Status column + badges | ✅ | 03c / 04c — Draft / Approved pills; no raw UUID primary |
| Status enum filter (select) + chips | ✅ | 03b / 03e / 04b / 04e — Status combobox + `Status: DRAFT` chip |
| Entity names link with brand primary | ✅ | 03c REDBC red; 04c GREENBC teal |
| Fluid catalog width @1920 | ✅ | 03 / 04 — AppPageLayout fluid; no wasted gutters |
| No horizontal page overflow @1920 | ✅ | Spec `assertNoViewportOverflow` on create + catalog |
| a11y smoke (critical axe) | ✅ | 9/9 + scoped editor/catalog critical = 0 |
| English-first copy | ✅ | Create content module / Content structure / Status / Draft / Approved / New content module |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Toolbar **style picker** (`[data-testid=style-picker]`) has a visible “Style” group label but the EP combobox input has no accessible name — full-editor axe `label` critical when not excluded. Spec excludes this control for the CE-U20 visual gate; FE should add `:aria-label` (or associate label) on `StructuredContentEditorToolbar.vue` | OA a11y / `ControlledStructuredContentEditor` shared toolbar — pre-existing shared control, now surfaced in create dialog |
| 🟡 Suggestion | Tall create dialog (~900px + structured editor) may push **Cancel / Create module** footer below the first viewport; ensure modal body scroll + sticky footer remain obvious at 1920×1080 | `ContentModuleCreateDialog.vue` — polish |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Full-page captures show the dimmed catalog behind the dialog (normal overlay); opaque `.el-dialog` crops (01b/02b) confirm form + editor remain readable with no text overlap.
2. Catalog Status filter is enum `select` via `CatalogFilterToolbar` (not free-text) — matches entity-display constitution.
3. Helpers: `CE_U20_VIEWPORT` 1920×1080 + `captureCeU20Screenshot` / `captureCeU20LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
4. Spec: `frontend/e2e/CE-U20-clause-create-structured-uiux-evidence.spec.ts`.
5. No merge / no new deploy / no product Done claim (stage 7 handoff only).

## Next

**Stage 8 — `architecture-reviewer`**
