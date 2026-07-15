# CE-U09 Functional Evidence Manifest — Master review reachability

**Task:** CE-U09 / Task Master **#84** — Hub Submit/Approve/Reject + dashboard `workspaceTab=approval` deep link  
**Slice:** `ce-u09-master-review-reachability` (`feat/ce-u09-master-review-reachability`)  
**BDD:** [docs/behavior/ce-u09-master-review-reachability.md](../../../docs/behavior/ce-u09-master-review-reachability.md) (`ready`)  
**Date:** 2026-07-15  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (healthz **UP**; CE-U09 worktree images)  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U09-master-review-reachability.spec.ts` — BDD-CE-U09-MRR-001 | **passed** |
| `CE-U09-master-review-reachability.spec.ts` — BDD-CE-U09-MRR-002 | **passed** |
| `CE-U09-master-review-reachability.spec.ts` — BDD-CE-U09-MRR-003 | **passed** |
| `CE-U09-master-review-reachability.spec.ts` — BDD-CE-U09-MRR-004 | **passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U09-master-review-reachability.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 4 passed (13.6s)
```

**HTML report:** `frontend/playwright-report/docker/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| MRR-001 Hub Submit for review | Designer opens DRAFT Hub → header **Submit for review** + journey CTA → submit → `PENDING_REVIEW` |
| MRR-002 Hub Approve | Group admin opens pending Hub → header **Approve** / **Reject** → Approve → `APPROVED` |
| MRR-003 fail-closed | Designer on pending Hub has no Approve/Reject/Submit in header actions |
| MRR-004 Dashboard deep link | Group admin Tasks `master-review` Open → `/masters/{id}/revisions/{line}?workspaceTab=approval` with approval tab active |

## Artifacts added

- `frontend/e2e/CE-U09-master-review-reachability.spec.ts`
- Fixture helpers in `frontend/e2e/helpers/masters-api.ts`:
  - `createDraftMasterForHubSubmit`
  - `createPendingReviewMasterForDecide` (designer submit → admin decide, same-person safe)
  - `getMasterDetailViaApi`

## Notes

1. Stage-6 gate covers MRR-001…004 (key Hub + dashboard journeys). MRR-005…007 covered by unit tests / code review.
2. Pending fixtures are created/submitted as `E2E_MASTER_DESIGNER` so `E2E_GROUP_ADMIN` can approve.
3. No merge performed (stage 6/7 handoff only).
