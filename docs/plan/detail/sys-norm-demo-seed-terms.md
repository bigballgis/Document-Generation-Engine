# SYS-NORM Wave 8 — Demo/验收 seed · honest empty · L1 Letterhead/母版

**Program / slice:** `sys-norm-demo-seed-terms` (SYS-NORM Wave **8**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#152** → **Done** (MAIN merge `8aca145b`; feature `7df6c563`; worktree **REMOVED**)  
**Active delivery slice:** **none** — sole-active **cleared**  
**Placement:** **merged** · worktree **REMOVED** · branch `feat/sys-norm-demo-seed-terms`  
**BDD:** [sys-norm-demo-seed-terms.md](../../behavior/sys-norm-demo-seed-terms.md) — **ready** (`BDD-SYS-NORM-W8-001…018`); `frontend_ui_in_scope=true`; `backend_seed_in_scope=true`  
**Upstream:** Waves **0–7 Done**; charter §2.8 / §2.9; CE-E02 / CE-G04 / F1 / TPC  
**Batch recommendation:** **solo** (`member_task_ids: ["152"]`; `proposed_slice_id: sys-norm-demo-seed-terms`;
`shared_acceptance_surface: asset seed/honest empty + L1 Letterhead/母版 + N13/N15/N16/N17/N21/N23`;
`evidence_amortization: one FE/BE evidence run for seed+terms`;
vetoes: checklist-#3b/#5a, CE-O02, #53, parked-UX-not-in-W8;
`on_red_split_hint: If seed BE fails, peel terminology FE to follow-up leaf`) — **closed**

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
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared** |
| Program | Waves **0–8 Done** — program **Done** (§5.2); **N18 deferred** (does not block) |
| Next after this leaf | Post-program parked queue (§4a) — **Not Started** / **not** activated; do **not** flip **#3b/#5a**; do **not** mark **#53** Done; do **not** activate CE-O02 |

---

## Exit criteria (from BDD W8-001…018)

| # | Criterion | BDD | Status |
| --- | --- | --- | --- |
| 1 | Asset library honest empty (default) | W8-001 | **Done** |
| 2 | Optional demo/验收 managed-asset seed path (documented + implemented) | W8-002 | **Done** ([demo-acceptance-asset-seed.md](../../operations/demo-acceptance-asset-seed.md) + `DemoAssetLibrarySeeder`) |
| 3 | N23 docs: demo-images bypass ≠ Asset Library | W8-003 | **Done** |
| 4 | Production / true-prod demo tier default off (TPC-C7) | W8-004 | **Done** |
| 5 | N13 Legal hold empty (manage → Create CTA) | W8-005 | **Done** |
| 6 | N13 Legal hold empty (no manage → no Create) | W8-006 | **Done** |
| 7 | N16 L1 English **Letterhead** | W8-007 | **Done** |
| 8 | N17 L1 Chinese **母版** | W8-008 | **Done** |
| 9 | Purge L1 Master mix; keep L3 `masterId` | W8-009 | **Done** |
| 10 | Terminology guide SSOT pointer | W8-010 | **Done** |
| 11 | English-first i18n for changed strings | W8-011 | **Done** |
| 12 | N15 Master revision empty design summary | W8-012 | **Done** |
| 13 | N21 Role journey timeline honest empty | W8-013 | **Done** |
| 14 | N21 Forbidden silent empty when steps empty | W8-014 | **Done** |
| 15 | N18 remains deferred (explicit; not claimed Done) | W8-015 | **Locked (deferred)** — does not block Wave/program Done |
| 16 | Parked UX out of scope | W8-016 | **Locked (OOS)** — post-program §4a |
| 17 | Residuals N19–N20 / N22 / P-Q1 defer with evidence if capacity | W8-017 | Capacity defer OK |
| 18 | Governance: no #3b/#5a flip; no #53 Done; phase None; CE-O02 Deferred | W8-018 | **Met** (program Done claimed; umbrella #53 stays in-progress) |

---

## Gate evidence

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN 2391** |
| Frontend lint / type-check / test / build | **GREEN** (test **1652**) |
| Stage 5 + 10 queued deploy | **DEPLOY_OK** |
| E2E / UIUX | E2E W8 **5/5** PASS; UIUX **PASS_WITH_NOTES** Critical=0 |
| Architecture / code quality | Arch **merge_with_notes** Critical=0 |
| Stage 11 merge | MAIN `8aca145b` (feature `7df6c563`); worktree **REMOVED** |

---

## Out of scope this leaf (still true)

- **N18** Legal hold actor EntityLink (explicitly deferred — later leaf + BDD)  
- **Parked UX (post-program queue, plan §4a):** Reminder timing; Asset library group isolation; Binding editor re-layout; Auto `referenceKey`  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Activating CE-O02 / RTL  

## Related docs

| Doc | Role |
| --- | --- |
| [sys-norm-demo-seed-terms.md](../../behavior/sys-norm-demo-seed-terms.md) | Wave 8 BDD SoT |
| [business-terminology-guide.md](../../product/business-terminology-guide.md) | L1 Letterhead / 母版 SSOT |
| [demo-acceptance-asset-seed.md](../../operations/demo-acceptance-asset-seed.md) | Seed ops + N23 (**implemented**) |
| [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md) | Empty-state product notes |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | Program wave table — Waves **0–8 Done** |
