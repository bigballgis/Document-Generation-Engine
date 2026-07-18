# ibl-c1-layout-metric-pdf — layout-metric PDF regression (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-c1-layout-metric-pdf` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-C1** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-C |
| **Finding** | **F17** (Golden corpus = DOCX XML/XPath + PDF text-extract only; `PIXEL_*` rejected; zero baseline PDF binaries; no layout regression) |
| **Task Master** | **#120** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Placement** | **ISOLATED** — `D:/working/DGE-ibl-c1-layout-metric-pdf` · `feat/ibl-c1-layout-metric-pdf` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |

---

## Why BDD is not-applicable

This leaf is a **golden/CI layout-metric regression** harness (PDFBox page count + text-position assertions) closing **F17** — **not** a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Outcomes are **CI / `mvn verify` assertions** on rendered PDF geometry metrics (page count + key text-position via PDFBox), not a product Given/When/Then for an end-user “compares layout” flow.
- Product Given/When/Then for a side-by-side compare UI belongs to **IBL-C2** (BDD **required**, frontend E2E mandatory) — **out of this leaf**.
- Cross-locale golden matrix / SYNTHETIC→LIBREOFFICE upgrades belong to **IBL-C3** — **out of this leaf**.

Program authority: IBL Wave C row **IBL-C1** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-C1**.

Freeze pointer (baselines under B6): [ADR-0060](../adr/rendering-authoring/0060-legal-reproducibility-freeze.md) + [legal-reproducibility-freeze.md](../operations/legal-reproducibility-freeze.md) — baselines checked in or generated under that freeze; **do not invent Word baselines**.

Analogous readiness: [ibl-b3-verapdf-pdfa-gate.md](./ibl-b3-verapdf-pdfa-gate.md) / [ibl-b6-repro-freeze.md](./ibl-b6-repro-freeze.md) — tooling / evidence / freeze slices with `bdd_readiness: not-applicable`.

---

## What is in scope (golden / CI / verify only)

| Deliverable | Intent |
| --- | --- |
| **PDFBox page-count assertions** | Golden/CI covers page count on agreed corpus PDFs |
| **PDFBox text-position assertions** | Golden/CI covers key text-position metrics (non-pixel) |
| **Anti-pixel stance** | Continue to **reject** `PIXEL_*` assertion kinds unless **PD-2** pixel/visual PDF regression ADR is **Accepted** |
| **Baselines** | Checked in or generated under **IBL-B6** legal-reproducibility freeze (ADR-0060 / ops freeze) |
| **Gates** | Backend `mvn verify` green with layout-metric regression wired |

---

## Explicit non-goals

- **No** new product UI/API/permission/audit journey; **no** Playwright E2E / UIUX for this leaf (`frontend_ui_in_scope=false`).
- **Reject `PIXEL_*`** unless §Pending **PD-2** (pixel / visual PDF regression) ADR is **Accepted** — do **not** silently enable pixel/visual compare.
- **No Word baselines invented** — no Path E Word-vs-LO page deltas; Word host measurement remains **IBL-B7** / **PD-3**.
- **OUT:** **IBL-C2** (side-by-side rendered compare UI — BDD required), **IBL-C3** (cross-locale golden matrix), **IBL-B7** (Word Path E — Blocked).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live or mark Wave IBL-C / IBL program **Done**.
- Formal phase remains **None**.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#120** | Owning leaf IBL-C1 |
| IBL program **F17** / **IBL-C1** acceptance | Gap + Done criteria (layout-metric golden/CI; anti-pixel) |
| **PD-2** | Gates any future pixel mode — requires new ADR; otherwise `PIXEL_*` rejected |
| **IBL-B6** freeze (ADR-0060 + ops) | Baseline generation / check-in authority |
| Batch / queue | Solo leaf; veto **C2** / **C3** / **B7** |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-c1-layout-metric-pdf.md
task_ids: ["120"]
frontend_ui_in_scope: false
```
