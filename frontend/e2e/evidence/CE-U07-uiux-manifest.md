# CE-U07 UIUX Evidence Manifest — Clause outdated bump

**Task:** CE-U07 / Task Master **#82**  
**Slice:** `ce-u07-clause-outdated-bump` (`feat/ce-u07-clause-outdated-bump`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (**UP**)  
**Verdict:** **PASS_WITH_NOTES** (no 🔴 Critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-U07-clause-outdated-bump.spec.ts` | **2/2 passed** |
| Stage 7 evidence: `CE-U07-clause-outdated-bump-uiux-evidence.spec.ts` | **1/1 passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U07-clause-outdated-bump.spec.ts `
  e2e/CE-U07-clause-outdated-bump-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts
```

## Screenshot inventory

| # | File | View / state |
| --- | --- | --- |
| 1 | `01-clause-outdated-badge-redbc-1920x1080.png` | Design → Clause references — out-of-date badge + bump affordance @1920 REDBC |

Path prefix: `frontend/e2e/evidence/CE-U07/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Out-of-date badge readable at 1920 | ✅ | 01 |
| Bump control adjacent to outdated row | ✅ | 01 + Stage 6 click path |
| English-first i18n labels | ✅ | Spec assertions (EN) |
| No text overlap on clause panel | ✅ | 01 full-page capture |
| Onboarding tour does not block dashboard Open | ✅ | COB-004 dismisses `onboarding-tour-skip` |

## Notes

1. Single REDBC @1920 capture is sufficient for badge/bump affordance; dual-brand matrix not required for this reminder slice.
2. Dashboard deep-link uses **Open** button (not entity link) — matches task hub pattern.
