---
id: DOC-ARCH-INTERNAL-TOOL-SELECTION-MATRIX
type: Architecture View
status: Proposed
sourceOfTruth: false
owners:
  - architecture
  - implementation
  - orchestration
dependsOn:
  - docs/architecture/orchestration-high-level-plan.md
  - docs/architecture/tdd-delivery-workflow.md
  - docs/documentation-architecture.md
related:
  - .github/agents/fullstack-orchestrator.agent.md
  - .github/agents/task-planning.agent.md
  - .github/agents/document-governance.agent.md
  - .github/agents/backend-execution.agent.md
  - .github/agents/frontend-execution.agent.md
  - .github/agents/script-execution.agent.md
  - .github/agents/e2e-test-execution.agent.md
  - .github/agents/e2e-uiux-test.agent.md
  - .github/agents/deploy-execution.agent.md
  - .github/agents/frontend-test.agent.md
  - .github/agents/backend-test.agent.md
  - .github/agents/frontend-quality-scan.agent.md
  - .github/agents/backend-quality-scan.agent.md
  - .github/agents/implementation-execution.agent.md
  - .github/agents/git-preflight-review.agent.md
  - .github/agents/git-commit-executor.agent.md
  - .github/agents/git-push-executor.agent.md
---

# Internal Tool Selection Matrix

## Purpose

This document maps common repository tasks to the specialized internal agents that should handle them.

It does not define product behavior or implementation details. It exists to reduce routing ambiguity and keep orchestration traceable.

## Selection Rules

- Start every execution-oriented request from [Orchestration High-Level Plan](./orchestration-high-level-plan.md) before choosing task-planning or execution agents.
- If the request does not match the active epic or a confirmed planned direction, route to `document-governance` and/or `task-planning` to refresh the high-level plan before implementation.
- Start from source-of-truth documents when requirements or behavior are unclear.
- Use `task-planning` before implementation when a task needs decomposition, scope slicing, or dependency ordering.
- Use `document-governance` for documentation-first changes, source-of-truth updates, and traceable governance edits.
- Use `script-execution` when the main deliverable is a repository script or a script hardening change.
- Use `frontend-execution` or `backend-execution` when the main deliverable is code changes in those layers.
- Use `frontend-test`, `backend-test`, `frontend-quality-scan`, or `backend-quality-scan` for focused verification and static gates.
- Use `e2e-test-execution` or `e2e-uiux-test` for Playwright evidence, with `e2e-uiux-test` reserved for visual, responsive, accessibility, theme, and interaction-polish evidence.
- Use `deploy-execution` for rollout, precheck, or deployment evidence tasks.
- Use `git-preflight-review`, `git-commit-executor`, and `git-push-executor` for stage-close checkpoints after each completed task that changes files.

## Recommended Routing

| Task Type | Primary Agent | Secondary Gate / Follow-up |
| --- | --- | --- |
| Requirement or backlog decomposition | `task-planning` | `document-governance` when docs change |
| Documentation-first governance update | `document-governance` | `scripts/validate-doc-structure.ps1` after structural edits |
| Backend feature or service change | `backend-execution` | `backend-test`, `backend-quality-scan` |
| Frontend feature or workflow change | `frontend-execution` | `frontend-test`, `frontend-quality-scan` |
| Script or automation hardening | `script-execution` | Script-specific validator or smoke run |
| Playwright stabilization / evidence | `e2e-test-execution` | Narrow Playwright rerun and artifact capture |
| UIUX visual / responsive / accessibility evidence | `e2e-uiux-test` | Screenshot, trace, and viewport-specific evidence |
| Deployment readiness / controlled rollout | `deploy-execution` | Precheck evidence and rollback readiness |
| Review-only technical triage | `agent-evolution` | Use when repeated failures or routing drift appear |
| Scoped commit readiness | `git-preflight-review` | `git-commit-executor` if ready |
| Scoped commit creation | `git-commit-executor` | `git-push-executor` after green gates |
| Push after task completion | `git-push-executor` | Confirm pushed head and clean scope |

## Common Orchestration Paths

### Documentation and Governance

1. `task-planning` when scope needs decomposition.
2. `document-governance` for the source-of-truth edit.
3. `git-preflight-review` for the changed files.
4. `git-commit-executor` for the scoped commit.
5. `git-push-executor` for branch synchronization.

### Backend Delivery

1. `task-planning` for task slicing.
2. `backend-execution` for implementation.
3. `backend-test` and `backend-quality-scan` for gates.
4. `git-preflight-review`, `git-commit-executor`, and `git-push-executor` for closure.

### Frontend Delivery

1. `task-planning` for task slicing.
2. `frontend-execution` for implementation.
3. `frontend-test`, `frontend-quality-scan`, and, when relevant, `e2e-uiux-test` for evidence.
4. `git-preflight-review`, `git-commit-executor`, and `git-push-executor` for closure.

### Script-First Automation

1. `task-planning` for scope definition.
2. `script-execution` for script changes and validation.
3. `git-preflight-review`, `git-commit-executor`, and `git-push-executor` for closure.

## Avoid These Mistakes

- Do not use `e2e-test-execution` for UIUX review work that requires theme, accessibility, or visual analysis.
- Do not use `frontend-test` or `backend-test` as a substitute for lint, type-check, or build ownership.
- Do not skip `task-planning` when the request is broad, ambiguous, or multi-step.
- Do not commit or push without the scoped git-preflight-review checkpoint.
- Do not treat untracked temporary files as functional work unless they are part of the approved scope.

## Confirmed vs Pending

Confirmed:

1. The repository already has specialized agents for planning, governance, backend, frontend, scripts, Playwright, deployment, and git closure.
2. Generic continuation requests should resume from the active epic in [Orchestration High-Level Plan](./orchestration-high-level-plan.md).
3. Stage-close work must remain scoped and traceable.

Pending:

1. This matrix may expand as new recurring workflows appear.
2. Future tool routing refinements should be captured here first before being copied into other docs.
