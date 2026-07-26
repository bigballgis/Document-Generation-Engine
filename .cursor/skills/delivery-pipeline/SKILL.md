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

## Specialist runtime (retry first)

When `Task` lacks a project specialist name or the subagent API fails, follow
`.cursor/skills/specialist-runtime-fallback/SKILL.md`: **retry** named type (≤3), then
**BLOCKED**. Auto GP is off unless user `allow-gp-fallback` / `允许降级`. Emit
`runtime_routing`. Does **not** authorize skipping gates.

Behavior: [docs/behavior/specialist-runtime-fallback.md](../../../docs/behavior/specialist-runtime-fallback.md).

## Handoff payload (copy into every Task prompt)

```
task_ids:
bdd_readiness: ready | blocked | not-applicable
placement: ISOLATED   # mandatory for delivery; MAIN only for read-only / main-only opt-out
worktree_path:
branch:
delivery_lane: full | light
delivery_lane_rationale: <cite BDD surface flags + light-lane E1–E5 when light>
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
runtime_routing:
  mode: NATIVE_SPECIALIST | RETRYING | BLOCKED | FALLBACK_GENERAL_PURPOSE | INLINE_CHECKLIST
  requested_subagent:
  actual_subagent:
  reason: ENUM_MISSING | API_UNAVAILABLE | TASK_REJECTED | NONE
  retry_count: 0
  retry_attempted: false
  user_opt_in_gp: false
  user_visible_note:
```

## Delivery lane (`full` | `light`)

Default **`full`**. Skill: `.cursor/skills/lightweight-delivery-lane/SKILL.md`.
Behavior: [lightweight-delivery-lane.md](../../../docs/behavior/lightweight-delivery-lane.md).

| Lane | Stages 5–7 (E2E) + 10 (Docker evidence) | Worktree (stage 0) | Unit/verify gates |
| --- | --- | --- | --- |
| **`full`** | Required when UI/runtime acceptance surfaces change (as today) | Mandatory for delivery (unless user `main-only`) | Required when code touched |
| **`light`** | **May skip** — record **N/A** + rationale | **Still mandatory** for multi-file delivery; light ≠ main-only | **Still required** when code touched |

**Light eligibility (all must hold):** BDD proves no management-UI acceptance surface;
BDD proves no runtime/OpenAPI/Flyway/generation acceptance (or pure docs/governance);
readiness `ready`/`not-applicable`; handoff records `delivery_lane: light` + rationale;
worktree rule still obeyed.

**Must not:** use light to weaken product behavior leaves; skip E2E for “flaky” without
BDD N/A proof; treat Batch Recommendation `merge` as light eligibility; claim Playwright
or deploy greens that were not run. Doubt → **`full`**. Mid-flight UI/runtime scope creep
→ upgrade to **`full`** and run skipped stages before Done.

## Session worktree rule (mandatory)

Every delivery session: stage −1 Batch Recommendation → stage 0 → code in
`../DGE-<slice-id>` → merge via `integration-merger` → **doc-sync + commit on MAIN**.

`delivery_lane: light` does **not** authorize implementing on MAIN. MAIN / `main-only`
remains an explicit user opt-out (or read-only / single mechanical edit) independent of lane.

## Single-lane serial (default, 2026-07-16)

At most **one** delivery leaf In Progress on this host. Queue other slices; do not
fan out writers for “continue / 自动执行后续”. Override only with user
`force-parallel` / `强制并行` (cap ≤2; still queue Docker).

Batch Recommendation may **merge** related tasks into that one leaf; it must **never**
create a second concurrent writer.

## Docker

Always `.\scripts\docker-deploy-queue.ps1` (never parallel stacks).
E2E docker needs stage 5 before stages 6–7.
On `delivery_lane: light` with eligibility, stage 5 and stage 10 are **N/A** (do not
queue a deploy “for completeness”).
