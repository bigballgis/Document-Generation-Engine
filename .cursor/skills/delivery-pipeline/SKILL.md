---
name: delivery-pipeline
description: Canonical delivery pipeline stage numbers, handoff payload, E2E-before-deploy stack prep, and isolated-worktree doc-sync path. Use whenever orchestrating or chaining specialists so stage order stays consistent.
---

# Delivery Pipeline

Authoritative stage table (matches `delivery-orchestrator` and
`delivery-orchestration-constitution.mdc`):

| # | Stage | Agent |
| --- | --- | --- |
| **−1** | **Batch Recommendation** | **`delivery-orchestrator`** (in-orchestrator checklist; skill `delivery-batch-recommend`) |
| 0 | Placement | `worktree-router` (**mandatory** for delivery) |
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
| 11 | Integrate | `integration-merger` (**mandatory** — merge + remove worktree) |
| 12 | Doc sync | `post-task-doc-sync` (on **MAIN** after stage 11) |
| 13 | Commit | `post-task-commit-review` |
| 14 | Verify (optional) | `verifier` |

## Pre-0: Batch Recommendation (mandatory on deliver)

Before stage 0, the orchestrator **must** run
`.cursor/skills/delivery-batch-recommend/SKILL.md` and emit `batch_recommendation`
(`merge` | `solo` | `split`). This amortizes fixed pipeline cost across **related**
tasks into **one** leaf — it is **not** multi-writer parallel.

Behavior: [docs/behavior/delivery-batch-recommend.md](../../../docs/behavior/delivery-batch-recommend.md).

## Handoff payload (copy into every Task prompt)

```
task_ids:
bdd_readiness: ready | blocked | not-applicable
placement: ISOLATED   # mandatory for delivery; MAIN only for read-only / main-only opt-out
worktree_path:
branch:
behavior_summary:
acceptance_scenarios:
gate_evidence:
upstream_findings:
stage_done_definition:
batch_recommendation:
  decision: merge | solo | split
  rationale:
  member_task_ids: []
  proposed_slice_id:
  shared_acceptance_surface:
  vetoes_applied: []
  evidence_amortization:
  on_red_split_hint:
```

## Session worktree rule (mandatory)

Every delivery session: stage −1 Batch Recommendation → stage 0 → code in
`../DGE-<slice-id>` → merge via `integration-merger` → **doc-sync + commit on MAIN**.

## Single-lane serial (default, 2026-07-16)

At most **one** delivery leaf In Progress on this host. Queue other slices; do not
fan out writers for “continue / 自动执行后续”. Override only with user
`force-parallel` / `强制并行` (cap ≤2; still queue Docker).

Batch Recommendation may **merge** related tasks into that one leaf; it must **never**
create a second concurrent writer.

## Docker

Always `.\scripts\docker-deploy-queue.ps1` (never parallel stacks).
E2E docker needs stage 5 before stages 6–7.
