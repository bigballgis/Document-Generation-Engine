# Reminder timing settings IA (System / Team)

**Program / slice:** `reminder-timing-settings-ia` (post-SYS-NORM parked UX §4a; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#153** → **In Progress** (sole-active delivery leaf)  
**Active delivery slice:** `reminder-timing-settings-ia`  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-reminder-timing-settings-ia` · branch `feat/reminder-timing-settings-ia`  
**BDD:** [reminder-timing-settings-ia.md](../../behavior/reminder-timing-settings-ia.md) — **ready** (`BDD-RT-IA-001…016`); `frontend_ui_in_scope=true`; `backend_api_contract_change=false`  
**Upstream:** SYS-NORM Waves **0–8 Done** (program **Done**); parked §4a activation via Batch **solo**  
**Batch recommendation:** **solo** (`member_task_ids: ["153"]`; `proposed_slice_id: reminder-timing-settings-ia`;
`shared_acceptance_surface: System settings Global default + Team settings group override + Dashboard Overview panel removal`;
`evidence_amortization: FE + E2E/UIUX + one docker queue`;
vetoes: parked-#2-asset-library-group-isolation, parked-#3-binding-editor-re-layout, parked-#4-auto-referenceKey, checklist-#3b/#5a, CE-O02, #53;
`on_red_split_hint: N/A solo`) — **open**

---

## Purpose

Relocate Reminder timing off Dashboard Overview into settings-appropriate IA:

- **GLOBAL_ADMIN** — System settings full page for **Global default** only
- **GROUP_ADMIN** — Team settings dialog on Groups/team surface for **Group override** only
- Remove `CollaborationTimeoutConfigPanel` from Dashboard Overview
- Reuse existing `GET`/`PUT /api/management/v1/collaboration-timeout-config` (no API contract change)
- Capability unchanged: overdue reminders are **notifications only**

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#153** only (`reminder-timing-settings-ia`) |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a Reminder timing activated; siblings stay Parked |
| Next after this leaf | Remain Parked until Batch Recommendation: Asset library group isolation; Binding editor re-layout; Auto `referenceKey` — do **not** flip **#3b/#5a**; do **not** mark **#53** Done; do **not** activate CE-O02 |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| RT-IA-T01 | Register TM **#153** + plan/ledger sole-active mirror | **Done** (plan-orchestrator stage 2) |
| RT-IA-T02 | Doc-keeper: terminology / permission / nav index sync (if needed) | **Done** (stage 3 docs-first — product/nav/PRD/requirements/domain/permission + P14/P21 IA supersession; leaf **not** Done) |
| RT-IA-T03 | Frontend: System settings Reminder timing page (GLOBAL) | **Not Started** |
| RT-IA-T04 | Frontend: Team settings dialog (GROUP override) | **Not Started** |
| RT-IA-T05 | Frontend: remove CollaborationTimeoutConfigPanel from Dashboard Overview | **Not Started** |
| RT-IA-T06 | FE unit/component tests (TDD) | **Not Started** |
| RT-IA-T07 | E2E + UIUX (BDD-RT-IA-001…016) | **Not Started** |
| RT-IA-T08 | Queued docker deploy evidence + merge + MAIN doc-sync | **Not Started** |

---

## Exit criteria (from BDD-RT-IA-001…016)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Global Admin opens System settings Reminder timing full page | RT-IA-001 | **Not Started** |
| 2 | Global Admin edits/saves Global default only | RT-IA-002 | **Not Started** |
| 3 | Group Admin opens Team settings dialog | RT-IA-003 | **Not Started** |
| 4 | Group Admin edits/saves group override only | RT-IA-004 | **Not Started** |
| 5 | Dashboard Overview no longer hosts CollaborationTimeoutConfigPanel | RT-IA-005 | **Not Started** |
| 6 | Non-admin roles cannot reach edit surfaces (fail-closed) | RT-IA-006 | **Not Started** |
| 7 | Group Admin cannot use System settings Global page | RT-IA-007 | **Not Started** |
| 8 | Existing API semantics preserved | RT-IA-008 | **Not Started** |
| 9 | Save success messaging | RT-IA-009 | **Not Started** |
| 10 | Save and load error messaging | RT-IA-010 | **Not Started** |
| 11 | L1 copy Reminder timing / 催办时限设置 | RT-IA-011 | **Not Started** |
| 12 | Notifications-only capability unchanged (regression) | RT-IA-012 | **Not Started** |
| 13 | E2E acceptance relocates off Dashboard | RT-IA-013 | **Not Started** |
| 14 | System settings nav visibility (Global Admin only) | RT-IA-014 | **Not Started** |
| 15 | Team settings does not appear on Dashboard | RT-IA-015 | **Not Started** |
| 16 | Unauthorized API remains fail-closed | RT-IA-016 | **Not Started** |

---

## Gate evidence

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **Not Started** (API reuse — expect green / no contract change) |
| Frontend lint / type-check / test / build | **Not Started** |
| Stage 5 + 10 queued deploy | **Not Started** |
| E2E / UIUX | **Not Started** |
| Architecture / code quality | **Not Started** |
| Stage 11 merge | **Not Started** |

---

## Out of scope this leaf

- Parked §4a siblings: Asset library group isolation; Binding editor re-layout; Auto `referenceKey`  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Activating CE-O02 / RTL  
- Backend API contract changes to `/collaboration-timeout-config`  
- Changing notifications-only reminder semantics  

## Related docs

| Doc | Role |
| --- | --- |
| [reminder-timing-settings-ia.md](../../behavior/reminder-timing-settings-ia.md) | BDD SoT (**ready**) |
| [business-terminology-guide.md](../../product/business-terminology-guide.md) | L1 Reminder timing / System settings / Team settings |
| [permission-matrix.md](../../security/permission-matrix.md) | `maintainCollaborationTimeoutConfig` |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | §4a parked queue — Reminder timing **In Progress** |
