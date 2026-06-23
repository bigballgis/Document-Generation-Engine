# Documentation Index

**Project baseline:** Restart from zero (2026-06-23); **P0–P11 re-earned Done** and **P13
(identity & group administration) Done** (2026-06-23) with implementation in `backend/` and
`frontend/`. See [PROJECT-STATUS-RESET.md](./PROJECT-STATUS-RESET.md)
and [plan/execution-sync-ledger.md](./plan/execution-sync-ledger.md).

## Start here

| Order | Document | Purpose |
| --- | --- | --- |
| 1 | [Master plan](./plan/master-plan.md) | Overall phase roadmap and status |
| 2 | [Plan layer index](./plan/README.md) | Detailed plans per phase (P0–P11) |
| 2b | [Execution sync ledger](./plan/execution-sync-ledger.md) | Epic/milestone mirror + gate evidence |
| 3 | [Orchestration high-level plan](./architecture/orchestration-high-level-plan.md) | Epic ordering and active epic rules |
| 4 | [Implementation task plan](./architecture/implementation-task-plan.md) | Technical waves M1–M14 |
| 5 | [Requirements plan](./requirements/requirements-plan.md) | Confirmed requirements + pending questions |
| 6 | [PRD](./product/PRD.md) | Product behavior and scope |

## Core product & domain

| Document | Purpose |
| --- | --- |
| [Requirements plan](./requirements/requirements-plan.md) | Raw confirmed requirements |
| [Non-functional requirements](./requirements/non-functional-requirements.md) | Quality, security, reliability constraints |
| [PRD](./product/PRD.md) | Product-facing behavior |
| [Domain model](./domain/domain-model.md) | Objects, states, invariants |
| [Permission matrix](./security/permission-matrix.md) | Roles, groups, authorization |

## Plan layer (execution truth)

| Document | Purpose |
| --- | --- |
| [Plan index](./plan/README.md) | Layer rules and phase links |
| [Execution sync ledger](./plan/execution-sync-ledger.md) | Epic/milestone ↔ phase mapping + evidence |
| [Master plan](./plan/master-plan.md) | P0–P11 phases — see phase detail plans for status |
| [Optimization plan & backlog](./plan/optimization-plan.md) | Evidence-backed optimization backlog (docs drift, gates, backend, frontend) |
| [UX & upgradeability optimization plan](./plan/ux-upgradeability-optimization-plan.md) | User-interaction completeness + extensibility — **Wave A Done**, **Wave B In Progress** (2026-06-23) |
| [P0 Foundation](./plan/detail/P0-foundation.md) | Scaffold, compose, gates |
| [P1 Login & session](./plan/detail/P1-login-session.md) | Local auth, role landing |
| [P2 Master management](./plan/detail/P2-master-management.md) | DOCX master, anchors, review |
| [P3 Template authoring](./plan/detail/P3-template-authoring.md) | Wizard, variables, content |
| [P4 Rendering & preview](./plan/detail/P4-rendering-preview.md) | DOCX/PDF, fidelity |
| [P5 Lifecycle governance](./plan/detail/P5-lifecycle-governance.md) | Test → approve → publish |
| [P6 API management](./plan/detail/P6-api-management.md) | Policy, credentials, AD Group |
| [P7 Runtime API](./plan/detail/P7-runtime-api.md) | OpenAPI v1 operations |
| [P8 Audit & contract](./plan/detail/P8-audit-contract.md) | Audit console, caller view |
| [P9 Production readiness](./plan/detail/P9-production-readiness.md) | Gates, observability, deploy |
| [P10 Runtime download](./plan/detail/P10-runtime-download.md) | Secure document download |
| [P11 Batch & async generation](./plan/detail/P11-batch-async.md) | Sync batch, async task query/cancel |
| [P13 Identity & group administration](./plan/detail/P13-identity-group-administration.md) | User + group management plane, fail-closed escalation guard, audit (Done 2026-06-23) |
| [P14 Confirmed large domains](./plan/detail/P14-confirmed-large-domains.md) | Clause modules, collaboration to-dos, export/import |

## Architecture

| Document | Purpose |
| --- | --- |
| [Architecture index](./architecture/README.md) | Architecture views entry |
| [System context](./architecture/system-context.md) | External actors and boundaries |
| [Module boundaries](./architecture/module-boundaries.md) | Bounded modules |
| [Runtime view](./architecture/runtime-view.md) | Deployment and components |
| [Data & storage view](./architecture/data-storage-view.md) | Persistence and retention |
| [Async messaging view](./architecture/async-messaging-view.md) | Kafka boundaries |
| [Security view](./architecture/security-view.md) | Auth, audit, fail-closed |
| [Technology stack decisions](./architecture/technology-stack-decisions.md) | ADR sync ledger |
| [TDD delivery workflow](./architecture/tdd-delivery-workflow.md) | Mandatory delivery loop |
| [Quality gate baseline](./architecture/quality-gate-threshold-baseline.md) | Threshold defaults |
| [AI development guide](./architecture/ai-development-guide.md) | Reading paths for implementers |

## Orchestration & milestones

| Document | Purpose |
| --- | --- |
| [Orchestration plan](./architecture/orchestration-high-level-plan.md) | Epic backlog E01–E12 |
| [Implementation task plan](./architecture/implementation-task-plan.md) | Waves 0–14 |
| [M1–M14 task sheets](./architecture/m1-task-sheet.md) | Milestone decomposition (see m2–m14 siblings) |
| [E01–E07 task sheets](./architecture/e01-task-sheet.md) | Epic decomposition (see e02–e07 siblings) |
| [E11 role-journey plan](./architecture/e11-role-journey-ui-continuation-plan.md) | Post-login navigation |
| [E12 development plan](./architecture/e12-frontend-role-journey-development-plan.md) | Role-operation UI |

## API

| Document | Purpose |
| --- | --- |
| [API index](./api/README.md) | Contract maintenance |
| [OpenAPI v1](./api/openapi-v1.yaml) | Formal runtime contract |
| [Contract outline](./api/contract-outline.md) | Narrative companion |
| [Examples](./api/examples/README.md) | Request/response examples |

## Operations

| Document | Purpose |
| --- | --- |
| [Production runbook](./operations/runbook.md) | Release gate, prod compose profile, observability |

## ADRs

| Document | Purpose |
| --- | --- |
| [ADR index](./adr/README.md) | Decision records (Accepted = decision, not task done) |

## Governance & constitution

| Document | Purpose |
| --- | --- |
| [Document as software charter](./document-as-software.md) | Operating philosophy |
| [Documentation architecture](./documentation-architecture.md) | Knowledge model |
| [Governance](./governance.md) | Update rules and anti-drift gates |
| [Git workflow](./git-workflow.md) | Version control workflow |
| [PROJECT-STATUS-RESET](./PROJECT-STATUS-RESET.md) | Zero baseline declaration |

## Project agent tooling (`.cursor/`)

| Asset | Purpose |
| --- | --- |
| `.cursor/agents/doc-keeper.md` | Documentation-as-code guardian |
| `.cursor/agents/backend-engineer.md` | Backend TDD implementer |
| `.cursor/agents/frontend-engineer.md` | Frontend TDD implementer |
| `.cursor/agents/architecture-reviewer.md` | Read-only architecture review |
| `.cursor/agents/plan-orchestrator.md` | Plan layer maintenance |
| `.cursor/agents/post-task-doc-sync.md` | Mandatory end-of-task documentation sync |
| `.cursor/skills/document-as-code/` | Doc workflow skill |
| `.cursor/skills/post-task-doc-sync/` | Post-task doc sync workflow |
| `.cursor/skills/tdd-feature-delivery/` | TDD loop skill |
| `.cursor/skills/i18n-english-first/` | English-first i18n skill |
| `.cursor/skills/plan-status-tracking/` | Plan status skill |
| `.cursor/rules/*.mdc` | Project constitutions (always apply) |

## Source-of-truth order (on conflict)

1. Latest explicit user confirmation  
2. [Requirements plan](./requirements/requirements-plan.md)  
3. [PRD](./product/PRD.md)  
4. [Domain model](./domain/domain-model.md)  
5. [Permission matrix](./security/permission-matrix.md)  
6. [ADRs](./adr/)

## Status vocabulary (project / epic / task)

`Not Started` | `In Progress` | `Blocked` | `Done`

Prior wave/epic closure claims are **void** until re-earned with real behavior and green gates.
