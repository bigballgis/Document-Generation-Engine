# N18 Legal-hold EntityLink + DOCUMENT_AUTHOR L1

**Program / slice:** `sys-norm-n18-role-l1` (post-SYS-NORM residual; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#157** (N18 Legal-hold Created-by EntityLink) + **#158** (`DOCUMENT_AUTHOR` L1 Confirmed) → **In Progress** (merge leaf)  
**Active delivery slice:** `sys-norm-n18-role-l1` (**sole-active**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-sys-norm-n18-role-l1` · branch `feat/sys-norm-n18-role-l1` · base `c4af526d`  
**BDD:** [sys-norm-n18-role-l1.md](../../behavior/sys-norm-n18-role-l1.md) — **ready** (`BDD-N18-L1-001…012`); `frontend_ui_in_scope=true`; `backend_api_contract_change=optional-additive`  
**Upstream:** SYS-NORM Waves **0–8 Done**; §4a parked UX (Reminder / Asset library / Binding editor + Auto `referenceKey`) **Done**; Wave 1 EntityLink primitives + CE-G04 Legal hold list shipped  
**Batch recommendation:** **merge** (`member_task_ids: ["157", "158"]`; `proposed_slice_id: sys-norm-n18-role-l1`;
`shared_acceptance_surface: LegalHoldListView EntityLink + DOCUMENT_AUTHOR L1 i18n/docs`;
`evidence_amortization: one FE gates + E2E + deploy`;
vetoes_applied: checklist-#3b/#5a, CE-O02, #53, #119-Word-host, #106-umbrella;
`on_red_split_hint: If N18 fails, peel L1 docs-only to solo`) — **open**

---

## Purpose

Deliver two post-SYS-NORM residuals under one shared FE leaf:

1. **#157 N18** — `LegalHoldListView` Created-by column uses `EntityLinkCell` (display name when present / username fallback / em dash; navigable to Users when identity-administration permitted; fail-closed plain text otherwise); optional additive `createdByDisplayName`.
2. **#158 DOCUMENT_AUTHOR L1** — lock Confirmed L1 display strings EN **Document author** / ZH **文档作者** (no interim suffix); role ID `DOCUMENT_AUTHOR` unchanged.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **`sys-norm-n18-role-l1`** (TM **#157** + **#158**) |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a residual N18 + P-Q1 L1 **In Progress** |
| Gate evidence | Pending implementation (stage 4+) |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02 / **#119** / **#106**; expand N19–N20; rename role ID |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| N18-T01 | Register TM **#157** + **#158** + plan/ledger sole-active mirror | **In Progress** (plan-orchestrator stage 2) |
| N18-T02 | Doc-keeper: terminology / matrix / ADR-0070 L1 Confirmed + optional OpenAPI additive | **Not Started** |
| N18-T03 | Frontend **#157**: LegalHoldListView Created-by EntityLinkCell | **Not Started** |
| N18-T04 | Frontend/docs **#158**: DOCUMENT_AUTHOR L1 EN/ZH lock (no interim) | **Not Started** |
| N18-T05 | Optional BE additive `createdByDisplayName` | **Not Started** (preferred; must not block EntityLink) |
| N18-T06 | FE unit/component tests (TDD) | **Not Started** |
| N18-T07 | E2E + UIUX (BDD-N18-L1-001…012) | **Not Started** |
| N18-T08 | Queued docker deploy evidence + merge + MAIN doc-sync | **Not Started** |

### Task Master members

| TM | Title | Status |
| --- | --- | --- |
| **#157** | N18 Legal-hold Created-by EntityLink | **In Progress** (leaf lead) |
| **#158** | DOCUMENT_AUTHOR L1 Confirmed EN/ZH | **In Progress** (merged member) |

---

## Exit criteria (from BDD-N18-L1-001…012)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Created by uses EntityLinkCell | N18-L1-001 | **Not Started** |
| 2 | Display name preferred as label | N18-L1-002 | **Not Started** |
| 3 | Username fallback | N18-L1-003 | **Not Started** |
| 4 | Link when identity admin permitted | N18-L1-004 | **Not Started** |
| 5 | Plain text when identity admin denied | N18-L1-005 | **Not Started** |
| 6 | Empty actor is em dash | N18-L1-006 | **Not Started** |
| 7 | Navigation lands on users catalog | N18-L1-007 | **Not Started** |
| 8 | English L1 Document author locked | N18-L1-008 | **Not Started** |
| 9 | Chinese L1 文档作者 locked | N18-L1-009 | **Not Started** |
| 10 | Role ID unchanged | N18-L1-010 | **Not Started** |
| 11 | No interim suffix on L1 | N18-L1-011 | **Not Started** |
| 12 | Locks / vetoes held (#3b/#5a/#53/CE-O02/#119) | N18-L1-012 | **Not Started** |

---

## Locks

- Do **not** mark umbrella **#53** Done.
- Do **not** activate **#119** (Word host) or CE-O02.
- Do **not** treat **#106** as a delivery leaf.
- Do **not** flip checklist **#3b** / **#5a**.
- Formal phase remains **None**.
