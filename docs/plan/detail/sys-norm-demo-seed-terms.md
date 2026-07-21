# SYS-NORM Wave 8 — Demo/验收 seed · honest empty · L1 Letterhead/母版

**Program / slice:** `sys-norm-demo-seed-terms` (SYS-NORM Wave **8**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#152** → **In Progress** (activated 2026-07-21; stage 2 plan-orchestrator)  
**Active delivery slice:** **#152** — host **sole-active** NON-CE delivery leaf  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-sys-norm-demo-seed-terms` · branch `feat/sys-norm-demo-seed-terms` · base `9f89019b`  
**BDD:** [sys-norm-demo-seed-terms.md](../../behavior/sys-norm-demo-seed-terms.md) — **ready** (`BDD-SYS-NORM-W8-001…018`); `frontend_ui_in_scope=true`; `backend_seed_in_scope=true`  
**Upstream:** Waves **0–7 Done** (Wave 7 TM **#151** `11356c63` / `f795b04a`); charter §2.8 / §2.9; CE-E02 / CE-G04 / F1 / TPC  
**Batch recommendation:** **solo** (`member_task_ids: ["152"]`; `proposed_slice_id: sys-norm-demo-seed-terms`;
`shared_acceptance_surface: asset seed/honest empty + L1 Letterhead/母版 + N13/N15/N16/N17/N21/N23`;
`evidence_amortization: one FE/BE evidence run for seed+terms`;
vetoes: checklist-#3b/#5a, CE-O02, #53, parked-UX-not-in-W8;
`on_red_split_hint: If seed BE fails, peel terminology FE to follow-up leaf`) — **active**

---

## Purpose

Close SYS-NORM Wave 8: product-default **honest empty** for zero managed assets / legal holds;
optional **demo/验收** managed-asset seed path; **N23** docs (`demo-images` ≠ Asset Library);
**N13** legal-hold empty; **L1** EN **Letterhead** / ZH **母版** (N16–N17); **N15** empty design
summary; **N21** journey empty guidance. **N18** remains deferred. Parked UX out of scope.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#152** Wave 8 (`sys-norm-demo-seed-terms`) |
| Program | Waves **0–7 Done**; Wave **8 In Progress** — program **not** Done |
| Next after this leaf | Program Done criteria (§5.2) iff Wave 8 Done + N* closed/deferred with evidence — **not** claimed yet |

---

## Exit criteria (from BDD W8-001…018)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Asset library honest empty (default) | W8-001 | Not Started |
| 2 | Optional demo/验收 managed-asset seed path (documented) | W8-002 | **Docs locked** ([demo-acceptance-asset-seed.md](../../operations/demo-acceptance-asset-seed.md)); BE impl Not Started |
| 3 | N23 docs: demo-images bypass ≠ Asset Library | W8-003 | **Docs Done** (ops + CE-E02/F1/TPC pointers; stage 3) |
| 4 | Production / true-prod demo tier default off (TPC-C7) | W8-004 | Docs aligned (TPC-C7/C7a); config verify with impl |
| 5 | N13 Legal hold empty (manage → Create CTA) | W8-005 | Docs note locked (CE-G04); FE impl Not Started |
| 6 | N13 Legal hold empty (no manage → no Create) | W8-006 | Docs note locked (CE-G04); FE impl Not Started |
| 7 | N16 L1 English **Letterhead** | W8-007 | Terminology SSOT locked; FE i18n Not Started |
| 8 | N17 L1 Chinese **母版** | W8-008 | Terminology SSOT locked; FE i18n Not Started |
| 9 | Purge L1 Master mix; keep L3 `masterId` | W8-009 | Terminology SSOT locked; FE sweep Not Started |
| 10 | Terminology guide SSOT pointer | W8-010 | **Docs Done** ([business-terminology-guide.md](../../product/business-terminology-guide.md); stage 3) |
| 11 | English-first i18n for changed strings | W8-011 | Not Started |
| 12 | N15 Master revision empty design summary | W8-012 | Not Started |
| 13 | N21 Role journey timeline honest empty | W8-013 | Not Started |
| 14 | N21 Forbidden silent empty when steps empty | W8-014 | Not Started |
| 15 | N18 remains deferred (explicit; not claimed Done) | W8-015 | Locked (deferred) |
| 16 | Parked UX out of scope | W8-016 | Locked (OOS) |
| 17 | Residuals N19–N20 / N22 / P-Q1 defer with evidence if capacity | W8-017 | Capacity |
| 18 | Governance: no #3b/#5a flip; no #53 Done; no program Done; phase None; CE-O02 Deferred | W8-018 | Locked |

---

## Gate evidence

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | pending |
| Frontend lint / type-check / test / build | pending |
| Stage 5 + 10 queued deploy | pending |
| E2E / UIUX | pending (`frontend_ui_in_scope=true`) |
| Architecture / code quality | pending |
| Stage 11 merge | pending |

---

## Implementation readiness (stage 2 activate — stages 3–4 next)

### Backend — seed path (in scope)

Optional profile/Flyway managed `library_asset` seed for demo/验收; production defaults keep
honest empty + demo classpath tier off. Do **not** change F1 resolver signature.

### Frontend — empty + terminology (in scope)

Asset Library / Legal holds honest empty; Letterhead/母版 L1 sweep; N15 design empty; N21
journey guidance; English-first i18n.

### Doc-keeper (stage 3) — before/with code

**Done (docs-first):** terminology SSOT Letterhead/母版 + Master-mix purge guidance;
[demo-acceptance-asset-seed.md](../../operations/demo-acceptance-asset-seed.md) (honest empty
default + optional seed + N23); CE-E02 / F1 / TPC / CE-G04 / catalog UX / charter §2.9
pointers; indexes. Matrix/OpenAPI unchanged (no new capability codes).

### Gate order (locked)

1. BDD **ready** (met)  
2. **doc-keeper** terminology/ops/N23 as needed  
3. TDD Red → Green (BE seed + FE empty/terms)  
4. E2E + UIUX  
5. Queued deploy evidence (stages 5/10)  
6. Stage 11 merge → MAIN doc-sync / commit-review  

---

## Out of scope this leaf

- **N18** Legal hold actor EntityLink (explicitly deferred — post-Wave later leaf + BDD)  
- **Parked UX (post-program queue, plan §4a — not Wave 8):** Reminder timing; Asset library group isolation; Binding editor re-layout; Auto `referenceKey`  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Claiming SYS-NORM program Done before Wave 8 impl Done  
- Activating CE-O02 / RTL  
- Changing F1 resolver signature / production demo-tier default on  
- Second Docker compose stack  

## Related docs (stage 3)

| Doc | Role |
| --- | --- |
| [sys-norm-demo-seed-terms.md](../../behavior/sys-norm-demo-seed-terms.md) | Wave 8 BDD SoT |
| [business-terminology-guide.md](../../product/business-terminology-guide.md) | L1 Letterhead / 母版 SSOT |
| [demo-acceptance-asset-seed.md](../../operations/demo-acceptance-asset-seed.md) | Seed ops + N23 |
| [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md) | Empty-state product notes |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | Program wave table |
