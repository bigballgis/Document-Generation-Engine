# Stage 5+10 evidence — demo-catalog-keep-bank-letters (#164)

**Captured:** 2026-07-23T20:09:46Z  
**Deploy:** DEPLOY_OK (`documentgenerationengine`)  
**Health:** `/healthz` = UP; frontend `:4173` = 200  

## Keep-set (must remain) — all present + PUBLISHED

| externalId | lifecycleStatus | releaseVersion |
| --- | --- | --- |
| CORP-FOL-OFFER | PUBLISHED | 1.0.0 |
| DEMO-ANNUAL-REVIEW | PUBLISHED | 1.0.0 |
| DEMO-COMMITMENT-LETTER | PUBLISHED | 1.0.0 |
| DEMO-COVENANT-WAIVER | PUBLISHED | 1.0.0 |
| DEMO-CREDIT-LIMIT-CONFIRM | PUBLISHED | 1.0.0 |
| DEMO-FACILITY-AMENDMENT | PUBLISHED | 1.0.0 |
| DEMO-FACILITY-RENEWAL | PUBLISHED | 1.0.0 |
| DEMO-FORMAL-DEMAND | PUBLISHED | 1.0.0 |

## Purge absent

`purge_absent` = **true** (sample: DEMO-FULL-FLOW-LETTER, DEMO-RETAIL-LETTER, retail/trade Meridian DEMO-* , LOAD-TPL-* — none present)

## Cleanup notes

- Official `deploy/cleanup-demo-catalog-keep-list.ps1` is page-size limited (~100). Acceptance had 500+ rows (LOAD-TPL + purge DEMO-*).
- Stage 10 used paginated DELETE (same API body as the script) then re-ran the official script for master soft-delete (8 keep masters retained).
- Content-module / asset-library SQL warnings: `content_module_ref` / `asset_library_item` relations absent in this DB (non-blocking; templates+masters OK).
- Before: `templates-before-cleanup.json` (522 unique externalIds, 8 keep present).
- After: `templates-after-cleanup.json` (8 keep only, all PUBLISHED).
- Publish: `all-demos-publish-summary.json` (`expectedCount=8`, 8/8 published; covenant waiver DRAFT→PUBLISHED).

## Stack

- `docgen-backend` healthy `:8080`
- `docgen-frontend` healthy `:4173`
- postgres / redis / minio / kafka healthy

## Next

`e2e-test-engineer` — keep-set smoke (do not merge / do not flip #3b/#5a).
