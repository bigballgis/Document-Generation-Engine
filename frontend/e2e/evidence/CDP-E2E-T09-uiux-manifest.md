# CDP-E2E-T09 UIUX Evidence Manifest

**Task:** CD-E2E-T09 / BDD-CDP-CMP-001 — Preview vs final structured comparison + warningCode filter  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport (primary):** **1920×1080** (desktop-first; CD-2 compare slice)  
**Stack:** Docker acceptance `http://127.0.0.1:4173` + `http://127.0.0.1:8080` (healthz/4173 **200**)  
**Placement:** ISOLATED `D:/working/DGE-cdp-e2e-t09-compare` / `feat/cdp-e2e-t09-compare`  
**Spec:** `frontend/e2e/CDP-E2E-T09-uiux-evidence.spec.ts`  
**Functional baseline:** `frontend/e2e/CDP-E2E-T09-preview-comparison.spec.ts` (Stage 6)  
**Verdict:** **PASS** (3/3 screenshots; REDBC @1920 comparison panel + warningCode filter; bank OA OK; no Critical UIUX blockers)

## Capture method

TEMPLATE_TESTER → Template testing → Preview runs → Details → `TemplatePreviewPanel` (structured comparison + `FidelityWarningList` warningCode filter). Fixture: image-scaling DRAFT + SUCCEEDED async preview. Brand: REDBC via `switchBrand`. Screenshots under `frontend/e2e/evidence/CDP-E2E-T09/screenshots/`.

| Item | Value |
| --- | --- |
| Roles exercised | Template Tester (`10000006`) |
| Brands | REDBC (required); GREENBC optional / deferred to T12 |
| Fixture | `prepareSucceededPreviewWithComparison` (IMAGE_SCALING binding + final-path preview) |
| Surfaces in scope | Preview runs history + comparison panel + fidelity warning filter |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `… CDP-E2E-T09-preview-comparison.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** |
| Stage 6: `… CDP-E2E-T09-uiux-evidence.spec.ts --workers=1` | **1/1 passed** (~7.7s) |
| Combined functional + UIUX | **2/2 passed** |
| Stage 7 visual review | Screenshots 01–03 inspected @1920 REDBC; stack still **200** on `:4173` / `:8080` |

## Screenshot inventory (3)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `CDP-E2E-T09/screenshots/01-preview-runs-comparison-panel-redbc-1920x1080.png` | Preview runs + comparison panel workspace | REDBC | 1920×1080 |
| 2 | `CDP-E2E-T09/screenshots/02-preview-comparison-panel-detail-redbc-1920x1080.png` | Panel detail (summary + structured table + warnings) | REDBC | 1920×1080 |
| 3 | `CDP-E2E-T09/screenshots/03-fidelity-warning-code-filter-redbc-1920x1080.png` | Fidelity warning list after warningCode filter (`IMAGE`) | REDBC | 1920×1080 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 01 — Red Bank logo + brand switcher + Templates nav + contained workspace |
| Structured comparison panel (summary + locationType/locationRef) | **PASS** | 02 — heading + comparison table (`Component` / `IMAGE_SCALING_ADJUSTED`) + severity badge |
| warningCode filter interaction | **PASS** | 03 — filter value `IMAGE`; matching row retained; footer “1 distinct warning code(s)” |
| Workspace tab shell + Preview runs surface | **PASS** | 01 — Template testing / Preview runs; Details expands panel (not inline form clutter) |
| English-first i18n | **PASS** | Shell + section titles EN (`Structured preview comparison`, `Fidelity warnings`, `Reset filters`) |
| No text overflow / overlap at 1920 | **PASS** | No clipping/overlap on workspace, comparison table, or filter row |
| Density / spacing rhythm | **PASS** | Moderate OA density; clear hierarchy; white baseline; no cramped controls |
| Tokens / no brand wash | **PASS** | Brand red on active nav / Details; white OA baseline; warning orange + viewed blue semantic |
| Entity display (no UUID primary in entity columns) | **PASS** | Dataset / locationRef human-readable codes; Preview ID UUID is technical metadata (acceptable) |
| Dual-brand REDBC / GREENBC | **PASS (REDBC)** | REDBC required and captured; GREENBC optional per BDD-CDP-CMP-001 / T12 owns dual-brand golden |
| A11y spot check (comparison + filter) | **PASS** | Headings present; filter inputs labeled; table headers; Mark viewed link distinguishable; contrast OK on white baseline |

## Stable selectors (functional + UIUX)

| Selector | Purpose |
| --- | --- |
| `.preview-run-history` | Preview runs history table |
| `button` name `/^details$/i` | Open preview detail panel (`templates.previewHistory.viewDetails`) |
| `.preview-panel` | `TemplatePreviewPanel` root |
| heading `/structured preview comparison/i` | Structured comparison section |
| `.comparison-table` | Comparison items table (`locationType` / `locationRef`) |
| `data-testid="fidelity-warning-list"` | Fidelity warning list |
| `data-testid="filter-warning-code"` | warningCode filter input |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Comparison / warning Message cells show raw messageKey** — Frames 02–03 display `generation.warning.fidelity.imageScalingAdjusted` instead of resolved English copy. Prefer `te(key) ? t(key) : summary` (same pattern as `locationLabel`). _Rule: English-first / hierarchy. Non-blocking for T09 (E2E-only; no product code in slice)._ Components: `TemplatePreviewPanel.vue` summary column; `FidelityWarningList.vue` message cell.

2. **Frame 01 comparison body clipped by viewport bottom** — Full-page shot correctly prioritizes Preview runs + panel chrome; detail is covered by frame 02. Optional: scroll-into-view before shot 01 if future reviews want both history + full table in one frame. _Non-blocking._

### 🟢 Nice to have

1. GREENBC capture of comparison panel (optional; deferred to CD-E2E-T12 dual-brand golden / not blocking T09).
2. warningCode filter as select when codes are a closed enum (today free-text matches BDD filter behavior).
3. Focus-visible ring evidence on warningCode input / Reset filters.

## Stage 7 decision

| Gate | Result |
| --- | --- |
| Evidence manifest + screenshots at 1920 | **PASS** (3) |
| Comparison panel + locationType/locationRef | **PASS** (frame 02) |
| warningCode filter evidence | **PASS** (frame 03) |
| Dual-brand REDBC (GREENBC optional) | **PASS** — REDBC captured; GREENBC N/A for T09 (same as T08) |
| Critical UIUX / a11y blockers on CDP T09 surfaces | **None** |
| **Overall stage 7 (T09)** | **PASS** |

## Related

- Functional E2E: `frontend/e2e/CDP-E2E-T09-preview-comparison.spec.ts`
- UIUX capture: `frontend/e2e/CDP-E2E-T09-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/preview-comparison-api.ts`, `uiux-evidence.ts` (`CDP_E2E_CD2_DECISION_*`)
- BDD: `docs/behavior/preview-comparison-journey.md`
- OA design: `.cursor/skills/frontend-oa-design/SKILL.md`
- Entity display: `.cursor/skills/frontend-entity-display/SKILL.md`
