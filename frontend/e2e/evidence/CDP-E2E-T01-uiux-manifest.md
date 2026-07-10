# CDP-E2E-T01 UIUX Evidence Manifest

**Task:** CD-E2E-T01 / BDD-CDP-MVP-001 — MVP golden-path browser UIUX closeout (dashboard TEST queue → testing decision → approval → publish/go-live → API policy)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-10  
**Viewport (primary):** **1920×1080** (desktop-first; slice-required)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (healthy after routeCapabilities redeploy)  
**Placement:** ISOLATED `d:\working\DGE-cdp-e2e-golden` / `feat/cdp-e2e-golden`  
**Spec:** `frontend/e2e/CDP-E2E-T01-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T01-mvp-golden-path.spec.ts` (Stage 6: **1/1 PASS**)  
**Verdict:** **PASS** (15/15 screenshots; REDBC + GREENBC; no Critical UIUX blockers)

## Capture method

Evidence captured via Playwright Chromium against Docker `:4173` while walking the same browser-only lifecycle as the functional golden path (fresh DRAFT fixture from `prepareCdpMvpGoldenDraft`). Brands switched in-app via `.brand-switcher` (`switchBrand`). Screenshots under `frontend/e2e/evidence/CDP-E2E-T01/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Template Author → Template Tester → Template Author → Template Approver → Group Admin |
| Brands | REDBC + GREENBC |
| Fixture | Fresh `E2E-CDP-MVP-GOLDEN-*` DRAFT → Live (Release 1.0.0) |
| Upstream production fixes in scope | `routeCapabilities.ts` (tester/approver/publisher templateManagement); `useTemplateDetailVisibility.ts` (dev workspace for decision roles) |

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/CDP-E2E-T01-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (~28.5s) |
| Functional baseline `CDP-E2E-T01-mvp-golden-path.spec.ts` | **1/1 PASS** (Stage 6 handoff) |
| `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **3/7 passed** — failures are **env/seed** (FOL catalog empty / lifecycle panel fixture), **not** CDP golden-path surfaces |

## Screenshot inventory (15)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T01/screenshots/01-author-testing-workspace-redbc-1920x1080.png` | Author dev workspace — Template testing tab, data sets, Full test / Submit for testing action rail | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T01/screenshots/02-dashboard-test-queue-redbc-1920x1080.png` | Tester `/dashboard?queue=TEST` — Waiting on my testing / In testing row + Open | REDBC | 1920×1080 |
| 3 | `CDP-E2E-T01/screenshots/03-brand-header-redbc-1920x1080.png` | Shell brand lockup — Red Bank | REDBC | crop |
| 4 | `CDP-E2E-T01/screenshots/04-dashboard-test-queue-greenbc-1920x1080.png` | Same TEST queue after brand switch | GREENBC | 1920×1080 |
| 5 | `CDP-E2E-T01/screenshots/05-brand-header-greenbc-1920x1080.png` | Shell brand lockup — Green Bank | GREENBC | crop |
| 6 | `CDP-E2E-T01/screenshots/06-testing-decision-dialog-redbc-1920x1080.png` | Confirm test pass dialog over testing workspace (checklist + Submit decision) | REDBC | 1920×1080 |
| 7 | `CDP-E2E-T01/screenshots/07-approval-workspace-pending-submit-redbc-1920x1080.png` | Author approval workspace after test pass (PENDING_SUBMIT) | REDBC | 1920×1080 |
| 8 | `CDP-E2E-T01/screenshots/08-approval-workspace-pending-decision-redbc-1920x1080.png` | Approver approval workspace — Approve / Reject action rail (PENDING_DECISION) | REDBC | 1920×1080 |
| 9 | `CDP-E2E-T01/screenshots/09-approval-decision-dialog-redbc-1920x1080.png` | Confirm approval dialog — rationale + evidence confirm | REDBC | 1920×1080 |
| 10 | `CDP-E2E-T01/screenshots/10-approval-workspace-pending-decision-greenbc-1920x1080.png` | Same approval workspace after brand switch | GREENBC | 1920×1080 |
| 11 | `CDP-E2E-T01/screenshots/11-publish-go-live-workspace-redbc-1920x1080.png` | Group Admin approval workspace — Confirm go-live (PENDING_RELEASE) | REDBC | 1920×1080 |
| 12 | `CDP-E2E-T01/screenshots/12-go-live-summary-dialog-redbc-1920x1080.png` | Go-live summary dialog (checklist + Confirm go-live) | REDBC | 1920×1080 |
| 13 | `CDP-E2E-T01/screenshots/13-api-policy-hub-redbc-1920x1080.png` | Hub External access after Live — route summary + generate paths | REDBC | 1920×1080 |
| 14 | `CDP-E2E-T01/screenshots/14-api-policy-retention-saved-redbc-1920x1080.png` | Retention domain saved — success banner (`retention-save-success`) | REDBC | 1920×1080 |
| 15 | `CDP-E2E-T01/screenshots/15-api-policy-hub-greenbc-1920x1080.png` | External access hub after brand switch (Live) | GREENBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01–02, 04, 07–08, 11, 13, 15 |
| Dual-brand REDBC / GREENBC | **PASS** | 02 vs 04; 03 vs 05; 08 vs 10; 13 vs 15 — primary accents follow brand |
| Logo / brand lockup switch | **PASS** | 03 Red Bank; 05 Green Bank |
| Workspace tab shell + single action rail | **PASS** | 01 (Full test / Submit); 06/08 (Confirm test pass / Approve·Reject); 11 (Confirm go-live) |
| Dashboard TEST queue (tester Open deep-link) | **PASS** | 02, 04 — human-readable item name; Open control; no raw UUID primary cell |
| Testing decision dialog | **PASS** | 06 — checklist + Cancel / Submit decision |
| Approval decision dialog | **PASS** | 09 — rationale + Cancel / Submit decision |
| Publish / go-live summary | **PASS** | 11–12 — Confirm go-live + summary checklist |
| API policy / External access | **PASS** | 13–15 — Live badge; routes; retention save success |
| English-first i18n | **PASS** | All captured surfaces EN |
| No text overflow / overlap at 1920 | **PASS** | Visual review of 01–02, 04, 06–15 |
| Tokens / no brand wash | **PASS** | Brand color on primary actions / active nav; white OA baseline |
| Entity display (no UUID primary) | **PASS** | Dashboard Item / template titles use display names; external IDs secondary |
| A11y spot check (golden-path surfaces) | **PASS** | Dialogs expose headings/roles; primary actions labeled; focusable Cancel/Submit |
| Full `a11y-smoke.spec.ts` suite | **N/A (env)** | 3/7 passed; failures FOL seed / lifecycle fixture — outside CDP golden-path UX |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **GREENBC nav active tint** — Sidebar “Templates” active highlight can remain red-tinted on GREENBC (frame 10) while header/logo correctly switch to teal. Primary CTAs on approval rail are semantic green/red (Approve/Reject), not brand-primary. _Rule: dual-brand theming. Cross-phase note (same as F7/P12 manifests)._ Evidence: `10-approval-workspace-pending-decision-greenbc-1920x1080.png`.

2. **PENDING_RELEASE approval sub-tab copy** — While awaiting go-live, “Submit for approval” sub-tab still shows placeholder “checks appear here when this version is ready to enter approval” (frames 11–12). Functional go-live CTA is correct on the action rail; consider routing default sub-tab to Publish readiness for PENDING_RELEASE. _Rule: workspace tab shell / journey clarity. Non-blocking._

### 🟢 Nice to have

1. Capture retention domain scrolled into view with GREENBC primary Save button for dual-brand form polish.
2. Focus-visible ring evidence on Confirm test pass / Approve dialogs.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + ≥8 screenshots at 1920 | **PASS** (15) |
| Dual-brand REDBC + GREENBC | **PASS** |
| Golden-path surfaces (queue → decision → approval → go-live → API policy) | **PASS** |
| Critical UIUX / a11y blockers on CDP surfaces | **None** |
| **Overall stage 7** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T01-mvp-golden-path.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T01-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/cdp-mvp-golden-api.ts`, `lifecycle-ui.ts`, `uiux-evidence.ts` (CDP_E2E_T01_*)
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
