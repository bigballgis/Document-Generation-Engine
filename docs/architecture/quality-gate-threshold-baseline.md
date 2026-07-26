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
  - .cursor/skills/code-quality-review/SKILL.md
  - docs/adr/technology-stack/0035-implementation-realization-and-quality-gate-baseline.md
  - docs/behavior/deps-security-refresh.md
  - docs/behavior/ai-scale-docs-conventions.md
  - docs/architecture/m9-task-sheet.md
  - docs/architecture/m9-t02-closure-plan.md
  - docs/evidence/security/README.md
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
3. Hygiene slice constraints (Task Master **#49** / `deps-security-refresh`, **Done** 2026-07-13) — **confirmed session constraints** remain durable policy (ops hygiene; not product requirements):
   - Remediations stay inside accepted stack ADRs and tech-stack guardrails; company-approved repositories only.
   - Spring Boot remains on the **3.3.x** line (current pin `3.3.13`); no Boot **3.4+ / 3.5** major-line jump without explicit user confirmation + ADR update.
   - ShedLock remains on the **6.x** line (current pin `6.10.0`; see ADR-0044 appendix); no **7.x** jump without confirmation + ADR update.
   - No major **Vue / Vite** line jump without explicit user confirmation + ADR update ([ADR-0022](../adr/technology-stack/0022-basic-technology-stack-baseline.md) / [ADR-0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md)).
   - Critical/High in changed dependency scope: remediate via baseline-safe bump **or** record an exception with the metadata in [Exception Handling](#exception-handling) (aligns with M9-T03 pattern: owner + expiry + cleanup task). #49 Vitest GHSA-5xrq-8626-4rwp: ADR-0029 amended 2026-07-17 (Vitest **3.x** floor **≥3.2.6**); exception **CLOSED** 2026-07-17 via Task Master **#50** (merge `6c8fff7d`; pins `vitest@3.2.7` + `@vitest/coverage-v8@3.2.7`; `pnpm audit` clean).
   - This slice does **not** close M9-T02 org intranet SCA upload; SBOM/SCA evidence ownership remains [docs/evidence/security/](../evidence/security/README.md) + [m9-t02-closure-plan.md](./m9-t02-closure-plan.md).
   - Behavior readiness: [deps-security-refresh.md](../behavior/deps-security-refresh.md) (`bdd_readiness: not-applicable`; Task Master **#49** **done**).

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
4. Soft review artifact budgets (agent / `code-quality-reviewer` signals — **not** a second
   hard CI SoT; must not invent stricter hard gates without ADR / user confirmation):
   - Java `@Service` / orchestrator and Vue SFC: review warn when > **400** LOC (skill soft).
   - `*Support` helpers: prefer <= **200** LOC and keep stateless; otherwise peel/extract.
   - Review warn/critical bands live in
     [code-quality-review SKILL](../../.cursor/skills/code-quality-review/SKILL.md);
     baseline **file/function hard thresholds above remain authoritative** on conflict.
   - When soft targets are exceeded on manually maintained sources, agents **prefer a
     separate peel leaf** over silent growth (see
     [ai-scale-docs-conventions.md](../behavior/ai-scale-docs-conventions.md)).
   - Generated artifacts (e.g. OpenAPI client): size flags apply only to **manual** edits.

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
