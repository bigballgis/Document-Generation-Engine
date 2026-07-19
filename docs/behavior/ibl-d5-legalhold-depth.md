# ibl-d5-legalhold-depth — Legal hold test depth (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d5-legalhold-depth` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-19 |
| **Program leaf** | **IBL-D5** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Wave IBL-D |
| **Finding** | **F23** — `legalhold` has only 2 thin test classes; Docker Playwright smoke subset 9/162 (Playwright expansion **out of this leaf**) |
| **Task Master** | **#127** |
| **Formal phase** | **None** (do **not** invent a P-phase; do **not** claim IBL program Done) |
| **Delivery status** | **Done** — MAIN merge `6f672271` / feature tip `2e56787e`; Wave IBL-D Done; sole-active cleared |
| **Placement** | ISOLATED `D:/working/DGE-ibl-d5-legalhold-depth` · `feat/ibl-d5-legalhold-depth` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management UI / Playwright E2E / UIUX in this leaf |
| **Batch** | **solo** (`member_task_ids: ["127"]`; vetoes **#3b/#5a GO**, go-live, Wave E, **IBL-B7** Word, Docker Playwright 9→162 expansion, Wave D Done claim from incomplete siblings) |
| **Product SoT (unchanged)** | [ce-g04-legal-hold.md](./ce-g04-legal-hold.md) (`BDD-CE-G04-001…017`) — hold create / enforce / block / release already confirmed |

---

## Why BDD is not-applicable

This leaf is **test-depth / regression hardening** for the existing `legalhold` module — **not** a new user journey:

- No new actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- Product Given/When/Then for legal hold remain SoT in **CE-G04** (`BDD-CE-G04-*`); this leaf **deepens automated coverage** of create / enforce / block (and critical hold regressions), not re-specs product behavior.
- Stage 1 gap scan: current backend tests are the two thin classes `LegalHoldServiceTest` + `LegalHoldExemptionServiceTest` (aligned with F23). **No product behavior gaps** discovered that would promote this leaf to `ready` / require new confirmed requirements.
- Expanding Docker Playwright beyond **9/162** is **explicitly out of IBL-D5** (coordinate with CDP/CE E2E ownership later — do not silently re-own CD-E2E).

Program authority: IBL Wave D row **IBL-D5** — **BDD: `not-applicable` (tests) / required only if behavior gaps found**. Gaps **not** found → **`not-applicable`**.

Acceptance pointer (authoritative for Done of this leaf): [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § Acceptance **IBL-D5**.

---

## Acceptance (delivery / IT — not product G/W/T)

These are **engineering acceptance** criteria for the slice — not product BDD scenarios inventing new user journeys. Red/green tests assert against **already confirmed** CE-G04 behavior.

1. **Given** the existing legal-hold product semantics (CE-G04 create / ACTIVE enforce / retention block / release)  
   **When** reviewers inspect the `legalhold` automated suite after this leaf  
   **Then** coverage is **meaningfully deeper than the prior 2 thin classes**, including create, enforce (exemption hit), and block (non-exempt / released / out-of-window) paths — not a rename-only or assertion-free expansion.

2. **Given** critical hold regressions (e.g. RELEASED never exempts; scope mismatch; fail-closed authorization / validation paths already in CE-G04)  
   **When** the deepened suite runs under backend verify  
   **Then** regressions for those critical hold behaviors are encoded as durable tests that fail if product semantics regress.

3. **Given** the leaf claims Done  
   **When** reviewers inspect gates  
   **Then** **backend `mvn verify`** is green — without expanding Docker Playwright 9/162, without flipping launch checklist GO items, and without claiming go-live / Wave E / IBL-B7 Word baselines.

---

## Explicit non-goals (OUT)

- **No** new product UI/API/permission/audit journey; **no** Playwright E2E / UIUX (`frontend_ui_in_scope=false`).
- **Do not** expand Docker Playwright subset beyond **9/162** in this leaf (CDP/CE E2E ownership).
- **Do not** flip checklist **#3b GO** / **#5a GO**.
- **Do not** claim go-live.
- **Do not** invent Word / pixel baselines; **IBL-B7** remains Blocked / out of scope.
- **Wave E** (multinational content model) — **out of this leaf** (wave remains Blocked / pending decisions).
- Formal phase remains **None**. Do **not** claim IBL program Done solely from D5; Wave D Done only when program rules allow after D5 close (do not invent Wave D Done before leaf Done + program sync).
- **Do not** revise ADR-0040 / ADR-0048 bodies; CE-G04 overlay semantics stay SoT.
- **Do not** invent new hold scopes, eDiscovery export, or GROUP_ADMIN hold management.

---

## Behavior-gap note (Stage 1)

| Check | Result |
| --- | --- |
| Product journey missing vs CE-G04? | **No** — CE-G04 SoT remains complete for create/enforce/block/release |
| Permission / contract / ADR conflict requiring new confirmed req? | **None found** |
| Promote to `ready`? | **No** — stay **`not-applicable`** |

If later implementation discovers a true product gap, escalate: re-open BDD as `blocked`/`ready`, persist into CE-G04 (or amendment), then continue — do **not** silently invent behavior in tests alone.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#127** | Owning leaf IBL-D5 |
| IBL program **F23** / **IBL-D5** acceptance | Gap + Done criteria (legalhold test depth; backend verify; Playwright 9/162 OUT) |
| [ce-g04-legal-hold.md](./ce-g04-legal-hold.md) | Product behavior SoT (`BDD-CE-G04-*`) — unchanged by this leaf |
| Prior **IBL-D1…D4** | Sibling Wave D infra leaves (Done) — orthogonal |
| Batch / queue | Solo leaf; vetoes **#3b/#5a GO**, go-live, Wave E, **IBL-B7**, Playwright expansion |

```
bdd_readiness: not-applicable
owning_doc: docs/behavior/ibl-d5-legalhold-depth.md
task_ids: ["127"]
frontend_ui_in_scope: false
```
