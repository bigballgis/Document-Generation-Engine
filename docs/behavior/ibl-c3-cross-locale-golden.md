# ibl-c3-cross-locale-golden — cross-locale golden matrix (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-c3-cross-locale-golden` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-C3** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-C |
| **Finding** | **F19** (closed — cross-locale matrix + honest SYNTHETIC/LIBREOFFICE labels) |
| **Task Master** | **#122** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Status** | **Done** (MAIN merge `bdfc285d` / feature tip `dbfff086`; worktree removed) |
| **Placement** | Merged to MAIN — worktree removed |
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

---

## Delivered (Done)

| Deliverable | Result |
| --- | --- |
| **en/zh themes** | `english-locale-letter` (`en-US`) + existing `chinese-uppercase-amount` (`zh-CN`) |
| **Multi-currency themes** | `multi-currency-amount` (EUR / USD / CNY via binary `FORMAT_AMOUNT`) |
| **LIBREOFFICE honesty** | New themes labeled **SYNTHETIC** (PDFBox projection); existing LIBREOFFICE packages **SKIP** PDF half when soffice unavailable |
| **No invented LO PDFs** | Host soffice **absent** — no LO-attributed binaries invented under `expected/` |
| **LO residual** | Mandatory LO CI / soffice-produced PDF upgrades → **IBL-D2** / **F21** (out of this leaf) |
| **Gates** | `mvn verify` **GREEN 2133**/0/11; arch **PASS_WITH_NOTES** `merge_go=true`; Stage 10 ForceRebuild **DEPLOY_OK**; FE/E2E **N/A** |
| **Evidence** | [ibl-c3-cross-locale-golden/](../plan/evidence/ibl-c3-cross-locale-golden/) + [ibl-c3-stage10-deploy/](../plan/evidence/ibl-c3-stage10-deploy/) |

**Wave IBL-C → Done** (C1+C2+C3). Do **not** claim all goldens are now LIBREOFFICE-produced. Do **not** claim Wave B / IBL program Done.

---

## Explicit non-goals (OUT)

- **No** product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **No pixel** baselines / `PIXEL_*` assertions.
- **No Word** baselines / Path E Word-vs-LO (remains **IBL-B7**).
- **IBL-D2** mandatory CI LO lane is **out of this leaf**.
- Formal phase remains **None**.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#122** | Owning leaf IBL-C3 → **done** |
| IBL program **F19** / **IBL-C3** acceptance | Gap + Done criteria (cross-locale matrix + LO label honesty) |
| Batch / queue | Solo leaf closed; next **#123** IBL-D1 (not activated) |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-c3-cross-locale-golden.md
task_ids: ["122"]
frontend_ui_in_scope: false
status: Done
```
