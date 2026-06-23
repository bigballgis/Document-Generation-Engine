---
name: tdd-feature-delivery
description: Test-first, behavior-driven delivery loop for backend (Java 21 + Spring Boot) and frontend (Vue 3 + TS) slices in this project. Use when implementing any feature slice, fixing a bug with a regression test, or before claiming a task done.
---

# TDD Feature Delivery

Behavior drives the plan; tests drive the code. No feature is "Done" without
real behavior and green gates.

## Loop

```
- [ ] 1. Behavior spec: actor, goal, trigger, preconditions, steps, system response,
        acceptance scenarios (Given/When/Then), boundary/exception, evidence, traceability
- [ ] 2. Confirm/update the owning source-of-truth document if behavior changes
- [ ] 3. Write a failing test (unit / contract / integration as appropriate)
- [ ] 4. Implement the smallest change to pass
- [ ] 5. Refactor with tests green
- [ ] 6. Run quality gates
- [ ] 7. Post-task doc sync (`.cursor/skills/post-task-doc-sync/SKILL.md`)
- [ ] 8. Update plan status, execution-sync-ledger, and affected docs
```

## Backend gates

```bash
mvn -B -ntp -f backend/pom.xml verify
```

- Includes Checkstyle + PMD + SpotBugs + JaCoCo.
- Coverage gate: changed lines >= 85%; security-critical / core domain >= 90%.

## Frontend gates

```bash
pnpm -C frontend lint
pnpm -C frontend type-check
pnpm -C frontend test
pnpm -C frontend build
```

- Use direct `pnpm` (or the shared resolver). Never `corepack pnpm` in scripts.

## Done definition

- Tests green, gates green, docs updated, behavior is durable and verifiable.
- Post-task doc sync completed (same change set as code).
- No secrets / passwords / raw variable values / full request bodies / full download
  URLs / full AD Group membership in logs, audit, UI, or contract output.
- Temporary in-memory/stub seams are explicitly marked transitional, not "Done".
