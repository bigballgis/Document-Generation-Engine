# Post-Queue Hardening Program (PQH) — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `post-queue-hardening-2026-07` (short: **PQH**) |
| **Created** | 2026-07-23 |
| **Status** | **In Progress** (Leaf 1 active — charter + F8) |
| **Formal phase** | **None** (NON-CE program; not a P-phase — same pattern as SYS-NORM residuals) |
| **Sole-active leaf** | TM **#159**+**#160** · `pqh-f8-format-date-tz` (**In Progress**) |
| **Placement** | **ISOLATED** · worktree `D:/working/DGE-pqh-f8-format-date-tz` · branch `feat/pqh-f8-format-date-tz` |
| **Batch (Leaf 1)** | **merge** · `member_task_ids: ["159","160"]` · `proposed_slice_id: pqh-f8-format-date-tz` |
| **Behavior SoT (F8)** | [pqh-f8-format-date-tz.md](../behavior/pqh-f8-format-date-tz.md) (**ready**; **BDD-PQH-F8-001…012**) |
| **Detail plan** | [detail/pqh-f8-format-date-tz.md](./detail/pqh-f8-format-date-tz.md) |
| **Upstream** | SYS-NORM Waves **0–8 Done** (do **not** reopen); IBL F8/Q2 residual; CE umbrella **#53** registry-only |

---

## 1. North star

Close post-queue platform honesty and UX residuals **after** SYS-NORM Waves 0–8 and
post-SYS-NORM N18+L1 — under a dedicated NON-CE serial program with an explicit
**user delivery-order priority lock** (overrides parent UX-first recommendation).

**This leaf (Leaf 1):** program charter docs + honest `FORMAT_DATE` timezone / as-of
semantics (IBL **F8** / **Q2**).

---

## 2. Priority lock (user delivery order — authoritative)

> This table is the **user override**. Do not reorder to parent UX-first defaults
> without explicit user confirmation.

| Priority | Theme | Items | Status |
| --- | --- | --- | --- |
| **1** | Charter / program | **PQH-CHARTER** — this program plan + plan/ledger/TM registration | **In Progress** (TM **#159**, this leaf) |
| **2** | Platform quality | **PQH-F8** — `FORMAT_DATE` optional IANA `zoneId` + documented UTC unary default; date-only as-of | **In Progress** (TM **#160**, this leaf) |
| **3** | UX polish | **N19–N20** then **N22** (SYS-NORM residuals; NON-CE) | **Queued** — Leaf 2 then Leaf 3 (TM **#161**, **#162** `pending`) |
| **4** | Go-live Word / fonts | **#119** / checklist **#3b** / **#5a** | **Blocked** backlog — do **not** activate without Word host |
| **5** | Rendering fidelity | Seal writer / ADR-0043 slice B | **Backlog** after F8 unless user reorders |

---

## 3. Serial queue

| Leaf | Slice id | TM members | Status | Focus |
| --- | --- | --- | --- | --- |
| **Leaf 1** | `pqh-f8-format-date-tz` | **#159** PQH-CHARTER + **#160** PQH-F8 | **In Progress** (sole-active) | Charter docs + FORMAT_DATE TZ/as-of |
| **Leaf 2** | *(TBD at activate)* | **#161** N19–N20 | **pending** (queued; not sole-active) | EntityLink where-used + MasterImpact |
| **Leaf 3** | *(TBD at activate)* | **#162** N22 | **pending** (queued; not sole-active) | Catalog row action pattern |

**Parked (not in serial activate queue):**

| Item | TM | Status |
| --- | --- | --- |
| **F7** Bucket4j → Redis / coordinated rate-limit (IBL Q1 / ADR-0039 residual) | **#163** | **pending** — **parked** backlog only; do **not** activate in Leaf 1–3 |

---

## 4. Vetoes (hard)

| Veto | Rule |
| --- | --- |
| checklist-#3b/#5a-GO | Never flip **#3b** / **#5a** to GO from this program |
| CE-O02 | Never activate CE-O02 |
| mark-#53-CE-Done | Never mark umbrella **#53** Done |
| activate-#119-Word-host | Never activate **#119** without licensed Word host |
| do-not-claim-IBL-CE-go-live-Done | Never claim IBL / CE / go-live Done |
| F7-parked-not-in-leaf | F7 stays parked — not in Leaf 1 |
| f8-vs-n19n22-unrelated-domains | Do not merge F8 code leaf with N19–N22 FE leaves |
| reopen-SYS-NORM-waves | Do **not** reopen SYS-NORM Waves **0–8** as In Progress |

---

## 5. Task Master map

| Alias | TM | Status | Notes |
| --- | --- | --- | --- |
| **PQH-CHARTER** | **#159** | **in-progress** | Docs/governance — this program |
| **PQH-F8** | **#160** | **in-progress** | FORMAT_DATE semantics — BDD ready |
| **N19–N20** | **#161** | **pending** | Queued Leaf 2; not sole-active |
| **N22** | **#162** | **pending** | Queued Leaf 3; not sole-active |
| **PQH-F7** (parked) | **#163** | **pending** | Parked backlog; not activated |

**Sole-active statement:** Host delivery sole-active = TM **#159**+**#160** only
(`pqh-f8-format-date-tz`). Prior N18 leaf **#157**+**#158** remains **Done**
(`a4f59c4d` / `b54281b1`) — stale sole-active claims cleared. **#161**/**#162**/**#163**
are **not** sole-active. Umbrella **#53** stays **in-progress** registry-only.

---

## 6. Formal phase invariant

**Formal phase remains None.** PQH is a NON-CE program registry (like SYS-NORM residuals),
not a sole-active P-phase. Do not invent `In Progress` rows on P0–P23 for this work.

---

## 7. Relation to other programs

| Program | Relation |
| --- | --- |
| [SYS-NORM](./system-normalization-program-2026-07.md) | Waves 0–8 **Done**; N19–N20 / N22 residuals **owned here** for queue — do not reopen waves |
| [IBL](./intl-bank-letter-readiness-program.md) | F8/Q2 scheduled as **PQH-F8**; F7/Q1 parked as **#163**; do not claim IBL Done |
| [CE](./core-excellence-program-2026-07.md) | Umbrella **#53** registry-only; do not fold PQH into CE |
| LRP / ADR-0043 | Seal writer / Path X residuals stay backlog priority 5 |

---

## 8. Exit (program)

Program may move to Done only when Leaf 1–3 are Done (or explicitly cancelled) **and**
user confirms parked items remain parked or are separately scheduled — still without
claiming IBL/CE/go-live Done or flipping checklist GO.
