# LRP-B6 UIUX Evidence Manifest

**Task:** LR-B6 session absolute-limit reminder banner (SCEN-UX-02, `docs/behavior/session-renewal-revocation.md` §8.7 + §12.3) — non-blocking warning banner in the OA shell with sign-in-again hand-off  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-07-04  
**Viewport:** 1440×900 (desktop-first, `LRP_B6_VIEWPORT`)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` restarted with `SESSION_ABSOLUTE_TTL=PT9M` (Part B runbook in `LRP-B6-session-renewal.spec.ts` header)  
**Verdict:** **PASS** (remediation applied and re-verified; no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `E2E_TARGET=docker pnpm exec playwright test LRP-B6-session-uiux-evidence.spec.ts --workers=1` | **1/1 passed** — 7 screenshots re-captured after image rebuild (stage 4, e2e-test-engineer) |
| `RENEWAL_PART=B E2E_TARGET=docker pnpm exec playwright test LRP-B6-session-renewal.spec.ts --workers=1` | **Part B passed** — `role="alert"`, exact en/zh-CN copy, renewal suppression, login redirect (stage 4, e2e-test-engineer) |
| `pnpm -C frontend test --run SessionLimitReminder` | **passed** — incl. new warning-scoped class-hook binding test |

Review method: static evidence review of the on-disk screenshots cross-checked against
`SessionLimitReminder.vue`, `ManagementShell.vue`, `global.scss` tokens, the i18n catalogs, and
spec §12.3. Contrast ratios independently recomputed (WCAG relative luminance) from the
code-declared token values; screenshots corroborate the rendered colors.

### Functional + accessibility coverage (referenced)

| Assertion | Where | Result |
| --- | --- | --- |
| Banner `role="alert"` | `LRP-B6-session-renewal.spec.ts` (`toHaveAttribute('role', 'alert')`) + `SessionLimitReminder.test.ts` | PASS |
| en + zh-CN title/message/action exact match to §12.3 | `LRP-B6-session-renewal.spec.ts` `REMINDER_COPY` exact-text assertions | PASS |
| Non-blocking (no auto-logout, banner persists >75s, renewal suspended) | `LRP-B6-session-renewal.spec.ts` Part B | PASS |
| Action button routes to login with `redirect` query preserved | `LRP-B6-session-renewal.spec.ts` Part B | PASS |
| Keyboard Tab order reaches action button; `:focus-visible` ring rendered | `LRP-B6-session-uiux-evidence.spec.ts` (`document.activeElement` assertion) + frame 07 | PASS |
| Warning-scoped style hooks stay bound on the rendered button | `SessionLimitReminder.test.ts` (`.el-button.session-limit-reminder__action`) | PASS |

## Screenshot inventory (7)

| # | File | View / state | Brand | Locale |
| --- | --- | --- | --- | --- |
| 1 | `LRP-B6/screenshots/01-session-reminder-redbc-en-1440x900.png` | Shell (My tasks) with reminder banner between header and body; amber outlined action | REDBC | en |
| 2 | `LRP-B6/screenshots/02-session-reminder-banner-redbc-en.png` | Banner close-up (locator capture, full 1440 width) | REDBC | en |
| 3 | `LRP-B6/screenshots/03-session-reminder-greenbc-en-1440x900.png` | Same shell after brand switch — action identical (brand-neutral warning) | GREENBC | en |
| 4 | `LRP-B6/screenshots/04-session-reminder-greenbc-zhcn-1440x900.png` | Same shell after locale switch | GREENBC | zh-CN |
| 5 | `LRP-B6/screenshots/05-session-reminder-redbc-zhcn-1440x900.png` | Brand switched back with zh-CN retained | REDBC | zh-CN |
| 6 | `LRP-B6/screenshots/06-session-reminder-banner-greenbc-en.png` | GREENBC banner close-up — pixel-consistent with #2 | GREENBC | en |
| 7 | `LRP-B6/screenshots/07-session-reminder-action-focus-visible-redbc-en.png` | Action button keyboard `:focus-visible` state — 2px brand outline, 2px offset (real Tab presses) | REDBC | en |

## Remediation applied (UIUX findings from initial review)

| Finding (initial review, 🔴) | Fix |
| --- | --- |
| Action button rendered Element Plus stock blue (`#409eff`/`#ecf5ff`/`#a0cfff`) in both brands; label contrast ≈2.53:1 (AA needs 4.5:1), boundary ≈1.57:1; hover jumped to brand hue | Scoped `.el-button.session-limit-reminder__action` rule overrides EP button variables with warning-semantic tokens: rest `--status-warning-text` text/border + transparent fill; hover/active `--status-warning-text-strong` + `--surface-card`. Compound selector + scoped attribute (specificity 0,3,0) outranks library and global plain-primary rules — no `!important`, no `:deep()` on the button rule |
| (🟡) No GREENBC banner close-up | Frame 06 added |
| (🟡) No focused-state frame | Frame 07 added (Tab-driven `:focus-visible`, `document.activeElement` asserted) |
| (🟢) Warning text contrast headroom | `--status-warning-text-strong: #92400e` token added; used for hover/active (7.09:1) |

## Copy parity vs spec §12.3 (L1 copy table)

| Key | Spec baseline | Catalog (`en.ts` / `zh-CN.ts`) | Rendered (screens 2/4/5/6) |
| --- | --- | --- | --- |
| `session.absoluteLimitReminder.title` | Session ending soon / 会话即将结束 | byte-identical | ✅ verbatim |
| `session.absoluteLimitReminder.message` | Your sign-in session is about to reach its time limit. Please save your work, then sign in again to continue. / 您的登录会话即将到达时长上限。请先保存当前工作，然后重新登录以继续使用。 | byte-identical | ✅ verbatim |
| `session.absoluteLimitReminder.action` | Sign in again / 重新登录 | byte-identical | ✅ verbatim |

No `token` / `JWT` technical vocabulary anywhere in the banner (unit test enforces `not.toMatch(/token|JWT/i)`); copy uses "sign-in session" / "登录会话" as mandated (§8.7). All strings via i18n keys — no literals in `SessionLimitReminder.vue`.

## Contrast spot-check (recomputed from declared tokens)

| Element | Foreground | Background | Ratio | AA target | Verdict |
| --- | --- | --- | --- | --- | --- |
| Banner title (15px / 650) | `--status-warning-text` `#b45309` | `--status-warning-bg` `#fffbeb` | 4.84:1 | 4.5:1 | ✅ |
| Banner message (13px) | `#b45309` | `#fffbeb` | 4.84:1 | 4.5:1 | ✅ |
| Action label, rest (12px) | `#b45309` | transparent → `#fffbeb` | 4.84:1 | 4.5:1 | ✅ |
| Action border, rest | `#b45309` | `#fffbeb` | 4.84:1 | 3:1 (non-text, 1.4.11) | ✅ |
| Action label/border, hover/active | `--status-warning-text-strong` `#92400e` | `--surface-card` `#ffffff` | 7.09:1 | 4.5:1 / 3:1 | ✅ |
| Focus ring | `--brand-primary` `#db0011` (REDBC) / `#00847F` (GREENBC) | `#fffbeb` | 5.04:1 / 4.40:1 | 3:1 (non-text) | ✅ |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| Warning semantics tokenized end-to-end (`--status-warning-bg/-text/-text-strong`, `--surface-card`, `--space-*`, `--font-size-*`; no raw hex in `SessionLimitReminder.vue`) | ✅ | Component styles L38–94 |
| Banner in document flow between header and `shell-body` — no occlusion/overflow/overlap of header, nav, or content | ✅ | 1, 3, 4, 5; `ManagementShell.vue` L187 |
| No text overflow/clipping/ellipsis at 1440×900 in either locale; `flex-wrap` + `flex-shrink: 0` guard the action | ✅ | 1–6 |
| Dual-brand rendering correct: shell chrome re-themes (REDBC red vs GREENBC teal); warning banner + action intentionally brand-neutral and identical across brands | ✅ | 1/5 vs 3/4; 2 vs 6 |
| Action button visual states: rest / hover / active / focus-visible defined via tokens; focus ring demonstrated | ✅ | Component L84–94; frame 7 |
| Contrast (banner text + action all states) | ✅ | Contrast table |
| `role="alert"` + keyboard reachability + visible focus | ✅ | Functional E2E, unit test, frame 7 |
| Density matches OA desktop baseline (8px/24px padding aligned with shell header gutter; 15px title + 13px body; not cramped) | ✅ | 2, 6 |
| Non-blocking pattern (no modal, no auto-logout, `closable=false` persistent reminder per §8.7) | ✅ | Part B functional run |
| English-first copy via i18n keys; zh-CN additive | ✅ | Copy parity table |
| No `!important`, no `:deep()` on the action override; scoped specificity documented in-code | ✅ | Component L77–94 |

## Findings

### 🔴 Critical (must fix before merge)

_None._ (Initial-review critical finding remediated and re-verified.)

### 🟡 Suggestion (should improve)

_None._

### 🟢 Nice to have

1. Capture a hover-state frame of the action button (white fill + `#92400e`) to complete the visual state set; currently code-verified + contrast-computed only.
2. Add a 1280-wide frame to document `flex-wrap` behavior at the narrower common desktop width.
3. Optional zh-CN banner close-up for locale-level detail parity (full-page zh frames 4–5 already show verbatim copy).

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/src/components/session/SessionLimitReminder.vue` | Warning-scoped action button states (rest/hover/active) via tokens |
| `frontend/src/styles/global.scss` | `--status-warning-text-strong: #92400e` semantic token |
| `frontend/src/components/session/SessionLimitReminder.test.ts` | Class-hook binding test for the scoped override |
| `frontend/e2e/LRP-B6-session-uiux-evidence.spec.ts` | Skeleton-settled guard; frames 06 (GREENBC close-up) + 07 (`:focus-visible`) |
| `frontend/e2e/evidence/LRP-B6/screenshots/01–07` | Re-captured evidence set (7 frames) |
| `frontend/e2e/evidence/LRP-B6-uiux-manifest.md` | This manifest |

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- Behavior spec: `docs/behavior/session-renewal-revocation.md` §8.7 + §12.3
- Functional baseline: `frontend/e2e/LRP-B6-session-renewal.spec.ts`
- Manifest pattern: `frontend/e2e/evidence/P12-AUD-B10-uiux-manifest.md`
