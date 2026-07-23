# Ops runbook — Slim demo catalog (keep bank-letter Live set)

**Status:** **Done** (2026-07-24) — MAIN merge `0e6d0bad`; acceptance cleanup + evidence archived; sole-active **cleared**  
**Last updated:** 2026-07-24  
**Slice / Task Master:** `demo-catalog-keep-bank-letters` · **#164** → **Done**  
**Behavior SoT (authoritative keep/purge):** [demo-catalog-keep-bank-letters.md](../behavior/demo-catalog-keep-bank-letters.md) (`BDD-DEMO-KEEP-001`…`014`; `bdd_readiness: ready`)  
**Plan detail:** [demo-catalog-keep-bank-letters.md](../plan/detail/demo-catalog-keep-bank-letters.md)  
**Shared helpers / acceptance SoT pointer:** [deploy/demo-shared/README.md](../../deploy/demo-shared/README.md)  
**Evidence home:** [docs/plan/evidence/demo-catalog-keep-bank-letters/](../plan/evidence/demo-catalog-keep-bank-letters/README.md)  
**Related:** [demo-acceptance-asset-seed.md](./demo-acceptance-asset-seed.md) (Wave 8 asset seed — retain opt-in; ≠ template catalog seed) · FOL-only precedent `deploy/demo-fol/cleanup-catalog-except-fol.ps1` (unsafe for KEEP-8; redirects to keep-8 cleanup)

---

## 1. Purpose

Contract the acceptance/screenshot demo catalog to the user-confirmed **8 Live bank-letter templates** and their referenced letterheads / standard clauses / asset-library assets. Purge other DEMO noise (templates, orphans, deploy packages) and retire Java `ApplicationRunner` seeders that would reintroduce purged IDs on reboot.

Authoritative load path for the keep-set: **deploy package + PowerShell import/publish** — not Java auto-seed.

---

## 2. Confirmed vs pending

| Kind | Content |
| --- | --- |
| **Confirmed (BDD / user 2026-07-24)** | Keep-list of 8 externalIds; purge-list of non-keep DEMO templates + 8 disk packages; fail-closed dependency checks; Java seeders for purged IDs must not reintroduce them on default boot; import/publish/generate registries must narrow to keep-set; vetoes below. |
| **Confirmed (ops note / historical)** | Why demo historically used Java ApplicationRunner + in-JVM DOCX builders (§3). Goal state: keep-set via deploy packages + import scripts; Java seeders disabled / not required by default. |
| **Delivered (#164 Done)** | `deploy/cleanup-demo-catalog-keep-list.ps1`; purge packages deleted from disk; registries narrowed to keep-8; purge Java seeders removed; acceptance cleanup keep-8 PUBLISHED + `purge_absent`; evidence under [demo-catalog-keep-bank-letters/](../plan/evidence/demo-catalog-keep-bank-letters/). |
| **Follow-ups (honest — not leaf reopen)** | (1) Orphan CM/asset SQL schema mismatch — **do not** claim BDD-DEMO-KEEP-004/005 fully automated; (2) cleanup script pagination (~100) — use paginated DELETE when catalog ≫ page size; (3) broader E2E helpers may still reference purged IDs. |
| **Out of scope (confirmed)** | Frontend empty-state UI (`frontend_ui_in_scope=false`); `CatalogLoadSeeder` / `LOAD-TPL-*`; flipping launch checklist **#3b/#5a**; marking **#53** / **#106** Done; touching **CE-O02**; claiming go-live / IBL / CE Done. |

Acceptance DB cleanup for this leaf **did run** (Stage 5/10); re-run the keep-list script after future noisy seeds if the catalog drifts.

---

## 3. Ops note — Historical Java seeders (confirmed)

**Why demo historically used Java classes**

1. **Boot-time ApplicationRunner auto-seed** — `DemoCatalogSeeder` / `DemoFullFlowCatalogSeeder` (`@ConditionalOnProperty docgen.demo-catalog.seed-enabled=true`) populated masters/templates on Spring Boot startup so acceptance/E2E stacks could obtain a minimal catalog **without** running PowerShell `deploy/demo-*/import-*-demo.ps1`.
2. **In-process DOCX builders** — helpers such as `DemoDocxFactory` / `DemoRetailLetterheadDocxBuilder` built letterhead DOCX bytes in-JVM (no pre-baked package asset required for those seed paths).
3. **Publish helpers in Java** — `DemoFullFlowPublishSupport` advanced full-flow draft content toward a published demo for executive/E2E journeys.

**Why those seeders are retired now**

| Driver | Confirmed fact |
| --- | --- |
| **Dual catalog sources** | Deploy-package import and Java auto-seed both created demo templates. After ops purged non-KEEP rows, a reboot (or `DOCGEN_SEED_DEMO_CATALOG=true`) could silently reintroduce purge IDs — especially Java-only `DEMO-FULL-FLOW-LETTER` / `DEMO-RETAIL-LETTER`. |
| **KEEP-8 acceptance SoT** | User confirmation 2026-07-24 contracted screenshot Live demos to **8** bank-letter templates. Wave B’s **20**-ID registry remains historical evidence of expand quality; it is **not** the default acceptance catalog size going forward. |
| **Single load path** | Canonical path = keep packages under `deploy/demo-*` + `import-all-demos.ps1` → `publish-all-demos.ps1` (optional `generate-all-demos.ps1`). Template-family Java seeders are retired so the catalog has one writer path. |

**Goal state for this leaf**

- Keep-set of **8** templates is **deploy package + import/publish scripts** driven.
- Java **template-family** seeders are **not required** for those eight (classes removed; default `seed-enabled=false`).
- Reboot must **not** reintroduce purged template IDs.
- `DemoAssetLibrarySeeder` stays opt-in (`DOCGEN_SEED_DEMO_ASSET_LIBRARY`, default `false`) for managed-asset bootstrap — must **not** re-seed purged template families. Distinct from template catalog seed; see [demo-acceptance-asset-seed.md](./demo-acceptance-asset-seed.md).
- `CatalogLoadSeeder` (`LOAD-TPL-*`) remains a separate opt-in load tool — **not** part of this keep/purge set.

Default today: `docgen.demo-catalog.seed-enabled=${DOCGEN_SEED_DEMO_CATALOG:false}`. Prefer class retirement so even `seed-enabled=true` cannot recreate purge IDs.

---

## 4. Keep-list (confirmed)

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

**Keep transitive deps:** referenced letterheads/masters, standard clauses / content modules, and asset-library assets bound by keep-set bindings. Retain `deploy/demo-shared/` helpers required by keep packages. After shrink, `demo-runtime-generate-manifest.json` must list **only** these eight externalIds.

---

## 5. Purge-list (confirmed)

### 5.1 Template externalIds

| externalId | Deploy package (if any) |
| --- | --- |
| `DEMO-RETAIL-ACCOUNT-OPEN` | `deploy/demo-retail-account` |
| `DEMO-RETAIL-ACCOUNT-BALANCE` | `deploy/demo-retail-account` |
| `DEMO-MORTGAGE-APPROVAL` | `deploy/demo-mortgage` |
| `DEMO-TRADE-LC-NOTICE` | `deploy/demo-trade-lc` |
| `DEMO-TRADE-GUARANTEE-NOTICE` | `deploy/demo-trade-lc` |
| `DEMO-RATE-CHANGE-NOTICE` | `deploy/demo-collection` |
| `DEMO-OVERDUE-COLLECTION` | `deploy/demo-collection` |
| `DEMO-WEALTH-STATEMENT` | `deploy/demo-wealth` |
| `DEMO-KYC-CDD-NOTICE` | `deploy/demo-kyc-cdd` |
| `DEMO-ACCOUNT-CLOSURE` | `deploy/demo-account-closure` |
| `DEMO-INSURANCE-ENDORSEMENT` | `deploy/demo-insurance-endorsement` |
| `DEMO-FULL-FLOW-LETTER` | *(Java seeder only — no deploy package)* |
| `DEMO-RETAIL-LETTER` | *(legacy Java seeder only)* |

### 5.2 Deploy packages removed from repo (Done)

`demo-retail-account`, `demo-mortgage`, `demo-trade-lc`, `demo-collection`, `demo-wealth`, `demo-kyc-cdd`, `demo-account-closure`, `demo-insurance-endorsement`.

### 5.3 Orphans

After purge templates are removed, soft-delete / remove masters, content modules, and asset-library rows **only if** they are not in the keep-set transitive dependency graph. Fail-closed if a candidate delete is still referenced by a keep template (§7).

### 5.4 Java seeders — class retirement (Done for template-family)

| Class | Policy |
| --- | --- |
| `DemoFullFlowCatalogSeeder` | **REMOVED** (seeds `DEMO-FULL-FLOW-LETTER`) |
| `DemoFullFlowPublishSupport` | **REMOVED** with seeder |
| `DemoCatalogSeeder` | **REMOVED** (seeds `DEMO-RETAIL-LETTER`) |
| `DemoCatalogSeedProperties` | **REMOVED** with `DemoCatalogSeeder` |
| `DemoCatalogSessions` | **RETAINED** (session helpers for retained seeders / fixtures) |
| `DemoDocxFactory` | **RETAINED** (CatalogLoadSeeder / E2E fixtures) |
| `DemoRetailLetterheadDocxBuilder` | **RETAINED** (CatalogLoadSeeder / E2E fixtures) |
| `DemoAssetLibrarySeeder` | **RETAIN** (opt-in `false` default) |
| `CatalogLoadSeeder` | **RETAIN** (opt-in `false` default; out of screenshot catalog scope) |

Config: leave `DOCGEN_SEED_DEMO_CATALOG` / `docgen.demo-catalog.seed-enabled` **false** by default. Forcing `true` in lab is **unsupported** for the slim catalog once seeders are retired.

---

## 6. Cleanup script usage

**FOL-only script redirected:** `deploy/demo-fol/cleanup-catalog-except-fol.ps1` now forwards to the keep-8 cleanup (FOL-only would delete the other seven keep templates).

```powershell
# From repo root — healthy Docker acceptance stack required
.\deploy\cleanup-demo-catalog-keep-list.ps1
.\deploy\cleanup-demo-catalog-keep-list.ps1 -BackendUrl http://localhost:8080 -WhatIf
```

| Item | Value |
| --- | --- |
| **Path** | `deploy/cleanup-demo-catalog-keep-list.ps1` |
| **Pattern** | Extends FOL cleanup (admin login, healthz wait, DELETE via management API, Postgres orphan soft-delete) for **all eight** externalIds |
| **Params** | `-BackendUrl` (default `:8080`), `-PostgresContainer` / DB creds, `-WhatIf` |
| **Admin session** | Local demo admin (e.g. `10000001`) |
| **Status** | **Landed** + acceptance evidence archived (Stage 5/10); note pagination follow-up (~100 page size) |

---

## 7. Fail-closed dependency checks (confirmed)

1. Resolve keep externalIds + transitive master / content-module / asset refs **before** deleting candidates.
2. If a candidate purge resource is still referenced by any keep-set template binding → **abort** that delete (or the whole batch) with non-zero exit; emit keep externalId + blocking dependency; leave keep-set `PUBLISHED` with intact bindings.
3. Partial delete that leaves keep-set broken is **forbidden**.
4. Authorization remains fail-closed for delete APIs; cleanup uses admin session only.
5. E2E temporary `E2E-*` templates are out of demo keep/purge scope (Playwright teardown).

---

## 8. Import / publish / generate (keep packages only — Done)

Registries narrowed to KEEP-8:

```powershell
# Repo root; healthy Docker acceptance stack required
.\deploy\import-all-demos.ps1
.\deploy\publish-all-demos.ps1
# Optional smoke:
.\deploy\generate-all-demos.ps1
```

| Check | Expected (confirmed target) |
| --- | --- |
| `import-all-demos.ps1` | Invokes **only** keep-package import scripts (7 packages / 8 templates) |
| `Get-DemoPublishExternalIds` | Returns exactly the **8** keep externalIds; **no** auto-insert of `DEMO-FULL-FLOW-LETTER` |
| `demo-runtime-generate-manifest.json` | Keep-set only |
| Publish evidence | `expectedCount=8` (e.g. `.tmp/evidence/all-demos-publish-summary.json`) |
| Generate | DOCX evidence only for the eight keep IDs |

---

## 9. Operator procedure (target sequence)

1. Confirm Docker acceptance stack healthy (`:8080` healthz). Prefer queued deploy: `.\scripts\docker-deploy-queue.ps1` (single host — never a second compose project).
2. Confirm keep packages are importable from the repo (or already Live in DB).
3. Confirm `DOCGEN_SEED_DEMO_CATALOG` unset/`false`.
4. Run `cleanup-demo-catalog-keep-list.ps1` (prefer `-WhatIf` first; paginate DELETE if catalog ≫ ~100).
5. Run import-all → publish-all (optional generate-all).
6. Reboot / redeploy and confirm purged IDs (`DEMO-FULL-FLOW-LETTER`, `DEMO-RETAIL-LETTER`, other purge externalIds) are **not** recreated.
7. Archive evidence under `docs/plan/evidence/demo-catalog-keep-bank-letters/` (catalog list, publish summary `expectedCount=8`, seeder silence notes).

---

## 10. Acceptance evidence (archived)

| Path | Purpose |
| --- | --- |
| [docs/plan/evidence/demo-catalog-keep-bank-letters/](../plan/evidence/demo-catalog-keep-bank-letters/README.md) | Leaf evidence home (Stage 5/6/10 artifacts) |
| Management API | `GET /api/management/v1/templates` — demo catalog externalId set = keep-set |
| Binding integrity | `POST .../bindings/validate` for each keep template |
| Boot silence | Default config does not recreate purge IDs |
| Contract tests | `DemoPublishRegistry` / orchestration contracts assert keep-set of **8** under `mvn verify` |

---

## 11. Hard vetoes

| Veto | Rule |
| --- | --- |
| Checklist **#3b / #5a** | Do **not** flip **GO** |
| Umbrella **#53** / **#106** | Do **not** mark **Done** |
| **CE-O02** | Do **not** touch / activate |
| Frontend | `frontend_ui_in_scope=false` — no empty-state UI work for this leaf |
| Go-live / IBL / CE | Do **not** claim program Done from this ops leaf |
| FOL-only cleanup | Do **not** use `cleanup-catalog-except-fol.ps1` as the slim-catalog tool |

---

## 12. Traceability

| Source | Relation |
| --- | --- |
| User confirmation 2026-07-24 | Authoritative keep-list of 8 — supersedes Wave B “20 ID” size for **screenshot acceptance** |
| BDD §5 / §9 | Inventory + historical seeder rationale |
| Wave A / Wave B behavior docs | Quality bar retained for keep families; non-keep packages retired from default demo catalog |
| SYS-NORM Wave 8 | Seed defaults honest-empty / opt-in — aligns with seeder disable-by-default |
