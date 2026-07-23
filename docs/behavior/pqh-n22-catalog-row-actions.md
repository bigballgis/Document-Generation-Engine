---
id: DOC-BEHAVIOR-PQH-N22-CATALOG-ROW-ACTIONS
type: Behavior Spec
status: Confirmed
readiness: ready
program: Post-Queue Hardening (PQH Leaf 3)
slice: pqh-n22-catalog-row-actions
taskMaster: "162"
related:
  - docs/plan/post-queue-hardening-program-2026-07.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/behavior/system-normalization-program.md
  - docs/behavior/sys-norm-shell-fluid-nav.md
  - docs/behavior/pqh-n19-n20-entitylink.md
  - frontend/src/components/common/TableEditMoreActions.vue
---

# PQH N22 — Catalog row action pattern (Edit/More)

> **Slice:** `pqh-n22-catalog-row-actions` · Batch Recommendation **`solo`**
> **Member task:** TM **#162** (SYS-NORM residual N22 under PQH Leaf 3) → **Done**
> **Placement:** MAIN after Stage 11 integrate `ef1b505d` (Stage 10 tip `c5121164`; UIUX tip `e60b488f`) · worktree **REMOVED** · **push blocked** (`main` ahead of `origin/main`)
> (was ISOLATED `D:/working/DGE-pqh-n22-catalog-row-actions` · `feat/pqh-n22-catalog-row-actions`).
> **Trace:** SYS-NORM §2.10 **N22** — Catalog row action pattern; Wave 1 delivered
> Users/Groups Edit/More via `TableEditMoreActions` (**BDD-SYS-NORM-W1-007/008**);
> catalog-wide remainder closed under PQH Leaf 3.
> **Locks / vetoes:** do **not** reopen SYS-NORM Waves **0–8**; do **not** flip
> checklist **#3b** / **#5a**; do **not** mark **#53** Done; do **not** activate
> CE-O02 / **#119**; do **not** activate F7 (**#163**). Formal phase **None**.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: false
open_questions: []
owning_doc: docs/behavior/pqh-n22-catalog-row-actions.md
task_ids: ["162"]
queue_slice_id: pqh-n22-catalog-row-actions
member_task_ids: ["162"]
batch_decision: solo
shared_acceptance_surface: >
  Asset Library + Legal Holds + API Invocations Actions columns use
  TableEditMoreActions; Users/Groups regression lock; optional Edit when no edit surface
scenario_ids:
  - BDD-PQH-N22-001 … BDD-PQH-N22-014
scenario_count: 14
in_scope_catalogs:
  - asset-library
  - legal-holds
  - api-invocations
regression_lock_catalogs:
  - users
  - groups
deferred_surfaces:
  - hub-nested-action-tables
  - api-policy-home-alerts-cta
  - task-hub-open
  - template-master-cm-catalogs-no-actions-column
vetoes_applied:
  - checklist-#3b/#5a-GO
  - CE-O02
  - mark-#53-CE-Done
  - activate-#119
  - do-not-reopen-SYS-NORM-Waves-0-8
on_red_split_hint: >
  Peel API Invocations if Asset Library + Legal Holds Edit/More-only path is green
```

---

## 0. Residual inventory (verified against worktree)

| Surface | Current state (pre-leaf) | Residual this leaf |
| --- | --- | --- |
| **Users** `UserManagementListSection` | Already `TableEditMoreActions` (Wave 1) | **Regression lock** |
| **Groups** `GroupManagementPanel` | Already `TableEditMoreActions` (Wave 1) | **Regression lock** |
| **Asset Library** `AssetLibraryListView` | Ad-hoc danger link `Disable` in Actions | **In scope** — More menu command; no invent edit surface |
| **Legal Holds** `LegalHoldListView` | Ad-hoc danger link `Release` in Actions | **In scope** — More menu command; no invent edit surface |
| **API Invocations** `ApiInvocationsView` | Two flat link buttons (Open detail + Open settings) | **In scope** — primary = Open detail; settings under More |
| Templates / Masters / Content modules catalogs | No row Actions column (EntityLink + row navigate) | **Out of scope** — N/A (do not invent Actions) |
| API Policy Home alerts table | Single “Open access” CTA column | **Deferred** — dashboard CTA, not entity Edit/More |
| Hub nested tables (test datasets, bindings, version lines, preview history, CM refs, clause pins, master revision/anchors) | Ad-hoc multi-button Actions | **Deferred** — capacity; defer with evidence |
| Task hub Open column | Single Open affordance | **Deferred** |

---

## 1. Actor / role

| Actor | Role / capability | Concern |
| --- | --- | --- |
| Asset librarian / operator | Can disable library assets (`canDisable`) | Sees consistent Actions; Disable remains available only when permitted and ACTIVE |
| Legal-hold administrator | Can manage holds (`canManage`) | Sees consistent Actions; Release remains available only when permitted and ACTIVE |
| API / ops operator | Can open invocations + package settings | Primary open-detail + secondary settings under More |
| Identity administrator | Users/Groups write access | Unchanged Wave 1 Edit/More presentation |
| Read-only / unauthorized | Lacks manage capability for the catalog | Actions column hidden or commands unavailable (fail-closed; existing gates) |

No new roles, capabilities, or authz bits.

---

## 2. User goal

On high-traffic **management catalogs** that still expose row Actions, the operator sees the
same bank-OA **Edit / More** presentation as Users/Groups:

1. Shared `TableEditMoreActions` primitive (`data-testid="table-edit-more-actions"`).
2. **Primary** control = the main non-destructive row action when one exists (e.g. Open detail).
3. **Secondary / lifecycle / destructive** commands live under **More** (e.g. Disable, Release, Open settings).
4. When a catalog has **no** edit/open surface (Asset Library, Legal Holds today), **Edit is omitted** — do not invent a fake Edit — and the sole lifecycle command still lives under More.

---

## 3. Trigger

| Surface | Trigger |
| --- | --- |
| Asset Library | Operator opens Asset Library list with ≥1 ACTIVE row and disable entitlement |
| Legal Holds | Operator opens Legal Holds list with ≥1 ACTIVE row and manage entitlement |
| API Invocations | Operator opens Invocation records with ≥1 row |
| Users / Groups | Admin opens identity catalogs (regression) |

---

## 4. Preconditions

- SYS-NORM Waves **0–8 Done** (remain Done — do **not** reopen).
- Wave 1 `TableEditMoreActions` shipped for Users/Groups
  (**BDD-SYS-NORM-W1-007/008**).
- PQH Leaf 1 (**#159**+**#160**) and Leaf 2 (**#161**) Done; this leaf is PQH Leaf 3 under TM **#162**.
- Existing domain confirmations and entitlement gates for Disable / Release / open detail /
  open package settings remain authoritative — this leaf is **presentation alignment only**.
- English-first i18n for any new labels (reuse existing keys where possible).

---

## 5. Primary journey

### 5.1 Pattern rules (Confirmed)

| Rule | Confirmed behavior |
| --- | --- |
| R1 | In-scope catalog Actions columns render via `TableEditMoreActions` |
| R2 | When a primary non-destructive row action exists → map it to Edit (label may be domain-specific via `editLabel` / edit slot) |
| R3 | Secondary, lifecycle, and destructive commands → More dropdown items (domain commands unchanged) |
| R4 | When **no** edit/open surface exists → Edit control **hidden** (`showEdit=false` or equivalent); More remains |
| R5 | Alignment/spacing matches Users/Groups (`data-testid="table-edit-more-actions"`) |
| R6 | Fail-closed: Actions column / commands still gated by existing `v-if` entitlements and row state |
| R7 | No API / OpenAPI / permission-matrix change required |

### 5.2 Asset Library

1. Operator opens Asset Library with disable entitlement and an ACTIVE asset.
2. Actions cell shows `TableEditMoreActions` **without** Edit.
3. Operator opens More → **Disable** (existing confirm flow unchanged).
4. Non-ACTIVE rows / no entitlement → no Disable (existing rules).

### 5.3 Legal Holds

1. Operator opens Legal Holds with manage entitlement and an ACTIVE hold.
2. Actions cell shows `TableEditMoreActions` **without** Edit.
3. Operator opens More → **Release** (existing confirm flow unchanged).

### 5.4 API Invocations

1. Operator opens Invocation records with ≥1 row.
2. Actions cell shows `TableEditMoreActions` with primary **Open detail** (Edit slot/label).
3. More contains **Open settings** (package settings navigation).
4. Commands invoke the same handlers as today’s flat buttons.

### 5.5 Users / Groups (lock)

Unchanged Wave 1 Edit + More commands and primitive usage.

---

## 6. System responses (success)

| Surface | Success presentation |
| --- | --- |
| Asset Library | `table-edit-more-actions` present; Disable under More; confirmDisable still runs |
| Legal Holds | `table-edit-more-actions` present; Release under More; confirmRelease still runs |
| API Invocations | Primary Open detail; Open settings under More; drawers/routes unchanged |
| Users / Groups | Still `TableEditMoreActions` with prior commands |

---

## 7. Boundary / exception / fail-closed

| Case | Behavior |
| --- | --- |
| No disable / manage entitlement | Actions column remains hidden (`v-if` unchanged) |
| Row not ACTIVE (asset / hold) | Lifecycle command absent or not offered (unchanged domain rules) |
| Destructive-only catalog | Edit hidden — never invent Edit that opens a non-existent dialog |
| Hub nested action tables | Out of scope this leaf (deferred) |
| Template / Master / CM catalogs | No Actions column invented |
| API Policy Home “Open access” CTA | Deferred (not Edit/More entity pattern) |
| Backend / OpenAPI | **No** contract change |
| SYS-NORM Waves 0–8 / checklist / CE | Unchanged; leaf must not reopen or flip |

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-PQH-N22-001 — Primitive supports optional Edit

**Given** `TableEditMoreActions` is rendered with Edit suppressed (`showEdit=false` or equivalent)  
**When** the actions cell mounts  
**Then** More is visible  
**And** the Edit control is not rendered  
**And** `data-testid="table-edit-more-actions"` is still present

### BDD-PQH-N22-002 — Asset Library Actions uses TableEditMoreActions

**Given** an operator with disable entitlement viewing Asset Library with ≥1 ACTIVE asset  
**When** the Actions column renders  
**Then** the cell uses `TableEditMoreActions`  
**And** it is not a bare standalone danger `el-button` as the sole Actions chrome

### BDD-PQH-N22-003 — Asset Library hides Edit (no invent surface)

**Given** Asset Library Actions on an ACTIVE row  
**When** the actions cell renders  
**Then** Edit is not shown  
**And** More is shown

### BDD-PQH-N22-004 — Asset Library Disable remains under More

**Given** Asset Library Actions on an ACTIVE row with disable entitlement  
**When** the operator opens More  
**Then** Disable is available  
**And** selecting it runs the existing disable confirmation path (`confirmDisable` / equivalent)

### BDD-PQH-N22-005 — Asset Library Actions fail-closed without entitlement

**Given** the operator lacks disable entitlement  
**When** the Asset Library table renders  
**Then** the Actions column is not shown (existing `canDisable` gate)

### BDD-PQH-N22-006 — Legal Holds Actions uses TableEditMoreActions

**Given** an operator with manage entitlement viewing Legal Holds with ≥1 ACTIVE hold  
**When** the Actions column renders  
**Then** the cell uses `TableEditMoreActions`  
**And** it is not a bare standalone danger `el-button` as the sole Actions chrome

### BDD-PQH-N22-007 — Legal Holds hides Edit; Release under More

**Given** Legal Holds Actions on an ACTIVE row  
**When** the actions cell renders  
**Then** Edit is not shown  
**And** More contains Release  
**And** selecting Release runs the existing release confirmation path (`confirmRelease` / equivalent)

### BDD-PQH-N22-008 — Legal Holds Actions fail-closed without entitlement

**Given** the operator lacks manage entitlement  
**When** the Legal Holds table renders  
**Then** the Actions column is not shown (existing `canManage` gate)

### BDD-PQH-N22-009 — API Invocations primary is Open detail

**Given** an Invocation records row  
**When** the Actions column renders  
**Then** `TableEditMoreActions` is used  
**And** the primary (Edit) control is labeled for Open detail (existing i18n key / equivalent)  
**And** activating it opens the same detail drawer/path as today

### BDD-PQH-N22-010 — API Invocations Open settings under More

**Given** an Invocation records row  
**When** the operator opens More  
**Then** Open settings is available  
**And** selecting it navigates to package settings for that row’s `templateId` (unchanged target)

### BDD-PQH-N22-011 — Users/Groups regression lock

**Given** an admin viewing Users or Groups with write access  
**When** the Actions column renders  
**Then** Edit and More still use `TableEditMoreActions`  
**And** existing More commands remain available (enable/disable, reset password, delete when permitted)

### BDD-PQH-N22-012 — Alignment contract (shared testid)

**Given** any in-scope catalog Actions cell (Asset Library, Legal Holds, API Invocations)  
**When** Actions render for an entitled row  
**Then** the root exposes `data-testid="table-edit-more-actions"`

### BDD-PQH-N22-013 — No Actions invented on EntityLink catalogs

**Given** Templates, Masters, or Content modules list catalogs  
**When** this leaf ships  
**Then** no new row Actions / Edit/More column is added  
**And** navigation remains EntityLink / row activate (unchanged)

### BDD-PQH-N22-014 — Deferred hub nested tables stay deferred

**Given** hub nested action tables (test datasets, bindings, version/preview history, etc.)  
**When** this leaf completes  
**Then** N22 for those surfaces remains explicitly deferred (not claimed Done)  
**And** SYS-NORM Waves 0–8 stay Done

---

## 9. Observable evidence

| Evidence | Expected |
| --- | --- |
| Unit / component tests | `TableEditMoreActions` optional Edit; catalog Actions mount with shared testid |
| FE gates | `pnpm -C frontend lint`, `type-check`, `test`, `build` green |
| E2E | Journey covers at least one in-scope catalog Actions (prefer Asset Library or Legal Holds + Invocations smoke) |
| UIUX | Edit/More spacing/hierarchy readable; Critical=0 |
| Deploy | Queued `docker-deploy-queue` Stage 5 and/or 10 when FE acceptance surface changes |
| API | No OpenAPI delta |

---

## 10. Traceability

| Item | Link |
| --- | --- |
| Task Master | **#162** |
| Program | [post-queue-hardening-program-2026-07.md](../plan/post-queue-hardening-program-2026-07.md) Leaf 3 |
| Origin | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) N22 |
| Charter | [system-normalization-program.md](./system-normalization-program.md) §2.9 / §2.10 N22 |
| Wave 1 primitive | [sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md) **BDD-SYS-NORM-W1-007/008** |
| Prior PQH FE leaf | [pqh-n19-n20-entitylink.md](./pqh-n19-n20-entitylink.md) (N22 was OOS there) |
| Primitive | `frontend/src/components/common/TableEditMoreActions.vue` |

---

## 11. Out of scope (explicit)

- Reopening SYS-NORM Waves 0–8 or flipping program Done
- Hub nested Actions tables (test datasets, bindings, version lines, preview downloads, etc.)
- API Policy Home alerts CTA column
- Task hub Open column
- Adding Actions columns to Template / Master / Content module catalogs
- New backend APIs, permission bits, or OpenAPI fields
- CE-O02 / **#119** / F7 (**#163**) / checklist **#3b**/**#5a** / marking **#53** Done

---

## 12. Handoff to plan-orchestrator

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: false
owning_doc: docs/behavior/pqh-n22-catalog-row-actions.md
task_ids: ["162"]
scenario_ids: [BDD-PQH-N22-001 … BDD-PQH-N22-014]
in_scope_catalogs: [asset-library, legal-holds, api-invocations]
regression_lock: [users, groups]
deferred: [hub-nested-action-tables, api-policy-home-alerts-cta, task-hub-open]
next: plan-orchestrator → frontend-engineer (TDD on TableEditMoreActions + three catalogs)
```
