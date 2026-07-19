# ibl-d1-testcontainers-flyway — Testcontainers PostgreSQL + Flyway lane (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d1-testcontainers-flyway` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-D1** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-D |
| **Finding** | **F20** (H2 / Flyway-off default verify; zero Testcontainers) |
| **Task Master** | **#123** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Delivery status** | **Done** — MAIN merge `1a686938` / feature tip `f399489c`; worktree removed; F20 closed |
| **Placement** | Merged to MAIN (was ISOLATED `D:/working/DGE-ibl-d1-testcontainers-flyway` · `feat/ibl-d1-testcontainers-flyway`) |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX |
| **Batch** | **solo** (`member_task_ids: ["123"]`; vetoes IBL-D2-LO-CI, IBL-B7, umbrella-106) |

---

## Why BDD is not-applicable

This leaf is **test infrastructure / CI lane** closing **F20** — **not** a user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Outcomes are a **documented Maven/CI profile** that runs against **Testcontainers PostgreSQL with Flyway on**, plus docs that explain the **H2 vs Testcontainers** split.
- Product Given/When/Then for bank-letter filling, rendering, or compare UI belong to other IBL waves — **out of this leaf**.
- Default local/dev `mvn verify` **may remain H2** (Flyway-off / lightweight) — that is an intentional split, not a product behavior change.

Program authority: IBL Wave D row **IBL-D1** — **BDD: `not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-D1**.

---

## Acceptance (delivery / CI — not product G/W/T)

These are **engineering acceptance** criteria for the slice — not product BDD scenarios for TDD Red of new user journeys.

1. **Given** a documented verify/CI lane (Maven profile and/or CI job)  
   **When** that lane runs  
   **Then** it uses **Testcontainers PostgreSQL** with **Flyway on**.

2. **Given** a broken or incompatible Flyway migration / SQL defect against PostgreSQL  
   **When** the TC+Flyway lane executes  
   **Then** the lane **fails** (defects are not silently skipped).

3. **Given** project docs for the test database strategy  
   **When** an implementer or reviewer reads them  
   **Then** the **H2 vs Testcontainers** split is explained (what each lane covers and when to use which).

4. **Given** the default developer gate  
   **When** `mvn verify` runs without the TC profile  
   **Then** it **may remain H2**-based (lightweight) — requiring TC for every local verify is **not** a Done requirement of this leaf.

5. **Given** the documented TC+Flyway profile/CI path  
   **When** the leaf claims Done  
   **Then** that profile/CI path is green with recorded evidence.

---

## Explicit non-goals (OUT)

- **No** product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **IBL-D2** mandatory LibreOffice CI lane (fail-not-skip when `soffice` absent) is **out of this leaf**.
- **Do not invent SLOs** (NFR confirmation / k6 path remains **IBL-D3**).
- **No** Word / pixel baselines; **IBL-B7** remains Blocked / out of scope.
- Formal phase remains **None**. Do **not** claim Wave D / IBL program Done from this leaf alone.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#123** | Owning leaf IBL-D1 |
| IBL program **F20** / **IBL-D1** acceptance | Gap + Done criteria (TC PostgreSQL + Flyway-on lane; H2 vs TC docs) |
| Batch / queue | Solo leaf; vetoes **IBL-D2**, **IBL-B7**, **umbrella-106** |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-d1-testcontainers-flyway.md
task_ids: ["123"]
frontend_ui_in_scope: false
```
