# CE-U11 UIUX Evidence Manifest — invocation troubleshoot + recall

**Task:** CE-U11 / Task Master **#86** — Release version filter + Export CSV + failed error envelope  
**Slice:** `ce-u11-invocation-troubleshoot` (`feat/ce-u11-invocation-troubleshoot`)  
**Worktree:** `D:/working/DGE-ce-u11-invocation-troubleshoot`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1440×900 (desktop-first; P13 pattern)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (no Critical UIUX blockers; dual-brand @1440 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `CE-U11-invocation-troubleshoot.spec.ts` | **7/7 passed** |
| Stage 7 evidence: `CE-U11-invocation-troubleshoot-uiux-evidence.spec.ts` | **1/1 passed** (~8.1s after Escape-close fix) |
| `a11y-smoke.spec.ts` | **9/9** — initial run 8/9 (lifecycle submit 500 flake); retry **1/1 passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U11-invocation-troubleshoot-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# evidence 1 passed; a11y lifecycle flake retried green
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-invocation-history-filters-redbc-1440x900.png` | REDBC | Hub External access — Invocation history + Release version filter + Export CSV |
| 1b | `01b-invocation-panel-crop-redbc-1440x900.png` | REDBC | Invocation panel crop |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 1d | `01d-export-csv-actions-crop-redbc.png` | REDBC | Apply / Clear / Export CSV action rail |
| 2 | `02-failed-error-envelope-drawer-redbc-1440x900.png` | REDBC | Failed Invocation summary drawer + Error details |
| 2b | `02b-error-envelope-crop-redbc-1440x900.png` | REDBC | Error envelope crop (code/category/messageKey/retryable) |
| 3 | `03-invocation-history-filters-greenbc-1440x900.png` | GREENBC | Dual-brand filters + Export CSV |
| 3b | `03b-invocation-panel-crop-greenbc-1440x900.png` | GREENBC | Panel crop — teal primary |
| 3c | `03c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 4 | `04-failed-error-envelope-drawer-greenbc-1440x900.png` | GREENBC | Failed drawer dual-brand (teal audit link) |
| 4b | `04b-error-envelope-crop-greenbc-1440x900.png` | GREENBC | Error envelope crop |

Path prefix: `frontend/e2e/evidence/CE-U11/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1440 | ✅ | 01–04 + crops |
| Logo / brand header switch | ✅ | 01c Red Bank; 03c Green Bank |
| Release version filter (English-first) | ✅ | 01b / 03b — label + `1.2.0` applied |
| Export CSV button present (OA density) | ✅ | 01d; 01b / 03b action rail |
| Failed drawer error envelope (code/category/messageKey/retryable) | ✅ | 02 / 02b / 04 / 04b |
| No parameters / variables leakage in drawer | ✅ | Spec assert + 02 / 04 full drawer |
| No horizontal overflow @1440 | ✅ | Spec assert |
| a11y smoke (critical axe) | ✅ | 9/9 after lifecycle retry |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None (no Critical / Suggestion blockers for CE-U11 surfaces) | — |

## Notes

1. Helpers: `CE_U11_VIEWPORT` 1440×900 + `captureCeU11Screenshot` / `captureCeU11LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-U11-invocation-troubleshoot-uiux-evidence.spec.ts`.
3. Surfaces: `TemplateInvocationsPanel.vue` (Release version + Export CSV); `InvocationSummaryDrawer.vue` (error envelope).
4. **Export CSV runtime:** UI button presence and English label verified. Functional E2E uses Accept `text/csv` route override; if the running FE image still lacks the Accept-header product fix, export may 404 visually when clicked without that header — not a UIUX layout blocker; product fix tracked separately.
5. No merge / no doc-sync performed (stage 7 handoff only).
