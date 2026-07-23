# PQH Leaf 4 — F7 Redis / coordinated runtime rate-limit

**Program / slice:** `pqh-f7-redis-rate-limit` (Post-Queue Hardening; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#163** (PQH-F7) → **Done** (Leaf 4 closed)  
**Active delivery slice:** `pqh-f7-redis-rate-limit` (**sole-active cleared**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-pqh-f7-redis-rate-limit` · branch `feat/pqh-f7-redis-rate-limit` (**REMOVED**)  
**MAIN tip / merge:** `b739a38f` (Stage 11 FF integrate on `main`; **push blocked** — `main` ahead of `origin/main`, GitHub 443)  
**BDD:** [pqh-f7-redis-rate-limit.md](../../behavior/pqh-f7-redis-rate-limit.md) — **ready**/shipped (`BDD-PQH-F7-001…012`); `frontend_ui_in_scope=false`; `backend_api_contract_change=true` (fail-closed **503** `RATE_LIMIT_BACKEND_UNAVAILABLE` when distributed Redis down)  
**Program:** [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md)  
**Upstream residual:** [intl-bank-letter-readiness-program.md](../intl-bank-letter-readiness-program.md) **F7** / **Q1** → **Done under PQH** (IBL waves stay as-is — **not** an IBL wave reopen; do **not** claim IBL Done)  
**Batch recommendation:** **solo** (`member_task_ids: ["163"]`; `proposed_slice_id: pqh-f7-redis-rate-limit`;
`shared_acceptance_surface: RuntimeRateLimitService/Filter + Redis coordinated limiter`;
`evidence_amortization: mvn verify + Stage 10 queued deploy; FE E2E N/A`;
vetoes_applied: checklist-#3b/#5a-GO, CE-O02, mark-#53-CE-Done, activate-#119-Word-host, do-not-claim-IBL-CE-go-live-Done;
`on_red_split_hint: N/A solo`) — **closed**

---

## Purpose

Deliver IBL **F7** / **Q1** under PQH Leaf 4 — Redis / coordinated **runtime** API rate-limit:

1. **Process-local default** — `distributed=false` (default / opt-in `RUNTIME_RATE_LIMIT_DISTRIBUTED`): today’s in-process Bucket4j unchanged.
2. **Coordinated quota** — `distributed=true`: shared Redis-backed bucket across ≥2 instances for `credentialId:accessAccount`.
3. **Identity + pass-through** — missing credential headers pass through (LR-B7); no IP bucket fallback.
4. **Fail-closed Redis down** — when distributed and Redis coordination unavailable → **503** `RATE_LIMIT_BACKEND_UNAVAILABLE` (retryable); **no** silent process-local fallback.
5. **Scope** — runtime filter only (`/api/{env}/v1/*`); not management/login limiters.

**Out of scope / deferred:** claiming multi-instance correctness complete (SSE / Redisson locks / Kafka); flipping **#3b/#5a**; activating CE-O02 / **#119**; marking **#53** Done; claiming IBL/CE/go-live Done; reopening SYS-NORM Waves 0–8.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **cleared** (no host delivery leaf In Progress) |
| Program | PQH **Done** — Leaf 1–4 **Done**; next queue **empty**; scheduled leaves closed (priority **#119**/checklist / seal-writer backlog remain outside PQH — not activated) |
| Gate evidence | `mvn verify` **GREEN 2425**/0/15; architecture **PASS_WITH_NOTES** Critical=0 `merge_go=true` (notes: Testcontainers IT before multi-replica enable; Redis TIME vs JVM clock; key hygiene; ADR lettuce-custom honesty); E2E/UIUX **N/A** (`frontend_ui_in_scope=false`); Stage 10 **DEPLOY_OK** [pqh-f7-redis-rate-limit-stage10-deploy/](../evidence/pqh-f7-redis-rate-limit-stage10-deploy/); MAIN FF merge `b739a38f`; worktree **REMOVED**; **push blocked** (GitHub 443; `main` ahead of `origin/main`) |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02 / **#119**; claim IBL/CE/go-live Done; reopen SYS-NORM Waves 0–8 |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| PQH-F7-T01 | Plan/TM sole-active activation + detail/ledger/program cross-links | **Done** |
| PQH-F7-T02 | Doc-keeper: OpenAPI / error catalog for `RATE_LIMIT_BACKEND_UNAVAILABLE` | **Done** |
| PQH-F7-T03 | Backend TDD: process-local parity + Redis coordinated limiter + fail-closed 503 (BDD-PQH-F7-001…012) | **Done** — Lettuce Lua `RedisRuntimeRateLimitService`; process-local default; distributed opt-in |
| PQH-F7-T04 | `mvn verify` + architecture review | **Done** — **2425**/0/15 GREEN; Arch **PASS_WITH_NOTES** Critical=0 `merge_go=true` |
| PQH-F7-T05 | Queued docker deploy evidence (Stage 5/10 as required) + merge + MAIN doc-sync | **Done** — Stage 10 **DEPLOY_OK**; Stage 11 FF `b739a38f`; Stage 12 this sync; Stage 13 owns commit; **push blocked** pending network |

### Task Master members

| TM | Alias | Title | Status |
| --- | --- | --- | --- |
| **#163** | PQH-F7 | Bucket4j → Redis / coordinated runtime rate-limit | **Done** |

### Closed (not this leaf)

| TM | Alias | Status |
| --- | --- | --- |
| **#162** | N22 | **Done** (Leaf 3 closed — [pqh-n22-catalog-row-actions.md](./pqh-n22-catalog-row-actions.md); MAIN `ef1b505d`) |
| **#161** | N19–N20 | **Done** (Leaf 2 closed — [pqh-n19-n20-entitylink.md](./pqh-n19-n20-entitylink.md); MAIN `20c67ac9`) |
| **#159** / **#160** | PQH-CHARTER / PQH-F8 | **Done** (Leaf 1 closed — [pqh-f8-format-date-tz.md](./pqh-f8-format-date-tz.md); MAIN `ab382c02`) |

---

## Exit criteria (from BDD-PQH-F7-001…012)

| # | Criterion | Status |
| --- | --- | --- |
| 1–12 | See behavior SoT **BDD-PQH-F7-001…012** | **Done** (shipped) |
| Locks | #53 / #3b/#5a / CE-O02 / #119 / IBL-CE-go-live held; Leaf 1–3 stay Done | **Held** |

---

## Related

| Doc | Role |
| --- | --- |
| [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md) | Program SoT |
| [pqh-f7-redis-rate-limit.md](../../behavior/pqh-f7-redis-rate-limit.md) | Behavior SoT |
| [intl-bank-letter-readiness-program.md](../intl-bank-letter-readiness-program.md) | F7/Q1 finding origin (not IBL wave reopen) |
| [execution-sync-ledger.md](../execution-sync-ledger.md) | Activation / evidence ledger |
| [pqh-f7-redis-rate-limit-stage10-deploy/](../evidence/pqh-f7-redis-rate-limit-stage10-deploy/) | Stage 10 deploy evidence |
| ADR-0044 multi-instance / topology | Scale-out rate-limit prerequisite |
| ADR-0039 | Redisson locks remain deferred (orthogonal) |

---

## Next stage

**Stage 13** `post-task-commit-review` on MAIN (commit doc-sync; retry push when GitHub 443 clears).  
**Next queue:** **empty** — PQH scheduled Leaf 1–4 closed; program **Done**. Do **not** auto-activate CE-O02 / **#119** / flip **#3b/#5a**.
