# CE-U05 UIUX Evidence Manifest — Fidelity viewed persistence

**Task:** CE-U05 / Task Master **#66** — fidelity viewed persistence + publish gate  
**Slice:** `ce-u05-fidelity-viewed-persist` (`feat/ce-u05-fidelity-viewed-persist`)  
**Reviewer:** e2e-uiux-reviewer (Stage 7)  
**Date:** 2026-07-14  
**Viewport:** 1920×1080 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:5173` + backend `http://127.0.0.1:8080` (**UP**)  
**Verdict:** **PASS_WITH_NOTES** (human-readable fidelity copy + edit binding link; no 🔴 Critical UIUX blockers)

## Test execution

| Command | Result |
| --- | --- |
| Stage 6 functional: `CE-U05-fidelity-viewed-persist.spec.ts` | **4/4 passed** |
| Stage 7 evidence: `CE-U05-uiux-evidence.spec.ts` | **1/1 passed** |

```powershell
$env:E2E_BASE_URL='http://127.0.0.1:5173'; $env:FRONTEND_PORT='5173'
pnpm -C frontend exec playwright test e2e/CE-U05-fidelity-viewed-persist.spec.ts `
  e2e/CE-U05-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1
```

## Screenshot inventory

| # | File | View / state |
| --- | --- | --- |
| 1 | `01-fidelity-warnings-human-message-redbc-1920x1080.png` | Preview panel — human-readable warning primary |
| 2 | `02-fidelity-warning-list-crop-redbc-1920x1080.png` | Warning list crop |
| 3 | `03-fidelity-warning-technical-expanded-redbc-1920x1080.png` | Technical code expanded |
| 4 | `04-edit-binding-link-crop-redbc-1920x1080.png` | Edit binding deep link |

Path prefix: `frontend/e2e/evidence/CE-U05/screenshots/`

## OA checklist

| Item | Status | Evidence |
| --- | --- | --- |
| Human-readable message primary; technical code secondary | ✅ | 01–03 |
| Edit binding link visible when artifact present | ✅ | 04 |
| Mark viewed control in warning row | ✅ | Stage 6 BDD-004 |
| Onboarding tour dismissed before brand switch | ✅ | Spec helper (LRP-C8 note) |
| No text overlap @1920 | ✅ | Crops 02–04 |

## Notes

1. **FRONTEND_PORT=5173** in worktree `.env` — E2E uses `E2E_BASE_URL` override; production acceptance default remains `:4173`.
2. **Backend Docker healthcheck** reports unhealthy (`wget` missing in JRE image) while `/healthz` returns **200** — deploy script may fail frontend dependency gate; manual `--no-deps` start used for evidence.
