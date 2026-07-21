# TM #153 Stage 6 — E2E functional evidence (mirror)

Canonical FE manifest: [frontend/e2e/evidence/reminder-timing-settings-ia-manifest.md](../../../../frontend/e2e/evidence/reminder-timing-settings-ia-manifest.md)

**Task:** #153 / `reminder-timing-settings-ia`  
**Date:** 2026-07-22  
**Config:** `frontend/playwright.docker.config.ts` @ `:4173` / `:8080`  
**Verdict:** **PASS** (12/12, 34.7s)

## Spec

`frontend/e2e/reminder-timing-settings-ia.spec.ts` (+ relocated regressions)

| Area | Result |
| --- | --- |
| System settings Global default journeys | PASS |
| Team settings Group override journey | PASS |
| Dashboard Overview panel absent | PASS |
| Fail-closed nav / deep-link | PASS |
| Relocated collaboration / P21 / a11y assertions | PASS |

## Residual for Stage 7 (UIUX)

- Re-capture dual-brand screenshots for System settings / Team settings dialog (P14-T02 filenames updated to `08/09-system-settings-timeout-config-panel-*.png`).
- Full `P14-T02-uiux-evidence.spec.ts` capture run (functional assertions already relocated).
- Optional axe on System settings Reminder timing page.

## Pre-existing residuals (out of Stage 6 IA scope)

Observed when broader files were run earlier; **not** regressions from Reminder timing relocate:

- `P21-T09b` confirm-on-behalf lifecycle dialog — `#template-lifecycle-panel` not mounted via `?tab=lifecycle` (hub IA).
- `P21-T10` journey/tasks hub sections — `#journey-section` / `#tasks-section` absent on bare `/dashboard` (tabbed dashboard IA).

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test `
  e2e/reminder-timing-settings-ia.spec.ts `
  e2e/collaboration-todos.spec.ts:132 `
  e2e/a11y-smoke.spec.ts:71 `
  e2e/P21-T10-global-admin-journey.spec.ts:41 `
  e2e/P21-T09b-reminder-exception-l1.spec.ts:22 `
  --config playwright.docker.config.ts --workers=1
```

**Blockers:** none  
**Next:** Stage **7** — `e2e-uiux-reviewer`
