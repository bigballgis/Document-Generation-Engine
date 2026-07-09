---
name: cursor-native-parallel
description: Use Cursor native /multitask, /worktree, /best-of-n, and Agents Window with this repo's worktree-router, deploy queue, and integration-merger. Apply when parallel agents, isolated checkouts, or model bake-offs are needed.
---

# Cursor native parallel (2026)

Official primitives (Cursor 3.x):

| Command / UI | Isolation | Use when |
| --- | --- | --- |
| `/multitask` | Context (async subagents) | Independent chunks; edits may share one checkout |
| `/worktree` | Filesystem (separate git checkout) | Overlapping files, risky refactors, long tasks |
| `/best-of-n` | Per-model worktree | Compare models on the same prompt |
| Agents Window | Session tabs | Supervise multiple agents side-by-side |

## Compose with this repo

1. **Placement** — still run `worktree-router` (or apply its rubric) so naming stays
   `../DGE-<slice>` + `feat/<slice>` and Docker policy stays `QUEUE_ONLY`.
2. **Parallel writers** — `/multitask` **+** `/worktree` (or `git worktree add`) when
   paths may overlap. Cap fan-out (≤3 writers) — Cursor has **no** automatic cost ceiling.
3. **Docker** — never one stack per worktree. Always `.\scripts\docker-deploy-queue.ps1`.
4. **Merge** — `integration-merger` (or `/apply-worktree` then merger checklist) → doc-sync
   + commit on **main**.
5. **Move agent root** — after creating a worktree, `move_agent_to_root` into that path.

## Anti-patterns (community / docs)

- Spawning dozens of subagents without a cap (quota burn).
- `/multitask` alone on overlapping file edits (merge hell).
- Nested subagents deeper than needed (model pin bugs at level 2+ reported on forums).
- Parallel `docker-deploy` from two worktrees.

## Slash commands in this repo

- `/deliver` — full pipeline via delivery-orchestrator
- `/multitask-slices` — parallel + worktree + queue policy
- `/deploy-queue` — queued deploy
- `/verify-done` — verifier PASS/FAIL
