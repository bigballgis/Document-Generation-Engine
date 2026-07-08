# UX-ENTITY-DISPLAY UIUX Evidence Manifest

**Task:** UX entity display rollout — audit activity log + template catalog  
**Reviewer:** e2e-uiux-reviewer (evidence via `UX-ENTITY-DISPLAY-uiux-evidence.spec.ts`)  
**Date:** 2026-07-08  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS**

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/UX-ENTITY-DISPLAY-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (7.2s) |

## Screenshot inventory (5)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `UX-ENTITY-DISPLAY/screenshots/01-activity-log-filters-redbc-1440x900.png` | Activity log — searchable Event type select | REDBC |
| 2 | `UX-ENTITY-DISPLAY/screenshots/02-activity-log-table-redbc-1440x900.png` | Activity log table — readable template names, fluid width | REDBC |
| 3 | `UX-ENTITY-DISPLAY/screenshots/03-templates-list-redbc-1440x900.png` | Templates catalog — EntityLinkCell name links | REDBC |
| 4 | `UX-ENTITY-DISPLAY/screenshots/04-brand-header-greenbc-1440x900.png` | Header after GREENBC switch | GREENBC |
| 5 | `UX-ENTITY-DISPLAY/screenshots/05-templates-list-greenbc-1440x900.png` | Templates catalog after brand switch | GREENBC |

## Entity display checklist

| Item | Status | Evidence |
| --- | --- | --- |
| No raw UUID in entity columns | ✅ | Spec `expectNoRawUuidInEntityCells` |
| Template column uses EntityLinkCell | ✅ | 02, 03 |
| Audit admin: plain text when no route access | ✅ | 02 — `.entity-link-cell__text` |
| Template list: navigable link when permitted | ✅ | 03 — `.entity-link-cell__link` |
| Event type filter = searchable select | ✅ | 01 |
| Catalog fluid layout | ✅ | Spec `expectFluidPageLayout` |
| Dual-brand REDBC / GREENBC | ✅ | 03 vs 05; header 04 |

## Findings

### 🔴 Critical

_None._

### 🟡 Suggestion

_None blocking merge._

## References

- `docs/architecture/ux-entity-display-constitution.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
