# Subagent model strategy

Each specialist under `.cursor/agents/` **must** pin an explicit `model` slug in YAML
frontmatter. **`inherit` is forbidden.**

> **Active policy (2026-07-15):** **All project specialists pin `cursor-grok-4.5-high-fast`.**
> Single-model fleet for production consistency — no Composer / GLM tier split.
> Do **not** pin `glm-5.2-*` (Cursor API pool). Do **not** use `inherit`.
>
> **Orthogonal:** Model pin ≠ Task enum availability. If Cursor does not expose project
> agent names on `Task`, follow `.cursor/skills/specialist-runtime-fallback/SKILL.md`
> (still keep this model pin on agent frontmatter; do not silently switch Composer/GLM).

## Canonical pin

| Slug | Role |
| --- | --- |
| **`cursor-grok-4.5-high-fast`** | **Every** `.cursor/agents/*.md` specialist |
| **`inherit`** | **Forbidden** |
| **`composer-2.5` / `composer-2.5-fast`** | **Retired** for project agents (do not reintroduce without explicit user request) |
| **`glm-5.2-*`** | **Avoid** (API pool) |

## Role tiers (capability, not model)

Tiers describe **pipeline responsibility** only. Model slug is identical for all.

| Tier | Responsibility | Agents |
| --- | --- | --- |
| **Governance** | Routing, plan, architecture, quality, merge, commit | `delivery-orchestrator`, `plan-orchestrator`, `architecture-reviewer`, `code-quality-reviewer`, `integration-merger`, `post-task-commit-review` |
| **Delivery** | BDD, docs, TDD implementation, E2E, doc-sync | `behavior-spec-author`, `doc-keeper`, `backend-engineer`, `frontend-engineer`, `rendering-engineer`, `e2e-test-engineer`, `e2e-uiux-reviewer`, `post-task-doc-sync` |
| **Execution** | Worktree, gates, deploy queue, verify | `worktree-router`, `build-deploy-agent`, `deploy-engineer`, `verifier` |

## Current assignments

| Agent | Tier | Model |
| --- | --- | --- |
| `delivery-orchestrator` | Governance | `cursor-grok-4.5-high-fast` |
| `plan-orchestrator` | Governance | `cursor-grok-4.5-high-fast` |
| `architecture-reviewer` | Governance | `cursor-grok-4.5-high-fast` |
| `code-quality-reviewer` | Governance | `cursor-grok-4.5-high-fast` |
| `post-task-commit-review` | Governance | `cursor-grok-4.5-high-fast` |
| `integration-merger` | Governance | `cursor-grok-4.5-high-fast` |
| `behavior-spec-author` | Delivery | `cursor-grok-4.5-high-fast` |
| `doc-keeper` | Delivery | `cursor-grok-4.5-high-fast` |
| `backend-engineer` | Delivery | `cursor-grok-4.5-high-fast` |
| `frontend-engineer` | Delivery | `cursor-grok-4.5-high-fast` |
| `rendering-engineer` | Delivery | `cursor-grok-4.5-high-fast` |
| `e2e-test-engineer` | Delivery | `cursor-grok-4.5-high-fast` |
| `e2e-uiux-reviewer` | Delivery | `cursor-grok-4.5-high-fast` |
| `post-task-doc-sync` | Delivery | `cursor-grok-4.5-high-fast` |
| `verifier` | Execution | `cursor-grok-4.5-high-fast` |
| `worktree-router` | Execution | `cursor-grok-4.5-high-fast` |
| `build-deploy-agent` | Execution | `cursor-grok-4.5-high-fast` |
| `deploy-engineer` | Execution | `cursor-grok-4.5-high-fast` |

## Production rules

1. **Pin in frontmatter** — every agent file has `model: cursor-grok-4.5-high-fast`.
2. **Parent `Task` calls** — do **not** pass `model` unless the user explicitly requests a different slug in the same session.
3. **Built-in Cursor Task types** — no project frontmatter; Cursor-owned. Observed in
   live sessions (set may grow/shrink by product version):
   - **Routing-relevant:** `explore`, `bugbot`
   - **Also commonly present:** `generalPurpose`, `shell`, `cursor-guide`,
     `ci-investigator`, `security-review`, `best-of-n-runner`, plus plugin agents such as
     `usage-query-agent` / `case-feedback-agent` when those plugins are enabled
   - **Not** in this built-in list: the **18** project specialists under `.cursor/agents/`
     — those appear on `Task` only when Cursor injects them for the session
4. **Region / availability failure** — if `cursor-grok-4.5-high-fast` is rejected, surface the error to the user; do **not** silently fall back to Composer/GLM without confirmation.
5. **No `inherit`** — ever.
6. **Enum vs files** — file existence under `.cursor/agents/` does **not** guarantee the
   name is in the live Task enum. Runtime policy:
   `.cursor/skills/specialist-runtime-fallback/SKILL.md` (retry ≤3 → GP under contract;
   forbid with `禁止降级` / `no-gp-fallback`; early opt-in `allow-gp-fallback` / `允许降级`).

## Fallback

Preferred order if Grok is unavailable in-region:

1. User-approved alternate first-party slug
2. Stop and report — do not invent a fleet-wide Composer rollback

Preferred order if project specialist Task type is unavailable:

1. Retry named type (≤3) / recover session (reload, new chat, correct workspace root)
2. After budget / ENUM_MISSING confirm → GP or inline under injected contract
3. If user said `禁止降级` / `no-gp-fallback` → remain **BLOCKED** — do not invent Done

## Supervisor mode

User stays in one parent session; parent/orchestrator spawn `Task` workers autonomously.
Parallel → worktree-router; Docker → queue; isolated Done → integration-merger then
doc-sync/commit on **main**.
