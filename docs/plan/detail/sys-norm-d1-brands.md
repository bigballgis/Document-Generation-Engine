# SYS-NORM Wave 6 — D1 DocumentBrand / LegalEntity runtime retirement

**Program / slice:** `sys-norm-d1-brands` (SYS-NORM Wave **6**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#150** → **In Progress** (activated 2026-07-21; sole-active delivery leaf)  
**Active delivery slice:** `sys-norm-d1-brands` — host **sole-active**  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-sys-norm-d1-brands` · branch `feat/sys-norm-d1-brands`  
**BDD:** [sys-norm-d1-brands.md](../../behavior/sys-norm-d1-brands.md) — **ready** (`BDD-SYS-NORM-D1-001…020`); `frontend_ui_in_scope=true`  
**ADR:** [ADR-0071 Accepted](../../adr/template-lifecycle/0071-retire-document-brand-legal-entity-surfaces.md)  
**Batch recommendation:** **solo** (`member_task_ids: ["150"]`; `proposed_slice_id: sys-norm-d1-brands`;
`shared_acceptance_surface: D1 brand/entity retirement`;
`evidence_amortization: one verify+E2E+deploy for D1`;
vetoes: checklist-#3b/#5a, CE-O02, #53, Wave-7-promotion, Wave-8-seed,
parked-reminder/asset/binding/refkey) — **active**

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
| Leaf status | **In Progress** (TM **#150**) |
| Formal phase | **None** |
| Host sole-active | **#150** `sys-norm-d1-brands` (Wave 5 **#149** sole-active **cleared** / Done) |
| Program | Waves **0–5 Done**; Wave **6 In Progress**; Waves **7–8 Not Started** — program **not** Done |
| Next after this leaf | Wave 7 `sys-norm-promotion-pack` — **Not Started** / **not** activated |

---

## Exit criteria (from BDD)

| # | Criterion | BDD / evidence |
| --- | --- | --- |
| 1 | Wave 6 BDD ready | [sys-norm-d1-brands.md](../../behavior/sys-norm-d1-brands.md) **D1-001…020** — **met** (`ready`) |
| 2 | Product surfaces retired (nav absent + hard routes) | D1-001, D1-006, D1-007, D1-008 |
| 3 | Management APIs fail-closed 404/410 + durable hard delete | D1-009, D1-010, D1-011 |
| 4 | Runtime / preview / test-generation simplify; letterhead SoT; `legalEntityCode` non-driving; allow-list retired | D1-004, D1-012…015, D1-017 |
| 5 | Legal holds kept; shell themes orthogonal; no brand/entity sidecar required for export | D1-003, D1-005, D1-016 |
| 6 | Governance locks held | D1-002, D1-018 — no `#3b/#5a` GO; no `#53` Done; no program Done; no Wave 7/8/parked fold |
| 7 | i18n English-first + OA style for retirement UX | D1-019 |
| 8 | Gates green + queued deploy + plan/ledger sync (Wave 6 Done ≠ program Done) | D1-020 |

---

## Implementation readiness (one leaf — stage 4 handoff)

Still **one** delivery leaf / one evidence run. Split notes for specialist owners only.

### Backend (`backend-engineer`) — atomic retire preferred

| Focus | Notes |
| --- | --- |
| Management API kill | DocumentBrand / LegalEntity CRUD + group `defaultLegalEntityCode` product APIs → **404/410** + stable codes (`DOCUMENT_BRAND_SURFACE_RETIRED` / `LEGAL_ENTITY_SURFACE_RETIRED` family; OpenAPI) |
| Durable delete | Flyway (or equivalent) hard-delete / irreversible retire of brand/entity persistence, seeds, default legal-entity bindings |
| Runtime simplify | generate / preview / test-generation **must not** resolve LegalEntity→DocumentBrand catalogs; letterhead/logo/seal from Letterhead (master) / non-catalog bindings |
| Context / allow-list | `context.legalEntityCode` opaque non-driving (no retired catalog 422s); `allowedDocumentBrandCodes` no longer gates |
| Keep | Legal holds backend unchanged; do not use UI theme codes as document brand MDM |
| on_red_split_hint | Keep BE retire **atomic**; do not peel mid-migration if that leaves dual catalogs alive |

### Frontend (`frontend-engineer`) — hard cleanup (Wave 1 nav already Done)

| Focus | Notes |
| --- | --- |
| Hard retire | Routes, catalogs, pickers, brand allow-list editors, happy-path API consumers for brand/entity |
| Legacy bookmarks | Close W1-004 soft allowance — no product catalog UI (404 / honest gone / redirect away from dual-catalog) |
| Keep / regression | Legal holds nav+behavior; Letterhead (master) logo/seal path; REDBC/GREENBC UI-only |
| E2E / UIUX | `frontend_ui_in_scope=true` — cover retirement + keeps + theme orthogonality |
| on_red_split_hint | If runtime simplify fails, FE nav cleanup already Done in Wave 1 may peel; keep BE retire atomic |

### Doc-keeper (stage 3 — before / with code)

| Focus | Notes |
| --- | --- |
| Matrix / product / OpenAPI | **Landed (docs-first):** permission-matrix §5.3 retire; OpenAPI 404/410 + `*_SURFACE_RETIRED`; catalog-navigation D1 row; terminology §4.6; domain §2.3.1; contract-outline / api README |
| Supersession | ADR-0065 historical; product direction follows ADR-0071 — Decision body not rewritten; IBL-E4 banner → Wave 6 ready BDD |
| Out of scope | Wave 7 promotion dry-run docs as delivery; Wave 8 terminology/seed; parked UX items |
| Status | Stage 3 docs **ready for BE/FE** — does **not** mark Wave 6 / #150 Done |

### Gate order (locked)

1. BDD **ready** (met)  
2. **doc-keeper** matrix / OpenAPI / catalog notes as needed  
3. TDD Red → Green (**BE** then/with **FE** in same leaf)  
4. E2E + UIUX  
5. Queued deploy evidence (stages 5/10)  
6. Stage 11 merge → MAIN doc-sync / commit-review  

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
