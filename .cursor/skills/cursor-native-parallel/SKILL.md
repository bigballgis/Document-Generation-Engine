---
name: cursor-native-parallel
description: OPT-IN only. Use Cursor native /multitask, /worktree, /best-of-n when the user explicitly forces parallel writers (force-parallel / 强制并行). Default delivery is single-lane serial — do not apply this skill for ordinary deliver / continue / queue work.
---

# Cursor native parallel (opt-in only, 2026-07-16)

**Default on this host:** single-lane serial — one delivery leaf, one worktree, one
pipeline at a time. See `docs/plan/core-excellence-program-2026-07.md` §9.2 and
`subagent-routing-mandate.mdc`.

Use this skill **only** when the user explicitly says `force-parallel` / `强制并行`
(or `/best-of-n` model bake-off). Ordinary “继续做 / 把剩下的做完” → **serial**
`/deliver`, not this skill.

## Official primitives (Cursor 3.x)

| Command / UI | Isolation | Use when (opt-in) |
| --- | --- | --- |
| `/multitask` | Context (async subagents) | Explicit force-parallel; independent chunks |
| `/worktree` | Filesystem (separate git checkout) | **Always** for delivery (also under serial) |
| `/best-of-n` | Per-model worktree | Compare models on the same prompt |
| Agents Window | Session tabs | Supervise agents; prefer one active writer |

## Compose with this repo (force-parallel only)

1. **Placement** — still run `worktree-router` so naming stays `../DGE-<slice>` +
   `feat/<slice>` and Docker policy stays `QUEUE_ONLY`.
2. **Writers** — cap fan-out at **≤2** (not 3). Prefer finishing one leaf before
   starting heavy gates on another.
3. **Docker** — never one stack per worktree. Always `.\scripts\docker-deploy-queue.ps1`.
4. **Merge** — `integration-merger` per green slice → doc-sync + commit on **main**.
5. **Move agent root** — `move_agent_to_root` into the feature path before writes.

## Anti-patterns

- Treating “继续 / 自动执行后续” as multitask (use serial queue).
- Spawning multiple CE leaves’ `mvn verify` / E2E / deploy at once.
- `/multitask` alone on overlapping file edits (merge hell).
- Parallel `docker-deploy` from two worktrees.

## Slash commands

- `/deliver` — **default** full pipeline (single leaf)
- `/multitask-slices` — **legacy opt-in**; requires user force-parallel wording
- `/deploy-queue` — queued deploy
- `/verify-done` — verifier PASS/FAIL
