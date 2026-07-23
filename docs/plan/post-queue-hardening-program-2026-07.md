# Post-Queue Hardening Program (PQH) — 2026-07

| Field | Value |
| --- | --- |
| **Program ID** | `post-queue-hardening-2026-07` (short: **PQH**) |
| **Created** | 2026-07-23 |
| **Status** | **In Progress** (Leaf 1 **Done**; Leaf 2 **#161** N19–N20 **Done**; Leaf 3 **#162** N22 **Done**; Leaf 4 **#163** F7 **In Progress**) |
| **Formal phase** | **None** (NON-CE program; not a P-phase — same pattern as SYS-NORM residuals) |
| **Sole-active leaf** | **#163** / `pqh-f7-redis-rate-limit` — **ACTIVE Leaf 4** (F7 unparked) |
| **Leaf 1 closed** | TM **#159**+**#160** · `pqh-f8-format-date-tz` → **Done** (MAIN tip `ab382c02`; feature `ee0893fe`; worktree **REMOVED**) |
| **Leaf 2 closed** | TM **#161** · `pqh-n19-n20-entitylink` → **Done** (MAIN tip `20c67ac9`; feature `1e023a35`; worktree **REMOVED**) |
| **Leaf 3 closed** | TM **#162** · `pqh-n22-catalog-row-actions` → **Done** (MAIN tip/merge `ef1b505d`; Stage 10 tip `c5121164`; worktree **REMOVED**) |
| **Leaf 4 active** | TM **#163** · `pqh-f7-redis-rate-limit` → **In Progress** (worktree `D:/working/DGE-pqh-f7-redis-rate-limit` · `feat/pqh-f7-redis-rate-limit`; base `850b51c9`) |
| **Batch (Leaf 1)** | **merge** · `member_task_ids: ["159","160"]` · `proposed_slice_id: pqh-f8-format-date-tz` — **closed** |
| **Batch (Leaf 2)** | **solo** · `member_task_ids: ["161"]` · `proposed_slice_id: pqh-n19-n20-entitylink` — **closed** |
| **Batch (Leaf 3)** | **solo** · `member_task_ids: ["162"]` · `proposed_slice_id: pqh-n22-catalog-row-actions` — **closed** |
| **Batch (Leaf 4)** | **solo** · `member_task_ids: ["163"]` · `proposed_slice_id: pqh-f7-redis-rate-limit` — **active** |
| **Behavior SoT (F8)** | [pqh-f8-format-date-tz.md](../behavior/pqh-f8-format-date-tz.md) (**ready**/shipped; **BDD-PQH-F8-001…012**) |
| **Behavior SoT (N19–N20)** | [pqh-n19-n20-entitylink.md](../behavior/pqh-n19-n20-entitylink.md) (**ready**/shipped; **BDD-PQH-N19N20-001…014**) |
| **Behavior SoT (N22)** | [pqh-n22-catalog-row-actions.md](../behavior/pqh-n22-catalog-row-actions.md) (**ready**/shipped; **BDD-PQH-N22-001…014**) |
| **Behavior SoT (F7)** | [pqh-f7-redis-rate-limit.md](../behavior/pqh-f7-redis-rate-limit.md) (**ready**; **BDD-PQH-F7-001…012**) |
| **Detail plan (Leaf 1)** | [detail/pqh-f8-format-date-tz.md](./detail/pqh-f8-format-date-tz.md) |
| **Detail plan (Leaf 2)** | [detail/pqh-n19-n20-entitylink.md](./detail/pqh-n19-n20-entitylink.md) |
| **Detail plan (Leaf 3)** | [detail/pqh-n22-catalog-row-actions.md](./detail/pqh-n22-catalog-row-actions.md) |
| **Detail plan (Leaf 4)** | [detail/pqh-f7-redis-rate-limit.md](./detail/pqh-f7-redis-rate-limit.md) |
| **Next queue head** | **empty** — Leaf 4 **#163** is **sole-active** (do not queue another delivery leaf) |
| **Upstream** | SYS-NORM Waves **0–8 Done** (do **not** reopen); IBL F8/Q2 residual **Done under PQH**; N19–N20 residual **Done under PQH**; N22 residual **Done under PQH**; IBL F7/Q1 **activated under PQH Leaf 4** (not IBL wave reopen); CE umbrella **#53** registry-only |

---

## 1. North star

Close post-queue platform honesty and UX residuals **after** SYS-NORM Waves 0–8 and
post-SYS-NORM N18+L1 — under a dedicated NON-CE serial program with an explicit
**user delivery-order priority lock** (overrides parent UX-first recommendation).

**Leaf 1 (closed):** program charter docs + honest `FORMAT_DATE` timezone / as-of
semantics (IBL **F8** / **Q2**) → **Done**.

**Leaf 2 (closed):** EntityLink where-used `groupCode` + MasterImpact fail-closed
(SYS-NORM **N19–N20**) → **Done**.

**Leaf 3 (closed):** Catalog row action pattern Edit/More via `TableEditMoreActions`
(SYS-NORM **N22**) — Asset Library + Legal Holds + API Invocations (+ Users/Groups
regression) → **Done** (`ef1b505d`).

**Leaf 4 (active):** Bucket4j → Redis / coordinated **runtime** rate-limit (IBL **F7** /
**Q1**; ADR-0044 #3) → **In Progress** (`pqh-f7-redis-rate-limit`).

---

## 2. Priority lock (user delivery order — authoritative)

> This table is the **user override**. Do not reorder to parent UX-first defaults
> without explicit user confirmation.

| Priority | Theme | Items | Status |
| --- | --- | --- | --- |
| **1** | Charter / program | **PQH-CHARTER** — this program plan + plan/ledger/TM registration | **Done** (TM **#159**) |
| **2** | Platform quality | **PQH-F8** — `FORMAT_DATE` optional IANA `zoneId` + documented UTC unary default; date-only as-of | **Done** (TM **#160**) |
| **3** | UX polish | **N19–N20** then **N22** (SYS-NORM residuals; NON-CE) | **Done** — Leaf 2 TM **#161** **Done**; Leaf 3 TM **#162** **Done** (`ef1b505d`) |
| **4** | Runtime scale-out honesty | **PQH-F7** — Redis / coordinated runtime rate-limit (IBL F7/Q1) | **In Progress** (TM **#163** Leaf 4 sole-active) |
| **5** | Go-live Word / fonts | **#119** / checklist **#3b** / **#5a** | **Blocked** backlog — do **not** activate without Word host |
| **6** | Rendering fidelity | Seal writer / ADR-0043 slice B | **Backlog** unless user reorders |

---

## 3. Serial queue

| Leaf | Slice id | TM members | Status | Focus |
| --- | --- | --- | --- | --- |
| **Leaf 1** | `pqh-f8-format-date-tz` | **#159** PQH-CHARTER + **#160** PQH-F8 | **Done** | Charter docs + FORMAT_DATE TZ/as-of |
| **Leaf 2** | `pqh-n19-n20-entitylink` | **#161** N19–N20 | **Done** | EntityLink where-used + MasterImpact |
| **Leaf 3** | `pqh-n22-catalog-row-actions` | **#162** N22 | **Done** | Catalog row action pattern |
| **Leaf 4** | `pqh-f7-redis-rate-limit` | **#163** PQH-F7 | **In Progress** (sole-active) | Redis / coordinated runtime rate-limit |

**Next queue:** **empty** while Leaf 4 is sole-active.

---

## 4. Vetoes (hard)

| Veto | Rule |
| --- | --- |
| checklist-#3b/#5a-GO | Never flip **#3b** / **#5a** to GO from this program |
| CE-O02 | Never activate CE-O02 |
| mark-#53-CE-Done | Never mark umbrella **#53** Done |
| activate-#119-Word-host | Never activate **#119** without licensed Word host |
| do-not-claim-IBL-CE-go-live-Done | Never claim IBL / CE / go-live Done |
| f8-vs-n19n22-unrelated-domains | Do not merge F8 code leaf with N19–N22 FE leaves |
| reopen-SYS-NORM-waves | Do **not** reopen SYS-NORM Waves **0–8** as In Progress |
| F7-was-parked-from-Leaf-1-3 | Historical: F7 was parked out of Leaf 1–3; **now** Leaf 4 — do not re-park without user direction |

---

## 5. Task Master map

| Alias | TM | Status | Notes |
| --- | --- | --- | --- |
| **PQH-CHARTER** | **#159** | **done** | Docs/governance — this program |
| **PQH-F8** | **#160** | **done** | FORMAT_DATE semantics — BDD shipped |
| **N19–N20** | **#161** | **done** | Leaf 2 closed (`pqh-n19-n20-entitylink`; MAIN `20c67ac9`) |
| **N22** | **#162** | **done** | Leaf 3 closed (`pqh-n22-catalog-row-actions`; MAIN `ef1b505d`) |
| **PQH-F7** | **#163** | **in-progress** | **ACTIVE Leaf 4** sole-active (`pqh-f7-redis-rate-limit`) |

**Sole-active statement:** Host delivery sole-active is TM **#163** / slice
`pqh-f7-redis-rate-limit` (PQH Leaf 4). Leaf 1 **#159**+**#160**, Leaf 2 **#161**,
and Leaf 3 **#162** remain **Done**. Umbrella **#53** stays **in-progress**
registry-only (not a delivery leaf).

---

## 6. Formal phase invariant

**Formal phase remains None.** PQH is a NON-CE program registry (like SYS-NORM residuals),
not a sole-active P-phase. Do not invent `In Progress` rows on P0–P23 for this work.

---

## 7. Relation to other programs

| Program | Relation |
| --- | --- |
| [SYS-NORM](./system-normalization-program-2026-07.md) | Waves 0–8 **Done**; N19–N20 residual **Done under PQH**; N22 residual **Done under PQH** — do not reopen waves |
| [IBL](./intl-bank-letter-readiness-program.md) | F8/Q2 **Done under PQH-F8** (#160); F7/Q1 **activated under PQH Leaf 4** (#163) — **not** an IBL wave reopen; do not claim IBL Done |
| [CE](./core-excellence-program-2026-07.md) | Umbrella **#53** registry-only; do not fold PQH into CE |
| LRP / ADR-0043 | Seal writer / Path X residuals stay backlog |

---

## 8. Exit (program)

Program may move to Done only when Leaf 1–4 are Done (or explicitly cancelled) **and**
user confirms remaining backlog disposition — still without claiming IBL/CE/go-live Done
or flipping checklist GO.

**Leaf 1–3 are Done** (2026-07-23). **Leaf 4 (#163) is In Progress** (unparked 2026-07-23).
Program stays **In Progress** until Leaf 4 closes (or user re-parks).
