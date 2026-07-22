---
id: DOC-BEHAVIOR-SYS-NORM-ROLES
type: Behavior Spec
status: Confirmed
readiness: ready
program: SYS-NORM
wave: 5
slice: sys-norm-roles
taskMaster: "149"
related:
  - docs/adr/authorization-security/0070-role-compression-six-roles.md
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/security/permission-matrix.md
  - docs/product/business-terminology-guide.md
  - docs/adr/template-lifecycle/0064-legal-compliance-approval-matrix.md
---

# SYS-NORM Wave 5 — Six-role compression (ADR-0070)

> **TM:** Task Master **#149** (**Done**) · slice `sys-norm-roles` ·
> MAIN merge `febb95b3` · worktree **REMOVED**.  
> **Locks:** [ADR-0070 Accepted](../adr/authorization-security/0070-role-compression-six-roles.md);
> charter [system-normalization-program.md](./system-normalization-program.md) §2.6 / §6;
> plan [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md)
> Wave 5 → **Done**.  
> **Gate order (completed):** permission-matrix **rewrite** (doc-keeper stage 3) + this BDD
> **`ready`** before production role-catalog / Flyway / FE enum code — all met.  
> **Formal phase:** **None**.  
> **Do not:** reopen ADR-0070 merges; merge `TEMPLATE_TESTER` into author; flip **#3b** /
> **#5a**; mark **#53** Done; fold closed-wave scope into this leaf.  
> **P-Q1 supersession (2026-07-22):** `DOCUMENT_AUTHOR` L1 EN **Document author** / ZH
> **文档作者** are **Confirmed** in [sys-norm-n18-role-l1.md](./sys-norm-n18-role-l1.md)
> **BDD-N18-L1-008…010** (ROLE-013 interim allowance was Wave 5 only).

```
bdd_readiness: ready
frontend_ui_in_scope: true
open_questions: []
pending_non_blocking: []
p_q1_behavior_sot: docs/behavior/sys-norm-n18-role-l1.md
owning_doc: docs/behavior/sys-norm-roles.md
task_ids: ["149"]
queue_slice_id: sys-norm-roles
scenario_ids:
  - BDD-SYS-NORM-ROLE-001 … BDD-SYS-NORM-ROLE-018
scenario_count: 18
batch_recommendation:
  decision: solo
  member_task_ids: ["149"]
  proposed_slice_id: sys-norm-roles
```

---

## 1. Actor / role

| Actor | Role / scope |
| --- | --- |
| Platform operator (`GLOBAL_ADMIN`) | Runs migration evidence review; assigns any of the six roles; rewrites seeds |
| Group administrator (`GROUP_ADMIN`, post-migration includes former approvers) | Assigns **ops** roles in authorized groups; performs compliance / single-track approvals; SEAL upload / content-module review decide within admin matrix |
| Document author (`DOCUMENT_AUTHOR`) | Union of former `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` letterhead + template (+ clause authoring per matrix); **no** `decideTests` / normal `decideApprovals` solely from merge |
| Template tester (`TEMPLATE_TESTER`) | Retained SoD holder for normal `decideTests` |
| Legal reviewer (`LEGAL_REVIEWER`) | Unchanged by compression (ADR-0064 legal track) |
| Audit administrator (`AUDIT_ADMIN`) | Unchanged by compression |
| Unauthorized / cross-group / legacy-role client | Fail-closed — no silent remap, no privilege leak |

Group isolation remains fail-closed (`GroupAccessService` / rewritten matrix).

---

## 2. User goal

1. Persist **exactly six** assignable management roles after Wave 5 migration.  
2. Remap durable user-role rows + JWT/session capabilities + FE role pickers / journey labels
   (Wave 5 allowed interim L1 for `DOCUMENT_AUTHOR`; P-Q1 now Confirmed via BDD-N18-L1-008…010).  
3. Retire `TEMPLATE_APPROVER`, `MASTER_DESIGNER`, `TEMPLATE_AUTHOR` from the assignable
   catalog with fail-closed assignment API behavior.  
4. Keep SoD: authors do **not** gain `decideTests`; tester remains distinct.  
5. Unblock TDD Red for catalog / migration / FE (matrix rewrite stage 3 **landed**).

---

## 3. Trigger

- Wave 5 Flyway (or equivalent) user-role migration runs on deploy.  
- Administrator opens Users identity admin → assign / edit roles.  
- Management login issues JWT / session capabilities for remapped users.  
- FE role dropdowns, onboarding tour maps, and collaboration queue role filters load.  
- Client POSTs legacy role codes on user create/update assignment API.  
- Post-migration user exercises former designer / author / approver journeys.

---

## 4. Preconditions

- ADR-0070 **Accepted**; charter §2.6 / §6 decision lock unchanged.  
- Waves **0–4** Done (program registry).  
- Wave 5 BDD this file = **`ready`** (stage 1).  
- Permission-matrix six-role rewrite **landed** (doc-keeper stage 3) —
  [permission-matrix.md](../security/permission-matrix.md) is the Confirmed docs SoT
  **before** production role-catalog / Flyway / FE enum code merges.  
- Actor authenticated with management JWT where UI/API scenarios apply.  
- Formal phase **None**; go-live checklist **#3b** / **#5a** untouched; **#53** not Done.

---

## 5. Primary journey

1. Doc-keeper rewrites [permission-matrix.md](../security/permission-matrix.md) to six-role
   catalog + capability cells per §6 of this spec (and ADR-0070).  
2. Implementers add failing tests for ROLE-001…018 (TDD Red).  
3. Flyway migrates durable assignments per §6.1 mapping; audit/migration evidence recorded.  
4. Backend `ManagementRole` (or SSOT enum), OpenAPI enums, capability projector, and
   assignment validators accept **only** the six roles.  
5. FE `MANAGEMENT_ROLE_VALUES` / i18n / role journey maps expose six roles; former approver
   journeys route through `GROUP_ADMIN`; designer+author journeys through `DOCUMENT_AUTHOR`.  
6. Operators assign `DOCUMENT_AUTHOR` / `TEMPLATE_TESTER` / `LEGAL_REVIEWER` (ops set) within
   group scope; legacy codes rejected fail-closed.  
7. E2E covers remapped seeds + picker + at least one SoD negative (author cannot decideTests).

### 5.1 System responses (success)

| Surface | Response |
| --- | --- |
| Assignable catalog | Exactly: `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR`, `TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN` |
| Migration | Durable remap per §6.1; idempotent; audit/migration evidence |
| JWT / capabilities | Claims + capability bits match rewritten matrix for the six roles |
| FE pickers / journeys | Only six assignable roles; no retired codes as selectable options |
| Assignment API | Accepts six roles under existing escalation rules; rejects retired/unknown fail-closed |
| Approvals (compliance / single-track) | Normal decider path = `GROUP_ADMIN` (+ `GLOBAL_ADMIN`); no assignable `TEMPLATE_APPROVER` |
| Legal track | `LEGAL_REVIEWER` + `decideLegalApprovals` unchanged (ADR-0064) |

### 5.2 Capability remap (Confirmed — mirrored in permission-matrix §13.2)

> Cell-level tables are rewritten by doc-keeper; this section locks **merge direction** for
> implementers (charter P-Q5). Do not invent privileges beyond the union / absorption rules.

| Capability (illustrative SSOT keys) | Post-Wave 5 role set |
| --- | --- |
| `manageMasters` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR` |
| `reviewMasters` | `GLOBAL_ADMIN`, `GROUP_ADMIN` |
| `authorTemplates` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR` |
| `authorContentModules` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR` |
| `exportTemplates` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR` |
| `decideTests` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `TEMPLATE_TESTER` — **not** `DOCUMENT_AUTHOR` |
| `decideApprovals` | `GLOBAL_ADMIN`, `GROUP_ADMIN` — former `TEMPLATE_APPROVER` absorbed |
| `decideLegalApprovals` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `LEGAL_REVIEWER` |
| `decideContentModuleReviews` | `GLOBAL_ADMIN`, `GROUP_ADMIN` |
| `publishTemplates` | `GLOBAL_ADMIN`, `GROUP_ADMIN` |
| `viewCollaborationWorkItems` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR`, `TEMPLATE_TESTER`, `LEGAL_REVIEWER` |
| `manageAssetLibrary` (list/route) | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR`, `TEMPLATE_TESTER` (tester ACTIVE-only list rule retained) |
| SEAL upload (service-layer) | `GLOBAL_ADMIN`, `GROUP_ADMIN` (absorbs former approver SEAL privilege) |
| IMAGE/OTHER upload | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR` |
| `readAudit` | `GLOBAL_ADMIN`, `GROUP_ADMIN`, `AUDIT_ADMIN` |
| Exception intervention (CE-G01) | `GLOBAL_ADMIN`, `GROUP_ADMIN` only |

**`GROUP_ADMIN` may assign (ops roles, authorized groups):** `DOCUMENT_AUTHOR`,
`TEMPLATE_TESTER`, `LEGAL_REVIEWER`.  
**Still forbidden for `GROUP_ADMIN`:** `GLOBAL_ADMIN`, `AUDIT_ADMIN`, `GROUP_ADMIN` →
**403** `ROLE_ASSIGNMENT_NOT_ALLOWED`.

---

## 6. Acceptance scenarios

### 6.1 Migration & catalog (charter ROLE-001…005 expanded)

#### BDD-SYS-NORM-ROLE-001 — Approver → Group Admin

**Given** a durable user has assignable role `TEMPLATE_APPROVER` and not `GROUP_ADMIN`  
**And** ADR-0070 is Accepted and Wave 5 migration is eligible to run  
**When** Wave 5 role migration runs  
**Then** the user has `GROUP_ADMIN` and does **not** retain `TEMPLATE_APPROVER` as an
assignable role code  
**And** `TEMPLATE_APPROVER` is removed from the assignable management catalog  
**And** the user can perform former compliance / single-track approval decisions in
authorized group scope (via `decideApprovals` as `GROUP_ADMIN`)  
**And** migration/audit evidence records the remap (`TEMPLATE_APPROVER` → `GROUP_ADMIN`)

#### BDD-SYS-NORM-ROLE-002 — Designer ∪ Author → DOCUMENT_AUTHOR

**Given** a durable user has `MASTER_DESIGNER` and/or `TEMPLATE_AUTHOR`  
**When** Wave 5 role migration runs  
**Then** the user has `DOCUMENT_AUTHOR` exactly once (idempotent if both were present)  
**And** `MASTER_DESIGNER` / `TEMPLATE_AUTHOR` are no longer assignable  
**And** the user can perform the **union** of letterhead/master authoring and template
(+ clause authoring per rewritten matrix) capabilities of the two source roles  
**And** the user does **not** gain `decideTests` solely from this merge  
**And** the user does **not** gain normal compliance / single-track `decideApprovals`
solely from this merge  
**And** the user does **not** gain `reviewMasters` / `publishTemplates` / exception
intervention solely from this merge

#### BDD-SYS-NORM-ROLE-003 — Tester retained (SoD)

**Given** the post-migration six-role catalog  
**When** an administrator assigns testing-only duties  
**Then** `TEMPLATE_TESTER` remains a distinct assignable role  
**And** normal test pass/fail capability (`decideTests`) is held by
`TEMPLATE_TESTER` (+ admins) and is **not** granted to pure `DOCUMENT_AUTHOR`  
**And** a pure `DOCUMENT_AUTHOR` session calling test-decide APIs is fail-closed (**403**)

#### BDD-SYS-NORM-ROLE-004 — Legal and Audit untouched by merge

**Given** users with only `LEGAL_REVIEWER` or only `AUDIT_ADMIN`  
**When** Wave 5 role migration runs  
**Then** those role assignments remain unchanged  
**And** `decideLegalApprovals` / legal-track stage rules (ADR-0064) remain  
**And** audit read/export capabilities for `AUDIT_ADMIN` are not removed by compression

#### BDD-SYS-NORM-ROLE-005 — Fail-closed unknown / retired legacy role on assignment API

**Given** migration completed and legacy role codes are retired from the assignable catalog  
**When** a client attempts to assign `TEMPLATE_APPROVER`, `MASTER_DESIGNER`, or
`TEMPLATE_AUTHOR` (create or update user roles)  
**Then** the API rejects **fail-closed** with stable error **422** `ROLE_NOT_ASSIGNABLE`
(`VALIDATION`; `retryable=false`)  
**And** the system does **not** silently ignore the code, does **not** auto-map without an
audited migration path, and does **not** persist the retired code  
**And** the same fail-closed applies to any other unknown management role token  
**And** this is distinct from **403** `ROLE_ASSIGNMENT_NOT_ALLOWED` (ops admin escalation)

### 6.2 Idempotency & dual-role users

#### BDD-SYS-NORM-ROLE-006 — Approver already Group Admin → single GROUP_ADMIN

**Given** a user already has both `TEMPLATE_APPROVER` and `GROUP_ADMIN`  
**When** Wave 5 migration runs  
**Then** the user has `GROUP_ADMIN` once  
**And** no duplicate role rows / claims for retired `TEMPLATE_APPROVER` remain

#### BDD-SYS-NORM-ROLE-007 — Designer + Author → single DOCUMENT_AUTHOR

**Given** a user has both `MASTER_DESIGNER` and `TEMPLATE_AUTHOR`  
**When** Wave 5 migration runs  
**Then** the user has `DOCUMENT_AUTHOR` once  
**And** neither legacy code remains assignable or persisted as active role

#### BDD-SYS-NORM-ROLE-008 — Migration re-run idempotent

**Given** Wave 5 migration has already completed successfully  
**When** the migration job/script is executed again  
**Then** role assignments remain stable (no duplicate `DOCUMENT_AUTHOR` / `GROUP_ADMIN`)  
**And** no user is escalated beyond the locked remap table

### 6.3 Matrix + JWT / session capabilities

#### BDD-SYS-NORM-ROLE-009 — Permission matrix six-role rewrite before production code merge

**Given** this BDD is `ready` and ADR-0070 is Accepted  
**When** Wave 5 leaves the docs/BDD gate toward implementation merge  
**Then** [permission-matrix.md](../security/permission-matrix.md) §3 role catalog and
§13 capability tables are rewritten to the six roles and §5.2 remap  
**And** the matrix no longer lists `TEMPLATE_APPROVER` / `MASTER_DESIGNER` /
`TEMPLATE_AUTHOR` as assignable management roles  
**And** production catalog/migration code does **not** merge ahead of that rewrite

#### BDD-SYS-NORM-ROLE-010 — JWT / session capabilities for DOCUMENT_AUTHOR

**Given** a migrated user whose only management role is `DOCUMENT_AUTHOR`  
**When** the user logs in (or refreshes session capabilities)  
**Then** JWT/role claims include `DOCUMENT_AUTHOR` and do **not** include retired codes  
**And** capabilities include the authoring union (`authorTemplates`, `manageMasters` /
letterhead authoring bits, `authorContentModules` as matrix)  
**And** capabilities do **not** include `decideTests` or `decideApprovals`

#### BDD-SYS-NORM-ROLE-011 — JWT / session capabilities for remapped Group Admin (ex-approver)

**Given** a user remapped from `TEMPLATE_APPROVER` to `GROUP_ADMIN`  
**When** the user logs in  
**Then** claims include `GROUP_ADMIN`  
**And** capabilities include `decideApprovals` and other `GROUP_ADMIN` bits per rewritten
matrix (privilege expansion **accepted**)  
**And** claims do **not** retain `TEMPLATE_APPROVER`

### 6.4 Frontend (UI in scope)

#### BDD-SYS-NORM-ROLE-012 — Role assignment picker exposes six roles only

**Given** an authorized admin opens Users → assign roles  
**When** the role picker renders  
**Then** selectable management roles are exactly the six target IDs  
**And** `TEMPLATE_APPROVER` / `MASTER_DESIGNER` / `TEMPLATE_AUTHOR` are absent as options  
**And** submitting a tampered legacy code via API still fails per ROLE-005

#### BDD-SYS-NORM-ROLE-013 — DOCUMENT_AUTHOR interim L1 labels (Pending-finalizable)

> **Historical Wave 5 acceptance only.** P-Q1 L1 strings are **Confirmed** 2026-07-22 —
> normative SoT [sys-norm-n18-role-l1.md](./sys-norm-n18-role-l1.md) **BDD-N18-L1-008…010**
> (EN **Document author** / ZH **文档作者**; no interim suffix).

**Given** `DOCUMENT_AUTHOR` role ID is locked and L1 EN/ZH strings are still **Pending**
(P-Q1 / terminology guide) — *Wave 5 delivery context*  
**When** FE renders role chips / pickers / journey copy for `DOCUMENT_AUTHOR`  
**Then** the UI may use an **interim** English-first i18n string (or visible role ID) that
clearly identifies the merged author role  
**And** implementers do **not** invent a “final” Confirmed L1 brand string in this wave  
**And** later residual leaf may replace interim copy without changing the role ID  
**And** Pending labels do **not** block Wave 5 `ready` or implementation

#### BDD-SYS-NORM-ROLE-014 — Journey / onboarding maps retire eight-role entries

**Given** post-migration FE role-journey / onboarding configuration  
**When** a `GROUP_ADMIN` (ex-approver) or `DOCUMENT_AUTHOR` session loads the shell  
**Then** approval queue / authoring journey entries resolve for the six-role catalog  
**And** no primary nav or tour step requires selecting a retired role code  
**And** tester journey remains keyed to `TEMPLATE_TESTER` / `decideTests`

#### BDD-SYS-NORM-ROLE-015 — Approver UI surfaces usable by GROUP_ADMIN

**Given** a template awaiting compliance / single-track approval in an authorized group  
**When** a remapped `GROUP_ADMIN` (former `TEMPLATE_APPROVER`) opens the approval decision UI  
**Then** the user can complete approve/reject with existing evidence rules  
**And** pure `DOCUMENT_AUTHOR` and pure `TEMPLATE_TESTER` still cannot perform that
compliance decide (fail-closed)

### 6.5 Seeds, OpenAPI, regression boundaries

#### BDD-SYS-NORM-ROLE-016 — Demo / test seeds cover six roles

**Given** management seed / test seeder data after Wave 5  
**When** acceptance or local bootstrap runs  
**Then** each of the six roles has at least one seed principal (or documented intentional
absence with honest empty — prefer one seed each for E2E)  
**And** no seed assigns retired role codes as active catalog roles

#### BDD-SYS-NORM-ROLE-017 — OpenAPI / contract enums match six-role catalog

**Given** management OpenAPI / contract-outline role enums for assignment and filters  
**When** Wave 5 contract updates land  
**Then** enums list the six roles only for assignable management roles  
**And** `ROLE_NOT_ASSIGNABLE` is documented for retired/unknown assignment attempts  
**And** `ROLE_ASSIGNMENT_NOT_ALLOWED` remains the escalation code for forbidden admin assigns

#### BDD-SYS-NORM-ROLE-018 — Governance boundaries (non-goals)

**Given** Wave 5 delivery scope  
**When** implementers or reviewers assess Done  
**Then** the leaf does **not** flip go-live checklist **#3b** / **#5a**  
**And** does **not** mark CE umbrella **#53** Done  
**And** does **not** claim SYS-NORM program Done (Waves 6–8 remain)  
**And** does **not** implement Wave 6 D1 hard-delete, Wave 7 promotion pack, or Wave 8
final L1 terminology sweep beyond interim `DOCUMENT_AUTHOR` copy  
**And** formal phase remains **None**

---

## 7. Boundary / exception behavior

| Case | Behavior |
| --- | --- |
| Retired role on assignment API | **422** `ROLE_NOT_ASSIGNABLE` — no silent map |
| Unknown role token | Same fail-closed as retired |
| `GROUP_ADMIN` assigns `GROUP_ADMIN` / `GLOBAL_ADMIN` / `AUDIT_ADMIN` | **403** `ROLE_ASSIGNMENT_NOT_ALLOWED` |
| Pure `DOCUMENT_AUTHOR` decides tests | **403** (existing test-entry / decideTests denial) |
| Pure `DOCUMENT_AUTHOR` compliance approve | **403** |
| Pure `LEGAL_REVIEWER` compliance approve | **403** (ADR-0064 unchanged) |
| Cross-group admin actions | Existing `GROUP_SCOPE_OUT_OF_RANGE` / not-found fail-closed |
| Self-approval / exception intervention | Still admin-only; former pure approver **gains** exception rights only because they become `GROUP_ADMIN` (accepted privilege expansion) |
| JWT still carrying retired role after incomplete migrate | Treat as authorization defect — migration must complete before traffic; runtime must not honor retired codes as assignable |

---

## 8. Observable evidence

| Evidence | What proves it |
| --- | --- |
| DB / Flyway | User-role rows remapped; retired codes absent from active assignments |
| Migration/audit log | Remap events for approver→admin and designer/author→`DOCUMENT_AUTHOR` |
| API | Assignment accept/reject codes; capability payload on session |
| JWT claims | Six-role IDs only for management business roles |
| FE | Picker options; journey maps; E2E screenshots / Playwright assertions |
| Matrix doc | Six-role tables committed in same Wave 5 change set (stage 3) |
| Gates | Backend `mvn verify`; FE lint/type-check/test/build; E2E + UIUX; queued deploy |

---

## 9. Traceability

| Source | Link / note |
| --- | --- |
| Decision ADR | [ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md) **Accepted** |
| Charter decisions | [system-normalization-program.md](./system-normalization-program.md) §2.6 / §6 |
| Program wave | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) Wave 5 |
| Matrix (rewrite = stage 3) | [permission-matrix.md](../security/permission-matrix.md) |
| Terminology (L1; P-Q1 Confirmed via N18-L1) | [business-terminology-guide.md](../product/business-terminology-guide.md); [sys-norm-n18-role-l1.md](./sys-norm-n18-role-l1.md) |
| Legal track unchanged | [ADR-0064](../adr/template-lifecycle/0064-legal-compliance-approval-matrix.md) |
| Slice / branch | `sys-norm-roles` / MAIN `febb95b3` (feature worktree **REMOVED**) |
| Task Master | **#149** (**Done**) |

---

## 10. Out of scope

- Final Confirmed EN/ZH L1 marketing strings for `DOCUMENT_AUTHOR` — **moved** to residual
  leaf `sys-norm-n18-role-l1` / **BDD-N18-L1-008…010** (no longer Pending for governance).  
- Wave 6 Document brands / Legal entities runtime retirement.  
- Wave 7 UAT→PROD promotion pack UI.  
- Reopening eight-role catalog or merging tester into author.  
- Flipping **#3b** / **#5a** or marking **#53** / SYS-NORM program Done.

---

## 11. Handoff

```
task_ids: ["149"]
status: Done
bdd_readiness: ready
frontend_ui_in_scope: true
merge_sha: febb95b3
placement: MAIN (worktree REMOVED)
batch_recommendation:
  decision: solo
  member_task_ids: ["149"]
  proposed_slice_id: sys-norm-roles
  closed: true
implementers: complete (BE + FE + E2E + deploy + merge)
gates: BE 2357/0/0; FE lint/type-check/test(1639)/build GREEN; E2E 7/7; UIUX PASS; Stage 5+10 DEPLOY_OK; arch Critical=0 merge_go; CQ allow merge
next: queue head sys-norm-d1-brands (Wave 6) — Not Started / not activated; sole-active cleared
```
