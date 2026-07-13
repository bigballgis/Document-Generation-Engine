# API-OPS-DISCOVERABILITY — Stage 6 Functional E2E Manifest

**Slice:** `api-ops-discoverability` (Task Master **#52**)  
**Stage:** 6 — e2e-test-engineer (functional)  
**Date:** 2026-07-14  
**Placement:** ISOLATED `D:/working/DGE-api-ops-discoverability` / `feat/api-ops-discoverability`  
**BDD readiness:** `ready` ([docs/behavior/api-ops-discoverability.md](../../../docs/behavior/api-ops-discoverability.md))  
**Spec:** `frontend/e2e/API-OPS-DISCOVERABILITY.spec.ts`  
**Fixture helper:** `frontend/e2e/helpers/submit-approval-gate-api.ts` (`prepareTemplatePendingRelease`) + `content-modules-api.ts` (`fetchDemoFullFlowApiPolicy`)  
**Verdict:** **PASS** (5/5)

## Environment

| Item | Value |
| --- | --- |
| UI | `http://127.0.0.1:4173` (Docker nginx) |
| API / healthz | `http://127.0.0.1:8080` **200** |
| Stage 5 | **DEPLOY_OK_WITH_NOTES** (parent handoff) |
| Fixture | `prepareTemplatePendingRelease` → `PENDING_RELEASE` + C10 skeleton `api_policy` with empty `allowedAdGroups` |
| Role | Group Admin (`E2E_GROUP_ADMIN`) |
| Config | `playwright.docker.config.ts` |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:E2E_SKIP_CATALOG_CLEANUP='true'
pnpm exec playwright test e2e/API-OPS-DISCOVERABILITY.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Result:** **5 passed** (≈17 s) — 2026-07-14 Docker acceptance.

## Scenario coverage

| BDD ID | Journey | Result |
| --- | --- | --- |
| **SCEN-AOD-01** | PENDING_RELEASE Hub shows External access tab; activates `?tab=apiAccess` | **PASS** |
| **SCEN-AOD-03** | `/api/policies/:id` redirects to Hub with apiAccess selected (not overview fallback); route summary visible | **PASS** |
| **SCEN-AOD-06** | `/api/policies` shows three summary cards (`publishedInScope` / `attention` / `pendingReleaseNeedingSetup`); pending ≥ 1 | **PASS** |
| **SCEN-AOD-07** | Overview = summary + alerts; no “Published packages” catalog heading; no alerts pagination | **PASS** |
| **SCEN-AOD-09** | Alerts table row for fixture externalId (`MISSING_AD_GROUP`) → Open external access → Hub `?tab=apiAccess` | **PASS** |
| **SCEN-AOD-13** | `data-testid=ad-groups-not-configured-warning` on Hub; `publish-gate-ad-groups-warning` on Approval → Publish readiness | **PASS** |
| **SCEN-AOD-14** | `published-vs-callable-hint` + warning copy distinguishes Published vs runtime-callable / fail-closed | **PASS** |

## Key assertions

1. PENDING_RELEASE + `canManageApiPolicy` registers Hub External access (AOD-C1).
2. Legacy `/api/policies/:id` deep link activates `apiAccess` with `aria-selected=true` (AOD-C2).
3. Overview readiness summary uses `data-testid="api-readiness-summary"` + three `summary-card-*` cards (AOD-C4); not a template catalog (AOD-C5 / SCEN-ALERT-04).
4. PENDING_RELEASE empty AD Group produces Overview `MISSING_AD_GROUP` alert with Hub deep link (AOD-C6).
5. Visible warnings: External access `ad-groups-not-configured-warning` + Approval Publish readiness `publish-gate-ad-groups-warning`; published ≠ runtime-callable copy (AOD-C8 / AOD-C9).

## Artifacts

| Path | Role |
| --- | --- |
| `frontend/e2e/API-OPS-DISCOVERABILITY.spec.ts` | Journey + assertions |
| `frontend/e2e/evidence/API-OPS-DISCOVERABILITY-manifest.md` | This manifest |
| `frontend/playwright-report/docker/` | HTML report (docker config) |

## Product code changes

**None** (test-only Stage 6).

## Blockers

None.
