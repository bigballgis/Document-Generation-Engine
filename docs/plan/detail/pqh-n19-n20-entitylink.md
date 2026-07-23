# PQH Leaf 2 — N19–N20 EntityLink where-used + MasterImpact

**Program / slice:** `pqh-n19-n20-entitylink` (Post-Queue Hardening; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#161** (PQH N19–N20) → **Done** (Leaf 2 closed)  
**Active delivery slice:** `pqh-n19-n20-entitylink` (**sole-active cleared**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-pqh-n19-n20-entitylink` · branch `feat/pqh-n19-n20-entitylink` (**REMOVED**)  
**MAIN tip:** `20c67ac9` (Stage 10 evidence on MAIN; feature tip `1e023a35`; E2E `fc50f06e`)  
**BDD:** [pqh-n19-n20-entitylink.md](../../behavior/pqh-n19-n20-entitylink.md) — **ready**/shipped (`BDD-PQH-N19N20-001…014`); `frontend_ui_in_scope=true`; `backend_api_contract_change=false`  
**Program:** [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md)  
**Upstream residual:** [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) N19–N20 (Waves **0–8** remain **Done** — do **not** reopen)  
**Batch recommendation:** **solo** (`member_task_ids: ["161"]`; `proposed_slice_id: pqh-n19-n20-entitylink`;
`shared_acceptance_surface: ContentModuleWhereUsedPanel groupCode EntityLink + MasterImpactPanel EntityLinkCell / fail-closed template links`;
`evidence_amortization: one FE gates + E2E + UIUX + queued deploy`;
vetoes_applied: n19n20-vs-n22, checklist-#3b/#5a-GO, CE-O02, mark-#53-CE-Done, activate-#119, do-not-reopen-SYS-NORM-Waves-0-8;
`on_red_split_hint: Peel N20 MasterImpact if where-used groupCode alone is green`) — **closed**

---

## Purpose

Deliver SYS-NORM residuals **N19–N20** under PQH Leaf 2:

1. **N19** — `ContentModuleWhereUsedPanel` **groupCode** column uses `EntityLinkCell` + `groupCatalogLink` (identity-administration fail-closed); **lock** existing template-name `EntityLinkCell` regression.
2. **N20** — `MasterImpactPanel` referenced templates use `EntityLinkCell` + `templateDetailLink` (fail-closed plain text when template-management denied).

**Out of scope:** N22 catalog row actions (TM **#162**); IBL-E6 nesting columns; API contract changes; reopening SYS-NORM Waves 0–8.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared for this leaf** — Leaf 3 TM **#162** N22 also **Done** (`ef1b505d`) |
| Program | PQH **Done** — Leaf 1–4 **Done** (this leaf closed first among Leaf 2–4; Leaf 4 **#163** → **Done** `b739a38f`) |
| Gate evidence | FE lint/type-check/test(**1721**)/build **GREEN**; E2E **7/7** PASS; UIUX **PASS_WITH_NOTES** Critical=0; architecture **PASS_WITH_NOTES** Critical=0 `merge_go=true`; Stage 5+10 **DEPLOY_OK** [pqh-n19-n20-entitylink/](../evidence/pqh-n19-n20-entitylink/) |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02 / **#119** / F7; reopen SYS-NORM Waves 0–8; claim IBL/CE/go-live Done |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| PQH-N19N20-T01 | Plan/TM sole-active activation + detail/ledger/program cross-links | **Done** |
| PQH-N19N20-T02 | Frontend TDD: where-used `groupCode` EntityLink + template name regression lock | **Done** |
| PQH-N19N20-T03 | Frontend TDD: MasterImpact `EntityLinkCell` fail-closed | **Done** |
| PQH-N19N20-T04 | FE lint / type-check / test / build | **Done** |
| PQH-N19N20-T05 | E2E + UIUX (BDD-PQH-N19N20-001…014) | **Done** |
| PQH-N19N20-T06 | Architecture (+ optional CQ) review | **Done** |
| PQH-N19N20-T07 | Queued docker deploy evidence + merge + MAIN doc-sync | **Done** (Stage 12 this sync) |

### Task Master members

| TM | Alias | Title | Status |
| --- | --- | --- | --- |
| **#161** | N19–N20 | EntityLink where-used + MasterImpact | **Done** |

### Queued / parked (not this leaf)

| TM | Alias | Status |
| --- | --- | --- |
| **#162** | N22 | **Done** (Leaf 3 closed — [pqh-n22-catalog-row-actions.md](./pqh-n22-catalog-row-actions.md); MAIN `ef1b505d`) |
| **#163** | PQH-F7 (Bucket4j→Redis) | **pending** (parked) |
| **#159** / **#160** | PQH-CHARTER / PQH-F8 | **Done** (Leaf 1 closed) |

---

## Exit criteria (from BDD-PQH-N19N20-001…014)

| # | Criterion | Status |
| --- | --- | --- |
| 1–14 | See behavior SoT **BDD-PQH-N19N20-001…014** | **Done** |
| Locks | Waves 0–8 stay Done; #162 not folded; #53 / #3b/#5a / CE-O02 / #119 held | **Done** (governance held) |

---

## Related

| Doc | Role |
| --- | --- |
| [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md) | Program SoT |
| [pqh-n19-n20-entitylink.md](../../behavior/pqh-n19-n20-entitylink.md) | Behavior SoT |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | N19–N20 residual origin (waves stay Done) |
| [sys-norm-n18-role-l1.md](../../behavior/sys-norm-n18-role-l1.md) | EntityLink fail-closed pattern reference |
| [ux-entity-display-constitution.md](../../architecture/ux-entity-display-constitution.md) | Entity display constitution |
| `.cursor/skills/frontend-entity-display/SKILL.md` | Implementer skill (prefer over doc-keeper) |
| [evidence/pqh-n19-n20-entitylink/](../evidence/pqh-n19-n20-entitylink/) | Stage 5+10 deploy evidence |
| [execution-sync-ledger.md](../execution-sync-ledger.md) | Activation / evidence ledger |
