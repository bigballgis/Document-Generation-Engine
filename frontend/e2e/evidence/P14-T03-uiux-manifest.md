# P14-T03 UIUX Evidence Manifest

**Task:** Template export/import UI (P14-T03) — `TemplateExportActions` on template detail, `TemplateImportDialog` on template list  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-06-27  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/P14-T03-uiux-evidence.spec.ts e2e/P14-T03-template-export-import.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | **7/7 passed** (UIUX evidence 5.9s; functional 7.8s; a11y smoke 1.8s) |

### Accessibility smoke

| Check | Result |
| --- | --- |
| Login page primary heading + form controls | PASS |
| Content modules list `h1` after author login | PASS |
| Tester workbench `h1` after tester login | PASS |
| Dashboard timeout config panel heading after global admin login | PASS |
| Templates list `h1` / import dialog (P14-T03 surfaces) | Not in smoke (see suggestions) |

## Screenshot inventory (9)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `P14-T03/screenshots/01-template-detail-export-redbc-1440x900.png` | Template detail — published template header with **Export bundle** dropdown | REDBC |
| 2 | `P14-T03/screenshots/02-template-detail-export-menu-redbc-1440x900.png` | Export dropdown open — Download JSON / Download ZIP menu items | REDBC |
| 3 | `P14-T03/screenshots/03-template-detail-export-greenbc-1440x900.png` | Same template detail after brand switch — Export bundle + Published badge | GREENBC |
| 4 | `P14-T03/screenshots/04-templates-list-import-redbc-1440x900.png` | Templates list — **Import template** secondary + **New template package** primary | REDBC |
| 5 | `P14-T03/screenshots/05-import-dialog-empty-redbc-1440x900.png` | Import dialog — empty file picker, master select, conflict policy radios | REDBC |
| 6 | `P14-T03/screenshots/06-import-dialog-bundle-loaded-redbc-1440x900.png` | Import dialog — bundle summary card, target master, primary **Import template** | REDBC |
| 7 | `P14-T03/screenshots/07-import-dialog-bundle-loaded-greenbc-1440x900.png` | Same loaded import dialog after brand switch — teal primary CTA | GREENBC |
| 8 | `P14-T03/screenshots/10-brand-header-greenbc-1440x900.png` | Shell header brand slot — GREENBC wordmark + logo | GREENBC |
| 9 | `P14-T03/screenshots/11-brand-header-redbc-1440x900.png` | Shell header brand slot — REDBC wordmark + logo | REDBC |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01–04 |
| White baseline, professional palette | ✅ | All screenshots |
| Dual-brand REDBC / GREENBC theming | ✅ | 01 vs 03; 06 vs 07; 10 vs 11 — primary CTA follows brand |
| Logo switches via shared slot (no page-local branding) | ✅ | 10, 11 |
| Data tables: headers, density, filters | ✅ (partial) | 01 — version lines table with filter row; 04 captured during list skeleton |
| Forms: label alignment, action hierarchy | ✅ | 05–07 — top labels, Cancel secondary / Import primary |
| Dialogs: purposeful, dismissable, no layout shift | ✅ | 05–07 — 560px dialog, bundle summary card, no overlap |
| No text overflow / overlap at 1440×900 | ✅ | UUID in bundle summary wraps (`word-break`); long template names ellipsize in table context |
| English-first copy via i18n | ✅ | Export bundle, Import template, conflict policy strings all English |
| Permission-aware surfaces | ✅ | Export visible on published template for group admin; Import on list for export capability |
| Interaction states (dropdown, loading export) | ✅ (partial) | 02 export menu; export loading state not captured |
| Empty / loading / error completeness | ✅ (partial) | Empty import 05; parse-error / API-error states not captured |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Sidebar active tint on GREENBC** — Screenshot 03 shows Templates nav highlight still red-tinted after GREENBC switch; primary Export/header follow brand but nav active state should use `--brand-primary`. _Rule: dual-brand theming._

2. **Templates list captured during skeleton load** — Screenshot 04 shows placeholder skeleton rows instead of populated table. Re-capture after `el-skeleton` clears for professional OA presentation. _Rule: data tables — loading/empty states._

3. **Conflict-policy radio uses Element Plus default blue** — On GREENBC import dialog (07), selected radio dot is blue while primary button is teal. Prefer brand token for selected radio accent. _Rule: dual-brand theming / component polish._

4. **Extend a11y smoke for templates surfaces** — Add templates list `h1` and import dialog title to `a11y-smoke.spec.ts` for regression coverage of P14-T03 headings. _Rule: accessibility smoke._

5. **Import dialog dismiss via Cancel** — E2E automation required navigation away to dismiss dialog before brand switch; verify Cancel / header close reliably closes dialog for keyboard users. _Rule: dialogs — dismissable._

### 🟢 Nice to have

1. Templates list GREENBC screenshot with populated rows.
2. Import dialog parse-error state (`el-alert`) and API error banner evidence.
3. Export button loading state during JSON/ZIP download.
4. Export-not-eligible hint on non-published template detail.

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P14-T03-uiux-evidence.spec.ts` | UIUX screenshot capture spec for export/import surfaces |
| `frontend/e2e/helpers/uiux-evidence.ts` | P14-T03 evidence dir + screenshot helpers |
| `frontend/e2e/evidence/P14-T03/screenshots/*.png` | 9 viewport screenshots (REDBC/GREENBC) |
| `frontend/e2e/evidence/P14-T03-uiux-manifest.md` | This manifest |

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/P14-T03-template-export-import.spec.ts`
