# SYS-NORM-W7-UIUX Evidence Manifest — Templates Import dry-run dialog

**Task:** SYS-NORM Wave 7 / Task Master **#151** — Import dry-run UI (promotion pack)  
**Slice:** `sys-norm-promotion-pack` (`feat/sys-norm-promotion-pack`)  
**Worktree:** `D:/working/DGE-sys-norm-promotion-pack`  
**Reviewer:** e2e-uiux-reviewer (Stage 7 re-review after Critical fix)  
**Date:** 2026-07-21  
**Viewport:** 1440×900 (standard)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (FE rebuilt with sticky footer)  
**Verdict:** **PASS_WITH_NOTES** (Critical = 0; dual-brand evidence + a11y-smoke green)

## Surfaces checked

| # | Surface | Route / state | Brands |
| --- | --- | --- | --- |
| 1 | Import dialog — empty / gated | `/templates` → Import template | REDBC + GREENBC |
| 2 | Import dialog — ready dry-run report | Check dependencies → Ready to import | REDBC + GREENBC |
| 3 | Import dialog — blocking dry-run report | Check dependencies → Not ready to import | REDBC |
| 4 | Brand header / logo switch | Shell header | REDBC + GREENBC |

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (prior): `SYS-NORM-W7-promotion-import-dry-run.spec.ts` | **4/4 passed** (handoff) |
| Stage 7 re-review evidence: `SYS-NORM-W7-uiux-evidence.spec.ts` | **5/5 passed** (incl. footer-in-viewport asserts) |
| `a11y-smoke.spec.ts` (Stage 7 re-review) | **9/9 passed** |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/SYS-NORM-W7-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# a11y 9/9; W7 UIUX 5/5 — total 14/14
```

## Screenshot inventory

Path prefix: `frontend/e2e/evidence/SYS-NORM-W7/screenshots/` (**12** files)

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-import-empty-redbc-1440x900.png` | REDBC | Empty Import dialog + shell |
| 1b | `01b-import-empty-dialog-redbc-crop.png` | REDBC | Empty dialog crop (footer visible) |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Red Bank header |
| 2 | `01-import-empty-greenbc-1440x900.png` | GREENBC | Empty Import dialog dual-brand |
| 2b | `01b-import-empty-dialog-greenbc-crop.png` | GREENBC | Empty dialog crop |
| 2c | `01c-brand-header-greenbc-crop.png` | GREENBC | Green Bank header |
| 3 | `02-import-ready-report-redbc-1440x900.png` | REDBC | Ready report — **footer fully in viewport** |
| 3b | `02b-import-ready-report-dialog-redbc-crop.png` | REDBC | Ready report dialog crop + sticky footer |
| 4 | `03-import-ready-report-greenbc-1440x900.png` | GREENBC | Ready report dual-brand — footer in viewport |
| 4b | `03b-import-ready-report-dialog-greenbc-crop.png` | GREENBC | Ready report dialog crop |
| 5 | `04-import-blocking-report-redbc-1440x900.png` | REDBC | Blocking / Not ready — footer in viewport |
| 5b | `04b-import-blocking-report-dialog-redbc-crop.png` | REDBC | Blocking dialog crop + disabled brand Import |

Spec: `frontend/e2e/SYS-NORM-W7-uiux-evidence.spec.ts`  
Helpers: `SYS_NORM_W7_VIEWPORT` + `captureSysNormW7*` in `frontend/e2e/helpers/uiux-evidence.ts`  
Manifest (this file): `frontend/e2e/evidence/SYS-NORM-W7-uiux-manifest.md`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| OA dialog pattern (title, description, form, footer actions) | ✅ | max-height + scrollable body + sticky footer |
| English-first i18n (`templates.import.*`) | ✅ | Title / Check dependencies / Dependency report / Ready|Not ready |
| Check dependencies + gated Import | ✅ | Stage 6 + evidence asserts Import disabled until ready |
| Dependency report hierarchy (counts + typed rows) | ✅ | Ready badge / Not ready badge; report scrolls in body |
| Dual-brand REDBC + GREENBC | ✅ | Header crops; `--brand-primary` asserted in evidence |
| Logo switch | ✅ | 01c Red Bank ↔ Green Bank |
| Enabled primary uses brand (not EP #409EFF) | ✅ | Assert red/teal family when Import enabled (oklab→sRGB normalize) |
| Disabled primary brand tint | ✅ | `--el-button-disabled-bg-color` brand mix; blocking Import muted red |
| Conflict radios use brand primary | ✅ | Selected radio REDBC red / GREENBC teal |
| No horizontal overflow @1440 | ✅ | Spec `assertNoViewportOverflow` |
| No vertical clip of primary actions @1440 | ✅ | Spec `assertImportFooterInViewport` on ready + blocking |
| a11y smoke (critical axe) | ✅ | **9/9** |
| Tokens / no magic hex in dialog SCSS | ✅ | `TemplateImportDialog.vue` uses CSS vars |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | **Prior Critical cleared:** After dry-run Dependency report (ready or blocking), dialog footer (**Cancel** / **Check dependencies** / **Import template**) remains fully inside 1440×900. Body scrolls; footer sticky with border-top + surface background. | OA dialogs / no clipping — `TemplateImportDialog.vue` |
| 🟢 Nice to have | Bundle summary still shows raw **Source template ID** UUID — acceptable for import provenance; `word-break` present. | Entity display exception — summary metadata |
| — | No data-leak / forbidden-surface issues on this Import path | — |

### Notes

1. Stage 6 functional journeys already prove gating, report clearing on policy change, blocking Import disabled, and commit → DRAFT (4/4).
2. Re-review visual evidence: Ready REDBC/GREENBC and Not-ready REDBC all show sticky footer with Import reachable; enabled Import brand-red/teal; disabled Import brand-tinted (not EP blue).
3. Evidence spec hardened: `assertImportFooterInViewport` + canvas `getImageData` normalize for Chromium `oklab()` button backgrounds.
4. Prior Suggestions (disabled primary blue wash; radio EP blue) **resolved** via `global.scss` brand mixes + dialog radio CSS vars.

## Stage 7 gate

**PASS_WITH_NOTES** — Critical = **0**. Unblocks Stage 8 (architecture-reviewer). Notes are Nice-to-have only (UUID provenance display).
