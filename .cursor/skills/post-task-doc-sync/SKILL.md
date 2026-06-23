---
name: post-task-doc-sync
description: Mandatory workflow after every behavior-changing task completes and gates pass — sync plan layer, execution ledger, epic/milestone sheets, and indexes before claiming Done.
---

# Post-Task Documentation Sync

Run this skill **after green gates**, **before Done**.

## Quick trigger

Any of: feature slice, bug fix with regression test, phase completion, epic/milestone closure.

## Workflow

```
1. Gather task IDs, behavior summary, gate commands, changed modules
2. Run post-task-doc-sync agent checklist (or inline if same session)
3. Update plan detail → master plan → execution-sync-ledger → architecture task sheets
4. Refresh indexes (docs/README.md, plan/README.md, root README if active phase changed)
5. Emit sync report; only then mark Done
```

## Files (in order)

| Order | File | When |
| --- | --- | --- |
| 1 | `docs/plan/detail/<phase>.md` | Task status changed |
| 2 | `docs/plan/master-plan.md` | Phase status changed |
| 3 | `docs/plan/execution-sync-ledger.md` | Always when task/epic/milestone moves |
| 4 | `docs/architecture/*task-sheet*.md` | Epic/milestone task moved |
| 5 | `docs/architecture/orchestration-high-level-plan.md` | Epic backlog changed |
| 6 | `docs/architecture/implementation-task-plan.md` | Wave status changed |
| 7 | `docs/README.md` / `docs/plan/README.md` | Index or new doc |

## Hard rules

- Do not claim Done without completing this sync in the same change set.
- Do not edit accepted ADR decisions for progress tracking.
- Exactly one phase `In Progress` at a time.
- Use agent: `.cursor/agents/post-task-doc-sync.md` for full checklist.

## Related

- `.cursor/rules/post-task-doc-sync-constitution.mdc` (always apply)
- `.cursor/skills/document-as-code/SKILL.md`
- `.cursor/skills/plan-status-tracking/SKILL.md`
