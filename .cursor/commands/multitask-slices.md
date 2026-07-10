# Parallelize independent slices safely.

Optional shortcut — parent auto-runs this when the user asks for parallel / multitask
slices (see subagent-routing-mandate Auto-intent).

User intent:

$ARGUMENTS

1. **One mandatory worktree per slice** — `worktree-router` stage 0 for each (`../DGE-<slice-id>`).
2. `move_agent_to_root` into each feature path before writers start.
3. Cap fan-out (≤3 concurrent writers); no automatic cost ceiling in Cursor.
4. Single Docker host: all deploys via `.\scripts\docker-deploy-queue.ps1` only.
5. Each green slice → `integration-merger` (stage 11) → doc-sync/commit on **MAIN** → remove worktree.
