# Evidence — demo-catalog-keep-bank-letters (#164)

**Status:** **Done** (2026-07-24) — MAIN merge `0e6d0bad`; feature `6e8cc8b3`; worktree **REMOVED**; sole-active **cleared**  
**Slice:** `demo-catalog-keep-bank-letters` · Task Master **#164** → **Done**  
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
| [templates-after-paginated-delete.txt](./templates-after-paginated-delete.txt) | Paginated DELETE workaround note |
| [stage5-10-summary.md](./stage5-10-summary.md) | Human summary + cleanup pagination + orphan SQL notes |
| [stage5-10-reverify-2026-07-24.md](./stage5-10-reverify-2026-07-24.md) | Post-merge re-verify: queue idle; KEEP-8 API; seeders absent from running JAR |
| [stage5-10-reverify-2026-07-24.md](./stage5-10-reverify-2026-07-24.md) | Post-merge re-verify: queue idle, seeders absent, live KEEP-8 / PURGE absent; cleanup re-run blocked by pwsh ENOENT |
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

## Gates cited

| Gate | Result |
| --- | --- |
| `mvn verify` | SUCCESS |
| FE lint / type-check / test / build | GREEN |
| `docker-deploy-queue` | DEPLOY_OK |
| Cleanup | keep-8 PUBLISHED; `purge_absent=true` |
| Stage 6 E2E | 14/14 PASS |
| Architecture | PASS_WITH_NOTES |
| Code quality | PASS_WITH_NOTES |

## Honest follow-ups (architecture / CQ notes — not Done blockers)

| Follow-up | Honesty |
| --- | --- |
| Orphan SQL schema mismatch | Stage 10 CM/asset orphan SQL hit absent relations (`content_module_ref` / `asset_library_item`) — **do not** claim BDD-DEMO-KEEP-004/005 fully automated |
| Cleanup pagination | Official script page-size ~100; Stage 10 used paginated DELETE for 500+ rows |
| Residual E2E purged IDs | Focused demos suite green; broader helpers may still reference purged IDs until a later retarget leaf |

## Hard vetoes

Do **not** flip checklist **#3b/#5a GO**; do **not** mark **#53** / **#106** Done; do **not** touch CE-O02; do **not** invent Word-host evidence; do **not** claim go-live / IBL / CE Done.

## Next

Stage 12 doc-sync complete → **post-task-commit-review** (Stage 13). UIUX Stage 7 **skipped** (`frontend_ui_in_scope=false`).
