---
name: worktree-isolation
description: Mandatory per-session git worktree before delivery writes; naming, create/merge/cleanup commands, and integration-merger handoff. Use at the start of every implementing session and whenever parallel agents or builds could collide.
---

# Worktree Isolation

## Policy (2026-07-10)

**Every delivery session gets a new worktree.** Implementation, TDD, and quality gates run
in the feature tree; merge to `main` only after green gates via `integration-merger`.

MAIN is for orchestration, read-only work, and post-merge doc-sync/commit — not for
feature WIP or `mvn verify`.

## Why

One shared working directory causes collisions on uncommitted files, `backend/target`,
`frontend/dist`, and Windows demo docx file locks. Git worktrees give each session its
own directory while sharing the object database.

**Docker is separate:** single Docker host — all deploys through the deploy queue
(`.cursor/skills/docker-deploy-queue/SKILL.md`).

## Who provisions

**Stage 0:** `worktree-router` (mandatory before delivery writes).

| Stay on MAIN | New ISOLATED worktree (default) |
| --- | --- |
| Read-only Q&A | Any file writes for a delivery slice |
| Single-line mechanical edit | Multi-file / behavior-changing work |
| User `main-only` / `no-worktree` opt-out | TDD, `mvn verify`, `pnpm build` |
| Post-merge doc-sync + commit | Parallel or sequential implementing sessions |

## Naming

| Item | Pattern | Example |
| --- | --- | --- |
| Directory | `../DGE-<slice-id>` | `../DGE-mgmt-ui-defects` |
| Branch | `feat/<slice-id>` | `feat/mgmt-ui-defects` |

## Create (from MAIN root)

```powershell
git fetch origin
git worktree add "..\DGE-<slice-id>" -b feat/<slice-id> origin/main
```

Then **`move_agent_to_root`** to the new path before implementers write files.

Shared caches OK: `~/.m2`, pnpm store.
Not shared: working tree, `backend/target/`, `frontend/dist/`.

## During work (feature worktree)

- All code, tests, and gate commands run **inside** the feature worktree.
- Do not run full gates on MAIN while this slice is open.
- Docker: enqueue via `docker-deploy-queue.ps1` only (single host).

## Merge & cleanup (mandatory)

After green gates → **`integration-merger`** (stage 11):

1. Merge `feat/<slice-id>` into `main` (PR or local `--no-ff`).
2. **`post-task-doc-sync`** + **`post-task-commit-review`** on **MAIN**.
3. Remove worktree:

```powershell
git worktree remove "..\DGE-<slice-id>"
git worktree prune
```

## Related agents

- `worktree-router` — stage 0 provision
- `integration-merger` — stage 11 merge + cleanup
- `delivery-orchestrator` — enforces 0 → … → 11 → 12 → 13
- `build-deploy-agent` — gates + queued Docker
