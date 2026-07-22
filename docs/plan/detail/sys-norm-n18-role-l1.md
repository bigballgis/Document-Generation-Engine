# N18 Legal-hold EntityLink + DOCUMENT_AUTHOR L1

**Program / slice:** `sys-norm-n18-role-l1` (post-SYS-NORM residual; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#157** (N18 Legal-hold Created-by EntityLink) + **#158** (`DOCUMENT_AUTHOR` L1 Confirmed) → **Done** (merge leaf)  
**Active delivery slice:** `sys-norm-n18-role-l1` (**sole-active cleared**)  
**Placement:** **ISOLATED** · worktree **REMOVED** · branch `feat/sys-norm-n18-role-l1` merged · MAIN merge `a4f59c4d` · feature tip `b54281b1`  
**BDD:** [sys-norm-n18-role-l1.md](../../behavior/sys-norm-n18-role-l1.md) — **ready**/shipped (`BDD-N18-L1-001…012`); `frontend_ui_in_scope=true`; `backend_api_contract_change=optional-additive`  
**Upstream:** SYS-NORM Waves **0–8 Done**; §4a parked UX (Reminder / Asset library / Binding editor + Auto `referenceKey`) **Done**; Wave 1 EntityLink primitives + CE-G04 Legal hold list shipped  
**Batch recommendation:** **merge** (`member_task_ids: ["157", "158"]`; `proposed_slice_id: sys-norm-n18-role-l1`;
`shared_acceptance_surface: LegalHoldListView EntityLink + DOCUMENT_AUTHOR L1 i18n/docs`;
`evidence_amortization: one FE gates + E2E + deploy`;
vetoes_applied: checklist-#3b/#5a, CE-O02, #53, #119-Word-host, #106-umbrella;
`on_red_split_hint: If N18 fails, peel L1 docs-only to solo`) — **closed**

---

## Purpose

Deliver two post-SYS-NORM residuals under one shared FE leaf:

1. **#157 N18** — `LegalHoldListView` Created-by column uses `EntityLinkCell` (display name when present / username fallback / em dash; navigable to Users when identity-administration permitted; fail-closed plain text otherwise); optional additive `createdByDisplayName`.
2. **#158 DOCUMENT_AUTHOR L1** — lock Confirmed L1 display strings EN **Document author** / ZH **文档作者** (no interim suffix); role ID `DOCUMENT_AUTHOR` unchanged.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared** (prior leaf `sys-norm-n18-role-l1`) |
| Program | SYS-NORM Waves **0–8 Done** — program **Done**; §4a residual N18 + P-Q1 L1 → **Done** |
| Gate evidence | FE lint/type-check/test(**1710**)/build **GREEN**; E2E SYS-NORM-N18 **5/5** PASS; UIUX **PASS_WITH_NOTES** Critical=0; Arch **merge_with_notes** Critical=0; Stage 5/10 **DEPLOY_OK**; MAIN merge `a4f59c4d` / feature `b54281b1` |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02 / **#119** / **#106**; expand N19–N20; rename role ID |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| N18-T01 | Register TM **#157** + **#158** + plan/ledger sole-active mirror | **Done** |
| N18-T02 | Doc-keeper: terminology / matrix / ADR-0070 L1 Confirmed + optional OpenAPI additive | **Done** |
| N18-T03 | Frontend **#157**: LegalHoldListView Created-by EntityLinkCell | **Done** |
| N18-T04 | Frontend/docs **#158**: DOCUMENT_AUTHOR L1 EN/ZH lock (no interim) | **Done** |
| N18-T05 | Optional BE additive `createdByDisplayName` | **Done** (delivered in leaf) |
| N18-T06 | FE unit/component tests (TDD) | **Done** |
| N18-T07 | E2E + UIUX (BDD-N18-L1-001…012) | **Done** (E2E **5/5**; UIUX **PASS_WITH_NOTES**) |
| N18-T08 | Queued docker deploy evidence + merge + MAIN doc-sync | **Done** (Stage 5/10 **DEPLOY_OK**; merge `a4f59c4d`; this sync) |

### Task Master members

| TM | Title | Status |
| --- | --- | --- |
| **#157** | N18 Legal-hold Created-by EntityLink | **Done** (leaf lead) |
| **#158** | DOCUMENT_AUTHOR L1 Confirmed EN/ZH | **Done** (merged member) |

---

## Exit criteria (from BDD-N18-L1-001…012)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Created by uses EntityLinkCell | N18-L1-001 | **Done** |
| 2 | Display name preferred as label | N18-L1-002 | **Done** |
| 3 | Username fallback | N18-L1-003 | **Done** |
| 4 | Link when identity admin permitted | N18-L1-004 | **Done** |
| 5 | Plain text when identity admin denied | N18-L1-005 | **Done** |
| 6 | Empty actor is em dash | N18-L1-006 | **Done** |
| 7 | Navigation lands on users catalog | N18-L1-007 | **Done** |
| 8 | English L1 Document author locked | N18-L1-008 | **Done** |
| 9 | Chinese L1 文档作者 locked | N18-L1-009 | **Done** |
| 10 | Role ID unchanged | N18-L1-010 | **Done** |
| 11 | No interim suffix on L1 | N18-L1-011 | **Done** |
| 12 | Locks / vetoes held (#3b/#5a/#53/CE-O02/#119) | N18-L1-012 | **Done** |

---

## Evidence pointers

- E2E: `frontend/e2e/SYS-NORM-N18-role-l1.spec.ts` · `frontend/e2e/evidence/SYS-NORM-N18/`
- Deploy: [sys-norm-n18-role-l1-stage10-deploy/](../evidence/sys-norm-n18-role-l1-stage10-deploy/)
- Program: [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md)
- Ledger: [execution-sync-ledger.md](../execution-sync-ledger.md)

---

## Locks

- Do **not** mark umbrella **#53** Done.
- Do **not** activate **#119** (Word host) or CE-O02.
- Do **not** treat **#106** as a delivery leaf.
- Do **not** flip checklist **#3b** / **#5a**.
- Formal phase remains **None**.
