# Subagent model strategy

Each specialist under `.cursor/agents/` **must** pin an explicit `model` slug in YAML
frontmatter. **`inherit` is forbidden.**

> **Active policy (2026-07-15):** **All project specialists pin `cursor-grok-4.5-high-fast`.**
> Single-model fleet for production consistency — no Composer / GLM tier split.
> Do **not** pin `glm-5.2-*` (Cursor API pool). Do **not** use `inherit`.

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
3. **Built-in Cursor types** (`explore`, `bugbot`) — no project frontmatter; Cursor-owned.
4. **Region / availability failure** — if `cursor-grok-4.5-high-fast` is rejected, surface the error to the user; do **not** silently fall back to Composer/GLM without confirmation.
5. **No `inherit`** — ever.

## Fallback (only with user confirmation)

Preferred order if Grok is unavailable in-region:

1. User-approved alternate first-party slug
2. Stop and report — do not invent a fleet-wide Composer rollback

## Supervisor mode

User stays in one parent session; parent/orchestrator spawn `Task` workers autonomously.
Parallel → worktree-router; Docker → queue; isolated Done → integration-merger then
doc-sync/commit on **main**.
