# SYS-NORM Wave 7 — UAT→PROD promotion pack + dry-run UI

**Program / slice:** `sys-norm-promotion-pack` (SYS-NORM Wave **7**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#151** → **Done** (2026-07-21; MAIN merge `11356c63`; feature `f795b04a`; worktree **REMOVED**)  
**Active delivery slice:** none — Wave 7 closed; host sole-active **cleared**  
**Placement (historical):** **ISOLATED** · worktree `D:/working/DGE-sys-norm-promotion-pack` · branch `feat/sys-norm-promotion-pack` — **REMOVED** after stage 11 merge  
**BDD:** [sys-norm-promotion-pack.md](../../behavior/sys-norm-promotion-pack.md) — **ready** / shipped (`BDD-SYS-NORM-PP-001…020`); `frontend_ui_in_scope=true`  
**Upstream:** extend CE-E01 / CE-E03 (default non-promotion paths preserved); ADR-0071 Decision 5 (no brand/entity sidecar)  
**Batch recommendation:** **solo** (`member_task_ids: ["151"]`; `proposed_slice_id: sys-norm-promotion-pack`;
`shared_acceptance_surface: UAT→PROD promotion pack + dry-run UI`;
`evidence_amortization: one mvn verify + FE gates + E2E + docker queue`;
vetoes: checklist-#3b/#5a, CE-O02, #53, Wave-8, parked-UX) — **closed**

---

## Purpose

Deliver UAT→PROD **promotion pack** export (dependency closure with asset binaries + clause
nesting graph; two-phase P2 masters; no secrets; no brand/entity sidecar) and PROD
**dry-run + commit** import (API + Templates catalog Import dialog UI), extending CE-E01/E03
without breaking non-promotion paths.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** (MAIN `11356c63` / feature `f795b04a`; worktree **REMOVED**) |
| Formal phase | **None** |
| Host sole-active | **cleared** |
| Program | Waves **0–8 Done** — program **Done** (Wave 8 TM **#152** `8aca145b`) |
| Next after this leaf | Wave 8 `sys-norm-demo-seed-terms` — **Not Started** / **not** activated |

---

## Exit criteria (from BDD)

| # | Criterion | BDD / evidence |
| --- | --- | --- |
| 1 | Wave 7 BDD ready | [sys-norm-promotion-pack.md](../../behavior/sys-norm-promotion-pack.md) **PP-001…020** — **met** (`ready` / shipped) |
| 2 | Promotion closure export (assets binary + nesting graph; fail-closed) | PP-C1/C2; PP-001… — **met** |
| 3 | No brand/entity sidecar; no secrets | PP-C4/C5 — **met** |
| 4 | Two-phase master/letterhead; import → DRAFT; re-test/re-approve/re-publish | PP-C6/C7 — **met** |
| 5 | Dry-run API zero writes + additive report types; commit transactional | PP-C8/C9/C10 — **met** |
| 6 | FE Import dialog dry-run UI (P-Q4) + E2E/UIUX | PP-C12; §5.1 — **met** (E2E W7 **4/4** + P14 **2/2**; UIUX **PASS_WITH_NOTES** Critical=0) |
| 7 | CE-E01/E03 non-promotion paths preserved | PP-C13 — **met** |
| 8 | Governance locks held | PP-C14 — **met** (no `#3b/#5a` GO; no `#53` Done; no program Done; no Wave 8/parked fold) |
| 9 | Gates green + queued deploy + plan/ledger sync (Wave 7 Done ≠ program Done) | PP-020 — **met** |

---

## Gate evidence (2026-07-21)

| Gate | Result |
| --- | --- |
| Backend `mvn verify` | **GREEN 2381** |
| Frontend lint / type-check / test / build | **GREEN** (test **~1640**) |
| Stage 5 + 10 queued deploy | **DEPLOY_OK** |
| E2E | W7 **4/4** + P14 **2/2** · `frontend/e2e/evidence/SYS-NORM-W7*` |
| UIUX | **PASS_WITH_NOTES** Critical=0 · [SYS-NORM-W7-uiux-manifest.md](../../../frontend/e2e/evidence/SYS-NORM-W7-uiux-manifest.md) |
| Architecture | **PASS_WITH_NOTES** Critical=0 |
| Code quality | **PASS_WITH_NOTES** Critical=0 |
| Stage 11 merge | MAIN `11356c63`; feature `f795b04a`; worktree **REMOVED** |

---

## Implementation readiness (closed — stage 4 delivered)

### Backend — promotion pack delivered

Opt-in `dependencyClosure=PROMOTION` on template/library export; nested clause graph +
asset binaries; import dry-run/commit extensions; fail-closed gates; OpenAPI additive;
default CE-E01/E03 non-promotion paths preserved.

### Frontend — Import dry-run UI delivered

`TemplateImportDialog`: **Check dependencies** (`dryRun=true`) + **Import** gated on
`readyToCommit`; report list; English-first i18n; OA dialog patterns; E2E/UIUX covered.

### Doc-keeper (stage 3) — landed before/with code

OpenAPI export/import notes; permission-matrix §5 cross-ref (no new codes); CE-E01/E03 /
catalog-navigation notes as needed.

### Gate order (locked — completed)

1. BDD **ready** (met)  
2. **doc-keeper** OpenAPI / matrix / export notes — **met**  
3. TDD Red → Green (BE + FE) — **met**  
4. E2E + UIUX — **met**  
5. Queued deploy evidence (stages 5/10) — **met**  
6. Stage 11 merge → MAIN doc-sync / commit-review — merge **met**; this sync = stage 12  

---

## Out of scope this leaf

- Wave 8 demo seed / L1 terminology sweep  
- Parked: reminder timing; asset library group isolation; binding editor re-layout; auto `referenceKey`  
- Flipping checklist **#3b** / **#5a**  
- Marking umbrella **#53** Done  
- Claiming SYS-NORM program Done  
- Activating CE-O02 / RTL  
- Breaking CE-E01/E03 default non-promotion paths  
- Second Docker compose stack / async Kafka export job  
