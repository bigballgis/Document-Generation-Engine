# Subagent model strategy

Each specialist under `.cursor/agents/` **must** pin an explicit `model` slug in YAML
frontmatter. **`inherit` is forbidden.**

> **Active policy (no API quota):** Cursor-included families only (Grok / GLM / Composer).
> Claude API-token slugs stay frozen until quota returns.

## Allowed families (current)

| Family | Slugs | Role |
| --- | --- | --- |
| **Grok 4.5 High Fast** | `grok-4.5-fast-xhigh` | Governance |
| **GLM 5.2 High** | `glm-5.2-high` | Delivery |
| **Composer 2.5 Fast** | `composer-2.5-fast` | Execution |
| **`inherit`** | — | **Forbidden** |

## Tiers

| Tier | Slug | When |
| --- | --- | --- |
| **Governance** | `grok-4.5-fast-xhigh` | Routing, plan, architecture, commit, merge |
| **Delivery** | `glm-5.2-high` | BDD, docs, TDD implementation, E2E, doc-sync |
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
| `behavior-spec-author` | Delivery | `glm-5.2-high` |
| `doc-keeper` | Delivery | `glm-5.2-high` |
| `backend-engineer` | Delivery | `glm-5.2-high` |
| `frontend-engineer` | Delivery | `glm-5.2-high` |
| `rendering-engineer` | Delivery | `glm-5.2-high` |
| `e2e-test-engineer` | Delivery | `glm-5.2-high` |
| `e2e-uiux-reviewer` | Delivery | `glm-5.2-high` |
| `post-task-doc-sync` | Delivery | `glm-5.2-high` |
| `verifier` | Execution | `composer-2.5-fast` |
| `worktree-router` | Execution | `composer-2.5-fast` |
| `build-deploy-agent` | Execution | `composer-2.5-fast` |
| `deploy-engineer` | Execution | `composer-2.5-fast` |

## Supervisor mode

User stays in one parent session; parent/orchestrator spawn `Task` workers autonomously.
Parallel → worktree-router; Docker → queue; isolated Done → integration-merger then
doc-sync/commit on **main**.

## Fallback if a slug is rejected

Governance → `grok-4.5-fast-xhigh` → `glm-5.2-high`  
Delivery → `glm-5.2-high` → `grok-4.5-fast-xhigh`  
Execution → `composer-2.5-fast` → `glm-5.2-high`

## API restore (later)

Still no `inherit`. Prefer Claude Fable/Opus/Sonnet for plan/governance/reasoning when
quota returns; keep Composer/GLM for implementation/execution as desired.
