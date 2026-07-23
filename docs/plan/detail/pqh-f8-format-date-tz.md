# PQH Leaf 1 — Charter + FORMAT_DATE timezone / as-of

**Program / slice:** `pqh-f8-format-date-tz` (Post-Queue Hardening; **NON-CE**; **not** a formal P-phase)  
**Formal plan phase:** **None**  
**Task Master:** **#159** (PQH-CHARTER) + **#160** (PQH-F8) → **Done** (merge leaf closed)  
**Active delivery slice:** `pqh-f8-format-date-tz` (**sole-active cleared**)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-pqh-f8-format-date-tz` · branch `feat/pqh-f8-format-date-tz` (**REMOVED**)  
**MAIN tip:** `ab382c02` (Stage 10 evidence on MAIN; feature tip `ee0893fe`)  
**BDD:** [pqh-f8-format-date-tz.md](../../behavior/pqh-f8-format-date-tz.md) — **ready**/shipped (`BDD-PQH-F8-001…012`); charter BDD **not-applicable**; `frontend_ui_in_scope=false`  
**Program:** [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md)  
**Batch recommendation:** **merge** (`member_task_ids: ["159", "160"]`; `proposed_slice_id: pqh-f8-format-date-tz`;
`shared_acceptance_surface: FORMAT_DATE timezone/as-of honesty + PQH charter docs`;
`evidence_amortization: mvn verify + Stage 10 deploy; E2E N/A`;
vetoes_applied: f8-vs-n19n22-unrelated-domains, checklist-#3b/#5a-GO, CE-O02, mark-#53-CE-Done, activate-#119-Word-host, F7-parked-not-in-leaf, do-not-claim-IBL-go-live-Done;
`on_red_split_hint: Peel F8 vs charter on red verify`) — **closed**

---

## Purpose

1. **#159 PQH-CHARTER** — Invent and register NON-CE program `post-queue-hardening-2026-07` with priority lock, serial queue, F7 parked, and vetoes.
2. **#160 PQH-F8** — Implement honest `FORMAT_DATE` optional IANA `zoneId` + documented UTC unary default; date-only as-of (IBL F8 / Q2).

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** |
| Formal phase | **None** |
| Host sole-active | **superseded** — Leaf 2 TM **#161** `pqh-n19-n20-entitylink` (**In Progress**) |
| Program | PQH **In Progress** — Leaf 1 **Done**; Leaf 2 N19–N20 **In Progress**; Leaf 3 N22 queued; F7 parked |
| Gate evidence | `mvn verify` **GREEN 2414/0/0/15**; architecture **PASS_WITH_NOTES** Critical=0 `merge_go=true`; Stage 10 **DEPLOY_OK** [pqh-f8-format-date-tz/](../evidence/pqh-f8-format-date-tz/); E2E/UIUX **N/A** |
| Do **not** | Flip **#3b/#5a**; mark **#53** Done; activate CE-O02 / **#119** / F7; mark N19–N22 Done; claim IBL/CE/go-live Done; reopen SYS-NORM Waves 0–8 |

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| PQH-T01 | Register TM **#159**+**#160** (+ queue stubs) + program/detail/ledger sole-active | **Done** |
| PQH-T02 | Doc-keeper: ADR-0056 / OpenAPI / contract-outline note for zone arg + UTC default (if required) | **Done** |
| PQH-T03 | Backend TDD: FORMAT_DATE zoneId / UTC unary / date-only as-of (BDD-PQH-F8-001…012) | **Done** |
| PQH-T04 | `mvn verify` + architecture review | **Done** |
| PQH-T05 | Queued docker deploy evidence (Stage 5/10 as required) + merge + MAIN doc-sync | **Done** (Stage 12 this sync) |

### Task Master members

| TM | Alias | Title | Status |
| --- | --- | --- | --- |
| **#159** | PQH-CHARTER | Post-queue hardening program charter | **Done** |
| **#160** | PQH-F8 | FORMAT_DATE timezone / as-of semantics | **Done** |

### Queued / parked (not this leaf)

| TM | Alias | Status |
| --- | --- | --- |
| **#161** | N19–N20 | **In Progress** (Leaf 2 sole-active — [pqh-n19-n20-entitylink.md](./pqh-n19-n20-entitylink.md)) |
| **#162** | N22 | **pending** (Leaf 3) |
| **#163** | PQH-F7 (Bucket4j→Redis) | **pending** (parked) |

---

## Exit criteria (from BDD-PQH-F8-001…012)

| # | Criterion | Status |
| --- | --- | --- |
| 1–12 | See behavior SoT **BDD-PQH-F8-001…012** | **Done** |
| Charter | Program plan + TM + sole-active + indexes | **Done** |

---

## Related

| Doc | Role |
| --- | --- |
| [post-queue-hardening-program-2026-07.md](../post-queue-hardening-program-2026-07.md) | Program SoT |
| [pqh-f8-format-date-tz.md](../../behavior/pqh-f8-format-date-tz.md) | F8 behavior SoT |
| [intl-bank-letter-readiness-program.md](../intl-bank-letter-readiness-program.md) | F8 / Q2 finding source |
| [system-normalization-program-2026-07.md](../system-normalization-program-2026-07.md) | N19–N22 residual origin (waves stay Done) |
| [evidence/pqh-f8-format-date-tz/](../evidence/pqh-f8-format-date-tz/) | Stage 10 deploy evidence |
