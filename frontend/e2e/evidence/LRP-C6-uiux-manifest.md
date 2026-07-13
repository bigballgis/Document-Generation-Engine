# LRP-C6 UIUX Evidence Manifest — Command palette

**Task:** LR-C6 / TaskMaster #32 — global command palette (Ctrl+K / Cmd+K)  
**Slice:** `lrp-c6-command-palette` (`feat/lrp-c6-command-palette`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first, `LRP_C6_VIEWPORT`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on command palette surfaces)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C6-command-palette.spec.ts --config playwright.docker.config.ts --workers=1` | **8/8 passed** (upstream) |
| Stage 7: `pnpm -C frontend exec playwright test e2e/LRP-C6-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** — 13 dual-brand screenshots |

Review method: Playwright evidence at 1440×900; dual-brand via `switchBrand` (REDBC ↔ GREENBC); visual inspection of on-disk PNGs; static cross-check of `CommandPalette.vue`, `useCommandPalette.ts`, English i18n keys.

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Empty query — routes / PAGES group | `CommandPalette` dialog | 01–03 (REDBC), 09–11 (GREENBC) |
| Search hit DEMO-RET → template results | Templates group + `DEMO-RETAIL-LETTER` | 04–05 (REDBC), 12–13 (GREENBC) |
| Keyboard highlight (`aria-selected` / active wash) | ArrowDown on search results | 06 |
| No-match empty (not LoadErrorPanel) | `command-palette-no-match` | 07–08 |

## Screenshot inventory (13)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C6/screenshots/01-palette-routes-empty-redbc-en-1440x900.png` | Palette open — PAGES routes over dashboard | REDBC | en |
| 2 | `LRP-C6/screenshots/02-palette-dialog-routes-redbc-en.png` | Dialog close-up — routes + hint footer | REDBC | en |
| 3 | `LRP-C6/screenshots/03-brand-header-redbc-en.png` | REDBC header logo / wordmark | REDBC | en |
| 4 | `LRP-C6/screenshots/04-palette-search-demo-ret-redbc-en-1440x900.png` | Search `DEMO-RET` — TEMPLATES hits | REDBC | en |
| 5 | `LRP-C6/screenshots/05-palette-dialog-search-hit-redbc-en.png` | Dialog close-up — template titles + externalId subtitles | REDBC | en |
| 6 | `LRP-C6/screenshots/06-palette-keyboard-highlight-redbc-en.png` | ArrowDown highlight (brand-tint active option) | REDBC | en |
| 7 | `LRP-C6/screenshots/07-palette-no-match-redbc-en-1440x900.png` | No-match empty copy over dashboard | REDBC | en |
| 8 | `LRP-C6/screenshots/08-palette-dialog-no-match-redbc-en.png` | Dialog close-up — “No matching pages or resources.” | REDBC | en |
| 9 | `LRP-C6/screenshots/09-palette-routes-empty-greenbc-en-1440x900.png` | Same routes palette under GREENBC | GREENBC | en |
| 10 | `LRP-C6/screenshots/10-palette-dialog-routes-greenbc-en.png` | Dialog close-up — teal focus ring on input | GREENBC | en |
| 11 | `LRP-C6/screenshots/11-brand-header-greenbc-en.png` | GREENBC header logo / wordmark | GREENBC | en |
| 12 | `LRP-C6/screenshots/12-palette-search-demo-ret-greenbc-en-1440x900.png` | Search hit under GREENBC (teal active wash) | GREENBC | en |
| 13 | `LRP-C6/screenshots/13-palette-dialog-search-hit-greenbc-en.png` | Dialog close-up — GREENBC search results | GREENBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Dialog a11y: `role="dialog"` + `aria-modal="true"` + accessible name | ✅ | Functional BDD-001; frames 01–02 |
| Focus on search input when opened; Esc closes | ✅ | Functional BDD-005; evidence open path |
| Dual-brand logo + primary wash (REDBC red / GREENBC teal) | ✅ | 03 vs 11; full-page 01 vs 09; search 04 vs 12; highlight 06 vs 12 |
| Grouped results + muted subtitles (route path / externalId · group) | ✅ | 02, 05, 10, 13 |
| No-match is empty copy — not `LoadErrorPanel` | ✅ | 07–08; `command-palette-no-match` |
| Keyboard highlight visible (`aria-selected` / `--active`) | ✅ | Frame 06 |
| No text overflow / clipping / overlap @1440×900 | ✅ | 01–13 |
| English-first i18n (`commandPalette.*`) | ✅ | Frames + catalogs |
| Tokens / brand primary for active option (not hex brand fork) | ✅ | `color-mix(... var(--brand-primary) ...)` in `CommandPalette.vue` |
| Entity display in palette hits uses human title + externalId subtitle (no raw UUID primary) | ✅ | Frames 04–05, 12–13 |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Listbox nesting** (`CommandPalette.vue`) — `role="listbox"` wraps `<section>` group titles, loading/error `<p>`, and `role="option"` buttons. Non-option descendants inside a listbox are an a11y structure smell (AT may announce extraneous content). Prefer: listbox containing only options, with group labels via `role="group"` / `aria-labelledby`, or move status/empty outside the listbox.  
   Rule: frontend-oa-design §Quality bar (accessible roles); WAI-ARIA listbox pattern.

2. **Magic layout sizes** (`CommandPalette.vue` scoped SCSS) — `width: min(640px, 100%)`, `max-height: min(72vh, 560px)`, `gap: 0.15rem`, `letter-spacing: 0.06em`, `font-weight: 550` sit outside the shared spacing/type token scale. Prefer tokenized max-widths / spacing steps when a palette size token exists (or document as intentional overlay exception).  
   Rule: frontend-oa-design §Foundations (tokens; no magic px).

### 🟢 Nice to have

1. Capture zh-CN frames for palette placeholder / no-match / hint parity (en frames sufficient for this slice).
2. Native `type="search"` clear affordance may render browser-default chrome; optional styling alignment with OA tokens if product wants stricter visual lock.

## Files added for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C6-uiux-evidence.spec.ts` | Evidence capture (empty / search / no-match / keyboard, dual brand) |
| `frontend/e2e/helpers/uiux-evidence.ts` | `LRP_C6_*` dirs + `captureLrpC6*` helpers |
| `frontend/e2e/evidence/LRP-C6/screenshots/01–13` | Screenshot set |
| `frontend/e2e/evidence/LRP-C6-uiux-manifest.md` | This manifest |

## Notes for architecture / doc-sync

- Stage 7 **PASS_WITH_NOTES** — ready for architecture-reviewer (stage 8).
- Upstream Stage 6 functional: `LRP-C6-command-palette` **8/8**.
- No product UI redesign in this stage; evidence + helpers only.
- Optional polish (listbox structure / magic sizes) → route to `frontend-engineer` if product wants follow-up; **non-blocking** for merge of LR-C6 behavior.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/LRP-C6-command-palette.spec.ts`
- BDD: `docs/behavior/lrp-c6-command-palette.md`
