---
id: DOC-ARCH-INDEX
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/document-as-software.md
  - docs/documentation-architecture.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
related:
  - docs/architecture/system-context.md
  - docs/architecture/module-boundaries.md
  - docs/architecture/technology-stack-decisions.md
  - docs/architecture/agent-evolution-governance.md
  - docs/architecture/implementation-task-plan.md
  - docs/architecture/tdd-delivery-workflow.md
  - docs/architecture/quality-gate-threshold-baseline.md
  - docs/architecture/runtime-view.md
  - docs/architecture/data-storage-view.md
  - docs/architecture/async-messaging-view.md
  - docs/architecture/security-view.md
  - docs/architecture/ai-development-guide.md
---

# Architecture Documentation

> **Sync status (2026-06-23):** P0–P11 re-earned Done. Epic/milestone mirror:
> [execution-sync-ledger.md](../plan/execution-sync-ledger.md). Phase truth:
> [master-plan.md](../plan/master-plan.md).

## Purpose

This directory contains architecture views that make the system rebuildable from documentation.

Architecture views describe current architectural facts, boundaries, responsibilities, and pending implementation questions. They do not replace product requirements, domain rules, permission rules, API contracts, or ADRs.

## Architecture Views

| Document | Purpose | Status |
| --- | --- | --- |
| [System Context](system-context.md) | External actors, upstream/downstream systems, and system boundary | Accepted baseline |
| [Module Boundaries](module-boundaries.md) | Internal bounded modules and responsibility boundaries | Accepted baseline |
| [Technology Stack Decision Log](technology-stack-decisions.md) | Persistent ledger for confirmed/pending technology choices and ADR synchronization status | Proposed baseline |
| [Agent Evolution Governance](agent-evolution-governance.md) | Controlled evolution protocol for agent prompts/hooks, incident-driven tuning, and rollback-safe adjustments | Proposed baseline |
| [Implementation Task Plan](implementation-task-plan.md) | Technical waves 0-14 mapped to OpenAPI v1 | Synced 2026-06-23 |
| [Execution sync ledger](../plan/execution-sync-ledger.md) | Epic/milestone ↔ phase mapping + gate evidence | Active mirror |
| [M1 Task Sheet](m1-task-sheet.md) | Wave 0-1: foundation + contract discovery | Done |
| [M2 Task Sheet](m2-task-sheet.md) | Wave 2: synchronous generation | Done |
| [M3 Task Sheet](m3-task-sheet.md) | Wave 3: batch and async lifecycle | Done |
| [M4 Task Sheet](m4-task-sheet.md) | Wave 4: download security | Done |
| [M5 Task Sheet](m5-task-sheet.md) | Wave 5: API management plane | Done |
| [M6 Task Sheet](m6-task-sheet.md) | Wave 6: lifecycle governance | Done |
| [M7 Task Sheet](m7-task-sheet.md) | Wave 7: runtime E2E integration | Done |
| [M8 Task Sheet](m8-task-sheet.md) | Wave 8: security remediation gates | Done (local) |
| [M9 Task Sheet](m9-task-sheet.md) | Wave 9: dependency scan recovery | In Progress |
| [M9-T02 Closure Plan](m9-t02-closure-plan.md) | M9 frontend dependency security steps | Not Started |
| [M10 Task Sheet](m10-task-sheet.md) | Wave 10: deferred security closure | Not Started |
| [M11 Task Sheet](m11-task-sheet.md) | Wave 11: intranet security baseline | Not Started |
| [M12 Task Sheet](m12-task-sheet.md) | Wave 12: runtime endpoint adapters | Done |
| [M13 Task Sheet](m13-task-sheet.md) | Wave 13: runtime HTTP transport | Done |
| [M14 Task Sheet](m14-task-sheet.md) | Wave 14: batch transport + async accept | Done |
| [E01 Task Sheet](e01-task-sheet.md) | Master and template authoring core | Done |
| [E02 Task Sheet](e02-task-sheet.md) | Lifecycle workflow productization | Done |
| [E03 Task Sheet](e03-task-sheet.md) | API management completion | Done |
| [E04 Task Sheet](e04-task-sheet.md) | Audit and governance console | Done |
| [E05 Task Sheet](e05-task-sheet.md) | Enterprise integration hardening | Done (thin slice) |
| [E06 Task Sheet](e06-task-sheet.md) | Management UI product finish | Done |
| [E06 Role-Journey Release Evidence](e06-role-journey-release-evidence.md) | E06 evidence slots | Not Started |
| [E07 Task Sheet](e07-task-sheet.md) | Production readiness | Done (local gates) |
| [E11 Role Journey UI Plan](e11-role-journey-ui-continuation-plan.md) | Post-login role navigation | Done |
| [E12 Development Plan](e12-frontend-role-journey-development-plan.md) | Role-operation journey UI | Done (thin slice) |
| [E12 Phase 1 Task Sheet](e12-phase1-task-sheet.md) | E12-T01..T05 | Done |
| [E12 Phase 2 Task Sheet](e12-phase2-task-sheet.md) | E12-T06..T10 | Done (T10 optional pending) |
| [E05 External Validation Evidence](e05-external-validation-evidence.md) | Deployment-time dependency evidence matrix | Not Started |
| [E05 External Evidence Execution Log](e05-external-evidence-execution-log.md) | Validation cycle log (empty) | Not Started |
| [Orchestration High-Level Plan](orchestration-high-level-plan.md) | Epic ordering and active epic rules | Synced 2026-06-23 |
| [Internal Tool Selection Matrix](internal-tool-selection-matrix.md) | Common task-to-agent routing guidance and orchestration checkpoint map | Proposed baseline |


| [Two-Round Optimization Plan](two-round-optimization-plan.md) | Persistent execution plan for production-grade then enterprise-grade uplift with mandatory local benchmark preflight | Proposed baseline |
| [Checkstyle Baseline Governance Plan](checkstyle-baseline-governance-plan.md) | Confirmed staged checkstyle baseline debt closure governance with explicit transition thresholds | Accepted baseline |
| [Playwright E2E Stabilization and Agent Plan](e2e-stabilization-and-agent-plan.md) | Documentation-first non-blocking E2E stabilization policy, smoke coverage, cadence, evidence, promotion criteria, and E2E/E2E UIUX/deployment agent rollout mapping (T01-T09) | Proposed baseline |
| [Fixed TDD Delivery Workflow](tdd-delivery-workflow.md) | Mandatory implementation workflow from task generation through review, commit, and push | Accepted baseline |
| [Quality Gate Threshold Baseline](quality-gate-threshold-baseline.md) | Fixed default quality thresholds for complexity, dependency, test coverage, and maintainability gates | Accepted baseline |
| [Runtime View](runtime-view.md) | Runtime components, deployment shape, and execution responsibilities | Accepted baseline |
| [Data and Storage View](data-storage-view.md) | Durable data, cache, object storage, retention, and storage ownership | Accepted baseline |
| [Async Messaging View](async-messaging-view.md) | Kafka usage, message boundaries, asynchronous processing, retries, and DLQ expectations | Accepted baseline |
| [Security View](security-view.md) | Authentication, authorization, sensitive data handling, audit, and fail-closed boundaries | Accepted baseline |
| [AI Development Guide](ai-development-guide.md) | AI reading paths, documentation-first implementation workflow, and rebuildability checklist | Accepted baseline |

## Source-of-Truth Boundary

Architecture views own implementation-facing architecture facts. They must link back to the documents that own product behavior and decisions:

- Development execution ordering and active epic ownership are governed by [Orchestration High-Level Plan](orchestration-high-level-plan.md).
- Product behavior remains owned by [PRD](../product/PRD.md).
- Domain objects and lifecycle rules remain owned by [Domain Model](../domain/domain-model.md).
- Permissions and authorization rules remain owned by [Permission Matrix](../security/permission-matrix.md).
- API schema and examples remain owned by [API Docs](../api/README.md) and [OpenAPI v1](../api/openapi-v1.yaml).
- Durable decisions remain owned by [ADRs](../adr/README.md).
- Documentation structure remains owned by [Documentation Architecture](../documentation-architecture.md).

If an architecture view conflicts with a source-of-truth document, update the source-of-truth document or mark a pending question before changing implementation assumptions.

## Maintenance Rules

- Add or update architecture views before implementation when module boundaries, runtime responsibilities, storage boundaries, async processing, or security architecture change.
- Keep architecture views concise and fact-oriented.
- Put open implementation choices under explicit pending sections.
- Update [Documentation Index](../README.md) when adding, moving, or retiring architecture documents.
- Run `../../scripts/validate-doc-structure.ps1` after changing this directory or architecture-related indexes.
