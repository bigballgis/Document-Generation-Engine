# ibl-d4-lo-pool-chaos — LibreOffice pool chaos / failover (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d4-lo-pool-chaos` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-D4** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-D |
| **Finding** | **F22** residual — LO pool chaos / failover suite (D3 closed k6 + NFR confirmation path; SLOs stay proposed) |
| **Task Master** | **#126** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Delivery status** | Stage 4 rendering-engineer **complete** (chaos suite + evidence; `mvn verify` GREEN) — **not** leaf Done / merge |
| **Placement** | ISOLATED `D:/working/DGE-ibl-d4-lo-pool-chaos` · `feat/ibl-d4-lo-pool-chaos` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |
| **Batch** | **solo** (`member_task_ids: ["126"]`; vetoes **IBL-D5-legalhold**, **IBL-B7-Word**, checklist GO / Wave D Done) |
| **Depends** | **IBL-B2** (#114) — capacity plan + queue/reject Micrometer metrics |

---

## Why BDD is not-applicable

This leaf is **IT / chaos test infrastructure** closing the remaining **F22** chaos half — **not** a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Outcomes are **chaos/failover integration tests** for LibreOffice (PDF conversion) pool **saturation / timeout / reject** paths, exercising and asserting against **IBL-B2 metrics** (queue gauges + rejection counters / fail-closed capacity codes already delivered).
- Product Given/When/Then for bank-letter filling, rendering, or compare UI belong to other IBL waves — **out of this leaf**.
- Prior **IBL-B2** capacity behavior and **IBL-D3** k6/NFR path remain SoT for capacity plan and load tooling; this leaf **does not** invent confirmed NFR SLOs or re-open B2 capacity defaults.

Program authority: IBL Wave D row **IBL-D4** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-D4**.

---

## Acceptance (delivery / IT — not product G/W/T)

These are **engineering acceptance** criteria for the slice — not product BDD scenarios for TDD Red of new user journeys.

1. **Given** the LibreOffice / PDF conversion pool under controlled saturation (bounded pool + queue as delivered by **IBL-B2**)  
   **When** chaos/failover IT scenarios drive concurrent conversion load past capacity  
   **Then** saturation / reject paths are exercised and fail-closed as designed (e.g. capacity-exceeded / reject semantics), **and** B2 Micrometer signals (queue gauges and/or rejection counters) move in the expected direction for the scenario.

2. **Given** timeout / hung-conversion style fault injection (or equivalent IT harness) against the LO/PDF conversion path  
   **When** the IT profile runs  
   **Then** timeout / failover behavior is covered by automated tests (honest skip only when LO/`soffice` is unavailable under a documented optional profile — not silent green that hides missing coverage on the intended IT lane).

3. **Given** reject / backpressure paths under pool pressure  
   **When** reviewers inspect test assertions  
   **Then** tests **tie to B2 metrics** (named gauges/counters from IBL-B2) and/or stable capacity error taxonomy — not ad-hoc unrelated counters invented only for this leaf.

4. **Given** the leaf claims Done  
   **When** reviewers inspect gates  
   **Then** **backend `mvn verify`** is green on the default lane, and the **IT / chaos profile** (or documented IT suite invocation) has recorded evidence of pass (or honest, documented precondition skip) — without flipping launch checklist GO items or inventing confirmed SLOs.

---

## Explicit non-goals (OUT)

- **No** product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **Do not** invent Word / pixel baselines; **IBL-B7** remains Blocked / out of scope.
- **Do not** mark **Wave D Done** from this leaf alone (**IBL-D5** remains out of scope / Not Started).
- **IBL-D5** legalhold depth — **out of this leaf**.
- Formal phase remains **None**. Do **not** claim IBL program Done.
- **Do not** invent confirmed NFR SLOs (D3 confirmation path / LR-D5 stay proposed until separate user confirmation).
- **Do not** re-scope IBL-B2 capacity plan defaults solely to make chaos green.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#126** | Owning leaf IBL-D4 |
| IBL program **F22** / **IBL-D4** acceptance | Gap + Done criteria (LO pool chaos/failover IT; ties to B2 metrics; backend verify / IT profile) |
| Prior **IBL-B2** (#114) / [ibl-b2-pdf-conversion-capacity.md](./ibl-b2-pdf-conversion-capacity.md) | Capacity plan + queue/reject metrics this leaf asserts against |
| Prior **IBL-D3** (#125) / [ibl-d3-k6-nfr-path.md](./ibl-d3-k6-nfr-path.md) | Sibling Wave D leaf — k6 + NFR path Done; chaos deferred here |
| Prior **IBL-D1** / **IBL-D2** | Sibling Wave D infra leaves (Done) — orthogonal |
| Batch / queue | Solo leaf; vetoes **IBL-D5-legalhold**, **IBL-B7-Word**, #3b/#5a GO, Wave D Done, go-live |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-d4-lo-pool-chaos.md
task_ids: ["126"]
frontend_ui_in_scope: false
```
