# slim-knip-scan — Knip dead-code tooling + baseline evidence (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `slim-knip-scan` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-12 |
| **Formal phase** | **None** (no sole-active) |
| **Task Master** | **#48** (`done`) |
| **Evidence** | [docs/evidence/slim-knip-scan/](../evidence/slim-knip-scan/README.md) |
| **Merge** | `ea7db64` (`ea7db649866458386c46134b91c83417d1563c0a`) |
| **Placement** | merged to `main`; worktree removed |

---

## Why BDD is not-applicable

This slice delivers **frontend engineering tooling + dated scan evidence**, not a product behavior change:

- No actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- No change to generation, authoring, publish, or runtime response contracts.
- Outcomes are **Knip config + runnable scripts + indexed baseline artifacts**, not new acceptance thresholds promoted into confirmed product requirements.
- Optional Wave-1 deletion of confirmed-unused orphan files (zero importers) is **dead-code hygiene** verified by frontend regression gates — not a new user-facing contract that needs Given/When/Then product BDD.

Analogous readiness: [LR-D6 load smoke](./lrp-d6-load-smoke.md) / [LR-E1 SSE evidence](./lrp-e1-sse-proxy-e2e.md) — harness/evidence slices with `bdd_readiness: not-applicable`.

---

## What is in scope

| Deliverable | Intent |
| --- | --- |
| **Knip ^6.26** | `frontend/package.json` + `frontend/knip.json` (Vite / Vitest / Playwright / Vue) |
| **Scripts** | `pnpm -C frontend knip` · `pnpm -C frontend knip:prod` · `.\scripts\knip-scan.ps1` |
| **Hygiene** | Declare missing `@eslint/js` (eslint config import was unlisted) |
| **Evidence** | `docs/evidence/slim-knip-scan/` (`README`, report txt/json, summary) + `docs/README.md` index |
| **Wave-1** | **Done** — deleted the two Knip-confirmed unused files; gates green |

### Optional Wave-1 (hygiene, not product BDD)

| File | Why optional |
| --- | --- |
| `frontend/src/components/templates/TemplateRuleConfigurator.vue` | Knip unused file; no importers in `frontend/src` |
| `frontend/src/views/audit/auditEventColumnFilters.ts` | Knip unused file; no importers in `frontend/src` |

**Include preference:** prefer delete in this slice **if** frontend gates stay green (`lint` / `type-check` / `test` / `build`). Treat as regression-gated cleanup, **not** a behavior-spec authoring trigger.

**Out of Wave-1:** unused exports (93) / unused exported types (66) — triage in a later slim slice; do not mass-delete without review.

---

## Acceptance bullets (tooling / evidence — not product G/W/T)

These are **delivery acceptance** criteria for the engineering slice — not product BDD scenarios for TDD Red of new user journeys.

1. **Given** the feature worktree `DGE-slim-knip-scan`  
   **When** `pnpm -C frontend knip` (or `.\scripts\knip-scan.ps1`) runs  
   **Then** Knip executes with project config; exit `1` with findings is acceptable until cleaned (informational until CI gate enabled).

2. **Given** scan artifacts under `docs/evidence/slim-knip-scan/`  
   **When** docs index is consulted  
   **Then** evidence README + report/summary are present and linked from `docs/README.md`.

3. **Given** frontend quality gates  
   **When** implementer finishes the slice (with or without Wave-1 deletes)  
   **Then** `pnpm -C frontend lint` · `type-check` · `test` · `build` are green.

4. **Given** optional Wave-1 deletes of the two unused files  
   **When** those files are removed  
   **Then** no remaining imports break; gates remain green; no user-facing route/component that previously rendered them is orphaned (confirmed: zero importers at readiness time).

---

## Explicit non-goals

- No product UI/API/permission/audit behavior change.
- No inventing company LDAP / Kafka registry coordinates.
- No production go-live claim; launch checklist overall remains **NO-GO** (#3b Word residual).
- No activating **CD-3**.
- Do **not** touch worktree `DGE-audit-governance`.
- Formal phase remains **None**.
- Do **not** enable Knip as a blocking CI gate in this slice (optional later).
- Do **not** mass-delete unused exports / types without a dedicated triage slice.

---

## Traceability

| Artifact | Role |
| --- | --- |
| [docs/evidence/slim-knip-scan/README.md](../evidence/slim-knip-scan/README.md) | Baseline scan evidence + re-run instructions |
| `frontend/knip.json` | Knip project config |
| `scripts/knip-scan.ps1` | Host runner writing evidence folder |
| Task Master **#48** | **done** (slice id `slim-knip-scan`; merge `ea7db64`) |
| Launch checklist | Unchanged overall **NO-GO** — this slice is not a go-live closer |

```
bdd_readiness: not-applicable
task_ids: [48]  # slim-knip-scan; done; no sole-active
owning_doc: docs/behavior/slim-knip-scan.md
```
