---
name: bdd-behavior-spec
description: Author and persist BDD behavior specs (actor, goal, trigger, Given/When/Then, boundaries) before TDD. Use at the start of behavior-changing work or when acceptance is unclear.
---

# BDD Behavior Spec

Skill companion to `behavior-spec-author`. No implementation until readiness is
`ready` or explicitly `not-applicable`.

## Capture checklist

```
- [ ] Actor / role
- [ ] Goal
- [ ] Trigger
- [ ] Preconditions
- [ ] Journey steps
- [ ] System responses (success + fail-closed)
- [ ] Acceptance scenarios (Given / When / Then)
- [ ] Boundary / exception behavior
- [ ] Observable evidence (API, UI, audit, document artifact)
- [ ] Traceability (requirements / PRD / domain / permission / ADR / task id)
```

## Readiness output

```
bdd_readiness: ready | blocked | not-applicable
open_questions: [...]   # if blocked
owning_doc: path        # where confirmed behavior was persisted
task_ids: [...]         # Task Master and/or plan task ids
```

## Persist before code

Write confirmed behavior into the owning source-of-truth document (prefer existing
section). Large multi-doc edits → delegate `doc-keeper`.

## Source-of-truth order

1. Latest explicit user confirmation  
2. `.taskmaster/tasks/tasks.json` (active work since 2026-07-05)  
3. `docs/plan/` (phase history + LRP/CDP/SOR live programs)  
4. requirements → PRD → domain → permission matrix → ADRs  

## Related

- Agent: `.cursor/agents/behavior-spec-author.md`
- Next: `plan-orchestrator` → implementers
