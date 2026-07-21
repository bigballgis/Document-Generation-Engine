# Reminder timing settings IA (System / Team)

**Program / slice:** `reminder-timing-settings-ia` (post-SYS-NORM parked UX §4a; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#153** → **Done**  
**Active delivery slice:** `reminder-timing-settings-ia` (closed)  
**Placement:** was **ISOLATED** · worktree `D:/working/DGE-reminder-timing-settings-ia` · branch `feat/reminder-timing-settings-ia` — **REMOVED** after merge  
**Merge:** MAIN `d213834f` · feature `807d8213`  
**BDD:** [reminder-timing-settings-ia.md](../../behavior/reminder-timing-settings-ia.md) — **ready**/shipped (`BDD-RT-IA-001…016`); `frontend_ui_in_scope=true`; `backend_api_contract_change=false`  
**Upstream:** SYS-NORM Waves **0–8 Done** (program **Done**); §4a Reminder timing delivered this leaf  
**Batch recommendation:** **solo** (`member_task_ids: ["153"]`; `proposed_slice_id: reminder-timing-settings-ia`;
`shared_acceptance_surface: System settings Global default + Team settings group override + Dashboard Overview panel removal`;
`evidence_amortization: FE + E2E/UIUX + one docker queue`;
vetoes: parked-#2-asset-library-group-isolation, parked-#3-binding-editor-re-layout, parked-#4-auto-referenceKey, checklist-#3b/#5a, CE-O02, #53;
`on_red_split_hint: N/A solo`) — **closed**

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
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | Succeeded by **#154** `asset-library-group-isolation` (**In Progress**) — this leaf remains **Done** |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a Reminder timing → **Done**; Asset library → **In Progress** (#154); Binding editor / Auto `referenceKey` stay **Parked** |
| Succeeded by | **#154** Asset library group isolation — do **not** flip **#3b/#5a**; do **not** mark **#53** Done; do **not** activate Binding editor / Auto `referenceKey` / CE-O02 |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| RT-IA-T01 | Register TM **#153** + plan/ledger sole-active mirror | **Done** |
| RT-IA-T02 | Doc-keeper: terminology / permission / nav index sync (if needed) | **Done** |
| RT-IA-T03 | Frontend: System settings Reminder timing page (GLOBAL) | **Done** |
| RT-IA-T04 | Frontend: Team settings dialog (GROUP override) | **Done** |
| RT-IA-T05 | Frontend: remove CollaborationTimeoutConfigPanel from Dashboard Overview | **Done** |
| RT-IA-T06 | FE unit/component tests (TDD) | **Done** |
| RT-IA-T07 | E2E + UIUX (BDD-RT-IA-001…016) | **Done** |
| RT-IA-T08 | Queued docker deploy evidence + merge + MAIN doc-sync | **Done** |

---

## Exit criteria (from BDD-RT-IA-001…016)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Global Admin opens System settings Reminder timing full page | RT-IA-001 | **Done** |
| 2 | Global Admin edits/saves Global default only | RT-IA-002 | **Done** |
| 3 | Group Admin opens Team settings dialog | RT-IA-003 | **Done** |
| 4 | Group Admin edits/saves group override only | RT-IA-004 | **Done** |
| 5 | Dashboard Overview no longer hosts CollaborationTimeoutConfigPanel | RT-IA-005 | **Done** |
| 6 | Non-admin roles cannot reach edit surfaces (fail-closed) | RT-IA-006 | **Done** |
| 7 | Group Admin cannot use System settings Global page | RT-IA-007 | **Done** |
| 8 | Existing API semantics preserved | RT-IA-008 | **Done** |
| 9 | Save success messaging | RT-IA-009 | **Done** |
| 10 | Save and load error messaging | RT-IA-010 | **Done** |
| 11 | L1 copy Reminder timing / 催办时限设置 | RT-IA-011 | **Done** |
| 12 | Notifications-only capability unchanged (regression) | RT-IA-012 | **Done** |
| 13 | E2E acceptance relocates off Dashboard | RT-IA-013 | **Done** |
| 14 | System settings nav visibility (Global Admin only) | RT-IA-014 | **Done** |
| 15 | Team settings does not appear on Dashboard | RT-IA-015 | **Done** |
| 16 | Unauthorized API remains fail-closed | RT-IA-016 | **Done** |

---

## Gate evidence

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **N/A** (no Java changes) |
| Frontend lint / type-check / test / build | **GREEN** (test **1665**) |
| Stage 5 + 10 queued deploy | **DEPLOY_OK** (`docker-deploy-queue`; `:8080`/`:4173` **200**) |
| E2E functional | **12/12 PASS** (`reminder-timing-settings-ia` + regressions) |
| UIUX | **PASS_WITH_NOTES** Critical=0 (`reminder-timing-settings-ia-uiux-manifest.md`; 16 screenshots) |
| Architecture | **PASS_WITH_NOTES** Critical=0 **GO_WITH_NOTES** (journey CTA fixed before merge) |
| Stage 11 merge | **Done** — MAIN `d213834f` / feature `807d8213`; worktree **REMOVED** |

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
| [reminder-timing-settings-ia.md](../../behavior/reminder-timing-settings-ia.md) | BDD SoT (**ready**/shipped) |
| [business-terminology-guide.md](../../product/business-terminology-guide.md) | L1 Reminder timing / System settings / Team settings |
| [permission-matrix.md](../../security/permission-matrix.md) | `maintainCollaborationTimeoutConfig` |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | §4a parked queue — Reminder timing **Done** |
| [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md) | System/Team settings IA — FE **Done** |
