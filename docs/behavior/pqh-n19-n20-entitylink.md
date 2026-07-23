---
id: DOC-BEHAVIOR-PQH-N19-N20-ENTITYLINK
type: Behavior Spec
status: Confirmed
readiness: ready
program: Post-Queue Hardening (PQH Leaf 2)
slice: pqh-n19-n20-entitylink
taskMaster: "161"
related:
  - docs/plan/post-queue-hardening-program-2026-07.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/behavior/system-normalization-program.md
  - docs/behavior/sys-norm-shell-fluid-nav.md
  - docs/behavior/sys-norm-n18-role-l1.md
  - docs/architecture/ux-entity-display-constitution.md
  - .cursor/skills/frontend-entity-display/SKILL.md
---

# PQH N19–N20 — EntityLink where-used + MasterImpact

> **Slice:** `pqh-n19-n20-entitylink` · Batch Recommendation **`solo`**  
> **Member task:** TM **#161** (SYS-NORM residuals N19–N20 under PQH Leaf 2).  
> **Placement:** ISOLATED · worktree `D:/working/DGE-pqh-n19-n20-entitylink` ·
> branch `feat/pqh-n19-n20-entitylink`.  
> **Trace:** SYS-NORM §2.10 N18–N20 row — **N19 where-used groupCode** /
> **N20 MasterImpact**; Wave 1 EntityLink primitives; N18 pattern reference.  
> **Locks / vetoes:** do **not** reopen SYS-NORM Waves **0–8**; do **not** flip
> checklist **#3b** / **#5a**; do **not** mark **#53** Done; do **not** activate
> CE-O02 / **#119**; do **not** fold N22 into this leaf. Formal phase **None**.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: false
open_questions: []
owning_doc: docs/behavior/pqh-n19-n20-entitylink.md
task_ids: ["161"]
queue_slice_id: pqh-n19-n20-entitylink
member_task_ids: ["161"]
batch_decision: solo
shared_acceptance_surface: >
  ContentModuleWhereUsedPanel groupCode EntityLink + MasterImpactPanel
  EntityLinkCell / fail-closed template links
scenario_ids:
  - BDD-PQH-N19N20-001 … BDD-PQH-N19N20-014
scenario_count: 14
vetoes_applied:
  - n19n20-vs-n22
  - checklist-#3b/#5a-GO
  - CE-O02
  - mark-#53-CE-Done
  - activate-#119
  - do-not-reopen-SYS-NORM-Waves-0-8
on_red_split_hint: Peel N20 MasterImpact if where-used groupCode alone is green
```

---

## 0. Residual inventory (verified against worktree)

| Surface | Current state (pre-leaf) | Residual this leaf |
| --- | --- | --- |
| **N19** `ContentModuleWhereUsedPanel` template name | Already `EntityLinkCell` + `templateDetailLink(row.id)`; subtitle `externalId` | **Lock** with BDD (regression); **add** Group column `EntityLinkCell` + `groupCatalogLink` (charter: “where-used groupCode”) |
| **N19** where-used `groupCode` column | Raw `prop="groupCode"` plain text | **In scope** — align to Wave 1 N2 / catalog pattern |
| **N20** `MasterImpactPanel` referenced templates | Bare `<router-link :to="templateDetailPath(…)">` — **no** permission gate; **not** `EntityLinkCell` | **In scope** — `EntityLinkCell` + `templateDetailLink`; fail-closed plain text |
| IBL-E6 nesting columns (`referenceKind` / path) | FE display optional / not Done condition | **Out of scope** |
| N22 catalog Actions | Queued TM **#162** | **Out of scope** |

---

## 1. Actor / role

| Actor | Role / capability | Concern |
| --- | --- | --- |
| Content-module operator | Can open content-module detail **Where used** tab (existing CE-G05 / catalog browse entitlement) | Sees template + group references; navigates when permitted |
| Master designer / operator | Can open Master package hub impact panel | Sees referenced templates; navigates when permitted |
| Template catalog reader | `canAccessRoute(route.template-management)` | Receives template detail deep-links |
| Identity administrator | `canAccessRoute(route.identity-administration)` | Receives groups catalog deep-links from where-used `groupCode` |
| Unauthorized viewer | Lacks template-management and/or identity-administration | Sees **labels** but **no** navigable links (fail-closed) |

No new roles, capabilities, or authz bits.

---

## 2. User goal

1. **N19 — Where used:** On a content module’s Where used table, the operator sees Wave 1–consistent entity display: template name remains an `EntityLinkCell` (name + externalId subtitle), and **groupCode** becomes an `EntityLinkCell` to the groups catalog when identity administration is allowed — never a raw UUID dump for the primary template column, never an ungated permission leak.
2. **N20 — Master impact:** On the master impact “referenced templates” list, each template uses the shared `EntityLinkCell` pattern (friendly name label; optional externalId subtitle when present) with `templateDetailLink` gating — not a bare always-on `router-link`.

---

## 3. Trigger

| Surface | Trigger |
| --- | --- |
| Where used | Operator opens Content module detail → workspace tab **Where used** with ≥1 referencing template row |
| Master impact | Operator opens Master package hub (or equivalent surface hosting `MasterImpactPanel`) with impact analysis that lists ≥1 referenced template |

---

## 4. Preconditions

- SYS-NORM Waves **0–8 Done** (remain Done — do **not** reopen).
- Wave 1 EntityLink primitives shipped: `EntityLinkCell`, `useEntityLinkTargets`
  (`templateDetailLink`, `groupCatalogLink`), catalog `groupCode` EntityLink
  (**BDD-SYS-NORM-W1-011…013**).
- N18 Legal-hold actor EntityLink shipped as pattern reference
  ([sys-norm-n18-role-l1.md](./sys-norm-n18-role-l1.md)).
- CE-G05 where-used API returns template `id`, `name`, `externalId`, `groupCode`
  (and related columns already rendered).
- Master impact payload may include `referencedTemplates[]` (`templateId`, `name`,
  optional `externalId` / `lifecycleStatus`) and/or legacy `referencedTemplateIds[]`.
- PQH Leaf 1 (**#159**+**#160**) Done; this leaf is PQH Leaf 2 under TM **#161**.

---

## 5. Primary journey

### 5.1 N19 — Content module Where used

1. Operator opens Content module detail → **Where used**.
2. For each row:
   - **Template name cell:** `EntityLinkCell` with `label = row.name` (trimmed;
     if blank → em dash `—`), `subtitle = row.externalId` when present,
     `to = templateDetailLink(row.id)`.
   - **Group cell:** `EntityLinkCell` with `label = row.groupCode` (trimmed;
     blank → em dash), `to = groupCatalogLink(row.groupCode)` — same rules as
     Wave 1 catalogs (wildcard `*` never links; identity route required).
3. When `templateDetailLink` / `groupCatalogLink` returns `undefined`, the cell
   shows plain text (no `router-link`).
4. Activating a permitted template link navigates to that template’s detail.
5. Activating a permitted group link navigates to `/entitlement/groups` with
   `q` prefilling the group code (when code is non-empty and not `*`).

### 5.2 N20 — Master impact referenced templates

1. Operator opens Master hub impact panel with references present.
2. Each referenced template renders **`EntityLinkCell`** (not bare `<router-link>`).
3. **Label:** prefer trimmed `template.name`; if missing/blank and only an id is
   available, show the id string as the honest fallback label (legacy
   `referencedTemplateIds` path) — still via `EntityLinkCell`, never a raw
   ungated link.
4. **Subtitle (optional):** `externalId` when present on `MasterReferencedTemplate`.
5. **Link:** `to = templateDetailLink(template.templateId)` — omitted when
   template-management route is not accessible (fail-closed).
6. Activating a permitted link navigates to template detail for that id.
7. Empty reference list keeps the existing honest empty copy
   (`masters.impact.noReferencedTemplates`).

---

## 6. System responses (success)

| Path | Observable response |
| --- | --- |
| N19 template permitted | Name cell = `EntityLinkCell` link → template detail path |
| N19 template denied | Same name/externalId labels; plain text |
| N19 group permitted | Group cell = `EntityLinkCell` → `/entitlement/groups?q=<code>` |
| N19 group denied / `*` | Group label plain text; no link |
| N20 permitted | Referenced template = `EntityLinkCell` link → template detail |
| N20 denied | Same label; plain text (no always-on router-link) |
| N20 empty list | Honest empty state (unchanged semantics) |

---

## 7. Boundary / exception / fail-closed

| Case | Behavior |
| --- | --- |
| No `template-management` route | No template detail links on where-used or MasterImpact |
| No `identity-administration` route | No group catalog links on where-used `groupCode` |
| `groupCode` = `*` | Never linked (`groupCatalogLink` rule) |
| Blank name / blank groupCode | Em dash `—`; not a link |
| Legacy MasterImpact ids-only payload | Label may equal template id; still `EntityLinkCell` + gated `to` |
| Missing impact payload | Existing unavailable / empty honesty unchanged |
| API / OpenAPI | **No** required contract change; reuse existing fields |
| N22 Actions / nesting UI columns | Out of scope |
| SYS-NORM Waves 0–8 / checklist / CE | Unchanged; leaf must not reopen or flip |

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-PQH-N19N20-001 — Where-used template name uses EntityLinkCell

**Given** a content-module Where used row with `name` = `Loan Notice` and `id` = `tpl-1`  
**When** the Name column renders  
**Then** the cell uses `EntityLinkCell` (not bare interpolated name text)

### BDD-PQH-N19N20-002 — Where-used template label + externalId subtitle

**Given** the row has `name` = `Loan Notice` and `externalId` = `TPL-1`  
**When** the Name cell renders  
**Then** the visible label is `Loan Notice`  
**And** the subtitle shows `TPL-1`

### BDD-PQH-N19N20-003 — Where-used template link when template management permitted

**Given** the viewer can access `route.template-management`  
**And** a where-used row has non-empty template `id`  
**When** the Name cell renders  
**Then** `EntityLinkCell` receives `to` from `templateDetailLink(id)` targeting that template’s detail

### BDD-PQH-N19N20-004 — Where-used template plain text when template management denied

**Given** the viewer cannot access `route.template-management`  
**When** the Name cell renders  
**Then** the label (and subtitle if any) still show  
**And** the cell is not a navigable link

### BDD-PQH-N19N20-005 — Where-used groupCode uses EntityLinkCell (N19 residual)

**Given** a where-used row with `groupCode` = `RETAIL`  
**When** the Group column renders  
**Then** the cell uses `EntityLinkCell` with label `RETAIL`  
**And** it is not a raw plain `prop="groupCode"` text cell

### BDD-PQH-N19N20-006 — Where-used groupCode link when identity administration permitted

**Given** the viewer can access `route.identity-administration`  
**And** `groupCode` = `RETAIL`  
**When** the Group cell renders  
**Then** `to` targets `/entitlement/groups` with `q` prefilling `RETAIL`

### BDD-PQH-N19N20-007 — Where-used groupCode plain text when denied or wildcard

**Given** either the viewer cannot access `route.identity-administration`  
**Or** `groupCode` = `*`  
**When** the Group cell renders  
**Then** the label still shows  
**And** the cell is not a navigable link

### BDD-PQH-N19N20-008 — MasterImpact uses EntityLinkCell (N20)

**Given** master impact `referencedTemplates` contains `{ templateId: 'tpl-1', name: 'Loan Contract' }`  
**When** the referenced-templates list renders  
**Then** each item uses `EntityLinkCell`  
**And** there is no bare ungated `<router-link>` as the sole link primitive

### BDD-PQH-N19N20-009 — MasterImpact label prefers name; optional externalId subtitle

**Given** a referenced template with `name` = `Loan Contract` and `externalId` = `TPL-1`  
**When** the cell renders  
**Then** the visible label is `Loan Contract`  
**And** when `externalId` is present it appears as subtitle

### BDD-PQH-N19N20-010 — MasterImpact link gated on template management

**Given** the viewer can access `route.template-management`  
**When** a referenced template cell renders  
**Then** `to` comes from `templateDetailLink(templateId)`  
**Given** the viewer cannot access `route.template-management`  
**When** the same cell renders  
**Then** the label still shows and the cell is not a navigable link

### BDD-PQH-N19N20-011 — MasterImpact ids-only fallback remains EntityLink + gated

**Given** impact provides only `referencedTemplateIds: ['tpl-1']` (no enriched `referencedTemplates`)  
**When** the list renders  
**Then** the item uses `EntityLinkCell` with an honest label (id fallback acceptable)  
**And** `to` is present only when template management is permitted

### BDD-PQH-N19N20-012 — E2E where-used navigation (authorized)

**Given** an authorized session with template management + identity administration  
**And** a content module with ≥1 where-used row (name + groupCode)  
**When** the operator opens Where used and activates the template name link  
**Then** the app navigates to that template’s detail  
**And** the group cell is an EntityLink (activatable to groups catalog with search prefill)

### BDD-PQH-N19N20-013 — E2E MasterImpact navigation (authorized) + fail-closed spot-check

**Given** an authorized session with template management and a master with ≥1 referenced template  
**When** the operator opens Master impact and activates a referenced template EntityLink  
**Then** the app navigates to that template’s detail  
**And** a session without template management shows the same label as plain text (no navigable link)

### BDD-PQH-N19N20-014 — Vetoes + English-first i18n

**Given** this leaf’s change set  
**When** plan / checklist / CE registry / SYS-NORM wave statuses are inspected  
**Then** SYS-NORM Waves **0–8** remain Done (not reopened)  
**And** checklist **#3b** / **#5a** are unchanged  
**And** umbrella **#53** is not marked Done  
**And** CE-O02 / **#119** / N22 are not activated  
**And** any new user-facing copy has English keys first in `en.ts` (zh-CN mirrors as needed); no hardcoded Chinese-only chrome in changed Vue surfaces

---

## 9. Observable evidence

| Evidence | Proof |
| --- | --- |
| UI N19 | `ContentModuleWhereUsedPanel` Name + Group = `EntityLinkCell`; link vs plain by route mocks |
| UI N20 | `MasterImpactPanel` referenced list = `EntityLinkCell` + `templateDetailLink` |
| Unit | Vitest: groupCode EntityLink; MasterImpact gating; no bare always-on router-link |
| E2E / UIUX | Stages 6–7: Where used + Master impact journeys (functional + visual/a11y) |
| API | No OpenAPI change required |
| Docs | Behavior SoT = this file; plan/ledger sync at post-task stages |

---

## 10. Traceability

| Source | Link |
| --- | --- |
| PQH program Leaf 2 | [post-queue-hardening-program-2026-07.md](../plan/post-queue-hardening-program-2026-07.md) |
| SYS-NORM N19–N20 residual origin | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) |
| Charter N18–N20 backlog | [system-normalization-program.md](./system-normalization-program.md) §2.10 |
| Wave 1 EntityLink / groupCode | [sys-norm-shell-fluid-nav.md](./sys-norm-shell-fluid-nav.md) **BDD-SYS-NORM-W1-010…013** |
| N18 pattern | [sys-norm-n18-role-l1.md](./sys-norm-n18-role-l1.md) |
| Entity display constitution | [ux-entity-display-constitution.md](../architecture/ux-entity-display-constitution.md) |
| Skill | [.cursor/skills/frontend-entity-display/SKILL.md](../../.cursor/skills/frontend-entity-display/SKILL.md) |
| Task Master | **#161** |

---

## 11. Out of scope

- N22 catalog row Actions (TM **#162**)
- IBL-E6 nesting column UI (`referenceKind` / `nestingPathSummary`) as Done condition
- Backend where-used / impact API redesign
- Legal-hold / Users actor EntityLink (N18 Done)
- Reopening SYS-NORM Waves 0–8; checklist GO flips; CE-O02; **#119**; umbrella **#53** Done
- Claiming PQH program Done / IBL / CE / go-live Done

---

## 12. Stage Done definition (this BDD stage)

Stage 1 Done when:

1. This file is persisted under `docs/behavior/` with `bdd_readiness: ready`.
2. Acceptance scenarios **BDD-PQH-N19N20-001…014** are complete and `open_questions` is empty.
3. Residual scope is explicit: N19 groupCode EntityLink + N20 MasterImpact EntityLinkCell/gating; template name EntityLink locked.
4. `frontend_ui_in_scope=true` recorded for E2E/UIUX stages.
5. Indexed from `docs/README.md`.
6. Handoff to **plan-orchestrator** (stage 2) to activate TM **#161** detail plan — then frontend-engineer TDD Red.
