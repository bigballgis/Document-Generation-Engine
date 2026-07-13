# LRP-C3 UIUX Evidence Manifest

**Task:** LR-C3 / TaskMaster #30 — structure-level undo/redo toolbar + editor chrome  
**Slice:** `lrp-c3-editor-undo-redo` (`feat/lrp-c3-editor-undo-redo`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first, `LRP_C3_VIEWPORT`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (UP; no redeploy)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on undo/redo History toolbar / editor chrome)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C3-undo-redo.spec.ts --config playwright.docker.config.ts --workers=1` | **7/7 passed** (upstream; frames under `LRP-C3-undo-redo/`) |
| Stage 7: `pnpm -C frontend exec playwright test e2e/LRP-C3-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** — 14 dual-brand screenshots |
| `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **9/9 passed** |

Review method: Playwright dual-brand capture at 1440×900 with `scrollIntoViewIfNeeded` on `.toolbar`; `switchBrand` REDBC↔GREENBC; visual inspection of on-disk PNGs; static cross-check of `ControlledStructuredContentEditor.vue` History group (`role="toolbar"`, `aria-label`/`title`, tokenized SCSS); English-first + zh-CN i18n for undo/redo tooltips; BDD C3-C14 toolbar contract.

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Empty history → Undo/Redo disabled | History toolbar + OA shell | 01–03 (REDBC), 08–10 (GREENBC) |
| After 2 inserts → Undo on / Redo off | History close-up | 04 (REDBC), 11 (GREENBC) |
| After Undo → both enabled + focus | History close-up | 05–06 (REDBC), 12–13 (GREENBC) |
| Editor chrome in binding workspace | Full page @1440×900 | 07 (REDBC), 14 (GREENBC) |
| Upstream functional states (disabled / cycle / truncate / save / restore / discard) | Stage 6 crops | `LRP-C3-undo-redo/01–06` |

## Screenshot inventory (Stage 7 — 14)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C3/screenshots/01-history-disabled-redbc-en-1440x900.png` | Binding editor — History Undo/Redo disabled in OA shell | REDBC | en |
| 2 | `LRP-C3/screenshots/02-history-disabled-closeup-redbc-en.png` | Toolbar close-up — History disabled | REDBC | en |
| 3 | `LRP-C3/screenshots/03-brand-header-redbc-en.png` | REDBC header logo / wordmark | REDBC | en |
| 4 | `LRP-C3/screenshots/04-undo-enabled-redo-disabled-closeup-redbc-en.png` | Undo enabled, Redo disabled | REDBC | en |
| 5 | `LRP-C3/screenshots/05-undo-redo-enabled-closeup-redbc-en.png` | Both enabled after Undo | REDBC | en |
| 6 | `LRP-C3/screenshots/06-undo-focus-redbc-en.png` | Undo focused (EP default blue active) | REDBC | en |
| 7 | `LRP-C3/screenshots/07-editor-history-redbc-en-1440x900.png` | Editor + History after edits | REDBC | en |
| 8 | `LRP-C3/screenshots/08-history-disabled-greenbc-en-1440x900.png` | Same disabled History under GREENBC | GREENBC | en |
| 9 | `LRP-C3/screenshots/09-history-disabled-closeup-greenbc-en.png` | Toolbar close-up — History disabled | GREENBC | en |
| 10 | `LRP-C3/screenshots/10-brand-header-greenbc-en.png` | GREENBC header logo / wordmark | GREENBC | en |
| 11 | `LRP-C3/screenshots/11-undo-enabled-redo-disabled-closeup-greenbc-en.png` | Undo enabled, Redo disabled | GREENBC | en |
| 12 | `LRP-C3/screenshots/12-undo-redo-enabled-closeup-greenbc-en.png` | Both enabled after Undo | GREENBC | en |
| 13 | `LRP-C3/screenshots/13-undo-focus-greenbc-en.png` | Undo focused (EP default blue active) | GREENBC | en |
| 14 | `LRP-C3/screenshots/14-editor-history-greenbc-en-1440x900.png` | Editor + History after edits | GREENBC | en |

Upstream functional frames (Stage 6, not dual-brand): `LRP-C3-undo-redo/01–06`. Prefer Stage 7 frames for brand/shell review.

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| History group first in structured toolbar; labeled `History` | ✅ | Close-ups 02/09; `toolbar.history` i18n |
| Undo/Redo use shared EP `el-button size="small"` (same vocabulary as Blocks/Inline) | ✅ | Close-ups; `ControlledStructuredContentEditor.vue` |
| Disabled vs enabled states visually distinct | ✅ | 02 vs 04/05; 09 vs 11/12 |
| Dual-brand shell primary (Save / nav / Refresh) REDBC red / GREENBC teal | ✅ | Full-page 01 vs 08; headers 03 vs 10 |
| Dual-brand logo switch | ✅ | 03 vs 10 |
| History buttons remain neutral secondary (not brand-fill) — correct hierarchy | ✅ | Close-ups; primary remains Save |
| `role="toolbar"` + `aria-label` + per-button `aria-label` + `title` tooltips | ✅ | Spec asserts + unit BDD-LRP-C3-005 |
| English-first tooltips include keyboard chords | ✅ | `en.ts` / `zh-CN.ts` |
| Tokenized chrome (`--border-color`, `--surface-muted`, `--text-muted`, `--radius-md`) | ✅ | Scoped SCSS `.toolbar` / `.group-label` |
| Readonly hides entire toolbar (incl. History) | ✅ | Unit BDD-LRP-C3-013 |
| No text overflow / clipping / overlap @1440×900 | ✅ | Full-page 01/07/08/14 |
| Contained workspace (detail/editor, not fluid catalog) | ✅ | Full-page frames |
| a11y smoke green | ✅ | a11y-smoke **9/9** |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Undo `:focus` uses Element Plus default blue active fill, not brand `--focus-ring-*`** (frames 06/13) — keyboard focus is visible, but ring/fill does not follow REDBC `#DB0011` / GREENBC `#00847F` tokens introduced in LR-C12. Optional: bridge EP default buttons in the structured toolbar to the same focus-ring treatment as shell/nav.  
   Rule: frontend-oa-design §Quality bar (visible focus); dual-brand focus color.

2. **Stage 6 frame `04-after-save-empty-history.png` cuts off the History toolbar** (below fold) — functional assertions still pass; Stage 7 scrolls `.toolbar` into view. Optionally align Stage 6 `captureEvidence` with `scrollIntoViewIfNeeded`.  
   Rule: e2e-frontend-testing evidence capture (same class of note as LRP-C2).

3. **Native `title` tooltips only for shortcut hints** — `aria-label` covers the control name; chord hint is hover-only. Optional: `aria-keyshortcuts` or a short visible helper. BDD C3-C14 satisfied as-is.  
   Rule: frontend-oa-design §Quality bar (accessible labels).

### 🟢 Nice to have

1. Capture zh-CN History / 撤销·重做 frames for locale layout parity.
2. Optional icon+text on Undo/Redo for faster scan (keep text labels for OA clarity).

## Files added for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C3-uiux-evidence.spec.ts` | Dual-brand evidence capture |
| `frontend/e2e/helpers/uiux-evidence.ts` | `LRP_C3_*` dirs + capture helpers |
| `frontend/e2e/evidence/LRP-C3/screenshots/01–14` | Screenshot set |
| `frontend/e2e/evidence/LRP-C3-uiux-manifest.md` | This manifest |

## Notes for architecture / doc-sync

- History Undo/Redo are **secondary** toolbar actions (neutral EP buttons) — correct OA hierarchy; brand primary remains Save / Restore draft / nav active.
- Doc-sync should record Stage 7 **PASS_WITH_NOTES**, evidence paths above, Stage 6 LRP-C3-undo-redo **7/7**, a11y-smoke **9/9**.
- No ADR / permission-matrix change required for this UIUX slice.
- Optional follow-up (non-blocking): brand-token focus ring on History buttons; Stage 6 scroll alignment — route to `frontend-engineer` if product wants polish.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/e2e-frontend-testing/SKILL.md`
- Behavior: `docs/behavior/lrp-c3-editor-undo-redo.md` (C3-C13/C14)
- Components: `ControlledStructuredContentEditor.vue`
- Manifest pattern: `frontend/e2e/evidence/LRP-C2-uiux-manifest.md`
