---
id: DOC-ARCH-E2E-STABILIZATION-PLAN
type: Architecture View
status: Proposed
sourceOfTruth: true
owners:
  - architecture
  - implementation
dependsOn:
  - docs/adr/technology-stack/0029-frontend-application-stack-baseline.md
  - docs/adr/technology-stack/0035-implementation-realization-and-quality-gate-baseline.md
  - docs/architecture/quality-gate-threshold-baseline.md
  - docs/architecture/tdd-delivery-workflow.md
related:
  - docs/architecture/implementation-task-plan.md
  - docs/architecture/e07-task-sheet.md
---

# Playwright E2E Stabilization and Agent Plan

## Purpose

Define a documentation-first, non-blocking stabilization phase for Playwright end-to-end (E2E) coverage and introduce explicit E2E, E2E UIUX, and deployment execution agents before E2E is promoted to a blocking delivery gate.

## Confirmed Decisions

1. Playwright is the accepted E2E tooling baseline through [ADR 0029](../adr/technology-stack/0029-frontend-application-stack-baseline.md).
2. Existing blocking baseline gates remain governed by [ADR 0035](../adr/technology-stack/0035-implementation-realization-and-quality-gate-baseline.md) and [Quality Gate Threshold Baseline](./quality-gate-threshold-baseline.md).
3. During this stabilization phase, Playwright E2E runs are non-blocking quality signals and evidence inputs, not commit/push blockers.
4. E2E, E2E UIUX, and deployment automation responsibilities are split into dedicated execution agents to keep evidence traceable and rollback paths explicit.
5. E2E UIUX automation is a specialist evidence layer for visual quality, responsive layout, accessibility basics, keyboard/focus behavior, theme switching, brand consistency, screenshot/pixel evidence, text overflow, and incoherent overlap; it does not replace generic Playwright execution, unit tests, or static quality gates.

## Pending Questions

1. Promotion timing: exact release milestone for switching E2E from non-blocking to blocking is pending objective trend evidence.
2. Environment mix: final ratio of local, CI, and deployment-environment E2E runs is pending team confirmation.
3. Deployment gate coupling: whether deployment agent failure should hard-block every release train or only protected branches is pending confirmation.

## Scope and Objectives

### In Scope

1. Establish Playwright smoke-path E2E coverage for critical user journeys.
2. Introduce an E2E execution agent for deterministic run orchestration and evidence capture.
3. Introduce an E2E UIUX evidence agent for visual, interaction, accessibility, responsive, and brand/theme acceptance checks.
4. Introduce a deployment verification agent for post-deploy smoke validation.
5. Track stability metrics needed for future promotion to blocking gate status.

### Out of Scope

1. Full regression E2E coverage for all screens and edge cases.
2. Immediate blocking policy change for commit/push or release gates.
3. Replacing existing unit, integration, contract, security, or static-analysis gates.
4. Treating screenshot or visual evidence as a blocking release gate before explicit promotion criteria are documented and approved.

## Non-Blocking Stabilization Policy

1. Playwright E2E failures are recorded as stabilization findings during this phase.
2. Failures must create or update a tracked remediation item with owner and target fix milestone.
3. Repeated failures on the same smoke path trigger escalation review but remain non-blocking until promotion criteria are met and documented.
4. Blocking quality gates defined in ADR 0035 remain unchanged until an explicit governance update promotes E2E.

## Initial Coverage (Smoke Paths)

1. Authentication and authorization smoke:
   - Successful sign-in to management UI.
   - Unauthorized role denied from restricted surface.
2. Template lifecycle smoke:
   - Open template list.
   - View template detail.
   - Trigger a safe lifecycle transition where allowed.
3. API management smoke:
   - Open API policy surface.
   - Validate baseline policy summary render.
4. Generation workflow smoke:
   - Start one document generation flow.
   - Verify task status progression and result visibility.
5. Deployment verification smoke:
   - Post-deploy route health and primary UI reachability.
   - One end-to-end happy path in deployed environment.
6. UIUX evidence smoke:
   - Dual-brand theme switch between `REDBC` and `GREENBC` without page-local branding drift.
   - Desktop OA layout remains readable and structurally stable across critical role journeys.
   - Supported smaller viewport readability baseline has no incoherent overlap or severe text overflow.
   - Keyboard focus and basic accessibility signals remain visible for primary controls.
   - Screenshot or pixel evidence is captured when a requested UIUX check depends on visual inspection.

## Execution Cadence and Evidence Capture

1. Per pull request:
   - Run a minimal local or CI smoke subset as non-blocking evidence.
2. Daily integration cadence:
   - Run full smoke suite against integration environment.
3. Pre-release cadence:
   - Run deployment smoke suite after deployment candidate rollout.

Required evidence fields per run:

1. Commit SHA or release candidate tag.
2. Environment identifier.
3. Suite scope (subset/full/deployment).
4. Pass/fail summary and flaky-test markers.
5. Artifact links (Playwright report, screenshots, traces, video when enabled).
6. UIUX scope summary when applicable: viewport set, theme preset set, target role journey, and exercised visual/accessibility/responsive dimensions.
7. Related remediation ticket references for failures.

## Promotion Criteria to Blocking Gate

Promotion from non-blocking to blocking requires all confirmed criteria:

1. Stability threshold:
   - At least 4 consecutive weeks of scheduled smoke runs with >= 98% pass rate excluding approved environment outages.
2. Flakiness threshold:
   - No unresolved flaky smoke test older than one sprint.
3. Recovery readiness:
   - Proven rollback procedure executed at least once in a controlled drill with captured evidence.
4. Operational readiness:
   - E2E, E2E UIUX, and deployment agents all have documented runbooks, owners, and on-call escalation contacts.
5. Governance update:
   - Documentation update approved to revise [Quality Gate Threshold Baseline](./quality-gate-threshold-baseline.md) and related workflow docs.

Until all criteria are met and approved, E2E remains non-blocking.

## Risks and Rollback

### Risks

1. Test flakiness can create noisy signals and alert fatigue.
2. Environment instability can mask product regressions.
3. Longer pipeline duration can reduce delivery throughput.
4. Incomplete evidence capture can weaken release auditability.

### Rollback Strategy

1. If E2E signal quality degrades, keep E2E non-blocking and reduce suite to deterministic smoke subset.
2. If deployment smoke becomes unstable, freeze promotion and route failures to release risk review.
3. If agent execution causes operational disruption, disable affected agent workflow while preserving logs and artifacts for diagnosis.
4. Any blocking-promotion rollback must be reflected in architecture docs before workflow enforcement changes.

## Task ID Mapping (T01-T09)

| Task ID | Objective | Primary Output | Status Type |
| --- | --- | --- | --- |
| T01 | Create architecture-level E2E stabilization plan | This document | Confirmed complete |
| T02 | Define initial Playwright smoke scenarios and ownership | Smoke scenario catalog and owners | Confirmed complete |
| T03 | Add E2E execution agent workflow | Agent definition, command contract, artifact paths | Confirmed complete |
| T04 | Add deployment verification agent workflow | Post-deploy smoke execution and evidence contract | Confirmed complete |
| T05 | Integrate non-blocking E2E run into PR/integration cadence | CI or task orchestration wiring | Planned |
| T06 | Establish evidence retention and reporting format | Run log schema and retention policy | Planned |
| T07 | Execute stabilization trend window and collect metrics | 4+ week trend evidence | Planned |
| T08 | Decide promotion to blocking gate and update baseline docs | Governance-approved gate policy update | Pending decision |
| T09 | Add E2E UIUX evidence agent workflow | Agent definition, routing contract, UIUX evidence dimensions, and artifact expectations | Confirmed complete |

## Alignment Notes

1. This plan does not override ADR 0029; it operationalizes Playwright usage in staged delivery.
2. This plan does not override ADR 0035; it preserves current blocking gates while E2E stabilizes.
3. Any future switch to blocking E2E requires a documented update to quality gate baseline and workflow documents in the same change.