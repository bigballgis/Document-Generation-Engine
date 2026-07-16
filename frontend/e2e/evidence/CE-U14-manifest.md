# CE-U14 Functional Evidence Manifest — Dashboard lifecycle todos

**Task:** CE-U14 / Task Master **#90** — Dashboard Tasks TEST / APPROVAL / PENDING_RELEASE deep links to decision surfaces  
**Slice:** `ce-u14-dashboard-lifecycle-todos` (`feat/ce-u14-dashboard-lifecycle-todos`)  
**BDD:** [docs/behavior/ce-u14-dashboard-lifecycle-todos.md](../../../docs/behavior/ce-u14-dashboard-lifecycle-todos.md) (`ready`)  
**Date:** 2026-07-16  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (healthz **UP**; stage-5 deploy evidence)  
**Compose project:** `documentgenerationengine`  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-001 | **passed** |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-002 | **passed** |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-003 | **passed** |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-004 | **passed** |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-005 | **passed** |
| `CE-U14-dashboard-lifecycle-todos.spec.ts` — BDD-CE-U14-DLT-006 | **passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U14-dashboard-lifecycle-todos.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 6 passed (54.5s)
```

### Regression

| Spec | Result |
| --- | --- |
| `CDP-E2E-T02-tester-pass-decision.spec.ts` | **passed** (1/1) |
| `collaboration-todos.spec.ts` | **passed** (4/4) — tabbed Tasks + CE-U14 Open URL assertions |

```powershell
$env:E2E_TARGET='docker'; $env:E2E_BASE_URL='http://127.0.0.1:4173'
pnpm -C frontend exec playwright test `
  e2e/CDP-E2E-T02-tester-pass-decision.spec.ts `
  e2e/collaboration-todos.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**HTML report:** `frontend/playwright-report/docker/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| DLT-001 TEST deep link | Tester Tasks `?queue=TEST` → Open → `/templates/{id}/dev/{devVersionId}?workspaceTab=testing&testingTab=previewRuns`; **Confirm test pass** / **Record test failure** visible without tab switch |
| DLT-002 APPROVAL deep link | Approver Tasks → Open → `workspaceTab=approval&approvalTab=submitApproval`; **Approve** / **Reject** on action rail |
| DLT-003 PENDING_RELEASE deep link | Group admin Tasks → Open → `workspaceTab=approval&approvalTab=publishReadiness`; **Confirm go-live** visible |
| DLT-004 fail-closed | `TEMPLATE_AUTHOR` unfiltered Tasks has no `queue-TEST` / `queue-APPROVAL` / `queue-PENDING_RELEASE` partitions |
| DLT-005 resolve loop | Tester Pass via Open → TEST work item gone from API + Tasks |
| DLT-006 behavior entry | `/dashboard?queue=TEST#tasks-section` selects Waiting on my testing tab; single `queue-TEST` partition |

## Artifacts added / updated

- `frontend/e2e/CE-U14-dashboard-lifecycle-todos.spec.ts` (new)
- `frontend/e2e/helpers/lifecycle-ui.ts` — narrowed dual-path: Open lands on decision actions without extra Template testing / Template approval click
- `frontend/e2e/collaboration-todos.spec.ts` — `?queue=TEST` + CE-U14 Open URL; tabbed heading/partition assertions
- `frontend/e2e/P21-T01a-task-hub.spec.ts` — Open URL accepts `workspaceTab=testing`

## Notes for e2e-uiux-reviewer (stage 7)

1. Tabbed Dashboard: page `h1` stays **My tasks**; queue label is the selected tab (e.g. Waiting on my testing), not a replaced h1.
2. Capture dual-brand @1920 screenshots for: TEST queue + Open testing rail; APPROVAL or PENDING_RELEASE Open decision rail; author fail-closed (no lifecycle decision partitions).
3. Action rail selectors: `#dev-workspace` + `.workspace-tab-shell__actions` (Confirm test pass / Approve / Confirm go-live).
4. No CE-U15 Stepper DOM expected.
5. No merge performed (stage 6 handoff only).
