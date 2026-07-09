#!/usr/bin/env bash
# After implementer subagents stop, remind parent to run post-task-doc-sync
# (on main if the slice used an isolated worktree — merge first via integration-merger).
set -euo pipefail

cat <<'EOF'
{
  "followup_message": "Implementer subagent stopped. If the slice is behavior-complete and gates are green: (1) if placement was ISOLATED, invoke integration-merger first; (2) invoke post-task-doc-sync on the main worktree; (3) then post-task-commit-review (honor no-commit/no-push). Do not claim Done until doc-sync (+ commit-review when commit is in scope) completes. Use handoff payload from .cursor/skills/delivery-pipeline/SKILL.md."
}
EOF
