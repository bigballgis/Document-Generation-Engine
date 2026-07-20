# CD-HARD-T06 — Audit list/export E2E re-evidence (Stage 6 functional)

**Task:** CD-HARD-T06 / Task Master **#140** / slice `cdp-cd3-hard-t06`  
**Wave:** CD-3 hardening closeout  
**Engineer:** e2e-test-engineer (Stage 6)  
**Date:** 2026-07-20  
**Pointer:** `docs/behavior/cd-hard-t06-audit-export-reevidence.md`  
**Canonical BDD:** `docs/behavior/audit-admin-query-journey.md` — `BDD-CDP-AUDIT-001`, `BDD-CDP-AUDIT-002`  
**Verdict:** **PASS** — **2 passed / 0 failed** (reuse CDP-E2E-T11; no new product behavior)

## Spec reuse

| Item | Value |
| --- | --- |
| Spec | `frontend/e2e/CDP-E2E-T11-audit-query.spec.ts` (no CD-HARD-T06 wrapper) |
| Historical T11 | Done (merge `6e3f825`); this run is CD-3 residual closeout re-evidence |
| UIUX | Stage 7 owns screenshots; baseline `frontend/e2e/evidence/CDP-E2E-T11-uiux-manifest.md` |

## Stack

| Surface | Result |
| --- | --- |
| Frontend `:4173` | **200** (DEPLOY_OK, SkipBuild reuse) |
| Backend `:8080` | **200** `/actuator/health` |
| Placement | ISOLATED `D:/working/DGE-cdp-cd3-hard-t06` / `feat/cdp-cd3-hard-t06` |

## Command (canonical docker acceptance)

```powershell
pnpm -C frontend exec playwright test e2e/CDP-E2E-T11-audit-query.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

| Metric | Value |
| --- | --- |
| Tests | **2** |
| Passed | **2** |
| Failed | **0** |
| Duration | ~17.3s |
| Config | `playwright.docker.config.ts` (workers=1) |
| HTML report | `frontend/playwright-report/docker/index.html` |

## Scenario map

| Scenario | Test title | Result |
| --- | --- | --- |
| BDD-CDP-AUDIT-001 | filter by event type updates list; view-only; no My to-dos | **PASS** (~5.6s) |
| BDD-CDP-AUDIT-002 | Export confirm triggers JSON download (not 403) | **PASS** (~5.6s) |

## Harness note (not product scope)

First-login AUDIT_ADMIN without tour dismiss: ManagementShell LR-C8 auto-open calls `ensureDashboardAnchors()` → `router.push('/dashboard')` → route guard denies `route.dashboard-home` → Forbidden (while `/audit` + `readAudit` remain valid).

T11 `beforeEach` seeds `docgen.onboardingTour.dismissed.v1:10000004=1` so CD-HARD-T06 re-evidence stays on Activity log filter/export. **Product fix** (AUDIT_ADMIN tour must not force dashboard) → `frontend-engineer` / LR-C8 residual — **out of CD-HARD-T06 scope**.

## CD-3 note (T11 lineage)

This Stage 6 run closes CD-HARD-T06 residual re-evidence for the same journeys historically proven by CDP-E2E-T11. No Activity log query/export product expansion.
