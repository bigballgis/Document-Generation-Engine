# CE-U03 UIUX Evidence Manifest — Schema-driven test data dialog

**Task:** CE-U03 / Task Master #55 — schema-driven test data form  
**Slice:** `ce-u03-testdata-schema-form` (`feat/ce-u03-testdata-schema-form`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-14  
**Viewport:** 1920×1080 (desktop-first, CDP parity)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (**UP**)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on schema form dialog)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-U03-testdata-schema-form.spec.ts` | **9/9 passed** (upstream) |
| Stage 7 evidence: `CE-U03-testdata-schema-form-uiux-evidence.spec.ts` | **4/4 passed** (~25.8s) |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test e2e/CE-U03-testdata-schema-form-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

Review method: visual inspection of on-disk PNGs; dual-brand token check (REDBC / GREENBC); zh-CN spot-check; large-payload collapse toggle; onboarding tour dismissed before brand/locale switches (LRP-C8 interaction).

### Surface coverage

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Schema form skeleton (compact schema) | Create dialog — fields, Generate, Advanced collapse | 01–04 (REDBC), 05–07 (GREENBC) |
| Advanced JSON expanded | Collapse panel + textarea | 02, 07 |
| Field error summary (client validation) | Inline summary list | 03, 06, 10 |
| Compute variable omitted | Form density without compute key | 04 |
| Large payload (≥12 vars) | Auto-expanded Advanced JSON + collapse toggle | 08–08d |
| zh-CN labels + required message | Dialog chrome + 此字段为必填项 | 09–10 |
| Brand header / logo | REDBC / GREENBC wordmark | 01c, 05c |

## Screenshot inventory (24)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `01-schema-form-skeleton-redbc-en-1920x1080.png` | Create dialog — schema fields | REDBC | en |
| 2 | `01b-schema-form-dialog-crop-redbc-en.png` | Dialog crop | REDBC | en |
| 3 | `01c-brand-header-redbc-en.png` | REDBC header | REDBC | en |
| 4 | `02-advanced-json-expanded-redbc-en-1920x1080.png` | Advanced JSON open | REDBC | en |
| 5 | `02b-advanced-json-dialog-crop-redbc-en.png` | Advanced JSON crop | REDBC | en |
| 6 | `03-field-errors-summary-redbc-en-1920x1080.png` | Required field errors | REDBC | en |
| 7 | `03b-field-errors-dialog-crop-redbc-en.png` | Field errors crop | REDBC | en |
| 8 | `04-compute-skip-form-density-redbc-en-1920x1080.png` | No compute field | REDBC | en |
| 9 | `04b-compute-skip-dialog-crop-redbc-en.png` | Compute-skip crop | REDBC | en |
| 10 | `05-schema-form-skeleton-greenbc-en-1920x1080.png` | Create dialog | GREENBC | en |
| 11 | `05b-schema-form-dialog-crop-greenbc-en.png` | Dialog crop | GREENBC | en |
| 12 | `05c-brand-header-greenbc-en.png` | GREENBC header | GREENBC | en |
| 13 | `06-field-errors-summary-greenbc-en-1920x1080.png` | Field errors | GREENBC | en |
| 14 | `06b-field-errors-dialog-crop-greenbc-en.png` | Field errors crop | GREENBC | en |
| 15 | `07-advanced-json-expanded-greenbc-en-1920x1080.png` | Advanced JSON | GREENBC | en |
| 16 | `07b-advanced-json-dialog-crop-greenbc-en.png` | Advanced JSON crop | GREENBC | en |
| 17 | `08-large-payload-advanced-expanded-redbc-en-1920x1080.png` | ≥12 vars auto-expand | REDBC | en |
| 18 | `08b-large-payload-dialog-crop-redbc-en.png` | Large dialog crop | REDBC | en |
| 19 | `08c-large-payload-collapsed-crop-redbc-en.png` | Collapsed Advanced JSON | REDBC | en |
| 20 | `08d-large-payload-reexpanded-crop-redbc-en.png` | Re-expanded | REDBC | en |
| 21 | `09-schema-form-zhcn-redbc-1920x1080.png` | zh-CN labels | REDBC | zh-CN |
| 22 | `09b-schema-form-dialog-crop-zhcn-redbc.png` | zh-CN dialog crop | REDBC | zh-CN |
| 23 | `10-field-errors-zhcn-redbc-1920x1080.png` | zh-CN required message | REDBC | zh-CN |
| 24 | `10b-field-errors-dialog-crop-zhcn-redbc.png` | zh-CN errors crop | REDBC | zh-CN |

Path prefix: `frontend/e2e/evidence/CE-U03-testdata-schema-form/screenshots/`

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Dialog uses Element Plus + tokenized spacing / typography | ✅ | 01b, 05b |
| Primary Save action visible; Cancel secondary | ✅ | 01, 05 |
| Field error summary readable; no overlap with footer | ✅ | 03, 06, 10 |
| Dual-brand primary tokens (REDBC / GREENBC) | ✅ | 01c vs 05c; 01 vs 05 |
| English-first base + zh-CN lazy parity on labels | ✅ | 09; `templates.testDataSets.*` |
| Advanced JSON collapse uses EP collapse header (large payload toggle) | ✅ | 08c–08d |
| No text overflow on dialog @1920×1080 | ✅ | All crops |
| Onboarding tour dismissed before brand/locale switch | ✅ | Spec helper; LRP-C8 note |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Onboarding tour blocks header interactions until Skip** — LR-C8 mask intercepts brand/locale switcher clicks. Evidence spec dismisses tour via `onboarding-tour-skip`; product authors should document same for manual QA. **Non-blocking** — documented in spec.

2. **Large-schema form scroll density** — ≥12 TEXT fields + expanded JSON increases dialog height (08). Acceptable at 1920×1080; consider sticky footer or max-height scroll region if schemas grow further. **Non-blocking**.

## Artifacts

| Path | Role |
| --- | --- |
| `frontend/e2e/CE-U03-testdata-schema-form-uiux-evidence.spec.ts` | Stage 7 capture spec |
| `frontend/e2e/helpers/uiux-evidence.ts` | CE-U03 evidence helpers |
| `frontend/e2e/evidence/CE-U03-testdata-schema-form-uiux-manifest.md` | This manifest |
| `frontend/e2e/evidence/CE-U03-testdata-schema-form/screenshots/*.png` | 24 frames |

## Handoff

- Stage 6 functional manifest: `CE-U03-testdata-schema-form-manifest.md` (**PASS** 9/9).
- Architecture review: **PASS_WITH_NOTES**, Critical=0 (upstream).
- Merge note: exclude incidental demo `.docx` fixture byte drift from slice merge (restore from `main`).
