# CE-E02 UIUX Evidence Manifest — Asset Library admin

**Task:** CE-E02 / Task Master **#79** — Asset library management UI  
**Slice:** `ce-e02-asset-library` (`feat/ce-e02-asset-library`)  
**Worktree:** `D:/working/DGE-ce-e02-asset-library`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-16  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (`dge-ce-e02-asset-library`)  
**Verdict:** **PASS** (no CE-E02-specific 🔴 Critical blockers; dual-brand list/upload verified; platform EP message-box layout noted as 🟡)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `ce-e02-asset-library.spec.ts` | **4/4 passed** |
| Stage 7 evidence: `CE-E02-asset-library-uiux-evidence.spec.ts` | **1/1 passed** (~6.0s) |
| `a11y-smoke.spec.ts` | **6/9 passed** — failures **out of scope** for Asset Library (see Notes) |

```powershell
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-E02-asset-library-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# evidence 1 passed; a11y 6/9 (content-modules strict + FOL master fixture)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-asset-library-list-redbc-1440x900.png` | REDBC | List + search + Active row + Disable |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Header — Red Bank logo |
| 1c | `01c-catalog-filters-redbc-crop.png` | REDBC | CatalogFilterToolbar (search / Class / Status / sort) |
| 2 | `02-upload-dialog-redbc-1440x900.png` | REDBC | Upload library asset dialog |
| 3 | `03-disable-confirm-redbc-1440x900.png` | REDBC | Disable confirm (locator crop) |
| 3b | `03b-disable-confirm-fullpage-redbc-1440x900.png` | REDBC | Disable confirm full page (layout evidence) |
| 4 | `04-asset-library-list-greenbc-1440x900.png` | GREENBC | List dual-brand |
| 4b | `04b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank logo |
| 5 | `05-upload-dialog-greenbc-1440x900.png` | GREENBC | Upload dialog dual-brand |
| 6 | `06-disable-confirm-greenbc-1440x900.png` | GREENBC | Disable confirm dual-brand |

Path prefix: `frontend/e2e/evidence/CE-E02-asset-library/screenshots/`  
Manifest: `frontend/e2e/evidence/CE-E02-asset-library-uiux-manifest.md`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1440 | ✅ | 01–06 |
| Logo / brand header switch | ✅ | 01b Red Bank; 04b Green Bank; `--brand-primary` asserted |
| Fluid catalog layout (`AppPageLayout` fluid) | ✅ | 01, 04 — full content width |
| Shared catalog chrome (toolbar + table + pagination) | ✅ | `CatalogFilterToolbar`, `AppDataTable`, enum selects for Class/Status |
| Entity display (no raw UUID primary) | ✅ | Spec assert + `EntityLinkCell` key + filename subtitle |
| Status badge readable | ✅ | Active green tag (03b / row assert) |
| Upload dialog form hierarchy | ✅ | 02, 05 — labels, hints, Cancel / Upload |
| Role-gated Disable (admin) | ✅ | Disable link visible for Global Admin |
| No horizontal overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe on shell samples) | ⚠️ | Masters + dashboard axe green; 3 unrelated a11y-smoke failures |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | **Disable confirm (`ElMessageBox`) is not a centered card** — computed `width: 1440px`, `position: static`, parent `.el-overlay-message-box` is `display: block` (not flex-centered); title/body/actions sit top-left and overlap shell chrome. Buttons remain usable. Same platform pattern previously noted on CDP-E2E-T11/T13 as non-blocking. **Not introduced by CE-E02** (`useConfirmAction` + EP). Recommend platform CSS fix so `--el-messagebox-width` / overlay flex apply. | OA “no overlap”; `useConfirmAction.ts` / Element Plus message-box; frames 03 / 03b / 06 |
| 🟡 Suggestion | Header locale switcher truncates to **“Engli…”** at 1440 — shell-wide, not Asset Library-specific. | Shell header polish |
| 🟢 Nice to have | Dialog **Upload** disabled state uses Element Plus default muted blue rather than brand-muted primary. | Upload dialog primary hierarchy |

## Notes

1. Helpers: `CE_E02_VIEWPORT` 1440×900 + `captureCeE02Screenshot` / `captureCeE02LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-E02-asset-library-uiux-evidence.spec.ts`.
3. Surfaces: `AssetLibraryListView.vue`, `AssetLibraryUploadDialog.vue`, `useAssetLibraryListView.ts` / `useConfirmAction.ts`.
4. a11y-smoke failures (not Asset Library): (a) content-modules “New content module” strict-mode duplicate button; (b)(c) FOL master “Meridian Wholesale FOL Master” missing on this stack — fixture/seed, not CE-E02.
5. No merge / no new deploy performed (stage 7 handoff). Route fixes for 🟡 message-box to `frontend-engineer` (platform), not required to re-open CE-E02 list/upload behavior.
