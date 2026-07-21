---
id: DOC-BEHAVIOR-REMINDER-TIMING-SETTINGS-IA
type: Behavior Spec
status: Confirmed
readiness: ready
program: post-SYS-NORM parked UX (§4a)
slice: reminder-timing-settings-ia
taskMaster: 153
related:
  - docs/plan/system-normalization-program-2026-07.md
  - docs/product/business-terminology-guide.md
  - docs/security/permission-matrix.md
  - docs/api/openapi-v1.yaml
---

# Reminder timing settings IA (System / Team)

> **TM:** Task Master **#153** · slice `reminder-timing-settings-ia`  
> **User confirmation (2026-07-21):** relocate Reminder timing off Dashboard Overview into
> proper IA — Global Admin **System settings** full page (bank-wide global default only);
> Group Admin **Team settings** on Groups/team surface → dialog (group override only).  
> **Capability unchanged:** overdue reminders are **notifications only** — they do **not**
> change workflow status.  
> **API unchanged:** `GET`/`PUT /api/management/v1/collaboration-timeout-config`.  
> **Out of scope:** parked §4a siblings (asset library group isolation, binding editor
> re-layout, auto `referenceKey`); CE-O02; checklist **#3b** / **#5a**; CE umbrella **#53**.

```
bdd_readiness: ready
frontend_ui_in_scope: true
backend_api_contract_change: false
open_questions: []
owning_doc: docs/behavior/reminder-timing-settings-ia.md
task_ids: ["153"]
queue_slice_id: reminder-timing-settings-ia
scenario_ids:
  - BDD-RT-IA-001 … BDD-RT-IA-016
scenario_count: 16
```

---

## 1. Actor / role

| Actor | Role / capability |
| --- | --- |
| Global Admin | `GLOBAL_ADMIN` with `maintainCollaborationTimeoutConfig` — edit **Global default** on System settings full page only |
| Group Admin | `GROUP_ADMIN` with `maintainCollaborationTimeoutConfig` — edit **Group override** via Team settings dialog on Groups/team surface only |
| Other management roles | `DOCUMENT_AUTHOR`, `TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN`, etc. — **no** Reminder timing edit surfaces (fail-closed / hidden) |

Authorization remains fail-closed per [permission-matrix.md](../security/permission-matrix.md)
(`maintainCollaborationTimeoutConfig` → GLOBAL, GROUP) and existing
`GroupAccessService.canMaintainCollaborationTimeoutConfig`.

---

## 2. User goal

Admins configure **when overdue reminders appear** for testing, approval, go-live
confirmation, and fix tasks from **settings-appropriate IA** — not from Dashboard Overview.

- Bank-wide operators set the **Global default** on a dedicated System settings page.
- Team operators set a **Group override** from the Groups/team surface via a dialog.
- Dashboard Overview remains a journey/summary surface without configuration chrome.

---

## 3. Trigger

| Actor | Trigger |
| --- | --- |
| Global Admin | Sidebar **System settings** (or **System settings → Reminder timing**) → full page |
| Group Admin | Groups/team surface **Team settings** control → opens dialog/modal |
| Any entitled admin (regression) | Direct URL to System settings Reminder timing route (authorization gated) |
| Misplaced legacy | Dashboard Overview no longer hosts `CollaborationTimeoutConfigPanel` |

---

## 4. Preconditions

- Actor authenticated with management JWT.
- Existing collaboration timeout config API and persistence (P14-T02b) available.
- Escalation / overdue-reminder behavior remains **notification-only** (P14-T02c) — no
  workflow status mutation from thresholds.
- L1 copy SSOT: [business-terminology-guide.md](../product/business-terminology-guide.md)
  — **Reminder timing** / **催办时限设置**.
- SYS-NORM Waves 0–8 Done; this leaf is post-program parked queue §4a head (Batch **solo**).

---

## 5. Primary journeys

### 5.1 Global Admin — System settings (full page)

1. Global Admin opens management shell → sees nav item **System settings**
   (en) / **系统设置** (zh-CN) when entitled.
2. Activates System settings (or System settings → Reminder timing) → lands on a
   **dedicated full page** (fluid AppPageLayout).
3. Page shows Reminder timing editor locked to **Global default** (`scopeType=GLOBAL`) —
   no GLOBAL/GROUP scope radio, no group-code field for override editing.
4. Edits threshold hours (TEST / APPROVAL / PENDING_RELEASE / REMEDIATION) → Save.
5. On success: success message; persisted via existing PUT. On failure: error message;
   prior values remain until a successful save.

**Canonical route (locked for TDD):** `/system/settings/reminder-timing`  
**Nav visibility:** only sessions with `GLOBAL_ADMIN` **and**
`maintainCollaborationTimeoutConfig=true`.  
**Optional shell:** If implementer introduces a System settings shell with a single
Reminder timing section, the shell MUST still present as a full page (not a dialog) and
MUST NOT add other product modules in this leaf.

### 5.2 Group Admin — Team settings (dialog)

1. Group Admin opens Groups/team surface (`/entitlement/groups` or equivalent team
   surface already in Entitlement nav).
2. Activates **Team settings** (en) / **团队设置** (zh-CN) control on that surface
   (page header / top action rail — not Dashboard).
3. Dialog/modal opens with Reminder timing editor locked to **Group override**
   (`scopeType=GROUP`) for the actor’s authorized group context (no GLOBAL scope,
   no free-form cross-group targeting beyond authorized scope).
4. Edits thresholds → Save → success/error messaging (same L1 keys as today).
5. Dismiss closes dialog; Groups list remains underneath.

### 5.3 Remove from Dashboard Overview

1. Any entitled admin opens Dashboard Overview.
2. `CollaborationTimeoutConfigPanel` is **absent** (not hidden by CSS; not mounted).
3. Collaboration to-dos / overdue follow-up queues on Dashboard **remain** (orthogonal).

---

## 6. System responses

### 6.1 Success

| Surface | Response |
| --- | --- |
| System settings page | Full-page Reminder timing for GLOBAL; Save → `Reminder timing saved.` / `催办时限已保存。` |
| Team settings dialog | Modal Reminder timing for GROUP override; same success copy |
| Load | Existing GET loads current thresholds; description copy states notifications-only |
| API | Unchanged envelope / `CollaborationTimeoutConfigView` / validation bounds (1–8760 hours) |

### 6.2 Boundary / exception / fail-closed

| Case | Response |
| --- | --- |
| Role without `maintainCollaborationTimeoutConfig` | System settings nav **hidden**; Team settings control **hidden**; deep-link to System settings route → Forbidden / existing route-guard fail-closed (no edit UI) |
| GROUP_ADMIN deep-links System settings Reminder timing route | Fail-closed Forbidden (or redirect away) — Global page is GLOBAL_ADMIN only |
| GLOBAL_ADMIN attempts GROUP override on System settings UI | **Not offered** — UI is Global default only (API may still accept authorized GROUP upserts per OpenAPI; this leaf does not invent a new GLOBAL-on-Groups Team settings requirement) |
| GROUP_ADMIN saves GLOBAL via UI | **Not offered** — dialog is Group override only |
| Cross-group GROUP upsert | Existing API 403 fail-closed |
| Unauthenticated | 401 on API; login gate on UI |
| Save/load failure | Existing error L1: `Unable to load/save reminder timing.` / zh-CN equivalents |
| Workflow status | Threshold changes **never** mutate source work-item status |

### 6.3 Explicit non-goals (this leaf)

- No new backend endpoints; no OpenAPI breaking change.
- No change to escalation scheduler semantics (notification-only).
- No Asset library / Binding editor / Auto `referenceKey` work.
- Do **not** mark TM **#153** Done from BDD alone; do **not** flip **#3b** / **#5a**;
  do **not** mark CE **#53** Done.

---

## 7. L1 copy (confirmed)

| Surface | EN | ZH-CN | Notes |
| --- | --- | --- | --- |
| Panel / page title | Reminder timing | 催办时限设置 | Existing `collaboration.timeoutConfig.title` |
| Description | Set when overdue reminders appear… notifications only — they do not change workflow status. | (existing zh-CN aligned) | Capability copy must remain |
| Save / success / errors | Reminder timing saved. / Unable to load\|save… | 催办时限已保存。 / 无法加载\|保存… | Existing keys |
| Nav — System settings | System settings | 系统设置 | New L1 for Global Admin entry |
| Control — Team settings | Team settings | 团队设置 | New L1 for Group Admin entry |
| Scope labels (when shown) | Global default / Group override | (existing) | Global page does not show scope switcher; dialog fixed to Group override |

Terminology SSOT cross-update:
[business-terminology-guide.md](../product/business-terminology-guide.md) §4.3 / admin IA labels.

---

## 8. Acceptance scenarios

### BDD-RT-IA-001 — Global Admin opens System settings Reminder timing full page

**Given** a `GLOBAL_ADMIN` session with `maintainCollaborationTimeoutConfig=true`  
**When** the actor activates sidebar **System settings** (or **System settings → Reminder timing**)  
**Then** the app navigates to the dedicated full page at `/system/settings/reminder-timing`  
**And** the page heading / primary title uses L1 **Reminder timing** / **催办时限设置**  
**And** the surface is a full page (not a dialog hosted on Dashboard).

### BDD-RT-IA-002 — Global Admin edits and saves Global default only

**Given** the Global Admin is on the System settings Reminder timing page  
**When** the actor changes one or more threshold hours and saves  
**Then** the client issues `PUT /api/management/v1/collaboration-timeout-config` with
`scopeType=GLOBAL` and `groupCode=null`  
**And** a success message **Reminder timing saved.** / **催办时限已保存。** is shown  
**And** a subsequent `GET` without `groupCode` returns the saved Global thresholds  
**And** the UI does **not** offer a GLOBAL/GROUP scope radio or group-code override editor.

### BDD-RT-IA-003 — Group Admin opens Team settings dialog

**Given** a `GROUP_ADMIN` session with `maintainCollaborationTimeoutConfig=true`  
**When** the actor opens the Groups/team surface and activates **Team settings**  
**Then** a dialog/modal opens containing the Reminder timing editor  
**And** the editor is scoped to **Group override** for the actor’s authorized group  
**And** the actor is **not** offered Global default editing in that dialog.

### BDD-RT-IA-004 — Group Admin edits and saves group override only

**Given** the Group Admin Team settings dialog is open  
**When** the actor changes threshold hours and saves  
**Then** the client issues `PUT` with `scopeType=GROUP` and the authorized `groupCode`  
**And** success messaging uses the same Reminder timing L1 success copy  
**And** `GET` with that `groupCode` reflects the saved override (or documented fallback
semantics unchanged).

### BDD-RT-IA-005 — Dashboard Overview no longer hosts CollaborationTimeoutConfigPanel

**Given** any entitled admin who previously saw Reminder timing on Dashboard Overview  
**When** the actor opens Dashboard Overview  
**Then** `CollaborationTimeoutConfigPanel` is not rendered  
**And** no Reminder timing heading / `.timeout-config-card` appears on Overview  
**And** collaboration task queues on Dashboard remain available when entitled.

### BDD-RT-IA-006 — Non-admin roles cannot reach edit surfaces (fail-closed)

**Given** a session **without** `maintainCollaborationTimeoutConfig` (e.g. `DOCUMENT_AUTHOR`,
`TEMPLATE_TESTER`, `LEGAL_REVIEWER`, `AUDIT_ADMIN`)  
**When** the shell builds navigation and the Groups surface renders  
**Then** **System settings** nav is hidden  
**And** **Team settings** is hidden  
**And** navigating directly to `/system/settings/reminder-timing` yields Forbidden /
route-guard fail-closed without an editable Reminder timing form.

### BDD-RT-IA-007 — Group Admin cannot use System settings Global page

**Given** a `GROUP_ADMIN` with `maintainCollaborationTimeoutConfig=true`  
**When** the actor attempts to open `/system/settings/reminder-timing`  
**Then** access is denied (hidden nav + fail-closed route guard)  
**And** the actor still reaches Group override via **Team settings** dialog.

### BDD-RT-IA-008 — Existing API semantics preserved

**Given** the management API contract for collaboration timeout config  
**When** this leaf ships  
**Then** `GET`/`PUT /api/management/v1/collaboration-timeout-config` paths, request/response
schemas, scope rules, hour bounds (1–8760), and error categories remain compatible  
**And** no new required fields or breaking enum changes are introduced  
**And** escalation remains notification-only (no workflow status mutation).

### BDD-RT-IA-009 — Save success messaging

**Given** an entitled admin on the correct IA surface (System settings page or Team settings dialog)  
**When** Save succeeds  
**Then** the UI shows **Reminder timing saved.** (en) / **催办时限已保存。** (zh-CN).

### BDD-RT-IA-010 — Save and load error messaging

**Given** an entitled admin on the correct IA surface  
**When** load or save fails (network / 4xx / 5xx)  
**Then** the UI shows the existing L1 errors **Unable to load reminder timing.** /
**Unable to save reminder timing.** (and zh-CN equivalents)  
**And** the UI does not claim success.

### BDD-RT-IA-011 — L1 copy Reminder timing / 催办时限设置

**Given** either Reminder timing surface (page or dialog)  
**When** the UI renders title and description  
**Then** primary title is **Reminder timing** / **催办时限设置**  
**And** description states that reminders are notifications only and do not change
workflow status  
**And** nav/control labels use **System settings** / **系统设置** and **Team settings** /
**团队设置** as specified in §7.

### BDD-RT-IA-012 — Notifications-only capability unchanged (regression)

**Given** saved Reminder timing thresholds  
**When** the escalation/overdue reminder path fires for an open work item  
**Then** an overdue reminder / escalation notification work item may appear  
**And** the source work item’s workflow status is unchanged by the threshold/reminder alone.

### BDD-RT-IA-013 — E2E acceptance relocates off Dashboard

**Given** Playwright coverage that previously asserted Reminder timing on `/dashboard`  
(`collaboration-todos.spec.ts` admin configures reminder timing; related global-admin
journey assertions)  
**When** this leaf updates E2E  
**Then** Global Admin save acceptance runs against System settings Reminder timing page  
**And** Group Admin save acceptance (if present) runs via Team settings dialog  
**And** Dashboard Overview assertions confirm the panel is **absent**.

### BDD-RT-IA-014 — System settings nav visibility (Global Admin only)

**Given** a `GLOBAL_ADMIN` with the capability and a `GROUP_ADMIN` with the capability  
**When** each session builds sidebar nav  
**Then** only the Global Admin session shows **System settings**  
**And** Group Admin does not see **System settings** but does see **Team settings** on
the Groups/team surface.

### BDD-RT-IA-015 — Team settings does not appear on Dashboard

**Given** a Group Admin on Dashboard  
**When** Overview / Tasks surfaces render  
**Then** there is no Team settings entry that re-hosts Reminder timing on Dashboard  
**And** Team settings remains on the Groups/team surface only.

### BDD-RT-IA-016 — Unauthorized API remains fail-closed

**Given** a caller without `maintainCollaborationTimeoutConfig` (or outside group scope)  
**When** the caller invokes `GET`/`PUT /api/management/v1/collaboration-timeout-config`  
**Then** the API returns **401/403** per existing contract  
**And** no configuration is mutated.

---

## 9. Observable evidence

| Evidence | Proves |
| --- | --- |
| Nav + route + full page DOM | BDD-RT-IA-001 / 014 |
| PUT body `scopeType=GLOBAL` + success toast + GET | BDD-RT-IA-002 / 009 |
| Groups surface → dialog + PUT `scopeType=GROUP` | BDD-RT-IA-003 / 004 |
| Dashboard Overview absence of `.timeout-config-card` / panel | BDD-RT-IA-005 / 015 |
| Hidden controls + Forbidden deep-link | BDD-RT-IA-006 / 007 |
| OpenAPI / contract tests unchanged green | BDD-RT-IA-008 / 016 |
| Playwright relocated specs | BDD-RT-IA-013 |
| Escalation status regression (existing E2E) | BDD-RT-IA-012 |

---

## 10. Traceability

| Source | Link |
| --- | --- |
| User IA confirmation | 2026-07-21 — System settings full page (Global); Team settings dialog (Group); remove from Dashboard |
| Task Master | **#153** · `reminder-timing-settings-ia` |
| Program parked queue | [system-normalization-program-2026-07.md](../plan/system-normalization-program-2026-07.md) §4a Reminder timing |
| Terminology | [business-terminology-guide.md](../product/business-terminology-guide.md) §4.3 |
| Permission | [permission-matrix.md](../security/permission-matrix.md) `maintainCollaborationTimeoutConfig` |
| API | [openapi-v1.yaml](../api/openapi-v1.yaml) `getCollaborationTimeoutConfig` / `upsertCollaborationTimeoutConfig` |
| Prior UI placement (to remove) | `frontend/src/components/dashboard/DashboardOverviewTab.vue` → `CollaborationTimeoutConfigPanel` |
| Prior E2E (to relocate) | `frontend/e2e/collaboration-todos.spec.ts`; `frontend/e2e/P21-T10-global-admin-journey.spec.ts` |

---

## 11. Implementation notes (non-normative for product; guidance for plan/FE)

- Reuse `CollaborationTimeoutConfigPanel` with props/modes (`globalPage` vs `groupDialog`)
  rather than duplicating form logic — **optional**; behavior above is normative.
- Remove `showTimeoutConfig` mounting from Dashboard Overview / related dashboard loaders
  once surfaces move.
- Register nav item + route key + icon (nav icon contract from SYS-NORM Wave 1).
- i18n: add `nav` keys for System settings / Team settings; keep
  `collaboration.timeoutConfig.*` values.

---

## 12. Stage 1 done definition

Stage 1 (behavior-spec-author) is **done** when:

1. This document exists under the feature worktree with `bdd_readiness: ready`.
2. Acceptance scenarios **BDD-RT-IA-001…016** are listed and traceable to TM **#153**.
3. Cross-links exist from `docs/README.md` behavior index (and terminology / §4a pointer
   as needed) without marking the delivery leaf **Done**.
4. `open_questions` is empty (or only non-blocking notes) — **no blockers** for
   `plan-orchestrator`.
5. **No** Vue/Java implementation, **no** TM **#153** Done, **no** merge in this stage.
