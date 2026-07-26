---
name: lightweight-delivery-lane
description: Eligibility-gated delivery_lane light|full — skip E2E+Docker (stages 5–7, 10) only when BDD proves no user-facing/runtime acceptance surface. Use when classifying evidence path for docs/governance or unit-only leaves. Does not weaken full product leaves or waive worktree.
---

# Lightweight Delivery Lane

Behavior SoT: [docs/behavior/lightweight-delivery-lane.md](../../../docs/behavior/lightweight-delivery-lane.md).

Wire into orchestrator handoffs via
[delivery-pipeline](../delivery-pipeline/SKILL.md) and
`delivery-orchestration-constitution.mdc`.

## Classify `delivery_lane`

| Value | When |
| --- | --- |
| **`full`** | Default. Any management-UI journey/visual change, OpenAPI/Flyway/runtime generation acceptance, permission-matrix / fail-closed authz semantics, or **doubt**. |
| **`light`** | **Only** when **all** eligibility checks E1–E5 hold (below). |

### Eligibility (all must hold)

| # | Criterion |
| --- | --- |
| E1 | BDD proves **no** management-UI user journey / visual acceptance change (`frontend_ui_in_scope=false` or UI out of scope) |
| E2 | BDD proves **no** runtime / OpenAPI / Flyway / generation acceptance change **or** leaf is pure docs/agent-governance with zero product code |
| E3 | Stage 1 readiness is `ready` or `not-applicable` (not `blocked`) |
| E4 | Handoff records `delivery_lane: light` + rationale quoting E1–E3 |
| E5 | Worktree still obeyed for multi-file delivery (**light ≠ main-only**) |

If any fails → **`full`**.

## What light may skip

| Stages | Light |
| --- | --- |
| **5, 6, 7** (E2E stack prep / functional / UIUX) | **Skip** — record **N/A** + rationale |
| **10** (queued Docker deploy evidence) | **Skip** — record **N/A** + rationale |

## What light must **not** skip

- Stage **−1** Batch Recommendation, **0** worktree (unless user `main-only` / `no-worktree`), **1** BDD
- Applicable **unit/verify** gates when Java/TS code is touched (`mvn verify` / frontend lint·type-check·test·build)
- Stages **11–13** (merge when ISOLATED, doc-sync, commit-review)
- Security-sensitive leaves → treat as **`full`** (E2 fails)

## Worktree vs main-only (independent of lane)

| Placement | When |
| --- | --- |
| **ISOLATED worktree** | Default for multi-file / behavior-doc delivery — **including `light`** |
| **MAIN / `main-only`** | Read-only Q&A; single mechanical one-line edit; or explicit user opt-out |
| Light lane | **Does not** authorize coding on MAIN |

## Forbidden

- Using light lane to weaken product UI / runtime leaves
- Skipping E2E because “flaky” without BDD N/A proof
- Citing Batch Recommendation `merge` alone as light eligibility
- Claiming Playwright/Docker greens that were not run
- Inventing a second Docker compose project

## Handoff fields

```
delivery_lane: full | light
delivery_lane_rationale: <cite BDD surface flags + E1–E5>
evidence_amortization: <e.g. docs lint/consistency; E2E/Docker N/A>
```

Mid-flight scope creep that adds UI/runtime surface → **upgrade to `full`** and run previously skipped stages before Done.
