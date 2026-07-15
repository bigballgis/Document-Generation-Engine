# Multitask slices (LEGACY OPT-IN — not the default)

**Default (2026-07-16):** single-lane serial via `/deliver`. Do **not** auto-run this
command for “继续做 / 把剩下的做完 / 自动执行后续”.

Only proceed when the user explicitly says `force-parallel` / `强制并行` (or clearly
insists on simultaneous writers after being told serial is default).

User intent:

$ARGUMENTS

If force-parallel is **not** explicit → re-route to **serial** `delivery-orchestrator`
on the sole-active queue head; park other worktrees.

If force-parallel **is** explicit:

1. **One mandatory worktree per slice** — `worktree-router` stage 0 (`../DGE-<slice-id>`).
2. `move_agent_to_root` into each feature path before writers start.
3. Cap fan-out (**≤2** concurrent writers; prefer 1). No automatic cost ceiling in Cursor.
4. Single Docker host: all deploys via `.\scripts\docker-deploy-queue.ps1` only.
5. Never overlap two slices’ `mvn verify` / full frontend gates / E2E / deploy when avoidable.
6. Each green slice → `integration-merger` (stage 11) → doc-sync/commit on **MAIN** → remove worktree.
