# Reminder timing settings IA — Stage 7 UIUX Evidence Manifest

**Task:** Task Master **#153** — Reminder timing settings IA  
**Slice:** `reminder-timing-settings-ia` (`feat/reminder-timing-settings-ia`)  
**Worktree:** `D:/working/DGE-reminder-timing-settings-ia`  
**Reviewer:** frontend-engineer (Stage 7 residual — Ask mode had blocked writes)  
**Date:** 2026-07-22  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP**  
**Verdict:** **PASS** (Critical = 0; dual-brand evidence captured)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | System settings Reminder timing (full page) | `/system/settings/reminder-timing` | REDBC + GREENBC |
| 2 | Dashboard Overview without timeout panel | `/dashboard` (GLOBAL_ADMIN) | REDBC + GREENBC |
| 3 | Team settings dialog (group-scoped editor) | `/entitlement/groups` → Team settings | REDBC + GREENBC |
| 4 | Forbidden System settings (no leak) | GROUP_ADMIN → `/system/settings/reminder-timing` | REDBC |
| 5 | GROUP_ADMIN dashboard without timeout panel | `/dashboard` | REDBC |

## Test execution

| Command | Result |
| --- | --- |
| `reminder-timing-settings-ia-uiux-evidence.spec.ts` | **5/5 passed** (~46.6s) |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/reminder-timing-settings-ia-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 5/5 passed
```

## Screenshot inventory

Path prefix: `frontend/e2e/evidence/reminder-timing-settings-ia/screenshots/` (**16** files)

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-system-settings-reminder-timing-redbc-1440x900.png` | REDBC | System settings Reminder timing full page |
| 1b | `01b-brand-header-redbc-crop.png` | REDBC | Shell brand header |
| 1c | `01c-timeout-panel-redbc-crop.png` | REDBC | Timeout / Reminder timing panel crop |
| 2 | `01-system-settings-reminder-timing-greenbc-1440x900.png` | GREENBC | System settings dual-brand |
| 2b | `01b-brand-header-greenbc-crop.png` | GREENBC | Shell brand header |
| 2c | `01c-timeout-panel-greenbc-crop.png` | GREENBC | Panel crop |
| 3 | `02-dashboard-no-timeout-redbc-1440x900.png` | REDBC | Dashboard Overview — no timeout panel |
| 4 | `02-dashboard-no-timeout-greenbc-1440x900.png` | GREENBC | Dashboard Overview dual-brand |
| 5 | `03-team-settings-dialog-redbc-1440x900.png` | REDBC | Groups → Team settings dialog |
| 5b | `03b-team-settings-dialog-redbc-crop.png` | REDBC | Dialog crop |
| 5c | `03c-brand-header-redbc-crop.png` | REDBC | Shell brand header |
| 6 | `03-team-settings-dialog-greenbc-1440x900.png` | GREENBC | Team settings dual-brand |
| 6b | `03b-team-settings-dialog-greenbc-crop.png` | GREENBC | Dialog crop |
| 6c | `03c-brand-header-greenbc-crop.png` | GREENBC | Shell brand header |
| 7 | `04-forbidden-system-settings-redbc-1440x900.png` | REDBC | GROUP_ADMIN forbidden — Access denied, no panel |
| 8 | `05-group-admin-dashboard-no-timeout-redbc-1440x900.png` | REDBC | GROUP_ADMIN dashboard — no Team settings / timeout on Overview |

Spec: `frontend/e2e/reminder-timing-settings-ia-uiux-evidence.spec.ts`  
Helpers: `REMINDER_TIMING_IA_*` + `captureReminderTimingIa*` in `frontend/e2e/helpers/uiux-evidence.ts`  
Manifest (this file): `frontend/e2e/evidence/reminder-timing-settings-ia-uiux-manifest.md`  
Functional evidence (Stage 6): `frontend/e2e/evidence/reminder-timing-settings-ia-manifest.md`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01–03 |
| System settings full page (not dashboard panel) | ✅ | 01 / 01c — fluid layout; no scope radios |
| Team settings on Groups surface (GROUP only) | ✅ | 03 / 03b — readonly group code; no System settings nav |
| Dashboard Overview without Reminder timing panel | ✅ | 02, 05 |
| Dual-brand REDBC / GREENBC | ✅ | 01 vs 02; 03 REDBC vs GREENBC; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01b / 03c crops |
| Forbidden surface — no data leak | ✅ | 04 — Access denied; no `.timeout-config-card` |
| No horizontal overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| English-first i18n | ✅ | Reminder timing / Team settings / System settings |
| Fluid layout on System settings | ✅ | Spec `assertFluidLayout` |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | **None critical.** System settings + Team settings dialog dual-brand evidence green. | — |
| 🟢 Note | Forbidden page has no brand switcher — evidence sets REDBC on `/dashboard` before deep-link. | Forbidden surface shell |
| 🟢 Note | GLOBAL_ADMIN journey CTA `setReminderDefaults` deep-links to `ROUTE_PATH_BY_KEY[systemSettingsReminderTiming]` (unit-tested); GROUP_ADMIN is not routed to System settings for edit. | Architecture Yellow #1 |

## Stage 7 gate

**PASS** — Critical = **0**. Durable dual-brand screenshots + this manifest unblock Stage 10 deploy evidence (after architecture clearance of Yellow #1).
