# F7 UIUX Evidence Manifest

**Task:** CORE-FORTRESS F7-T10 — Authoring UX (dirty guard + side-by-side preview)  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-07-09  
**Viewport:** 1440×900 desktop-first; narrow 375×812 for stacked layout  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Spec:** `frontend/e2e/F7-uiux-evidence.spec.ts`  
**Verdict:** **PASS** (no critical UIUX blockers for F7 surfaces)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/F7-uiux-evidence.spec.ts --config playwright.docker.config.ts` | **1/1 passed** (~14s) |
| Functional baseline `CORE-FORTRESS-F7-dirty-guard.spec.ts` / `CORE-FORTRESS-F7-side-by-side-preview.spec.ts` | Assumed green from prior F7-T08/T09 (not re-run in this review) |
| `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | **3/7 passed** — failures are **env/seed** (FOL catalog empty, lifecycle panel timeout), **not F7 authoring surfaces** |

## Screenshot inventory (15)

| # | File | View / state | Brand | Viewport |
| --- | --- | --- | --- | --- |
| 1 | `F7/screenshots/01-side-by-side-empty-preview-redbc-1440x900.png` | Side-by-side editor + empty final-chain preview | REDBC | 1440×900 |
| 2 | `F7/screenshots/02-brand-header-redbc-1440x900.png` | Shell brand slot — Red Bank | REDBC | 1440×900 |
| 3 | `F7/screenshots/03-preview-pane-boundary-empty-redbc-1440x900.png` | Preview pane — CD-PIT-08 boundary + empty state | REDBC | 1440×900 |
| 4 | `F7/screenshots/04-dirty-guard-dialog-redbc-1440x900.png` | Unsaved changes dialog (Stay / Discard / Save) | REDBC | 1440×900 |
| 5 | `F7/screenshots/05-preview-stale-badge-redbc-1440x900.png` | Stale badge + Refresh now after structure mutate | REDBC | 1440×900 |
| 6 | `F7/screenshots/06-side-by-side-stale-redbc-1440x900.png` | Full side-by-side with stale preview | REDBC | 1440×900 |
| 7 | `F7/screenshots/07-brand-header-greenbc-1440x900.png` | Shell brand slot — Green Bank | GREENBC | 1440×900 |
| 8 | `F7/screenshots/08-side-by-side-stale-greenbc-1440x900.png` | Side-by-side stale; primary Refresh teal | GREENBC | 1440×900 |
| 9 | `F7/screenshots/09-preview-stale-badge-greenbc-1440x900.png` | Preview pane stale (GREENBC primary) | GREENBC | 1440×900 |
| 10 | `F7/screenshots/10-dirty-guard-dialog-greenbc-1440x900.png` | Dirty guard dialog; Save primary teal | GREENBC | 1440×900 |
| 11 | `F7/screenshots/11-side-by-side-stacked-redbc-375x812.png` | Stacked layout + Hide preview + boundary/stale | REDBC | 375×812 |
| 12 | `F7/screenshots/12-side-by-side-stacked-toggle-redbc-375x812.png` | Preview toggle control (“Hide preview”) | REDBC | 375×812 |
| 13 | `F7/screenshots/13-side-by-side-preview-collapsed-redbc-375x812.png` | Preview collapsed — “Show preview” CTA | REDBC | 375×812 |
| 14 | `F7/screenshots/14-side-by-side-stacked-greenbc-375x812.png` | Stacked + Hide preview; Refresh teal | GREENBC | 375×812 |
| 15 | `F7/screenshots/15-dirty-guard-focus-stay-greenbc-1440x900.png` | Dirty guard open; Stay focus spot-check | GREENBC | 1440×900 |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | **PASS** | 04, 10, 15 |
| Dual-brand REDBC / GREENBC | **PASS** | 02 vs 07; 06 vs 08; 04 vs 10; 11 vs 14 — primary actions follow brand (`#DB0011` / `#00847F`) |
| Logo / brand switch | **PASS** | 02 Red Bank; 07 Green Bank |
| Side-by-side ≥ md (editor + preview) | **PASS** | 01, 06, 08 — no stacked class at 1440 |
| Narrow &lt; md stacked + collapse toggle | **PASS** | 11–14 — Hide/Show preview; boundary visible when expanded |
| Dirty guard dialog hierarchy | **PASS** | 04, 10 — Stay / Discard changes / Save and continue |
| Stale badge + Refresh now CTA | **PASS** | 05, 06, 08, 09, 11, 14 |
| CD-PIT-08 non-authoritative boundary copy | **PASS** | 03, 05, 11, 14 — “guidance only” / “not legal evidence” |
| Empty preview state | **PASS** | 01, 03 — “No preview yet” + refresh CTA |
| English-first i18n | **PASS** | All F7 strings via `common.dirtyGuard.*` / `templates.authoring.*` |
| No text overflow / overlap at 1440×900 | **PASS** | Visual review of 01, 04–06, 08, 10 |
| Tokens / no brand wash | **PASS** | Brand color on primary actions only; white OA baseline |
| A11y spot check (F7 surfaces) | **PASS** | Spec asserts focus on Refresh now + dirty-guard Stay; dialog headings/roles present; toggle has visible label |
| Full `a11y-smoke.spec.ts` suite | **N/A (env)** | Failures tied to FOL seed / lifecycle fixture — outside F7 authoring UX |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Narrow 375px shell density** — At 375px the management sidebar still consumes horizontal space; evidence capture collapses the sidebar for usable authoring screenshots. Consider auto-collapse or overlay drawer on very narrow viewports for authoring workspaces. _Rule: desktop-first responsive; F7-C6 stacked preview is OK, shell chrome is pre-existing._

2. **GREENBC nav active tint** — Same cross-phase note as prior manifests: sidebar active highlight may remain red-tinted on GREENBC in some captures. Primary F7 CTAs correctly use teal. _Rule: dual-brand theming._

### 🟢 Nice to have

1. Capture dirty-guard dialog at 375px (keyboard leave) for mobile polish evidence.
2. Preview pane still surfaces raw Preview ID UUID in metadata (existing `TemplatePreviewPanel`); not an F7 entity-column violation, but could use a truncated/copy pattern later.

## Related

- Behavior: `docs/behavior/core-fortress-f7-authoring-ux.md`
- Plan: `docs/plan/detail/CORE-FORTRESS-f7-authoring-ux.md` (F7-T10)
- Functional E2E: `frontend/e2e/CORE-FORTRESS-F7-dirty-guard.spec.ts`, `frontend/e2e/CORE-FORTRESS-F7-side-by-side-preview.spec.ts`
- UIUX capture: `frontend/e2e/F7-uiux-evidence.spec.ts`
- Helpers: `frontend/e2e/helpers/core-fortress-f7.ts`, `frontend/e2e/helpers/uiux-evidence.ts` (`F7_*`)
- Components: `DirtyGuardConfirmDialog.vue`, `AuthoringSideBySideLayout.vue`, `AuthoringPreviewPane.vue`
