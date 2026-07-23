# Evidence — demo-catalog-keep-bank-letters (#164)

**Status:** Stage **6** E2E functional **PASS** (2026-07-24) — **not Done** (no merge; do not flip #3b/#5a)  
**Slice:** `demo-catalog-keep-bank-letters` · Task Master **#164** → **In Progress**  
**Behavior SoT:** [demo-catalog-keep-bank-letters.md](../../../behavior/demo-catalog-keep-bank-letters.md) (`BDD-DEMO-KEEP-001`…`014`)  
**Ops runbook:** [demo-catalog-keep-bank-letters.md](../../../operations/demo-catalog-keep-bank-letters.md)  
**Shared helpers / KEEP SoT:** [deploy/demo-shared/README.md](../../../../deploy/demo-shared/README.md)  
**Plan detail:** [demo-catalog-keep-bank-letters.md](../../detail/demo-catalog-keep-bank-letters.md)

## Intent

Archive post-cleanup acceptance evidence for the slim screenshot catalog (**8** keep externalIds only): catalog membership, publish/generate summaries (`expectedCount=8`), fail-closed dependency check notes, and reboot seeder silence.

## Stage 5+10 artifacts (this folder)

| Artifact | Notes |
| --- | --- |
| [deploy-status.json](./deploy-status.json) | Queued deploy `DEPLOY_OK`; `/healthz` UP; `:4173` 200 |
| [templates-before-cleanup.json](./templates-before-cleanup.json) | Before: 522 unique externalIds; 8 keep present |
| [templates-after-cleanup.json](./templates-after-cleanup.json) | After: **8** keep only; `purge_absent=true`; all `PUBLISHED` |
| [stage5-10-summary.md](./stage5-10-summary.md) | Human summary + cleanup pagination note |
| [cleanup-whatif.log](./cleanup-whatif.log) / [cleanup-real.log](./cleanup-real.log) / [cleanup-masters.log](./cleanup-masters.log) | Cleanup runs |
| [cleanup-paginated-delete.log](./cleanup-paginated-delete.log) | Paginated DELETE (API page size ~100 workaround) |
| [all-demos-publish-summary.json](./all-demos-publish-summary.json) | `expectedCount=8` / 8 published |
| [publish-all-demos.log](./publish-all-demos.log) | Publish chain including `DEMO-COVENANT-WAIVER` DRAFT→PUBLISHED |

## Stage 6 artifacts

| Artifact | Notes |
| --- | --- |
| [stage6-e2e-summary.md](./stage6-e2e-summary.md) | Playwright docker PASS 14/14; keep-set + runtime generate |
| `frontend/playwright-report/docker/` | HTML report from Stage 6 run |

## Keep-set confirmed

`DEMO-COVENANT-WAIVER`, `DEMO-FORMAL-DEMAND`, `DEMO-COMMITMENT-LETTER`, `DEMO-FACILITY-AMENDMENT`, `DEMO-FACILITY-RENEWAL`, `DEMO-ANNUAL-REVIEW`, `DEMO-CREDIT-LIMIT-CONFIRM`, `CORP-FOL-OFFER`

## Hard vetoes

Do **not** flip checklist **#3b/#5a GO**; do **not** mark **#53** / **#106** Done; do **not** touch CE-O02; do **not** invent Word-host evidence; do **not** claim go-live / IBL / CE Done.

## Next

`architecture-reviewer` (Stage 8). UIUX Stage 7 **skip** (`frontend_ui_in_scope=false`).
