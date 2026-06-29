# P21-T01b UIUX Evidence Manifest

**Task:** RoleJourneyTimeline reusable stepper + dashboard reference integration (§12.4)  
**Reviewer:** e2e-uiux-reviewer (evidence captured via `P21-T01b-uiux-evidence.spec.ts`)  
**Date:** 2026-06-30  
**Viewport:** 1440×900 (desktop-first, REDBC-primary)  
**Stack:** Local Vite dev `http://127.0.0.1:5173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P21-T01b-uiux-evidence.spec.ts --workers=1` | **1/1 passed** (6.1s) |
| `pnpm exec playwright test e2e/P21-T01b-journey-timeline.spec.ts --workers=1` | **3/3 passed** (5.4s) |

## Screenshot inventory (4)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `P21-T01b/screenshots/01-author-journey-six-steps-redbc-1440x900.png` | TEMPLATE_AUTHOR — 6 steps, onboarding guidance, above `#tasks-section` | REDBC |
| 2 | `P21-T01b/screenshots/02-tester-journey-three-steps-redbc-1440x900.png` | TEMPLATE_TESTER — 3 steps | REDBC |
| 3 | `P21-T01b/screenshots/03-approver-no-journey-section-redbc-1440x900.png` | TEMPLATE_APPROVER-only — `#journey-section` absent, task hub intact | REDBC |
| 4 | `P21-T01b/screenshots/04-journey-step-focus-visible-redbc-1440x900.png` | Keyboard focus on step 2 after ArrowRight — visible focus ring | REDBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Journey section above task hub (`#journey-section` → `#tasks-section`) | ✅ | 01 |
| Author 6-step cluster-① catalog (L1 labels, no IT jargon) | ✅ | 01 |
| Tester 3-step catalog | ✅ | 02 |
| Approver-only hides journey (no data leak) | ✅ | 03 |
| Onboarding state (`currentStepIndex=null`, all upcoming) | ✅ | 01, 02 |
| Navigation landmark + guidance | ✅ | 01 |
| Keyboard focus ring (`:focus-visible`, brand token) | ✅ | 04 |
| No text overflow / overlap at 1440×900 | ✅ | Visual review |
| English-first i18n (no hardcoded literals) | ✅ | Functional |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **6-step horizontal wrap** — At 1440px all steps fit; on narrower laptops may wrap to two rows (acceptable; monitor in T03+ detail embed).

2. **GREENBC dual-brand** — Cross-phase theme note; REDBC-primary evidence sufficient for this slice.

### 🟢 Nice to have

1. Master designer 4-step journey screenshot (cluster-① parity; optional).

## Related specs

- Functional: `frontend/e2e/P21-T01b-journey-timeline.spec.ts`
- UIUX capture: `frontend/e2e/P21-T01b-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/uiux-evidence.ts`
- Plan: `docs/plan/detail/P21-role-journey-frontend-redesign.md` §12.4
