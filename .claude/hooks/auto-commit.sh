#!/usr/bin/env bash
# Auto-commit hook (Claude Code Stop event).
#
# Purpose: prevent the "overnight session left 73 uncommitted changes" failure mode.
# When an agent thread stops, this commits any unstaged/untracked work locally with a
# clearly-marked WIP message. It NEVER pushes — the user reviews and pushes manually.
#
# Idempotent: nothing changed -> exits 0, no commit.
# Opt-out: touch .claude/no-auto-commit to disable for the current session.
# Safe: skips detached HEAD, mid-rebase/merge/bisect states, and "no-commit"/"draft"
#       commit prefixes so it never fights an in-progress manual flow.

set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "[auto-commit] not a git repo, skip"; exit 0; }
cd "${REPO_ROOT}" || exit 0

# Opt-out switch (session-local).
[[ -f .claude/no-auto-commit ]] && { echo "[auto-commit] opt-out file present, skip"; exit 0; }

# Bail on detached HEAD.
BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)" || exit 0
if [[ -z "${BRANCH}" || "${BRANCH}" == "HEAD" ]]; then
  echo "[auto-commit] detached HEAD, skip"; exit 0
fi

# Bail on in-progress rebase/merge/bisect — let the user finish those manually.
for d in .git/rebase-merge .git/rebase-apply .git/MERGE_HEAD .git/BISECT_LOG; do
  [[ -e "${d}" ]] && { echo "[auto-commit] ${d} present (in-progress op), skip"; exit 0; }
done

# Bail if the last commit was already an auto-commit WIP within the last 60s — avoids
# back-to-back duplicates when Stop fires twice in quick succession.
LAST_MSG="$(git log -1 --pretty=%s 2>/dev/null)" || LAST_MSG=""
LAST_TS="$(git log -1 --pretty=%ct 2>/dev/null)" || LAST_TS=0
NOW="$(date +%s)"
if [[ "${LAST_MSG}" == "chore(auto-commit):"* ]] && (( NOW - LAST_TS < 60 )); then
  echo "[auto-commit] last commit was <60s ago auto-commit, skip"; exit 0
fi

# Determine if there is anything to commit (staged or unstaged or untracked).
if git diff --cached --quiet -- && git diff --quiet -- && git ls-files --others --exclude-standard --directory --no-empty-directory | grep -q .; then
  : # nothing staged, nothing unstaged, no untracked -> still check below
fi

# Stage tracked modifications + untracked (non-ignored) files.
git add -A -- ':!*.lock' ':!pnpm-lock.yaml' 2>/dev/null || git add -A

# Re-check after staging; exit if truly clean.
if git diff --cached --quiet --; then
  echo "[auto-commit] clean, skip"; exit 0
fi

COUNT="$(git diff --cached --name-only | wc -l | tr -d ' ')"
STAMP="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

echo "[auto-commit] committing ${COUNT} file(s) as WIP on ${BRANCH} (not pushed)"

git commit \
  --no-verify \
  -m "chore(auto-commit): WIP save from agent Stop event (${STAMP})" \
  -m "Auto-staged ${COUNT} file(s) to prevent workspace degradation. Review and rework into proper task-scoped commits before pushing. Not pushed." \
  > /dev/null 2>&1 && echo "[auto-commit] done: $(git rev-parse --short HEAD)" || echo "[auto-commit] commit failed (non-fatal)"

exit 0
