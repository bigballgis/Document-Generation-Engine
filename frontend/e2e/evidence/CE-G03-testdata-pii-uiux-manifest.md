# CE-G03 UIUX Evidence Manifest — Test data PII governance

**Task:** CE-G03 / Task Master #74 — testdata PII badges, handling, EXPLICIT confirm, schema `piiCategory`  
**Slice:** `ce-g03-testdata-pii` (`feat/ce-g03-testdata-pii`)  
**Placement:** ISOLATED `D:/working/DGE-ce-g03-testdata-pii`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-16  
**Viewport:** 1440×900 (desktop-first) + 1280×800 zh-CN spot-check  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (**UP**; frontend rebuilt into `documentgenerationengine` for Critical fix)  
**Verdict:** **PASS** (🔴 Critical = 0)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-G03-testdata-pii.spec.ts` | **5/5 passed** (upstream) |
| Stage 7 a11y smoke: `a11y-smoke.spec.ts` | **9/9 passed** |
| Stage 7 evidence: `CE-G03-testdata-pii-uiux-evidence.spec.ts` | **6/6 passed** (~25s; confirm frames re-verified post-fix) |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'; $env:E2E_SKIP_CATALOG_CLEANUP='true'
pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts e2e/CE-G03-testdata-pii-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

Review method: visual inspection of on-disk PNGs; dual-brand token check (REDBC / GREENBC); axe critical=0 on G03 dialogs; zh-CN + narrow spot-check.

### Surface coverage

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| PII badge + handling radios (BDD-012) | Create test data set dialog | 01–01d (REDBC), 03–03c (GREENBC) |
| EXPLICIT confirm empty / validation / filled (BDD-014) | Confirm sensitive test data dialog | 02–02d (REDBC), 04–04b (GREENBC) |
| Schema `piiCategory` select + tree PII marker | Variables tab edit variable | 05–05e (REDBC) |
| A11y (no critical axe) | Create + EXPLICIT dialogs | 06 |
| zh-CN narrow | Create dialog @1280×800 | 07–07b |
| Brand header / logo | REDBC / GREENBC wordmark | 01d, 03c |

## Screenshot inventory

Path prefix: `frontend/e2e/evidence/CE-G03-testdata-pii/screenshots/`

| File | View / state | Brand | Locale |
| --- | --- | --- | --- |
| `01-pii-badge-handling-redbc-en-1440x900.png` | Create dialog + PII handling | REDBC | en |
| `01b-pii-dialog-crop-redbc-en.png` | Dialog crop | REDBC | en |
| `01c-pii-handling-group-crop-redbc-en.png` | Handling radios crop | REDBC | en |
| `01d-brand-header-redbc-en.png` | REDBC header | REDBC | en |
| `02-explicit-confirm-empty-redbc-en-1440x900.png` | EXPLICIT confirm empty | REDBC | en |
| `02b-explicit-confirm-crop-redbc-en.png` | Confirm crop (full checkbox text) | REDBC | en |
| `02c-explicit-confirm-validation-crop-redbc-en.png` | Reason required error | REDBC | en |
| `02d-explicit-confirm-filled-crop-redbc-en.png` | Reason filled + checkbox | REDBC | en |
| `03-pii-badge-handling-greenbc-en-1440x900.png` | Create dialog | GREENBC | en |
| `03b-pii-dialog-crop-greenbc-en.png` | Dialog crop | GREENBC | en |
| `03c-brand-header-greenbc-en.png` | GREENBC header | GREENBC | en |
| `04-explicit-confirm-greenbc-en-1440x900.png` | EXPLICIT confirm | GREENBC | en |
| `04b-explicit-confirm-crop-greenbc-en.png` | Confirm crop (teal primary) | GREENBC | en |
| `05-variables-tree-search-redbc-en-1440x900.png` | Variables search `customerName` | REDBC | en |
| `05b-variables-tree-with-pii-badge-redbc-en-1440x900.png` | Tree PII badge visible | REDBC | en |
| `05c-variable-pii-category-select-redbc-en-1440x900.png` | Edit variable + PII category | REDBC | en |
| `05d-variable-edit-dialog-crop-redbc-en.png` | Edit dialog crop | REDBC | en |
| `05e-pii-category-options-redbc-en-1440x900.png` | Category dropdown options | REDBC | en |
| `06-a11y-explicit-confirm-crop-redbc-en.png` | A11y confirm crop | REDBC | en |
| `07-pii-dialog-zhcn-narrow-redbc-1280x800.png` | zh-CN narrow | REDBC | zh-CN |
| `07b-pii-dialog-crop-zhcn-narrow-redbc.png` | zh-CN dialog crop | REDBC | zh-CN |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Dialog uses EP + tokenized spacing / typography | ✅ | 01b, 03b |
| PII badge on marked fields only (BDD-012) | ✅ | 01b |
| Handling radios; SYNTHETIC recommended default | ✅ | 01c |
| Dual-brand primary on Confirm/Save (REDBC red / GREENBC teal) | ✅ | 02b vs 04b; 01d vs 03c |
| Schema `piiCategory` select (enum AppSearchSelect) | ✅ | 05d–05e |
| English-first + zh-CN lazy parity | ✅ | 07b |
| No horizontal overflow @1440 / 1280 | ✅ | Spec assert + frames |
| A11y smoke + G03 axe critical = 0 | ✅ | a11y-smoke + test 06 |
| No text overflow / clipping on G03 surfaces | ✅ | 02b / 04b full secondaryConfirm wrap |

## Findings

### 🔴 Critical (must fix before merge)

_None._ (Resolved during Stage 7: EXPLICIT confirm checkbox label truncation — dialog `560px` + `.pii-secondary-confirm` wrap in `TemplateTestDataSetEditDialog.vue`; verified on live `:4173`.)

### 🟡 Suggestion (should improve)

1. **PII handling radio selected color stays Element Plus blue** rather than brand primary — Save/Confirm buttons correctly use REDBC/GREENBC. Evidence: `01c`. Non-blocking.  
2. **Stale reason validation after typing** — after failed submit, filling reason may leave «A reason is required…» until next submit (`02d` capture sequence). Prefer clear-on-input. Non-blocking (functional gate still enforces on submit).

### 🟢 Nice to have

1. CamelCase tree folder label «Customer customer» for `customerName` is pre-existing variable-tree naming; search expands to leaf with PII badge.

## Counts

| Severity | Count |
| --- | --- |
| 🔴 Critical | **0** |
| 🟡 Major / Suggestion | 2 |
| 🟢 Minor / Nice to have | 1 |

## Artifacts

| Path | Role |
| --- | --- |
| `frontend/e2e/CE-G03-testdata-pii-uiux-evidence.spec.ts` | Stage 7 capture spec |
| `frontend/e2e/helpers/uiux-evidence.ts` | CE-G03 evidence helpers |
| `frontend/e2e/evidence/CE-G03-testdata-pii-uiux-manifest.md` | This manifest |
| `frontend/e2e/evidence/CE-G03-testdata-pii/screenshots/*.png` | Dual-brand frames |
| `frontend/e2e/evidence/CE-G03-testdata-pii-manifest.md` | Stage 6 functional (PASS) |
| `frontend/src/components/templates/TemplateTestDataSetEditDialog.vue` | Critical truncation fix |

## Handoff

- Stage 6 functional: **PASS** 5/5.  
- Stage 7 UIUX: **PASS** (Critical = 0).  
- **architecture-reviewer may proceed:** **YES**.  
- Park U13/C06 untouched.  
- **No commit** (orchestrator constraint).
