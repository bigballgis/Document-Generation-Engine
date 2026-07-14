# CE-U08 UIUX Evidence Manifest — Content module review loop

**Task:** CE-U08 / Task Master **#83** — Dashboard CM review/rework todos + rejectionReason versions column  
**Slice:** `ce-u08-content-module-review-loop` (`feat/ce-u08-content-module-review-loop`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (**UP**; CE-U08 worktree images)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-U08-content-module-review-loop.spec.ts` | **3/3 passed** (prior handoff) |
| Stage 7 evidence: `CE-U08-content-module-review-loop-uiux-evidence.spec.ts` | **2/2 passed** (15.7s) |
| Optional: `a11y-smoke.spec.ts` | **not green** — stack `ERR_CONNECTION_REFUSED` during redeploy flap (optional; not a CE-U08 surface blocker) |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U08-content-module-review-loop-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 2 passed (15.7s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-dashboard-cm-review-redbc-1920x1080.png` | REDBC | My tasks — Standard clauses to review partition |
| 1b | `01b-cm-review-partition-crop-redbc-1920x1080.png` | REDBC | Review partition crop |
| 2 | `02-dashboard-cm-review-greenbc-1920x1080.png` | GREENBC | Same dashboard after brand switch |
| 2b | `02b-brand-header-greenbc-crop.png` | GREENBC | Header logo + Green Bank title |
| 3 | `03-lifecycle-approve-reject-redbc-1920x1080.png` | REDBC | Review & release — Approve/Reject + history |
| 4 | `04-lifecycle-approve-reject-greenbc-1920x1080.png` | GREENBC | Same lifecycle decision rail |
| 5 | `05-dashboard-cm-rework-redbc-1920x1080.png` | REDBC | My tasks — Standard clauses to fix partition |
| 5b | `05b-cm-rework-partition-crop-redbc-1920x1080.png` | REDBC | Rework partition crop |
| 6 | `06-dashboard-cm-rework-greenbc-1920x1080.png` | GREENBC | Rework dashboard dual-brand |
| 7 | `07-lifecycle-rejection-rework-redbc-1920x1080.png` | REDBC | Rejected timeline + Submit for approval |
| 8 | `08-versions-rejection-reason-redbc-1920x1080.png` | REDBC | Versions — Rejection reason column |
| 9 | `09-versions-rejection-reason-greenbc-1920x1080.png` | GREENBC | Versions dual-brand |
| 9b | `09b-brand-header-greenbc-crop.png` | GREENBC | Header crop on versions |
| 10 | `10-brand-header-redbc-crop.png` | REDBC | Header logo after switch back |

Path prefix: `frontend/e2e/evidence/CE-U08/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC (#DB0011) + GREENBC (#00847F) @1920 | ✅ | 01–02, 03–04, 05–06, 08–09 + header crops |
| Logo switch (`BrandLogo` / header brand text) | ✅ | 02b, 09b, 10 |
| CM review partition readable; entity names not raw UUID | ✅ | 01, 01b, 02 |
| CM rework partition + Open deep-link affordance | ✅ | 05, 05b, 06 |
| Lifecycle Approve/Reject action rail + review history | ✅ | 03, 04 |
| Rejection reason in timeline + Submit for approval | ✅ | 07 |
| Versions `Rejection reason` column (`Wording not acceptable`) | ✅ | 08, 09 |
| English-first i18n labels | ✅ | Spec assertions + screenshots |
| No text overflow / overlap @1920 | ✅ | Full-page + partition crops |
| Onboarding tour does not block brand switch / Open | ✅ | Spec dismiss helper |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 | Optional `a11y-smoke.spec.ts` could not complete — Docker frontend flap (`ERR_CONNECTION_REFUSED`) after evidence capture. Re-run when stack is stable; not a CE-U08 visual defect. | frontend-oa-design §a11y smoke |
| 🟢 | Empty template partitions above CM sections show empty-state illustration while CM rows exist — expected (separate workflow kinds); no clutter fix required for this slice. | Dashboard task hub |

## Notes

1. Helpers: `CE_U08_VIEWPORT` 1920×1080 + `captureCeU08Screenshot` / `captureCeU08LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-U08-content-module-review-loop-uiux-evidence.spec.ts`.
3. Functional stage-6 evidence remains in `frontend/e2e/evidence/CE-U08-manifest.md` (separate from this UIUX manifest).
4. No merge performed (stage 7 handoff only).
