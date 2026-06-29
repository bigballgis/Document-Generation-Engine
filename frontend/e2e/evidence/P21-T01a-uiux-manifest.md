# P21-T01a UIUX Evidence Manifest

**Task:** Task hub deepening — queue partitions, restored columns, SLA/overdue badges, Open actions, segmented errors  
**Reviewer:** e2e-uiux-reviewer (evidence captured via `P21-T01a-uiux-evidence.spec.ts`)  
**Date:** 2026-06-29  
**Viewport:** 1440×900 (desktop-first, REDBC-primary)  
**Stack:** Local Vite dev `http://127.0.0.1:5173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P21-T01a-uiux-evidence.spec.ts --workers=1` | **1/1 passed** (10.5s) |
| `pnpm exec playwright test e2e/P21-T01a-task-hub.spec.ts --workers=1` | **5/5 passed** |
| `pnpm exec playwright test e2e/P21-T01-behavior-nav.spec.ts e2e/collaboration-todos.spec.ts --workers=1` | **11/11 passed** (regression) |

## Screenshot inventory (7)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `P21-T01a/screenshots/01-unfiltered-hub-partitions-redbc-1440x900.png` | GROUP_ADMIN unfiltered hub — multiple queue partitions | REDBC |
| 2 | `P21-T01a/screenshots/02-queue-test-landing-title-redbc-1440x900.png` | TESTER `?queue=TEST#tasks-section` — dynamic h1 + restored row | REDBC |
| 3 | `P21-T01a/screenshots/03-test-partition-restored-columns-redbc-1440x900.png` | TEST partition close-up — trigger/summary/age/submitter/Open | REDBC |
| 4 | `P21-T01a/screenshots/04-overdue-badge-test-aged-redbc-1440x900.png` | Aged TEST row — **Overdue reminder** badge | REDBC |
| 5 | `P21-T01a/screenshots/05-escalation-partition-overdue-badge-redbc-1440x900.png` | ESCALATION partition — overdue badge always shown | REDBC |
| 6 | `P21-T01a/screenshots/06-open-button-lifecycle-tab-redbc-1440x900.png` | Open → template lifecycle tab | REDBC |
| 7 | `P21-T01a/screenshots/07-collaboration-segmented-error-redbc-1440x900.png` | Collaboration fetch failure — stat cards persist + error panel | REDBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Queue-partitioned task hub (non-mixed table) | ✅ | 01, 03 |
| URL-driven landing title (`?queue=TEST`) | ✅ | 02 |
| Restored columns (trigger/summary/age/submitter) | ✅ | 03 |
| Overdue reminder badge (TEST aged + ESCALATION) | ✅ | 04, 05 |
| Open inline action → lifecycle detail | ✅ | 06 |
| Segmented load/error (shell persists) | ✅ | 07 |
| No text overflow / overlap at 1440×900 | ✅ | Visual review — 03 close-up fits bounds |
| English-first i18n (`Open`, `Overdue reminder`) | ✅ | Functional + screenshot review |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **8-column collaboration table density** — At 1440px the partition table is wide; monitor horizontal scroll on smaller laptops. _Evidence:_ 03 close-up acceptable at 1440×900.

2. **GREENBC nav active tint** — Cross-phase theme note (P14-T03/P18-T10); REDBC-primary evidence sufficient for this slice.

### 🟢 Nice to have

1. GREENBC dual-brand screenshot for queue landing (optional per §12.3 observable list).

## Related specs

- Functional: `frontend/e2e/P21-T01a-task-hub.spec.ts`
- UIUX capture: `frontend/e2e/P21-T01a-uiux-evidence.spec.ts`
- Regression: `frontend/e2e/P21-T01-behavior-nav.spec.ts`, `frontend/e2e/collaboration-todos.spec.ts`
- Helpers: `frontend/e2e/helpers/ui.ts`, `frontend/e2e/helpers/uiux-evidence.ts`
