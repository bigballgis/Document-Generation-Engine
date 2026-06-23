---
name: post-task-commit-review
description: Mandatory workflow after post-task-doc-sync — review the full change set, escalate to security/bugbot/architecture subagents when needed, then stage, commit, and push before claiming Done. Skips push only when the user explicitly opts out.
---

# Post-Task Commit Review

Run this skill **after green gates** and **after post-task-doc-sync**, **before Done**.

## Pipeline position

```
Implement → Gates → post-task-doc-sync → post-task-commit-review → Done
```

## Quick trigger

Any behavior-changing task where the parent delegated end-of-task commit (default for implementer agents).

## Workflow

```
1. Confirm gates green + doc sync complete (sync report or checklist done)
2. Invoke post-task-commit-review agent (or follow its checklist inline)
3. Review change set; escalate to security-review / bugbot / architecture-reviewer if needed
4. Block on 🔴 Critical; fix and re-gate if code changes
5. git status + diff + log → stage → commit → push → verify status
6. Emit commit-gate report; only then mark Done
```

## Launch subagent

```
Task tool:
  subagent_type: post-task-commit-review
  description: Post-task commit review
  run_in_background: false
```

Prompt must include: task IDs, behavior summary, gate commands, doc sync confirmation.

## Hard rules

- Do not claim Done without commit review completing in the same change set (or explicit “no commit needed”).
- Push by default after commit; skip only when the user explicitly opted out in the same session.
- Do not stage secrets or credential files.
- Delegating to this agent constitutes commit authorization for that task’s change set.

## Related

- `.cursor/agents/post-task-commit-review.md` (full checklist)
- `.cursor/rules/post-task-commit-review-constitution.mdc` (always apply)
- `.cursor/skills/post-task-doc-sync/SKILL.md` (previous step)
- `.cursor/agents/architecture-reviewer.md` (review baseline)
