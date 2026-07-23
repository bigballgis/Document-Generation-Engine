# Slim demo catalog — keep bank-letter Live set

**Program / slice:** `demo-catalog-keep-bank-letters` (ad-hoc **NON-CE** ops-demo cleanup leaf; **not** a formal P-phase; **not** IBL Wave B; **not** CE-O02)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#164** (`demo-catalog-keep-bank-letters`) → **Done**  
**Active delivery slice:** none (**sole-active cleared**)  
**Placement:** **ISOLATED** · worktree **REMOVED** · branch `feat/demo-catalog-keep-bank-letters` merged  
**Merge SHA:** `0e6d0bad` · feature commit `6e8cc8b3`  
**BDD:** [demo-catalog-keep-bank-letters.md](../../behavior/demo-catalog-keep-bank-letters.md) — **ready** (`BDD-DEMO-KEEP-001…014`); `frontend_ui_in_scope=false`  
**Ops runbook:** [demo-catalog-keep-bank-letters.md](../../operations/demo-catalog-keep-bank-letters.md)  
**Evidence:** [demo-catalog-keep-bank-letters/](../evidence/demo-catalog-keep-bank-letters/README.md)  
**Batch recommendation:** **solo** (`member_task_ids: ["164"]`; `proposed_slice_id: demo-catalog-keep-bank-letters`;
`shared_acceptance_surface: acceptance catalog after cleanup shows only keep-set`;
`evidence_amortization: mvn verify + docker-deploy-queue cleanup evidence + keep-set smoke`;
vetoes_applied: do-not-flip-3b-5a, do-not-mark-53-106-Done, do-not-touch-CE-O02, no-frontend-unless-empty-state;
`on_red_split_hint: Peel Java seeder retire vs script cleanup if verify fails`) — **closed**

**Upstream (Done, do not reopen):** Wave A TM **#141** [`bank-letter-demo-refresh`](./bank-letter-demo-refresh.md) · Wave B TM **#142** expand · PQH Leaf 4 TM **#163** Done (`b739a38f`)

---

## Purpose

Contract acceptance/screenshot demo catalog to the user-confirmed **8 Live bank-letter templates** and their referenced letterhead / standard clauses / asset-library assets; purge other DEMO noise packages and orphans; retire Java `ApplicationRunner` seeders that would reintroduce purged template IDs on reboot; keep deploy PowerShell import/publish as the authoritative keep-set load path.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared** |
| Umbrella #53 / #106 | Registry-only — **not** Done (veto held) |
| Gate evidence | `mvn verify` SUCCESS; FE lint/type-check/test/build GREEN; `docker-deploy-queue` DEPLOY_OK; cleanup keep-8 PUBLISHED + `purge_absent`; **focused** Stage 6 demos E2E **14/14** PASS (not full suite); architecture **PASS_WITH_NOTES**; CQ **PASS_WITH_NOTES** |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119**; touch CE-O02; invent frontend empty-state work; claim go-live / IBL / CE Done |

---

## KEEP / PURGE inventory (delivered)

### KEEP-8 Live templates + shared

| # | externalId | Deploy package |
| --- | --- | --- |
| 1 | `DEMO-COVENANT-WAIVER` | `deploy/demo-covenant-waiver` |
| 2 | `DEMO-FORMAL-DEMAND` | `deploy/demo-formal-demand` |
| 3 | `DEMO-COMMITMENT-LETTER` | `deploy/demo-commitment` |
| 4 | `DEMO-FACILITY-AMENDMENT` | `deploy/demo-facility-amendment` |
| 5 | `DEMO-ANNUAL-REVIEW` | `deploy/demo-annual-review` |
| 6 | `DEMO-FACILITY-RENEWAL` | `deploy/demo-annual-review` |
| 7 | `DEMO-CREDIT-LIMIT-CONFIRM` | `deploy/demo-credit-limit` |
| 8 | `CORP-FOL-OFFER` | `deploy/demo-fol` |

**Retain:** `deploy/demo-shared/` (bank style + runtime generate helpers scoped to KEEP).

### PURGE packages removed from repo

`demo-retail-account`, `demo-mortgage`, `demo-trade-lc`, `demo-collection`, `demo-wealth`, `demo-kyc-cdd`, `demo-account-closure`, `demo-insurance-endorsement`

### Java classes

| Policy | Classes |
| --- | --- |
| **Removed** | `DemoCatalogSeeder`, `DemoFullFlowCatalogSeeder`, `DemoFullFlowPublishSupport`, `DemoCatalogSeedProperties` |
| **Retained** | `DemoAssetLibrarySeeder`, `CatalogLoadSeeder`, `DemoCatalogSessions`, `DemoRetailLetterheadDocxBuilder`, `DemoDocxFactory` |

Authoritative load path: PowerShell `import-all-demos.ps1` → `publish-all-demos.ps1` (optional `generate-all-demos.ps1`) — **not** Java template-family auto-seed.

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| DEMO-KEEP-T01 | Plan/TM sole-active activation + detail/ledger/index cross-links | **Done** |
| DEMO-KEEP-T02 | Doc-keeper: package/README/ops note + indexes for keep-set / seeder retirement | **Done** |
| DEMO-KEEP-T03 | Backend/ops: purge demos + orphans; retire unused Java seeders/scripts (BDD-DEMO-KEEP-001…014) | **Done** (templates+masters keep-set proven; orphan CM/asset SQL follow-up recorded) |
| DEMO-KEEP-T04 | `mvn verify` + queued docker-deploy cleanup evidence + keep-set smoke | **Done** |
| DEMO-KEEP-T05 | Stage 11 merge + MAIN doc-sync + commit-review | **Done** for merge + Stage 12 sync (`0e6d0bad`); Stage 13 owns commit/push |

### Task Master members

| TM | Alias | Title | Status |
| --- | --- | --- | --- |
| **#164** | `demo-catalog-keep-bank-letters` | Slim demo catalog keep 8 bank-letter Live templates | **Done** (`0e6d0bad`) |

### Related (closed)

| TM | Alias | Status |
| --- | --- | --- |
| **#141** | Wave A refresh | **Done** (`aa88170f` / `5ae9575a`) |
| **#142** | Wave B expand | **Done** (`288ce98f`) |
| **#163** | PQH-F7 | **Done** (`b739a38f`) |

---

## Exit criteria (from BDD-DEMO-KEEP-001…014)

| # | Criterion | Status |
| --- | --- | --- |
| 001–002, 006–014 (keep-set / purge templates / registries / seeders / smoke) | Acceptance catalog KEEP-8 + purge absent + registries narrowed + Java seeders retired + generate smoke | **Done** (evidence under [demo-catalog-keep-bank-letters/](../evidence/demo-catalog-keep-bank-letters/)) |
| 004–005 (orphan CM / asset-library) | Soft-delete orphans only when unreferenced | **Partial / not fully automated** — Stage 10 SQL used table names absent in this DB (`content_module_ref` / `asset_library_item`); do **not** claim full BDD-004/005 automation |
| Locks | #53 / #106 / #3b/#5a / CE-O02 held; Wave A/B stay Done | **Held** |

### Architecture / CQ follow-ups (honest — not blocking Done)

| Follow-up | Note |
| --- | --- |
| Orphan SQL schema mismatch | Cleanup SQL relation names do not match this acceptance schema; CM/asset orphan purge not proven |
| Cleanup pagination | Official `cleanup-demo-catalog-keep-list.ps1` page-size ~100; Stage 10 used paginated DELETE workaround for 500+ rows |
| Residual E2E purged IDs | Focused Stage 6 demos **14/14** PASS only — **do not** claim full `test:e2e:docker` green; broader helpers may still reference purged IDs (`DEMO_FULL_FLOW_*` etc.) until a later retarget leaf |

---

## Owners (pipeline)

1. **doc-keeper** (stage 3) → **Done**  
2. **backend-engineer** (stage 4) → **Done**  
3. **build-deploy-agent** / **e2e-test-engineer** / reviews / **integration-merger** → **Done**  
4. **post-task-doc-sync** (stage 12) → **Done** (this sync)  
5. **post-task-commit-review** (stage 13) → **next** (owns commit/push)

---

## Vetoes (hard)

- Checklist **#3b / #5a** — do **not** flip **GO**
- Umbrella **#53** / **#106** — do **not** mark **Done**
- **CE-O02** — do **not** touch / activate
- **Frontend** — `frontend_ui_in_scope=false`; no empty-state UI
- Do **not** claim go-live / IBL / CE program Done
- Do **not** reopen P22/P23 for this ops leaf
