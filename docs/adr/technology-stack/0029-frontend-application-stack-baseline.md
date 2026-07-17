---
id: ADR-0029
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0029"
topic: technology-stack
related:
  - docs/architecture/README.md
  - docs/architecture/runtime-view.md
  - docs/architecture/ai-development-guide.md
  - docs/architecture/technology-stack-decisions.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
  - docs/behavior/fe-vitest-3-upgrade.md
  - docs/operations/deps-security-refresh-frontend-audit.md
---

# ADR 0029: Frontend Application Stack Baseline

## Status

Accepted (amended 2026-07-17 — Vitest **2.x → 3.x**, security floor **≥3.2.6**)

## Context

The management frontend needs a stable application stack so the documented architecture, implementation planning, and future UI work stay aligned.

The user confirmed a Vue-based frontend with Element Plus, Pinia, Vue Router 4, Axios, pnpm, SCSS/CSS Modules, and a Vue testing stack. This ADR records those accepted frontend application baseline decisions so they no longer remain as session-only notes in the technology decision log.

## Decision

The confirmed frontend application baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Frontend UI stack | Element Plus + Pinia + Vue Router 4 + Axios + pnpm | Main management UI application stack baseline. |
| Frontend styling | SCSS + CSS Modules | Frontend styling baseline for component-scoped styling and maintainable UI composition. |
| Frontend testing | Vitest **3.x** (security floor **≥3.2.6**) + Vue Test Utils + Playwright | Unit, component, and end-to-end testing baseline. Align `@vitest/coverage-v8` to the same Vitest **3.2.6+** major line. |
| Frontend package manager lock strategy | Enforce pnpm-lock.yaml | Lockfile baseline for the frontend toolchain. |

These decisions are accepted as the frontend application foundation. More specialized frontend architecture choices remain pending until they are explicitly confirmed and synchronized into follow-up ADRs or architecture views.

## Amendment — 2026-07-17 (Vitest 2.x → 3.x; security floor ≥3.2.6)

**Prior decision (2026-06-08):** Frontend testing = **Vitest** (unpinned major) + Vue Test Utils + Playwright. Ops hygiene slice Task Master **#49** retained **Vitest 2.1.9** with an open Critical exception for [GHSA-5xrq-8626-4rwp](https://github.com/advisories/GHSA-5xrq-8626-4rwp) pending ADR confirmation of a Vitest **2 → 3** major jump.

**Amended decision:** Frontend testing = **Vitest 3.x** with security floor **`vitest` ≥3.2.6** and aligned **`@vitest/coverage-v8` ≥3.2.6**, plus unchanged **Vue Test Utils** and **Playwright**.

| Field | Value |
| --- | --- |
| **Date** | 2026-07-17 |
| **Rationale** | Explicit user confirmation via「继续剩余任务」to unblock Task Master **#50** (`fe-vitest-3-upgrade`) and remediate **GHSA-5xrq-8626-4rwp** (Vitest UI arbitrary file read/exec on unpatched `<3.2.6`). Patched line requires Vitest **≥3.2.6** (major **2 → 3**). |
| **Behavior note** | [fe-vitest-3-upgrade.md](../../behavior/fe-vitest-3-upgrade.md) — `bdd_readiness: not-applicable` (dev/tooling security remediation, not a product journey). |
| **Security floor** | `vitest` **≥3.2.6**; `@vitest/coverage-v8` aligned to the same **3.2.6+** line. Vitest **4.x** remains out of scope unless separately confirmed. |
| **Scope of amendment** | Unit/component test runner major line only (**Vitest 2.x → 3.x** / floor **≥3.2.6**). Does **not** invent Vue / Vite / Pinia / vue-router / vue-i18n major jumps. Vue Test Utils + Playwright unchanged. |
| **Implementation surface** | `frontend/package.json` + `frontend/pnpm-lock.yaml` (+ Vitest config/harness only as needed) owned by **frontend-engineer** (pipeline stage 4). This ADR records the **accepted target** baseline before/with that change. |
| **Exception closeout** | [deps-security-refresh-frontend-audit.md](../../operations/deps-security-refresh-frontend-audit.md) — **CLOSED** 2026-07-17 (Task Master **#50**; merge `6c8fff7d`; pins `vitest@3.2.7` + `@vitest/coverage-v8@3.2.7`; `pnpm audit` clean). |

**Consequences of this amendment:**

- Frontend unit/component tests run on the Vitest **3.x** major line; harness/config deltas may be required for green `pnpm -C frontend test`.
- The #49 Critical exception for GHSA-5xrq-8626-4rwp is no longer ADR-blocked; cleanup task **#50** delivered the pin bump and audit closeout (**Done**).
- Advisory remains **dev-only** (Vitest UI server); production Docker UI and `audit --prod` posture unchanged by this tooling line alone.
- Rollback path for the upgrade slice: restore Vitest **2.1.9** + matching coverage peer and re-open the exception metadata if needed.

## Consequences

- The frontend stack is now documented as an accepted baseline rather than scattered session notes.
- The management frontend can be implemented consistently with the documented runtime view and AI development guide.
- Future frontend toolchain or UI stack changes should be made by updating this ADR and the affected architecture views together.
- After the 2026-07-17 amendment, stack mirrors (tech-stack guardrails, architecture decision log) must say **Vitest 3.x (floor ≥3.2.6)**, not an implied Vitest 2 lock.

## Alternatives Considered

- Keeping the frontend stack only in the technology decision log: rejected because durable decisions need an ADR.
- Splitting styling, testing, and package management into separate ADRs: rejected for this baseline because they form one cohesive frontend application foundation.
- Using a different component library or state-management stack: not selected because the user confirmed the current Vue-centered baseline.
- Remaining on Vitest 2.1.9 with a standing Critical exception: rejected after explicit user confirmation to take the Vitest **3.2.6+** security floor (Task **#50**).

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Runtime View](../../architecture/runtime-view.md)
- [AI Development Guide](../../architecture/ai-development-guide.md)
- [Technology Stack Decision Log](../../architecture/technology-stack-decisions.md)
- [Basic Technology Stack Baseline ADR](./0022-basic-technology-stack-baseline.md)
- [fe-vitest-3-upgrade behavior note](../../behavior/fe-vitest-3-upgrade.md) (Task Master **#50**)
- [deps-security-refresh frontend audit / exception](../../operations/deps-security-refresh-frontend-audit.md)
