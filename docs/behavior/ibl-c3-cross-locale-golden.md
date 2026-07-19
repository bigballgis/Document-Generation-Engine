# ibl-c3-cross-locale-golden — cross-locale golden matrix (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-c3-cross-locale-golden` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-C3** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-C |
| **Finding** | **F19** (Cross-locale/multi-script matrix incomplete; Chinese-amount theme only; several golden PDF halves SYNTHETIC) |
| **Task Master** | **#122** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Status** | **Not Started** (BDD stub only; implementation follows plan-orchestrator) |
| **Placement** | **ISOLATED** — `D:/working/DGE-ibl-c3-cross-locale-golden` · `feat/ibl-c3-cross-locale-golden` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |

---

## Why BDD is not-applicable

This leaf is **golden corpus / test-infra + docs honesty** closing **F19** — **not** a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Outcomes are **corpus theme coverage** (en/zh + multi-currency) and **honest PDF-half provenance labels** (LIBREOFFICE only when produced by LibreOffice; otherwise SKIP / honest SYNTHETIC), exercised under backend verify (and LO lane when IBL-D2 exists later).
- Product Given/When/Then for side-by-side rendered compare belongs to **IBL-C2** (already Done) — **out of this leaf**.
- Layout-metric PDFBox assertions belong to **IBL-C1** (already Done) — **out of this leaf**.

Program authority: IBL Wave C row **IBL-C3** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-C3**.

Analogous readiness: [ibl-c1-layout-metric-pdf.md](./ibl-c1-layout-metric-pdf.md) / [ibl-b6-repro-freeze.md](./ibl-b6-repro-freeze.md) — corpus / evidence / freeze slices with `bdd_readiness: not-applicable`.

---

## Acceptance sketch (from plan F19 / IBL-C3)

| Deliverable | Intent |
| --- | --- |
| **en/zh themes** | Golden corpus includes English and Chinese locale/script themes (not Chinese-amount-only) |
| **Multi-currency themes** | Corpus covers multi-currency letter themes beyond a single currency fixture |
| **LIBREOFFICE honesty** | PDF halves labeled **LIBREOFFICE** are produced by LibreOffice (`soffice`) — not silently SYNTHETIC |
| **No invented LO PDFs** | When `soffice` is absent: **SKIP** and/or keep **honest SYNTHETIC** labels — never invent LO-attributed PDFs |
| **Label correction** | If a half cannot be LO-produced, correct the label honestly (SYNTHETIC / SKIP) rather than claiming LIBREOFFICE |
| **Gates** | Backend `mvn verify` green; LO lane when **IBL-D2** exists (optional note — D2 is later; not mandatory CI for this leaf until D2 lands) |

---

## Explicit non-goals (OUT)

- **No** product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **Do not** claim Wave **IBL-C Done** without C3 closeout (this leaf closes C3; Wave Done only after C3 Done + program rules).
- **No pixel** baselines / `PIXEL_*` assertions (unless a future pixel ADR is Accepted — not this leaf).
- **No Word** baselines / Path E Word-vs-LO (remains **IBL-B7**).
- **IBL-D2** mandatory CI LO lane is **out of this leaf** (may note “when D2 exists” only; do not implement D2 here).
- Formal phase remains **None**.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#122** | Owning leaf IBL-C3 |
| IBL program **F19** / **IBL-C3** acceptance | Gap + Done criteria (cross-locale matrix + LO label honesty) |
| Batch / queue | Solo leaf; vetoes: IBL-D2-LO-CI-lane, IBL-B7-Word, umbrella-106, Wave-D-unrelated |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-c3-cross-locale-golden.md
task_ids: ["122"]
frontend_ui_in_scope: false
```
