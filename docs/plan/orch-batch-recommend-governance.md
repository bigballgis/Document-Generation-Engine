# Delivery Batch Recommendation — governance note

| Field | Value |
| --- | --- |
| Slice | `orch-batch-recommend` |
| Date | 2026-07-16 |
| Formal phase | **None** (agent/governance — not a P-phase; not CE-O01) |
| Status | **Done** (2026-07-16) |

## Purpose

Add **pre-stage-0 Batch Recommendation** so `delivery-orchestrator` can intentionally
merge **related** pending tasks into **one** delivery leaf and amortize fixed pipeline
cost (worktree → gates → E2E → Docker queue → merge → doc-sync).

This is **not** multi-writer parallel. Single-lane serial (§9.2) and
`docker-deploy-queue.ps1` remain mandatory. `force-parallel` stays opt-in only.

## Anchors

| Artifact | Path |
| --- | --- |
| Skill | `.cursor/skills/delivery-batch-recommend/SKILL.md` |
| Pipeline | `.cursor/skills/delivery-pipeline/SKILL.md` (stage −1) |
| Agent | `.cursor/agents/delivery-orchestrator.md` |
| Behavior | [docs/behavior/delivery-batch-recommend.md](../behavior/delivery-batch-recommend.md) |
| Constitutions | `delivery-orchestration-constitution.mdc`, `subagent-routing-mandate.mdc`, `worktree-and-deploy-queue-constitution.mdc` |
| Index | `AGENTS.md`, `docs/README.md`, `docs/plan/README.md` |

## Caps (v1)

- Member tasks ≤ 3
- Module domains ≤ 2
- Prefer split/solo if projected files > 25
- Never merge into an In Progress sole-active leaf

## Explicit non-goals

- Selective tests / risk-tier E2E skip (Future)
- Activating Task Master **#81** CE-O01
