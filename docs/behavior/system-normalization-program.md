# BDD / Charter — System Normalization Program (2026-07-21)

| Field | Value |
| --- | --- |
| **文件状态** | Wave 0 charter **`ready`**（plan + ADR SoT）；Wave 1 BDD **ready/Done**；Wave 2 BDD **ready/Done**（[sys-norm-hub-ia.md](./sys-norm-hub-ia.md)；TM **#146** `5d77db80` / `992f6822`）；Wave 3 BDD **ready/Done**（[sys-norm-external-ops.md](./sys-norm-external-ops.md)；TM **#147** `18a9e3b2` / `f21dda5e`）；Wave 4 BDD **ready/Done** docs-close（[sys-norm-test-artifacts.md](./sys-norm-test-artifacts.md)；TM **#148**；product **#144** PTA）；Wave 5 BDD **ready/Done**（[sys-norm-roles.md](./sys-norm-roles.md)；**BDD-SYS-NORM-ROLE-001…018**；TM **#149** `febb95b3`）；Wave 6 BDD **ready/Done**（[sys-norm-d1-brands.md](./sys-norm-d1-brands.md)；**BDD-SYS-NORM-D1-001…020**；TM **#150** `64b0a650`）；Wave 7 BDD **ready/Done**（[sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md)；**BDD-SYS-NORM-PP-001…020**；TM **#151** `11356c63` / `f795b04a`）；Wave 8 实现 BDD = **ready/Done**（[sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md)；**BDD-SYS-NORM-W8-001…018**；TM **#152** `8aca145b` / `7df6c563`；**N18 deferred**；program Waves **0–8 Done**） |
| **BDD ID 前缀** | `BDD-SYS-NORM`（程序级）；波次实现前缀见 §8 |
| **编写日期** | 2026-07-21 |
| **程序 / 队列** | System Normalization Program · Wave **0** first leaf |
| **Slice** | `sys-norm-charter` |
| **Branch** | `feat/sys-norm-charter` |
| **Worktree** | `D:/working/DGE-sys-norm-charter` |
| **Placement** | ISOLATED |
| **Task / leaf** | Wave 0 TM **#143** `sys-norm-charter` → **Done**；Wave 1 TM **#145** `sys-norm-shell-fluid-nav` → **Done**（handoff briefly cited `#144` — `#144` remains PTA）；Wave 2 TM **#146** `sys-norm-hub-ia` → **Done** (`5d77db80` / `992f6822`); Wave 3 TM **#147** `sys-norm-external-ops` → **Done** (`18a9e3b2` / `f21dda5e`)；Wave 4 TM **#148** `sys-norm-test-artifacts` → **Done** docs-close（product **#144** `ac36ecbc` / `6bc74ff1`）；Wave 5 TM **#149** `sys-norm-roles` → **Done** (`febb95b3`)；Wave 6 TM **#150** `sys-norm-d1-brands` → **Done** (`64b0a650`)；Wave 7 TM **#151** `sys-norm-promotion-pack` → **Done**（`11356c63` / `f795b04a`）；Wave 8 TM **#152** `sys-norm-demo-seed-terms` → **Done**（`8aca145b` / `7df6c563`；sole-active **cleared**；program Waves **0–8 Done**；**N18 deferred**） |
| **Formal phase** | None invented — program tracked via plan doc (doc-keeper) + this charter |
| **Frontend UI** | Wave 0：**`frontend_ui_in_scope=false`**（docs/ADR only）。Wave 5：**`frontend_ui_in_scope=true`**（[sys-norm-roles.md](./sys-norm-roles.md)）。Wave 6：**`frontend_ui_in_scope=true`**（[sys-norm-d1-brands.md](./sys-norm-d1-brands.md)）。Wave 7：**`frontend_ui_in_scope=true`**（dry-run UI — [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md)）。Wave 8：**`frontend_ui_in_scope=true`**（seed/empty + L1 terms — [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md)）。Waves 1–4 UI 见各波次 BDD |
| **Owning docs** | **本文件（程序行为 / 决策接受 SoT）**；计划纲领 → [`docs/plan/system-normalization-program-2026-07.md`](../plan/system-normalization-program-2026-07.md)；角色压缩 → **[ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md)**（**Accepted**）；D1 退役 → **[ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)**（**Accepted**；**supersedes** ADR-0065 **product-surface**） |

```
bdd_readiness: ready
wave0_scope: plan+ADR SoT + program acceptance scenarios (no production code)
per_wave_impl_bdd: Wave 1 ready/Done; Wave 2 ready/Done ([sys-norm-hub-ia.md](./sys-norm-hub-ia.md)); Wave 3 ready/Done ([sys-norm-external-ops.md](./sys-norm-external-ops.md) BDD-SYS-NORM-W3-001…018; merge 18a9e3b2 / f21dda5e); Wave 4 ready/Done docs-close ([sys-norm-test-artifacts.md](./sys-norm-test-artifacts.md) BDD-SYS-NORM-W4-001…010 → BDD-PTA-001…009; TM #148; product evidence #144 ac36ecbc / 6bc74ff1); Wave 5 ready/Done ([sys-norm-roles.md](./sys-norm-roles.md) BDD-SYS-NORM-ROLE-001…018; TM #149 febb95b3); Wave 6 ready/Done ([sys-norm-d1-brands.md](./sys-norm-d1-brands.md) BDD-SYS-NORM-D1-001…020; TM #150 64b0a650); Wave 7 ready/Done ([sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) BDD-SYS-NORM-PP-001…020; TM #151 11356c63 / f795b04a); Wave 8 BDD ready + In Progress ([sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) BDD-SYS-NORM-W8-001…018; TM #152 sole-active; docs-first stage 3)
open_questions:
  - DOCUMENT_AUTHOR L1 display name finalization (EN/ZH) — ID locked; label finalizable (non-blocking; P-Q1 / Wave 8 capacity residual)
owning_doc: docs/behavior/system-normalization-program.md
task_ids: ["143", "145", "146", "147", "148", "149", "150", "151", "152"]
queue_slice_ids: ["sys-norm-shell-fluid-nav", "sys-norm-hub-ia", "sys-norm-external-ops", "sys-norm-test-artifacts", "sys-norm-roles", "sys-norm-d1-brands", "sys-norm-promotion-pack", "sys-norm-demo-seed-terms"]
sole_active: "152"
next_queue_head: sys-norm-demo-seed-terms
wave7_behavior_sot: docs/behavior/sys-norm-promotion-pack.md
wave8_behavior_sot: docs/behavior/sys-norm-demo-seed-terms.md
suggested_adrs:
  - 0070 — role compression (6 management roles) — Accepted path: authorization-security/0070-role-compression-six-roles.md
  - 0071 — D1 retire DocumentBrand/LegalEntity product surfaces — Accepted; supersedes ADR-0065 UX
scenario_count_wave0: 12
wave6_runtime_sot: docs/behavior/sys-norm-d1-brands.md
```

---

## 0. Purpose of this document

This is the **program-level behavior / decision-acceptance charter** for the System
Normalization Program confirmed by the user on **2026-07-21**.

| Layer | Wave 0 status | Notes |
| --- | --- | --- |
| Confirmed product decisions (§2) | **Locked** | Do not reopen in delivery |
| Program acceptance scenarios (§5) | **Ready** | Evidence = docs/ADR Accepted + plan wave table — **not** runtime E2E |
| Role compression acceptance (§6) | **Decision locked**; runtime BDD = **Wave 5 ready/Done** | ADR-0070 + [sys-norm-roles.md](./sys-norm-roles.md) (`febb95b3`) |
| D1 retirement acceptance (§7) | **Decision locked**; runtime BDD = **Wave 6 ready/Done** ([sys-norm-d1-brands.md](./sys-norm-d1-brands.md); TM **#150** `64b0a650`); FE nav Wave 1 **Done** | Point to ADR-0071; supersede ADR-0065 management UX |
| Per-wave runtime BDD (§8) | Wave 1 **ready/Done**; Wave 2 **ready/Done**; Wave 3 **ready/Done** ([sys-norm-external-ops.md](./sys-norm-external-ops.md); `18a9e3b2` / `f21dda5e`); Wave 4 **ready/Done** docs-close ([sys-norm-test-artifacts.md](./sys-norm-test-artifacts.md); TM **#148**; product **#144** PTA); Wave 5 **ready/Done** ([sys-norm-roles.md](./sys-norm-roles.md) **BDD-SYS-NORM-ROLE-001…018**; TM **#149** `febb95b3`); Wave 6 **ready/Done** ([sys-norm-d1-brands.md](./sys-norm-d1-brands.md) **BDD-SYS-NORM-D1-001…020**; TM **#150** `64b0a650`); Wave 7 **ready/Done** ([sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) **BDD-SYS-NORM-PP-001…020**；TM **#151** `11356c63` / `f795b04a`); Wave 8 BDD **ready** + wave **In Progress** ([sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) **BDD-SYS-NORM-W8-001…018**；TM **#152** sole-active；docs-first stage 3) | Wave 8 SoT + product/ops docs locked; BE/FE impl next |

**Wave 0 Done** = durable program SoT + ADR decision documents Accepted (or equivalent
decision-lock status) + indexes/plan activation — **no** `mvn` / `pnpm` / E2E / deploy
required for the charter leaf itself.

---

## 1. Actors, goal, trigger

### 1.1 Actors / roles

| Actor | Role in program |
| --- | --- |
| Delivery / governance stakeholders | Lock SoT (behavior, plan, ADR, matrix intent); serialize Waves 0→8 |
| Bank OA operators (authors, testers, group admins, legal, audit) | Consume normalized management UI / workflows after later waves |
| Parent agent / delivery-orchestrator | Enforce single-lane serial waves; docs-only Wave 0 |

### 1.2 User goal

Normalize the **whole management system** so layout, Template Package Hub IA, External
services, testing artifacts, navigation/governance (incl. D1), roles, UAT→PROD promotion,
and L1 terminology are consistent with the **confirmed** remedi decisions — and fold in
similar issues **N1–N23** without reopening locked forks.

### 1.3 Trigger

User confirmation of the System Normalization Program and decision set (**2026-07-21**),
starting Wave 0 `sys-norm-charter`.

### 1.4 Preconditions

- Host sole-active prior leaf cleared; Docker queue idle for later waves.
- Parked sibling worktrees (hub IA / test artifacts) **must not** absorb this leaf.
- ADR-0065 remains historically Accepted for IBL-E4 delivery; D1 **supersedes product
  surface** going forward (see §7) — not a silent rewrite of past impl evidence.

---

## 2. Confirmed decisions (LOCKED — do not reopen)

> Everything in this section is **Confirmed** product fact as of 2026-07-21.
> Implementation waves realize these facts; they do not re-decide them.

### 2.1 Layout

- **All management pages are fluid** — system-wide consistency (not hubs-only).
- Prior “catalog = fluid / detail = contained” default is **superseded for management
  AppPageLayout** by this program (constitution / UX docs update with Wave 0 plan + Wave 1).

### 2.2 Template Package Hub IA

- Hub primary surface = **Version lines** only (fluid).
- **Properties** control in hub header → **right drawer** (content formerly Overview tab).
- **Remove** hub secondary tabs: **Overview**, **Dependencies**, **External access**.
- **Dependencies** live on **per-version** surfaces (release / dev detail), not package hub tab.
- **API model A (not B):**
  - Package-level API settings remain SoT under **External services** (ADR-0040 / api-mgmt baseline).
  - Hub header provides **API settings** jump to package settings.
  - Per-version row shows **version perspective** (generate path, default-route indicator,
    callable/warning summary) + deep-link into package settings.
  - **Forbidden:** invent per-version ApiPolicy entities / break package-level policy SoT.

### 2.3 External services

- **Invocation records** = **separate page** (not only embedded hub panel).
- Dashboard-like overview: performance, failure rate, artifacts, and related ops signals.
- **Package API settings** = single edit surface (migrate off hub External access tab).

### 2.4 Testing artifacts

- On **published** (and **history**) Testing surfaces: operators can **download DOCX/PDF**
  from test runs.
- History must expose durable `previewId` / artifact keys needed for download.
- **Read-only ≠ no download** for authorized viewers of published/history testing evidence.

### 2.5 Nav / governance — D1

- Missing nav icons for document-brands / legal-entities are defects **until D1 removes** those
  surfaces (may still be fixed transiently if surfaces briefly remain).
- **Security & activity** grouping must not host brands/entities as if they were security.
- **D1 CONFIRMED — Retire Document brands + Legal entities product surfaces:**
  - Letterhead / logo / seal live in **Letterhead (master)** assets and workflows.
  - Understanding cost of separate brand/entity catalogs is too high for this internal bank app.
  - New ADR **supersedes / withdraws IBL-E4 management UX** (ADR-0065 product surface).
  - Runtime simplify in **Wave 6** (nav removal may start Wave 1 after ADR lock).
  - **Keep Legal holds.**
  - Shell **REDBC / GREENBC** remain **UI-only** chrome (orthogonal; not document brand MDM).

### 2.6 Roles (compression target = 6 management roles)

| Keep / target role | Action |
| --- | --- |
| `GLOBAL_ADMIN` | Keep |
| `GROUP_ADMIN` | **Absorb** `TEMPLATE_APPROVER` (approvers become group admins; privilege accepted) |
| `DOCUMENT_AUTHOR` | **Merge** `MASTER_DESIGNER` ∪ `TEMPLATE_AUTHOR` (stable ID preferred; **L1 name finalizable**) |
| `TEMPLATE_TESTER` | **Keep** (do not merge into author) |
| `LEGAL_REVIEWER` | Keep (ADR-0064 legal track unchanged by this merge) |
| `AUDIT_ADMIN` | Keep |

**Retire from assignable catalog:** `TEMPLATE_APPROVER`, `MASTER_DESIGNER`, `TEMPLATE_AUTHOR`
(after migration).

**Gate:** permission matrix + ADR-0070 + Wave 5 BDD **before** production code (Wave 5).

### 2.7 Export / UAT→PROD promotion pack (design facts)

- Promotion pack = dependency closure (assets binary, clause nesting graph as required).
- Master / letterhead inclusion follows **two-phase P2** — **no skip** of APPROVED master state.
- **No** brand/entity sidecar (aligned with D1).
- **No** secrets in pack.
- Import lands as **DRAFT** on PROD; must **re-test / re-approve / re-publish** on PROD.
- Management UI **dry-run** is in scope for Wave 7 — runtime SoT
  [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) (**BDD-SYS-NORM-PP-001…020**;
  P-Q4 resolved: Import dialog **Check dependencies** + gated **Import**).

### 2.8 Terminology (L1)

- L1 English: **Letterhead**; L1 Chinese: **母版**.
- Purge user-facing mixed “Master” wording on L1 primary surfaces.
- API / L3 may keep `masterId` and technical identifiers.

### 2.9 Other confirmed UX remedi

- Dev editor blank whiteboard: wrong surface + silent empty → fix redirects + honest empty states.
- **Asset library empty (Wave 8):** product default = **honest empty** when zero managed
  `library_asset`; optional demo/验收 managed-asset seed is **documented and off by default**
  in prod ([demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md)).
- **N23:** classpath `demo-images` rendering bypass ≠ Asset Library catalog items.
- **N13 / N15 / N21:** Legal hold empty catalog, master revision empty design summary, and
  role journey timeline silent empty → honest empty guidance (Wave 8).
- Users **Authorized groups** → EntityLink when permitted.
- Table **Actions** Edit/More system-wide alignment.
- Clause (and similar) **locale metadata de-duplication**.
- **N18** Legal hold actor EntityLink — **explicitly deferred** (not Wave 8 Done).
- **Parked UX (post-program queue, not Wave 8):** Reminder timing; Asset library group
  isolation; Binding editor re-layout; Auto `referenceKey` — plan §4a.

### 2.10 Similar-issue backlog (N1–N23) — in program scope

| ID | Theme | Summary |
| --- | --- | --- |
| N1 | EntityLink | Task hub `entityName` / `groupCode` not navigable |
| N2 | EntityLink | Catalog `groupCode` columns plain text |
| N3 | Actions | Users/Groups Edit+More baseline/gap alignment |
| N4 | Metadata dup | Template overview locale variants redundancy |
| N5 | Metadata dup | Release detail status / lifecycle double display |
| N6 | Dual surface | Legacy `#apiAccess` vs hub External access |
| N7–N9 | EntityLink / redundancy | Overview groupCode; hub header links; External ID column dup |
| N10 | Nav contract | Every nav item must map to icon (contract test) |
| N11 | Route key | legal-entities routeKey bug — **moot after D1** |
| N12 | Filter toolbar | Brand list ad-hoc filters — **moot after D1** |
| N13 | Seed | Legal hold empty catalog story |
| N14 | Hub IA parity | Master hub same IA debt as template hub |
| N15 | Empty design | Master revision empty design summary |
| N16–N17 | Terminology | EN Master mix; EN/ZH L1 split |
| N18–N20 | EntityLink | **N18 Legal hold actor — Deferred** (Wave 1 closeout 2026-07-21; not claimed Done); N19–N20 where-used groupCode / MasterImpact — later waves |
| N21 | Journey | Role journey timeline silent empty |
| N22 | Actions | Catalog row action pattern inconsistency |
| N23 | Docs/seed | `demo-images` bypass vs managed asset story |

Wave assignment of N* items is a **plan** concern (doc-keeper / plan-orchestrator); this
charter only locks that they are **in program backlog**.

---

## 3. Pending (NOT confirmed — do not promote)

| ID | Topic | Notes |
| --- | --- | --- |
| P-Q1 | `DOCUMENT_AUTHOR` L1 labels | EN/ZH display strings finalizable; **role ID direction locked** |
| P-Q2 | ADR-0071 exact registry number | **Resolved** — ADR-0071 Accepted at `template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md` |
| P-Q3 | Wave 1 nav hide vs Wave 6 hard delete | **Resolved** — Wave 1 nav hide Done; Wave 6 cutover locked in [sys-norm-d1-brands.md](./sys-norm-d1-brands.md) §5.2 (D1-C1…C13) |
| P-Q4 | Promotion dry-run UX detail | **Resolved** (2026-07-21) — [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) §5.1：Extend Templates **Import** dialog with **Check dependencies** (`dryRun=true`) + **Import** gated on `readyToCommit=true`; itemized dependency report; English-first; E2E in Wave 7 |
| P-Q5 | Per-capability remapping table cells | Wave 5 BDD + matrix rewrite — **merge direction locked** in §2.6 / §6 |
| P-Q6 | Whether parked hub/test worktrees are reused | Orchestrator: **do not fold**; prune/reuse is ops, not product fork |

**Explicitly not pending:** layout fluid, Hub IA model A, External services split, testing
downloads, D1 retire brands/entities, role merges, promotion pack design facts, Letterhead/母版 L1.

---

## 4. Primary journey (Wave 0 — governance)

1. Stakeholders confirm remedi decisions (done 2026-07-21).
2. Delivery opens Wave 0 leaf `sys-norm-charter` in isolated worktree.
3. Behavior charter (this file) locks Confirmed vs Pending and program acceptance scenarios.
4. Plan orchestrator / doc-keeper publish program plan + ADR-0070 + ADR-0071 (+ indexes).
5. Wave 0 merges to main; Waves 1–8 execute **serially** with per-wave BDD authored **at
   wave start** (stubs below are not implementation-ready).

### 4.1 System responses (Wave 0 success)

- Program behavior charter exists and separates Confirmed / Pending.
- Role compression and D1 retirement have **decision-acceptance** scenarios and ADR pointers.
- No production code change required for charter Done.
- Later waves cannot reopen §2 forks without explicit user override.

### 4.2 Boundary / fail-closed (program governance)

- Do not implement Wave 5 role matrix code before ADR-0070 Accepted + Wave 5 BDD `ready`.
- Do not claim ADR-0065 “never happened”; D1 **supersedes product direction** going forward.
- Do not invent per-version ApiPolicy (model B) during Hub IA work.
- Do not remove Legal holds under D1.
- Do not merge `TEMPLATE_TESTER` into author roles.
- Single-lane serial: at most one delivery leaf heavy work at a time.

### 4.3 Observable evidence (Wave 0)

| Evidence | What proves Wave 0 |
| --- | --- |
| This file on `feat/sys-norm-charter` | Program SoT + acceptance scenarios |
| Program plan doc (doc-keeper) | Wave table 0→8 + backlog N1–N23 |
| ADR-0070 / ADR-0071 Accepted (or decision-locked) | Role + D1 decisions durable |
| Index / ledger notes | Discoverable from `docs/README.md` |
| Absence of product code in Wave 0 commit set | Docs-only charter |

---

## 5. Acceptance scenarios — Wave 0 program charter

### BDD-SYS-NORM-W0-001 — Confirmed decisions persisted

**Given** the user confirmed the System Normalization Program on 2026-07-21  
**When** Wave 0 charter is published  
**Then** §2 lists layout fluid, Hub IA (Properties drawer; tabs removed; Dependencies
per-version; API model A), External services split, testing artifact downloads, D1
retirement, role compression, promotion pack design facts, L1 Letterhead/母版, and other UX
items as **Confirmed**  
**And** none of those forks are marked Pending.

### BDD-SYS-NORM-W0-002 — Pending separated

**Given** Wave 0 charter is published  
**When** a reader inspects §3  
**Then** only true gaps (e.g. DOCUMENT_AUTHOR L1 label finalization, ADR number alignment,
wave sequencing detail) appear as Pending  
**And** confirmed decisions are not re-listed as open product questions.

### BDD-SYS-NORM-W0-003 — Per-wave impl BDD not falsely ready

**Given** Wave 0 charter is the active SoT  
**When** an implementer starts Wave *n* (n≥1)  
**Then** they must author or promote that wave’s BDD to `ready` before TDD Red  
**And** must not treat §8 stubs as implementation-ready acceptance.

### BDD-SYS-NORM-W0-004 — Docs-only Wave 0

**Given** leaf `sys-norm-charter`  
**When** Wave 0 completes  
**Then** production runtime behavior is unchanged by the charter leaf itself  
**And** Done evidence is documentation / ADR decision lock, not E2E screenshots.

### BDD-SYS-NORM-W0-005 — Serial wave queue

**Given** Batch Recommendation **split** with waves 0→8  
**When** Wave 0 is In Progress or merging  
**Then** Waves 1–8 remain queued (no mega-merge of FE/BE domains into Wave 0)  
**And** parked sibling worktrees are not folded into this leaf.

### BDD-SYS-NORM-W0-006 — Traceability to ADR + plan

**Given** Wave 0 handoff to doc-keeper / plan-orchestrator  
**When** program docs are written  
**Then** they reference this behavior file  
**And** ADR-0070 (roles) and ADR-0071 (D1) (or registry-aligned numbers) carry the §6 / §7
acceptance scenarios  
**And** N1–N23 appear on the program backlog.

---

## 6. Role compression — ADR-0070 acceptance scenarios

**Status:** Decision **locked** (2026-07-21). **Implementation = Wave 5 Done** (TM **#149** · merge `febb95b3`).  
**ADR pointer:** [ADR-0070](../adr/authorization-security/0070-role-compression-six-roles.md) (**Accepted** 2026-07-21).  
**Runtime BDD SoT (ready/delivered):** [sys-norm-roles.md](./sys-norm-roles.md) — **BDD-SYS-NORM-ROLE-001…018**
(`frontend_ui_in_scope=true`). Charter §6.3 sketches remain decision-lock summaries; full
Given/When/Then + FE/JWT/matrix acceptance live in the Wave 5 file.  
**Supersedes (intent):** prior 8-role assignable catalog narrative in permission matrix /
onboarding materials — **matrix rewrite in Wave 5** (doc-keeper after this BDD `ready`), not
Wave 0 code.

### 6.1 Target state (locked)

Six assignable management roles: `GLOBAL_ADMIN`, `GROUP_ADMIN`, `DOCUMENT_AUTHOR`,
`TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN`.

### 6.2 Migration semantics (locked)

| From | To | Semantics |
| --- | --- | --- |
| `TEMPLATE_APPROVER` | `GROUP_ADMIN` | Approvers **become group admins** (privilege expansion **accepted**) |
| `MASTER_DESIGNER` and/or `TEMPLATE_AUTHOR` | `DOCUMENT_AUTHOR` | Union of letterhead + template (+ clause authoring per matrix) capabilities; **no** test decide / approval decide / master review admin / publish as pure author |
| Users with both designer + author | `DOCUMENT_AUTHOR` once | Idempotent migration |
| Users with approver + group admin | `GROUP_ADMIN` once | Idempotent |

**SoD retained:** `TEMPLATE_TESTER` remains the normal `decideTests` role; authors do not gain
test-pass by this merge. Self-approval / exception intervention rules remain fail-closed and
are re-expressed against `GROUP_ADMIN` as the normal compliance approver.

### 6.3 Acceptance scenarios (decision lock — Wave 0; impl Wave 5)

#### BDD-SYS-NORM-ROLE-001 — Approver → Group Admin

**Given** a user has assignable role `TEMPLATE_APPROVER` and not `GROUP_ADMIN`  
**When** Wave 5 migration runs (after ADR-0070 Accepted)  
**Then** the user has `GROUP_ADMIN`  
**And** `TEMPLATE_APPROVER` is no longer assignable  
**And** the user can perform former compliance approval decisions in scope  
**And** audit/migration evidence records the role remap.

#### BDD-SYS-NORM-ROLE-002 — Designer ∪ Author → DOCUMENT_AUTHOR

**Given** a user has `MASTER_DESIGNER` and/or `TEMPLATE_AUTHOR`  
**When** Wave 5 migration runs  
**Then** the user has `DOCUMENT_AUTHOR`  
**And** `MASTER_DESIGNER` / `TEMPLATE_AUTHOR` are no longer assignable  
**And** the user can perform letterhead + template authoring capabilities of the union  
**And** the user does **not** gain `decideTests` or normal compliance `decideApprovals` solely from this merge.

#### BDD-SYS-NORM-ROLE-003 — Tester retained

**Given** the post-migration role catalog  
**When** an administrator assigns testing-only duties  
**Then** `TEMPLATE_TESTER` remains a distinct assignable role  
**And** test pass/fail decision capability is not folded into `DOCUMENT_AUTHOR`.

#### BDD-SYS-NORM-ROLE-004 — Legal and Audit untouched by merge

**Given** users with only `LEGAL_REVIEWER` or only `AUDIT_ADMIN`  
**When** Wave 5 migration runs  
**Then** those role assignments remain  
**And** legal-track / audit capabilities are not removed by role compression.

#### BDD-SYS-NORM-ROLE-005 — Fail-closed unknown legacy role assignment API

**Given** migration completed and legacy role codes retired  
**When** a client attempts to assign `TEMPLATE_APPROVER` / `MASTER_DESIGNER` / `TEMPLATE_AUTHOR`  
**Then** the system reject is fail-closed (stable error)  
**And** does not silently ignore or map without audit.

*Runtime detail locked in Wave 5 BDD:* **422** `ROLE_NOT_ASSIGNABLE` — see
[BDD-SYS-NORM-ROLE-005](./sys-norm-roles.md#bdd-sys-norm-role-005--fail-closed-unknown--retired-legacy-role-on-assignment-api).

---

## 7. D1 retirement — ADR-0071 acceptance scenarios (supersede ADR-0065 product surface)

**Status:** Decision **locked** (2026-07-21). **Runtime BDD = Wave 6 ready/Done**
([sys-norm-d1-brands.md](./sys-norm-d1-brands.md) **BDD-SYS-NORM-D1-001…020**; TM **#150** `64b0a650`;
`frontend_ui_in_scope=true`; delivered MAIN `64b0a650`). FE nav hide **Wave 1 Done**.  
**ADR pointer:** [ADR-0071](../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) (**Accepted** 2026-07-21).  
**Relationship to ADR-0065:** ADR-0065 remains the historical Accepted decision for IBL-E4
delivery. D1 **supersedes / withdraws the product requirement** for DocumentBrand +
LegalEntity **management UX and ongoing dual-catalog product surface**. Letterhead/logo/seal
governance moves to **Letterhead (master)**. Legal holds **kept**. Shell REDBC/GREENBC
**UI-only** unchanged.

**Runtime SoT:** Full Given/When/Then + cutover locks (D1-C1…C13) live in
[sys-norm-d1-brands.md](./sys-norm-d1-brands.md). Charter §7.1 sketches remain decision-lock
summaries; implementers use the Wave 6 leaf file for TDD Red.

### 7.1 Acceptance scenarios (decision lock — Wave 0; runtime detail Wave 6)

#### BDD-SYS-NORM-D1-001 — Product surfaces retired

**Given** D1 ADR is Accepted and Wave 6 (and agreed FE nav wave) complete  
**When** an operator uses management navigation  
**Then** Document brands and Legal entities catalog routes are **absent**  
**And** operators manage letterhead/logo/seal via **Letterhead (master)** flows  
**And** Legal holds navigation remains available.

*Runtime detail:* [BDD-SYS-NORM-D1-001](./sys-norm-d1-brands.md#bdd-sys-norm-d1-001--product-surfaces-retired) (+ D1-006…008 hard retire).

#### BDD-SYS-NORM-D1-002 — Supersede ADR-0065 management UX requirement

**Given** ADR-0071 (or registry-aligned D1 ADR) is Accepted  
**When** future delivery cites document-brand product direction  
**Then** new work follows D1 (letterhead-in-master)  
**And** does not reintroduce DocumentBrand/LegalEntity management UX as a required surface  
**And** ADR-0065 is marked superseded/withdrawn **for product surface** (historical impl evidence retained).

#### BDD-SYS-NORM-D1-003 — Shell themes orthogonal

**Given** D1 retirement is in effect  
**When** an operator switches REDBC / GREENBC  
**Then** only management UI chrome changes  
**And** the switcher is not reintroduced as DocumentBrand MDM.

#### BDD-SYS-NORM-D1-004 — Runtime simplify fail-closed (Wave 6 runtime SoT)

**Given** Wave 6 runtime simplify BDD is **`ready`** ([sys-norm-d1-brands.md](./sys-norm-d1-brands.md))  
**When** generate/preview/test-generation paths no longer depend on LegalEntity→DocumentBrand catalogs  
**Then** resolution uses letterhead/master per Wave 6 locks (D1-C3…C5)  
**And** `context.legalEntityCode` is **non-driving** (whitelist-opaque; no retired catalog 422s)  
**And** management brand/entity APIs fail-closed **404/410** with stable retired-surface codes  
**And** implementable scenarios are **BDD-SYS-NORM-D1-004** + **D1-009…015** in the Wave 6 leaf.

#### BDD-SYS-NORM-D1-005 — Export packs omit brand/entity sidecar

**Given** promotion pack design (§2.7) and D1  
**When** a UAT→PROD pack is designed/exported (Wave 7)  
**Then** the pack does **not** require DocumentBrand/LegalEntity sidecar catalogs  
**And** letterhead/master dependency rules follow two-phase P2  
**And** Wave 6 already asserts no runtime/API **requires** those sidecars
([BDD-SYS-NORM-D1-005](./sys-norm-d1-brands.md#bdd-sys-norm-d1-005--export-packs-omit-brandentity-sidecar)).

---

## 8. Per-wave implementation BDD (stubs until authored at wave start)

> Author full Given/When/Then at wave start. Waves 1–8 BDD are authored; Wave 8 wave **In Progress**
> (TM **#152**; docs-first stage 3 locked; BE/FE impl next — not program Done).

| Wave | Slice id (suggested) | Stub readiness | Focus |
| --- | --- | --- | --- |
| 1 | `sys-norm-shell-fluid-nav` | **ready** / **Done** (`7a62be44`; TM **#145**) — [sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md) | Fluid all management pages; nav icons + contract test; Security = audit + legal holds; brands/entities nav hide (ADR-0071); Edit/More; EntityLink N1/N2/N3/users groups; **N18 deferred** |
| 2 | `sys-norm-hub-ia` | **ready/Done** (TM **#146**; [sys-norm-hub-ia.md](./sys-norm-hub-ia.md) **BDD-SYS-NORM-W2-001…018**; merge `5d77db80` / `992f6822`) | Template (+ Master parity) Hub Properties drawer; remove wrong tabs; version Dependencies; API jump model A; Dev blank-surface; locale de-dupe; legacy apiAccess |
| 3 | `sys-norm-external-ops` | **ready/Done** (TM **#147**; [sys-norm-external-ops.md](./sys-norm-external-ops.md) **BDD-SYS-NORM-W3-001…018**; merge `18a9e3b2` / `f21dda5e`) | External services dashboard; invocation records page; package API settings completion; nav; redirects |
| 4 | `sys-norm-test-artifacts` | **ready/Done** docs-close (TM **#148**; [sys-norm-test-artifacts.md](./sys-norm-test-artifacts.md) **BDD-SYS-NORM-W4-001…010** → **BDD-PTA-001…009**; product evidence TM **#144** `ac36ecbc` / `6bc74ff1`) | Published/history Testing durable DOCX/PDF downloads — §5.1 satisfied by PTA; **no** new product residual |
| 5 | `sys-norm-roles` | **ready/Done** ([sys-norm-roles.md](./sys-norm-roles.md) **BDD-SYS-NORM-ROLE-001…018**; TM **#149** `febb95b3`) | Implement §6 after ADR-0070; matrix rewrite (stage 3) + FE role labels + migration + JWT |
| 6 | `sys-norm-d1-brands` | **ready/Done** ([sys-norm-d1-brands.md](./sys-norm-d1-brands.md) **BDD-SYS-NORM-D1-001…020**; TM **#150** `64b0a650`) | Implement §7 runtime/management retirement per ADR-0071 |
| 7 | `sys-norm-promotion-pack` | **ready/Done** ([sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) **BDD-SYS-NORM-PP-001…020**；TM **#151** `11356c63` / `f795b04a`) | UAT→PROD pack + dry-run UI per §2.7；P-Q4 resolved |
| 8 | `sys-norm-demo-seed-terms` | **ready** + wave **In Progress** (TM **#152** sole-active; docs-first stage 3) — [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) **BDD-SYS-NORM-W8-001…018** | Asset honest empty + optional demo/验收 seed; L1 Letterhead/母版; N13/N15/N16–N17/N21/N23; **N18 deferred**; parked UX OOS |

**Forbidden:** marking wave **implementation** Done without green gates + doc-sync. Wave 8 BDD `ready` / docs-first ≠ Wave 8 program Done.

---

## 9. Traceability

| Source | Link / note |
| --- | --- |
| User confirmation | 2026-07-21 System Normalization Program + role/D1 follow-ups |
| This charter | `docs/behavior/system-normalization-program.md` |
| Program plan | `docs/plan/system-normalization-program-2026-07.md` (doc-keeper) |
| Role ADR | ADR-0070 (doc-keeper) |
| D1 ADR | ADR-0071 suggested (doc-keeper); supersedes ADR-0065 product surface |
| Historical brands | [ADR-0065](../adr/template-lifecycle/0065-legal-entity-document-brand-variants.md), [ibl-e4-entity-document-brands.md](./ibl-e4-entity-document-brands.md) |
| Permissions (six-role catalog — Wave 5 Done) | [permission-matrix.md](../security/permission-matrix.md) — rewrite + runtime catalog `febb95b3` |
| Terminology SSOT | [business-terminology-guide.md](../product/business-terminology-guide.md) |
| Hub nav baseline (to amend) | [catalog-navigation-ux.md](../product/catalog-navigation-ux.md) |
| Wave 2 Hub IA BDD | [sys-norm-hub-ia.md](./sys-norm-hub-ia.md) |
| Wave 3 External ops BDD | [sys-norm-external-ops.md](./sys-norm-external-ops.md) |
| Wave 4 Testing artifacts BDD | [sys-norm-test-artifacts.md](./sys-norm-test-artifacts.md) |
| Wave 5 Roles BDD | [sys-norm-roles.md](./sys-norm-roles.md) |
| Wave 6 D1 brands BDD | [sys-norm-d1-brands.md](./sys-norm-d1-brands.md) (**ready/Done**; **BDD-SYS-NORM-D1-001…020**; TM **#150** `64b0a650`) |
| Wave 7 promotion pack BDD | [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) (**ready**/Done; **BDD-SYS-NORM-PP-001…020**; TM **#151** Done `11356c63` / `f795b04a`；P-Q4 resolved) |
| Wave 8 demo seed / terms BDD | [sys-norm-demo-seed-terms.md](./sys-norm-demo-seed-terms.md) (**ready**/Done; **BDD-SYS-NORM-W8-001…018**; TM **#152** Done `8aca145b` / `7df6c563`; N18 deferred) |
| Wave 8 seed / N23 ops | [demo-acceptance-asset-seed.md](../operations/demo-acceptance-asset-seed.md) |
| Wave 8 terminology SSOT | [business-terminology-guide.md](../product/business-terminology-guide.md) § SYS-NORM / §4.5 |
| Batch recommendation | `split` / `sys-norm-charter` / docs-only amortization |

---

## 10. Handoff notes for doc-keeper / plan-orchestrator

1. Create program plan with wave table 0→8, Done criteria, N1–N23 backlog mirror.
2. Author **ADR-0070** Accepted (decision = §2.6 + §6); embed or link ROLE scenarios.
3. Author **ADR-0071** (or registry number) Accepted; mark ADR-0065 product-surface
   superseded/withdrawn; keep Legal holds; shell themes UI-only.
4. Index this behavior file in `docs/README.md`; activate ledger note for Wave 0.
5. Update terminology + catalog-navigation **intent** sections to Confirmed where §2 applies
   (implementation still later waves).
6. Do **not** flip go-live checklist **#3b** / **#5a** via this program charter.
