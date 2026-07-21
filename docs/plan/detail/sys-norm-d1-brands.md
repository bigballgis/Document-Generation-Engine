# SYS-NORM Wave 6 — D1 DocumentBrand / LegalEntity runtime retirement

**Program / slice:** `sys-norm-d1-brands` (SYS-NORM Wave **6**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#150** → **Done** (2026-07-21; MAIN merge `64b0a650`; worktree **REMOVED**)  
**Active delivery slice:** none — sole-active **cleared** after Wave 6 merge  
**Placement (historical):** **ISOLATED** · worktree `D:/working/DGE-sys-norm-d1-brands` · branch `feat/sys-norm-d1-brands` — **REMOVED** after stage 11 merge  
**BDD:** [sys-norm-d1-brands.md](../../behavior/sys-norm-d1-brands.md) — **ready** / delivered (`BDD-SYS-NORM-D1-001…020`); `frontend_ui_in_scope=true`  
**ADR:** [ADR-0071 Accepted](../../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md) — impl landed; Accepted decision text unchanged  
**Batch recommendation:** **solo** (`member_task_ids: ["150"]`; `proposed_slice_id: sys-norm-d1-brands`;
`shared_acceptance_surface: D1 brand/entity retirement`;
`evidence_amortization: one verify+E2E+deploy for D1`;
vetoes: checklist-#3b/#5a, CE-O02, #53, Wave-7-promotion, Wave-8-seed,
parked-reminder/asset/binding/refkey) — **closed**

---

## Purpose

Complete ADR-0071 **D1** after Wave 1 nav hide: retire Document brands + Legal entities as
**required product surfaces** (UI + management API + runtime catalog dependency); drive
letterhead / logo / seal from **Letterhead (master)**; keep **Legal holds**; keep shell
`REDBC`/`GREENBC` UI-only; runtime simplify fail-closed (no LegalEntity→DocumentBrand
catalog resolve).

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** (MAIN `64b0a650`; worktree **REMOVED**) |
| Formal phase | **None** |
| Host sole-active | **cleared** for Wave 6 — next queue head Wave 7 **not** activated |
| Program | Waves **0–6 Done**; Waves **7–8 Not Started** — program **not** Done |
| Next after this leaf | Wave 7 `sys-norm-promotion-pack` — **Not Started** / **not** activated |

---

## Exit criteria (from BDD)

| # | Criterion | BDD / evidence |
| --- | --- | --- |
| 1 | Wave 6 BDD ready | [sys-norm-d1-brands.md](../../behavior/sys-norm-d1-brands.md) **D1-001…020** — **met** (`ready`) |
| 2 | Product surfaces retired (nav absent + hard routes) | D1-001, D1-006, D1-007, D1-008 — **met** |
| 3 | Management APIs fail-closed 404/410 + durable hard delete | D1-009, D1-010, D1-011 — **met** (Flyway **V76**) |
| 4 | Runtime / preview / test-generation simplify; letterhead SoT; `legalEntityCode` non-driving; allow-list retired | D1-004, D1-012…015, D1-017 — **met** |
| 5 | Legal holds kept; shell themes orthogonal; no brand/entity sidecar required for export | D1-003, D1-005, D1-016 — **met** |
| 6 | Governance locks held | D1-002, D1-018 — **met** (no `#3b/#5a` GO; no `#53` Done; no program Done; no Wave 7/8/parked fold) |
| 7 | i18n English-first + OA style for retirement UX | D1-019 — **met** |
| 8 | Gates green + queued deploy + plan/ledger sync (Wave 6 Done ≠ program Done) | D1-020 — **met** |

---

## Gate evidence (2026-07-21)

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN 2370** · Flyway **V76** |
| Frontend lint / type-check / test / build | **GREEN** (test **1634**) |
| Stage 5 + 10 queued deploy | **DEPLOY_OK** — [stage5](../evidence/sys-norm-d1-brands-stage5-deploy/) · [stage10](../evidence/sys-norm-d1-brands-stage10-deploy/) |
| E2E | **16/16** |
| UIUX | **PASS_WITH_NOTES** Critical=0 |
| Architecture | **PASS_WITH_NOTES** Critical=0 |
| Code quality | **PASS_WITH_NOTES** Critical=0 |
| Stage 11 merge | MAIN `64b0a650`; worktree **REMOVED** |

---

## Implementation readiness (closed — stage 4 delivered)

### Backend — atomic retire delivered

Management API kill (404/410 + `*_SURFACE_RETIRED`); Flyway **V76** hard-delete/retire;
runtime/preview/test-generation simplify (letterhead SoT; `legalEntityCode` opaque;
allow-list retired); Legal holds kept.

### Frontend — hard cleanup delivered

Routes/catalogs/pickers/API consumers retired; Legal holds + Letterhead logo/seal path +
REDBC/GREENBC UI-only regression covered by E2E/UIUX.

### Doc-keeper (stage 3) — landed before/with code

permission-matrix §5.3; OpenAPI 404/410 + surface-retired codes; catalog-navigation /
terminology / domain / contract notes.

### Gate order (locked — completed)

1. BDD **ready** (met)  
2. **doc-keeper** matrix / OpenAPI / catalog notes — **met**  
3. TDD Red → Green (BE + FE) — **met**  
4. E2E + UIUX — **met**  
5. Queued deploy evidence (stages 5/10) — **met**  
6. Stage 11 merge → MAIN doc-sync / commit-review — merge **met**; this sync = stage 12  

---

## Out of scope this leaf

- Wave 7 promotion pack / dry-run UI  
- Wave 8 demo seed / L1 terminology sweep  
- Parked: reminder timing; asset library group isolation; binding editor re-layout; auto `referenceKey`  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Claiming SYS-NORM program Done  
- Reintroducing DocumentBrand/LegalEntity management UX as a required surface  
- Silent rewrite of ADR-0065 Decision body  
