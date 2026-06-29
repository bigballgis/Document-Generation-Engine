# P14-T02 UIUX Evidence Manifest

**Task:** Collaboration to-dos on Dashboard + timeout config (P14-T02) — management UI visual & interaction review  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-06-29  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

> **Consolidation note (2026-06-29):** The standalone Tester / Approver / Escalation
> workbench routes were removed (COR-T11). Collaboration to-dos for every role now live
> on the unified `DashboardView` task hub (`/dashboard#tasks-section`), and legacy
> `/workbench/*` URLs redirect to the dashboard tasks section. Screenshots and a11y
> checks below were re-captured against the dashboard surface.

## Test execution

| Command | Result |
| --- | --- |
| `E2E_TARGET=docker pnpm exec playwright test P14-T02-uiux-evidence.spec.ts collaboration-todos.spec.ts a11y-smoke.spec.ts` | **10/10 passed** (3.0m total) |

### Functional + accessibility coverage

| Spec | Result |
| --- | --- |
| `P14-T02-uiux-evidence.spec.ts` — dashboard task hub, lifecycle tab, timeout config, dual-brand | PASS (15.9s) |
| `collaboration-todos.spec.ts` — dashboard TEST queue to-do, legacy workbench redirect, overdue escalation, admin timeout thresholds | 4/4 PASS |
| `a11y-smoke.spec.ts` — login, content modules, tester dashboard task hub, templates, dashboard timeout config | 5/5 PASS |

### Accessibility smoke

| Check | Result |
| --- | --- |
| Login page primary heading + form controls | PASS |
| Content modules list `h1` after author login | PASS |
| Tester dashboard task hub `h1` after tester login | PASS |
| Templates list `h1` after login | PASS |
| Dashboard `CollaborationTimeoutConfigPanel` heading after global admin login | PASS |

## Screenshot inventory (9)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-dashboard-tasks-redbc-1440x900.png` | `/dashboard#tasks-section` — tester My Tasks hub, TEST queue to-do with template + group | REDBC |
| 2 | `screenshots/02-dashboard-tasks-greenbc-1440x900.png` | Same dashboard task hub after brand switch | GREENBC |
| 3 | `screenshots/03-template-lifecycle-tab-greenbc-1440x900.png` | Template detail lifecycle tab opened from a dashboard to-do | GREENBC |
| 4 | `screenshots/04-template-lifecycle-tab-redbc-1440x900.png` | Same lifecycle tab | REDBC |
| 5 | `screenshots/05-dashboard-approver-tasks-redbc-1440x900.png` | `/dashboard#tasks-section` — approver APPROVAL queue to-do | REDBC |
| 6 | `screenshots/06-dashboard-escalation-tasks-redbc-1440x900.png` | `/dashboard#tasks-section` — group-admin escalation to-do | REDBC |
| 7 | `screenshots/07-dashboard-escalation-tasks-greenbc-1440x900.png` | Same escalation task hub after brand switch | GREENBC |
| 8 | `screenshots/08-dashboard-timeout-config-panel-greenbc-1440x900.png` | Dashboard `CollaborationTimeoutConfigPanel` — global scope, threshold grid | GREENBC |
| 9 | `screenshots/09-dashboard-timeout-config-panel-redbc-1440x900.png` | Same timeout config panel after brand switch | REDBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01–07 |
| White baseline, professional palette | ✅ | All screenshots |
| Dual-brand REDBC / GREENBC theming | ✅ | 01 vs 02; 06 vs 07; 08 vs 09 |
| Logo switches via shared slot (no page-local branding) | ✅ | 01 vs 02; 06 vs 07 |
| Data tables: headers, density, filters, pagination | ✅ | 01, 05, 06 — column filters, sort icons, pagination footer |
| Forms: label alignment, action hierarchy | ✅ | 08, 09 — scope radios, threshold grid, Refresh secondary / Save primary |
| Unified task hub for all roles (no per-role workbench routes) | ✅ | 01 (tester), 05 (approver), 06 (group-admin escalation) on one dashboard |
| Legacy `/workbench/*` URLs redirect to dashboard tasks | ✅ | `collaboration-todos.spec.ts` redirect test PASS |
| No text overflow / overlap at 1440×900 | ✅ | Visual review — long template names ellipsize cleanly; no clipped controls |
| English-first copy via i18n | ✅ | Page titles, column headers, timeout labels all English |
| Permission-aware surfaces (role-specific to-do visibility) | ✅ | Tester / approver / group-admin see their own queue rows on the shared hub |
| Interaction states (primary/secondary buttons, table rows) | ✅ | Save thresholds primary CTA; Refresh outline; clickable to-do rows route to lifecycle |
| Empty / loading / error completeness | ✅ (partial) | Populated states captured; skeleton-cleared asserts in spec; empty states not shot (see suggestions) |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Sidebar active state tint on GREENBC** — On GREENBC screenshots (02, 07), the active
   dashboard nav item still uses a red-tinted highlight rather than brand green. Primary
   actions follow brand correctly; nav active state should use `--brand-primary` tokens for
   full dual-brand parity. _Rule: dual-brand theming._

2. **Task hub noise from accumulated E2E fixtures** — The dashboard task list shows rows
   from prior runs; functional but noisy for demo/review. Consider periodic E2E cleanup or
   isolated fixture templates. _Rule: data tables — sensible density / professional OA presentation._

3. **Timeout panel `lastUpdated` formatting** — `09-dashboard-timeout-config-panel-redbc-1440x900.png`
   displays a raw ISO timestamp instead of a locale-aware formatted date/time.
   _Rule: professional OA presentation._

4. **Missing empty-state evidence** — Dashboard task hub empty state (`el-empty`) not
   captured when a role's queue has zero items. _Rule: state completeness._

### 🟢 Nice to have

1. Dedicated brand-header crop screenshots (as in P14-T01) for dashboard sessions.
2. Capture group-admin dashboard with `scopeGroup` selected to show group-code field layout.
3. Capture approver task hub with many rows to validate pagination sticky-header behavior under load.

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P14-T02-uiux-evidence.spec.ts` | UIUX screenshot capture spec for the dashboard task hub + timeout config |
| `frontend/e2e/helpers/uiux-evidence.ts` | P14-T02 evidence dir + screenshot helpers |
| `frontend/e2e/helpers/collaboration-api.ts` | `APPROVAL` queue support in `seedCollaborationWorkItem` |
| `frontend/e2e/helpers/ui.ts` | Dashboard task-row / filter helpers (`dashboardTaskRow`, `filterDashboardTasksByItem`) |
| `frontend/e2e/collaboration-todos.spec.ts` | Functional baseline: dashboard to-dos + legacy workbench redirect |
| `frontend/e2e/a11y-smoke.spec.ts` | Tester dashboard task hub h1 + timeout panel heading smoke |
| `frontend/e2e/evidence/P14-T02/screenshots/*.png` | 9 viewport screenshots (REDBC/GREENBC) |

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/collaboration-todos.spec.ts`
