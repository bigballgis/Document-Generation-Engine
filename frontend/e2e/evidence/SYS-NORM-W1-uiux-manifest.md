# SYS-NORM-W1 UIUX Evidence Manifest — shell fluid + nav trim + EditMore + EntityLink

**Task:** SYS-NORM Wave 1 / Task Master **#144** — shell fluid + Security nav D1 hide + EditMore + EntityLink  
**Slice:** `sys-norm-shell-fluid-nav` (`feat/sys-norm-shell-fluid-nav`)  
**Worktree:** `D:/working/DGE-sys-norm-shell-fluid-nav`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-21  
**Viewport:** 1440×900 (standard) + 1800×900 (fluid-beyond-contained proof)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (no redeploy)  
**Verdict:** **PASS** (Critical = 0; dual-brand evidence complete)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `SYS-NORM-W1-shell-fluid-nav.spec.ts` | **7/7 passed** (handoff) |
| Stage 7 evidence: `SYS-NORM-W1-uiux-evidence.spec.ts` | **4/4 passed** |
| `a11y-smoke.spec.ts` | **9/9 passed** (same session prior to evidence re-run) |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/SYS-NORM-W1-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# a11y 9/9 + evidence 01–04 / 05–08 PASS; 09–12 initially timed out on invisible `.authorized-groups` crop
# Spec fix: crop `userGroupLink` after scrollIntoViewIfNeeded
pnpm -C frontend exec playwright test `
  e2e/SYS-NORM-W1-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 4 passed (54.8s)
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-security-nav-shell-redbc-1440x900.png` | REDBC | Shell + Security trim |
| 1b | `01b-security-nav-crop-redbc.png` | REDBC | Sidebar crop — Activity log + Legal holds only |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `01-security-nav-shell-greenbc-1440x900.png` | GREENBC | Shell dual-brand |
| 2b | `01b-security-nav-crop-greenbc.png` | GREENBC | Sidebar crop — teal active |
| 2c | `01c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 3 | `05-templates-catalog-fluid-redbc-1440x900.png` | REDBC | Templates catalog fluid @1440 |
| 3b | `05b-templates-catalog-fluid-redbc-1800x900.png` | REDBC | Catalog fluid beyond 1440 cap @1800 |
| 4 | `06-template-detail-fluid-redbc-1800x900.png` | REDBC | Template detail fluid @1800 |
| 4b | `06b-template-detail-fluid-redbc-1440x900.png` | REDBC | Template detail fluid @1440 |
| 5 | `07-templates-catalog-fluid-greenbc-1440x900.png` | GREENBC | Catalog fluid dual-brand |
| 5b | `07b-templates-catalog-fluid-greenbc-1800x900.png` | GREENBC | Catalog @1800 dual-brand |
| 6 | `08-template-detail-fluid-greenbc-1800x900.png` | GREENBC | Detail fluid dual-brand |
| 6b | `08b-brand-header-greenbc-crop.png` | GREENBC | Header crop |
| 7 | `09-users-editmore-entitylink-redbc-1440x900.png` | REDBC | Users Edit/More + EntityLink groups |
| 7b | `09b-users-editmore-crop-redbc.png` | REDBC | Edit / More alignment crop |
| 7c | `09c-users-entitylink-crop-redbc.png` | REDBC | Group EntityLink (`CORP`) crop |
| 7d | `09d-users-more-menu-redbc-1440x900.png` | REDBC | More menu — Disable / Reset password / Delete |
| 8 | `10-groups-editmore-redbc-1440x900.png` | REDBC | Groups Edit/More |
| 8b | `10b-groups-editmore-crop-redbc.png` | REDBC | Groups actions crop |
| 9 | `11-users-editmore-entitylink-greenbc-1440x900.png` | GREENBC | Users dual-brand |
| 10 | `12-groups-editmore-greenbc-1440x900.png` | GREENBC | Groups dual-brand |
| 10b | `12b-brand-header-greenbc-crop.png` | GREENBC | Header crop |
| 11 | `13-templates-entitylink-redbc-1440x900.png` | REDBC | Templates row EntityLink |
| 11b | `13b-templates-row-entitylink-redbc.png` | REDBC | Row crop — RETAIL link readable |
| 12 | `13-templates-entitylink-greenbc-1440x900.png` | GREENBC | Templates EntityLink dual-brand |
| 12b | `13b-templates-row-entitylink-greenbc.png` | GREENBC | Row crop dual-brand |

Path prefix: `frontend/e2e/evidence/SYS-NORM-W1/screenshots/` (**27** files on disk)

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Fluid catalog (no 1440 inner / `--fluid`) | ✅ | 05 / 05b / 07 / 07b — class assert + width >1440 @1800 |
| Fluid detail workspace | ✅ | 06 / 06b / 08 — template detail `--fluid`, no `__inner` |
| Security nav = Activity log + Legal holds only | ✅ | 01b REDBC/GREENBC; brands/entities absent |
| Nav icons present (Security + core items) | ✅ | 01b + Stage 6 icon asserts |
| Dual-brand REDBC + GREENBC | ✅ | 01–13 pairs; `--brand-primary` asserted |
| Logo / brand header switch | ✅ | 01c Red Bank; 01c/08b/12b Green Bank |
| Users/Groups Edit/More alignment | ✅ | 09b / 10b; geometric assert (y-drift ≤2, gap ≤24) |
| EntityLink readable, no UUID primary | ✅ | 09c CORP; 13b RETAIL; no UUID assert |
| No horizontal page overflow @1440/@1800 | ✅ | Spec `assertNoViewportOverflow` |
| a11y smoke (critical axe) | ✅ | 9/9 + scoped shell-nav critical = 0 |
| English-first copy | ✅ | Activity log / Legal holds / User management / Edit / More |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| 🟡 Suggestion | Templates catalog row crop @1440 shows trailing column text truncation (e.g. Last updated by → `Temp…`). Fluid width @1800 improves use of space; consider sticky/min-width tuning for dense catalog columns so 1440 remains fully scannable without ellipsis on identity columns. | OA density / `AppDataTable` templates catalog — not a Wave 1 blocker |
| 🟡 Suggestion | Users **Roles** / **Status** tags read low-contrast (light blue / light green on white) vs brand EntityLink emphasis — pre-existing EP tag styling; optional token contrast pass. | OA a11y contrast — not introduced by #144 |
| 🟢 Nice to have | Users More menu items render light-gray; readable but secondary hierarchy could match OA tertiary weight tokens more clearly | `TableEditMoreActions` + EP dropdown |
| — | No 🔴 Critical | — |

### Notes (non-blocking)

1. Wave 1 acceptance overrides older OA skill wording that kept detail pages `contained` — evidence confirms **fluid-all** for catalog + detail samples (`AppPageLayout` `--fluid`).
2. N18 deferred (handoff) — not in Stage 7 scope.
3. Helpers: `SYS_NORM_W1_VIEWPORT` 1440×900 + `SYS_NORM_W1_WIDE_VIEWPORT` 1800×900 + `captureSysNormW1Screenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
4. Spec: `frontend/e2e/SYS-NORM-W1-uiux-evidence.spec.ts`.
5. Evidence crop note: first attempt used `.authorized-groups` locator which can be off-viewport/clipped; switched to visible EntityLink after `scrollIntoViewIfNeeded`.

## Stage 7 gate

**PASS** — ready for Stage 8 architecture-reviewer (no UIUX blockers for merge of #144).
