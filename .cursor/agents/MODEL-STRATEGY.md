# Subagent model strategy

Each specialist under `.cursor/agents/` **must** pin an explicit `model` slug in YAML
frontmatter. **`inherit` is forbidden.**

> **Active policy (no Cursor API pool):** Prefer Cursor first-party families only
> (**Grok** + **Composer**). Do **not** pin `glm-5.2-*` — GLM Coding Plan still
> deducts Cursor API quota in this workspace. Claude API-token slugs stay frozen
> until quota returns.

## Allowed families (current)

| Family | Slugs | Role |
| --- | --- | --- |
| **Grok 4.5 High Fast** | `grok-4.5-fast-xhigh` | Governance |
| **Composer 2.5** | `composer-2.5` | Delivery |
| **Composer 2.5 Fast** | `composer-2.5-fast` | Execution |
| **`inherit`** | — | **Forbidden** |
| **GLM 5.2** | `glm-5.2-high` etc. | **Avoid** (API pool) |

## Tiers

| Tier | Slug | When |
| --- | --- | --- |
| **Governance** | `grok-4.5-fast-xhigh` | Routing, plan, architecture, commit, merge |
| **Delivery** | `composer-2.5` | BDD, docs, TDD implementation, E2E, doc-sync |
| **Execution** | `composer-2.5-fast` | Gates, deploy queue, worktree placement |

## Current assignments

| Agent | Tier | Model |
| --- | --- | --- |
| `delivery-orchestrator` | Governance | `grok-4.5-fast-xhigh` |
| `plan-orchestrator` | Governance | `grok-4.5-fast-xhigh` |
| `architecture-reviewer` | Governance | `grok-4.5-fast-xhigh` |
| `code-quality-reviewer` | Governance | `grok-4.5-fast-xhigh` |
| `post-task-commit-review` | Governance | `grok-4.5-fast-xhigh` |
| `integration-merger` | Governance | `grok-4.5-fast-xhigh` |
| `behavior-spec-author` | Delivery | `composer-2.5` |
| `doc-keeper` | Delivery | `composer-2.5` |
| `backend-engineer` | Delivery | `composer-2.5` |
| `frontend-engineer` | Delivery | `composer-2.5` |
| `rendering-engineer` | Delivery | `composer-2.5` |
| `e2e-test-engineer` | Delivery | `composer-2.5` |
| `e2e-uiux-reviewer` | Delivery | `composer-2.5` |
| `post-task-doc-sync` | Delivery | `composer-2.5` |
| `verifier` | Execution | `composer-2.5-fast` |
| `worktree-router` | Execution | `composer-2.5-fast` |
| `build-deploy-agent` | Execution | `composer-2.5-fast` |
| `deploy-engineer` | Execution | `composer-2.5-fast` |

## Supervisor mode

User stays in one parent session; parent/orchestrator spawn `Task` workers autonomously.
Parallel → worktree-router; Docker → queue; isolated Done → integration-merger then
doc-sync/commit on **main**.

## Fallback if a slug is rejected

Governance → `grok-4.5-fast-xhigh` → `composer-2.5`  
Delivery → `composer-2.5` → `composer-2.5-fast` → `grok-4.5-fast-xhigh`  
Execution → `composer-2.5-fast` → `composer-2.5`

Do **not** fall back to `glm-5.2-*` while API pool conservation is required.

## API restore (later)

Still no `inherit`. Prefer Claude Fable/Opus/Sonnet for plan/governance/reasoning when
quota returns; optionally reintroduce GLM for Delivery if API pool is acceptable.
Keep Composer for Execution.
