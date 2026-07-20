# CONTENT-SPOTCHECK — bank-letter-demo-refresh (Wave A / #141)

**When:** 2026-07-20 (post Java-fix image redeploy — durable path, no SQL hotfix)  
**Worktree:** `D:/working/DGE-bank-letter-demo-refresh` · `feat/bank-letter-demo-refresh`  
**Path:** `docker-deploy-queue` (Java image rebuilt) → `import-all-demos` → `publish-all-demos` → `generate-all-demos`

## Generate summary

| Result | Count | Notes |
| --- | ---: | --- |
| SUCCESS | **13 / 13** | All manifest content markers + size floors + no placeholder leak |
| FAILED | **0 / 13** | — |

**SQL hotfix:** none in this run. Durability proven on redeployed backend image containing publish STOP + release_version finder fix.

## Root cause (Stage 4 — fixed in image)

1. **HTTP 500 INTERNAL_ERROR (previously 6–7 templates):** demo re-import cloned a new in-flight line then re-published as `1.0.0` without superseding the prior PUBLISHED row. Runtime `findByTemplateIdAndReleaseVersion` returned two rows → `NonUniqueResultException` → swallowed as `INTERNAL_ERROR`.
2. **DEMO-FULL-FLOW-LETTER content marker miss:** `Ensure-DemoFullFlowCatalogContent` in `publish-all-demos.ps1` had overwritten HEADER with a shallow binding; fixed to Wave A Meridian letter JSON.

## Durable code / script fixes (in deployed image + worktree)

| File | Change |
| --- | --- |
| `TemplateLifecycleApprovalFlowSupport` | On publish, STOP prior PUBLISHED rows with the same `releaseVersion` |
| `TemplateVersionRepository` | `findByTemplateIdAndReleaseVersion` → latest `dev_version_number` (fail-soft for historical duplicates) |
| `TemplateLifecyclePublishVersionSelectionTest` | Regression: same-release publish stops prior row (2 tests GREEN in `mvn verify`) |
| `deploy/publish-all-demos.ps1` | Full-flow HEADER binding = Wave A Meridian letter JSON |

## Successful DOCX — sizeBytes + markers (post-redeploy)

| externalId | sizeBytes | Meridian | Placeholder leak (`{{` / TODO / lorem) |
| --- | ---: | --- | --- |
| CORP-FOL-OFFER | 30162 | yes | no |
| DEMO-FULL-FLOW-LETTER | 4775 | yes | no |
| DEMO-RETAIL-ACCOUNT-OPEN | 6866 | yes | no |
| DEMO-RETAIL-ACCOUNT-BALANCE | 6359 | yes | no |
| DEMO-MORTGAGE-APPROVAL | 9053 | yes | no |
| DEMO-CREDIT-LIMIT-CONFIRM | 9710 | yes | no |
| DEMO-TRADE-LC-NOTICE | 8018 | yes | no |
| DEMO-TRADE-GUARANTEE-NOTICE | 6960 | yes | no |
| DEMO-RATE-CHANGE-NOTICE | 6190 | yes | no |
| DEMO-OVERDUE-COLLECTION | 6485 | yes | no |
| DEMO-ANNUAL-REVIEW | 7218 | yes | no |
| DEMO-FACILITY-RENEWAL | 7057 | yes | no |
| DEMO-WEALTH-STATEMENT | 8733 | yes | no |

Artifacts: `docs/plan/evidence/bank-letter-demo-refresh/generated_<externalId>.docx` · machine manifest [generated-docx-manifest.json](./generated-docx-manifest.json) · publish [all-demos-publish-summary.json](./all-demos-publish-summary.json) (13/13) · [spotcheck-sizes.json](./spotcheck-sizes.json).

## Gates note

- Unit: `TemplateLifecyclePublishVersionSelectionTest` **GREEN** (2 tests) inside full `mvn verify` (2312 / 0 fail / 15 skipped).
- Wave A generate evidence: **13/13 SUCCESS** on live `:8080` stack **after** Java image redeploy, **without** SQL hotfix.
