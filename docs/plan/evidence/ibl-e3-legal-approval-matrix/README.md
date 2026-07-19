# IBL-E3 Stage 6 — E2E functional evidence

| Field | Value |
| --- | --- |
| **Task** | #130 / IBL-E3 |
| **Slice** | `ibl-e3-legal-approval-matrix` |
| **Worktree** | `D:/working/DGE-ibl-e3-legal-approval-matrix` |
| **Date** | 2026-07-20 |
| **Config** | `frontend/playwright.docker.config.ts` @ `:4173` / `:8080` |
| **Verdict** | **PASS** (4/4, ~44.6s) |
| **LEGAL_REVIEWER seed** | `10000009` / `ChangeMe123!` (V71) |

## Spec

`frontend/e2e/ibl-e3-legal-approval-matrix.spec.ts`

| Test | BDD |
| --- | --- |
| Author configures `LEGAL_THEN_COMPLIANCE`; Legal stage CTA gating | BDD-IBL-E3-015 (+ mode save / E3-C2) |
| LEGAL queue tab + deep-link to decision surface | BDD-IBL-E3-016 |
| LEGAL Approve → COMPLIANCE Approve → `PENDING_RELEASE` | BDD-IBL-E3-005 / 006 |
| Role-gated CTAs on COMPLIANCE + mode lock 422 | BDD-IBL-E3-009 / 010 / 003 |

Helpers: `prepareTemplatePendingLegalDecision` / `prepareTemplatePendingComplianceDecision` in `frontend/e2e/helpers/submit-approval-gate-api.ts`.

## Command

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test e2e/ibl-e3-legal-approval-matrix.spec.ts `
  --config playwright.docker.config.ts --workers=1 --trace on
```

## Artifacts

| Kind | Path |
| --- | --- |
| Summary | [latest-summary.json](./latest-summary.json) |
| HTML report | [report/index.html](./report/index.html) |
| Traces | `traces/*-trace.zip` |
| Digests | [digests.txt](./digests.txt) |
| Also | `frontend/playwright-report/docker/` |

## Defect fixed during Stage 6 (for FE awareness)

`LEGAL_REVIEWER` login landed on `/forbidden` because client `routeCapabilities` omitted:

- `rolesAllowRoute` LEGAL_REVIEWER set (dashboard / template / asset-library)
- `templateManagement` capability check for `canDecideLegalApprovals`

Fixed in `frontend/src/auth/routeCapabilities.ts` (+ unit test) and redeployed (`COMPOSE_PROJECT_NAME=documentgenerationengine`).

## Gaps for Stage 7 (`e2e-uiux-reviewer`)

- Dual-brand REDBC/GREENBC visual evidence @1440×900
- Stage indicator + Approve/Reject action rail polish
- LEGAL queue tablist / partition a11y
- Overflow / Bank OA chrome on multi-stage banners

## Next

Stage **7** — `e2e-uiux-reviewer`

**Does NOT claim:** Task #130 Done / Wave E Done / go-live.
