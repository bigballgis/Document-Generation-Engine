---
name: delivery-pipeline
description: Canonical delivery pipeline stage numbers, handoff payload, E2E-before-deploy stack prep, and isolated-worktree doc-sync path. Use whenever orchestrating or chaining specialists so stage order stays consistent.
---

# Delivery Pipeline

Authoritative stage table (matches `delivery-orchestrator` and
`delivery-orchestration-constitution.mdc`):

| # | Stage | Agent |
| --- | --- | --- |
| 0 | Placement | `worktree-router` |
| 1 | Behavior spec | `behavior-spec-author` |
| 2 | Plan / Task Master | `plan-orchestrator` |
| 3 | Docs-first | `doc-keeper` |
| 4 | Implement | `backend-engineer` / `frontend-engineer` / `rendering-engineer` |
| 5 | E2E stack prep | `build-deploy-agent` (queue) |
| 6 | E2E functional | `e2e-test-engineer` |
| 7 | E2E UIUX | `e2e-uiux-reviewer` |
| 8 | Architecture | `architecture-reviewer` |
| 9 | Code quality | `code-quality-reviewer` (optional) |
| 10 | Deploy evidence | `build-deploy-agent` (queue) |
| 11 | Integrate | `integration-merger` (isolated only) |
| 12 | Doc sync | `post-task-doc-sync` (on **main** after merge) |
| 13 | Commit | `post-task-commit-review` |
| 14 | Verify (optional) | `verifier` |

## Handoff payload (copy into every Task prompt)

```
task_ids:
bdd_readiness: ready | blocked | not-applicable
placement: MAIN | ISOLATED
worktree_path:
branch:
behavior_summary:
acceptance_scenarios:
gate_evidence:
upstream_findings:
stage_done_definition:
```

## Isolated worktree rule

Code in feature worktree → merge via `integration-merger` → **doc-sync + commit on main**.

## Docker

Always `.\scripts\docker-deploy-queue.ps1` (never parallel stacks).
E2E docker needs stage 5 before stages 6–7.
