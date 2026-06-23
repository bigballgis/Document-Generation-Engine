#!/usr/bin/env bash
# Chains post-task-commit-review after post-task-doc-sync completes.
# Matcher in hooks.json already limits this to subagent_type post-task-doc-sync.
set -euo pipefail

cat <<'EOF'
{
  "followup_message": "post-task-doc-sync has completed. You MUST now invoke the post-task-commit-review subagent (Task tool: subagent_type=post-task-commit-review, run_in_background=false). Pass task IDs, behavior summary, gate evidence, and doc-sync confirmation. Follow .cursor/skills/post-task-commit-review/SKILL.md. Do not claim Done until commit review passes, changes are committed (or verified nothing to commit), and pushed to the tracked remote (skip push only if the user explicitly opted out in this session)."
}
EOF
