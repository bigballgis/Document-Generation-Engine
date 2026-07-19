# ibl-d3-k6-nfr-path — k6 load suite + NFR confirmation path (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d3-k6-nfr-path` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-D3** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-D |
| **Finding** | **F22** (no industry load tool; NFR SLOs still proposed — no confirmation path) |
| **Task Master** | **#125** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Delivery status** | **Not Started** → Stage 1 BDD gate only (implementation follows) |
| **Placement** | ISOLATED `D:/working/DGE-ibl-d3-k6-nfr-path` · `feat/ibl-d3-k6-nfr-path` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |
| **Batch** | **solo** (`member_task_ids: ["125"]`; vetoes **IBL-D4-chaos**, **IBL-D5-legalhold**, **IBL-B7-Word**) |

---

## Why BDD is not-applicable

This leaf is **load-suite + NFR confirmation-path infrastructure** closing the tooling half of **F22** — **not** a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Outcomes are a **checked-in k6 (or company-approved) load suite** plus a documented path that **feeds measured results into** [NFR §待确认 / LR-D5](../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation) — values remain **proposed / awaiting confirmation**.
- **No confirmed SLO is invented or promoted** in this leaf. User confirmation of any NFR number is a **separate** governance act (explicit user/PRD confirmation), not an implementer side-effect of green scripts.
- Product Given/When/Then for bank-letter filling, rendering, or compare UI belong to other IBL waves — **out of this leaf**.
- Prior measurement harness (LR-D6 / IBL-B2 capacity evidence) remains historical **measured-input**; this leaf adds an industry (or company-approved) suite + confirmation-path docs/evidence wiring — it does **not** re-open or invent SLOs from those smokes.

Program authority: IBL Wave D row **IBL-D3** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-D3**.

---

## Acceptance (delivery / infra — not product G/W/T)

These are **engineering acceptance** criteria for the slice — not product BDD scenarios for TDD Red of new user journeys.

1. **Given** the repository after this leaf  
   **When** reviewers inspect the load tooling  
   **Then** a **k6** suite (or another **company-approved** load tool under dependency-policy / ADR if required) is **checked in** and runnable via a documented scripted command.

2. **Given** a scripted load run against the allowed acceptance environment (local Docker stack — never shared/production)  
   **When** results are recorded  
   **Then** evidence is stored under the leaf evidence path and **feeds** NFR §待确认 (LR-D5) as **measured-input / proposed** material — **not** as confirmed SLOs.

3. **Given** any latency / error-rate / concurrency numbers appear in docs or evidence from this leaf  
   **When** reviewers check NFR status vocabulary  
   **Then** those numbers remain **proposed — awaiting confirmation** (or equivalent pending status); **no** row is flipped to a **confirmed** SLO without a **separate** explicit user confirmation record.

4. **Given** the leaf claims Done  
   **When** reviewers inspect gates  
   **Then** **scripted run evidence** exists (command, date/stack note, result summary pointer) — without promoting inventing SLOs or flipping launch checklist GO items.

---

## Explicit non-goals (OUT)

- **No** product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not invent confirmed SLOs** — NFR §待确认 stays pending until user confirms separately.
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **Do not** mark **Wave D Done** from this leaf alone (**IBL-D4**, **IBL-D5** remain out of scope / Not Started).
- **IBL-D4** LO pool chaos / failover — **out of this leaf** (batch veto **IBL-D4-chaos**).
- **IBL-D5** legalhold depth — **out of this leaf** (batch veto **IBL-D5-legalhold**).
- **IBL-B7** Word / pixel baselines — **out of this leaf** (batch veto **IBL-B7-Word**).
- Formal phase remains **None**. Do **not** claim IBL program Done.
- Do **not** adopt a new load tool without dependency-policy verification against company-approved repositories (+ ADR where required).

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#125** | Owning leaf IBL-D3 |
| IBL program **F22** / **IBL-D3** acceptance | Gap + Done criteria (k6/approved suite + NFR confirmation path; no invented SLOs) |
| [non-functional-requirements.md](../requirements/non-functional-requirements.md) § LR-D5 / 待确认 | Sink for measured-input; confirmation is user-owned |
| Prior **LR-D6** / **IBL-B2** | Historical capacity/load evidence — measured-input only; not confirmed SLOs |
| Prior **IBL-D1** / **IBL-D2** | Sibling Wave D infra leaves (Done) — orthogonal |
| Batch / queue | Solo leaf; vetoes **IBL-D4-chaos**, **IBL-D5-legalhold**, **IBL-B7-Word** |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-d3-k6-nfr-path.md
task_ids: ["125"]
frontend_ui_in_scope: false
```
