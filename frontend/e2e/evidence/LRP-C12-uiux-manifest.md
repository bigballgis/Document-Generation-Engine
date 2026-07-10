# LRP-C12 UIUX Evidence Manifest

**Task:** LR-C12 / TaskMaster #27 — keyboard a11y (skip-link, focus-ring tokens, activatable table focus)  
**Slice:** `lrp-c12-keyboard-a11y` (`feat/lrp-c12-keyboard-a11y`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (Stage 5 DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX / focus-visibility blockers)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C12-keyboard-journey.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** (re-verified Stage 7) |
| `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **9/9 passed** (incl. LR-C12 axe masters + dashboard critical=0) |
| Stage 7 capture | Dual-brand focus screenshots @1440×900 + computed-style probe (`--focus-ring-*` → brand primary) |

Review method: Playwright keyboard journey + a11y smoke; visual inspection of on-disk dual-brand PNGs; computed-style confirmation that `:focus-visible` outline uses tokenized `--focus-ring-color` / `--focus-ring-width` / `--focus-ring-offset` mapped to `--brand-primary` for REDBC `#DB0011` and GREENBC `#00847F`; static cross-check of `ManagementShell.vue`, `AppDataTable.vue`, `global.scss`.

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Skip-link first Tab / focus reveal + ring | `ManagementShell` `.skip-link` | 01–02 (REDBC + GREENBC) |
| Nav keyboard focus ring | `button.nav-item` | 03–04 (REDBC + GREENBC) |
| Activatable catalog row focus ring | `AppDataTable` `.app-data-table__activatable-row` | 05–06 (REDBC + GREENBC) |
| Dual-brand logo / chrome | `BrandLogo` + header | 07 (REDBC + GREENBC) |

## Screenshot inventory (14)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C12/screenshots/01-skip-link-focus-redbc-en-1440x900.png` | Templates shell — skip-link revealed + red focus ring | REDBC | en |
| 2 | `LRP-C12/screenshots/02-skip-link-closeup-redbc-en.png` | Skip-link close-up (“Skip to main content”, brand text) | REDBC | en |
| 3 | `LRP-C12/screenshots/01-skip-link-focus-greenbc-en-1440x900.png` | Same skip-link after GREENBC switch | GREENBC | en |
| 4 | `LRP-C12/screenshots/02-skip-link-closeup-greenbc-en.png` | Skip-link close-up (teal text) | GREENBC | en |
| 5 | `LRP-C12/screenshots/03-nav-focus-redbc-en-1440x900.png` | Sidebar “My tasks” with red `:focus-visible` ring | REDBC | en |
| 6 | `LRP-C12/screenshots/04-nav-focus-closeup-redbc-en.png` | Nav item close-up | REDBC | en |
| 7 | `LRP-C12/screenshots/03-nav-focus-greenbc-en-1440x900.png` | Sidebar “My tasks” with teal `:focus-visible` ring | GREENBC | en |
| 8 | `LRP-C12/screenshots/04-nav-focus-closeup-greenbc-en.png` | Nav item close-up | GREENBC | en |
| 9 | `LRP-C12/screenshots/05-table-row-focus-redbc-en-1440x900.png` | First activatable Templates row — inset red focus ring | REDBC | en |
| 10 | `LRP-C12/screenshots/06-table-row-focus-closeup-redbc-en.png` | Row focus close-up | REDBC | en |
| 11 | `LRP-C12/screenshots/05-table-row-focus-greenbc-en-1440x900.png` | First activatable row — inset teal focus ring | GREENBC | en |
| 12 | `LRP-C12/screenshots/06-table-row-focus-closeup-greenbc-en.png` | Row focus close-up | GREENBC | en |
| 13 | `LRP-C12/screenshots/07-brand-header-redbc-en.png` | REDBC header logo / wordmark | REDBC | en |
| 14 | `LRP-C12/screenshots/07-brand-header-greenbc-en.png` | GREENBC header logo / wordmark | GREENBC | en |

### Computed-style probe (Stage 7)

| Control | REDBC outline | GREENBC outline | Tokens |
| --- | --- | --- | --- |
| `a.skip-link` | `rgb(219, 0, 17) solid 2px` offset `2px` | `rgb(0, 132, 127) solid 2px` offset `2px` | `--focus-ring-*` → `--brand-primary` |
| `button.nav-item` | same | same | same |
| `tr.app-data-table__activatable-row` | same, offset `-2px` (inset) | same, offset `-2px` | `tabindex="0"`; `:focus-visible` matches |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Skip-link first focusable; English-first `nav.skipToMainContent` | ✅ | Journey Tab→skip; frames 01–02; i18n en/zh-CN |
| Focus rings tokenized (no ad-hoc hex/px on rings) | ✅ | `global.scss` `--focus-ring-color/width/offset`; shell / table / EP bridges |
| Dual-brand focus color (REDBC red / GREENBC teal) | ✅ | Frames 01/03/05/09 vs 01g/03g/05g; computed probe |
| Dual-brand logo switch | ✅ | Frames 13–14 |
| Activatable table row keyboard focus visible + Enter opens detail | ✅ | Frames 09–12; journey 1/1 |
| Nav `:focus-visible` ring | ✅ | Frames 05–08 |
| No overflow / clipping / overlap @1440×900 on focus states | ✅ | Full-page frames 01/03/05 |
| a11y smoke green (critical axe = 0 on covered views) | ✅ | a11y-smoke **9/9** |
| Fluid Templates catalog (no wasted gutters) | ✅ | Full-page frames |
| Entity columns remain human-readable (no raw UUID primary) | ✅ | Template name + external id subtitle on focused row |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **No checked-in `LRP-C12-uiux-evidence.spec.ts`** — Stage 7 used an ad-hoc capture script against the live Docker stack. Prefer a durable evidence spec (C9 pattern) under `frontend/e2e/` with `uiux-evidence.ts` helpers (`switchBrand`, 1440×900 dirs) so dual-brand focus frames are reproducible in CI.  
   Rule: e2e-uiux-reviewer evidence machinery / frontend-oa-design §Definition of done.

2. **Locator close-ups can clip `outline`** — CSS `outline` paints outside the box; tight `locator.screenshot()` crops (esp. nav close-ups) under-represent the ring vs full-page frames. Prefer full-page or padded clip for focus evidence.  
   Rule: evidence quality (not a product defect).

### 🟢 Nice to have

1. zh-CN skip-link frame (`nav.skipToMainContent` →「跳到主要内容」) for locale layout parity.
2. Optional keyboard Tab-order golden (skip → chrome → nav → filters → table) beyond the journey’s skip→row→Enter path.
3. Delete or promote any temporary capture scripts after a formal evidence spec lands.

## Files for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C12-keyboard-journey.spec.ts` | Functional keyboard journey (Stage 6) |
| `frontend/e2e/a11y-smoke.spec.ts` | Shell smoke + LR-C12 axe (masters / dashboard) |
| `frontend/e2e/evidence/LRP-C12/screenshots/01–07` | Dual-brand focus / brand chrome frames |
| `frontend/e2e/evidence/LRP-C12/capture-report.json` | Computed-style probe dump |
| `frontend/e2e/evidence/LRP-C12-uiux-manifest.md` | This manifest |
| `frontend/src/components/layout/ManagementShell.vue` | Skip-link + nav / shell focus styles |
| `frontend/src/components/common/AppDataTable.vue` | Activatable row `tabindex` + `:focus-visible` |
| `frontend/src/styles/global.scss` | `--focus-ring-*` tokens + EP focus bridges |

## Notes for architecture / doc-sync

- Stage 7 verdict: **PASS_WITH_NOTES** — ready for architecture-reviewer (stage 8). No merge from this reviewer.
- No ADR / permission-matrix / API contract change required for UIUX presentation.
- Doc-sync (later, on MAIN after merge) should record Stage 7 PASS_WITH_NOTES, evidence paths above, Stage 6 journey 1/1, and a11y-smoke 9/9.
- Optional follow-up (non-blocking): formal `LRP-C12-uiux-evidence.spec.ts` → route to `frontend-engineer`.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/LRP-C12-keyboard-journey.spec.ts`
- Prior manifest pattern: `frontend/e2e/evidence/LRP-C9-uiux-manifest.md`, `LRP-C10-uiux-manifest.md`
