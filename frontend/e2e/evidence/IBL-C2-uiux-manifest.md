# IBL-C2 UIUX Evidence Manifest — Side-by-side rendered PDF compare (F18)

**Task:** IBL-C2 / Task Master **#121** — F18 rendered output compare UI  
**Slice:** `ibl-c2-rendered-compare-ui` (`feat/ibl-c2-rendered-compare-ui`)  
**Worktree:** `D:/working/DGE-ibl-c2-rendered-compare-ui`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-19  
**Viewport:** 1920×1080 (desktop dual-brand) + 900×900 (narrow stacked)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (Stage 5 DEPLOY_OK; Stage 6 functional already green; **no redeploy** this pass)  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 + narrow stack artifacts present; harness onboarding dismiss hardened)

## Behavior SoT

- `docs/behavior/ibl-c2-rendered-compare-ui.md` (`BDD-IBL-C2`, `ready`)
- Spec: `frontend/e2e/IBL-C2-rendered-compare-uiux-evidence.spec.ts`
- Helpers: `dismissOnboardingTourIfPresent` + `switchBrand` in `frontend/e2e/helpers/uiux-evidence.ts`

## Coordination with Stage 6

| Artifact | Status |
| --- | --- |
| Functional: `IBL-C2-rendered-compare.spec.ts` | Prior Stage 6 **PASS** (no redeploy required) |
| UIUX evidence: `IBL-C2-rendered-compare-uiux-evidence.spec.ts` | **3/3 passed** (this stage) |

## Test execution

| Command | Result |
| --- | --- |
| Stage 7 evidence: `IBL-C2-rendered-compare-uiux-evidence.spec.ts` | **3/3 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (rendered-compare-dialog REDBC + GREENBC) | **0 critical** |
| Spec asserts | Side-by-side @1920; stacked @900; brand primary close button; no horizontal overflow |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts e2e/IBL-C2-rendered-compare-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 12 passed (1.5m) — 2026-07-19
```

## Harness fix (Stage 7 Critical unblock)

Prior fail: REDBC@1920 timed out on `switchBrand` — `.brand-switcher` click intercepted by `[data-testid=onboarding-tour] .el-tour__hollow` because post-login dismiss used a 3s `isVisible` poll that missed delayed LR-C8 tour mount.

Changes:

1. Shared `dismissOnboardingTourIfPresent` in `uiux-evidence.ts` — wait up to 12–15s for Skip or hollow; `force: true` Skip click; Esc if only hollow; retry ≤3 until both gone.
2. `switchBrand` dismisses tour before brand click and retries dismiss+click on intercept.
3. IBL-C2 evidence spec uses shared helper with long post-login wait + short post-switchBrand dismiss.

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-side-by-side-compare-redbc-1920x1080.png` | REDBC | Compare dialog — dual PDF panes side-by-side |
| 1b | `01b-compare-dialog-crop-redbc-1920x1080.png` | REDBC | Dialog crop |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-side-by-side-compare-greenbc-1920x1080.png` | GREENBC | Same compare surface dual-brand |
| 2b | `02b-compare-dialog-crop-greenbc-1920x1080.png` | GREENBC | Dialog crop — teal primary |
| 2c | `02c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `03-stacked-compare-redbc-900x900.png` | REDBC | Narrow viewport — panes stacked |
| 3b | `03b-stacked-panel-crop-redbc-900x900.png` | REDBC | Stacked panel crop |

Path prefix: `frontend/e2e/evidence/IBL-C2/screenshots/` (**8** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01 / 02 full frames |
| Logo / brand header switch | ✅ | 01c Red Bank; 02c Green Bank |
| Side-by-side dual PDF panes @1920 | ✅ | Spec layout assert + 01 / 02 |
| Stacked panes @900 still show both PDFs | ✅ | Spec stacked assert + 03 / 03b |
| Brand primary on dialog Close | ✅ | REDBC `rgb(219,0,17)`; GREENBC `rgb(0,132,127)` |
| No horizontal page overflow @1920 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | 9/9 + dialog-scoped critical = 0 |
| English-first copy | ✅ | Compare rendered outputs / pane labels |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | No 🔴 Critical | — |
| 🟢 Nice to have | Shared dismiss helper can be adopted by other CE/PRR UIUX specs that still inline the 3s poll | `uiux-evidence.ts` |

### Notes (non-blocking)

1. Harness-only fix — no Vue product change; no Docker redeploy.
2. Fixture: `prepareCdpMvpGoldenDraft` + two SUCCEEDED async preview runs with PDF.
3. Helpers: `IBL_C2_VIEWPORT` 1920×1080 / `IBL_C2_NARROW_VIEWPORT` 900×900 + `captureIblC2*`.

## Verdict / merge gate

| Gate | Value |
| --- | --- |
| **Verdict** | **PASS** |
| **Critical** | **0** |
| **merge_go** | **true** (Stage 7 retry recommendation) |

## Next

**Stage 8 — `architecture-reviewer`** (or Stage 7 reviewer re-ack with this manifest)
