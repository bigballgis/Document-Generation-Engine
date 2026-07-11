# LRP-C2 UIUX Evidence Manifest

**Task:** LR-C2 / TaskMaster #29 — structured editor local draft recovery banner  
**Slice:** lrp-c2-local-draft-recovery (eat/lrp-c2-local-draft-recovery)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first, LRP_C2_VIEWPORT)  
**Stack:** Docker frontend http://127.0.0.1:4173 + backend http://127.0.0.1:8080 (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on LR-C2 recovery surfaces)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: pnpm -C frontend exec playwright test e2e/LRP-C2-draft-recovery.spec.ts --config playwright.docker.config.ts --workers=1 | **4/4 passed** (upstream; frames under LRP-C2-draft-recovery/) |
| Stage 7: pnpm -C frontend exec playwright test e2e/LRP-C2-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1 | **1/1 passed** — 14 dual-brand screenshots |
| pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1 | **9/9 passed** |

Review method: Playwright evidence capture at 1440×900 with scrollIntoViewIfNeeded on data-testid=structured-draft-recovery-banner; dual-brand via switchBrand (REDBC ↔ GREENBC); visual inspection of on-disk PNGs; static cross-check of StructuredDraftRecoveryBanner.vue, ControlledStructuredContentEditor.vue, English i18n 	emplates.structuredEditor.draftRecovery.*.

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Reload → recovery banner + timestamps + Restore/Discard | StructuredDraftRecoveryBanner in binding editor | 01–04 (REDBC), 05–08 (GREENBC) |
| After Restore + Save → remount no banner | Controlled structured editor | 09–10 |
| Dirty-guard Discard → remount banner again | Same banner after leave/reopen | 11–12 (REDBC), 13–14 (GREENBC) |

## Screenshot inventory (14)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | LRP-C2/screenshots/01-recovery-banner-redbc-en-1440x900.png | Dev bindings editor — recovery banner in OA shell | REDBC | en |
| 2 | LRP-C2/screenshots/02-recovery-banner-closeup-redbc-en.png | Banner close-up (title, message, Draft saved timestamp, Restore/Discard) | REDBC | en |
| 3 | LRP-C2/screenshots/03-brand-header-redbc-en.png | REDBC header logo / wordmark | REDBC | en |
| 4 | LRP-C2/screenshots/04-restore-focus-redbc-en.png | Restore draft focused (primary red) | REDBC | en |
| 5 | LRP-C2/screenshots/05-recovery-banner-greenbc-en-1440x900.png | Same recovery surface after brand switch | GREENBC | en |
| 6 | LRP-C2/screenshots/06-recovery-banner-closeup-greenbc-en.png | Banner close-up — Restore teal | GREENBC | en |
| 7 | LRP-C2/screenshots/07-brand-header-greenbc-en.png | GREENBC header logo / wordmark | GREENBC | en |
| 8 | LRP-C2/screenshots/08-restore-focus-greenbc-en.png | Restore draft focused (primary teal) | GREENBC | en |
| 9 | LRP-C2/screenshots/09-after-save-no-banner-redbc-en-1440x900.png | Remount after Save — no recovery banner | REDBC | en |
| 10 | LRP-C2/screenshots/10-editor-no-banner-closeup-redbc-en.png | Editor close-up — toolbar + restored paragraphs, no banner | REDBC | en |
| 11 | LRP-C2/screenshots/11-banner-after-dirty-guard-discard-redbc-en-1440x900.png | Banner returns after dirty-guard Discard | REDBC | en |
| 12 | LRP-C2/screenshots/12-banner-after-dirty-guard-closeup-redbc-en.png | Remount banner close-up | REDBC | en |
| 13 | LRP-C2/screenshots/13-banner-after-dirty-guard-discard-greenbc-en-1440x900.png | Same remount banner under GREENBC | GREENBC | en |
| 14 | LRP-C2/screenshots/14-banner-after-dirty-guard-closeup-greenbc-en.png | Remount banner close-up — Restore teal | GREENBC | en |

Upstream functional frames (Stage 6, not dual-brand): LRP-C2-draft-recovery/01–04. Prefer Stage 7 frames for visual review — Stage 6 viewport shots often miss the banner below the fold without scroll.

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Recovery banner uses shared EP l-alert warning + brand-primary Restore | ✅ | Frames 02/06; StructuredDraftRecoveryBanner.vue |
| Dual-brand Restore primary (REDBC red / GREENBC teal) | ✅ | 02/04 vs 06/08; 11/12 vs 13/14 |
| Dual-brand logo + shell chrome switch | ✅ | 03 vs 07; full-page 01 vs 05 |
| Timestamps visible (Draft saved: …) via locale formatter | ✅ | 02, 06, 12, 14 |
| Restore / Discard actions clear; hierarchy primary vs secondary | ✅ | Close-ups 02/06 |
| After-save remount: no banner | ✅ | 09–10 |
| Dirty-guard Discard retains draft → banner returns | ✅ | 11–14 |
| No text overflow / clipping / overlap @1440×900 | ✅ | Full-page 01/05/11/13 |
| English-first i18n (	emplates.structuredEditor.draftRecovery.*) | ✅ | Frames + n.ts |
| a11y smoke green | ✅ | a11y-smoke **9/9** |
| Contained workspace layout (detail/editor, not fluid catalog) | ✅ | Dev editor shell frames |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Duplicate timestamp copy** (StructuredDraftRecoveryBanner.vue + n.ts) — message already embeds {draftTimestamp} (“A local draft from …”) and the meta line repeats “Draft saved: {timestamp}”. Consider keeping a single timestamp line to reduce noise.  
   Rule: frontend-oa-design §Components (hierarchy / density); i18n English-first clarity.

2. **Stage 6 functional screenshots often omit the banner from the viewport** (LRP-C2-draft-recovery.spec.ts captureEvidence without scrollIntoViewIfNeeded) — Playwright 	oBeVisible can pass while the banner sits below the fold at 1440×900. Stage 7 evidence scrolls the banner into view; optionally align Stage 6 captures.  
   Rule: e2e-frontend-testing evidence capture; UIUX evidence mechanics.

3. **Restore :focus-visible ring is subtle on EP primary buttons** (frames 04/08) — keyboard focus works; ring contrast against the cream alert + brand fill is weaker than shell/nav focus tokens. Optional: ensure banner actions inherit the same --focus-ring-* treatment as LR-C12.  
   Rule: frontend-oa-design §Quality bar (visible focus).

### 🟢 Nice to have

1. Capture zh-CN frames for draft-recovery copy parity (en frames sufficient for this slice).
2. When serverUpdatedAt is present, add a dual-brand frame showing the “Server content: …” meta line (-if="serverTimestampLabel").

## Files added for evidence

| Path | Purpose |
| --- | --- |
| rontend/e2e/LRP-C2-uiux-evidence.spec.ts | Dual-brand evidence capture |
| rontend/e2e/helpers/uiux-evidence.ts | LRP_C2_* dirs + captureLrpC2* helpers |
| rontend/e2e/evidence/LRP-C2/screenshots/01–14 | Screenshot set |
| rontend/e2e/evidence/LRP-C2-uiux-manifest.md | This manifest |

## Notes for architecture / doc-sync

- Recovery UX is a warning alert + brand-primary Restore / secondary Discard — correct OA vocabulary; no ad-hoc chrome.
- Doc-sync should record Stage 7 **PASS_WITH_NOTES**, evidence paths above, Stage 6 LRP-C2-draft-recovery 4/4, a11y-smoke 9/9.
- No ADR / permission-matrix change required for this UIUX slice.
- Optional follow-up (non-blocking): dedupe timestamp copy; align Stage 6 screenshot scroll; strengthen Restore focus ring — route to rontend-engineer if product wants polish.

## References

- .cursor/skills/frontend-oa-design/SKILL.md
- .cursor/skills/e2e-frontend-testing/SKILL.md
- Functional baseline: rontend/e2e/LRP-C2-draft-recovery.spec.ts
- Components: StructuredDraftRecoveryBanner.vue, ControlledStructuredContentEditor.vue
- Manifest pattern: rontend/e2e/evidence/LRP-C9-uiux-manifest.md
