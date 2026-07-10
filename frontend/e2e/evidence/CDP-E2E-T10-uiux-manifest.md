# CDP-E2E-T10 UIUX Evidence Manifest

**Task:** CD-E2E-T10 / BDD-CDP-FID-001…004 — Fidelity viewed confirmation on Pass / Approve / Publish  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 fidelity gate)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (healthz/4173 **200**)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t10-fidelity` / `feat/cdp-e2e-t10-fidelity`  
**Spec:** `frontend/e2e/CDP-E2E-T10-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T10-fidelity-viewed.spec.ts` (Stage 6 — 6/6)  
**Verdict:** **PASS** (6/6 screenshots; REDBC @1920 Pass/Approve/Publish fidelity checkbox + disabled primary; bank OA OK; no Critical UIUX blockers)

## Capture method

TEMPLATE_TESTER / TEMPLATE_APPROVER / GROUP_ADMIN → Dashboard Open → lifecycle dialog with `data-testid="confirm-fidelity-viewed"` left unchecked while other required fields/checks satisfied → primary action remains disabled. Brand: REDBC via `switchBrand`. Screenshots under `frontend/e2e/evidence/CDP-E2E-T10/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Template Tester (`10000006`), Template Approver, Group Admin |
| Brands | REDBC (required); GREENBC optional / deferred to T12 |
| Fixtures | `prepareRetailTemplateInTesting`, `prepareTemplatePendingApprovalDecision`, `prepareTemplatePendingRelease` |
| Surfaces in scope | Confirm test pass / Confirm approval / Go-live summary dialogs |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `… CDP-E2E-T10-fidelity-viewed.spec.ts --config playwright.docker.config.ts --workers=1` | **6/6 passed** (upstream) |
| Stage 7: `pnpm -C frontend exec playwright test e2e/CDP-E2E-T10-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **3/3 passed** (~33.3s) |
| Stage 7 visual review | Screenshots 01–06 inspected @1920 REDBC; stack **200** on `:4173` / `:8080` |

## Screenshot inventory (6)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T10/screenshots/01-pass-dialog-fidelity-unchecked-disabled-submit-redbc-1920x1080.png` | Pass dialog workspace — fidelity unchecked, Submit disabled | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T10/screenshots/02-pass-dialog-fidelity-checkbox-detail-redbc-1920x1080.png` | Pass dialog detail — coverage + preview checked; fidelity unchecked | REDBC | 1920×1080 |
| 3 | `CDP-E2E-T10/screenshots/03-approve-dialog-fidelity-unchecked-disabled-submit-redbc-1920x1080.png` | Approve dialog workspace — fidelity unchecked, Submit disabled | REDBC | 1920×1080 |
| 4 | `CDP-E2E-T10/screenshots/04-approve-dialog-fidelity-checkbox-detail-redbc-1920x1080.png` | Approve dialog detail — rationale + key evidence; fidelity unchecked | REDBC | 1920×1080 |
| 5 | `CDP-E2E-T10/screenshots/05-publish-dialog-fidelity-unchecked-disabled-confirm-redbc-1920x1080.png` | Publish Go-live summary workspace — fidelity unchecked, Confirm disabled | REDBC | 1920×1080 |
| 6 | `CDP-E2E-T10/screenshots/06-publish-dialog-fidelity-checkbox-detail-redbc-1920x1080.png` | Publish dialog detail — checklist + fidelity checkbox + disabled Confirm | REDBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01/03/05 — Red Bank logo + brand switcher + Templates nav + contained workspace |
| Fidelity viewed checkbox (`confirm-fidelity-viewed`) | **PASS** | 02/04/06 — EN label “I reviewed fidelity warnings for the latest preview.” |
| Fail-closed primary (disabled without fidelity) | **PASS** | 02 Pass Submit; 04 Approve Submit; 06 Confirm go-live — all disabled with fidelity unchecked |
| Workspace tab shell + dialog pattern | **PASS** | Dialogs for supplemental confirmation; action rail on tab row (Confirm test pass / Approve / Confirm go-live) |
| English-first i18n | **PASS** | Dialog titles, checkbox copy, Cancel / Submit / Confirm go-live EN |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap on dialogs, checklist, or footer actions |
| Density / spacing rhythm | **PASS** | Moderate OA density; clear hierarchy; white baseline |
| Tokens / no brand wash | **PASS** | Brand red on active nav / primary accents; white OA baseline; semantic Ready/Informational pills |
| Entity display (no UUID primary) | **PASS** | Package names human-readable; no raw UUID as primary entity label |
| Dual-brand REDBC / GREENBC | **PASS (REDBC)** | REDBC required and captured; GREENBC optional per T12 dual-brand golden |
| A11y spot check (dialogs + checkboxes) | **PASS** | Dialog titles; labeled checkboxes; Cancel + primary; disabled primary distinguishable; contrast OK on white baseline |

## Stable selectors (functional + UIUX)

| Selector | Purpose |
| --- | --- |
| `data-testid="confirm-fidelity-viewed"` | Fidelity viewed confirmation checkbox (Pass / Approve / Publish) |
| `button` name `/^submit decision$/i` | Pass / Approve primary (disabled until fidelity + other gates) |
| `button` name `/^confirm go-live$/i` | Publish primary (disabled until fidelity) |
| Dialog title `/confirm test pass/i` | Tester Pass dialog |
| Dialog title `/confirm approval/i` | Approver dialog |
| Dialog title `/go-live summary/i` | Team-lead publish summary |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Disabled primary contrast** — Element Plus disabled primary (`Submit decision` / `Confirm go-live`) remains a light brand-tinted blue; distinguishable but closer to enabled than a grey disabled token. Optional: stronger disabled token for fail-closed gates. _Non-blocking._ Components: `TemplateLifecycleDecisionDialog.vue`, `TemplatePublishSummaryDialog.vue`.

2. **Publish checklist length** — Go-live summary (frames 05–06) is tall; fidelity checkbox sits near the footer after a long Informational list. Optional: sticky footer confirmation or collapse Informational rows so the gate is above the fold without scrolling on shorter viewports. _Non-blocking at 1920._

### 🟢 Nice to have

1. GREENBC capture of the three dialogs (optional; deferred to CD-E2E-T12).
2. Focus-visible ring evidence on `confirm-fidelity-viewed` when tabbing into the gate.
3. Optional “checked → enabled” success frame (FID-004 happy path) — functional E2E covers behavior; UIUX scoped to blocked state.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (6) |
| Pass dialog fidelity gate (FID-001) | **PASS** (frames 01–02) |
| Approve dialog fidelity gate (FID-002) | **PASS** (frames 03–04) |
| Publish dialog fidelity gate (FID-003) | **PASS** (frames 05–06) |
| Dual-brand REDBC (GREENBC optional) | **PASS** — REDBC captured; GREENBC N/A for T10 |
| Critical UIUX / a11y blockers on CDP T10 surfaces | **None** |
| **Overall stage 7 (T10)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T10-fidelity-viewed.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T10-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/lifecycle-ui.ts`, `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`, task id `CDP-E2E-T10`)
- BDD: `docs/behavior/fidelity-viewed-confirmation-journey.md`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
