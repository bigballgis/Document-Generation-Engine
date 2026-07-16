---
name: specialist-runtime-fallback
description: Retry-first specialist routing when Cursor Task fails or project agents are missing from the enum. Default is retry then BLOCKED — no automatic generalPurpose downgrade unless the user explicitly opts in (allow-gp-fallback / 允许降级).
---

# Specialist Runtime — Retry First (no silent downgrade)

**Owner:** parent agent + `delivery-orchestrator`  
**Behavior SoT:** [docs/behavior/specialist-runtime-fallback.md](../../../docs/behavior/specialist-runtime-fallback.md)

**Policy (2026-07-16, user confirmed):** Prefer **retry**, not **downgrade**.
Automatic `generalPurpose` fallback is **off** unless the user says `allow-gp-fallback` /
`允许降级` in the same session.

## Problem

Project specialists live in `.cursor/agents/*.md`. Cursor’s `Task` tool may:

1. **API flake** — type exists but call fails (`ENOTFOUND`, timeout, `unavailable`)
2. **ENUM_MISSING** — session only exposes built-ins (`generalPurpose`, `explore`, …)

Downgrading to `generalPurpose` hides the real failure and weakens gates. This skill
requires **honest retry**, then **BLOCKED** with recovery hints.

## Decision ladder (mandatory)

1. **Native** — `Task(subagent_type=<requested project agent>)` when the enum includes it.
2. **Retry (API flake)** — on transport / `ENOTFOUND` / `unavailable` / timeout:
   - Retry the **same** `subagent_type` up to **3** total attempts (initial + 2 retries)
   - Space retries briefly (a few seconds); do not change the type or invent GP
   - Record `retry_count` in `runtime_routing`
3. **ENUM_MISSING** — if the name is **not** in the Task enum:
   - Retrying the same missing name will not help
   - Emit `BLOCKED` with recovery hints (below) — **do not** auto-switch to GP
4. **After retries exhausted / enum still missing** → `mode: BLOCKED`
   - Stop delivery writes (except safe read-only diagnosis)
   - Tell the user clearly; do **not** claim Done
5. **Opt-in downgrade only** — if user said `allow-gp-fallback` / `允许降级`:
   - Then and only then may use `FALLBACK_GENERAL_PURPOSE` with full contract injection
   - Or documented `INLINE_CHECKLIST` for agents that authorize it
6. **Never** treat GP/inline as success of the named specialist. Emit `runtime_routing`.

## Recovery hints (for BLOCKED)

Surface these to the user (pick what fits the facts):

- New Agent chat / reload window so `.cursor/agents` re-registers on Task
- Confirm workspace root is the repo (or intended feature worktree), not a stale path
- Wait and retry when `api2.cursor.sh` / Task API is down
- Same-session opt-in: `允许降级` / `allow-gp-fallback` if they accept GP under contract

## Contract injection (only when user opted into GP)

Same as before — prompt must bind `.cursor/agents/<requested>.md` + skills + pipeline;
emit `runtime_routing` with `mode: FALLBACK_GENERAL_PURPOSE` and
`user_opt_in: allow-gp-fallback`.

## Mandatory output block

```
runtime_routing:
  mode: NATIVE_SPECIALIST | RETRYING | BLOCKED | FALLBACK_GENERAL_PURPOSE | INLINE_CHECKLIST
  requested_subagent: <name>
  actual_subagent: <name or none>
  reason: ENUM_MISSING | API_UNAVAILABLE | TASK_REJECTED | NONE
  retry_count: <0..3>
  retry_attempted: true | false
  user_opt_in_gp: true | false
  user_visible_note: <one line>
```

Routing line examples:

- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=RETRYING (1/3)`
- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=BLOCKED (API_UNAVAILABLE after 3 attempts)`
- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=FALLBACK_GENERAL_PURPOSE (user allow-gp-fallback)`

## Forbidden

| Action | Status |
| --- | --- |
| Auto `generalPurpose` without user `allow-gp-fallback` / `允许降级` | **Forbidden** |
| Lazy GP when named specialist is in the enum | **Forbidden** |
| Skipping worktree / Batch Recommendation / gates because of BLOCKED | **Forbidden** (stop instead) |
| Claiming named `Task(specialist)` succeeded after GP/inline | **Forbidden** |
| Infinite retry loops with no cap | **Forbidden** (cap = 3 attempts) |

## Limits (honest)

- **API flake** — retry often recovers (as with `api2.cursor.sh` blips).
- **ENUM_MISSING** — retry cannot invent enum entries; only session/workspace recovery
  or explicit user opt-in GP can unblock.
- This skill cannot force Cursor to always inject `.cursor/agents` into Task.

## Related

- Routing: `.cursor/rules/subagent-routing-mandate.mdc`
- Orchestrator: `.cursor/agents/delivery-orchestrator.md`
- Pipeline: `.cursor/skills/delivery-pipeline/SKILL.md`
