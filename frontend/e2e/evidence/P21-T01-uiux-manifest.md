# P21-T01 UIUX Evidence Manifest

**Task:** Role-journey frontend redesign — behavior nav (`My to-dos` group), L1 terminology, dashboard queue deep-links  
**Reviewer:** e2e-uiux-reviewer (evidence captured via `P21-T01-uiux-evidence.spec.ts`)  
**Date:** 2026-06-29  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Local Vite dev `http://127.0.0.1:5173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P21-T01-uiux-evidence.spec.ts --workers=1` | **1/1 passed** (32.6s) |
| `pnpm exec playwright test e2e/P21-T01-behavior-nav.spec.ts --workers=1` | **7/7 passed** (functional baseline) |
| `pnpm exec playwright test e2e/a11y-smoke.spec.ts e2e/collaboration-todos.spec.ts --workers=1` | **9/9 passed** (copy sync regression) |

## Screenshot inventory (8)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `P21-T01/screenshots/01-tester-sidebar-my-todos-redbc-1440x900.png` | TESTER sidebar — `My to-dos` group with single testing entry | REDBC |
| 2 | `P21-T01/screenshots/02-dashboard-queue-test-landing-redbc-1440x900.png` | Dashboard landing after `?queue=TEST#tasks-section` deep-link | REDBC |
| 3 | `P21-T01/screenshots/03-dashboard-queue-test-landing-greenbc-1440x900.png` | Same queue landing after brand switch | GREENBC |
| 4 | `P21-T01/screenshots/04-group-admin-sidebar-six-entries-redbc-1440x900.png` | GROUP_ADMIN — six behavior entries under `My to-dos` | REDBC |
| 5 | `P21-T01/screenshots/05-global-admin-sidebar-six-entries-redbc-1440x900.png` | GLOBAL_ADMIN — six behavior entries under `My to-dos` | REDBC |
| 6 | `P21-T01/screenshots/06-audit-admin-no-my-todos-group-redbc-1440x900.png` | AUDIT_ADMIN — no `My to-dos` group; Activity log visible | REDBC |
| 7 | `P21-T01/screenshots/07-brand-header-redbc-1440x900.png` | Shell brand slot | REDBC |
| 8 | `P21-T01/screenshots/08-brand-header-greenbc-1440x900.png` | Shell brand slot | GREENBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01–06 |
| Dual-brand REDBC / GREENBC | ✅ | 02 vs 03; 07 vs 08 |
| Behavior nav `My to-dos` group | ✅ | 01, 04, 05 |
| Role-scoped behavior entries (tester = 1, admin = 6) | ✅ | 01 vs 04/05 |
| AUDIT_ADMIN group hidden (not disabled) | ✅ | 06 |
| Dashboard `?queue=TEST` deep-link landing | ✅ | 02, 03 |
| L1 terminology (`My to-dos`, `Reminder timing`) | ✅ | Functional specs + 02 |
| English-first i18n | ✅ | All surfaces use `nav.*` / `dashboard.*` keys |
| No text overflow / overlap at 1440×900 | ✅ | Visual review — sidebar and dashboard fit bounds |
| Nav `:focus-visible` ring | ✅ | **Fixed** in `ManagementShell.vue` (2026-06-29) |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **GREENBC nav active tint** — Same cross-phase note as P14-T03/P18-T10; sidebar active highlight may remain red-tinted on GREENBC. _Rule: dual-brand theming._ **Defer:** existing platform-wide theme follow-up.

2. **Docker E2E stack stale copy** — Docker frontend (`:4173`) may serve a pre-P21 build until redeployed; local dev + backend is the authoritative gate for this slice.

### 🟢 Nice to have

1. Focus-visible screenshot with keyboard tab-through on behavior nav items.
2. `Reminder timing` panel screenshot for GLOBAL_ADMIN (covered functionally in `collaboration-todos.spec.ts`).

## Related specs

- Functional: `frontend/e2e/P21-T01-behavior-nav.spec.ts`
- UIUX capture: `frontend/e2e/P21-T01-uiux-evidence.spec.ts`
- Copy regression: `frontend/e2e/a11y-smoke.spec.ts`, `frontend/e2e/collaboration-todos.spec.ts`
- Helpers: `frontend/e2e/helpers/nav.ts`, `frontend/e2e/helpers/uiux-evidence.ts`
