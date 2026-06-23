---
id: DOC-ARCH-QUALITY-GATE-THRESHOLD-BASELINE
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
  - implementation
dependsOn:
  - docs/architecture/tdd-delivery-workflow.md
  - docs/architecture/m1-task-sheet.md
  - docs/governance.md
  - docs/git-workflow.md
related:
  - .github/skills/fullstack-command-matrix/SKILL.md
  - .github/skills/long-term-maintainability-fences/SKILL.md
  - .github/skills/code-review-static-scan/SKILL.md
  - .github/skills/task-auto-workflow/SKILL.md
   - docs/adr/technology-stack/0035-implementation-realization-and-quality-gate-baseline.md
---

# Quality Gate Threshold Baseline

## Purpose

This document hardens quality gates into explicit default thresholds so implementation quality is enforceable and consistent.

If stack-specific thresholds are stricter, use the stricter thresholds.

## Blocking Policy

The following are blocking for commit and push:

1. Any failed required quality gate command.
2. Any unresolved high-severity review finding.
3. Any architecture boundary violation without approved exception.
4. Missing required knowledge-capture update for significant design changes.

## Default Thresholds

## Security and Dependency

1. Vulnerability policy:
   - Block on any `critical` or `high` vulnerability in changed dependency scope from available approved evidence sources.
   - `medium` requires risk note and remediation ticket with target due date.
   - In intranet-constrained mode where external-source dependency-check feed paths are non-resolvable, external dependency-check execution is optional and non-blocking.
   - In that mode, blocking dependency-security baseline is internal registry/SCA advisory evidence plus SBOM artifacts; `critical` and `high` findings in available internal evidence remain blocking.
2. Dependency governance:
   - New dependency requires explicit rationale in task/review summary.
   - Unpinned dependency for production path is blocking unless toolchain requires range pinning with lockfile.

## Complexity and Size

1. Function-level complexity:
   - Default target: cyclomatic complexity <= 10.
   - Hard block threshold: > 15 unless approved exception with refactor ticket.
2. Function length:
   - Default target: <= 80 lines.
   - Hard block threshold: > 120 lines unless decomposition plan is approved.
3. File length:
   - Default target: <= 500 lines.
   - Hard block threshold: > 800 lines unless split plan is approved.

## Duplication and Dead Code

1. Duplication:
   - New duplication in changed scope should not exceed 3%.
   - Hard block threshold: > 5% new duplication in changed scope.
2. Dead code:
   - New dead code paths introduced in changed scope are blocking unless feature-flagged with explicit removal ticket.

## Test and Coverage

1. Required tests:
   - Unit, integration, contract, and regression tests in affected scope must pass.
2. Changed-line coverage baseline:
   - Backend changed lines: >= 85%.
   - Frontend changed lines: >= 80%.
   - Security-critical or core-domain modules: >= 90%.
3. Flaky tests:
   - Known flaky tests in changed scope are blocking unless quarantined with owner and fix due date.

## Knowledge Capture and Maintainability

1. Significant changes require maintenance notes update in at least one place:
   - Task sheet execution notes.
   - Architecture view update.
   - ADR update when boundary or durable decision changes.
2. Temporary workaround policy:
   - Requires debt ticket id, owner, and expiration milestone (default <= 2 sprints).

## Significant Change Criteria

Any one of the following is significant:

1. New module or layer introduction.
2. New external dependency or infrastructure capability.
3. New public API behavior or backward-compatibility impact.
4. Cross-module dependency direction change.
5. Security or authorization behavior changes.

## Exception Handling

1. Exception request must include:
   - reason,
   - risk,
   - owner,
   - expiration date,
   - cleanup task id.
2. Expired exceptions are blocking until renewed or removed.

## Confirmed Transitional Checkstyle Policy

Confirmed on 2026-06-10 for repository baseline convergence:

1. Temporary gate criterion (Stage A): delta-clean on changed files is accepted for wave closure while full baseline debt is being burned down.
2. Burn-down quota (Stage B): each wave must reduce at least 200 Checkstyle warnings from tracked baseline scope.
3. Full restoration threshold (Stage C): return to strict full-scope Checkstyle blocking when warning count reaches 300.

This transition does not authorize weakening unrelated gates (tests, PMD, SpotBugs, security, or architecture boundaries).

## Maintenance and Refresh

1. Review thresholds monthly in lightweight mode.
2. Review thresholds quarterly in full mode.
3. Trigger immediate review after major security advisories or major framework upgrades.

Use [best-practice-refresh](../../.github/skills/best-practice-refresh/SKILL.md) for refresh workflow.
