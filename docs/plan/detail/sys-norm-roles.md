# SYS-NORM Wave 5 — Six-role compression

**Program / slice:** `sys-norm-roles` (SYS-NORM Wave **5**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#149** → **Done** (2026-07-21; MAIN merge `febb95b3`; worktree **REMOVED**)  
**Active delivery slice:** none — sole-active **cleared** after Wave 5 merge  
**Placement (historical):** **ISOLATED** · worktree `D:/working/DGE-sys-norm-roles` · branch `feat/sys-norm-roles` — **REMOVED** after stage 11 FF merge  
**BDD:** [sys-norm-roles.md](../../behavior/sys-norm-roles.md) — **ready** / delivered (`BDD-SYS-NORM-ROLE-001…018`); `frontend_ui_in_scope=true`  
**ADR:** [ADR-0070 Accepted](../../adr/authorization-security/0070-role-compression-six-roles.md) — impl landed; Accepted decision text unchanged  
**Batch recommendation:** **solo** (`member_task_ids: ["149"]`; `proposed_slice_id: sys-norm-roles`) — **closed**

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
| Leaf status | **Done** (MAIN `febb95b3`; worktree **REMOVED**) |
| Formal phase | **None** |
| Host sole-active | **cleared** (Wave 5 closed; Wave 6 **#150** also **Done** `64b0a650`) |
| Next queue head | Wave 7 `sys-norm-promotion-pack` — **Not Started** / **not** activated (Wave 6 Done — [sys-norm-d1-brands.md](./sys-norm-d1-brands.md)) |
| Program | Waves **0–6 Done**; Waves **7–8 Not Started** — program **not** Done |

---

## Exit criteria

| # | Criterion | Evidence |
| --- | --- | --- |
| 1 | Wave 5 BDD ready | [sys-norm-roles.md](../../behavior/sys-norm-roles.md) **BDD-SYS-NORM-ROLE-001…018** — **met** |
| 2 | Permission-matrix rewrite (doc-keeper) before production code | [permission-matrix.md](../../security/permission-matrix.md) — **met** (stage 3) |
| 3 | Six-role catalog + migration + JWT/FE per ADR-0070 | Gates + ROLE scenarios green — **met** |
| 4 | Program plan Wave 5 → Done; Wave 6 not auto-activated | [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) — **met** |
| 5 | Vetoes held | No `#3b/#5a` GO; no `#53` Done; no Wave 6+ fold; no SYS-NORM program Done — **met** |

---

## Gate evidence (2026-07-21)

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN 2357/0/0** |
| Frontend lint / type-check / test / build | **GREEN** (test **1639**) |
| Stage 5 + 10 queued deploy | **DEPLOY_OK** — [stage5](../evidence/sys-norm-roles-stage5-deploy/) · [stage10](../evidence/sys-norm-roles-stage10-deploy/) |
| E2E | **7/7** |
| UIUX | **PASS** |
| Architecture | Critical=0 · `merge_go` |
| Code quality | allow merge |
| Stage 11 merge | MAIN `febb95b3` FF; worktree **REMOVED** |

---

## Gate order (locked — completed)

1. BDD **ready** (met)  
2. **doc-keeper** matrix rewrite (stage 3) — **landed**  
3. TDD Red → Green (BE + FE) — **met**  
4. E2E + UIUX (`frontend_ui_in_scope=true`) — **met**  
5. Queued deploy evidence (stages 5/10) — **met**  
6. Stage 11 merge → MAIN doc-sync / commit-review — merge **met**; this sync = stage 12  

---

## Out of scope this leaf

- Wave 6 D1 brand/entity runtime retirement  
- Wave 7 promotion pack  
- Wave 8 terminology / demo seed  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Claiming SYS-NORM program Done  
