# TM #144 Functional Evidence Manifest — Published template test artifacts

**Task:** Task Master **#144** — `published-template-test-artifacts`  
**Slice:** `published-template-test-artifacts` (`feat/published-template-test-artifacts`)  
**Worktree:** `D:/working/DGE-published-template-test-artifacts`  
**BDD:** [docs/behavior/published-template-test-artifacts.md](../../../docs/behavior/published-template-test-artifacts.md) (`ready`)  
**Date:** 2026-07-21  
**Stack:** Docker frontend `http://127.0.0.1:4173` + backend `http://127.0.0.1:8080` (stage-5 DEPLOY_OK)  
**Verdict:** **PASS** (4/4)

## Test execution

| Spec | Result |
| --- | --- |
| `TM144-published-template-test-artifacts.spec.ts` — BDD-PTA-001 | **passed** |
| `TM144-published-template-test-artifacts.spec.ts` — BDD-PTA-002 | **passed** |
| `TM144-published-template-test-artifacts.spec.ts` — BDD-PTA-005 | **passed** |
| `TM144-published-template-test-artifacts.spec.ts` — BDD-PTA-006 | **passed** |

```powershell
$env:E2E_TARGET='docker'; $env:FRONTEND_PORT='4173'
pnpm -C frontend exec playwright test `
  e2e/TM144-published-template-test-artifacts.spec.ts `
  --config playwright.docker.config.ts --workers=1
# 4 passed (19.3s)
```

**HTML report:** `frontend/playwright-report/docker/`  
**Plan evidence mirror:** `docs/plan/evidence/published-template-test-artifacts-stage6-e2e/`

## Acceptance coverage

| BDD | Evidence |
| --- | --- |
| PTA-001 Preview + batch history; no authoring runs | `/templates/{id}/releases/{version}?workspaceTab=testing` → `[data-testid=release-testing-readonly]` + `.batch-test-history` + `.preview-run-history`; zero Run preview / Full test; no `.test-data-set-panel` / `#dev-workspace` |
| PTA-002 SUCCEEDED DOCX/PDF download | Select SUCCEEDED preview row → Download DOCX/PDF buttons enabled → GET `.../previews/{id}/artifacts/{docx\|pdf}` **ok** (not 401/403/410) |
| PTA-005 Open preview selects history row | Batch history expand + `[data-testid=batch-history-open-preview]` → `.preview-run-history tr.is-selected` with enabled Download DOCX |
| PTA-006 Open data set read-only feedback | `[data-testid=batch-history-open-data-set]` → English info toast (`Data sets are not editable on published release detail…`); stay on release Testing; no authoring Data sets |

### Optional not run

| BDD | Status |
| --- | --- |
| PTA-003 STOPPED/DEPRECATED | **Skipped** — no STOPPED/DEPRECATED release lines with SUCCEEDED preview seed on this stack |
| PTA-004 BE sampleResults persistence | Out of scope for FE E2E (BE unit/API) |
| PTA-007 / PTA-008 / PTA-009 | Covered indirectly (fail-closed not expanded; no PUBLISHED download block observed; no authoring controls re-enabled) |

### Fixture notes

- Fresh `preparePublishedTemplate*` / lifecycle publish fixtures currently **422** `TEMPLATE_VALIDATION_FAILED` on `approval-decision` on this Docker stack.
- Stage 6 uses catalog **PUBLISHED** demos with durable SUCCEEDED preview artifacts (prefer `CORP-FOL-OFFER`, then other DEMO-* published lines).
- Batch `sampleResults` on demo history often lack `previewId` (pre-fix / sync path) — PTA-005/006 inject sampleResults via Playwright route when needed (documented residual for real async-batch seed).

## Artifacts added / updated

- `frontend/e2e/TM144-published-template-test-artifacts.spec.ts` (new)
- `frontend/e2e/evidence/TM144-published-template-test-artifacts-manifest.md` (this file)
- `docs/plan/evidence/published-template-test-artifacts-stage6-e2e/TM144-stage6-e2e-manifest.md`

## Residuals for e2e-uiux-reviewer (stage 7)

1. Dual-brand @1920: release Testing read-only surface — preview history + batch history + read-only summary copy.
2. English-first: Preview run history / Test run history / Download DOCX|PDF / Open preview / Open data set toast.
3. Confirm selected preview row highlight (`is-selected`) is visible under bank OA tokens after Open preview.
4. Confirm toast placement does not obscure download actions.
5. Do **not** assert `showAuthoringSection` true for PUBLISHED.
6. No merge / MAIN doc-sync from stage 6.
