# CE-U12 UIUX Evidence Manifest — caller contract copyable examples

**Task:** CE-U12 / Task Master **#87** — Caller contract copyable examples (curl + test-dataset payload + copy)  
**Slice:** `ce-u12-contract-copyable-examples` (`feat/ce-u12-contract-copyable-examples`)  
**Worktree:** `D:/working/DGE-ce-u12-contract-copyable-examples`  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-15  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` — **UP** (`COMPOSE_PROJECT_NAME=documentgenerationengine`; queue idle)  
**Verdict:** **PASS** (no 🔴 Critical UIUX blockers; dual-brand @1920 artifacts present)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional (handoff): `CE-U12-contract-copyable-examples.spec.ts` | **7/7 passed** |
| Stage 7 evidence: `CE-U12-contract-copyable-examples-uiux-evidence.spec.ts` | **1/1 passed** (~8.9s) |
| `a11y-smoke.spec.ts` | **9/9** — initial run 8/9 (masters login flake); masters retry **1/1 passed**; dashboard OK in same run |

```powershell
pnpm -C frontend exec playwright test `
  e2e/a11y-smoke.spec.ts `
  e2e/CE-U12-contract-copyable-examples-uiux-evidence.spec.ts `
  --config playwright.docker.config.ts --workers=1
# evidence 1 passed; a11y masters flake retried green
```

## Screenshot inventory

| # | File | Brand | View / state |
| --- | --- | --- | --- |
| 1 | `01-contract-examples-dataset-redbc-1920x1080.png` | REDBC | Examples + selected dataset (full page) |
| 1b | `01b-copyable-example-crop-redbc-1920x1080.png` | REDBC | Copyable example panel crop |
| 1c | `01c-brand-header-redbc-crop.png` | REDBC | Header — Red Bank |
| 2 | `02-copy-curl-feedback-redbc-1920x1080.png` | REDBC | After Copy curl (success path) |
| 3 | `03-contract-examples-dataset-greenbc-1920x1080.png` | GREENBC | Examples dual-brand |
| 3b | `03b-copyable-example-crop-greenbc-1920x1080.png` | GREENBC | Panel crop — teal primary |
| 3c | `03c-brand-header-greenbc-crop.png` | GREENBC | Header — Green Bank |
| 4 | `04-empty-dataset-hint-redbc-1920x1080.png` | REDBC | Empty dataset guidance |
| 4b | `04b-empty-dataset-crop-redbc-1920x1080.png` | REDBC | Empty hint + `variables: {}` crop |
| 5 | `05-empty-dataset-hint-greenbc-1920x1080.png` | GREENBC | Empty hint dual-brand |
| 5b | `05b-empty-dataset-crop-greenbc-1920x1080.png` | GREENBC | Empty hint crop |
| 6 | `06-copy-curl-focus-greenbc-crop.png` | GREENBC | Copy curl focus affordance |

Path prefix: `frontend/e2e/evidence/CE-U12/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Dual-brand REDBC + GREENBC @1920 | ✅ | 01–03, 04–05 + crops |
| Logo / brand header switch | ✅ | 01c Red Bank; 03c Green Bank |
| Full curl + Auth / Idempotency-Key placeholders | ✅ | 01b, 03b |
| Payload reflects selected test data set | ✅ | 01b (`E2E-U12-UIUX-Acme`) |
| Copy curl primary / Copy payload secondary | ✅ | 01b (red), 03b (teal) |
| Empty dataset English hint; curl still copyable | ✅ | 04b, 05b |
| No horizontal overflow @1920 | ✅ | Spec assert + full-page shots |
| a11y smoke (critical axe) | ✅ | 9/9 after masters retry |

## Findings

| Severity | Finding | Rule / surface |
| --- | --- | --- |
| — | None | — |

## Notes

1. Helpers: `CE_U12_VIEWPORT` 1920×1080 + `captureCeU12Screenshot` / `captureCeU12LocatorScreenshot` in `frontend/e2e/helpers/uiux-evidence.ts`.
2. Spec: `frontend/e2e/CE-U12-contract-copyable-examples-uiux-evidence.spec.ts`.
3. Surface: `TemplateCallerContractPanel.vue` / `.scss` — token-based panel; `AppSearchSelect` for dataset.
4. No merge / no new deploy performed (stage 7 handoff).
