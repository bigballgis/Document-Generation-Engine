# LRP-C7 UIUX Evidence Manifest — Notification center (bell + dropdown)

**Task:** LR-C7 / TaskMaster #33 — shell notification bell OA polish, dual-brand, a11y  
**Slice:** `lrp-c7-notification-center` (`feat/lrp-c7-notification-center`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-11  
**Viewport:** 1440×900 (desktop-first, `LRP_C7_VIEWPORT`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (DEPLOY_OK)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers on notification surfaces)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6: `pnpm -C frontend exec playwright test e2e/LRP-C7-notification-center.spec.ts --config playwright.docker.config.ts --workers=1` | **5/5 passed** (upstream) |
| Stage 7 a11y: `pnpm -C frontend exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **9/9 passed** |
| Stage 7 evidence: `pnpm -C frontend exec playwright test e2e/LRP-C7-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **1/1 passed** — 14 dual-brand screenshots |

Review method: Playwright evidence at 1440×900; dual-brand via `switchBrand` (REDBC ↔ GREENBC); visual inspection of on-disk PNGs; static cross-check of `NotificationBell.vue`, English i18n keys (`collaboration.notifications.*`).

### Surface coverage (handoff)

| Scenario | Surface | Evidence frames |
| --- | --- | --- |
| Shell + badge (≥1 unread) | Header bell + brand badge | 01–03 (REDBC), 10–12 (GREENBC) |
| Bell focus (keyboard / programmatic) | `notification-bell` | 04 |
| Open dropdown with items | Popover list + Mark all | 05–06 (REDBC), 13–14 (GREENBC) |
| Empty state | `notification-empty` (“No notifications”) | 07–08 (REDBC; list mocked empty after mark-all) |
| Zero-unread shell (no badge) | Bell without badge | 09 |

## Screenshot inventory (14)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-C7/screenshots/01-shell-bell-badge-redbc-en-1440x900.png` | Dashboard shell — bell + red badge | REDBC | en |
| 2 | `LRP-C7/screenshots/02-header-bell-badge-redbc-en.png` | Header close-up — badge “1” | REDBC | en |
| 3 | `LRP-C7/screenshots/03-brand-header-redbc-en.png` | REDBC logo / wordmark | REDBC | en |
| 4 | `LRP-C7/screenshots/04-bell-focus-redbc-en.png` | Bell locator crop (focus path) | REDBC | en |
| 5 | `LRP-C7/screenshots/05-dropdown-open-redbc-en-1440x900.png` | Dropdown open over My tasks | REDBC | en |
| 6 | `LRP-C7/screenshots/06-dropdown-items-redbc-en.png` | Items + red “Mark all as read” | REDBC | en |
| 7 | `LRP-C7/screenshots/07-dropdown-empty-redbc-en-1440x900.png` | Empty dropdown over dashboard | REDBC | en |
| 8 | `LRP-C7/screenshots/08-dropdown-empty-closeup-redbc-en.png` | “No notifications” empty copy | REDBC | en |
| 9 | `LRP-C7/screenshots/09-header-bell-zero-unread-redbc-en.png` | Bell, no badge (0 unread) | REDBC | en |
| 10 | `LRP-C7/screenshots/10-shell-bell-badge-greenbc-en-1440x900.png` | Dashboard shell — bell + teal badge | GREENBC | en |
| 11 | `LRP-C7/screenshots/11-header-bell-badge-greenbc-en.png` | Header close-up — teal badge “1” | GREENBC | en |
| 12 | `LRP-C7/screenshots/12-brand-header-greenbc-en.png` | GREENBC logo / wordmark | GREENBC | en |
| 13 | `LRP-C7/screenshots/13-dropdown-open-greenbc-en-1440x900.png` | Dropdown open under GREENBC | GREENBC | en |
| 14 | `LRP-C7/screenshots/14-dropdown-items-greenbc-en.png` | Items + teal “Mark all as read” | GREENBC | en |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Bell visible for collaboration-capable role; badge uses `--brand-primary` | ✅ | 02 vs 11 (red vs teal badge) |
| Dual-brand logo switch (REDBC / GREENBC) | ✅ | 03 vs 12; full-page 01 vs 10 |
| Dropdown `role="region"` + titled “Notifications” | ✅ | Functional + frames 06 / 14 |
| Item primary + meta (template · queue · age); no raw UUID primary | ✅ | 06, 14 — human `templateName` |
| Mark-all uses brand primary text | ✅ | Red 06 / teal 14 |
| Empty copy English-first (`No notifications`) | ✅ | 08 |
| Zero-unread: badge hidden, bell remains | ✅ | 09 |
| Tokens / `color-mix` brand washes (hover / expanded) | ✅ | `NotificationBell.vue` scoped SCSS |
| Focus styles declared (`:focus-visible` + focus tokens) | ✅ / note | CSS present; see 🟡 #2 |
| a11y-smoke green on docker stack | ✅ | 9/9 |
| No overflow / clipping / overlap @1440×900 | ✅ | 01–14 |
| English-first i18n (`collaboration.notifications.*`) | ✅ | Frames + `en.ts` |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **`aria-expanded` / `aria-haspopup` not present on live trigger** (`NotificationBell.vue`) — Template declares `aria-haspopup="true"` and `:aria-expanded="dropdownOpen"`, but the Element Plus popover reference button in the Docker DOM is an `el-tooltip__trigger` **without** those attributes (only `aria-label` / optional `aria-describedby`). Screen readers lose expanded state. Prefer a wrapper that retains ARIA, or EP API that preserves reference attrs.  
   Rule: frontend-oa-design §Quality bar (accessible roles); WAI-ARIA disclosure pattern.  
   **Non-blocking** — `aria-label` with unread count still works; dropdown has `role="region"` + accessible name.

2. **Empty state after “Mark all as read” is not the empty copy** — `markAllRead` keeps items (marked `read: true`) and `fetchList` typically still returns rows, so `notification-empty` is only shown when the list payload is truly `[]`. Evidence frames 07–08 used a one-shot route mock after mark-all. Product may want unread-only list **or** empty when all items are read.  
   Rule: frontend-oa-design §State completeness (empty).  
   **Non-blocking** — empty UI exists and renders correctly when items.length === 0.

3. **Focus-visible ring hard to prove via locator crop** — Frame 04 is a tiny crop; Chromium `:focus-visible` may not paint on programmatic `focus()`. CSS uses tokenized focus ring. Prefer Tab-order evidence in a follow-up if product wants stricter proof.  
   Rule: frontend-oa-design §State completeness (focus visible).

### 🟢 Nice to have

1. Upstream first-poll auth race (unread hydrate via Page Visibility) — do **not** treat as UIUX Critical; functional specs already re-hydrate.
2. Capture zh-CN frames for title / Mark all / empty parity (en sufficient for this slice).
3. `popper-class="notification-dropdown-popper"` has no dedicated global stylesheet; EP default popover chrome + scoped content is acceptable for OA polish today.

## Files added for evidence

| Path | Purpose |
| --- | --- |
| `frontend/e2e/LRP-C7-uiux-evidence.spec.ts` | Evidence capture (badge / open / empty / focus, dual brand) |
| `frontend/e2e/helpers/uiux-evidence.ts` | `LRP_C7_*` dirs + `captureLrpC7*` helpers |
| `frontend/e2e/evidence/LRP-C7/screenshots/01–14` | Screenshot set |
| `frontend/e2e/evidence/LRP-C7-uiux-manifest.md` | This manifest |

## Notes for architecture / doc-sync

- Stage 7 **PASS_WITH_NOTES** — ready for architecture-reviewer (stage 8): **yes**.
- Upstream Stage 6 functional: `LRP-C7-notification-center` **5/5**.
- No product UI redesign in this stage; evidence + helpers only.
- Optional ARIA / empty-state polish → route to `frontend-engineer` if product wants follow-up; **non-blocking** for merge of LR-C7 behavior.

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- `.cursor/skills/frontend-entity-display/SKILL.md`
- `docs/architecture/management-ui-constitution.md`
- Functional baseline: `frontend/e2e/LRP-C7-notification-center.spec.ts`
- Component: `frontend/src/components/layout/NotificationBell.vue`
