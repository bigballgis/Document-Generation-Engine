# P12-API-PACKAGE-ACCESS UIUX Evidence Manifest

**Task:** Package hub external access tab + cross-package overview (T12)  
**Reviewer:** e2e-uiux-reviewer (evidence captured via Playwright)  
**Date:** 2026-07-03  
**Viewport:** 1440×900 (desktop-first)  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080`  
**Verdict:** **PASS**

## Test execution

| Command | Result |
| --- | --- |
| `E2E_TARGET=docker FRONTEND_PORT=4173 pnpm exec playwright test e2e/P12-API-PACKAGE-ACCESS-uiux-evidence.spec.ts --config playwright.docker.config.ts --workers=1` | **3/3 passed** |
| `E2E_TARGET=docker FRONTEND_PORT=4173 pnpm exec playwright test e2e/P12-API-PACKAGE-ACCESS.spec.ts e2e/P12-API-PACKAGE-ACCESS-RUNTIME.spec.ts --config playwright.docker.config.ts --workers=1` | **7/7 passed** |

## Screenshot inventory

| # | File | View / state | Brand |
| --- | --- | --- | --- |
| 1 | `screenshots/01-hub-external-access-l1-redbc-1440x900.png` | Package hub — External access tab L1 (retention + invocations panel) | REDBC |
| 2 | `screenshots/02-hub-external-access-full-redbc-1440x900.png` | Full page context (hub + shell) | REDBC |
| 3 | `screenshots/03-hub-external-access-l1-greenbc-1440x900.png` | External access tab after brand switch | GREENBC |
| 4 | `screenshots/04-api-services-overview-redbc-1440x900.png` | Cross-package external services overview (nav entry) | REDBC |

## Notes

- Overview route uses client-side navigation (`External services overview` nav) — direct `page.goto('/api/policies')` hits nginx `/api/` proxy.
- Dual-brand token switch verified on hub tab; no text overflow on L1 retention controls at 1440×900.
