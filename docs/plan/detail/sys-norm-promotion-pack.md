# SYS-NORM Wave 7 — UAT→PROD promotion pack + dry-run UI

**Program / slice:** `sys-norm-promotion-pack` (SYS-NORM Wave **7**; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#151** → **In Progress** (activated 2026-07-21; stage 2 plan-orchestrator)  
**Active delivery slice:** **sole-active** host leaf — Wave 7 only  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-sys-norm-promotion-pack` · branch `feat/sys-norm-promotion-pack` · base MAIN tip `51b96e36` (Wave 6 Done `64b0a650`)  
**BDD:** [sys-norm-promotion-pack.md](../../behavior/sys-norm-promotion-pack.md) — **ready** (`BDD-SYS-NORM-PP-001…020`); `frontend_ui_in_scope=true`  
**Upstream:** extend CE-E01 / CE-E03 (do not break default v1/v2 paths); ADR-0071 Decision 5 (no brand/entity sidecar)  
**Batch recommendation:** **solo** (`member_task_ids: ["151"]`; `proposed_slice_id: sys-norm-promotion-pack`;
`shared_acceptance_surface: UAT→PROD promotion pack + dry-run UI`;
`evidence_amortization: one mvn verify + FE gates + E2E + docker queue`;
vetoes: checklist-#3b/#5a, CE-O02, #53, Wave-8, parked-UX) — **open**

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
| Leaf status | **In Progress** (TM **#151**; sole-active) |
| Formal phase | **None** |
| Host sole-active | **#151** `sys-norm-promotion-pack` |
| Program | Waves **0–6 Done**; Wave **7 In Progress**; Wave **8 Not Started** — program **not** Done |
| Next after this leaf | Wave 8 `sys-norm-demo-seed-terms` — **Not Started** / **not** activated |

---

## Exit criteria (from BDD)

| # | Criterion | BDD / evidence |
| --- | --- | --- |
| 1 | Wave 7 BDD ready | [sys-norm-promotion-pack.md](../../behavior/sys-norm-promotion-pack.md) **PP-001…020** — **met** (`ready`) |
| 2 | Promotion closure export (assets binary + nesting graph; fail-closed) | PP-C1/C2; PP-001… |
| 3 | No brand/entity sidecar; no secrets | PP-C4/C5 |
| 4 | Two-phase master/letterhead; import → DRAFT; re-test/re-approve/re-publish | PP-C6/C7 |
| 5 | Dry-run API zero writes + additive report types; commit transactional | PP-C8/C9/C10 |
| 6 | FE Import dialog dry-run UI (P-Q4) + E2E/UIUX | PP-C12; §5.1 |
| 7 | CE-E01/E03 non-promotion paths preserved | PP-C13 |
| 8 | Governance locks held | PP-C14 — no `#3b/#5a` GO; no `#53` Done; no program Done; no Wave 8/parked fold |
| 9 | Gates green + queued deploy + plan/ledger sync (Wave 7 Done ≠ program Done) | PP-020 |

---

## Implementation readiness (stage 2 — activated)

### Backend — pending stage 4

Promotion closure profile on template/library export; nested clause graph + asset binaries;
import dry-run/commit extensions; fail-closed gates; OpenAPI additive.

### Frontend — pending stage 4

`TemplateImportDialog`: **Check dependencies** (`dryRun=true`) + **Import** gated on
`readyToCommit`; report list; English-first i18n; OA dialog patterns.

### Doc-keeper (stage 3) — next

OpenAPI export/import notes; permission-matrix §5 cross-ref (no new codes); CE-E01/E03 /
catalog-navigation notes as needed.

### Gate order (locked)

1. BDD **ready** (met)  
2. **doc-keeper** OpenAPI / matrix / export notes  
3. TDD Red → Green (BE + FE)  
4. E2E + UIUX  
5. Queued deploy evidence (stages 5/10)  
6. Stage 11 merge → MAIN doc-sync / commit-review  

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
