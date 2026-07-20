# TM #144 UIUX Evidence Manifest — Published template test artifacts

**Task:** Task Master **#144** — `published-template-test-artifacts`  
**Slice:** `published-template-test-artifacts` (`feat/published-template-test-artifacts`)  
**Worktree:** `D:/working/DGE-published-template-test-artifacts`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-21  
**Viewport:** 1920×1080 (desktop-first; OA standard dual-brand)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 DEPLOY_OK)  
**Verdict:** **PASS** (Critical = 0)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `TM144-published-template-test-artifacts.spec.ts` | **4/4 passed** (see functional manifest) |
| Stage 7 evidence: `TM144-published-template-test-artifacts-uiux-evidence.spec.ts` | **3/3 passed** (~1.8m) |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (release Testing REDBC/GREENBC + Open preview + toast) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/TM144-published-template-test-artifacts-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# a11y 9/9 + UIUX 3/3 (UIUX alone re-run: 3 passed, 1.8m)
```

## Screenshot inventory (23)

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-release-testing-readonly-redbc-1920x1080.png` | REDBC | Release Testing — read-only summary + Test run history + Preview run history |
| 1b | `01b-release-testing-panel-crop-redbc-1920x1080.png` | REDBC | `[data-testid=release-testing-readonly]` crop |
| 1c | `01c-batch-history-crop-redbc-1920x1080.png` | REDBC | `.batch-test-history` crop |
| 1d | `01d-preview-history-crop-redbc-1920x1080.png` | REDBC | `.preview-run-history` + Download DOCX/PDF |
| 1e | `01e-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-release-testing-readonly-greenbc-1920x1080.png` | GREENBC | Same Testing surface dual-brand |
| 2b | `02b-release-testing-panel-crop-greenbc-1920x1080.png` | GREENBC | Release testing panel crop |
| 2c | `02c-batch-history-crop-greenbc-1920x1080.png` | GREENBC | Batch history crop |
| 2d | `02d-preview-history-crop-greenbc-1920x1080.png` | GREENBC | Preview history crop |
| 2e | `02e-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-open-preview-selected-redbc-1920x1080.png` | REDBC | Batch expand + Open preview → selected preview row |
| 3b | `03b-preview-history-selected-crop-redbc-1920x1080.png` | REDBC | Preview history with selection |
| 3c | `03c-selected-row-crop-redbc-1920x1080.png` | REDBC | `tr.is-selected` brand accent soft (pink/red tint) |
| 4 | `04-open-preview-selected-greenbc-1920x1080.png` | GREENBC | Open preview selection dual-brand |
| 4b | `04b-preview-history-selected-crop-greenbc-1920x1080.png` | GREENBC | Preview history with selection |
| 4c | `04c-selected-row-crop-greenbc-1920x1080.png` | GREENBC | `tr.is-selected` teal accent soft |
| 4d | `04d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 5 | `05-open-data-set-toast-redbc-1920x1080.png` | REDBC | Open data set → English info toast; downloads visible |
| 5b | `05b-toast-crop-redbc.png` | REDBC | Toast copy crop |
| 5c | `05c-preview-downloads-with-toast-crop-redbc-1920x1080.png` | REDBC | Preview downloads while toast present |
| 6 | `06-open-data-set-toast-greenbc-1920x1080.png` | GREENBC | Toast dual-brand |
| 6b | `06b-toast-crop-greenbc.png` | GREENBC | Toast copy crop |
| 6c | `06c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |

Path prefix: `frontend/e2e/evidence/TM144-published-template-test-artifacts/screenshots/`

## OA checklist (bank OA + Stage 6 residuals)

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–02, 03–04, 05–06 |
| Logo / brand header switch | ✅ | 01e Red Bank; 02e / 04d / 06c Green Bank |
| Read-only summary English copy | ✅ | 01 / 02 — “This published release completed the testing workflow…” |
| Test run history + Preview run history | ✅ | 01c / 01d / 02c / 02d |
| No Run preview / Full test / authoring | ✅ | Spec `assertPublishedTestingNoAuthoring`; frames show no authoring controls; **no** `showAuthoringSection` true assertion |
| DOCX/PDF download affordances | ✅ | 01d / 03c / 04c — Download DOCX / Download PDF visible |
| Open preview → `is-selected` visible under brand tokens | ✅ | 03c pink/red accent; 04c teal accent (`--brand-accent-soft`) |
| Open data set toast English-first, non-blocking | ✅ | 05b / 06b — “Data sets are not editable on published release detail…” |
| Toast does not obscure download buttons | ✅ | Spec geometry assert + 05 / 05c (toast bottom; downloads clear) |
| No horizontal overflow @1920 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke + inline critical axe | ✅ | 9/9 + 0 critical on changed surfaces |
| English-first i18n copy | ✅ | All frames (en locale) |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Batch **Results** cell can render sparse `/ passed` (missing numeric succeeded/total) on some history rows | `BatchTestHistoryPanel.vue` counts column — polish; not PTA acceptance blocker (same class of residual as CE-U18) |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Fixture: catalog PUBLISHED demos with SUCCEEDED preview artifacts (prefer FOL / DEMO-*); batch `sampleResults` for Open preview / Open data set injected via Playwright route when seed lacks `previewId` (same Stage 6 residual).
2. Selection highlight uses `TemplatePreviewRunHistoryPanel.scss` `:deep(.preview-run-row.is-selected > td) { background: var(--brand-accent-soft) }` — verified distinct from white under both brands.
3. Toast is Element Plus `ElMessage.info` (bottom/default placement) — geometry check confirms no overlap with Download DOCX.
4. Helpers: `TM144_PTA_VIEWPORT` 1920×1080 + `captureTm144PtaScreenshot` / `captureTm144PtaLocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
5. Spec: `frontend/e2e/TM144-published-template-test-artifacts-uiux-evidence.spec.ts`.
6. No app code changes in Stage 7. No merge / commit / Done claim.

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/TM144-published-template-test-artifacts-uiux-evidence.spec.ts` | Stage 7 dual-brand UIUX capture + residual asserts |
| `frontend/e2e/helpers/uiux-evidence.ts` | `TM144_PTA_*` evidence dirs + capture helpers |
| `frontend/e2e/evidence/TM144-published-template-test-artifacts-uiux-manifest.md` | This manifest |
| `frontend/e2e/evidence/TM144-published-template-test-artifacts/screenshots/*` | 23 PNG frames |

## References

- BDD: `docs/behavior/published-template-test-artifacts.md`
- Functional evidence: `frontend/e2e/evidence/TM144-published-template-test-artifacts-manifest.md`
- `.cursor/skills/frontend-oa-design/SKILL.md`
- Pattern: `frontend/e2e/CE-U18-batch-test-history-uiux-evidence.spec.ts`

## Next

**Stage 8 — `architecture-reviewer`**
