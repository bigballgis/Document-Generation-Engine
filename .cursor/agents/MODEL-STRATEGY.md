# Subagent model strategy

Each specialist under `.cursor/agents/` pins a `model` in YAML frontmatter so tasks run on
the right cost/capability tier instead of always inheriting the parent session model.

## Tiers

| Tier | Slug pattern | When to use |
| --- | --- | --- |
| **Orchestration** | `claude-opus-4-8-thinking-high` | Multi-step routing, pipeline enforcement, high-stakes governance |
| **Reasoning** | `claude-4.6-sonnet-high-thinking`, `gpt-5.5-high`, `gpt-5.4-high` | BDD specs, docs, plan sync, commit review |
| **Implementation** | `gpt-5.3-codex-high`, `composer-2.5` | Backend/frontend TDD, Playwright journeys |
| **Execution** | `composer-2.5-fast` | Script gates, Docker ops, read-only UIUX evidence |

Use exact slugs from the Cursor model picker. `inherit` remains valid when a specialist should
follow the parent session model (none pinned by default in this repo).

## Assignments

| Agent | Model | Rationale |
| --- | --- | --- |
| `delivery-orchestrator` | `claude-opus-4-8-thinking-high` | End-to-end routing; must not skip gates or mis-order pipeline |
| `architecture-reviewer` | `claude-opus-4-8-thinking-high` | Read-only but high impact; ADR/module/permission drift is costly |
| `behavior-spec-author` | `claude-4.6-sonnet-high-thinking` | Given/When/Then clarity and requirement traceability |
| `plan-orchestrator` | `gpt-5.5-high` | Structured plan layer; single active phase invariant |
| `doc-keeper` | `claude-4.6-sonnet-high-thinking` | Source-of-truth reconciliation across many docs |
| `backend-engineer` | `gpt-5.3-codex-high` | Java 21 + Spring Boot TDD |
| `frontend-engineer` | `gpt-5.3-codex-high` | Vue 3 + TypeScript TDD |
| `e2e-test-engineer` | `composer-2.5` | Playwright functional journeys |
| `e2e-uiux-reviewer` | `composer-2.5-fast` | Read-only visual/responsive/a11y evidence |
| `build-deploy-agent` | `composer-2.5-fast` | Maven/pnpm gates and deploy scripts |
| `deploy-engineer` | `composer-2.5-fast` | Docker compose rollout and rollback evidence |
| `post-task-doc-sync` | `composer-2.5` | Checklist-driven doc/plan sync with repo context |
| `post-task-commit-review` | `gpt-5.4-high` | Pre-commit review gate; block on critical findings |

## Override and fallback

1. **Agent file wins** — frontmatter `model:` is the durable default for that specialist.
2. **Task tool** — parent may pass `model` on a single invocation; prefer file config for consistency.
3. **Plan / Max Mode** — some billing tiers restrict non-Composer subagents; enable Max Mode or
   usage-based billing if a pinned slug is ignored.
4. **Team policy** — org admin may block specific models; fall back to allowed slugs and update this table.
5. **Reload** — restart Cursor after editing agent definitions.

## Changing assignments

Edit the agent's `model:` in its `.md` file **and** update the table above in the same change set
so the strategy doc stays the source of truth.
