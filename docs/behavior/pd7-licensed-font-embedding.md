# pd7-licensed-font-embedding — licensed font pursue (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `pd7-licensed-font-embedding` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-20 |
| **Program leaf** | **B2 / PD-7** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) §8 PD Confirmation (**OUT of IBL-E / Wave E**) |
| **Product confirmation** | **Confirmed as pursue licensed font embedding** **2026-07-19** — procurement / LRP pairing |
| **Task Master** | **#139** `in-progress` (**sole-active**) — **not** an IBL-E task |
| **ADR** | [ADR-0069 Accepted](../adr/rendering-authoring/0069-licensed-font-embedding-pursue.md) — pursue path + procurement gate (**Accepted ≠ embedding Done**) |
| **Procurement** | [licensed-font-embedding-procurement.md](../operations/licensed-font-embedding-procurement.md) — license/delivery gates **Pending** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done / go-live) |
| **Placement** | **ISOLATED** — `D:/working/DGE-pd7-licensed-font-embedding` · `feat/pd7-licensed-font-embedding` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |

---

## Why BDD is not-applicable

This leaf is a **docs / procurement / ADR pursue** path — not a user-observable API or runtime journey:

- No new actor journey, management UI surface, OpenAPI contract, permission rule, or audit semantics in the honest shippable scope.
- Shipped production font baseline remains **[ADR-0041 Accepted](../adr/rendering-authoring/0041-rendering-font-baseline.md)** — Debian jammy CJK + Carlito/Caladea **metric-compatible substitutes**, explicitly **not** licensed Microsoft Calibri/Cambria.
- Confirmed product intent is to **pursue** licensed embedding (true Calibri etc.) via **procurement + LRP pairing**, not to invent or commit font binaries in-repo this leaf.
- Authoring Given/When/Then for “embed Calibri when pack present” would invent a runtime surface this leaf does **not** own unless a later leaf activates fail-closed hooks with licensed assets in hand.

Analogous readiness: [ibl-b6-repro-freeze.md](./ibl-b6-repro-freeze.md) / [ibl-b3-verapdf-pdfa-gate.md](./ibl-b3-verapdf-pdfa-gate.md) — docs / governance slices with `bdd_readiness: not-applicable`.

Program authority: IBL §8 **PD-7** — **Confirmed pursue**; **OUT of Wave E leaf set**.

---

## What is in scope (docs / procurement / ADR only)

| Deliverable | Intent |
| --- | --- |
| **ADR amendment or successor** | Record pursue path: licensed pack procurement prerequisite; ADR-0041 remains the **shipped** baseline until licensed assets exist under approved policy |
| **Procurement checklist** | Concrete license / vendor / redistribution constraints for embedding true Calibri (etc.) in conversion images — pairing with LRP font / launch checklist items |
| **Honest cannot-ship statement** | Document that **licensed font embedding cannot ship** without procured licensed assets; no fake fonts, no invented binaries in git |
| **LRP pairing (no GO flip)** | Cross-link LRP / [launch-readiness-checklist.md](../operations/launch-readiness-checklist.md) residuals; **do not** flip checklist **#3b GO** / **#5a GO** |
| **Index** | Reachable from `docs/README.md` (+ ADR/ops as chosen by doc-keeper) |
| **Gates** | Docs review (architecture-reviewer as needed) — no product UI journey; FE/E2E/UIUX **N/A** |

### Optional later (OUT of this leaf’s BDD / Done bar)

Fail-closed **code hooks/stubs** that refuse a licensed-embedding mode when the licensed pack is absent may be scheduled in a **follow-on leaf**. That leaf must re-run `behavior-spec-author` with `bdd_readiness: ready` and Given/When/Then — **not** implied Done by this docs pursue leaf.

---

## Explicit non-goals

- **No** inventing or committing font binaries (`.ttf` / `.otf` / vendor packs) into the repository.
- **No** claiming true Calibri/Cambria embedding is already shipped; ADR-0041 substitutes remain authoritative for Docker PDF.
- **No** flipping LRP / launch checklist **#3b GO** / **#5a GO**.
- **No** new product UI/API/permission journey; **no** Playwright E2E / UIUX.
- **OUT of IBL-E** — Wave E stays **Done** without a PD-7 IBL-E task.
- **Do not** claim go-live or mark IBL program **Done**.
- Formal phase remains **None**.

---

## Acceptance (stage Done for this leaf)

Leaf Done when **all** of the following hold (docs/governance evidence — not G/W/T product tests):

1. ADR amendment **or** successor ADR is authored, indexed, and reviewable against ADR-0041 honesty (substitutes ≠ licensed Calibri).
2. Procurement checklist exists (ops or plan-linked) covering license acquisition, redistribution into conversion images, and pairing with LRP font baseline / checklist residuals.
3. Explicit durable statement: **cannot ship licensed font embedding without licensed assets** (no fake fonts).
4. LRP pairing documented; checklist **#3b/#5a** remain **not GO** unless separately evidenced elsewhere.
5. This behavior note + `docs/README.md` index updated; post-task doc-sync / commit-review on MAIN after merge.

---

## Traceability

| Artifact | Role |
| --- | --- |
| IBL §8 **PD-7** | Product confirmation — pursue licensed embedding; OUT of Wave E; pursue docs leaf **Done** (#139 `b966874a`; **Accepted ≠ embedding Done**) |
| [ADR-0041](../adr/rendering-authoring/0041-rendering-font-baseline.md) | Shipped font baseline (Carlito/Caladea + CJK) — not licensed Calibri |
| [ADR-0069](../adr/rendering-authoring/0069-licensed-font-embedding-pursue.md) | Pursue path + procurement gate (**Accepted ≠ embedding Done**) |
| [ADR-0060](../adr/rendering-authoring/0060-legal-reproducibility-freeze.md) | Legal freeze still reaffirms ADR-0041 fonts until post-procurement re-cut |
| [licensed-font-embedding-procurement.md](../operations/licensed-font-embedding-procurement.md) | Procurement checklist — pending vs confirmed separated |
| LRP LR-A2 / LR-A5 / checklist font items | Pairing surface; no **#3b/#5a GO** flip from this leaf |
| Slice `pd7-licensed-font-embedding` | Isolated worktree delivery leaf — **Done** (`b966874a`; sole-active cleared) |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/pd7-licensed-font-embedding.md
task_ids: ["139"]
frontend_ui_in_scope: false
open_questions: []
```
