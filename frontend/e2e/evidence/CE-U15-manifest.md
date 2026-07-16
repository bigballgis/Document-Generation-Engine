# CE-U15 Functional Evidence Manifest — Lifecycle Stepper + Go fix

**Task:** CE-U15 / Task Master **#91** — Dev workspace lifecycle Stepper + publish readiness Go-fix deep links  
**Slice:** `ce-u15-lifecycle-stepper` (`feat/ce-u15-lifecycle-stepper`)  
**Worktree:** `D:/working/DGE-ce-u15-lifecycle-stepper`  
**BDD:** [docs/behavior/ce-u15-lifecycle-stepper.md](../../../docs/behavior/ce-u15-lifecycle-stepper.md) (`ready`)  
**Date:** 2026-07-17  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS**

## Test execution

| Spec | Result |
| --- | --- |
| `CE-U15-lifecycle-stepper.spec.ts` — BDD-CE-U15-LSS-001 | **passed** |
| `CE-U15-lifecycle-stepper.spec.ts` — BDD-CE-U15-LSS-002 | **passed** |
| `CE-U15-lifecycle-stepper.spec.ts` — BDD-CE-U15-LSS-004 | **passed** |
| `CE-U15-lifecycle-stepper.spec.ts` — BDD-CE-U15-LSS-006 | **passed** |
| `CE-U15-lifecycle-stepper.spec.ts` — BDD-CE-U15-LSS-010 | **passed** |

```powershell
pnpm -C frontend exec playwright test `
  e2e/CE-U15-lifecycle-stepper.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 5 passed (1.0m)
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/ce-u15-stage6-e2e/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| LSS-001 DRAFT Stepper | Author opens DRAFT `/templates/{id}/dev/{devVersionId}` → `data-testid=lifecycle-stepper`; Draft `aria-current=step`; upcoming steps `is-upcoming`; no Submit/Approve/Publish CTAs on stepper |
| LSS-002 status advance | TESTING → Testing current + Draft completed; APPROVAL/PENDING_DECISION → Pending approval; PENDING_RELEASE → Pending release |
| LSS-004 Go fix ANCHOR_INTEGRITY | publishReadiness + pending `ANCHOR_INTEGRITY` → English **Go fix** (`publish-gate-go-fix-ANCHOR_INTEGRITY`) → `workspaceTab=design&designTab=bindings` |
| LSS-006 mapping sample | COVERAGE_THRESHOLDS → testing/coverage; FIDELITY_WARNINGS_VIEWED → testing/previewRuns; CONTENT_MODULE_EFFECTIVE_EXPIRED → design/contentModules |
| LSS-010 U14 regression | Dashboard Tasks (`?queue=TEST`) still has **zero** `lifecycle-stepper` / `[data-ce-u15-stepper]` |

### Fixture notes

- DRAFT: `prepareDraftTemplateWithCleanBinding`
- TESTING / PENDING_DECISION / PENDING_RELEASE: CE-U14 helpers (`prepareRetailTemplateInTesting`, `prepareTemplatePendingApprovalDecision`, `prepareTemplatePendingRelease`)
- Go-fix pending checklist items: Playwright `page.route` forces selected `publish-gate` checkCodes to `ready=false` (gate evaluation algorithm out of CE-U15 scope)

## Artifacts added / updated

- `frontend/e2e/CE-U15-lifecycle-stepper.spec.ts` (new)
- `frontend/e2e/evidence/CE-U15-manifest.md` (this file)
- `docs/plan/evidence/ce-u15-stage6-e2e/CE-U15-stage6-e2e-manifest.md`

## Notes for e2e-uiux-reviewer (stage 7)

1. Capture dual-brand @1920 screenshots: DRAFT stepper; PENDING_RELEASE publish readiness + Go fix row; post-Go-fix design/bindings.
2. Confirm Stepper sits above Workspace Tab Shell / page header; does not collide with action rail.
3. Dashboard Tasks must remain stepper-free (U14 regression).
4. English-first labels: Workflow progress / Draft…Published / Go fix; zh-CN 前往修复 optional spot-check.
5. No merge / MAIN doc-sync from stage 6.
