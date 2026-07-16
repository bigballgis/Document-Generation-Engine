# CE-G04 UIUX Evidence Manifest — Legal Hold admin

**Task:** CE-G04 / Task Master **#75** — Legal Hold management UI  
**Slice:** `ce-g04-legal-hold` (`feat/ce-g04-legal-hold`)  
**Worktree:** `D:/working/DGE-ce-g04-legal-hold`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-16  
**Viewport:** 1440×900 (desktop-first gate); spot-check 1280×800 + zh-CN  
**Stack:** Vite FE `http://127.0.0.1:5173` + API `http://127.0.0.1:8080` — **UP** (Stage 5/6 acceptance stack; Docker FE :4173 not listening)  
**Verdict:** **PASS** (no CE-G04-specific Critical blockers; dual-brand list/create/release verified; platform EP message-box layout noted as Suggestion)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `CE-G04-legal-hold.spec.ts` | **4/4 passed** |
| Stage 7 evidence: `CE-G04-legal-hold-uiux-evidence.spec.ts` | **1/1 passed** (~11.9s) |
| Embedded axe (wcag2a/aa + 2.1) on Legal Holds list @REDBC | **0 critical** (asserted in evidence spec) |
| `a11y-smoke.spec.ts` | **8/9 passed** — 1 failure out of scope (content-modules duplicate button strict mode) |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-G04-legal-hold-uiux-evidence.spec.ts `
  --workers=1
# 1 passed (~11.9s) against FRONTEND_PORT=5173
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-legal-hold-list-loaded-redbc-1440x900.png` | REDBC | Loaded list + Active/Released rows + Create |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Header — Red Bank logo |
| 1c | `01c-status-filter-redbc-crop.png` | REDBC | Status enum filter |
| 1d | `01d-active-row-redbc-crop.png` | REDBC | ACTIVE row: HOLD-… / status / Release |
| 2 | `02-create-dialog-template-window-redbc-1440x900.png` | REDBC | Create dialog TEMPLATE_WINDOW fields |
| 3 | `03-create-dialog-invocation-set-redbc-1440x900.png` | REDBC | Create dialog INVOCATION_SET fields |
| 4 | `04-release-confirm-redbc-1440x900.png` | REDBC | Release MessageBox (locator crop) |
| 4b | `04b-release-confirm-fullpage-redbc-1440x900.png` | REDBC | Release confirm full page (layout evidence) |
| 5 | `05-legal-hold-empty-released-filter-redbc-1440x900.png` | REDBC | Status=Released filter (prior RELEASED rows; not EmptyStatePanel) |
| 5c | `05c-legal-hold-list-zh-CN-redbc-1440x900.png` | REDBC | zh-CN locale — layout intact |
| 5d | `05d-legal-hold-list-redbc-1280x800.png` | REDBC | Narrow desktop spot-check |
| 6 | `06-legal-hold-list-loaded-greenbc-1440x900.png` | GREENBC | Loaded list dual-brand |
| 6b | `06b-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank logo |
| 7 | `07-create-dialog-template-window-greenbc-1440x900.png` | GREENBC | Create dialog primary = teal |
| 8 | `08-release-confirm-greenbc-1440x900.png` | GREENBC | Release confirm dual-brand |

Path prefix: `frontend/e2e/evidence/CE-G04-legal-hold/screenshots/`  
Manifest: `frontend/e2e/evidence/CE-G04-legal-hold-uiux-manifest.md`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1440 | ✅ | 01–08; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01b Red Bank; 06b Green Bank; greenbc logo src asserted |
| Fluid catalog layout (`AppPageLayout` fluid) | ✅ | 01, 06 — full content width |
| Shared table chrome | ✅ | `AppDataTable` + `AppTablePagination`; enum Status filter |
| Entity display (no raw UUID primary) | ✅ | Spec UUID assert; Hold ID `HOLD-…`; template via `EntityLinkCell` |
| Status badge readable | ✅ | Active success tag (01d); Released info tag on list |
| Create dialog form hierarchy | ✅ | 02/03/07 — labels, Cancel / Create hold, scope switch |
| Release confirm usable | ✅ | Buttons Cancel / Release hold; holdExternalId in copy |
| Role-gated Create/Release (GLOBAL_ADMIN) | ✅ | Header Create + row Release visible |
| No horizontal overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| Locale switch layout | ✅ | 05c zh-CN |
| a11y (critical axe on Legal Holds) | ✅ | Evidence spec axe assert |
| EmptyStatePanel visual | ⚠️ | Not captured — Released filter still had rows from Stage 6; code path exists |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | **Release confirm (`ElMessageBox`) is not a centered card** — title/body/actions sit top-left and overlap shell chrome (04 / 04b / 08). Buttons remain usable. Same platform pattern previously noted on CE-E02 / CDP-E2E-T11/T13. **Not introduced by CE-G04** (`useConfirmAction` / EP). Recommend platform CSS fix for overlay flex + `--el-messagebox-width`. | OA “no overlap”; `LegalHoldListView` → confirmRelease; frames 04 / 04b / 08 |
| 🟡 Suggestion | **@1280×800 document `scrollWidth` exceeds viewport** (~1322 vs 1280) — wide table columns (Hold ID + scope + summary + status + reason + createdBy + createdAt + actions). Desktop gate is 1440 (green). Prefer keeping table internal horizontal scroll without expanding `documentElement.scrollWidth`, or drop less-critical columns at narrow widths. | Desktop-first density; `LegalHoldListView.vue` table; frame 05d |
| 🟡 Suggestion | **INVOCATION_SET field shows error-colored border immediately after scope switch** (03) before submit — likely Element Plus re-validate on rules change. Prefer clearValidate on scopeType change or validate only on submit/blur. | Form polish; `LegalHoldCreateDialog.vue` |
| 🟡 Suggestion | **EmptyStatePanel not evidenced** in this run — Status=Released still returned prior RELEASED fixtures. Code has `EmptyStatePanel` + `legal-hold-create-open-empty`. Optional follow-up: route-mock empty page for a dedicated empty screenshot. | State completeness; frame 05 |
| 🟢 Nice to have | Status filter is a lone `el-select` rather than `CatalogFilterToolbar` — acceptable for a single enum filter; optional align with Asset Library catalog chrome. | Shared component vocabulary |
| 🟢 Nice to have | Header locale switcher truncates to **“Engli…”** / **“简体…”** at 1440 — shell-wide, not Legal Hold-specific. | Shell header polish |

## Notes

1. Helpers: `CE_G04_VIEWPORT` 1440×900 + `CE_G04_NARROW_VIEWPORT` 1280×800 + `captureCeG04Screenshot` / `captureCeG04LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-G04-legal-hold-uiux-evidence.spec.ts`.
3. Surfaces: `LegalHoldListView.vue`, `LegalHoldCreateDialog.vue`, `useLegalHoldListView.ts`.
4. No product Vue code changed for Stage 7 (evidence + helpers + manifest only).
5. No merge / no Done claim for #75 / no stage 11–13 performed.
6. Playwright process may print a Windows UV_HANDLE_CLOSING noise after pass; evidence run itself is green.

## Handoff → Stage 8 architecture-reviewer

- UIUX verdict **PASS** — proceed with architecture review of Legal Hold module boundaries (API / exemption / retention hooks / capabilities / OpenAPI).
- Carry forward platform MessageBox centering as known non-blocking debt (not CE-G04-specific).
- Optional FE polish (scope-switch validate, empty evidence route-mock) can queue after #75 Done if needed — not blockers for merge of CE-G04 behavior.
