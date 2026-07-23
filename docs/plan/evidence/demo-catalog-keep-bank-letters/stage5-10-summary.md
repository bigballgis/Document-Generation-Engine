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

## Re-verify (2026-07-24 build-deploy-agent, post-merge)

- Queue idle; stack healthy; running backend jar **SEEDERS_ABSENT**.
- Live API inventory still **8 KEEP / PURGE absent** — see `stage5-10-reverify-2026-07-24.md`.
- No new deploy queued (SkipBuild not required). Cleanup/import not re-run (pwsh ENOENT mid-session).

## Next

Stage 6 E2E **14/14** PASS → merge `0e6d0bad` → Stage 12/13. Follow-ups remain: orphan SQL schema mismatch (BDD-004/005 not fully automated); cleanup pagination workaround.
