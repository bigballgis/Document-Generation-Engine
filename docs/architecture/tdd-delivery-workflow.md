---
id: DOC-ARCH-TDD-DELIVERY-WORKFLOW
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
  - implementation
dependsOn:
  - docs/requirements/requirements-plan.md
  - docs/product/PRD.md
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
  - docs/api/openapi-v1.yaml
  - docs/architecture/module-boundaries.md
related:
  - docs/architecture/ai-development-guide.md
  - docs/architecture/implementation-task-plan.md
  - docs/git-workflow.md
  - docs/governance.md
  - .github/skills/task-auto-workflow/SKILL.md
  - .github/skills/git-change-management/SKILL.md
---

# Fixed TDD Delivery Workflow

## Purpose

This workflow hardens the default engineering process for requirement delivery and implementation.

The workflow is mandatory for all implementation work and must be followed in sequence:

0. Confirm behavior specification
1. Generate tasks
2. Generate task sheet
3. Review required skills
4. Implement requirement with TDD
5. Test requirement
6. Run code review and static scans
7. Verify production-grade acceptance gates
8. Commit
9. Push

## Mandatory Stages

## Stage 0: Requirement Intake and Source Mapping

### Required Inputs

1. Confirmed requirement source in requirements, product, domain, security, and API docs.
2. Architecture boundary ownership.
3. Unresolved questions list.
4. Behavior specification for behavior-changing work: actor/role, user goal, trigger, preconditions, user journey, system responses, acceptance scenarios, Given/When/Then or equivalent, boundary and exception behavior, observable evidence, and traceability.

### Exit Gate

- Implementation scope is traceable to source-of-truth docs.
- Behavior-changing work has BDD readiness = ready, or is blocked pending user confirmation and source-document update.
- No unresolved blocker is silently treated as confirmed.

## Stage 1: Task Generation

### Required Actions

1. Split requirement into implementable tasks by module boundary.
2. Declare acceptance criteria for each task.
3. Declare BDD scenario coverage and corresponding test intent for each task.

### Exit Gate

- Every behavior-changing task maps to at least one behavior specification and requirement statement.
- Every task has explicit done criteria.

## Stage 2: Task Sheet Generation

### Required Actions

1. Create or update a task sheet with task id, scope, owner, dependencies, and done criteria.
2. Include test cases and review checkpoints per task.

### Exit Gate

- Task sheet is complete and reviewable.
- All tasks have testable outcomes.

## Stage 3: Skills Readiness Review

### Required Actions

1. Review required project skills for the task type.
2. Confirm skill coverage before editing code.

### Required Skill Matrix

1. `documentation-governance`: requirements and source-of-truth synchronization.
2. `task-auto-workflow`: fixed end-to-end execution pipeline.
3. `git-change-management`: safe commit and push control.

### Exit Gate

- Skill usage scope is explicit.
- Any missing capability is recorded as pending and escalated.

## Stage 4: Requirement Implementation (Strict TDD)

### Mandatory Cycle

All implementation tasks must run Red-Green-Refactor in this order:

1. Red: write or update a failing test that encodes the BDD scenario or equivalent behavior assertion.
2. Green: implement the minimal code to pass the failing test.
3. Refactor: improve structure and readability while keeping tests green.

### TDD Constraints

1. No feature code without a preceding failing test.
2. No merge-ready change with skipped failing tests.
3. Refactor must preserve behavior and keep full test suite green.

### Exit Gate

- Each implemented behavior has a corresponding behavior scenario and test first history.
- Unit and integration scope for the requirement is complete.

## Stage 5: Requirement Testing

### Required Actions

1. Run focused tests for changed modules.
2. Run broader regression tests required by dependency impact.
3. Record any intentionally deferred tests.

### Exit Gate

- Required tests pass.
- Any deferred test has explicit reason and risk note.

## Stage 6: Code Review + Static Scan (Blocking Gate)

### Required Actions

1. Review for requirement correctness and behavioral regressions.
2. Review architecture conformance, module ownership, and dependency direction.
3. Review code style consistency and maintainability.
4. Run static quality checks for the stack in use.

### Minimum Static Scan Coverage

1. Formatter and lint checks.
2. Type and compile checks.
3. Security/static analysis checks.

### Exit Gate

- No blocking review finding remains.
- Static checks report no blocking issues.

## Stage 7: Verify Production-Grade Acceptance Gates

### Required Actions

1. Verify persistence gate for persistence-sensitive behavior (durable storage boundary present and tested).
2. Verify integration gate (real integration path coverage exists beyond mock-only assertions).
3. Verify deployment gate for user-visible changes (redeploy evidence + runtime verification evidence).
4. Verify evidence gate (completion output contains auditable command outcomes and runtime/operational evidence references).

### Exit Gate

- Production-grade acceptance gates are satisfied, or scope is explicitly documented as transitional/non-production with residual risk and owner follow-up.

## Stage 8: Commit

### Required Actions

1. Review diff and stage only intended files for the completed Task ID.
2. Keep commit scoped and coherent to one Task ID unless the user explicitly approves a combined task commit.
3. Include test, scan, and UIUX evidence outcomes in review summary.

### Exit Gate

- Commit is traceable to requirement/task ids and contains only the approved scope for the completed Task ID.
- Commit contains only approved, reviewed, and scanned changes.

## Stage 9: Push

### Required Actions

1. Push only after successful Task ID scoped commit and Stage 6 gate pass.
2. Verify branch tracking and remote sync result.

### Exit Gate

- Remote branch is updated successfully.
- Final report includes files changed, validations, review findings, commit id, and push status.

## Blocking Rules

1. If TDD order is broken, stop and return to Red phase.
2. If tests fail, do not commit or push.
3. If static scans fail with blocking findings, do not commit or push.
4. If required UIUX blocking evidence is missing or failed, do not commit or push.
5. If review finds unresolved high-severity issues, do not commit or push.
6. If unrelated dirty files cannot be safely separated, do not commit or push.
7. If production-grade acceptance gates are missing or red for claimed-complete behavior, do not commit or push.

## Quality Baseline for Code Consistency

1. Keep naming, layering, and module boundaries consistent with architecture docs.
2. Keep dependency direction aligned with module boundary rules.
3. Avoid hidden business logic in generic utility layers.
4. Keep tests readable, deterministic, and requirement-traceable.

## Output Contract Per Task

Each completed implementation task must report:

1. Requirement and task ids covered.
2. Tests added or changed.
3. Static scans run and outcomes.
4. Code review findings and resolutions.
5. Commit id and push status.
6. Remaining risks or pending follow-ups.
