# SYS-NORM Wave 5 — Six-role compression

**Program / slice:** `sys-norm-roles` (SYS-NORM Wave **5**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#149** → **In Progress** (activated 2026-07-21; stage 2 plan-orchestrator)  
**Active delivery slice:** `sys-norm-roles` — **sole-active**  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-sys-norm-roles` · branch `feat/sys-norm-roles` (base `origin/main` @ `32ead955`)  
**BDD:** [sys-norm-roles.md](../../behavior/sys-norm-roles.md) — **ready** (`BDD-SYS-NORM-ROLE-001…018`); `frontend_ui_in_scope=true`  
**ADR:** [ADR-0070 Accepted](../../adr/authorization-security/0070-role-compression-six-roles.md)  
**Batch recommendation:** **solo** (`member_task_ids: ["149"]`; `proposed_slice_id: sys-norm-roles`) — **active**

---

## Purpose

Implement ADR-0070 six-role compression end-to-end: permission-matrix rewrite (doc-keeper
**stage 3 before** production code), durable role migration remap, JWT/session capabilities,
FE role pickers / journey labels, and fail-closed assignment (`ROLE_NOT_ASSIGNABLE`).
Retain SoD: `TEMPLATE_TESTER` remains distinct; authors do not gain `decideTests`.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** (activated; matrix rewrite + code **not** Done) |
| Formal phase | **None** |
| Host sole-active | **#149** `sys-norm-roles` |
| Next queue head (after this leaf) | `sys-norm-d1-brands` (Wave 6) — **Not Started** / **not** activated |
| Program | Waves **0–4 Done**; Wave **5 In Progress**; Waves **6–8 Not Started** — program **not** Done |

---

## Exit criteria

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | Wave 5 BDD ready | [sys-norm-roles.md](../../behavior/sys-norm-roles.md) **BDD-SYS-NORM-ROLE-001…018** |
| 2 | Permission-matrix rewrite (doc-keeper) before production code | [permission-matrix.md](../../security/permission-matrix.md) — stage 3 |
| 3 | Six-role catalog + migration + JWT/FE per ADR-0070 | Gates + ROLE scenarios green |
| 4 | Program plan Wave 5 → Done; Wave 6 not auto-activated | [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) |
| 5 | Vetoes held | No `#3b/#5a` GO; no `#53` Done; no Wave 6+ fold; no SYS-NORM program Done |

---

## Gate order (locked)

1. BDD **ready** (met)  
2. **doc-keeper** matrix rewrite (stage 3) — **before** role-catalog / Flyway / FE enum code  
3. TDD Red → Green (BE + FE)  
4. E2E + UIUX (`frontend_ui_in_scope=true`)  
5. Queued deploy evidence (stages 5/10)  
6. Stage 11 merge → MAIN doc-sync / commit-review  

---

## Out of scope this leaf

- Wave 6 D1 brand/entity runtime retirement  
- Wave 7 promotion pack  
- Wave 8 terminology / demo seed  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Claiming SYS-NORM program Done  
