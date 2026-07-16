---
name: specialist-runtime-fallback
description: Formal runtime fallback when Cursor Task enum lacks project specialists or Task API fails — use generalPurpose (or documented inline checklist) under the requested agent contract with auditable runtime_routing. Use on every ENUM_MISSING / API_UNAVAILABLE before improvising.
---

# Specialist Runtime Fallback

**Owner:** parent agent + `delivery-orchestrator`  
**Behavior SoT:** [docs/behavior/specialist-runtime-fallback.md](../../../docs/behavior/specialist-runtime-fallback.md)

## Problem

Project specialists live in `.cursor/agents/*.md` and routing rules name them
(`delivery-orchestrator`, `backend-engineer`, …). Cursor’s `Task` tool enum sometimes
exposes **only built-ins** (`generalPurpose`, `explore`, `shell`, `bugbot`, …). Agents then
say “specialist enum unavailable” and silently improvise — or stop.

This skill makes the degradation **explicit, ordered, and contract-bound**.

## Decision ladder (mandatory)

1. **Prefer native** — `Task(subagent_type=<requested project agent>)` when the enum
   includes that name.
2. **One retry** — if first call fails with transport/`ENOTFOUND`/`unavailable`, retry
   **once** with the same `subagent_type` (short wait OK).
3. **Classify failure**
   - `ENUM_MISSING` — name not in current Task enum
   - `API_UNAVAILABLE` — enum may exist but Task/subagent API fails (DNS, 5xx, timeout)
   - `TASK_REJECTED` — Cursor rejected the type for another stated reason
4. **Fallback (in order)**
   - **A.** `Task(subagent_type=generalPurpose)` with **full contract injection** (below)
   - **B.** Documented **inline checklist** only for agents that already authorize it
     (`worktree-router`, `integration-merger`, `verifier`, and checklist-style
     `post-task-doc-sync` / `post-task-commit-review` when the agent file says so)
   - **C.** `BLOCKED` — stop and report; do **not** invent a second compose stack or skip
     gates to “look Done”
5. **Never** treat fallback as success of the named specialist. Emit `runtime_routing`.

## Contract injection (required for FALLBACK_GENERAL_PURPOSE)

The `generalPurpose` Task prompt **must** include:

1. `You are acting as <requested_subagent> under FALLBACK_GENERAL_PURPOSE.`
2. Absolute paths to read and obey:
   - `.cursor/agents/<requested_subagent>.md`
   - Owning skill(s) named in that agent file
   - `.cursor/skills/delivery-pipeline/SKILL.md` (if delivery)
   - `.cursor/skills/delivery-batch-recommend/SKILL.md` (if deliver entry / orchestrator)
3. The stage handoff payload (including `batch_recommendation` when applicable).
4. Hard constraints: single-lane serial; one Docker queue; no fake Done; emit
   `runtime_routing` in the final report.
5. `requested_subagent` + `reason` + `retry_attempted`.

Do **not** pass a different `model` unless the user explicitly requested one.

## Mandatory output block

Emit whenever fallback or inline checklist is used (and optionally when native succeeds
as `NATIVE_SPECIALIST` for audit):

```
runtime_routing:
  mode: NATIVE_SPECIALIST | FALLBACK_GENERAL_PURPOSE | INLINE_CHECKLIST | BLOCKED
  requested_subagent: <name>
  actual_subagent: <name or parent-inline>
  reason: ENUM_MISSING | API_UNAVAILABLE | TASK_REJECTED | NONE
  contract_sources: [.cursor/agents/....md, skills...]
  retry_attempted: true | false
  user_visible_note: <one line>
```

Routing line example:

`[routing] intent=deliver → subagent=delivery-orchestrator → runtime=FALLBACK_GENERAL_PURPOSE (ENUM_MISSING)`

## What is still forbidden

| Action | Status |
| --- | --- |
| `generalPurpose` when the enum **includes** the named specialist | **Forbidden** (lazy routing) |
| Skipping worktree / Batch Recommendation / doc-sync because of fallback | **Forbidden** |
| Claiming `Task(delivery-orchestrator)` ran when it was GP | **Forbidden** |
| Using fallback to justify `force-parallel` or second Docker stack | **Forbidden** |
| Silent parent multi-file product implementation without even GP contract | **Forbidden** |

## Built-in Cursor types

`explore` / `bugbot` remain preferred for their niches when available — they are not
project specialists and do not need this fallback ladder.

## Related

- Routing: `.cursor/rules/subagent-routing-mandate.mdc`
- Orchestrator: `.cursor/agents/delivery-orchestrator.md`
- Pipeline: `.cursor/skills/delivery-pipeline/SKILL.md`
- Model pin (orthogonal): `.cursor/agents/MODEL-STRATEGY.md`
