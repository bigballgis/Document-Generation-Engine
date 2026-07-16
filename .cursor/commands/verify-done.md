# Verify the current slice is actually Done.

Optional shortcut — parent auto-runs this when the user asks to verify / 验收 Done
(see subagent-routing-mandate Auto-intent).

$ARGUMENTS

Invoke `Task(subagent_type=verifier)`. On flake: **retry** ≤3. If enum missing / still
failing → follow `.cursor/agents/verifier.md` **inline** per
`.cursor/skills/specialist-runtime-fallback/SKILL.md` (unless `禁止降级` /
`no-gp-fallback`). Emit `runtime_routing`.
Check: gates green, E2E if frontend, queued deploy evidence if behavior changed, doc-sync, commit-review (or explicit no-commit), worktree cleaned if isolated.
Report PASS / FAIL with blockers only — do not implement fixes unless asked.
