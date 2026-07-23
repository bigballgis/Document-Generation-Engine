# Stage 5+10 re-verify — build-deploy-agent (#164)

**Captured:** 2026-07-23T20:19Z–20:25Z (session)  
**Worktree (requested):** `D:/working/DGE-demo-catalog-keep-bank-letters` (`feat/demo-catalog-keep-bank-letters`) — **already merged & removed** by the time this re-verify finished (MAIN evidence README: merge `0e6d0bad`, feature `6e8cc8b3`, worktree REMOVED).  
**Agent:** build-deploy-agent · Stages 5+10 re-check  
**Not claiming Done** from this subagent (slice already marked Done on MAIN evidence README).

## Deploy queue

| Check | Result |
| --- | --- |
| `.\scripts\docker-deploy-queue.ps1 -Status` | `DEPLOY_QUEUE: idle (no lock)`; Pending ticket files: **0** |
| Redeploy this session | **Not queued** — SkipBuild / no rebuild |

### SkipBuild rationale (documented)

- Acceptance stack already healthy: `http://localhost:8080/healthz` → **200** `{"status":"UP"}`; `http://localhost:4173/` → **200**.
- Running image `documentgenerationengine-docgen-backend:latest` (`sha256:a885fee7ba04…`, created **2026-07-23T19:50:27Z**) — `/app/app.jar` ZipFile listing: **`SEEDERS_ABSENT_FROM_JAR`** (`DemoCatalogSeeder` / `DemoFullFlowCatalogSeeder` not present).
- Catalog already slimmed to KEEP-8 against existing DB; full rebuild not required for cleanup verification against current inventory.
- Prefer-full-deploy rule satisfied: seeders already retired in the running image from prior queued deploy of this leaf.

## Live catalog inventory (API, this session)

Auth: `POST /api/management/v1/auth/login` as `10000001` (same as cleanup script).  
List: `GET /api/management/v1/templates?size=500`.

| Metric | Value |
| --- | --- |
| `total` unique externalIds | **8** |
| KEEP present | **8/8** |
| PURGE sample absent | **true** |

KEEP externalIds observed:

```
CORP-FOL-OFFER
DEMO-ANNUAL-REVIEW
DEMO-COMMITMENT-LETTER
DEMO-COVENANT-WAIVER
DEMO-CREDIT-LIMIT-CONFIRM
DEMO-FACILITY-AMENDMENT
DEMO-FACILITY-RENEWAL
DEMO-FORMAL-DEMAND
```

PURGE IDs checked absent (none in list):  
`DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-RETAIL-ACCOUNT-BALANCE`, `DEMO-MORTGAGE-APPROVAL`, `DEMO-TRADE-LC-NOTICE`, `DEMO-TRADE-GUARANTEE-NOTICE`, `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION`, `DEMO-WEALTH-STATEMENT`, `DEMO-KYC-CDD-NOTICE`, `DEMO-ACCOUNT-CLOSURE`, `DEMO-INSURANCE-ENDORSEMENT`, `DEMO-FULL-FLOW-LETTER`, `DEMO-RETAIL-LETTER`.

## Cleanup / import this session

| Step | Status |
| --- | --- |
| `cleanup-demo-catalog-keep-list.ps1 -WhatIf` | **Blocked** mid-session — host `pwsh.exe` spawn `ENOENT` |
| `cleanup-demo-catalog-keep-list.ps1` (real) | **Blocked** (same) |
| `import-all-demos.ps1` | **Not run** this session — KEEP-8 already present; prior publish evidence in `all-demos-publish-summary.json` (`expectedCount=8`, 8/8) |

Prior durable cleanup evidence (same folder): `templates-before-cleanup.json` (522 → keep), `templates-after-cleanup.json` (`purge_absent=true`, all PUBLISHED), `stage5-10-summary.md`, `deploy-status.json` (`DEPLOY_OK`).

## Blockers

1. **Shell runtime failure:** after initial health/inventory/seeder checks, Cursor shell could not spawn `C:\Program Files\PowerShell\7\pwsh.exe` (`ENOENT`). Prevented WhatIf → real cleanup re-run and log refresh.
2. **Worktree path:** subsequent probes could not resolve `D:/working/DGE-demo-catalog-keep-bank-letters` (path missing / inaccessible from agent FS). Evidence updates written on MAIN evidence path that already holds #164 artifacts.
3. **Browser MCP:** navigate to localhost healthz failed (`No browser tab available`) — could not substitute for shell API checks after pwsh died.

## Return summary

- **deploy:** `DEPLOY_OK` (existing stack; queue idle; no new deploy required)
- **cleanup exit code (this session):** n/a — blocked by pwsh ENOENT (prior evidence + live inventory confirm KEEP-8 / PURGE absent)
- **inventory paths:** `templates-before-cleanup.json`, `templates-after-cleanup.json`, this reverify note
- **import-all-demos this session:** no
- **merge / Done:** not claimed
