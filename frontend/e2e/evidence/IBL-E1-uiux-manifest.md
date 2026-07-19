# IBL-E1 UIUX Evidence Manifest — Locale-variant model (#128)

**Task:** IBL-E1 / Task Master **#128** — management UI locale declare / catalog filter / family sibling nav  
**Slice:** `ibl-e1-locale-variant-model` (`feat/ibl-e1-locale-variant-model`)  
**Worktree:** `D:/working/DGE-ibl-e1-locale-variant-model`  
**Date:** 2026-07-20  
**Viewport:** 1440×900 (a11y smoke / P14_T01_VIEWPORT)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict (Critical a11y gate):** **PASS** — `a11y-smoke` **9/9**

## Behavior SoT

- `docs/behavior/ibl-e1-locale-variant-model.md` (`ready`; BDD-IBL-E1-013…015)
- Functional: `frontend/e2e/IBL-E1-locale-variant-model.spec.ts` + `frontend/e2e/evidence/IBL-E1-locale-variant-model-manifest.md` (stage 6 **4/4 PASS**)
- Helpers: `frontend/e2e/helpers/ibl-e1-locale-variant-api.ts` (locale-aware) + shared fixture helpers below

## Critical unblock (stage 7 / a11y-smoke)

**Symptom:** `a11y-smoke` failed ~7/9 — shared Playwright helpers `POST /templates` without required body `locale` (IBL-E1 `@NotBlank`). API returned `fieldErrors.locale = REQUIRED` (example: `collaboration-api.ts` → `prepareTemplateInTesting`, used by lifecycle / testing workspace smokes via `prepareTemplatePendingSubmitReady`).

**Fix:** Add valid BCP-47 `locale: 'en-US'` (English fixture copy) to every e2e helper create payload for `/templates` and `/content-modules`.

### Helpers updated

| File | Creates fixed |
| --- | --- |
| `helpers/collaboration-api.ts` | template (a11y-smoke path) |
| `helpers/submit-approval-gate-api.ts` | template |
| `helpers/template-export-import-api.ts` | template |
| `helpers/structured-authoring-api.ts` | 2× template |
| `helpers/legal-holds-api.ts` | template |
| `helpers/ce-u13-variable-rename-api.ts` | template |
| `helpers/ce-g05-annual-review-api.ts` | template |
| `helpers/ce-u03-testdata-schema-api.ts` | template |
| `helpers/ce-g03-testdata-pii-api.ts` | template |
| `helpers/cdp-mvp-golden-api.ts` | template |
| `helpers/content-modules-api.ts` | 4× template + 4× content-module |
| `helpers/ibl-e1-locale-variant-api.ts` | already locale-aware (no change) |

## Test execution

| Command | Result |
| --- | --- |
| `pnpm exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1` | **9/9 passed** (26.8s) — 2026-07-20 |

```powershell
cd D:\working\DGE-ibl-e1-locale-variant-model\frontend
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm exec playwright test e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts --workers=1
# 9 passed (26.8s)
```

### a11y-smoke coverage

| # | Scenario | Result |
| --- | --- | --- |
| 1 | Login primary heading / form | passed |
| 2 | Content modules list h1 | passed |
| 3 | Tester dashboard task hub h1 | passed |
| 4 | Templates list h1 | passed |
| 5 | Dashboard timeout config (admin) | passed |
| 6 | Lifecycle submit gate (API fixture + locale) | passed |
| 7 | Template testing workspace h2 (API fixture + locale) | passed |
| 8 | Masters list — zero critical axe | passed |
| 9 | Dashboard — zero critical axe | passed |

**HTML report:** `frontend/playwright-report/docker/`  
**Remaining non-E1 failures:** none observed in this run.

## Dual-brand / visual polish

Screenshot helpers exist (`IBL_E1_*` in `uiux-evidence.ts` → `frontend/e2e/evidence/IBL-E1/screenshots/`). Dedicated dual-brand UIUX evidence capture spec may still be authored/run by stage-7 reviewer; this manifest records the **Critical a11y-smoke unblock** so UIUX review is unblocked.

## Findings

| Severity | Item | Status |
| --- | --- | --- |
| 🔴 Critical | e2e helper create missing `locale` → a11y-smoke fixture fail | **Resolved** |
| — | Dual-brand screenshot inventory for create/catalog/family-nav | Pending dedicated UIUX capture (non-blocking for a11y-smoke) |

## Out of scope (this fix)

- No MAIN / fe-audience-manuals edits
- No Task Master #3b / #5a status flips
- No merge / post-task doc-sync / Done claim
- FE unit gates skipped (e2e helpers only)
