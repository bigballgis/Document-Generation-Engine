# CE-U14 UIUX Evidence Manifest — Dashboard lifecycle todos

**Task:** CE-U14 / Task Master **#90** — Dashboard Tasks TEST / APPROVAL / PENDING_RELEASE deep links to decision surfaces  
**Slice:** `ce-u14-dashboard-lifecycle-todos` (`feat/ce-u14-dashboard-lifecycle-todos`)  
**Worktree:** `D:/working/DGE-ce-u14-dashboard-lifecycle-todos`  
**Reviewer:** e2e-uiux-reviewer (Stage 7 retry attempt 2)  
**Date:** 2026-07-16  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand @1920 artifacts present; no CE-U15 Stepper DOM)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `CE-U14-dashboard-lifecycle-todos.spec.ts` | **6/6 passed** (see `CE-U14-manifest.md`) |
| Stage 7 evidence: `CE-U14-dashboard-lifecycle-todos-uiux-evidence.spec.ts` | **3/3 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** |
| Inline critical axe (TEST queue / testing rail / APPROVAL rail / author fail-closed) | **0 critical** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U14-dashboard-lifecycle-todos-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 12 passed (56.7s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-test-queue-redbc-1920x1080.png` | REDBC | Tasks `?queue=TEST` — Waiting on my testing + In testing partition |
| 1b | `01b-test-queue-tasks-crop-redbc-1920x1080.png` | REDBC | `#tasks-section` crop |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-testing-action-rail-redbc-1920x1080.png` | REDBC | Open → `#dev-workspace` testing rail (Confirm test pass / Record test failure) |
| 2b | `02b-dev-workspace-crop-redbc-1920x1080.png` | REDBC | `#dev-workspace` crop |
| 2c | `02c-workspace-actions-crop-redbc-1920x1080.png` | REDBC | `.workspace-tab-shell__actions` crop |
| 3 | `03-testing-action-rail-greenbc-1920x1080.png` | GREENBC | Testing rail dual-brand |
| 3b | `03b-dev-workspace-crop-greenbc-1920x1080.png` | GREENBC | `#dev-workspace` crop |
| 3c | `03c-workspace-actions-crop-greenbc-1920x1080.png` | GREENBC | Action rail crop |
| 3d | `03d-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3e | `03e-test-queue-greenbc-1920x1080.png` | GREENBC | TEST queue dual-brand |
| 4 | `04-approval-queue-redbc-1920x1080.png` | REDBC | Tasks `?queue=APPROVAL` |
| 4b | `04b-approval-action-rail-redbc-1920x1080.png` | REDBC | Open → approval rail (Approve / Reject) |
| 4c | `04c-dev-workspace-approval-crop-redbc-1920x1080.png` | REDBC | `#dev-workspace` approval crop |
| 4d | `04d-workspace-actions-approval-crop-redbc-1920x1080.png` | REDBC | Approve/Reject action crop |
| 5 | `05-approval-action-rail-greenbc-1920x1080.png` | GREENBC | Approval rail dual-brand |
| 5b | `05b-workspace-actions-approval-crop-greenbc-1920x1080.png` | GREENBC | Action rail crop |
| 5c | `05c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 6 | `06-author-fail-closed-redbc-1920x1080.png` | REDBC | Author Tasks — no lifecycle decision partitions |
| 6b | `06b-author-tasks-crop-redbc-1920x1080.png` | REDBC | `#tasks-section` crop |
| 7 | `07-author-fail-closed-greenbc-1920x1080.png` | GREENBC | Author fail-closed dual-brand |
| 7b | `07b-author-tasks-crop-greenbc-1920x1080.png` | GREENBC | Tasks crop |
| 7c | `07c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 7d | `07d-author-queue-test-deeplink-greenbc-1920x1080.png` | GREENBC | Author `?queue=TEST` does not invent TEST rows |

Path prefix: `frontend/e2e/evidence/CE-U14/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–03e, 04–05, 06–07 |
| Logo / brand header switch | ✅ | 01c Red Bank; 03d / 05c / 07c Green Bank |
| TEST queue + testing action rail | ✅ | 01 + 02/03 (`#dev-workspace` + `.workspace-tab-shell__actions`) |
| APPROVAL decision rail | ✅ | 04b / 05 (Approve / Reject) |
| Author fail-closed (no TEST/APPROVAL/PENDING_RELEASE partitions) | ✅ | 06 / 07 / 07d |
| No CE-U15 Stepper DOM | ✅ | Spec asserts `.el-steps` / stepper selectors count 0 on all surfaces |
| No horizontal overflow @1920 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | 9/9 + inline critical axe on changed surfaces |
| English-first copy | ✅ | My tasks / Waiting on my testing / Confirm test pass / Approve |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None | — |

### Notes (non-blocking)

1. Table Stage/Summary/Submitter cells truncate with ellipsis at 1920 — expected dense OA table behavior; Open CTA remains visible.
2. Pass/Fail and Approve/Reject use semantic green/red on the action rail (not brand-primary wash) — consistent with existing lifecycle decision affordances.
3. Helpers: `CE_U14_VIEWPORT` 1920×1080 + `captureCeU14Screenshot` / `captureCeU14LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
4. Spec: `frontend/e2e/CE-U14-dashboard-lifecycle-todos-uiux-evidence.spec.ts`.
5. No merge / no new deploy performed (stage 7 handoff only). No product Done claim.
