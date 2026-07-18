# ibl-b6-repro-freeze — legal-reproducibility freeze docs (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-b6-repro-freeze` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-B6** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-B |
| **Finding** | **F16** (no deterministic legal-reproducibility freeze: LO version + font set + content-hash PDF baselines) |
| **Task Master** | **#118** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Placement** | **ISOLATED** — `D:/working/DGE-ibl-b6-repro-freeze` · `feat/ibl-b6-repro-freeze` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |

---

## Why BDD is not-applicable

This leaf is **ops / ADR freeze documentation** for legal reproducibility — not a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Deliverable is an **ADR and/or ops freeze doc** that records LibreOffice version, font set, and **content-hash baseline procedure**, then indexes it from docs — closing **F16** as a documentation/governance gap.
- Product Given/When/Then for a “user freezes reproducibility” journey would invent a UI/API surface this leaf does not own.
- **Do not invent Word baselines** or Path E measurement numbers here — that belongs to **IBL-B7** (Blocked on licensed MS Word host).

Program authority: IBL Wave B row **IBL-B6** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-B6**.

Analogous readiness: [ibl-b3-verapdf-pdfa-gate.md](./ibl-b3-verapdf-pdfa-gate.md) / [lrp-d2-backup-restore.md](./lrp-d2-backup-restore.md) — docs / ops / tooling slices with `bdd_readiness: not-applicable`.

---

## What is in scope (docs / ADR only)

| Deliverable | Intent |
| --- | --- |
| **LO version pin** | Record the LibreOffice version used for deterministic PDF conversion / golden path |
| **Font set** | Record the font set required for reproducible layout |
| **Content-hash baseline procedure** | Document how content-hash PDF baselines are produced, stored, and compared |
| **Index** | Reachable from `docs/README.md` (+ ops/ADR as chosen by doc-keeper) |
| **Gates** | Docs review (architecture-reviewer as needed) — no product UI journey |

### Authored freeze (2026-07-19 — #118 **Done**)

| Artifact | Status |
| --- | --- |
| [ADR-0060](../adr/rendering-authoring/0060-legal-reproducibility-freeze.md) | **Accepted** — LO record-at-cut, ADR-0041 font reaffirm, SHA-256 content-hash procedure |
| [legal-reproducibility-freeze.md](../operations/legal-reproducibility-freeze.md) | Ops runbook |
| Indexes | `docs/README.md`, ADR index, golden-corpus README, domain-model cross-link |

Leaf **#118** → **Done** (MAIN merge `8722f4f1` / `8e8c62e6`; arch **PASS_WITH_NOTES** `merge_go=true`; docs-only; deploy/FE/E2E N/A). Wave **IBL-B** stays **In Progress** (B1–B6 Done; B7 Blocked). Sole-active cleared. Next queue (not activated): **#120** IBL-C1.

---

## Explicit non-goals

- **No** new product UI/API/permission/audit journey; **no** Playwright E2E / UIUX for this leaf.
- **No Word baselines invented** — no Path E Word-vs-LO page deltas, no invented measurement numbers.
- **OUT:** **IBL-B7** (Word Path E / checklist #3b GO path — remains Blocked until host exists).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live or mark Wave IBL-B / IBL program **Done**.
- Formal phase remains **None**.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#118** | Owning leaf IBL-B6 |
| IBL program **F16** / **IBL-B6** acceptance | Gap + Done criteria (freeze doc/ADR) |
| Batch / queue | Solo leaf; veto **B7** |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-b6-repro-freeze.md
task_ids: ["118"]
frontend_ui_in_scope: false
```
