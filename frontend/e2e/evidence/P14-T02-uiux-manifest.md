# P14-T02 UIUX Evidence Manifest

**Task:** Collaboration workbench panels + timeout config (P14-T02) — management UI visual & interaction review  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-06-27  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P14-T02-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | **5/5 passed** (P14-T02 evidence 11.5s; a11y smoke 3.9s) |

### Accessibility smoke

| Check | Result |
| --- | --- |
| Login page primary heading + form controls | PASS |
| Content modules list `h1` after author login | PASS |
| Tester workbench `h1` ("Tester workbench") after tester login | PASS |
| Dashboard `CollaborationTimeoutConfigPanel` heading after global admin login | PASS |

## Screenshot inventory (8)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-tester-workbench-redbc-1440x900.png` | `/workbench/tester` — TEST queue table with template, group, submitter, age, summary | REDBC |
| 2 | `screenshots/02-tester-workbench-greenbc-1440x900.png` | Same tester workbench after brand switch | GREENBC |
| 3 | `screenshots/03-approver-workbench-greenbc-1440x900.png` | `/workbench/approver` — APPROVAL queue rows | GREENBC |
| 4 | `screenshots/04-approver-workbench-redbc-1440x900.png` | Same approver workbench after brand switch | REDBC |
| 5 | `screenshots/05-escalation-workbench-redbc-1440x900.png` | `/workbench/escalation` — timeout escalation summary rows | REDBC |
| 6 | `screenshots/06-escalation-workbench-greenbc-1440x900.png` | Same escalation workbench after brand switch | GREENBC |
| 7 | `screenshots/07-dashboard-timeout-config-panel-greenbc-1440x900.png` | Dashboard `CollaborationTimeoutConfigPanel` — global scope, threshold grid | GREENBC |
| 8 | `screenshots/08-dashboard-timeout-config-panel-redbc-1440x900.png` | Same timeout config panel after brand switch | REDBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01–06 |
| White baseline, professional palette | ✅ | All screenshots |
| Dual-brand REDBC / GREENBC theming | ✅ | Header logo/wordmark + primary Save button follow brand (07 vs 08) |
| Logo switches via shared slot (no page-local branding) | ✅ | 01 vs 02; 05 vs 06 |
| Data tables: headers, density, filters, pagination | ✅ | 01, 05 — column filters, sort icons, pagination footer |
| Forms: label alignment, action hierarchy | ✅ | 07, 08 — scope radios, threshold grid, Refresh secondary / Save primary |
| No text overflow / overlap at 1440×900 | ✅ | Visual review — long template names ellipsize cleanly; no clipped controls |
| English-first copy via i18n | ✅ | Page titles, column headers, timeout labels all English |
| Permission-aware surfaces (role-specific workbenches) | ✅ | Tester / approver / group-admin escalation routes render expected queue data |
| Interaction states (primary/secondary buttons, table rows) | ✅ | Save thresholds primary CTA; Refresh outline; zebra table rows |
| Empty / loading / error completeness | ✅ (partial) | Populated states captured; empty states not shot (see suggestions) |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Sidebar active state tint on GREENBC** — On GREENBC screenshots (02, 06), the active workbench nav item still uses a red-tinted highlight bar/background rather than brand green. Primary actions follow brand correctly; nav active state should use `--brand-primary` tokens for full dual-brand parity. _Rule: dual-brand theming._

2. **Workbench table noise from accumulated E2E fixtures** — Tester (01, 02) shows 13+ rows and escalation (05, 06) shows multiple duplicate escalation summaries from prior runs. Functional but noisy for demo/review. Consider periodic E2E cleanup or isolated fixture templates. _Rule: data tables — sensible density / professional OA presentation._

3. **Timeout panel `lastUpdated` formatting** — `08-dashboard-timeout-config-panel-redbc-1440x900.png` displays raw ISO timestamp (`2026-06-26T18:26:37.636803Z`) instead of a locale-aware formatted date/time. _Rule: professional OA presentation._

4. **Missing empty-state evidence** — Approver/tester/escalation empty states (`el-empty`) not captured when queues have zero items. _Rule: state completeness._

### 🟢 Nice to have

1. Dedicated brand-header crop screenshots (as in P14-T01) for workbench sessions.
2. Capture group-admin dashboard with `scopeGroup` selected to show group-code field layout.
3. Capture approver workbench with many rows to validate pagination sticky-header behavior under load.

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P14-T02-uiux-evidence.spec.ts` | UIUX screenshot capture spec for P14-T02 surfaces |
| `frontend/e2e/helpers/uiux-evidence.ts` | P14-T02 evidence dir + screenshot helpers |
| `frontend/e2e/helpers/collaboration-api.ts` | `APPROVAL` queue support in `seedCollaborationWorkItem` |
| `frontend/e2e/a11y-smoke.spec.ts` | Tester workbench h1 + timeout panel heading smoke |
| `frontend/e2e/evidence/P14-T02/screenshots/*.png` | 8 viewport screenshots (REDBC/GREENBC) |

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/collaboration-todos.spec.ts`
