# CDP-E2E-T04 UIUX Evidence Manifest

**Task:** CD-E2E-T04 / BDD-CDP-APPR-001 — Approver approve decision (APPROVAL queue → Confirm approval)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-10  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 decision slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080`  
**Placement:** ISOLATED `d:\working\DGE-cdp-e2e-cd2-t02` / `feat/cdp-e2e-cd2-t02`  
**Spec:** `frontend/e2e/CDP-E2E-T04-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T04-approver-approve-decision.spec.ts` (Stage 6)  
**Verdict:** **PASS** (3/3 screenshots; REDBC + GREENBC APPROVAL queue; no Critical UIUX blockers)

## Capture method

Evidence captured via Playwright Chromium against Docker `:4173` while walking the approver approve path (fresh PENDING_DECISION fixture from `prepareTemplatePendingApprovalDecision`). Brands switched in-app via `.brand-switcher` (`switchBrand`). Screenshots under `frontend/e2e/evidence/CDP-E2E-T04/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Template Approver |
| Brands | REDBC + GREENBC (queue); REDBC (approve dialog) |
| Fixture | Fresh `E2E CDP T04 UX *` PENDING_DECISION → PENDING_RELEASE after approve |
| Surfaces in scope | Dashboard `?queue=APPROVAL` dual-brand + Confirm approval dialog |

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/CDP-E2E-T04-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (upstream Stage 6/7 handoff) |
| Functional baseline `CDP-E2E-T04-approver-approve-decision.spec.ts` | **PASS** (Stage 6 handoff) |

## Screenshot inventory (3)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T04/screenshots/01-dashboard-approval-queue-redbc-1920x1080.png` | Approver `/dashboard?queue=APPROVAL` — Waiting on my approval / Awaiting approval row + Open | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T04/screenshots/02-dashboard-approval-queue-greenbc-1920x1080.png` | Same APPROVAL queue after brand switch | GREENBC | 1920×1080 |
| 3 | `CDP-E2E-T04/screenshots/03-approve-decision-dialog-redbc-1920x1080.png` | Confirm approval dialog — rationale + evidence confirm + Cancel / Submit decision | REDBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01–03 |
| Dual-brand REDBC / GREENBC | **PASS** | 01 vs 02 — logo, switcher, active tab/nav accents follow brand |
| Logo / brand lockup switch | **PASS** | 01 Red Bank; 02 Green Bank |
| Workspace tab shell + single action rail | **PASS** | 03 — Approve / Reject on approval tab rail; dialog for decision |
| Dashboard APPROVAL queue (approver Open deep-link) | **PASS** | 01–02 — human-readable item name; Open control; no raw UUID primary cell |
| Approval decision dialog | **PASS** | 03 — required rationale + evidence checkbox + Cancel / Submit decision |
| English-first i18n | **PASS** | All captured surfaces EN |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap; Stage/Submitter use intentional ellipsis |
| Tokens / no brand wash | **PASS** | Brand color on primary accents / active nav; white OA baseline |
| Entity display (no UUID primary) | **PASS** | Dashboard Item uses display name |
| A11y spot check (approve surfaces) | **PASS** | Dialog heading/role; labeled rationale; Cancel/Submit; dismiss control |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Dashboard Stage / Submitter ellipsis** — At 1920×1080, Stage and Submitter truncate with ellipsis on the APPROVAL queue (frames 01–02), same pattern as T02 TEST queue. Consider tooltip/`title` on truncated cells. _Rule: text overflow / entity readability. Non-blocking._ Evidence: `01-dashboard-approval-queue-redbc-1920x1080.png`, `02-dashboard-approval-queue-greenbc-1920x1080.png`.

2. **Approve dialog capture state** — Evidence frame shows rationale filled but “I reviewed key evidence…” unchecked (Submit still visible). Capture is pre-submit (Cancel then functional helper completes). Optional: capture a second frame with checkbox checked for ready-to-submit state. _Rule: dialog state completeness. Non-blocking._ Evidence: `03-approve-decision-dialog-redbc-1920x1080.png`.

### 🟢 Nice to have

1. Capture Confirm approval dialog under GREENBC for dual-brand dialog primary CTA polish.
2. Focus-visible ring evidence on rationale field + Submit decision.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (3) |
| Dual-brand REDBC + GREENBC (APPROVAL queue) | **PASS** |
| Approver approve surfaces (queue → approve dialog) | **PASS** |
| Critical UIUX / a11y blockers on CDP T04 surfaces | **None** |
| **Overall stage 7 (T04)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T04-approver-approve-decision.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T04-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/lifecycle-ui.ts`, `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`)
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
