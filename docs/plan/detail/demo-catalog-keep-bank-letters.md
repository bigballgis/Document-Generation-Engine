# Slim demo catalog — keep bank-letter Live set

**Program / slice:** `demo-catalog-keep-bank-letters` (ad-hoc **NON-CE** ops-demo cleanup leaf; **not** a formal P-phase; **not** IBL Wave B; **not** CE-O02)  
**Formal plan phase:** **None** — single-active-phase discipline OK (does not occupy a P* slot)  
**Task Master:** **#164** (`demo-catalog-keep-bank-letters`) → **In Progress** (sole-active host delivery leaf)  
**Active delivery slice:** `demo-catalog-keep-bank-letters` (**sole-active confirmed**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-demo-catalog-keep-bank-letters` · branch `feat/demo-catalog-keep-bank-letters`  
**Integration base:** `origin/main` @ `719538b9`  
**BDD:** [demo-catalog-keep-bank-letters.md](../../behavior/demo-catalog-keep-bank-letters.md) — **ready** (`BDD-DEMO-KEEP-001…014`); `frontend_ui_in_scope=false`  
**Ops runbook:** [demo-catalog-keep-bank-letters.md](../../operations/demo-catalog-keep-bank-letters.md)  
**Evidence stub:** [demo-catalog-keep-bank-letters/](../evidence/demo-catalog-keep-bank-letters/README.md)  
**Batch recommendation:** **solo** (`member_task_ids: ["164"]`; `proposed_slice_id: demo-catalog-keep-bank-letters`;
`shared_acceptance_surface: acceptance catalog after cleanup shows only keep-set`;
`evidence_amortization: mvn verify + docker-deploy-queue cleanup evidence + keep-set smoke`;
vetoes_applied: do-not-flip-3b-5a, do-not-mark-53-106-Done, do-not-touch-CE-O02, no-frontend-unless-empty-state;
`on_red_split_hint: Peel Java seeder retire vs script cleanup if verify fails`) — **open** (leaf In Progress)

**Upstream (Done, do not reopen):** Wave A TM **#141** [`bank-letter-demo-refresh`](./bank-letter-demo-refresh.md) · Wave B TM **#142** expand · PQH Leaf 4 TM **#163** Done (`b739a38f`; sole-active cleared before this leaf)

---

## Purpose

Contract acceptance/screenshot demo catalog to the user-confirmed **8 Live bank-letter templates** and their referenced letterhead / standard clauses / asset-library assets; purge other DEMO noise packages and orphans; retire Java `ApplicationRunner` seeders that would reintroduce purged template IDs on reboot; keep deploy PowerShell import/publish as the authoritative keep-set load path.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** |
| Formal phase | **None** |
| Host sole-active | **#164** / `demo-catalog-keep-bank-letters` (**confirmed**) |
| Umbrella #53 / #106 | Registry-only — **do not** mark Done |
| Gate evidence | Not yet (plan activation only — implementers own verify/deploy) |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; touch CE-O02; invent frontend empty-state work unless catalog empty; claim go-live / IBL / CE Done |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| DEMO-KEEP-T01 | Plan/TM sole-active activation + detail/ledger/index cross-links | **Done** (stage 2 plan-orchestrator) |
| DEMO-KEEP-T02 | Doc-keeper: package/README/ops note + indexes for keep-set / seeder retirement | **Done** (stage 3 — ops [demo-catalog-keep-bank-letters.md](../../operations/demo-catalog-keep-bank-letters.md); `deploy/demo-shared/README.md` KEEP-8 SoT; behavior §9 retirement rationale; evidence stub; `docs/README.md` indexed) — **#164** remains **In Progress** |
| DEMO-KEEP-T03 | Backend/ops: purge demos + orphans; retire unused Java seeders/scripts (BDD-DEMO-KEEP-001…014) | **Not Started** |
| DEMO-KEEP-T04 | `mvn verify` + queued docker-deploy cleanup evidence + keep-set smoke | **Not Started** |
| DEMO-KEEP-T05 | Stage 11 merge + MAIN doc-sync + commit-review | **Not Started** |

### Task Master members

| TM | Alias | Title | Status |
| --- | --- | --- | --- |
| **#164** | `demo-catalog-keep-bank-letters` | Slim demo catalog keep 8 bank-letter Live templates | **In Progress** |

### Related (closed)

| TM | Alias | Status |
| --- | --- | --- |
| **#141** | Wave A refresh | **Done** (`aa88170f` / `5ae9575a`) |
| **#142** | Wave B expand | **Done** (`288ce98f`) |
| **#163** | PQH-F7 | **Done** (`b739a38f`; prior sole-active cleared) |

---

## Exit criteria (from BDD-DEMO-KEEP-001…014)

| # | Criterion | Status |
| --- | --- | --- |
| 1–14 | See behavior SoT **BDD-DEMO-KEEP-001…014** | **Not Started** (spec ready) |
| Locks | #53 / #106 / #3b/#5a / CE-O02 held; Wave A/B stay Done | **Held** |

---

## Owners (pipeline)

1. **doc-keeper** (stage 3) — ops runbook + index sync → **Done** (this leaf docs; **#164** still In Progress)  
2. **backend-engineer** (stage 4) — planned cleanup script + package/registry shrink + Java seeder retirement (**next**)  
3. **build-deploy-agent** — queued Docker cleanup / keep-set smoke evidence

---

## Vetoes (hard)

- Checklist **#3b / #5a** — do **not** flip **GO**
- Umbrella **#53** / **#106** — do **not** mark **Done**
- **CE-O02** — do **not** touch / activate
- **Frontend** — `frontend_ui_in_scope=false`; no empty-state UI unless keep-set somehow empty (should not)
- Do **not** claim go-live / IBL / CE program Done
- Do **not** reopen P22/P23 for this ops leaf
