# Reminder timing settings IA — Stage 6 E2E functional evidence

**Task:** #153 · `reminder-timing-settings-ia`  
**Date:** 2026-07-22  
**Config:** `frontend/playwright.docker.config.ts` @ `:4173` / `:8080`  
**Verdict:** **PASS** (12/12 focused acceptance set, 34.7s)

## Specs

| Spec | Role |
| --- | --- |
| `frontend/e2e/reminder-timing-settings-ia.spec.ts` | Primary BDD-RT-IA journeys |
| `frontend/e2e/collaboration-todos.spec.ts` (System settings save) | BDD-RT-IA-013 relocate |
| `frontend/e2e/P21-T10-global-admin-journey.spec.ts` (Reminder timing case) | Dashboard absent + System settings |
| `frontend/e2e/P21-T09b-reminder-exception-l1.spec.ts` (Reminder timing L1) | L1 jargon surface relocated |
| `frontend/e2e/a11y-smoke.spec.ts` (System settings heading) | Heading smoke relocated |

## Coverage map

| Test | BDD |
| --- | --- |
| GLOBAL_ADMIN System settings nav → full page | BDD-RT-IA-001, 014 |
| GLOBAL_ADMIN save `scopeType=GLOBAL` | BDD-RT-IA-002, 009, 011 |
| GROUP_ADMIN Team settings dialog save GROUP | BDD-RT-IA-003, 004, 009 |
| Dashboard Overview no `.timeout-config-card` / Team settings | BDD-RT-IA-005, 015 |
| Fail-closed deep-link / non-maintain roles | BDD-RT-IA-006, 007 |
| Relocated smoke/regression cases | BDD-RT-IA-013 |

Not asserted in this functional set (covered elsewhere / Stage 7):

- BDD-RT-IA-008 / 016 — OpenAPI + API 401/403 contract tests
- BDD-RT-IA-010 — load/save error messaging (unit + existing error keys)
- BDD-RT-IA-012 — escalation notification-only (`collaboration-todos` overdue path)
- Visual/responsive/dual-brand polish — Stage 7 `e2e-uiux-reviewer`

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

**Report:** `frontend/playwright-report/docker`  
**Blockers:** none for Stage 6 functional IA  
**Next:** Stage **7** — `e2e-uiux-reviewer`
