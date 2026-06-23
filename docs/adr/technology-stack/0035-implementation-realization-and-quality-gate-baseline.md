---
id: ADR-0035
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - implementation
adrNumber: "0035"
topic: technology-stack
related:
  - docs/architecture/technology-stack-decisions.md
  - docs/architecture/module-boundaries.md
  - docs/architecture/implementation-task-plan.md
  - docs/architecture/quality-gate-threshold-baseline.md
  - docs/architecture/tdd-delivery-workflow.md
---

# ADR 0035: Implementation Realization and Quality Gate Baseline

## Status

Accepted

## Context

The technology decision log still contains several session-confirmed rows that were not synchronized to a dedicated ADR. Those rows govern implementation realization and delivery quality-gate behavior and should be durable decisions instead of chat-only confirmations.

## Decision

The following baseline is accepted:

| Area | Decision | Notes |
| --- | --- | --- |
| Runtime split strategy | Day-1 multi-service split | Implementation follows service boundaries from architecture views; no single-process monolith target for day-1 runtime shape. |
| Task state and output metadata persistence strategy | PostgreSQL for durable state + Redis for hot-status acceleration | Durable task state and output metadata remain in PostgreSQL; Redis is used for hot-path status and acceleration scenarios. |
| Module/package naming convention | Business-module-first packaging (module-first, layered inside module) | Package ownership follows business modules first, then internal layers per module. |
| Java static scan baseline | Checkstyle + SpotBugs + PMD | Blocking static quality baseline for backend code review gates. |
| Dependency security scan baseline | OWASP Dependency-Check, block high/critical | Security baseline remains blocking on high/critical findings when scan execution is enabled. |
| Coverage gate baseline | JaCoCo >= 85% changed lines, >= 90% core/security-critical modules | Coverage gate baseline for implementation and review gates. |
| Frontend quality command baseline | pnpm lint + pnpm type-check + pnpm test + pnpm build | Frontend quality gate command set is fixed for baseline delivery workflow. |

Execution exception rule for constrained intranet environments:

- If NVD feeds are unreachable in CI due network constraints, dependency scanning may run as an explicit opt-in job switch rather than default-on.
- This exception changes execution mode only; it does not change the accepted risk posture or target blocking policy for high/critical vulnerabilities.

## Consequences

- The technology decision log can mark these rows as ADR-backed accepted decisions.
- Implementation and CI quality-gate behavior now have one durable reference document.
- Future changes to runtime split, package convention, or quality gates should update this ADR and related architecture workflow documents together.

## Alternatives Considered

- Keep these items in task sheets only: rejected because task sheets are execution artifacts, not durable decision records.
- Split each item into separate ADRs: rejected for now because the items are tightly coupled as one implementation realization and gate baseline.

## Related Documents

- [Technology Stack Decision Log](../../architecture/technology-stack-decisions.md)
- [Module Boundaries](../../architecture/module-boundaries.md)
- [Implementation Task Plan](../../architecture/implementation-task-plan.md)
- [Quality Gate Threshold Baseline](../../architecture/quality-gate-threshold-baseline.md)
- [Fixed TDD Delivery Workflow](../../architecture/tdd-delivery-workflow.md)