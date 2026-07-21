# Asset library group isolation — Stage 7 UIUX Evidence Manifest

**Task:** Task Master **#154** — Asset library group isolation  
**Slice:** `asset-library-group-isolation` (`feat/asset-library-group-isolation`)  
**Worktree:** `D:/working/DGE-asset-library-group-isolation`  
**Reviewer:** e2e-uiux-reviewer (Stage 7 — retry after Ask-mode write block)  
**Date:** 2026-07-22  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand list + upload group-required evidence captured)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | Asset library list + `ScopedGroupSelect` group filter | `/library/assets` | REDBC + GREENBC |
| 2 | Upload dialog — owning group required (Upload disabled) | Upload library asset dialog | REDBC + GREENBC |
| 3 | Brand header / logo switch | Shell header | REDBC + GREENBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `asset-library-group-isolation.spec.ts` | **2/2 passed** |
| Stage 6 functional (handoff): `ce-e02-asset-library` | **4/4 passed** |
| Stage 7 evidence: `asset-library-group-isolation-uiux-evidence.spec.ts` | **1/1 passed** (~13.0s) |
| `a11y-smoke.spec.ts` | **9/9 passed** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/asset-library-group-isolation-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 10/10 passed (~40.0s)
```

## Screenshot inventory

Path prefix: `frontend/e2e/evidence/asset-library-group-isolation/screenshots/` (**11** files)

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-asset-library-list-group-filter-redbc-1440x900.png` | REDBC | List + Group filter (All groups) + searched fixture row (RETAIL) |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Shell brand header — Red Bank |
| 1c | `01c-group-filter-redbc-crop.png` | REDBC | `ScopedGroupSelect` Group filter crop |
| 1d | `01d-catalog-filters-redbc-crop.png` | REDBC | CatalogFilterToolbar (search / Class / Status / sort) |
| 2 | `02-upload-dialog-group-required-redbc-1440x900.png` | REDBC | Upload dialog — Group required; Upload disabled |
| 2b | `02b-upload-group-field-redbc-crop.png` | REDBC | Upload Group field crop |
| 3 | `03-asset-library-list-group-filter-greenbc-1440x900.png` | GREENBC | List dual-brand |
| 3b | `03b-brand-header-greenbc-crop.png` | GREENBC | Shell brand header — Green Bank |
| 3c | `03c-group-filter-greenbc-crop.png` | GREENBC | Group filter crop |
| 4 | `04-upload-dialog-group-required-greenbc-1440x900.png` | GREENBC | Upload dialog dual-brand — Group required |
| 4b | `04b-upload-group-field-greenbc-crop.png` | GREENBC | Upload Group field crop |

Spec: `frontend/e2e/asset-library-group-isolation-uiux-evidence.spec.ts`  
Helpers: `ASSET_LIBRARY_GROUP_ISOLATION_*` + `captureAssetLibraryGroupIsolation*` in `frontend/e2e/helpers/uiux-evidence.ts`  
Manifest (this file): `frontend/e2e/evidence/asset-library-group-isolation-uiux-manifest.md`  
BDD SoT: `docs/behavior/asset-library-group-isolation.md` (BDD-ALGI-015/016 visual)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01, 03 |
| Group filter via `ScopedGroupSelect` (not free-text) | ✅ | 01c / 03c — Group / All groups |
| Upload owning group required; primary Upload disabled until selected | ✅ | 02 / 04 — asterisk + disabled Upload |
| Dual-brand REDBC / GREENBC | ✅ | 01 vs 03; 02 vs 04; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01b Red Bank; 03b Green Bank; GREENBC logo `src` asserted |
| Fluid catalog layout (`AppPageLayout` fluid) | ✅ | Spec `assertFluidLayout` |
| Entity display (no raw UUID primary) | ✅ | Spec UUID assert + human-readable asset key / RETAIL group link |
| No horizontal overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| English-first i18n | ✅ | Asset library / Upload library asset / Group / Upload asset |
| a11y smoke | ✅ | 9/9 passed |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | **None critical.** Group filter + upload group-required dual-brand evidence green. | — |
| 🟢 Note | Dense table **Uploaded** column may ellipsize calendar date at 1440 (e.g. `7/22/20…`) — readable enough; no overlap/clip of actions. | Table density polish; not a group-isolation regression |
| 🟢 Note | Shell locale switcher truncates to **“Engli…”** at 1440 — platform-wide, previously noted on CE-E02. | Shell header polish |

## Stage 7 gate

**PASS** — Critical = **0**. Durable dual-brand screenshots + this manifest unblock Stage 8 architecture review / Stage 10 deploy evidence. No merge performed.
