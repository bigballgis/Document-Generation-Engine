# ibl-d2-lo-mandatory-lane — CI LibreOffice mandatory lane (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d2-lo-mandatory-lane` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-D2** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-D |
| **Finding** | **F21** (LibreOffice-dependent tests skip silently when `soffice` missing — green without conversion) |
| **Task Master** | **#124** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Delivery status** | **Done** (MAIN merge `21be3a99` / feature tip `4fd0c5da`; worktree removed; F21 closed) |
| **Placement** | Merged to MAIN (was ISOLATED `D:/working/DGE-ibl-d2-lo-mandatory-lane` · `feat/ibl-d2-lo-mandatory-lane`) |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |
| **Batch** | **solo** (`member_task_ids: ["124"]`; vetoes **IBL-D3-k6**, **IBL-D4-chaos**, **IBL-D5-legalhold**, **IBL-B7-Word**) |

---

## Why BDD is not-applicable

This leaf is **CI / test infrastructure** closing **F21** — **not** a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Outcomes are a **documented mandatory LibreOffice CI profile/lane** where LO-dependent tests **fail** (not skip) when `soffice` is absent, plus docs that keep **optional local skip** honest for developer machines without LibreOffice.
- Product Given/When/Then for bank-letter filling, rendering, or compare UI belong to other IBL waves — **out of this leaf**.
- Default local/dev `mvn verify` **may continue to skip** LO-dependent tests when `soffice` is missing — that optional skip remains documented; the **mandatory CI lane** must not stay green by silent skip.

Program authority: IBL Wave D row **IBL-D2** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-D2**.

---

## Acceptance (delivery / CI — not product G/W/T)

These are **engineering acceptance** criteria for the slice — not product BDD scenarios for TDD Red of new user journeys.

1. **Given** the mandatory LibreOffice CI profile/lane is selected  
   **And** `soffice` is **absent** (or not discoverable on PATH)  
   **When** LO-dependent tests run under that profile  
   **Then** those tests **FAIL** (or the lane fails) — they **must not** be reported as skipped/success that hides missing conversion.

2. **Given** a developer machine **without** LibreOffice / `soffice`  
   **When** default local verify runs **without** the mandatory LO CI profile  
   **Then** optional skip of LO-dependent tests **remains allowed** and is **documented** (why skip is OK locally vs fail-closed on the CI lane).

3. **Given** the mandatory LO CI profile with `soffice` **present**  
   **When** the lane executes  
   **Then** LO-dependent coverage (font smoke / parallel conversion IT / LIBREOFFICE golden halves as wired by this leaf) exercises real conversion rather than silent skip-as-green.

4. **Given** the leaf claims Done  
   **When** reviewers inspect evidence  
   **Then** **CI evidence** for the mandatory LO lane is recorded (job/profile name, pass or intentional fail-on-absent proof, and any local-skip doc pointer).

---

## Explicit non-goals (OUT)

- **No** product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **Do not invent** LibreOffice-attributed golden PDF binaries when `soffice` is absent (honesty residual from IBL-C3 / F19 stays honest).
- **No** Word / pixel baselines; **IBL-B7** remains Blocked / out of scope (batch veto **IBL-B7-Word**).
- **IBL-D3** k6 / NFR SLO confirmation, **IBL-D4** LO chaos, **IBL-D5** legalhold depth — **out of this leaf** (batch vetoes applied).
- Formal phase remains **None**. Do **not** mark **Wave D Done** from this leaf alone (D3–D5 remain open after D2). Do **not** claim IBL program Done.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#124** | Owning leaf IBL-D2 |
| IBL program **F21** / **IBL-D2** acceptance | Gap + Done criteria (mandatory LO CI fail-not-skip; optional local skip docs; CI evidence) |
| Prior **IBL-C3** / **F19** | Honesty residual: LO PDF upgrade / mandatory LO CI → this leaf |
| Prior **IBL-D1** / **F20** | Sibling Wave D infra leaf (TC+Flyway) — already Done; orthogonal |
| Batch / queue | Solo leaf; vetoes **IBL-D3-k6**, **IBL-D4-chaos**, **IBL-D5-legalhold**, **IBL-B7-Word** |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-d2-lo-mandatory-lane.md
task_ids: ["124"]
frontend_ui_in_scope: false
```
