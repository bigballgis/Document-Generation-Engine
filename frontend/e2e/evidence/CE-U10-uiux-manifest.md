# CE-U10 UIUX Evidence Manifest — sharedGroupCodes UI

**Task:** CE-U10 / Task Master **#85** — Share to groups create + settings + summary  
**Slice:** `ce-u10-shared-group-codes-ui` (`feat/ce-u10-shared-group-codes-ui`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (**UP**)  
**Verdict:** **PASS** (no 🔴 Critical UIUX blockers; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-U10-shared-group-codes-ui.spec.ts` | **6/6 passed** |
| Stage 7 evidence: `CE-U10-shared-group-codes-ui-uiux-evidence.spec.ts` | **2/2 passed** (23.2s) |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U10-shared-group-codes-ui-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 2 passed (23.2s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-create-share-to-groups-redbc-1920x1080.png` | REDBC | Create dialog with Share to groups |
| 1b | `01b-create-dialog-crop-redbc-1920x1080.png` | REDBC | Create dialog crop |
| 2 | `02-create-share-to-groups-greenbc-1920x1080.png` | GREENBC | Create dialog dual-brand |
| 2b | `02b-brand-header-greenbc-crop.png` | GREENBC | Header logo crop |
| 3 | `03-create-author-no-share-redbc-1920x1080.png` | REDBC | Author create — no Share control |
| 4 | `04-create-author-no-share-greenbc-1920x1080.png` | GREENBC | Author create dual-brand |
| 5 | `05-detail-summary-shared-redbc-1920x1080.png` | REDBC | Detail summary Owner + Shared with |
| 5b | `05b-page-header-summary-crop-redbc.png` | REDBC | Page header crop |
| 6 | `06-detail-summary-shared-greenbc-1920x1080.png` | GREENBC | Detail summary dual-brand |
| 7 | `07-settings-dialog-redbc-1920x1080.png` | REDBC | Module settings Share to groups |
| 7b | `07b-settings-dialog-crop-redbc.png` | REDBC | Settings dialog crop |
| 8 | `08-settings-dialog-greenbc-1920x1080.png` | GREENBC | Settings dual-brand |
| 9 | `09-settings-confirm-redbc-1920x1080.png` | REDBC | Confirm shared group changes MessageBox |
| 9b | `09b-brand-header-greenbc-crop.png` | GREENBC | Header crop |
| 10 | `10-brand-header-redbc-crop.png` | REDBC | Header crop |

Path prefix: `frontend/e2e/evidence/CE-U10/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02, 03–04, 05–06, 07–08 + header crops |
| Logo switch (`BrandLogo` / header brand text) | ✅ | 02b, 09b, 10 |
| Create Share to groups multiselect readable | ✅ | 01, 01b, 02 |
| Author fail-closed (no Share control) | ✅ | 03, 04 |
| Detail summary Owner / Shared with English-first | ✅ | 05, 05b, 06 |
| Settings dialog + Save affordance | ✅ | 07, 07b, 08 |
| Confirm MessageBox before shared change | ✅ | 09 |
| No text overflow / overlap @1920 | ✅ | Full-page + crops |
| Modal closed before brand switch | ✅ | Spec closes dialogs so header switcher is reachable |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None | — |

## Notes

1. Helpers: `CE_U10_VIEWPORT` 1920×1080 + `captureCeU10Screenshot` / `captureCeU10LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-U10-shared-group-codes-ui-uiux-evidence.spec.ts`.
3. Functional stage-6 evidence: `frontend/e2e/evidence/CE-U10-manifest.md`.
4. No merge performed (stage 7 handoff; parent runs integration-merger).
