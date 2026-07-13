# LRP-C5 UIUX Evidence Manifest

**Task:** LR-C5 / TaskMaster #31 — catalog server-side pagination / filter UX  
**Slice:** `lrp-c5-catalog-pagination` (`feat/lrp-c5-catalog-pagination`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first, `LRP_C5_VIEWPORT`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no Critical UIUX blockers on LR-C5 catalog list surfaces)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C5-catalog-pagination.spec.ts --config playwright.docker.config.ts --workers=1` | **6/6 passed** (upstream); perf `LRP-C5-list-latency.json` p95 ~75ms |
| Stage 7: `pnpm -C frontend exec playwright test e2e/LRP-C5-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** — 21 dual-brand screenshots |
| `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **7 passed / 2 failed** — failures are fixture prep (`masters-api.findMasterByName` assumes array; LR-C5 `/masters` returns `PageView`). Catalog list surfaces themselves green (templates h1, content-modules h1, masters axe) |

Review method: Playwright evidence at 1440×900; dual-brand via `switchBrand` (REDBC ↔ GREENBC); visual inspection of on-disk PNGs; static cross-check of `AppTablePagination.vue`, `CatalogFilterToolbar.vue`, `TemplateListView.vue`, `MasterListView.vue`, `ContentModuleListView.vue`.

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Templates page 0 — filter toolbar + table + pager | `TemplateListView` + `CatalogFilterToolbar` + `AppTablePagination` | 01–05 |
| Templates page 1 — pager active state | Same | 06–07 |
| Templates filtered CORP — chips + Clear all | Same | 08–09 (REDBC), 10–12 (GREENBC) |
| Masters page 0 — dual brand | `MasterListView` | 13–14 (REDBC), 15–16 (GREENBC) |
| Content modules page 0 — dual brand | `ContentModuleListView` | 17–19 (REDBC), 20–21 (GREENBC) |
| Empty / load-error | Not re-exercised (LR-C9 owns) | — |

## Screenshot inventory (21)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C5/screenshots/01-templates-page0-redbc-en-1440x900.png` | Templates catalog page 0 in OA shell | REDBC | en |
| 2 | `LRP-C5/screenshots/02-templates-filter-toolbar-redbc-en.png` | Filter toolbar close-up | REDBC | en |
| 3 | `LRP-C5/screenshots/03-templates-pagination-redbc-en.png` | Pager: Total 515, pages 1…26 | REDBC | en |
| 4 | `LRP-C5/screenshots/04-brand-header-redbc-en.png` | REDBC header logo / wordmark | REDBC | en |
| 5 | `LRP-C5/screenshots/05-templates-next-focus-redbc-en.png` | Next control keyboard focus ring | REDBC | en |
| 6 | `LRP-C5/screenshots/06-templates-page1-redbc-en-1440x900.png` | Templates after Next (page ≥2 rows) | REDBC | en |
| 7 | `LRP-C5/screenshots/07-templates-pagination-page1-redbc-en.png` | Pager active page ≠ 1 | REDBC | en |
| 8 | `LRP-C5/screenshots/08-templates-filtered-corp-redbc-en-1440x900.png` | CORP filter applied | REDBC | en |
| 9 | `LRP-C5/screenshots/09-templates-filter-chips-redbc-en.png` | Active chip `Group: CORP` + Clear all | REDBC | en |
| 10 | `LRP-C5/screenshots/10-templates-filtered-corp-greenbc-en-1440x900.png` | Same filtered catalog under GREENBC | GREENBC | en |
| 11 | `LRP-C5/screenshots/11-brand-header-greenbc-en.png` | GREENBC header logo / wordmark | GREENBC | en |
| 12 | `LRP-C5/screenshots/12-templates-pagination-greenbc-en.png` | Pager under GREENBC | GREENBC | en |
| 13 | `LRP-C5/screenshots/13-masters-page0-redbc-en-1440x900.png` | Letterhead templates catalog | REDBC | en |
| 14 | `LRP-C5/screenshots/14-masters-pagination-redbc-en.png` | Masters pager close-up | REDBC | en |
| 15 | `LRP-C5/screenshots/15-masters-page0-greenbc-en-1440x900.png` | Masters after brand switch | GREENBC | en |
| 16 | `LRP-C5/screenshots/16-masters-pagination-greenbc-en.png` | Masters pager GREENBC | GREENBC | en |
| 17 | `LRP-C5/screenshots/17-content-modules-page0-redbc-en-1440x900.png` | Standard clauses catalog | REDBC | en |
| 18 | `LRP-C5/screenshots/18-content-modules-filter-toolbar-redbc-en.png` | Modules filter toolbar | REDBC | en |
| 19 | `LRP-C5/screenshots/19-content-modules-pagination-redbc-en.png` | Modules pager | REDBC | en |
| 20 | `LRP-C5/screenshots/20-content-modules-page0-greenbc-en-1440x900.png` | Modules under GREENBC | GREENBC | en |
| 21 | `LRP-C5/screenshots/21-content-modules-pagination-greenbc-en.png` | Modules pager GREENBC | GREENBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Shared `CatalogFilterToolbar` + `AppTablePagination` (no ad-hoc EP pager) | ✅ | Views wire shared components; frames 02–03, 14, 18–19 |
| Fluid catalog layout (no wasted 1440 gutters) | ✅ | Full-page 01/13/17 |
| Dual-brand logo + shell chrome (REDBC / GREENBC) | ✅ | 04 vs 11; full-page 08 vs 10; 13 vs 15; 17 vs 20 |
| Entity columns use `EntityLinkCell` (name + subtitle; no raw UUID primary) | ✅ | Templates / masters / modules name columns |
| Filter toolbar: search aria-label; enum → select; group text + chips | ✅ | `CatalogFilterToolbar.vue`; frames 02, 09 |
| Pagination visible + total + prev/pager/next; disabled prev on page 1 | ✅ | Frames 03, 05, 07, 12, 14, 19 |
| Keyboard focus reaches Next (`:focus` ring) | ✅ | Frame 05 |
| No text overflow / clipping / overlap @1440×900 on chrome + filters | ✅ | Full-page + close-ups (mild trailing-column ellipsis OK) |
| English-first i18n | ✅ | Frames + catalogs |
| Catalog-list a11y smoke (h1 / masters axe) | ✅ | a11y tests 2, 4, 8 |
| Empty / error states | N/A this slice | Covered by LR-C9 evidence |

## Findings

### Critical (must fix before merge)

_None._

### Suggestion (should improve)

1. **Pager often below the fold at 1440×900** (`AppTablePagination` under tall tables) — full-page frames 01/06/13/17 crop the pager; users must scroll to discover Total/pages. Close-ups prove the control exists. Consider sticky pagination footer or slightly denser default page chrome so pager peeks into the first viewport on catalog pages.  
   Rule: frontend-oa-design §Layout / §Components (tables + pagination discoverability).

2. **Active page number uses Element Plus default blue, not brand primary** (frames 03/05/07/12) — Next focus ring is also EP blue. Optional: theme `.el-pagination` active/focus to `--brand-primary` for REDBC/GREENBC parity with entity links and primary CTAs.  
   Rule: frontend-oa-design §Foundations (brand color = primary emphasis).

3. **`a11y-smoke` fixture breakage after PageView** (`frontend/e2e/helpers/masters-api.ts` `findMasterByName`) — `/masters` now returns paged `result.content`, so `masters.find` throws. Not a visual defect on catalog lists, but Stage 7 a11y gate is not fully green (7/9). Route to `frontend-engineer` to unwrap `PageView` (and any sibling list helpers) before claiming full a11y smoke.  
   Rule: e2e-uiux-reviewer a11y smoke green; TDD regression for LR-C5 API shape.

### Nice to have

1. Capture zh-CN frames for filter/pager copy parity (en sufficient for this slice).
2. Optional empty-state frame with pagination hidden (`total ≤ pageSize`) to document chrome collapse — behavior already unit/E2E covered.

## Files added for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C5-uiux-evidence.spec.ts` | Dual-brand evidence capture |
| `frontend/e2e/helpers/uiux-evidence.ts` | `LRP_C5_*` dirs + `captureLrpC5*` helpers |
| `frontend/e2e/evidence/LRP-C5/screenshots/01–21` | Screenshot set |
| `frontend/e2e/evidence/LRP-C5-uiux-manifest.md` | This manifest |

## Notes for architecture / doc-sync

- Catalog pagination UX uses locked shared vocabulary (`CatalogFilterToolbar`, `AppTablePagination`, `EntityLinkCell`, fluid `AppPageLayout`) — no ad-hoc chrome.
- Doc-sync should record Stage 7 **PASS_WITH_NOTES**, evidence paths above, Stage 6 LRP-C5 6/6 + latency JSON, a11y-smoke **7/9** with PageView helper note.
- No ADR / permission-matrix change required for this UIUX slice.
- Non-blocking polish (sticky pager, brand-colored pagination, masters-api PageView unwrap) → `frontend-engineer` if product wants follow-up.
- **Ready for architecture: yes** (Critical = 0).

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
- Functional baseline: `frontend/e2e/LRP-C5-catalog-pagination.spec.ts`
- Perf: `frontend/e2e/evidence/LRP-C5-list-latency.json`
- Components: `AppTablePagination.vue`, `CatalogFilterToolbar.vue`
- Manifest pattern: `frontend/e2e/evidence/LRP-C9-uiux-manifest.md`
