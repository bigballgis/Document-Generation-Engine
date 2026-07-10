# LRP-C9 UIUX Evidence Manifest

**Task:** LR-C9 / TaskMaster #14 — unified catalog list states (`LoadErrorPanel` + role-aware empty CTAs)  
**Slice:** `lrp-c9-load-error-panel` (`feat/lrp-c9-load-error-panel`)  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-07-10  
**Viewport:** 1440×900 (desktop-first, `LRP_C9_VIEWPORT`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on LR-C9 surfaces)

## Test execution

| Command | Result |
| --- | --- |
| `pnpm -C frontend exec playwright test e2e/LRP-C9-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** — 12 screenshots |
| `pnpm -C frontend exec playwright test e2e/LRP-C9-list-states.spec.ts --config playwright.docker.config.ts --workers=1` | **3/3 passed** (upstream stage 6) |
| `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **3 passed / 4 failed** — failures are FOL/demo-seed prerequisites (`CORP-FOL-OFFER` / content-module fixtures), **not** LR-C9 list-state surfaces |

Review method: Playwright evidence capture at 1440×900 for Templates error / Templates empty+CTA / Groups error across REDBC + GREENBC; visual inspection of on-disk screenshots; static cross-check of `LoadErrorPanel.vue`, `EmptyStatePanel.vue`, `TemplateListView.vue`, `GroupManagementPanel.vue`, and English i18n keys.

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| LR-C9-A Templates load failure + Retry | `TemplateListView` + `LoadErrorPanel` | 01–05 |
| LR-C9-B Templates empty + author CTA | `TemplateListView` + `EmptyStatePanel` | 06–08 |
| LR-C9-A Groups load failure + Retry | `GroupManagementPanel` + `LoadErrorPanel` | 09–12 |

## Screenshot inventory (12)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C9/screenshots/01-templates-load-error-redbc-en-1440x900.png` | Templates catalog — LoadErrorPanel + Retry in OA shell | REDBC | en |
| 2 | `LRP-C9/screenshots/02-templates-load-error-panel-redbc-en.png` | LoadErrorPanel close-up (icon, title, retryable hint, Retry) | REDBC | en |
| 3 | `LRP-C9/screenshots/03-templates-load-error-greenbc-en-1440x900.png` | Same Templates error after brand switch — Retry teal | GREENBC | en |
| 4 | `LRP-C9/screenshots/04-brand-header-greenbc-templates-error.png` | GREENBC header logo / brand chrome | GREENBC | en |
| 5 | `LRP-C9/screenshots/05-templates-retry-focus-redbc-en.png` | Retry keyboard focus ring on LoadErrorPanel | REDBC | en |
| 6 | `LRP-C9/screenshots/06-templates-empty-cta-redbc-en-1440x900.png` | Empty catalog + role-aware “New template package” CTA | REDBC | en |
| 7 | `LRP-C9/screenshots/07-templates-empty-panel-redbc-en.png` | EmptyStatePanel close-up | REDBC | en |
| 8 | `LRP-C9/screenshots/08-templates-empty-cta-greenbc-en-1440x900.png` | Empty + CTA after GREENBC switch | GREENBC | en |
| 9 | `LRP-C9/screenshots/09-groups-load-error-redbc-en-1440x900.png` | Groups catalog — LoadErrorPanel + Retry | REDBC | en |
| 10 | `LRP-C9/screenshots/10-groups-load-error-panel-redbc-en.png` | Groups LoadErrorPanel close-up | REDBC | en |
| 11 | `LRP-C9/screenshots/11-groups-load-error-greenbc-en-1440x900.png` | Groups error after GREENBC switch | GREENBC | en |
| 12 | `LRP-C9/screenshots/12-brand-header-greenbc-groups-error.png` | GREENBC header on Groups error | GREENBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Shared `LoadErrorPanel` / `EmptyStatePanel` (no ad-hoc EP empty/error) | ✅ | Views wire shared components; frames 02, 07, 10 |
| Tokens / brand primary on Retry + empty CTA (REDBC red / GREENBC teal) | ✅ | 01/02 vs 03; 06 vs 08; 09 vs 11 |
| Dual-brand logo + shell chrome switch | ✅ | 04, 12; full-page 03/08/11 |
| No text overflow / clipping / overlap at 1440×900 | ✅ | 01–12 |
| Retryable hint when `retryable` | ✅ | Copy “This error is retryable…” on 02, 05, 10 |
| English-first i18n (`templates.error.loadList`, `templates.list.empty`, `templates.create.open`, `identity.error.loadGroups`, `common.retry`) | ✅ | Frames + catalogs |
| Role-aware empty CTA (author sees create; wired via `#actions` slot) | ✅ | 06–08; functional LR-C9-B |
| Keyboard focus reaches Retry (`:focus-visible` ring) | ✅ | Frame 05 |
| Fluid catalog layout (no wasted 1440 gutters on list pages) | ✅ | Templates / Groups full-width content |
| Error icon remains semantic red (EP `el-result`); action uses brand primary | ✅ | Intentional; 02 vs 03 |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Duplicate create CTA on Templates empty** (`TemplateListView.vue`) — page header already exposes “New template package” / “Import template”, and `EmptyStatePanel` repeats the create CTA. Acceptable OA discoverability pattern; consider suppressing header primary when the empty panel owns the CTA to reduce twin primaries.  
   Rule: frontend-oa-design §Components (button hierarchy / density).

2. **Groups error chrome stays interactive** (`GroupManagementPanel.vue`) — search + “Create group” remain enabled while `LoadErrorPanel` is showing. Not a data leak; mild confusion risk if create succeeds while list is broken. Optional: disable header actions while `errorMessage` is set.  
   Rule: frontend-oa-design §State completeness.

### 🟢 Nice to have

1. Capture zh-CN frames for LoadErrorPanel / empty copy parity (en frames already sufficient for this slice).
2. Re-run full `a11y-smoke.spec.ts` after FOL/demo catalog seed is restored on the acceptance stack (current failures are seed/env, not LR-C9).

## Files added for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C9-uiux-evidence.spec.ts` | Evidence capture (Templates error/empty, Groups error, dual brand) |
| `frontend/e2e/helpers/uiux-evidence.ts` | `LRP_C9_*` dirs + `captureLrpC9*` helpers |
| `frontend/e2e/evidence/LRP-C9/screenshots/01–12` | Screenshot set |
| `frontend/e2e/evidence/LRP-C9-uiux-manifest.md` | This manifest |

## Notes for architecture / doc-sync

- Shared panels are the correct vocabulary; no new one-off error/empty widgets introduced.
- Doc-sync should record Stage 7 **PASS_WITH_NOTES**, evidence paths above, and upstream Stage 6 `LRP-C9-list-states` 3/3.
- No ADR / permission-matrix change required for this UIUX slice.
- Optional follow-up (non-blocking): header-action disable-on-error and empty-state CTA dedupe — route to `frontend-engineer` if product wants polish.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
- Functional baseline: `frontend/e2e/LRP-C9-list-states.spec.ts`
- Components: `LoadErrorPanel.vue`, `EmptyStatePanel.vue`
- Manifest pattern: `frontend/e2e/evidence/LRP-B6-uiux-manifest.md`
