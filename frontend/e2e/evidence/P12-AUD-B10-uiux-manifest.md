# P12-AUD-B10 UIUX Evidence Manifest

**Task:** Submit-for-approval evidence checklist gate (§5.8) — lifecycle submit gate card, summary dialog, author journey CTA  
**Reviewer:** e2e-uiux-reviewer  
**Date:** 2026-07-01  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS** (remediation applied; no critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| `E2E_TARGET=docker pnpm exec playwright test P12-AUD-B10-uiux-evidence.spec.ts` | **1/1 passed** (13.3s) |
| `E2E_TARGET=docker pnpm exec playwright test P12-AUD-B10-submit-approval-gate.spec.ts` | **3/3 passed** (17.7s) |
| `E2E_TARGET=docker pnpm exec playwright test a11y-smoke.spec.ts --grep "submit gate"` | **1/1 passed** |

### Functional + accessibility coverage

| Spec | Result |
| --- | --- |
| `P12-AUD-B10-uiux-evidence.spec.ts` — dual-brand lifecycle ready/blocked, summary dialog, author journey CTA | PASS |
| `P12-AUD-B10-submit-approval-gate.spec.ts` — pass path, blocker path, checklist labels | 3/3 PASS |
| `a11y-smoke.spec.ts` — template lifecycle submit gate `h3` heading after author login | PASS |

## Screenshot inventory (6)

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-author-journey-submit-cta-redbc-1440x900.png` | Template detail — author role journey timeline with enabled “Submit for approval” CTA | REDBC |
| 2 | `screenshots/02-lifecycle-submit-gate-ready-redbc-1440x900.png` | Workflow status tab — green submission readiness checklist, enabled submit button | REDBC |
| 3 | `screenshots/03-lifecycle-submit-gate-ready-greenbc-1440x900.png` | Same ready checklist after brand switch | GREENBC |
| 4 | `screenshots/04-lifecycle-submit-gate-blocked-redbc-1440x900.png` | Workflow status tab — TEST_RESULTS blocker, disabled submit button | REDBC |
| 5 | `screenshots/05-lifecycle-submit-gate-blocked-greenbc-1440x900.png` | Same blocked checklist after brand switch | GREENBC |
| 6 | `screenshots/06-submit-summary-dialog-ready-redbc-1440x900.png` | “Review before submit for approval” summary dialog (ready checklist) | REDBC |

## Remediation applied (UIUX findings)

| Finding | Fix |
| --- | --- |
| Submit gate card reused publish/go-live copy | Added `templates.submitGate.*` i18n keys; lifecycle card uses submit-specific title/description/tags |
| Author journey CTA silent no-op when gate blocked | Journey CTA disabled when submit gate not ready; `handleSubmitForApproval` shows `ElMessage.warning` |
| Dual-brand switch helper stale selector | `switchBrand` selects “Retail Bank” / “Green Bank” options (not internal `GREENBC` code) |

## OA checklist (bank OA standard)

| Item | Status | Evidence |
| --- | --- | --- |
| OA shell (brand bar + left nav + content) | ✅ | 01–06 |
| White baseline, professional palette | ✅ | All screenshots |
| Dual-brand REDBC / GREENBC theming | ✅ | 02 vs 03; 04 vs 05 |
| Submit-specific copy (not go-live wording) | ✅ | 02 — “Submission readiness checks” |
| Checklist tags (Ready / Pending / Informational) | ✅ | 02, 04 |
| Disabled primary when blockers present | ✅ | 04, 05 |
| Summary dialog hierarchy + confirm/cancel | ✅ | 06 |
| Author journey CTA aligned with lifecycle gate | ✅ | 01 |
| English-first copy via i18n | ✅ | All screenshots |
| No text overflow / overlap at 1440×900 | ✅ | Visual review |

## Findings

### 🔴 Critical (must fix before merge)

_None._

### 🟡 Suggestion (should improve)

1. **Summary dialog checklist title** still uses generic “Pre-release checks” inside `submitApprovalSummary.checklistTitle` — consider a submit-specific label for full copy parity.
2. **Blocked journey CTA** is disabled (not merely warned) when gate fails — acceptable but consider inline hint under CTA for discoverability.

### 🟢 Nice to have

1. Capture author journey CTA on GREENBC for dual-brand journey evidence.
2. Capture `ElMessage.warning` toast when user attempts submit via keyboard while gate blocked.

## Files added / extended

| Path | Purpose |
| --- | --- |
| `frontend/e2e/P12-AUD-B10-uiux-evidence.spec.ts` | UIUX screenshot capture spec (6 frames) |
| `frontend/e2e/evidence/P12-AUD-B10-uiux-manifest.md` | This manifest |
| `frontend/e2e/helpers/uiux-evidence.ts` | P12-AUD-B10 evidence dirs + `switchBrand` label fix |
| `frontend/src/i18n/locales/en.ts` / `zh-CN.ts` | `templates.submitGate.*`, `lifecycle.submitGateBlocked` |
| `frontend/src/views/templates/detail/TemplateDetailLifecycleTab.vue` | Submit-specific gate card copy |
| `frontend/src/views/templates/TemplateDetailView.vue` | Journey CTA disable + warning on blocked submit |
| `frontend/src/components/journey/TemplateAuthorJourneyBlock.vue` | `primaryCtaDisabled` prop |
| `frontend/e2e/a11y-smoke.spec.ts` | Submit gate heading smoke |

## References

- `.cursor/skills/frontend-oa-design/SKILL.md`
- Functional baseline: `frontend/e2e/P12-AUD-B10-submit-approval-gate.spec.ts`
- Pattern: `frontend/e2e/P14-T02-uiux-evidence.spec.ts`
