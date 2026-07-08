# P13 External Services — UIUX evidence manifest

**Task:** P13-ESO-F05 — hub L1, overview alerts, invocation history + drawer, dual-brand  
**Spec:** `frontend/e2e/P13-EXTERNAL-SERVICES-uiux-evidence.spec.ts`  
**Viewport:** 1440×900

## Screenshots

| # | File | Surface | Brand |
|---|------|---------|-------|
| 1 | `screenshots/01-hub-route-summary-redbc-1440x900.png` | Hub External access — route summary panel | REDBC |
| 2 | `screenshots/02-hub-external-access-full-redbc-1440x900.png` | Hub External access — full tab scroll | REDBC |
| 3 | `screenshots/03-hub-external-access-l1-greenbc-1440x900.png` | Hub External access — full tab | GREENBC |
| 4 | `screenshots/04-overview-alerts-redbc-1440x900.png` | External services overview — alerts card | REDBC |
| 5 | `screenshots/05-hub-invocation-history-redbc-1440x900.png` | Hub — invocation history filters/table | REDBC |
| 6 | `screenshots/06-overview-alerts-greenbc-1440x900.png` | External services overview — alerts card | GREENBC |
| 7 | `screenshots/07-hub-invocation-drawer-redbc-1440x900.png` | Hub — invocation summary drawer (runtime row) | REDBC |

## Functional cross-links

| Journey | Spec | Status |
|---------|------|--------|
| CDP-E2E-T07 output policy save | `CDP-E2E-T07-api-policy-edit-save.spec.ts` | Docker PASS |
| CDP-E2E-T13 S1–S3 materialize/default route | `CDP-E2E-T13-api-package-materialize.spec.ts` | Docker PASS (3/3) |
| P13 route summary + legacy redirect + alerts | `P13-EXTERNAL-SERVICES.spec.ts` | Docker PASS (3/3) |
| BDD S7 batch logical vs flat | `P12-API-PACKAGE-ACCESS-RUNTIME.spec.ts` | Docker PASS |

## Status

**PASS** — 7 screenshots captured; REDBC + GREENBC coverage for hub + overview; invocation drawer with seeded runtime generate.
