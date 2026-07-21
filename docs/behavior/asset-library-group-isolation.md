---
id: DOC-BEHAVIOR-ASSET-LIBRARY-GROUP-ISOLATION
type: Behavior Spec
status: Confirmed
readiness: ready
program: post-SYS-NORM parked UX (§4a)
slice: asset-library-group-isolation
taskMaster: 154
supersedes:
  - docs/behavior/ce-e02-asset-library.md#E02-C12 (platform-shared catalog)
  - docs/behavior/ce-e02-asset-library.md#E02-C1 (bare assetKey ≡ storage key, platform-wide)
  - docs/behavior/ce-e02-asset-library.md#E02-C7 (ACTIVE conflict scoped platform-wide)
  - docs/behavior/ce-e02-asset-library.md#E02-C13 (resolver call-site group-agnostic)
related:
  - docs/behavior/ce-e02-asset-library.md
  - docs/operations/demo-acceptance-asset-seed.md
  - docs/security/permission-matrix.md
  - docs/domain/domain-model.md
  - docs/api/openapi-v1.yaml
  - docs/plan/system-normalization-program-2026-07.md
---

# Asset library group isolation

> **TM:** Task Master **#154** · slice `asset-library-group-isolation`  
> **User confirmation (2026-07-22):** Asset library **MUST** have group isolation
> (supersedes CE-E02 platform-shared catalog **E02-C12**).  
> **Hard isolation v1:** no cross-group asset share.  
> **Migration:** fail-closed honest quarantine (DISABLED + admin rehome) — **not** silent
> assign-to-`PLATFORM`/`CORP` as if ownership were known.  
> **Out of scope:** Binding editor re-layout; Auto `referenceKey`; CE-O02; checklist
> **#3b** / **#5a**; CE umbrella **#53**; inventing a fictional `PLATFORM` business group.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: true
open_questions: []
owning_doc: docs/behavior/asset-library-group-isolation.md
amendment_doc: docs/behavior/ce-e02-asset-library.md (§15 Amendment ALGI)
task_ids: ["154"]
queue_slice_id: asset-library-group-isolation
scenario_ids:
  - BDD-ALGI-001 … BDD-ALGI-018
scenario_count: 18
migration_decision: ALGI-M1 quarantine-disable + admin rehome
permission_matrix_update: done (doc-keeper 2026-07-22) — scope CE-E02 actions by authorized groupCode
```

---

## 1. Actor / role

| Actor | Role | Concern |
| --- | --- | --- |
| **GLOBAL_ADMIN** | Global admin | List **all** groups (optional `groupCode` filter); upload/disable any group; SEAL allowed |
| **GROUP_ADMIN** | Group admin | List/upload/disable **only** within authorized groups; SEAL allowed in scope |
| **DOCUMENT_AUTHOR** | Document author (Wave 5 union of former designer∪author) | List in authorized groups; upload `IMAGE`/`OTHER` in authorized groups; **no** SEAL; **no** disable |
| **TEMPLATE_TESTER** | Tester | List **ACTIVE only** in authorized groups; no upload/disable |
| **LEGAL_REVIEWER / AUDIT_ADMIN** | Other management | **No** asset-library route (fail-closed); audit via existing audit APIs |
| **系统** | Catalog + MinIO + `GroupAccessService` + render path | Enforce `(groupCode, assetKey)` identity; group-scoped resolve; audit |
| **渲染路径** | Template preview / generate / runtime | Resolve `imageRef`/`sealRef` **only** against template owning group's ACTIVE assets |

Role codes follow Wave 5 six-role catalog ([sys-norm-roles.md](./sys-norm-roles.md) /
[permission-matrix.md](../security/permission-matrix.md)). CE-E02 historical names
(`TEMPLATE_AUTHOR`, `TEMPLATE_APPROVER`, `MASTER_DESIGNER`) map per ADR-0070.

---

## 2. User goal

1. Each managed asset is owned by exactly one business `groupCode` (hard isolation v1).
2. Operators list / upload / disable assets only within authorized groups; GLOBAL may see all
   or filter by group.
3. Template bindings continue to use bare `imageRef` / `sealRef` (= logical `assetKey`), but
   resolution succeeds **only** when that key is **ACTIVE** in the **template's** `groupCode`.
4. Existing platform-shared rows do **not** silently become “shared forever” or fake-owned by
   an invented platform group — migration is honest and fail-closed until an admin rehomes.

---

## 3. Trigger

- Authorized user opens Asset library (`/library/assets`) and uses group filter / upload with group.
- `GET/POST /api/management/v1/library/assets` (group-scoped) and disable for a
  `(groupCode, assetKey)` identity.
- Template preview / generate / runtime resolves `imageRef` / `sealRef` for a template with
  known `groupCode`.

---

## 4. Preconditions

- CE-E02 (#79) delivered (platform-shared catalog baseline to supersede).
- Wave 5 six-role catalog live; `GroupAccessService` fail-closed patterns exist for other
  group-scoped resources.
- Seed business groups in demo/acceptance typically include `CORP` / `RETAIL` / `TRADE` /
  `WEALTH` — **there is no durable `PLATFORM` asset-owner group** in domain seed reality.
- Product default Asset Library remains honest-empty when zero managed rows
  ([demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md)).
- This slice delivers in isolation worktree `asset-library-group-isolation`; Binding editor /
  Auto `referenceKey` vetoed.

---

## 5. Confirmed decisions (locked)

| ID | Decision | Source |
| --- | --- | --- |
| **ALGI-C1** | **Owning group required.** Every `library_asset` row has non-null `groupCode` (owning business group). v1 **hard isolation:** no `sharedGroupCodes`, no cross-group read/write/resolve. | User confirm 2026-07-22 |
| **ALGI-C2** | **Uniqueness.** Natural identity is **`(groupCode, assetKey)`** (both trim). Same `assetKey` **may** exist in different groups. Concurrent ACTIVE conflict is per-group only → `409` `api.error.assetLibrary.assetKeyConflict`. | Prefer composite uniqueness |
| **ALGI-C3** | **Supersede E02-C12.** Platform-shared catalog is **withdrawn** for ongoing product behavior. `GROUP_ADMIN` is **not** equivalent to `GLOBAL_ADMIN` across all groups. | Supersede CE-E02 |
| **ALGI-C4** | **Storage key namespacing (amends E02-C1).** Logical binding ref remains bare `assetKey`. Physical MinIO object key for a managed asset is **group-namespaced**: `{groupCode}/{assetKey}` (plus existing optional `.png`/`.jpg`/`.jpeg` candidate suffixes under that namespaced key). Bindings **must not** embed the `groupCode/` prefix. | Composite uniqueness + resolver honesty |
| **ALGI-C5** | **Group-scoped resolve (amends E02-C13 call-site).** For template `T` with `groupCode=G` and ref `K`: succeed **iff** catalog has **ACTIVE** `(G, K)` and namespaced object exists. If another group has ACTIVE `(H, K)` only → **fail-closed** same not-found shape (`IMAGE_ASSET_NOT_FOUND` / `SEAL_ASSET_NOT_FOUND` / existing messageKeys). Raw MinIO presence of a bare or foreign-group key **must not** satisfy managed resolve for `G`. Demo classpath tier remains N23 / TPC orthogonal (prod off). | Fail-closed cross-group |
| **ALGI-C6** | **List scope.** Non-global actors: results ∩ authorized groups. Optional query `groupCode` (exact) further filters; unauthorized `groupCode` → empty page (no leak) consistent with LR-C5 catalog pattern. `GLOBAL_ADMIN`: omit `groupCode` → all groups; with filter → that group only. Default `status=ACTIVE` unchanged (E02-C10). Response includes `groupCode`. | Target #2 |
| **ALGI-C7** | **Upload requires group.** Multipart **must** include `groupCode` (existing business group). Actor must be authorized for that group (GLOBAL always). Missing/blank → `422` `api.error.assetLibrary.groupCodeRequired` (stable key). Unauthorized group → `403`. SEAL still GLOBAL/GROUP only (Wave 5). | Target #7 + CE-E02 SEAL gate |
| **ALGI-C8** | **Disable identity.** Disable targets `(groupCode, assetKey)`. API must identify both (path or required query — OpenAPI locks one shape). Actor must be GLOBAL or GROUP admin authorized for that `groupCode`. Semantics retain E02-C6 (DISABLED + delete namespaced resolvable keys). Idempotent disable of already DISABLED remains `200`. | Target #2 |
| **ALGI-C9** | **DISABLED re-upload (amends E02-C7).** Re-upload allowed when same `(groupCode, assetKey)` is DISABLED → ACTIVE + rewrite namespaced object + `ASSET_LIBRARY_REUPLOAD`. ACTIVE conflict still `409` **within that group**. | CE-E02 ops continuity |
| **ALGI-C10** | **FE.** Asset library list: group filter via **ScopedGroupSelect** (or identical scoped options source). Upload dialog **requires** group selection before submit; hide/disable controls fail-closed. English-first i18n; bank OA. | Target #7 |
| **ALGI-C11** | **No cross-group share v1.** Explicit non-goal: share-to-groups, copy-on-read, global asset pool. Future share would need a new BDD. | Optional v1 locked as hard isolation |
| **ALGI-C12** | **Migration ALGI-M1 (LOCKED) — quarantine-disable + admin rehome.** See §6. **Rejected:** silent assign-all-to-`PLATFORM` (group does not exist in seed reality); silent assign-all-to-`CORP` **as ACTIVE** (would falsely claim ownership and keep cross-demo resolves working as if shared). | Fail-closed honest |
| **ALGI-C13** | **Demo/验收 seed (amends Wave 8 seeder contract).** When `DOCGEN_SEED_DEMO_ASSET_LIBRARY=true`, seeder must create **group-scoped** ACTIVE `IMG-1` / `SEAL-1` for **each existing seeded business group** among `{CORP,RETAIL,TRADE,WEALTH}` that is present — not one platform-shared row. Prod default seed **off** unchanged. | Seed reality + isolation |
| **ALGI-C14** | **Doc-keeper SoT sync (2026-07-22).** permission-matrix §13.2 CE-E02+ALGI group scope; domain-model + data-storage-view `groupCode` / composite identity / namespaced storage; OpenAPI list/upload/disable + `AssetLibraryAssetView.groupCode` + required upload `groupCode` + disable query `groupCode`; requirements/PRD platform-shared wording superseded. Binding editor / Auto `referenceKey` remain OOS. | Doc-keeper stage |
| **ALGI-C15** | **Explicit non-goals.** Binding editor re-layout; Auto `referenceKey`; CE-E03/CE-O01/CE-O02; virus scan; inventing `PLATFORM` group; flipping **#3b/#5a**; marking **#53** Done. | Batch vetoes |

### 5.1 Action matrix (group-scoped; Wave 5 roles)

| Action | GLOBAL | GROUP (authorized groups only) | DOCUMENT_AUTHOR (authz groups) | TEMPLATE_TESTER (authz groups) | LEGAL / AUDIT |
| --- | --- | --- | --- | --- | --- |
| List (incl. DISABLED query) | ✓ all / filter | ✓ scoped | ✓ scoped (DISABLED query OK) | ✓ **ACTIVE only** | — |
| Upload IMAGE / OTHER | ✓ + `groupCode` | ✓ + `groupCode` in scope | ✓ + `groupCode` in scope | — | — |
| Upload SEAL | ✓ | ✓ in scope | — | — | — |
| Disable | ✓ | ✓ in scope | — | — | — |
| Route `route.asset-library-management` | ✓ | ✓ | ✓ | ✓ | — |

Capability `manageAssetLibrary` still gates route visibility; **fine-grained actions + group
intersection** enforced server-side (fail-closed).

---

## 6. Migration strategy (ALGI-M1) — testable

### 6.1 Why not default ACTIVE ownership

| Candidate | Why rejected |
| --- | --- |
| Assign all rows ACTIVE to `PLATFORM` | No durable `PLATFORM` asset-owner group in domain/demo seed |
| Assign all rows ACTIVE to `CORP` | Demo templates in RETAIL/TRADE/WEALTH also referenced shared keys; ACTIVE-on-CORP would **lie** about ownership and leave other groups broken without an honest empty/quarantine signal |
| Leave `groupCode` null | Violates ALGI-C1; ambiguous resolve |

### 6.2 Locked upgrade steps

For **every pre-existing** `library_asset` row that lacks owning-group semantics (CE-E02 era):

1. Set `group_code` = **quarantine owner**:
   - Prefer existing seed group **`CORP`** if that group row exists in the deployment's group
     catalog;
   - Else = lexicographically first existing non-deleted business `groupCode` in that
     deployment (deterministic, documented in migration comment/audit).
2. Force `status = DISABLED` (even if previously ACTIVE).
3. **Delete** any resolvable object keys for that asset under the **legacy bare** `assetKey`
   (± extension candidates) **and** under the new namespaced form if partially written —
   same fail-closed posture as E02 disable — so MinIO alone cannot satisfy resolve.
4. Emit audit `ASSET_LIBRARY_MIGRATE_QUARANTINE` (summary: `assetKey`, quarantine
   `groupCode`, actor=`SYSTEM` / migration id; **no** bytes).
5. **Admin rehome (ops):** authorized admin uploads (or DISABLED re-upload) the asset into
   the **intended** `groupCode` with same or new `assetKey` → ACTIVE under ALGI-C7/C9.
   No silent auto-ACTIVE.

**Post-migration observable:** template resolve for former shared keys fails closed until a
group-local ACTIVE row exists for that template's group (unless demo seeder ALGI-C13 ran).

### 6.3 Empty / greenfield databases

If zero `library_asset` rows: migration is a no-op beyond schema (`group_code` NOT NULL +
unique `(group_code, asset_key)`). Honest empty preserved.

---

## 7. Primary journeys

### 7.1 Group-scoped upload + list

1. `DOCUMENT_AUTHOR` authorized for `RETAIL` opens Asset library → selects group `RETAIL`
   (ScopedGroupSelect) → uploads `IMAGE` / `IMG-LOGO`.
2. System writes catalog `(RETAIL, IMG-LOGO)` ACTIVE + MinIO `RETAIL/IMG-LOGO` (± ext) →
   audit `ASSET_LIBRARY_UPLOAD`.
3. List with group filter `RETAIL` shows the row; `CORP`-only actor does not see it.

### 7.2 GLOBAL filter

1. `GLOBAL_ADMIN` lists without `groupCode` → sees assets from multiple groups (incl.
   `groupCode` column).
2. Applies filter `groupCode=CORP` → only CORP rows.

### 7.3 Resolve fail-closed cross-group

1. Template in `CORP` binds `imageRef=IMG-LOGO`.
2. Only `RETAIL` has ACTIVE `IMG-LOGO` → preview/generate → `IMAGE_ASSET_NOT_FOUND`
   (fail-closed).
3. After CORP uploads ACTIVE `IMG-LOGO` → resolve succeeds from CORP namespaced object.

### 7.4 Migration quarantine then rehome

1. Upgrade DB with legacy ACTIVE bare-key row `IMG-1`.
2. After migration: row DISABLED under quarantine `groupCode`; bare MinIO key gone; resolve
   fails for all template groups.
3. Admin re-uploads `IMG-1` to `CORP` → ACTIVE; CORP templates resolve; other groups still
   fail until they have their own ACTIVE row (or demo seeder ALGI-C13).

---

## 8. System responses (success / fail-closed)

| Operation | HTTP | Notes |
| --- | --- | --- |
| Upload | `201` | `result` includes `groupCode`; namespaced object exists |
| List | `200` | Scoped page; items include `groupCode` |
| Disable | `200` | `DISABLED`; namespaced object not resolvable |
| Upload missing `groupCode` | `422` | `api.error.assetLibrary.groupCodeRequired` |
| Upload/list/disable outside authz group | `403` | No existence leak for disable-by-guess when unauthorized |
| Resolve missing / cross-group / DISABLED | existing not-found | No new success path via foreign group |

---

## 9. Acceptance scenarios (Given / When / Then)

### BDD-ALGI-001 — Upload requires groupCode and scopes ownership

**Given** `DOCUMENT_AUTHOR` authorized for `RETAIL` (not `CORP`)  
**When** `POST …/library/assets` multipart with `groupCode=RETAIL`, `assetKey=IMG-ALGI-001`, `IMAGE`, valid PNG  
**Then** `201`; catalog `(RETAIL, IMG-ALGI-001)` `ACTIVE`; object exists at namespaced key  
**And** list as CORP-only actor does not include that row  

### BDD-ALGI-002 — Upload missing groupCode rejected

**Given** authorized upload actor  
**When** upload omits `groupCode` or sends blank  
**Then** `422` `api.error.assetLibrary.groupCodeRequired`  
**And** no catalog row / no object write  

### BDD-ALGI-003 — Upload to unauthorized group fail-closed

**Given** `GROUP_ADMIN` authorized only for `CORP`  
**When** upload with `groupCode=RETAIL`  
**Then** `403`  
**And** no write  

### BDD-ALGI-004 — Uniqueness is per (groupCode, assetKey)

**Given** `(CORP, IMG-ALGI-004)` ACTIVE  
**When** upload `(RETAIL, IMG-ALGI-004)` IMAGE by actor authorized for RETAIL  
**Then** `201` (allowed)  
**When** second upload `(CORP, IMG-ALGI-004)` while ACTIVE  
**Then** `409` `api.error.assetLibrary.assetKeyConflict`  

### BDD-ALGI-005 — List scoped to authorized groups

**Given** ACTIVE assets in `CORP` and `RETAIL`; actor authorized only for `CORP`  
**When** `GET …/library/assets` without `groupCode`  
**Then** `200`; only `CORP` rows; no RETAIL keys leaked  

### BDD-ALGI-006 — GLOBAL_ADMIN lists all and can filter

**Given** ACTIVE assets in `CORP` and `RETAIL`; `GLOBAL_ADMIN` session  
**When** list without `groupCode`  
**Then** both groups' rows returned (each with `groupCode`)  
**When** list with `groupCode=CORP`  
**Then** only CORP rows  

### BDD-ALGI-007 — Unauthorized groupCode filter empty (no leak)

**Given** actor authorized only for `CORP`  
**When** list with `groupCode=RETAIL`  
**Then** `200` empty page (or equivalent empty content) — **not** `200` with RETAIL data  

### BDD-ALGI-008 — Disable scoped + removes namespaced object

**Given** ACTIVE `(CORP, IMG-ALGI-008)` resolvable for CORP templates  
**When** `GROUP_ADMIN` authorized for CORP disables that identity  
**Then** `200`; status `DISABLED`; CORP resolve → `IMAGE_ASSET_NOT_FOUND`  
**And** audit `ASSET_LIBRARY_DISABLE`  

### BDD-ALGI-009 — Disable outside authorized group forbidden

**Given** ACTIVE `(RETAIL, IMG-ALGI-009)`; actor GROUP admin for CORP only  
**When** disable RETAIL identity  
**Then** `403` (no existence oracle)  

### BDD-ALGI-010 — Resolve only within template group ACTIVE

**Given** template `T` with `groupCode=CORP`; ACTIVE `(RETAIL, IMG-ALGI-010)` only  
**When** preview/generate resolves `imageRef=IMG-ALGI-010` for `T`  
**Then** fail-closed `IMAGE_ASSET_NOT_FOUND` (same messageKey family as missing)  
**Given** ACTIVE `(CORP, IMG-ALGI-010)` also exists  
**When** resolve again for `T`  
**Then** success embeds CORP namespaced bytes (not RETAIL's)  

### BDD-ALGI-011 — Resolve ignores foreign MinIO bare key

**Given** template `groupCode=CORP`; no ACTIVE catalog `(CORP, IMG-ALGI-011)`  
**And** object storage still has legacy/foreign bytes for bare `IMG-ALGI-011` or `RETAIL/IMG-ALGI-011`  
**When** resolve `imageRef=IMG-ALGI-011` for that template  
**Then** fail-closed not-found (catalog gate wins)  

### BDD-ALGI-012 — Migration quarantine disables legacy rows

**Given** pre-ALGI database row: bare-key ACTIVE `IMG-ALGI-012` with resolvable MinIO object (no group)  
**When** ALGI-M1 migration runs  
**Then** row has non-null quarantine `groupCode` (CORP if present else first group); `status=DISABLED`  
**And** legacy bare resolvable keys are removed  
**And** audit `ASSET_LIBRARY_MIGRATE_QUARANTINE`  
**And** template resolve for `IMG-ALGI-012` fails closed in every business group  

### BDD-ALGI-013 — Admin rehome after quarantine

**Given** quarantined DISABLED `(CORP, IMG-ALGI-013)` after migration  
**When** authorized admin uploads `groupCode=CORP`, `assetKey=IMG-ALGI-013`, valid IMAGE  
**Then** `201`; `ACTIVE`; CORP template resolve succeeds  
**And** audit `ASSET_LIBRARY_REUPLOAD` or `ASSET_LIBRARY_UPLOAD` per E02/ALGI rules  

### BDD-ALGI-014 — SEAL upload still admin-gated and group-scoped

**Given** `DOCUMENT_AUTHOR` authorized for `CORP`  
**When** upload `SEAL` with `groupCode=CORP`  
**Then** `403`  
**Given** `GROUP_ADMIN` authorized for `CORP`  
**When** upload `SEAL` / `SEAL-ALGI-014` with `groupCode=CORP`  
**Then** `201`; CORP `resolveSealRef` succeeds; RETAIL template with same key fails closed  

### BDD-ALGI-015 — FE group filter + upload requires group (E2E)

**Given** authorized browser session on Docker acceptance stack  
**When** open `/library/assets`  
**Then** group filter (ScopedGroupSelect) is present and usable  
**When** open upload without selecting group and attempt submit  
**Then** UI blocks submit (required group); no new row  
**When** select authorized group + IMAGE + key + file → submit  
**Then** list shows row with that group; no console fatal; UIUX Critical=0 (sample)  

### BDD-ALGI-016 — FE GLOBAL can clear/omit filter to see all (E2E)

**Given** `GLOBAL_ADMIN` browser session; assets exist in ≥2 groups  
**When** clear group filter (or “all groups”)  
**Then** rows from multiple groups visible  
**When** filter one group  
**Then** only that group's rows  

### BDD-ALGI-017 — Demo seeder group-scopes keys when enabled

**Given** `DOCGEN_SEED_DEMO_ASSET_LIBRARY=true` and seed groups CORP+RETAIL exist  
**When** application seed runs  
**Then** ACTIVE `(CORP, IMG-1)` and `(RETAIL, IMG-1)` (and SEAL-1 analog) exist as managed rows  
**And** no platform-shared unscoped row remains the only copy  

### BDD-ALGI-018 — TEMPLATE_TESTER read-only ACTIVE in scope

**Given** `TEMPLATE_TESTER` authorized for `CORP` only; CORP has ACTIVE+DISABLED; RETAIL has ACTIVE  
**When** list  
**Then** `200` CORP ACTIVE only  
**When** upload or disable  
**Then** `403`  

---

## 10. Boundary / exception

| Boundary | Behavior |
| --- | --- |
| Concurrent upload same `(groupCode, assetKey)` | One wins; other `409`; never two ACTIVE for same identity |
| Cross-group share / copy | **Forbidden v1** |
| Resolve with blank template groupCode | Fail-closed not-found / defensive error (no platform fallback) |
| Quarantine owner group missing entirely (zero groups) | Migration fails closed at deploy/migration time (cannot invent PLATFORM); ops must seed groups first — document in Flyway comment |
| Demo classpath tier on + catalog miss | LAB-only N23 behavior may still resolve classpath; **not** Asset Library catalog; prod default off |
| Promotion-pack asset embed (Wave 7) | Still embeds by `assetKey` bytes in pack; **import materialization** into Asset Library must write **target template/group** scoped rows (implementer aligns with PP APIs; no platform-shared reintroduction) |
| E01 ASSET_KEY probe | Presence = ACTIVE catalog row in the **relevant group context** of the template/bundle under test — not global bare key |

---

## 11. Observable evidence

| Evidence | Proof |
| --- | --- |
| API | List/upload/disable envelopes include/require `groupCode` |
| DB | Unique `(group_code, asset_key)`; no null `group_code` |
| MinIO | Namespaced keys; legacy bare keys removed on quarantine/disable |
| Resolve | Cross-group / quarantined → not-found; same-group ACTIVE → bytes |
| Audit | UPLOAD / DISABLE / REUPLOAD / MIGRATE_QUARANTINE |
| UI | ScopedGroupSelect filter; upload group required |
| Gates | Backend tests BDD-ALGI-001…014/017/018; FE unit; Playwright 015–016; `mvn verify` + FE gates + queued deploy |

---

## 12. Traceability

| Doc | Relation |
| --- | --- |
| This file | **Behavior SoT** for slice `asset-library-group-isolation` / TM **#154** |
| [ce-e02-asset-library.md](./ce-e02-asset-library.md) §15 | Amendment — supersedes E02-C12 / amends C1/C7/C13 |
| [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) / [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md) | Seed contract amend (ALGI-C13) |
| [permission-matrix.md](../security/permission-matrix.md) | **Updated** (doc-keeper 2026-07-22) — group-scope CE-E02 + ALGI actions |
| [domain-model.md](../domain/domain-model.md) | Asset catalog gains `groupCode` + composite identity |
| [openapi-v1.yaml](../api/openapi-v1.yaml) / [contract-outline.md](../api/contract-outline.md) | API contract change in scope |
| [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) §4a | Parked queue head |
| User confirmation 2026-07-22 | Group isolation required; Binding editor / Auto referenceKey vetoed |

---

## 13. TDD Red mapping (suggested)

| Layer | Failing tests first |
| --- | --- |
| Migration | `migrate_legacyRow_quarantineDisabled_bareKeyRemoved` |
| Upload/list/disable | group required; authz scope; composite uniqueness; GLOBAL filter |
| Resolve | `resolve_crossGroupActive_failClosed`; `resolve_sameGroupActive_ok`; `resolve_ignoresBareForeignObject` |
| Seed | `demoSeeder_writesPerGroupActiveKeys` |
| FE | upload requires group; ScopedGroupSelect filter |
| E2E | `asset-library-group-isolation.spec.ts` — filter/upload/GLOBAL all-groups |

---

## 14. Handoff

```
bdd_readiness: ready
task_ids: ["154"]
slice: asset-library-group-isolation
owning_doc: docs/behavior/asset-library-group-isolation.md
amendment_doc: docs/behavior/ce-e02-asset-library.md (§15)
scenario_ids: ["BDD-ALGI-001","BDD-ALGI-002","BDD-ALGI-003","BDD-ALGI-004","BDD-ALGI-005","BDD-ALGI-006","BDD-ALGI-007","BDD-ALGI-008","BDD-ALGI-009","BDD-ALGI-010","BDD-ALGI-011","BDD-ALGI-012","BDD-ALGI-013","BDD-ALGI-014","BDD-ALGI-015","BDD-ALGI-016","BDD-ALGI-017","BDD-ALGI-018"]
migration_decision: ALGI-M1 quarantine-disable (group_code=CORP-if-exists-else-first-group; status=DISABLED; remove legacy bare MinIO keys; audit MIGRATE_QUARANTINE) + admin rehome via group-scoped upload/reupload
open_questions: []
frontend_ui_in_scope: true
backend_api_contract_change: true
permission_matrix_update: doc-keeper
next: plan-orchestrator
```
