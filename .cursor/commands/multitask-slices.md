# Parallelize independent slices safely.

Optional shortcut — parent auto-runs this when the user asks for parallel / multitask
slices (see subagent-routing-mandate Auto-intent).

User intent:

$ARGUMENTS

1. Invoke `worktree-router` for each independent slice (prefer ISOLATED if build contention).
2. Prefer Cursor `/multitask` + `/worktree` for filesystem isolation when agents will edit overlapping paths.
3. Cap fan-out (suggest ≤3 concurrent writers) — no automatic cost ceiling in Cursor.
4. Single Docker host: all deploys via `.\scripts\docker-deploy-queue.ps1` only.
5. Each isolated green slice → `integration-merger` → doc-sync/commit on **main**.
