# Demo full lifecycle UIUX evidence manifest

| Field | Value |
| --- | --- |
| Spec | `frontend/e2e/demo-full-lifecycle-uiux-evidence.spec.ts` |
| Functional baseline | `frontend/e2e/demo-full-lifecycle.spec.ts` |
| Viewport | 1440×900 desktop |
| Brands | REDBC (API management), GREENBC (template API access tab) |

## Verdict

**Verdict: PASS** (3/3 screenshots; REDBC + GREENBC; 2026-07-03)

## Scenarios

Published demo template `DEMO-FULL-FLOW-LETTER` after full lifecycle seed (draft → test → approval → publish → API policy).

## Screenshots

| # | File | Surface | Brand |
| --- | --- | --- | --- |
| 1 | `01-api-management-home-redbc-1440x900.png` | API management catalog with published full-flow row | REDBC |
| 2 | `02-api-policy-detail-domains-redbc-1440x900.png` | API policy detail domain navigation | REDBC |
| 3 | `03-template-api-access-tab-greenbc-1440x900.png` | Template hub → External access policy summary | GREENBC |

## Checks

- No forbidden L1 tokens on primary API management surfaces
- Published template visible in API management list
- Domain nav shows five policy domains
- Template API access tab shows AD group + release version summary
