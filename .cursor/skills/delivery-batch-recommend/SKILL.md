---
name: delivery-batch-recommend
description: Pre-stage-0 Batch Recommendation — decide merge|solo|split of related tasks into one delivery leaf to amortize fixed pipeline cost. Use at every deliver entry before worktree-router. Not multi-writer parallel.
---

# Delivery Batch Recommendation (pre-0 / stage −1)

**Owner:** `delivery-orchestrator` (checklist runs in-orchestrator; do **not** spawn a
parallel routing agent). Consult `plan-orchestrator` only when Task Master / queue facts
are unclear.

**Behavior SoT:** [docs/behavior/delivery-batch-recommend.md](../../../docs/behavior/delivery-batch-recommend.md)

**Pipeline position:** **Before** stage **0** `worktree-router`. Every `/deliver` or
auto-mapped deliver intent **must** emit a `batch_recommendation` block first.

## Problem this solves

This repo’s delivery leaf has high **fixed cost** (worktree → BDD → TDD → full verify →
E2E → Docker queue → merge → doc-sync → commit). Small unrelated changes that each pay
the full tax waste host capacity. The remedy is **intentional related merge into one
leaf** — amortize evidence — **not** multi-writer `force-parallel`.

## Research synthesis (project-adapted)

| Industry idea | How we apply it |
| --- | --- |
| **High transaction-cost / PR trap** | People unconsciously inflate PRs when CI is expensive. We **consciously** merge **related** work to share one evidence run, while keeping the changeset reviewable and rollback-scoped. |
| **Merge queue (Trunk / Graphite-style)** | Related changes share one heavy verify/E2E/deploy. Keep `member_task_ids` inside the batch so red gates can **attribute** and **split/bisect** — never pretend one fuzzy mega-story. |
| **MinimumCD warning** | Do not smash unrelated changes into an undebuggable blob. Hard caps + vetoes keep integration thin. |
| **DORA small batches** | Batching amortizes **execution and evidence**, not BDD clarity. Each behavior scenario stays traceable; do not blur multiple unclear behaviors into one BDD. |
| **Future (out of v1)** | Selective tests / risk-tier skip E2E — **not** this skill. Mark only. |

## Mandatory output (every deliver entry)

```
batch_recommendation:
  decision: merge | solo | split
  rationale: <fact-based; never speculative>
  member_task_ids: [...]
  proposed_slice_id: <single leaf id>
  shared_acceptance_surface: <same E2E journey / same OpenAPI contract / same module …>
  vetoes_applied: [...]
  evidence_amortization: <gates this batch will share: verify / E2E / deploy …>
  on_red_split_hint: <how to split back to solo on failure>
```

Copy the block into the handoff payload for every downstream `Task`.

## Fact inputs (required — no invention)

Gather from the repo before deciding:

1. **Task Master / plan queue** — dependencies, queue head, pending neighbors (`tasks.json` / MCP / `docs/plan/`).
2. **Path overlap** — same `frontend/src/...` subtree, same backend `com.bank.docgen.<module>`, same behavior doc.
3. **Shared acceptance surface** — same user journey, same OpenAPI contract, same Flyway migration chain.
4. **Same fixed-cost class** — do all candidates need E2E + queued deploy?
5. **Sole-active / worktree occupancy** — `git worktree list`; Docker lock status. Never merge into an In Progress leaf.
6. **Behavior clarity** — if acceptance is unclear → BDD first (`decision` stays solo/split for those ids); do not batch unclear work.

## Decision rules

### must-merge (should combine)

- **Hard dependency:** A cannot be tested/accepted without B landing.
- **Same BDD acceptance surface** — adjacent defects / reinforcements of one journey.
- **Same E2E journey** would be re-run identically if split into separate PRs.

### may-merge (optional cost amortization)

- Same module / low coupling; projected members and domains within hard caps.
- Same doc-sync / ledger close-out surface.

### must-solo / veto (forbid merge)

| Veto | Example |
| --- | --- |
| Unrelated risk domains | Permission model vs rendering kernel; breaking Flyway vs pure UI copy |
| Hard cap exceeded | See caps below |
| Sole-active / Docker lock held by another leaf | **Queue** next leaf; do **not** fold into the running one |
| Unclear behavior | Stage 1 BDD first; no batch until ready |
| Parked unrelated worktree | e.g. do not absorb CE-O01 PDF/A into an unrelated governance slice |

### Hard caps (v1 defaults — tune in this skill only)

| Cap | Default | On exceed |
| --- | --- | --- |
| Member tasks | **≤ 3** | `split` or `solo` |
| Estimated module domains | **≤ 2** (backend module **or** FE route domain) | `split` / `solo` |
| Diff reviewability | Heuristic **> 25 files** → prefer `split` / `solo` | Avoid “cannot review at a glance” mega-leaf |

### Decision semantics

| `decision` | Meaning |
| --- | --- |
| **`merge`** | Multiple `task_ids` → **one** `proposed_slice_id` / **one** worktree / **one** run of stages 0–13 |
| **`solo`** | Single task (or veto forced) — normal one-leaf deliver |
| **`split`** | User/queue handed a too-large bag → break into **multiple serial** leaves (still single-lane; **not** parallel writers) |

## Checklist (orchestrator — before stage 0)

1. List candidate `task_ids` and parked worktrees.
2. Collect fact inputs (above).
3. Apply vetoes → record `vetoes_applied`.
4. Apply must-merge / may-merge within caps.
5. Choose `merge` | `solo` | `split`; set `proposed_slice_id` and `on_red_split_hint`.
6. Emit `batch_recommendation` in the orchestration report **and** handoff.
7. Only then call `worktree-router` for the **single** chosen leaf (or queue the first split leaf).

## Red-gate split hint

When `decision: merge`, always populate `on_red_split_hint`, e.g.:

- “If verify fails in module X, peel `task_id=B` to a new solo leaf; keep A.”
- “If E2E journey J fails, bisect by re-running with only `member_task_ids[0]`.”

Never hide member ids inside a vague story title.

## Explicit non-goals (v1)

- **Not** multi-writer parallel / not a substitute for `force-parallel`.
- **Not** selective test skipping or risk-tier E2E bypass (Future).
- **Not** merging into an already In Progress sole-active leaf.
- **Not** inventing product facts — decisions must cite repo facts.

## Related

- Pipeline: `.cursor/skills/delivery-pipeline/SKILL.md` (stage −1 + handoff)
- Agent: `.cursor/agents/delivery-orchestrator.md`
- Constitutions: `delivery-orchestration-constitution.mdc`, `subagent-routing-mandate.mdc`,
  `worktree-and-deploy-queue-constitution.mdc`
- Command: `.cursor/commands/deliver.md`
- Behavior: `docs/behavior/delivery-batch-recommend.md`
