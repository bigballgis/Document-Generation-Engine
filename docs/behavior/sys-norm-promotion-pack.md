---
id: DOC-BEHAVIOR-SYS-NORM-PROMOTION-PACK
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 7
slice: sys-norm-promotion-pack
taskMaster: "151"
frontend_ui_in_scope: true
related:
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/behavior/ce-e01-export-bundle-v2.md
  - docs/behavior/ce-e03-full-library-export.md
  - docs/behavior/ce-e02-asset-library.md
  - docs/behavior/sys-norm-d1-brands.md
  - docs/behavior/ibl-e6-clause-nesting-governance.md
  - docs/adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md
  - docs/adr/template-lifecycle/0067-clause-nesting-module-graph-governance.md
  - docs/product/PRD.md
  - docs/security/permission-matrix.md
---

# SYS-NORM Wave 7 — UAT→PROD promotion pack + dry-run UI

> **Slice:** `sys-norm-promotion-pack` · TM **#151** (**In Progress** / sole-active).  
> **Placement:** ISOLATED · worktree `D:/working/DGE-sys-norm-promotion-pack` ·
> branch `feat/sys-norm-promotion-pack` · base `51b96e36`.  
> **Locks:** charter [system-normalization-program.md](./system-normalization-program.md)
> §2.7; plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md)
> Wave 7; ADR-0071 Decision 5; upstream CE-E01 / CE-E03 (**extend**, do not break).  
> **Formal phase:** **None**.  
> **Do not:** flip checklist **#3b** / **#5a**; mark **#53** Done; claim SYS-NORM program Done;
> implement Wave 8 (`sys-norm-demo-seed-terms`) or parked UX in this leaf; invent go-live.

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
pending_non_blocking: []
openapi_param_locked: dependencyClosure=PROMOTION
owning_doc: docs/behavior/sys-norm-promotion-pack.md
task_ids: ["151"]
queue_slice_id: sys-norm-promotion-pack
scenario_ids:
  - BDD-SYS-NORM-PP-001 … BDD-SYS-NORM-PP-020
scenario_count: 20
batch_recommendation:
  decision: solo
  member_task_ids: ["151"]
  proposed_slice_id: sys-norm-promotion-pack
  shared_acceptance_surface: UAT→PROD promotion pack + dry-run UI
  vetoes_applied:
    - checklist-#3b/#5a
    - CE-O02
    - "#53"
    - Wave-8
    - parked-UX
  evidence_amortization: one mvn verify + FE gates + E2E + docker queue
  on_red_split_hint: N/A (solo)
```

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    sole-active cleared after Wave 6; Wave 7 is next queue head;
    promotion pack + dry-run UI share one acceptance surface;
    do not fold Wave 8 / parked UX
  member_task_ids: ["151"]
  proposed_slice_id: sys-norm-promotion-pack
  shared_acceptance_surface: UAT→PROD promotion pack + dry-run UI
  evidence_amortization: one verify + FE gates + E2E + queued deploy
  on_red_split_hint: N/A (solo)
```

| IN（本叶） | OUT（明确禁止 / 后续叶） |
| --- | --- |
| Promotion **dependency closure** export (assets **binary** + clause nesting graph as required) | Wave 8 demo seed / L1 terminology sweep |
| Two-phase master/letterhead P2 — **no skip** of APPROVED | Flip checklist **#3b** / **#5a** |
| **No** brand/entity sidecar; **no** secrets in pack | Mark **#53** Done; claim SYS-NORM program Done |
| Import lands **DRAFT** on PROD; re-test / re-approve / re-publish | Auto-APPROVE / auto-PUBLISH via pack |
| Extend CE-E01 fail-closed dry-run + commit (API) | Break v1/v2 default paths or half-import |
| Management UI **dry-run** (P-Q4 resolved below) | New standalone “Promotion” nav product area |
| E2E + UIUX for dry-run dialog journey | Parked reminder / asset isolation / binding editor UX |
| Library ZIP may carry same closure when exported under promotion profile | Full async Kafka export job; second Docker stack |

---

## 1. Actor / role

| Actor | Role / scope |
| --- | --- |
| `GLOBAL_ADMIN` | Export promotion packs / library promotion ZIP in global scope; dry-run + commit import |
| `GROUP_ADMIN` | Same within authorized groups |
| `DOCUMENT_AUTHOR` | Export/import own templates per matrix §5 (post Wave 5 role catalog) |
| Platform / ops engineer | Scripted UAT→PROD promote via API; uses dry-run before commit |
| Management UI operator (same export/import capability) | Uses Templates catalog **Import** dialog dry-run + commit |
| `TEMPLATE_TESTER` / `LEGAL_REVIEWER` / `AUDIT_ADMIN` (role alone) | **No** export/import/dry-run — fail-closed `403` |
| System | Assembles promotion closure; dependency report; transactional import; audit |

Group isolation remains fail-closed. No new permission codes — reuse matrix §5 导出/导入.

---

## 2. User goal

1. Export a **UAT→PROD promotion pack** whose dependency closure includes, as required:
   letterhead/master DOCX evidence, **clause nesting graph closure**, and **asset binaries**
   for referenced keys — **without** DocumentBrand/LegalEntity sidecars and **without** secrets.
2. On PROD, **dry-run** the pack (API + management UI) to see a structured dependency report
   with zero business writes.
3. Commit import only when ready: templates (and any materialized clauses/assets) land as
   **DRAFT**; masters/letterheads never skip **APPROVED**; operators must
   **re-test / re-approve / re-publish** on PROD.
4. Preserve CE-E01 / CE-E03 default behaviors for non-promotion exports/imports (fail-closed
   gates intact).

---

## 3. Trigger

- Authorized `GET …/templates/{id}/export` with **promotion closure profile** + `format=zip`
  (and `bundleVersion=2` baseline).
- Authorized `POST …/library/export` with the same promotion closure profile (optional batch).
- Authorized `POST …/templates/import` with `dryRun=true|false` (JSON or multipart ZIP) —
  existing CE-E01 path, extended report types.
- Management UI: Templates catalog → **Import** dialog → **Check dependencies** (dry-run)
  and/or **Import** (commit).

---

## 4. Preconditions

- Waves **0–6** Done; ADR-0071 Accepted; D1 surfaces retired.
- CE-E01 v2 + dry-run/commit **Done**; CE-E03 library export **Done**; CE-E02 asset library
  exists for materialize targets; IBL-E6 nesting graph governance **Done**.
- Actor authenticated with management JWT and matrix §5 export/import capability.
- Formal phase **None**; checklist **#3b** / **#5a** untouched; **#53** not Done.
- This leaf implements only in the isolated worktree (not MAIN).

---

## 5. Confirmed decisions (Wave 7 locks)

| ID | Decision | Source |
| --- | --- | --- |
| **PP-C1** | **Promotion pack = CE-E01 v2 ZIP + promotion dependency-closure profile** (not a breaking replacement). OpenAPI / contract lock: query/body param **`dependencyClosure=PROMOTION`** (omit = default). Default E01 v2 (no promotion profile) remains keys-only per E01-C7. Promotion export **must** embed asset binaries for every key in `assetKeyManifest` under fixed relative paths `artifacts/assets/{assetKey}` (path-safe encoding; manifest retains original key). | charter §2.7; extend E01; OpenAPI |
| **PP-C2** | **Clause nesting closure:** when a template references nested content modules (ADR-0067), promotion export includes **transitive** `clauseSnapshots` for the nesting closure (depth ≤ 8) and a machine-readable `clauseNestingGraph` (edges + depth). Cycles / depth violations → export **fail-closed** (same codes family as IBL-E6). Non-nested templates omit graph or emit empty edges. | charter §2.7; ADR-0067 |
| **PP-C3** | **Library promotion ZIP:** CE-E03 structure retained; when body `dependencyClosure=PROMOTION`, each nested `templates/{id}.zip` is a promotion pack (PP-C1/C2), and root may include deduped `assets/{assetKey}` binaries (like `masters/` / `clauses/`). Default E03 without profile unchanged (keys only). | extend E03; OpenAPI |
| **PP-C4** | **No brand/entity sidecar** in any promotion pack / manifest / nested JSON (ADR-0071 / D1-005). Letterhead/logo/seal travel only via master/letterhead artifacts. | §2.7; D1 |
| **PP-C5** | **No secrets:** packs, manifests, dry-run reports, and UI must never contain API credentials, runtime secrets, or test-data variable plaintext. | §2.7; E01-C3 |
| **PP-C6** | **Two-phase master / letterhead (P2):** pack **must not** write a master/letterhead into `APPROVED` (or any post-APPROVED) state on import. If the pack materializes a missing master from embedded DOCX, landing status is **`DRAFT`** only. Template commit that binds a non-APPROVED master remains fail-closed (`masterNotApproved` / equivalent). Operators approve letterhead on PROD via normal master review before publishable template binds. **Forbidden:** skip APPROVED; auto-approve via pack. | §2.7; domain master review |
| **PP-C7** | **Template import → DRAFT:** successful commit creates/resets template as **`DRAFT`** on PROD; must **re-test → re-approve → re-publish** on PROD (PRD §10). Never import as PUBLISHED / PENDING_RELEASE shortcut. | §2.7; PRD §10; E01-C16 |
| **PP-C8** | **Asset dry-run / commit:** if binary present in pack → `ASSET_KEY` disposition `WILL_MATERIALIZE` (non-blocking); if key missing on target **and** binary absent → `MISSING` (**blocking**). Commit materializes CE-E02 library assets in the **same transaction** as template/clause writes; failure → full rollback (no half-import). | extend E01-C14 |
| **PP-C9** | **Dry-run API:** keep CE-E01 contract — `dryRun=true` → HTTP **200** + `dependencyReport` + `imported=false` + **zero** business-table writes; audit `TEMPLATE_IMPORT_DRY_RUN`. Blocking → `readyToCommit=false`. Commit with blocking → **422** `api.error.template.importDependenciesUnsatisfied` + report. | E01-C10…C15 |
| **PP-C10** | **New/extended report types (additive):** allow `dependencyType` values already in E01 plus `CLAUSE_NESTING` and `ASSET_BINARY` (or reuse `ASSET_KEY` with codes `ASSET_WILL_MATERIALIZE` / `ASSET_BINARY_ABSENT`). Stable UPPER_SNAKE `code` + English-first `messageKey`. | extend E01-C11 |
| **PP-C11** | **Permissions:** no new capability codes; matrix §5 export/import unchanged. | matrix §5 |
| **PP-C12** | **FE dry-run UI in scope** (`frontend_ui_in_scope=true`) — P-Q4 resolved in §5.1. | charter Wave 7 |
| **PP-C13** | **Compatibility:** v1 import/export and non-promotion v2/E03 paths **must** keep prior fail-closed semantics. Promotion profile is opt-in on export; import auto-detects embedded assets/graph when present. | upstream extend |
| **PP-C14** | **Non-goals (hard):** Wave 8; parked UX; CE-O02; RTL; go-live; flip #3b/#5a; mark #53 Done; claim program Done; brand/entity sidecar restore; async export job; second compose stack. | governance vetoes |

### 5.1 P-Q4 — Promotion dry-run UX (resolved)

| UX lock | Decision |
| --- | --- |
| **Surface** | Extend existing Templates catalog **Import** dialog (`TemplateImportDialog`) — **not** a new top-level nav product, **not** External services. |
| **Actions** | Secondary/primary pair: **Check dependencies** (calls import API `dryRun=true`) and **Import** (commit `dryRun=false`). English-first i18n keys. |
| **Gate** | **Import** is enabled only when the latest dry-run for the current file+master+policy returns `readyToCommit=true`. Changing file/master/policy clears the report and disables Import until a new dry-run. |
| **Report UI** | Show `readyToCommit`, `blockingCount` / `warningCount` / `infoCount`, and a scrollable item list (`dependencyType`, disposition, code, localized message). No secrets / clause full text / DOCX bytes. |
| **Errors** | Authz `403` → honest capability denial; structural/parse errors → existing alert pattern; commit `422` with report → show report (same shape as dry-run). |
| **OA** | Dialog remains bank OA (single job: import with dependency preview); no hero/cards clutter; use existing dialog + table/list patterns. |
| **E2E** | Playwright covers: open Import → upload promotion ZIP → select APPROVED master → Check dependencies → see ready/blocking states → Import enabled only when ready → after success land on DRAFT (or list refresh). |
| **Out of this UX** | Full library export UI; multi-pack batch import wizard; Wave 8 seed screens. |

Charter Pending **P-Q4** is **resolved** by this section (promote to confirmed for Wave 7).

---

## 6. Primary journey

### 6.1 Export promotion pack (UAT)

1. Authorized operator requests single-template (or library) export with **promotion closure**.
2. System assembles E01 v2 baseline + embeds asset binaries + nesting graph closure + master DOCX.
3. System strips secrets; omits brand/entity sidecars; writes export audit; returns ZIP.

### 6.2 Dry-run on PROD (API + UI)

1. Operator opens Import dialog (or calls API), selects pack + target `masterId` + conflict policy.
2. Operator runs **Check dependencies** / `dryRun=true`.
3. System returns dependency report; **no** business writes; UI renders report; Import gated on `readyToCommit`.

### 6.3 Commit import + PROD lifecycle

1. On `readyToCommit=true`, operator commits.
2. System re-validates; transactional materialize (clauses / assets / optional DRAFT master) + template **DRAFT**.
3. Operator re-tests, re-approves, re-publishes on PROD; letterhead must reach **APPROVED** via normal review if newly materialized.

---

## 7. System responses

| Situation | Response |
| --- | --- |
| Promotion export success | `200` `application/zip`; assets binaries + nesting closure present; audit |
| Promotion export nesting illegal | Fail-closed (IBL-E6 code family); no partial pack |
| Missing asset object at export | Fail-closed for that key (blocking export) — pack must be closed |
| Dry-run | `200` + report; `imported=false`; zero business writes; dry-run audit |
| Commit blocking | `422` + report; no half-import |
| Commit success | `201`; template `DRAFT`; optional materialized DRAFT master/assets/clauses; import audit |
| Unauthorized | `403` |
| Brand/entity sidecar requested/required | Must not appear; path does not depend on retired catalogs |
| Attempt to land master APPROVED via pack | Rejected fail-closed |

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-SYS-NORM-PP-001 — Promotion export embeds asset binaries

**Given** an export-eligible template whose bindings reference ≥1 library asset key with object bytes present  
**And** the caller requests export with the **promotion closure** profile and `format=zip`  
**When** the export completes  
**Then** the ZIP contains `artifacts/assets/{assetKey}` bytes for each manifest key  
**And** `assetKeyManifest` still lists those keys  
**And** the pack contains no DocumentBrand/LegalEntity sidecar catalogs  
**And** the pack contains no secrets / credentials / test-data variable plaintext

### BDD-SYS-NORM-PP-002 — Promotion export includes clause nesting closure

**Given** a template that references a content module which nests another module (depth ≥ 2, ≤ 8)  
**When** promotion export runs  
**Then** `clauseSnapshots` include the full nesting closure  
**And** `clauseNestingGraph` describes the edges  
**And** export does not silently truncate the closure

### BDD-SYS-NORM-PP-003 — Nesting cycle / depth fail-closed on export

**Given** a nesting graph that would violate ADR-0067 (cycle or depth > 8)  
**When** promotion export is requested  
**Then** the API fails closed with a stable nesting error code/messageKey  
**And** no downloadable half-pack is returned

### BDD-SYS-NORM-PP-004 — Default E01 v2 remains keys-only

**Given** the same template as PP-001  
**When** export uses `bundleVersion=2` **without** promotion closure profile  
**Then** ZIP has no `artifacts/assets/` binary tree (E01-C7 preserved)  
**And** CE-E01 scenarios remain green

### BDD-SYS-NORM-PP-005 — No brand/entity sidecar (D1 alignment)

**Given** D1 retirement is in effect  
**When** a promotion pack is exported  
**Then** pack JSON/ZIP does not require or embed DocumentBrand/LegalEntity sidecar catalogs  
**And** letterhead evidence is via master/letterhead artifacts only

### BDD-SYS-NORM-PP-006 — Library promotion ZIP carries closure

**Given** ≥2 export-eligible templates with shared asset keys under promotion profile  
**When** `POST …/library/export` with promotion closure runs  
**Then** each nested `templates/{id}.zip` is a promotion pack (binaries + nesting as required)  
**And** root-level asset binaries are deduped when present  
**And** default non-promotion library export behavior is unchanged

### BDD-SYS-NORM-PP-007 — Dry-run zero writes (API)

**Given** known baseline counts for templates, content modules, masters, and library assets  
**When** `POST …/templates/import` with a promotion ZIP + `dryRun=true` + valid `masterId`  
**Then** HTTP `200`, `imported=false`, full `dependencyReport`  
**And** baseline counts are unchanged  
**And** `TEMPLATE_IMPORT_DRY_RUN` audit exists (no sensitive payloads)

### BDD-SYS-NORM-PP-008 — Dry-run asset binary → WILL_MATERIALIZE

**Given** promotion pack embeds binary for key `k` and target library lacks `k`  
**When** dry-run runs  
**Then** the asset item is non-blocking (`WILL_MATERIALIZE` or `ASSET_WILL_MATERIALIZE`)  
**And** missing binary + missing target key remains **blocking**

### BDD-SYS-NORM-PP-009 — Dry-run nesting closure covered

**Given** pack includes nested clause snapshots for the closure  
**And** target lacks those modules  
**When** dry-run runs  
**Then** nested clauses are `WILL_MATERIALIZE` (or OK if present)  
**And** incomplete nesting closure without snapshots is **blocking** (`CLAUSE_NESTING` / missing)

### BDD-SYS-NORM-PP-010 — Commit lands template DRAFT; no publish shortcut

**Given** `readyToCommit=true` for a promotion pack  
**When** commit import runs (`dryRun=false`)  
**Then** HTTP `201` and the template status is **`DRAFT`**  
**And** the template is **not** PUBLISHED / PENDING_RELEASE  
**And** PROD requires re-test → re-approve → re-publish before a new release exists

### BDD-SYS-NORM-PP-011 — Two-phase master P2 — no skip APPROVED

**Given** a promotion pack whose embedded master DOCX does not match any APPROVED target master  
**And** commit is configured/allowed to materialize a master from the pack  
**When** commit succeeds  
**Then** any materialized master/letterhead status is **`DRAFT`** (not APPROVED)  
**And** subsequent template publish/bind still requires PROD master **APPROVED** via normal review  
**And** any request that would set master APPROVED via pack import is rejected fail-closed

### BDD-SYS-NORM-PP-012 — Commit blocking → no half-import

**Given** dry-run would yield `readyToCommit=false`  
**When** commit is attempted  
**Then** `422` `api.error.template.importDependenciesUnsatisfied` with `dependencyReport`  
**And** no new template DRAFT, no partial clause/asset/master rows remain

### BDD-SYS-NORM-PP-013 — Transaction rollback on mid-commit failure

**Given** pre-check passes but asset or clause materialization fails mid-transaction  
**When** commit runs  
**Then** the operation errors  
**And** no half-imported template/clause/asset/master residue remains

### BDD-SYS-NORM-PP-014 — Secrets never appear

**Given** source templates have API policies/credentials and test datasets  
**When** promotion export, dry-run report, or Import UI report is produced  
**Then** none contain secrets, credential material, or test-data variable plaintext

### BDD-SYS-NORM-PP-015 — Authorization fail-closed

**Given** a user without matrix §5 export/import capability (e.g. `TEMPLATE_TESTER` alone)  
**When** promotion export, dry-run, or commit is requested (API or UI entry)  
**Then** the system responds `403` (API) or hides/disables the Import affordance (UI)  
**And** no pack contents or dependency details for unauthorized resources are leaked

### BDD-SYS-NORM-PP-016 — UI dry-run report rendering (P-Q4)

**Given** an entitled operator opens Templates → Import and selects a promotion ZIP + APPROVED master  
**When** the operator clicks **Check dependencies**  
**Then** the dialog shows `readyToCommit` and counts plus itemized dependency rows  
**And** no business entities are created  
**And** English-first strings are used (zh-CN may exist as secondary)

### BDD-SYS-NORM-PP-017 — UI Import gated on readyToCommit

**Given** the latest dry-run report has `readyToCommit=false`  
**When** the operator views the Import dialog  
**Then** **Import** is disabled  
**And** when a new dry-run returns `readyToCommit=true`, **Import** becomes enabled  
**And** changing file/master/policy clears the report and disables **Import** until re-check

### BDD-SYS-NORM-PP-018 — UI commit success → DRAFT observable

**Given** UI dry-run is green (`readyToCommit=true`)  
**When** the operator clicks **Import** and the API returns success  
**Then** the UI signals success and the imported template is observable as **DRAFT**  
**And** the dialog does not claim the template is published

### BDD-SYS-NORM-PP-019 — CE-E01 fail-closed import preserved

**Given** a non-promotion v2 ZIP with unmet blocking dependencies  
**When** commit import runs  
**Then** behavior remains E01 fail-closed (`422` + report, no half-import)  
**And** Wave 7 changes do not weaken that gate

### BDD-SYS-NORM-PP-020 — Governance non-goals hold

**Given** Wave 7 delivery closes against this spec  
**When** Done evidence is reviewed  
**Then** checklist **#3b** / **#5a** are not flipped GO  
**And** Task Master **#53** is not marked Done  
**And** SYS-NORM program is not claimed Done  
**And** Wave 8 / parked UX items are not implemented in this leaf  
**And** formal phase remains **None**

---

## 9. Boundary / exception

| Boundary | Behavior |
| --- | --- |
| Empty `assetKeyManifest` | Valid; no asset binary tree required |
| JSON-only v2 (no ZIP) | May dry-run inspect; cannot commit as self-contained promotion pack without binaries/DOCX |
| Master already APPROVED + hash match | `MASTER_PIN` `OK`; no master materialization required |
| Cross-group master | Existing `masterGroupMismatch` fail-closed |
| Concurrent dry-runs | Allowed; no mutual writes |
| Oversized pack | Existing size guards; fail-closed, no silent truncate |
| Library import API | Still **out of scope** — unwrap nested ZIPs and use single-template import |
| Brand/entity APIs | Remain retired (404/410); promotion must not revive them |

---

## 10. Observable evidence

| Evidence | Proof |
| --- | --- |
| ZIP listing | `artifacts/master.docx`, `artifacts/assets/*`, nesting graph/snapshots |
| API | dry-run report; commit `201` DRAFT; `422` blocking |
| DB | dry-run unchanged counts; commit DRAFT only; master not auto-APPROVED |
| UI | Import dialog dry-run report + gated Import |
| Audit | export / `TEMPLATE_IMPORT_DRY_RUN` / import (no secrets) |
| Gates | `mvn verify`; FE lint/type-check/test/build; Playwright E2E + UIUX; queued Docker deploy |
| Governance | #3b/#5a/#53/program Done untouched |

---

## 11. Traceability

| Source | Relation |
| --- | --- |
| [system-normalization-program.md](./system-normalization-program.md) §2.7 / Wave 7 | Charter design facts + P-Q4 resolution |
| [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) Wave 7 | Program wave row |
| [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) | Upstream single-template pack + dry-run/commit — **extend** |
| [ce-e03-full-library-export.md](./ce-e03-full-library-export.md) | Upstream library ZIP — **extend** under promotion profile |
| [ce-e02-asset-library.md](./ce-e02-asset-library.md) | Asset materialize target |
| [sys-norm-d1-brands.md](./sys-norm-d1-brands.md) D1-005 | No brand/entity sidecar |
| [ibl-e6-clause-nesting-governance.md](./ibl-e6-clause-nesting-governance.md) / ADR-0067 | Nesting closure rules |
| [ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) Decision 5 | Promotion packs + two-phase P2 |
| [PRD.md](../product/PRD.md) §10 | Import → DRAFT; re-test/approve/publish |
| [permission-matrix.md](../security/permission-matrix.md) §5 | Export/import authz |
| Task Master **#151** | Execution leaf (**In Progress** / sole-active) |

---

## 12. TDD Red mapping (suggested)

| Layer | Suggested failing tests |
| --- | --- |
| Backend export | `promotionExport_embedsAssetBinaries`; `promotionExport_includesNestingClosure`; `promotionExport_cycleFailsClosed`; `defaultV2_stillKeysOnly`; `promotionExport_omitsBrandEntitySidecar`; `promotionExport_stripsSecrets` |
| Backend dry-run | `dryRun_assetBinary_willMaterialize`; `dryRun_missingBinary_blocking`; `dryRun_nestingIncomplete_blocking`; `dryRun_zeroWrites` |
| Backend commit | `commit_templateDraftOnly`; `commit_masterMaterialize_draftNotApproved`; `commit_blocking_422_noPartial`; `commit_midFailure_rollback`; `e01_nonPromotion_failClosed_regression` |
| Authz | `promotionExportImport_forbiddenForTester` |
| Frontend | `importDialog_dryRunRendersReport`; `importDialog_importDisabledUntilReady`; `importDialog_clearsReportOnInputChange` |
| E2E | promotion ZIP dry-run ready/blocking + gated import → DRAFT |
| Contract | OpenAPI promotion profile param; asset binary paths; extended dependencyType/codes |

---

## 13. Handoff

```
bdd_readiness: ready
task_ids: ["151"]
slice: sys-norm-promotion-pack
placement: ISOLATED
worktree_path: d:\working\DGE-sys-norm-promotion-pack
branch: feat/sys-norm-promotion-pack
behavior_doc: docs/behavior/sys-norm-promotion-pack.md
frontend_ui_in_scope: true
scenario_ids: BDD-SYS-NORM-PP-001 … BDD-SYS-NORM-PP-020
p_q4: resolved (§5.1 — Import dialog Check dependencies + gated Import)
next: doc-keeper → backend-engineer + frontend-engineer
formal_phase: None
vetoes:
  - checklist-#3b/#5a
  - CE-O02
  - "#53"
  - Wave-8
  - parked-UX
  - program-Done
```
