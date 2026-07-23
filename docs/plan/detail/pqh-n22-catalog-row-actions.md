# PQH Leaf 3 — N22 Catalog row action pattern (Edit/More)

**Program / slice:** `pqh-n22-catalog-row-actions` (Post-Queue Hardening; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#162** (PQH N22) → **In Progress** (sole-active Leaf 3)  
**Active delivery slice:** `pqh-n22-catalog-row-actions` (**host sole-active**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-pqh-n22-catalog-row-actions` · branch `feat/pqh-n22-catalog-row-actions`  
**BDD:** [pqh-n22-catalog-row-actions.md](../../behavior/pqh-n22-catalog-row-actions.md) — **ready** (`BDD-PQH-N22-001…014`); `frontend_ui_in_scope=true`; `backend_api_contract_change=false`  
**Program:** [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md)  
**Upstream residual:** [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) N22 (Waves **0–8** remain **Done** — do **not** reopen)  
**Batch recommendation:** **solo** (`member_task_ids: ["162"]`; `proposed_slice_id: pqh-n22-catalog-row-actions`;
`shared_acceptance_surface: Asset Library + Legal Holds + API Invocations Actions columns use TableEditMoreActions; Users/Groups regression lock; optional Edit when no edit surface`;
`evidence_amortization: one FE gates + E2E + UIUX + queued deploy`;
vetoes_applied: checklist-#3b/#5a-GO, CE-O02, mark-#53-CE-Done, activate-#119, do-not-reopen-SYS-NORM-Waves-0-8, F7-parked-not-in-leaf;
`on_red_split_hint: Peel API Invocations if Asset Library + Legal Holds Edit/More-only path is green`) — **active**

---

## Purpose

Deliver SYS-NORM residual **N22** under PQH Leaf 3 — catalog-wide **Edit / More** row-action pattern via `TableEditMoreActions`:

1. **Asset Library** — move ad-hoc Disable into More; do **not** invent an edit surface.
2. **Legal Holds** — move ad-hoc Release into More; do **not** invent an edit surface.
3. **API Invocations** — primary = Open detail; package settings under More.
4. **Users / Groups** — Wave 1 Edit/More **regression lock**.

**Out of scope / deferred:** hub-nested action tables; API Policy Home alerts CTA; task-hub Open; inventing Actions on Template/Master/CM catalogs; F7 (**#163**); reopening SYS-NORM Waves 0–8; API contract changes.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#162** `pqh-n22-catalog-row-actions` |
| Program | PQH **In Progress** — Leaf 1 **Done**; Leaf 2 **Done**; Leaf 3 **In Progress**; F7 **#163** parked |
| Gate evidence | Pending frontend-engineer → FE gates / E2E / UIUX / Arch / Stage 5+10 |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02 / **#119** / F7; reopen SYS-NORM Waves 0–8; claim IBL/CE/go-live Done |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| PQH-N22-T01 | Plan/TM sole-active activation + detail/ledger/program cross-links | **Done** (this stage) |
| PQH-N22-T02 | Frontend TDD: Asset Library More-only lifecycle (`TableEditMoreActions`) | **Done** (stage 4 FE) |
| PQH-N22-T03 | Frontend TDD: Legal Holds More-only lifecycle | **Done** (stage 4 FE) |
| PQH-N22-T04 | Frontend TDD: API Invocations primary Open detail + settings under More | **Done** (stage 4 FE) |
| PQH-N22-T05 | Frontend TDD: Users/Groups Edit/More regression lock | **Done** (stage 4 FE) |
| PQH-N22-T06 | FE lint / type-check / test / build | **Done** (stage 4 FE; evidence on feature branch) |
| PQH-N22-T07 | E2E + UIUX (BDD-PQH-N22-001…014) | **Not Started** |
| PQH-N22-T08 | Architecture (+ optional CQ) review | **Not Started** |
| PQH-N22-T09 | Queued docker deploy evidence + merge + MAIN doc-sync | **Not Started** |

### Task Master members

| TM | Alias | Title | Status |
| --- | --- | --- | --- |
| **#162** | N22 | Catalog row action pattern | **In Progress** (sole-active) |

### Closed / parked (not this leaf)

| TM | Alias | Status |
| --- | --- | --- |
| **#161** | N19–N20 | **Done** (Leaf 2 closed) |
| **#159** / **#160** | PQH-CHARTER / PQH-F8 | **Done** (Leaf 1 closed) |
| **#163** | PQH-F7 (Bucket4j→Redis) | **pending** (parked) |

---

## Exit criteria (from BDD-PQH-N22-001…014)

| # | Criterion | Status |
| --- | --- | --- |
| 1–14 | See behavior SoT **BDD-PQH-N22-001…014** | **Not Started** |
| Locks | Waves 0–8 stay Done; #53 / #3b/#5a / CE-O02 / #119 / F7 held | **In Progress** (governance held) |

---

## Related

| Doc | Role |
| --- | --- |
| [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md) | Program SoT |
| [pqh-n22-catalog-row-actions.md](../../behavior/pqh-n22-catalog-row-actions.md) | Behavior SoT |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | N22 residual origin (waves stay Done) |
| [sys-norm-shell-fluid-nav.md](../../behavior/sys-norm-shell-fluid-nav.md) | Wave 1 Users/Groups Edit/More reference |
| [pqh-n19-n20-entitylink.md](../../behavior/pqh-n19-n20-entitylink.md) | Prior PQH Leaf 2 |
| `frontend/src/components/common/TableEditMoreActions.vue` | Shared Actions pattern |
| `.cursor/skills/frontend-oa-design/SKILL.md` | OA / Actions implementer skill |
| [execution-sync-ledger.md](../execution-sync-ledger.md) | Activation / evidence ledger |

---

## Next stage

**frontend-engineer** (stage 4) — TDD implementation for in-scope catalogs + regression locks.  
**doc-keeper:** skip unless contract/docs gap appears.
