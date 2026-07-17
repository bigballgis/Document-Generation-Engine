# fe-vitest-3-upgrade — Vitest 3.2.6+ security remediation (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `fe-vitest-3-upgrade` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-17 |
| **Formal phase** | **None** (no sole-active; do **not** activate CD-3) |
| **Task Master** | **#50** → **Done** (merge `6c8fff7d`) |
| **Placement** | Was **ISOLATED** — `D:/working/DGE-fe-vitest-3-upgrade` · `feat/fe-vitest-3-upgrade` (worktree **removed** after merge) |
| **Parent residual** | [deps-security-refresh](./deps-security-refresh.md) (#49) Critical exception [GHSA-5xrq-8626-4rwp](https://github.com/advisories/GHSA-5xrq-8626-4rwp) |
| **Evidence / exception SoT** | [deps-security-refresh-frontend-audit.md](../operations/deps-security-refresh-frontend-audit.md) |

---

## Actor / goal / trigger

| Field | Value |
| --- | --- |
| **Actor / role** | Frontend platform engineer (not an end-user product role) |
| **Goal** | Upgrade `vitest` + `@vitest/coverage-v8` from **2.1.9** to **≥3.2.6** so `pnpm audit` no longer reports Critical GHSA-5xrq-8626-4rwp; keep frontend quality gates green |
| **Trigger** | User「继续剩余任务」— explicit activation + confirmation to unblock **#50** (was waiting user confirmation + ADR for Vitest 2→3) |
| **Preconditions** | Isolated worktree on `feat/fe-vitest-3-upgrade`; #49 exception row documents residual Critical (expires **2026-10-13**); production / Docker management UI deps unaffected (`audit --prod` was clean) |

---

## Why BDD is not-applicable

This slice is a **dev/tooling dependency security remediation**, not a product behavior change:

- No actor / role journey, management UI surface, API contract, permission rule, or audit semantics.
- No intentional change to generation, authoring, publish, or runtime response contracts.
- Outcomes are **Vitest major pin bump + config/test harness adjustments as needed + advisory cleared + frontend gates green** — not new user-facing acceptance thresholds.
- Product Given/When/Then scenarios would invent UI journeys this upgrade does not own; regression is covered by existing `pnpm -C frontend` gates (`lint` / `type-check` / `test` / `build`).

Analogous readiness: [deps-security-refresh](./deps-security-refresh.md) / [boot-4-1-upgrade](./boot-4-1-upgrade.md) / [slim-knip-scan](./slim-knip-scan.md) — tooling/ops/hygiene slices with `bdd_readiness: not-applicable`.

---

## What is in scope (tooling only)

| Deliverable | Intent |
| --- | --- |
| **Vitest pin** | `vitest` **2.1.9 → ≥3.2.6** (patched line for GHSA-5xrq-8626-4rwp) |
| **Coverage peer** | `@vitest/coverage-v8` aligned to the same Vitest **3.2.6+** major line |
| **Harness hygiene** | Adjust Vitest config / test files only as required for green `pnpm test` after the major bump |
| **Audit closure** | Close or supersede the #49 exception row once Critical is remediated |
| **Gates** | `pnpm -C frontend lint` · `type-check` · `test` · `build` green |
| **Governance** | ADR-0029 amended 2026-07-17 — Vitest **3.x** security floor **≥3.2.6** (+ aligned `@vitest/coverage-v8`); Vue Test Utils + Playwright unchanged |

---

## Acceptance bullets (ops hygiene — not product G/W/T)

These are **delivery acceptance** criteria for the engineering slice — **not** product BDD scenarios for TDD Red of new user journeys.

1. **Given** the feature worktree `DGE-fe-vitest-3-upgrade` on `feat/fe-vitest-3-upgrade`  
   **When** `vitest` and `@vitest/coverage-v8` are upgraded to **≥3.2.6** (lockfile aligned)  
   **Then** [GHSA-5xrq-8626-4rwp](https://github.com/advisories/GHSA-5xrq-8626-4rwp) is remediated in the frontend dependency audit (Critical cleared for this finding).

2. **Given** the Vitest 3 major bump  
   **When** implementer finishes harness/config adjustments  
   **Then** `pnpm -C frontend lint` · `type-check` · `test` · `build` are **GREEN**, with **no** intentional management UI / API / permission behavior change.

3. **Given** the #49 exception row for Vitest 2.1.9  
   **When** the slice is closed  
   **Then** [deps-security-refresh-frontend-audit.md](../operations/deps-security-refresh-frontend-audit.md) records the exception as **closed** (or equivalent remediation note) — do not leave an open Critical with a past cleanup task.

4. **Given** stack documentation  
   **When** doc-keeper / post-task sync runs  
   **Then** ADR-0029 (and mirrored stack notes if any) reflect Vitest **3.x** tooling baseline with security floor **≥3.2.6**, without inventing Vue/Vite/Pinia major jumps.

---

## Explicit non-goals

- No product UI/API/permission/audit behavior change; **no** Playwright E2E / UIUX product scenarios for this leaf.
- No inventing formal phase / sole-active program; formal phase remains **None**.
- No activating **CD-3**.
- No production go-live claim; launch checklist overall remains unchanged by this leaf.
- No Vue / Vite / Pinia / vue-router / vue-i18n major line jumps in this slice (only Vitest + coverage peer).
- Do **not** treat Vitest UI server exposure as a production runtime concern — advisory is **dev-only**; still avoid exposing `@vitest/ui` to untrusted networks.
- Do **not** rewrite Accepted ADR decision text as “progress”; only amend baseline pin/note for the confirmed Vitest 3 tooling line.

---

## Traceability

| Artifact | Role |
| --- | --- |
| Task Master **#50** | Owning cleanup task for #49 Vitest Critical residual |
| Task Master **#49** / [deps-security-refresh.md](./deps-security-refresh.md) | Parent hygiene slice; left Vitest 2.1.9 exception |
| [deps-security-refresh-frontend-audit.md](../operations/deps-security-refresh-frontend-audit.md) | Exception metadata + deferred majors table |
| [ADR-0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md) | Frontend stack baseline — **amended 2026-07-17** (Vitest **3.x** floor **≥3.2.6**; ADR gate cleared for #50 FE bump) |
| [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) | Critical/High remediation / exception pattern |
| User confirmation (2026-07-17) | 「继续剩余任务」unblocks #50 Vitest 2→3 |
| `frontend/package.json` + lockfile | Implementation surface for pin bump (frontend-engineer) |

```
bdd_readiness: not-applicable
task_ids: [50]
owning_doc: docs/behavior/fe-vitest-3-upgrade.md
```
