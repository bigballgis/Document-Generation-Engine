# BDD 行为规格：Slim demo catalog — keep screenshot bank-letter Live set

| Field | Value |
| --- | --- |
| **文件状态** | `ready` |
| **BDD ID 前缀** | `BDD-DEMO-KEEP` |
| **编写日期** | 2026-07-24 |
| **Slice** | `demo-catalog-keep-bank-letters` |
| **Branch** | `feat/demo-catalog-keep-bank-letters` |
| **Worktree** | `D:/working/DGE-demo-catalog-keep-bank-letters` |
| **Placement** | ISOLATED |
| **Integration base** | `origin/main` |
| **task_ids** | `["164"]`（alias slice id `demo-catalog-keep-bank-letters`） |
| **Batch recommendation** | **solo** — single leaf; amortize one cleanup + registry shrink + seeder retirement evidence run |
| **Owning docs** | **本文件（本叶行为 SoT）**；对照 [bank-letter-demo-refresh.md](./bank-letter-demo-refresh.md)（Wave A Done）；[bank-letter-demo-expand.md](./bank-letter-demo-expand.md)（Wave B Done）；[sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md)（Wave 8 seed defaults）；[deploy/demo-shared/README.md](../../deploy/demo-shared/README.md)；ops note §9（本文件）+ operator runbook [demo-catalog-keep-bank-letters.md](../operations/demo-catalog-keep-bank-letters.md)；evidence stub [demo-catalog-keep-bank-letters/](../plan/evidence/demo-catalog-keep-bank-letters/README.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`**（清理后仍有 8 个 Live 模板；不改空态文案；无新管理面 UI） |

```
bdd_readiness: ready
frontend_ui_in_scope: false
open_questions: []
owning_doc: docs/behavior/demo-catalog-keep-bank-letters.md
task_ids: ["164"]
slice_id: demo-catalog-keep-bank-letters
scenario_count: 14
scenario_ids:
  - BDD-DEMO-KEEP-001 … BDD-DEMO-KEEP-014
user_confirmation: 2026-07-24 authoritative keep-list (parent handoff screenshot)
```

**完成声明约束：** 本叶将验收/截图用演示目录收缩为用户确认的 **8 个 Live bank-letter 模板**及其被引用的 letterhead / standard clauses / asset-library 资产；删除其余 DEMO 噪声包与孤儿资源；停用/删除会在重启时重新种入已删模板的 Java `ApplicationRunner` seeders。**禁止**翻转 checklist **#3b/#5a GO**；**禁止**标记 **#53 / #106** Done；**禁止**触碰 CE-O02；**禁止**在依赖检查失败时硬删 keep-set 绑定依赖；**禁止**宣称 go-live / IBL / CE program Done。

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    Keep-list cleanup, package/registry shrink, orphan purge, and Java seeder
    retirement share one acceptance surface (catalog after cleanup = keep-set only).
    Amortize one verify + one queued deploy evidence run. Not multi-writer.
  member_task_ids: ["164"]
  proposed_slice_id: demo-catalog-keep-bank-letters
  shared_acceptance_surface: >
    acceptance catalog after cleanup shows only keep-set;
    import-all / publish-all / generate-all / runtime manifest scoped to keep packages;
    Java seeders disabled/removed so reboot does not reintroduce purged IDs
  vetoes_applied:
    - checklist-#3b/#5a-GO
    - CE-O02
    - mark-#53-CE-Done
    - mark-#106-Done
    - activate-#119
    - no-frontend-unless-empty-state
  on_red_split_hint: >
    if Java seeder retirement breaks unrelated tests,
    split seeder-retirement vs package-purge
```

---

## 1. Goal

验收栈与仓库演示目录**仅保留**用户 2026-07-24 确认的截图用银行信函 Live 集（8 个模板 externalId）。Ops 执行清理后：

1. Keep-set 模板仍为 `PUBLISHED`（Live），绑定的 letterhead / content modules / asset-library 引用完整可用。
2. Purge-set 模板从目录与编排脚本中消失；未被 keep-set 引用的 master / clause / asset 行被清除。
3. 默认启动路径**不会**通过 Java seeder 把已删模板（尤其 `DEMO-FULL-FLOW-LETTER`、`DEMO-RETAIL-LETTER`）重新种回。
4. Keep-set 的权威装载路径为 **deploy 包 + PowerShell import/publish**，不再依赖 Java auto-seed。

---

## 2. Actor / role

| Actor | 角色 | 范围 |
| --- | --- | --- |
| **Ops / demo engineer** | 收缩 `deploy/demo-*` 与 `import-all` / `publish-all` / `generate-all` 注册表；执行 keep-set cleanup；退役 Java seeders | 仓库演示包 + Docker 验收栈（`:8080` / `:4173`） |
| **Acceptance reviewer** | 确认管理面模板目录与 API 列表仅含 keep-set；抽查 keep-set DOCX 仍可 generate | 管理面 catalog + `.tmp/generated_*.docx` |
| **System** | 依赖图检查 fail-closed；软删/API 删除；条件属性控制 ApplicationRunner | Postgres + Spring Boot demo seeders |
| **Runtime caller**（既有） | 仅对 keep-set 已发布模板 generate（CORP_API） | 既有 credential；无新调用方字段 |

---

## 3. Trigger

| 触发 | 说明 |
| --- | --- |
| **T1 — User keep-list** | 2026-07-24 用户权威确认 KEEP 8 模板；要求 purge 其余 demo 噪声 |
| **T2 — Ops cleanup run** | 工程师在健康 Docker 栈上执行 keep-set cleanup（演进自 `deploy/demo-fol/cleanup-catalog-except-fol.ps1`，扩展为 keep-8），并同步仓库包/注册表 |
| **T3 — Boot / redeploy** | 验收栈重启或 `docker-deploy-queue` 后，确认 purged IDs 不会被 Java seeder 重新引入 |
| **T4 — Import/publish chain** | Ops 跑 `import-all-demos.ps1` → `publish-all-demos.ps1`（可选 `generate-all-demos.ps1`）仅覆盖 keep 包 |

---

## 4. Preconditions

1. Docker 验收栈健康（backend `:8080`）。
2. Keep-set 八包已可从仓库导入（或 DB 中已存在对应 Live 行）：
   - `demo-covenant-waiver`, `demo-formal-demand`, `demo-commitment`, `demo-facility-amendment`, `demo-annual-review`, `demo-credit-limit`, `demo-fol`
3. 操作者持有可删模板/查 catalog 的管理会话（local demo admin，如 `10000001`）。
4. `docgen.demo-catalog.seed-enabled` / `DOCGEN_SEED_DEMO_CATALOG` 默认为 `false`（当前 `application.yml`）；本叶要求退役后仍默认关闭，且不再依赖开启 full-flow seed。
5. **Out of scope (confirmed):** `CatalogLoadSeeder`（`LOAD-TPL-*`，LR-C5 分页负载）不是截图 bank-letter 目录；保持 opt-in 默认 `false`，本叶不将其列入 purge-set。
6. **Out of scope (confirmed):** 不翻转 #3b/#5a；不标记 #53/#106 Done；不触碰 CE-O02；无前端空态文案变更（keep-set 非空）。

---

## 5. Authoritative inventory (2026-07-24)

### 5.1 Keep-set — Live templates (8)

| # | externalId | Deploy package | Master (letterhead) name |
| --- | --- | --- | --- |
| 1 | `DEMO-COVENANT-WAIVER` | `deploy/demo-covenant-waiver` | Meridian Covenant Waiver Master |
| 2 | `DEMO-FORMAL-DEMAND` | `deploy/demo-formal-demand` | Meridian Formal Demand Master |
| 3 | `DEMO-COMMITMENT-LETTER` | `deploy/demo-commitment` | Meridian Commitment Letter Master |
| 4 | `DEMO-FACILITY-AMENDMENT` | `deploy/demo-facility-amendment` | Meridian Facility Amendment Master |
| 5 | `DEMO-ANNUAL-REVIEW` | `deploy/demo-annual-review` | Meridian Annual Credit Review Master |
| 6 | `DEMO-FACILITY-RENEWAL` | `deploy/demo-annual-review` | Meridian Facility Renewal Master |
| 7 | `DEMO-CREDIT-LIMIT-CONFIRM` | `deploy/demo-credit-limit` | Meridian Credit Limit Master |
| 8 | `CORP-FOL-OFFER` | `deploy/demo-fol` | Meridian Wholesale FOL Master |

**Keep transitive deps:** each template’s referenced letterheads/masters, standard clauses / content modules (`contentModuleRefs` in package manifests / SQL), and asset-library assets bound by keep-set bindings. Shared helpers under `deploy/demo-shared/` remain if still required by keep packages (bank style / runtime generate manifest entries for keep-set only).

### 5.2 Purge-set — template packages (externalId → deploy package)

Verified 2026-07-24 against `*-template-config.json` under `deploy/demo-*` (worktree HEAD = `origin/main`).

| externalId | Deploy package | Notes |
| --- | --- | --- |
| `DEMO-RETAIL-ACCOUNT-OPEN` | `deploy/demo-retail-account` | Wave A retail |
| `DEMO-RETAIL-ACCOUNT-BALANCE` | `deploy/demo-retail-account` | same package |
| `DEMO-MORTGAGE-APPROVAL` | `deploy/demo-mortgage` | |
| `DEMO-TRADE-LC-NOTICE` | `deploy/demo-trade-lc` | |
| `DEMO-TRADE-GUARANTEE-NOTICE` | `deploy/demo-trade-lc` | same package |
| `DEMO-RATE-CHANGE-NOTICE` | `deploy/demo-collection` | |
| `DEMO-OVERDUE-COLLECTION` | `deploy/demo-collection` | same package |
| `DEMO-WEALTH-STATEMENT` | `deploy/demo-wealth` | |
| `DEMO-KYC-CDD-NOTICE` | `deploy/demo-kyc-cdd` | Wave B |
| `DEMO-ACCOUNT-CLOSURE` | `deploy/demo-account-closure` | Wave B |
| `DEMO-INSURANCE-ENDORSEMENT` | `deploy/demo-insurance-endorsement` | Wave B |
| `DEMO-FULL-FLOW-LETTER` | *(no deploy package)* | Java `DemoFullFlowCatalogSeeder` only |
| `DEMO-RETAIL-LETTER` | *(no deploy package)* | Legacy Java `DemoCatalogSeeder` only |

**Shared infra retained (not a template package):** `deploy/demo-shared/` (bank style / runtime generate helpers). After shrink, `demo-runtime-generate-manifest.json` must list **only** keep-set externalIds.

| Category | Items |
| --- | --- |
| **Deploy packages to delete from repo** | `demo-retail-account`, `demo-mortgage`, `demo-trade-lc`, `demo-collection`, `demo-wealth`, `demo-kyc-cdd`, `demo-account-closure`, `demo-insurance-endorsement` |
| **Orchestration shrink** | `deploy/import-all-demos.ps1`, `Get-DemoPublishExternalIds` in `demo-import-shared.ps1`, `publish-all-demos.ps1` full-flow helpers, `generate-all-demos.ps1` consumers, `deploy/demo-shared/demo-runtime-generate-manifest.json`, `DemoPublishRegistry` preferred list — **only keep-set IDs** |
| **Existing FOL-only cleanup** | `deploy/demo-fol/cleanup-catalog-except-fol.ps1` is **too narrow** (keeps only `CORP-FOL-OFFER`); replace/evolve to keep-set-of-8 cleanup with dependency checks |
| **Tests / generators for purge families** | `*MasterDocxAssetGeneratorTest` and contract tests that assert purged package IDs must be retired or narrowed to keep-set; keep-set generators/tests retained |

### 5.3 Java seeders — retain / remove recommendation (observation for implementers)

| Class | Seeds | Recommendation | Rationale |
| --- | --- | --- | --- |
| `DemoFullFlowCatalogSeeder` | `DEMO-FULL-FLOW-LETTER` | **REMOVE** (or delete + drop registry insert) | Purge-set; not in KEEP; historically boot-seeded without deploy package |
| `DemoFullFlowPublishSupport` | publish helper for full-flow | **REMOVE** with seeder | Only serves purged full-flow path |
| `DemoCatalogSeeder` | `DEMO-RETAIL-LETTER` | **REMOVE** | Legacy retail draft; not in KEEP; obsolete vs package import |
| `DemoRetailLetterheadDocxBuilder` | in-JVM letterhead for retail/full-flow seed | **REMOVE if unused** after seeder deletion; retain only if keep-set tests still need builder APIs | Historical DOCX factory for ApplicationRunner path |
| `DemoAssetLibrarySeeder` | shared asset-library bootstrap | **RETAIN** (opt-in `false` default) | Not a template-family seeder; must not reintroduce purged templates |
| `CatalogLoadSeeder` | `LOAD-TPL-*` pagination load | **RETAIN** (opt-in `false` default) | Out of screenshot catalog scope (LR-C5); not purge-set |

Default config today: `docgen.demo-catalog.seed-enabled=${DOCGEN_SEED_DEMO_CATALOG:false}`. Goal: remove unused seeder classes so even `seed-enabled=true` cannot recreate purge IDs; keep-set loaded only via deploy import.

### 5.4 Inventory sources examined

- `deploy/import-all-demos.ps1` — currently 15 package import chain
- `deploy/publish-all-demos.ps1` + `Get-DemoPublishExternalIds` — 20 external IDs incl. `DEMO-FULL-FLOW-LETTER`
- `deploy/generate-all-demos.ps1` + `deploy/demo-shared/demo-runtime-generate-manifest.json`
- `deploy/demo-fol/cleanup-catalog-except-fol.ps1` — FOL-only precedent
- `backend/src/main/java/com/bank/docgen/demo/*` ApplicationRunners + `application.yml` seed flags
- `backend/src/test/java/com/bank/docgen/demo/support/DemoPublishRegistry.java`

---

## 6. Primary journey

1. Ops confirms keep-list (already authoritative 2026-07-24).
2. Implementer shrinks repo registries, **deletes purge-target packages from disk**, and retires unused Java seeders/tests (implementation leaf — after this BDD).
3. Ops (or pipeline) runs **keep-set cleanup** against acceptance DB:
   - Resolve keep externalIds + transitive master / content-module / asset refs.
   - Delete or soft-delete purge templates.
   - Purge orphan masters / modules / assets **only if** not in keep transitive set.
4. If any keep-set binding would break → **abort cleanup** (fail-closed); report blocking refs; leave catalog unchanged for that delete batch.
5. Ensure Java seeders for purged IDs are disabled/removed; reboot stack.
6. Run `import-all-demos.ps1` → `publish-all-demos.ps1` for keep packages only; verify catalog = keep-set.
7. Confirm `DemoPublishRegistry` / orchestration contract tests green under `mvn verify`.
8. Acceptance reviewer lists templates via management API / UI — only the 8 keep externalIds (plus non-demo system fixtures if any, e.g. E2E temp `E2E-*`, which remain out of demo catalog scope and are teardown-managed).

---

## 7. System responses

### Success

- Management template search/list for demo catalog shows exactly the 8 keep externalIds as Live/`PUBLISHED` (after publish chain).
- Purge externalIds return not-found / absent from list.
- Keep-set bindings validate; preview/generate still succeed for keep templates.
- Backend boot with default config does not create `DEMO-FULL-FLOW-LETTER` or `DEMO-RETAIL-LETTER`.
- `import-all-demos.ps1` only invokes keep package import scripts; `Get-DemoPublishExternalIds` returns the 8 keep IDs (no full-flow insert).

### Fail-closed

- Cleanup that would delete a master/module/asset still referenced by a keep-set template **must not proceed** for that resource; emit clear error and exit non-zero.
- Partial delete that leaves keep-set broken is forbidden; prefer transactional/batched abort.
- Authorization remains fail-closed for delete APIs (existing platform rules); cleanup uses admin session only.

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-DEMO-KEEP-001 — Keep-set remains Live after cleanup

```gherkin
Given the acceptance stack has the eight keep-set templates in PUBLISHED (Live) state
  And each keep template has resolvable letterhead and content-module bindings
When Ops runs keep-set catalog cleanup (keep externalIds = authoritative list)
Then each of DEMO-COVENANT-WAIVER, DEMO-FORMAL-DEMAND, DEMO-COMMITMENT-LETTER,
     DEMO-FACILITY-AMENDMENT, DEMO-ANNUAL-REVIEW, DEMO-FACILITY-RENEWAL,
     DEMO-CREDIT-LIMIT-CONFIRM, CORP-FOL-OFFER remains present and PUBLISHED
  And keep-set bindings still validate
```

### BDD-DEMO-KEEP-002 — Purge-set templates removed from catalog

```gherkin
Given purge-set templates exist in the acceptance catalog
  (e.g. DEMO-RETAIL-ACCOUNT-OPEN, DEMO-MORTGAGE-APPROVAL, DEMO-FULL-FLOW-LETTER, …)
When Ops runs keep-set catalog cleanup successfully
Then management template search by each purge externalId returns no active row
  And the demo catalog list does not include purge externalIds
```

### BDD-DEMO-KEEP-003 — Orphan masters purged only when unreferenced

```gherkin
Given masters exist for purged packages and for keep-set letterheads
When cleanup removes purge templates and evaluates master references
Then masters referenced by any keep-set template are retained
  And masters with zero remaining template references from keep-set are soft-deleted or removed
  And no keep-set template loses its letterhead binding
```

### BDD-DEMO-KEEP-004 — Orphan clauses / content modules purged only when unreferenced

```gherkin
Given content modules exist for purge packages and for keep-set packages
When cleanup evaluates content-module / standard-clause references
Then modules referenced by keep-set bindings or package SQL for keep packages are retained
  And modules only used by purge-set are removed or soft-deleted
```

### BDD-DEMO-KEEP-005 — Orphan asset-library rows purged only when unreferenced

```gherkin
Given asset-library rows exist that are unused by keep-set bindings
  And some assets are referenced by keep-set templates
When cleanup evaluates asset references
Then keep-referenced assets remain ACTIVE and resolvable
  And unreferenced orphan demo assets may be purged
  And cleanup does not delete assets still referenced by keep-set
```

### BDD-DEMO-KEEP-006 — Fail-closed when dependency check fails

```gherkin
Given a candidate purge resource is still referenced by a keep-set template binding
When cleanup attempts to delete that resource
Then the operation aborts for that resource (or the whole cleanup batch) with a non-zero exit
  And an observable error names the keep externalId and blocking dependency
  And the keep-set template remains PUBLISHED with intact bindings
```

### BDD-DEMO-KEEP-007 — Java seeders do not reintroduce purged IDs on reboot

```gherkin
Given DemoFullFlowCatalogSeeder / DemoCatalogSeeder paths that seed
      DEMO-FULL-FLOW-LETTER or DEMO-RETAIL-LETTER are disabled by default or removed
  And purge-set templates are absent from the catalog
When the backend restarts with default acceptance configuration
  (DOCGEN_SEED_DEMO_CATALOG unset/false)
Then DEMO-FULL-FLOW-LETTER and DEMO-RETAIL-LETTER are not recreated
  And keep-set templates are unaffected
```

### BDD-DEMO-KEEP-008 — import-all / publish registries only keep packages

```gherkin
Given the repository registries have been slimmed to keep packages
When Ops inspects deploy/import-all-demos.ps1 and Get-DemoPublishExternalIds
Then only keep-package import scripts are invoked
  And publish externalId list equals the eight keep externalIds
  And DEMO-FULL-FLOW-LETTER is not auto-inserted into the publish list
```

### BDD-DEMO-KEEP-009 — Keep-set import/publish chain still works

```gherkin
Given a clean or cleaned acceptance DB and healthy backend
When Ops runs import-all-demos.ps1 then publish-all-demos.ps1
Then all eight keep externalIds reach PUBLISHED
  And publish evidence expectedCount equals 8
  And no purge package import is required
```

### BDD-DEMO-KEEP-010 — Runtime generate manifest scoped to keep-set

```gherkin
Given demo-runtime-generate-manifest.json is updated for the slim catalog
When generate-all-demos.ps1 runs after publish
Then DOCX evidence is produced only for the eight keep externalIds
  And purged externalIds are absent from the manifest
```

### BDD-DEMO-KEEP-011 — FOL-only cleanup superseded by keep-8 cleanup

```gherkin
Given deploy/demo-fol/cleanup-catalog-except-fol.ps1 historically kept only CORP-FOL-OFFER
When the slim-catalog cleanup tool is used for this leaf
Then it keeps all eight authoritative externalIds (not FOL alone)
  And running the obsolete FOL-only script is documented as unsafe for this leaf
    (or the script is replaced/redirected to keep-8 behavior)
```

### BDD-DEMO-KEEP-012 — Ops documentation: why Java seeders historically existed

```gherkin
Given operators need to know why demo historically used Java ApplicationRunner seeders
When they read the ops note in this behavior doc (§9) and/or linked ops doc updated in the same change set
Then the note explains:
  - ApplicationRunner auto-seed on boot for acceptance/E2E without PowerShell import
  - in-process DOCX builders (e.g. DemoDocxFactory / letterhead builders) generated masters
  - goal state: keep-set is deploy-package + import-script driven; Java seeders not required
    for the eight keep templates (disabled by default / removed if unused)
```

### BDD-DEMO-KEEP-013 — Purge packages removed from repo disk

```gherkin
Given the eight purge-target deploy packages existed under deploy/demo-*
  (demo-retail-account, demo-mortgage, demo-trade-lc, demo-collection,
   demo-wealth, demo-kyc-cdd, demo-account-closure, demo-insurance-endorsement)
When the implementer completes the slim-catalog repo change set
Then those package directories are absent from the repository
  And deploy/demo-shared/ and the seven keep package directories remain
  And no import-all / publish-all / generate-all path references a deleted package script
```

### BDD-DEMO-KEEP-014 — DemoPublishRegistry and orchestration contract tests green

```gherkin
Given DemoPublishRegistry and DemoPublishOrchestrationContractTest previously asserted 20 IDs
When registries are slimmed to the eight keep externalIds and seeder inserts are removed
Then DemoPublishRegistry.allPublishExternalIds equals the keep-set of 8
  And DemoPublishOrchestrationContractTest (and related demo package structure contracts)
      pass under mvn verify without referencing purge packages or DEMO-FULL-FLOW-LETTER
  And no test requires DemoCatalogSeeder / DemoFullFlowCatalogSeeder to be present on the classpath
     unless replaced by an equivalent keep-set fixture
```

---

## 9. Ops note — Historical Java seeder rationale (confirmed)

**Operator runbook (keep/purge + cleanup procedure):** [docs/operations/demo-catalog-keep-bank-letters.md](../operations/demo-catalog-keep-bank-letters.md) — includes this note, keep/purge inventories, planned `deploy/cleanup-demo-catalog-keep-list.ps1` usage, fail-closed checks, and vetoes. Script path is **planned** until implementers land it. Shared style helpers: [deploy/demo-shared/README.md](../../deploy/demo-shared/README.md) — acceptance catalog SoT there is the **KEEP-8** list (not the historical Wave B 20-ID registry).

**Why demo historically used Java classes**

1. **Boot-time ApplicationRunner auto-seed** — Classes such as `DemoCatalogSeeder` and `DemoFullFlowCatalogSeeder` (`@ConditionalOnProperty docgen.demo-catalog.seed-enabled=true`) populated masters/templates on Spring Boot startup so acceptance stacks and E2E could obtain a minimal catalog **without** running PowerShell `deploy/demo-*/import-*-demo.ps1`.
2. **In-process DOCX builders** — Helpers such as `DemoDocxFactory`, `DemoRetailLetterheadDocxBuilder`, and related support built letterhead DOCX bytes in-JVM (no pre-baked package asset required for those seed paths).
3. **Publish helpers in Java** — `DemoFullFlowPublishSupport` advanced full-flow draft content toward a published demo for executive/E2E journeys.

**Why those seeders are retired now (confirmed intent; code pending stage 4)**

1. **Single authoritative load path** — Screenshot / acceptance demos must load from **deploy packages + PowerShell import/publish** only. A second in-JVM path (`ApplicationRunner` + in-process DOCX) is a dual catalog source: reboot or `DOCGEN_SEED_DEMO_CATALOG=true` can recreate IDs that ops deliberately purged.
2. **KEEP-8 supersedes the Wave B 20-ID default** — User confirmation 2026-07-24 contracted the Live screenshot set to eight bank-letter templates. Java-only rows (`DEMO-FULL-FLOW-LETTER`, `DEMO-RETAIL-LETTER`) and non-KEEP packages are purge-set noise for that surface.
3. **Fail-closed reboot** — After cleanup, default boot must not reintroduce purged externalIds. Prefer **class retirement** so even a mistaken `seed-enabled=true` cannot recreate purge IDs.

**Goal state for this leaf**

- The screenshot keep-set of **8** templates is **deploy package + import/publish scripts** driven.
- Java **template-family** seeders are **not required** for those eight (remain **disabled by default**; unused seeder classes deleted or quarantined by implementers).
- Reboot must **not** reintroduce purged template IDs.
- Optional `DemoAssetLibrarySeeder` stays opt-in for managed asset bootstrap tests; it must not re-seed purged template families (asset seed ≠ template catalog seed — [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md)).
- `CatalogLoadSeeder` (`LOAD-TPL-*`) remains a separate opt-in load tool — not part of the screenshot bank-letter keep/purge set.

**Confirmed vs pending (doc layer)**

| Kind | Content |
| --- | --- |
| **Confirmed** | KEEP-8 inventory; PURGE packages/IDs; historical seeder rationale; dual-source retirement rationale; fail-closed deps; vetoes |
| **Pending (implementers)** | Disk purge of purge packages; registry shrink; Java seeder class deletion; `cleanup-demo-catalog-keep-list.ps1`; Docker/verify evidence |

---

## 10. Boundary / exception behavior

| Case | Behavior |
| --- | --- |
| Keep template missing before cleanup | Cleanup must not invent templates; import keep packages first or fail with clear message |
| Concurrent import of purge package | After registry shrink, import-all must not call purged scripts; manual legacy script run is out-of-band and not supported |
| E2E temporary `E2E-*` templates | Not part of demo keep/purge; left to Playwright global-teardown (existing tip on FOL cleanup) |
| Required unit/E2E fixture still needs a purged ID | Implementer **replaces** fixture with keep-set equivalent or narrows test before deleting seeder; fail-closed: do not leave red gates |
| Authorization denied on delete | Fail closed; no partial silent skip that claims cleanup Done |
| `DOCGEN_SEED_DEMO_CATALOG=true` forced in lab | Documented as unsupported for slim catalog; if class remains, enabling it must not recreate purge-set IDs preferred for deletion of those seeders |

---

## 11. Observable evidence

| Evidence | How to observe |
| --- | --- |
| Catalog membership | `GET /api/management/v1/templates` (admin) — externalId set equals keep-set |
| Lifecycle | Keep templates `lifecycleStatus=PUBLISHED` |
| Binding integrity | `POST .../bindings/validate` success for each keep template |
| Seeder silence | Boot logs lack seed-of purged IDs; DB search for purge externalIds empty after reboot |
| Registry | Diff of `import-all-demos.ps1`, `Get-DemoPublishExternalIds`, `demo-runtime-generate-manifest.json` |
| Disk purge | `deploy/demo-*` directory listing — only keep packages + `demo-shared` |
| Contract tests | `DemoPublishOrchestrationContractTest` / `DemoPublishRegistry` assert keep-set of 8 |
| Publish/generate | `.tmp/evidence/all-demos-publish-summary.json` `expectedCount=8`; generate manifest keep-only |
| Ops note | §9 of this file + operator runbook [demo-catalog-keep-bank-letters.md](../operations/demo-catalog-keep-bank-letters.md) linked from `docs/README.md` |
| Evidence home | [docs/plan/evidence/demo-catalog-keep-bank-letters/](../plan/evidence/demo-catalog-keep-bank-letters/README.md) (stub until Stage 5/10) |

---

## 12. Traceability

| Source | Relation |
| --- | --- |
| **User confirmation 2026-07-24** | Authoritative keep-list of 8 externalIds — **supersedes** Wave B “20 ID” catalog size for screenshot acceptance |
| [bank-letter-demo-refresh.md](./bank-letter-demo-refresh.md) | Wave A quality bar for retained families; this leaf **narrows** catalog size |
| [bank-letter-demo-expand.md](./bank-letter-demo-expand.md) | Wave B added families; this leaf **retires** non-keep Wave A/B packages from default demo catalog |
| [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) | Seed defaults honest-empty / opt-in; aligns with seeder disable-by-default |
| `deploy/demo-fol/cleanup-catalog-except-fol.ps1` | Precedent cleanup; must evolve to keep-8 |
| PRD / P22 / P23 product contracts | **Not** rewritten here; keep-set remains bank-letter quality demos; do not flip go-live checklist |

---

## 13. Non-goals

- Frontend empty-state copy or catalog UX redesign (`frontend_ui_in_scope=false`).
- Changing product API contracts for template CRUD beyond using existing delete/soft-delete.
- Marking formal phase / CE-O02 / #53 / #106 / #3b / #5a.
- Deleting `CatalogLoadSeeder` load tooling.
- Implementing cleanup scripts in the BDD stage (plan-orchestrator → implementers next).

---

## 14. BDD readiness

```
bdd_readiness: ready
open_questions: []
owning_doc: docs/behavior/demo-catalog-keep-bank-letters.md
task_ids: ["164"]
slice_id: demo-catalog-keep-bank-letters
keep_inventory:
  - DEMO-COVENANT-WAIVER (demo-covenant-waiver)
  - DEMO-FORMAL-DEMAND (demo-formal-demand)
  - DEMO-COMMITMENT-LETTER (demo-commitment)
  - DEMO-FACILITY-AMENDMENT (demo-facility-amendment)
  - DEMO-ANNUAL-REVIEW (demo-annual-review)
  - DEMO-FACILITY-RENEWAL (demo-annual-review)
  - DEMO-CREDIT-LIMIT-CONFIRM (demo-credit-limit)
  - CORP-FOL-OFFER (demo-fol)
purge_inventory:
  packages:
    - demo-retail-account
    - demo-mortgage
    - demo-trade-lc
    - demo-collection
    - demo-wealth
    - demo-kyc-cdd
    - demo-account-closure
    - demo-insurance-endorsement
  template_external_ids:
    - DEMO-RETAIL-ACCOUNT-OPEN
    - DEMO-RETAIL-ACCOUNT-BALANCE
    - DEMO-MORTGAGE-APPROVAL
    - DEMO-TRADE-LC-NOTICE
    - DEMO-TRADE-GUARANTEE-NOTICE
    - DEMO-RATE-CHANGE-NOTICE
    - DEMO-OVERDUE-COLLECTION
    - DEMO-WEALTH-STATEMENT
    - DEMO-KYC-CDD-NOTICE
    - DEMO-ACCOUNT-CLOSURE
    - DEMO-INSURANCE-ENDORSEMENT
    - DEMO-FULL-FLOW-LETTER
    - DEMO-RETAIL-LETTER
  java_seeders:
    remove:
      - DemoFullFlowCatalogSeeder
      - DemoFullFlowPublishSupport
      - DemoCatalogSeeder
      - DemoRetailLetterheadDocxBuilder (if unused after seeder removal)
    retain:
      - DemoAssetLibrarySeeder (opt-in false default)
      - CatalogLoadSeeder (opt-in false default; LOAD-TPL-* out of scope)
acceptance_scenarios:
  - BDD-DEMO-KEEP-001
  - BDD-DEMO-KEEP-002
  - BDD-DEMO-KEEP-003
  - BDD-DEMO-KEEP-004
  - BDD-DEMO-KEEP-005
  - BDD-DEMO-KEEP-006
  - BDD-DEMO-KEEP-007
  - BDD-DEMO-KEEP-008
  - BDD-DEMO-KEEP-009
  - BDD-DEMO-KEEP-010
  - BDD-DEMO-KEEP-011
  - BDD-DEMO-KEEP-012
  - BDD-DEMO-KEEP-013
  - BDD-DEMO-KEEP-014
frontend_ui_in_scope: false
ops_runbook: docs/operations/demo-catalog-keep-bank-letters.md
next: backend-engineer
```
