# Subagent model strategy

Each specialist under `.cursor/agents/` pins a `model` in YAML frontmatter so tasks run on
the right cost/capability tier instead of always inheriting the parent session model.

> **⚠️ Temporary global override active (no API token).** All specialists are currently pinned
> to `composer-2.5` to avoid consuming API-token models. Each agent file keeps its original
> default in an inline comment (`# temp override (no API token); default: <slug>`). The
> **Assignments** table below still lists the durable per-agent defaults — restore from it
> when tokens are available. See **Restore to per-agent defaults** at the bottom.

## Allowed families

**Only Composer and Claude slugs** are permitted for subagents in this repo. Do not pin GPT,
Codex, or other families.

| Family | Slugs used here |
| --- | --- |
| **Claude Fable** | `claude-fable-5-thinking-xhigh` — **`plan-orchestrator` only** |
| **Claude Opus / Sonnet** | `claude-opus-4-8-thinking-high`, `claude-4.6-sonnet-high-thinking` |
| **Composer** | `composer-2.5`, `composer-2.5-fast` |

Use exact slugs from the Cursor model picker. `inherit` remains valid when a specialist should
follow the parent session model (none pinned by default in this repo).

## Tiers

| Tier | Slug | When to use |
| --- | --- | --- |
| **Plan** | `claude-fable-5-thinking-xhigh` | Plan layer maintenance only (`plan-orchestrator`) |
| **Governance** | `claude-opus-4-8-thinking-high` | Pipeline routing, architecture review, pre-commit gate |
| **Reasoning** | `claude-4.6-sonnet-high-thinking` | BDD specs and documentation reconciliation |
| **Implementation** | `composer-2.5` | Backend/frontend TDD, Playwright, UIUX evidence, structured doc sync |
| **Execution** | `composer-2.5-fast` | Script gates and Docker ops only |

## Assignments

| Agent | Model | Rationale |
| --- | --- | --- |
| `plan-orchestrator` | `claude-fable-5-thinking-xhigh` | **Plan only** — master/detail plan, single active phase |
| `delivery-orchestrator` | `claude-opus-4-8-thinking-high` | End-to-end routing; must not skip gates or mis-order pipeline |
| `architecture-reviewer` | `claude-opus-4-8-thinking-high` | Read-only but high impact; ADR/module/permission drift is costly |
| `post-task-commit-review` | `claude-opus-4-8-thinking-high` | Final commit gate; block on critical findings |
| `behavior-spec-author` | `claude-4.6-sonnet-high-thinking` | Given/When/Then clarity and requirement traceability |
| `doc-keeper` | `claude-4.6-sonnet-high-thinking` | Source-of-truth reconciliation across many docs |
| `backend-engineer` | `composer-2.5` | Java 21 + Spring Boot TDD |
| `frontend-engineer` | `composer-2.5` | Vue 3 + TypeScript TDD |
| `e2e-test-engineer` | `composer-2.5` | Playwright functional journeys |
| `e2e-uiux-reviewer` | `composer-2.5` | Visual/responsive/a11y/brand evidence needs full Composer capability |
| `post-task-doc-sync` | `composer-2.5` | Checklist-driven plan/doc sync; not plan authoring |
| `build-deploy-agent` | `composer-2.5-fast` | Maven/pnpm gates and deploy scripts |
| `deploy-engineer` | `composer-2.5-fast` | Docker compose rollout and rollback evidence |

## Override and fallback

1. **Agent file wins** — frontmatter `model:` is the durable default for that specialist.
2. **Task tool** — parent may pass `model` on a single invocation; prefer file config for consistency.
3. **Fable scope** — Fable is pinned **only** on `plan-orchestrator`; do not assign Fable elsewhere.
4. **Family lock** — new assignments must stay within Composer + Claude; update this doc when adding agents.
5. **Plan / Max Mode** — some billing tiers restrict non-Composer subagents; enable Max Mode or
   usage-based billing if a pinned Claude slug is ignored.
6. **Team policy** — org admin may block specific models; fall back to allowed slugs and update this table.
7. **Reload** — restart Cursor after editing agent definitions.

## Changing assignments

Edit the agent's `model:` in its `.md` file **and** update the table above in the same change set
so the strategy doc stays the source of truth.

## Restore to per-agent defaults

The temporary override sets every agent to `composer-2.5`. To switch back to the durable defaults
listed in **Assignments**, edit each `.md` frontmatter — the original slug is preserved inline:

| Agent | Restore `model:` to |
| --- | --- |
| `plan-orchestrator` | `claude-fable-5-thinking-xhigh` |
| `delivery-orchestrator` | `claude-opus-4-8-thinking-high` |
| `architecture-reviewer` | `claude-opus-4-8-thinking-high` |
| `post-task-commit-review` | `claude-opus-4-8-thinking-high` |
| `behavior-spec-author` | `claude-4.6-sonnet-high-thinking` |
| `doc-keeper` | `claude-4.6-sonnet-high-thinking` |
| `build-deploy-agent` | `composer-2.5-fast` |
| `deploy-engineer` | `composer-2.5-fast` |
| `backend-engineer` / `frontend-engineer` / `e2e-test-engineer` / `e2e-uiux-reviewer` / `post-task-doc-sync` | `composer-2.5` (unchanged) |

Steps to restore one agent: open its `.cursor/agents/<agent>.md`, replace the
`model: composer-2.5  # temp override …` line with `model: <default slug>`, then restart Cursor.
