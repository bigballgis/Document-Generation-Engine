# Verify the current slice is actually Done.

Optional shortcut — parent auto-runs this when the user asks to verify / 验收 Done
(see subagent-routing-mandate Auto-intent).

$ARGUMENTS

Invoke `Task(subagent_type=verifier)`. On flake: **retry** ≤3. If enum missing / still
failing → **BLOCKED** + recovery hints per
`.cursor/skills/specialist-runtime-fallback/SKILL.md`. Follow
`.cursor/agents/verifier.md` **inline** only if the user said `allow-gp-fallback` /
`允许降级` (emit `runtime_routing`).
Check: gates green, E2E if frontend, queued deploy evidence if behavior changed, doc-sync, commit-review (or explicit no-commit), worktree cleaned if isolated.
Report PASS / FAIL with blockers only — do not implement fixes unless asked.
