# Git Workflow

## Purpose

Git is the version control system for this project. It tracks documentation, governance rules, and future source code changes.

This project currently uses Git primarily for docs-as-code: requirements, product rules, domain models, permission matrices, governance rules, and ADRs are all versioned as project assets.

## Current Policy

- Documentation changes are version controlled through Git.
- Documentation changes should be small, reviewable, and traceable.
- Requirement changes should be captured in the documentation before implementation work begins.
- Future code changes must be checked against the documentation before completion.
- Commits and pushes require explicit user request unless a repository workflow policy, such as `task-auto-workflow`, grants standing task-level automation for the current request.
- Under standing task-level automation, each completed Task ID that changes files must receive scoped preflight, scoped commit, and push after green required gates unless the user explicitly says `no-commit`, `no-push`, `draft`, `plan-only`, `review-only`, or equivalent.

## Fixed Delivery Workflow For Implementation

All implementation tasks must follow [Fixed TDD Delivery Workflow](architecture/tdd-delivery-workflow.md).

Mandatory order:

1. Generate tasks.
2. Generate task sheet.
3. Review required skills.
4. Implement requirement with strict Red-Green-Refactor TDD.
5. Run requirement tests.
6. Run code review and static scans.
7. Commit.
8. Push.

Do not skip, reorder, or merge stages in a way that bypasses quality gates.

## Relationship With Documentation Governance

Use the documentation governance workflow before committing documentation changes.

Recommended order:

```text
User confirms requirement or process change
    -> update docs using documentation-governance
    -> verify links and consistency
    -> review git status and diff
    -> commit when requested or standing task-level automation applies
    -> push when requested or standing task-level automation applies
```

## Change Separation

Prefer separate commits for separate concerns:

- Requirement updates.
- Documentation structure changes.
- Governance workflow changes.
- ADR additions.
- Future implementation changes.
- Future test changes.

If one user request requires multiple related files, they can be committed together when they form one coherent change.

## Suggested Commit Types

Use concise commit messages such as:

- `docs: update requirements`
- `docs: add PRD and domain model`
- `docs: reorganize project structure`
- `docs: add documentation governance`
- `docs: clarify API authorization scope`
- `adr: record template lifecycle decision`
- `chore: update repository structure`

## Safe Git Rules

- Inspect status before staging or committing.
- Review diffs before committing.
- Stage only intended files.
- For Task ID based work, stage only files approved for that Task ID and do not combine multiple Task IDs without explicit user approval.
- Do not use destructive commands unless explicitly requested.
- Do not overwrite user changes.
- Do not commit secrets, credentials, or environment-specific private configuration.

## Mandatory Quality Gates Before Commit And Push (Code Changes)

For implementation code changes, all gates below are blocking:

1. TDD evidence: failing test first, then passing implementation, then refactor.
2. Requirement tests: changed-scope tests and required regression tests pass.
3. Static scans: formatter/lint/type-compile/security checks pass without blocking findings.
4. Code review: no unresolved high-severity findings.
5. Diff hygiene: stage only intended files and avoid unrelated edits.
6. Task scope hygiene: keep one completed Task ID per commit unless explicitly approved otherwise.

If any gate fails, do not commit or push.

## Skills

Use these project skills when relevant:

- `documentation-governance`: requirement, product, domain, permission, ADR, and documentation index updates.
- `git-change-management`: Git status review, diff review, commit preparation, and remote sync workflow.

## Pre-Commit Documentation Checklist

Before a documentation commit, verify:

- New or moved docs are linked from the [documentation index](README.md).
- Process changes are reflected in [documentation governance](governance.md).
- Project instructions reference current source-of-truth paths.
- Confirmed and pending requirements remain separate.
- ADRs are added for durable decisions when appropriate.

## Open Questions

- Branching model details remain to be finalized.
- Pull request ownership and reviewer assignment model remain to be finalized.
- Tool-specific static scan command matrix per repository stack remains to be finalized.
