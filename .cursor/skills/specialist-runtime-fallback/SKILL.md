---
name: specialist-runtime-fallback
description: Retry-first specialist routing when Cursor Task fails or project agents are missing from the enum. Retry named specialist up to 3 attempts, then allow generalPurpose / inline downgrade with full contract injection unless the user forbids it (no-gp-fallback / 禁止降级).
---

# Specialist Runtime — Retry then GP Downgrade

**Owner:** parent agent + `delivery-orchestrator`  
**Behavior SoT:** [docs/behavior/specialist-runtime-fallback.md](../../../docs/behavior/specialist-runtime-fallback.md)

**Policy (2026-07-16, user confirmed):** Prefer **retry**, then **allow downgrade**.
After the retry budget is exhausted (or `ENUM_MISSING` is confirmed), use
`FALLBACK_GENERAL_PURPOSE` / `INLINE_CHECKLIST` with full contract injection.
User may forbid downgrade with `禁止降级` / `no-gp-fallback` (then `BLOCKED` + recovery hints).
Early opt-in `allow-gp-fallback` / `允许降级` may skip waiting for the full budget.

## Problem

Project specialists live in `.cursor/agents/*.md`. Cursor’s `Task` tool may:

1. **API flake** — type exists but call fails (`ENOTFOUND`, timeout, `unavailable`)
2. **ENUM_MISSING** — session only exposes built-ins (`generalPurpose`, `explore`, …)

Silent GP without a budget hides flakes; infinite `BLOCKED` without a downgrade path
stalls delivery when the enum cannot be recovered in-session. This skill requires
**honest retry**, then **allowed downgrade under contract**.

## Decision ladder (mandatory)

1. **Native** — `Task(subagent_type=<requested project agent>)` when the enum includes it.
2. **Retry (API flake)** — on transport / `ENOTFOUND` / `unavailable` / timeout:
   - Retry the **same** `subagent_type` up to **3** total attempts (initial + 2 retries)
   - Space retries briefly (a few seconds); do not invent GP mid-budget
   - Record `retry_count` in `runtime_routing`
3. **ENUM_MISSING** — if the name is **not** in the Task enum:
   - Do not burn three identical doomed calls
   - One confirmation is enough to enter the post-retry downgrade path
4. **After retries exhausted / ENUM_MISSING confirmed** → downgrade allowed:
   - Default: `FALLBACK_GENERAL_PURPOSE` (or `INLINE_CHECKLIST` when the agent doc allows)
   - Inject `.cursor/agents/<requested>.md` + owning skills + pipeline constraints
   - Emit `runtime_routing` (`mode: FALLBACK_GENERAL_PURPOSE` or `INLINE_CHECKLIST`)
5. **Forbid downgrade** — if user said `禁止降级` / `no-gp-fallback`:
   - Stay `mode: BLOCKED` + recovery hints; do not spawn GP
6. **Early opt-in** — if user said `allow-gp-fallback` / `允许降级` before budget spent:
   - May downgrade immediately; set `user_opt_in_gp: true`
7. **Never** treat GP/inline as success of the named specialist. Emit `runtime_routing`.
8. **Never** use GP for convenience while the named specialist **is** in the enum.

## Recovery hints (when BLOCKED or before downgrade)

Surface when useful:

- New Agent chat / reload window so `.cursor/agents` re-registers on Task
- Confirm workspace root contains `.cursor/agents/` (orphan worktree roots often do not)
- Wait and retry when `api2.cursor.sh` / Task API is down
- Same-session: `禁止降级` / `no-gp-fallback` to refuse GP; or reload to prefer native again

## Contract injection (mandatory on GP / inline)

Prompt must bind:

- `.cursor/agents/<requested>.md`
- Relevant skills (delivery-pipeline, worktree-isolation or `main-only` placement, TDD, etc.)
- Stage order and Done definition (no skipped gates)

Emit:

```
runtime_routing:
  mode: FALLBACK_GENERAL_PURPOSE | INLINE_CHECKLIST
  requested_subagent: <name>
  actual_subagent: generalPurpose | inline
  reason: ENUM_MISSING | API_UNAVAILABLE | TASK_REJECTED
  retry_count: <0..3>
  retry_attempted: true | false
  user_opt_in_gp: true | false
  user_visible_note: <one line>
```

## Mandatory output block

```
runtime_routing:
  mode: NATIVE_SPECIALIST | RETRYING | BLOCKED | FALLBACK_GENERAL_PURPOSE | INLINE_CHECKLIST
  requested_subagent: <name>
  actual_subagent: <name or generalPurpose or inline or none>
  reason: ENUM_MISSING | API_UNAVAILABLE | TASK_REJECTED | NONE
  retry_count: <0..3>
  retry_attempted: true | false
  user_opt_in_gp: true | false
  user_visible_note: <one line>
```

Routing line examples:

- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=RETRYING (1/3)`
- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=FALLBACK_GENERAL_PURPOSE (after 3 API retries)`
- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=FALLBACK_GENERAL_PURPOSE (ENUM_MISSING confirmed)`
- `[routing] intent=deliver → subagent=delivery-orchestrator → runtime=BLOCKED (user 禁止降级)`

## Forbidden

| Action | Status |
| --- | --- |
| Lazy `generalPurpose` while named specialist **is** in the enum | **Forbidden** |
| Claiming named `Task(specialist)` succeeded after GP/inline | **Forbidden** |
| Skipping Batch Recommendation / worktree-or-main-only placement / gates on downgrade | **Forbidden** |
| Infinite retry loops with no cap | **Forbidden** (cap = 3 attempts) |
| GP after budget when user said `禁止降级` / `no-gp-fallback` | **Forbidden** |

## Limits (honest)

- **API flake** — retry often recovers.
- **ENUM_MISSING** — retry cannot invent enum entries; downgrade unblocks under contract;
  reload / correct workspace root still preferred for native specialists.
- Worktree cleanup that leaves the chat on an orphan root without `.cursor/agents/`
  commonly empties the project enum until Open Folder / Reload / new chat.

## Related

- Routing: `.cursor/rules/subagent-routing-mandate.mdc`
- Orchestrator: `.cursor/agents/delivery-orchestrator.md`
- Pipeline: `.cursor/skills/delivery-pipeline/SKILL.md`
