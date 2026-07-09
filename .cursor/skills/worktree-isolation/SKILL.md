---
name: worktree-isolation
description: Decide when to use an isolated git worktree vs the main tree, naming conventions, create/remove commands, and handoff to integration-merger. Use whenever parallel agents, build contention, or feature-branch isolation is in play.
---

# Worktree Isolation

## Why

One shared working directory causes collisions on uncommitted files, `backend/target`,
`frontend/dist`, and agent edits. Git worktrees give each slice its own directory while
sharing the object database.

**Docker is separate:** this machine has a **single Docker host** — do not spin parallel
compose stacks. All deploys go through the deploy queue
(`.cursor/skills/docker-deploy-queue/SKILL.md`).

## Who decides

Invoke `worktree-router` (or apply this rubric inline when the parent is read-only advising).

| Stay on MAIN | Create ISOLATED worktree |
| --- | --- |
| Sequential single slice | Parallel agents / slices |
| Docs-only, tiny mechanical edit | Concurrent Maven/pnpm builds likely |
| No other writer on overlapping paths | Risky experiment / best-of-N |

## Naming

| Item | Pattern | Example |
| --- | --- | --- |
| Directory | `../DGE-<slice-id>` | `../DGE-F5-async` |
| Branch | `feat/<slice-id>` | `feat/f5-async` |

## Create

```powershell
# From main repo root
git fetch origin
git worktree add "..\DGE-<slice-id>" -b feat/<slice-id> origin/main
```

Point the implementing agent root at the new path (`move_agent_to_root`).

Shared caches OK: `~/.m2`, pnpm store. Do **not** share `backend/target` or `frontend/dist`
across trees (each tree has its own).

## During work

- TDD inner loops run **inside** the feature worktree.
- Do not run `docker-deploy.ps1` from two trees at once — enqueue deploy on the single host.
- Prefer merging via `integration-merger` when the slice is green.

## Cleanup (after merge)

Owned by `integration-merger`:

```powershell
git worktree remove "..\DGE-<slice-id>"
git worktree prune
```

## Related agents

- `worktree-router` — placement decision
- `integration-merger` — merge + remove worktree
- `delivery-orchestrator` — calls router before parallel/multi-slice work
- `build-deploy-agent` — gates + queued Docker deploy
