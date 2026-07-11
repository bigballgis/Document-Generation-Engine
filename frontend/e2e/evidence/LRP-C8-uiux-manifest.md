# LRP-C8 UIUX Evidence Manifest — Role onboarding tour

**Task:** LR-C8 / TaskMaster #34 — role onboarding tour (el-tour spotlight + OA action card), Help Replay, dual-brand  
**Slice:** `lrp-c8-role-onboarding-tour` (`feat/lrp-c8-role-onboarding-tour`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on tour / Help Replay surfaces)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C8-onboarding-tour.spec.ts --config playwright.docker.config.ts --workers=1` | **4/4 passed** (upstream) |
| Stage 7 a11y: `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **9/9 passed** |
| Stage 7 evidence: Playwright capture @1440×900 (dual brand via brand switcher; tour closed before GREENBC switch) | **10 dual-brand screenshots** |

Review method: visual inspection of on-disk PNGs; computed style check (`Next` REDBC vs GREENBC); static cross-check of `OnboardingTour.vue`, Help menu in `ManagementShell.vue`, English i18n (`onboardingTour.*`).

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Tour open — step 1 (Create template) | Spotlight mask + fixed OA action card | 01–02 (REDBC), 06–07 (GREENBC) |
| Brand header / logo | REDBC / GREENBC wordmark | 03, 08 |
| Help → Replay menu | `help-menu` + `help-menu-replay-tour` | 04 (REDBC), 09–10 (GREENBC) |
| Replay re-opens step 1 | Tour card after Replay | 05 (REDBC) |

## Screenshot inventory (10)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C8/screenshots/01-tour-open-step1-redbc-en-1440x900.png` | Dashboard — tour open step 1 + red Next | REDBC | en |
| 2 | `LRP-C8/screenshots/02-tour-action-card-redbc-en.png` | OA card close-up — Skip / Next | REDBC | en |
| 3 | `LRP-C8/screenshots/03-brand-header-redbc-en.png` | REDBC logo / wordmark | REDBC | en |
| 4 | `LRP-C8/screenshots/04-help-menu-replay-open-redbc-en-1440x900.png` | Help open — “Replay role tour” | REDBC | en |
| 5 | `LRP-C8/screenshots/05-tour-replayed-step1-redbc-en-1440x900.png` | After Replay — step 1 card + mask | REDBC | en |
| 6 | `LRP-C8/screenshots/06-tour-open-step1-greenbc-en-1440x900.png` | Tour open under GREENBC | GREENBC | en |
| 7 | `LRP-C8/screenshots/07-tour-action-card-greenbc-en.png` | OA card — teal Next `#00847F` | GREENBC | en |
| 8 | `LRP-C8/screenshots/08-brand-header-greenbc-en.png` | GREENBC logo / wordmark | GREENBC | en |
| 9 | `LRP-C8/screenshots/09-header-help-menu-greenbc-en.png` | Header Help under GREENBC | GREENBC | en |
| 10 | `LRP-C8/screenshots/10-help-menu-replay-open-greenbc-en-1440x900.png` | Help Replay open under GREENBC | GREENBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Fixed OA action card (not EP default tour chrome) | ✅ | EP `.el-tour__content` / buttons `display:none`; card owns copy + actions — frames 02 / 07 |
| Primary Next uses `--brand-primary` (REDBC red / GREENBC teal) | ✅ | 02 vs 07; computed `rgb(0, 132, 127)` = `#00847F` on GREENBC |
| Dual-brand logo switch | ✅ | 03 vs 08; full-page 01 vs 06 |
| Card a11y: `role="dialog"` + `aria-modal="true"` + labelled title | ✅ | DOM + `OnboardingTour.vue` |
| Focus-visible on tour buttons (tokenized brand ring) | ✅ | CSS `:focus-visible` + `--brand-primary` |
| Help menu English-first (“Help”, “Replay role tour”) | ✅ | 04, 10; `onboardingTour.help.*` |
| Tour chrome English-first (Skip / Next / Create template + guidance) | ✅ | 01–02, 06–07 |
| Tokens for card surface / border / spacing (not raw brand hex forks) | ✅ | `OnboardingTour.vue` scoped SCSS |
| a11y-smoke green on docker stack | ✅ | 9/9 |
| No text overflow / clipping on card @1440×900 | ✅ | 01–10 |
| Host `height:0` intentional — visual focus is mask + fixed card | ✅ | Handoff note; card `position:fixed` bottom-center |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Help dropdown can remain open after Replay** (`ManagementShell.vue` + `el-dropdown`) — After clicking `help-menu-replay-tour`, the dropdown item can stay visible while the tour mask + action card open (observed `dropdownStillOpen=1` and frame 05). Prefer closing the dropdown on command (or `@visible-change`) before `replay()`.  
   Rule: frontend-oa-design §Components (dialogs/overlays purposeful, no clutter).  
   **Non-blocking** — Replay still opens step 1; Esc / outside click clears the menu.

2. **Spotlight cutout may be weak on Dashboard Overview** (`useOnboardingTour.ts`) — Targets `[data-journey-timeline] [data-journey-step]`, falling back to the timeline. Author landing on **Overview** often has no journey timeline in DOM, so EP mask dims the full viewport without a clear step hole (frames 01 / 06). Product may want auto-select “Template authoring workflow” (or nav Templates) before open so spotlight anchors a real step.  
   Rule: frontend-oa-design §Quality bar (visual focus / hierarchy).  
   **Non-blocking** — OA card + guidance remain clear; functional BDD open/skip/replay PASS.

3. **Brand switcher (and header chrome) blocked while tour mask is up** — `el-tour__hollow` intercepts pointer events across the viewport (expected for a modal tour). Evidence / users must Skip or Finish before switching brand.  
   Rule: none violated — document for evidence authors.  
   **Non-blocking**.

### 🟢 Nice to have

1. Dedicated `LRP-C8-uiux-evidence.spec.ts` + `captureLrpC8*` helpers (mirror C6/C7) for repeatable CI evidence — capture this round used an ad-hoc Playwright script against the live Docker stack.
2. zh-CN frames for Skip / Next / Replay parity (en sufficient for this slice).
3. Capture Skip→closed shell frame and Next focus-visible crop in a follow-up evidence pass.

## Files added for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/evidence/LRP-C8/screenshots/01–10` | Screenshot set |
| `frontend/e2e/evidence/LRP-C8-uiux-manifest.md` | This manifest |

## Notes for architecture / doc-sync

- Stage 7 **PASS_WITH_NOTES** — ready for architecture-reviewer (stage 8): **yes**.
- Upstream Stage 6 functional: `LRP-C8-onboarding-tour` **4/4**.
- No product UI redesign in this stage; evidence + review notes only.
- Optional Help-dropdown close / journey-tab spotlight polish → route to `frontend-engineer` if product wants follow-up; **non-blocking** for merge of LR-C8 behavior.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/LRP-C8-onboarding-tour.spec.ts`
- Components: `frontend/src/components/layout/OnboardingTour.vue`, Help block in `ManagementShell.vue`
- Composable: `frontend/src/composables/useOnboardingTour.ts`
