# P14-T01 UIUX Evidence Manifest

**Task:** Content module lifecycle (P14-T01) — management UI visual & interaction review  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-06-26  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P14-T01-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | **3/3 passed** (35.0s) |

### Accessibility smoke

| Check | Result |
| --- | --- |
| Login page primary heading + form controls | PASS |
| Content modules list `h1` ("Content modules") after author login | PASS |
| New content module primary action visible | PASS |

## Screenshot inventory (8)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-content-modules-list-redbc-1440x900.png` | `/content-modules` list with RETAIL group filter, table + pagination | REDBC |
| 2 | `screenshots/02-content-modules-list-greenbc-1440x900.png` | Same list after brand switch | GREENBC |
| 3 | `screenshots/03-create-content-module-dialog-1440x900.png` | Create content module dialog (form fields, Cancel / Create module) | REDBC |
| 4 | `screenshots/04-content-module-detail-draft-1440x900.png` | Module detail — draft version 1.0.0, Submit for approval CTA | REDBC |
| 5 | `screenshots/05-template-content-module-references-panel-1440x900.png` | `TemplateContentModuleReferencesPanel` on template Authoring tab | REDBC |
| 6 | `screenshots/06-lifecycle-impact-preview-dialog-1440x900.png` | STOP lifecycle impact preview (referencing templates, remediation) | REDBC |
| 7 | `screenshots/10-brand-header-greenbc-1440x900.png` | Header brand slot — GREENBC logo + wordmark | GREENBC |
| 8 | `screenshots/11-brand-header-redbc-1440x900.png` | Header brand slot — REDBC logo + wordmark | REDBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01, 02, 04 |
| White baseline, professional palette | ✅ | All screenshots |
| Dual-brand REDBC / GREENBC theming | ✅ | 01 vs 02; primary buttons follow brand color |
| Logo switches via shared slot (no page-local branding) | ✅ | 10 vs 11 |
| Data tables: headers, density, pagination | ✅ | 01, 02, 05 |
| Forms/dialogs: label alignment, action hierarchy | ✅ | 03, 06 |
| No text overflow / overlap at 1440×900 | ✅ | Visual review — no clipping observed |
| English-first copy via i18n | ✅ | All visible strings English |
| Permission-aware surfaces (author vs group admin) | ✅ | Author list/create/detail; admin lifecycle dialog |
| Interaction states (primary/secondary buttons, tags) | ✅ | Draft tag, Open lock status, warning banner in impact dialog |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Template references panel table density** — `05-template-content-module-references-panel-1440x900.png` shows multiple accumulated E2E reference rows from prior runs. Consider periodic E2E data cleanup or isolated fixture templates so reviewers see a cleaner default density. _Rule: data tables — sensible density._

2. **Content modules list row volume** — List screenshots show 16 modules (pagination page 1/2) dominated by E2E-prefixed rows. Functional but noisy for demo/review environments. _Rule: professional OA presentation._

### 🟢 Nice to have

1. Capture empty-state screenshot when no group is selected (`ContentModuleListView` group filter placeholder state).
2. Capture GREENBC variant of create dialog and lifecycle impact dialog for full dual-brand dialog parity (list + header already covered).

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P14-T01-uiux-evidence.spec.ts` | UIUX screenshot capture spec |
| `frontend/e2e/helpers/uiux-evidence.ts` | Viewport, brand switch, screenshot helpers |
| `frontend/e2e/helpers/content-modules-api.ts` | `createDraftContentModule` fixture for draft detail shot |
| `frontend/e2e/a11y-smoke.spec.ts` | Content modules `h1` a11y smoke |

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/content-module-lifecycle.spec.ts`
