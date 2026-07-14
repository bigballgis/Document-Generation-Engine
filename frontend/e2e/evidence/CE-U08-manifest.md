# CE-U08 Functional Evidence Manifest — Content module review loop

**Task:** CE-U08 / Task Master **#83** — Dashboard CM pending/rework todos + rejectionReason versions column  
**Slice:** `ce-u08-content-module-review-loop` (`feat/ce-u08-content-module-review-loop`)  
**Tip:** `4b02787e`  
**BDD:** [docs/behavior/ce-u08-content-module-review-loop.md](../../../docs/behavior/ce-u08-content-module-review-loop.md) (`ready`)  
**Date:** 2026-07-15  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (healthz **UP**)  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U08-content-module-review-loop.spec.ts` — BDD-CE-U08-CMRL-001 | **passed** |
| `CE-U08-content-module-review-loop.spec.ts` — BDD-CE-U08-CMRL-002 | **passed** |
| `CE-U08-content-module-review-loop.spec.ts` — BDD-CE-U08-CMRL-003 | **passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U08-content-module-review-loop.spec.ts `
  --config playwright.docker.config.ts
# 3 passed (11.0s)
```

**HTML report:** `frontend/playwright-report/docker/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| CMRL-001 Dashboard pending-review todo | Approver sees `content-module-review` partition → Open → `/content-modules/{id}?workspaceTab=lifecycle` with Approve/Reject |
| CMRL-002 Dashboard rework todo | Author sees `content-module-rework` partition → Open → lifecycle with rejection reason + Submit for approval |
| CMRL-003 Versions rejectionReason column | API `rejectionReason` = `"Wording not acceptable"`; versions tab shows DRAFT + Rejection reason column text |

## Artifacts added

- `frontend/e2e/CE-U08-content-module-review-loop.spec.ts`
- Fixture helpers in `frontend/e2e/helpers/content-modules-api.ts`:
  - `createSubmittedContentModuleForReview`
  - `createRejectedContentModuleForRework`
  - `getContentModuleDetailViaApi`

## Notes

1. Minimum stage-6 gate is CMRL-001/002/003 (CMRL-004…007 deferred to optional follow-up / UIUX stage).
2. Hash `#tasks-section` refreshes CM workflow tasks after fixture seed (same pattern as CE-U07).
3. No merge performed (stage 6 handoff only).
