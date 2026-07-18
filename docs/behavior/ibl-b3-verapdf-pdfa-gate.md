# ibl-b3-verapdf-pdfa-gate — veraPDF PDF/A verify gate (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-b3-verapdf-pdfa-gate` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-B3** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-B |
| **Finding** | **F12** (PDF/A verification is XMP/`pdfaid` metadata-only today) |
| **Task Master** | **#115** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Placement** | **MERGED** — MAIN `3710811a` (includes `c81054b0` + `e0102ddb`); worktree removed; prior ISOLATED `D:/working/DGE-ibl-b3-verapdf-pdfa-gate` · `feat/ibl-b3-verapdf-pdfa-gate` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |

---

## Why BDD is not-applicable

This leaf is a **CI / `mvn verify` tooling gate** for PDF/A artifacts, not a new user-facing product journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Does **not** redefine CE-O01 product behavior (`pdfArchivalProfile`, LibreOffice PDF/A-2b filter, encryption mutex) — that journey is already specified and delivered under [ce-o01-pdfa-output.md](./ce-o01-pdfa-output.md) + [ADR-0058](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md).
- Outcomes are **veraPDF (or company-approved equivalent) wired into verify/CI on PDF/A artifacts** so F12 is no longer metadata-only — plus dependency-policy / docs for the tool — not new confirmed product requirements.
- Product Given/When/Then for a “user validates PDF/A” journey would invent a UI/API surface this leaf does not own; regression remains on existing backend verify / CI lanes.

Program authority: IBL Wave B row **IBL-B3** — **BDD: `not-applicable`**.

Analogous readiness: [slim-knip-scan](./slim-knip-scan.md) / [fe-vitest-3-upgrade](./fe-vitest-3-upgrade.md) / [lrp-d6-load-smoke](./lrp-d6-load-smoke.md) — tooling / harness / evidence slices with `bdd_readiness: not-applicable`.

---

## Related product context (pointer only — do not invent new requirements)

| Artifact | Role for this leaf |
| --- | --- |
| [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § IBL-B3 acceptance | Delivery acceptance: verify/CI runs veraPDF (or approved equivalent) on PDF/A artifacts — not XMP-only; closes **F12** |
| [ce-o01-pdfa-output.md](./ce-o01-pdfa-output.md) | **Existing** product BDD for archival PDF output (CE-O01) — unchanged by this leaf |
| [ADR-0058](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md) | **Accepted** PDF/A-2b archival decision — unchanged; this leaf strengthens **verification evidence**, not the ADR decision text |
| [ADR-0059](../adr/rendering-authoring/0059-verapdf-pdfa-verify-gate.md) | **Accepted** veraPDF Greenfield verify-gate dependency + wiring (IBL-B3) |
| [verapdf-pdfa-verify-gate.md](../operations/verapdf-pdfa-verify-gate.md) | Ops: how to run / local skip / CI fail-not-skip |

---

## What is in scope (tooling / verify only)

| Deliverable | Intent |
| --- | --- |
| **veraPDF (or approved equivalent)** | Validate PDF/A artifacts in `mvn verify` and/or a dedicated CI profile — beyond `PdfAidXmpAssertor` / `pdfaid` XMP-only |
| **Dependency policy** | Company-approved artifact availability per tech-stack / dependency policy before pin |
| **Docs** | Index + ops/dependency notes as needed for how the gate runs |
| **Gates** | Backend verify / CI lane green with the validator wired |

---

## Explicit non-goals

- **No** new product UI/API/permission/audit journey; **no** Playwright E2E / UIUX for this leaf.
- **Do not** invent new CE-O01 / ADR-0058 product requirements or amend Accepted ADR decision text as “progress.”
- **OUT:** **IBL-B4** (long-clause overflow), **IBL-B7** (Word Path E / checklist #3b).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live or mark Wave IBL-B / IBL program **Done**.
- Formal phase remains **None**.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#115** | Owning leaf IBL-B3 |
| IBL program F12 / IBL-B3 row | Gap + acceptance (tooling gate) |
| CE-O01 / ADR-0058 | Related product archival context (pointer only) |
| Batch / queue | Solo leaf; veto B4 / B7 |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-b3-verapdf-pdfa-gate.md
task_ids: ["115"]
frontend_ui_in_scope: false
```
