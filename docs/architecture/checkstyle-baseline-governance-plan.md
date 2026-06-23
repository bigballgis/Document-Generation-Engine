---
id: DOC-ARCH-CHECKSTYLE-BASELINE-GOVERNANCE-PLAN
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
  - implementation
dependsOn:
  - docs/architecture/quality-gate-threshold-baseline.md
  - docs/governance.md
  - docs/architecture/m14-task-sheet.md
related:
  - backend/pom.xml
  - docs/architecture/tdd-delivery-workflow.md
---

# Checkstyle Baseline Governance Plan

## Purpose

This plan records a controlled path to close repository-wide Checkstyle baseline debt without weakening existing quality-gate commitments.

## Current Evidence Snapshot

As of 2026-06-11:

1. M14 focused tests are green:
   - RuntimeBatchEndpointAdapterTest: 8 tests, 0 failures, 0 errors.
   - RuntimeBatchSpringControllerTest: 4 tests, 0 failures, 0 errors.
2. SpotBugs report is clean (`total_bugs='0'`).
3. Checkstyle repository baseline remains noisy.
4. Local convergence evidence for this wave:
  - `TemplateLifecycleOperationService.java` and `TemplateReviewWorkflowService.java` pass focused validation with 0 errors.
  - Latest local hotspot reduction focused on Javadoc and line-length debt in the template lifecycle workflow services.

## Constraints

1. Do not change accepted technology baseline.
2. Do not lower quality thresholds by default.
3. Do not treat temporary local command success as governance closure.
4. Keep all exceptions explicit, time-bound, and traceable.

## Staged Closure Proposal

### Stage A: Delta-Clean Rule

1. Requirement: changed files in each wave must pass focused Checkstyle includes.
2. Enforcement: keep full Checkstyle run visible, but gate merge readiness on delta-clean plus no new full-scope escalation.
3. Exit condition: two consecutive waves satisfy delta-clean.

### Stage B: Module Baseline Burn-Down

1. Prioritize one module family per wave (for example runtime adapters first).
2. For each wave, reduce warning count by agreed minimum quota.
3. Track burn-down evidence in wave task sheets.

### Stage C: Full-Gate Restoration

1. Re-enable strict full-scope Checkstyle blocking when baseline warnings reach agreed threshold.
2. Remove transitional notes from task sheets and governance docs in the same change.

## Confirmed Decisions (2026-06-10)

User confirmed the following governance parameters:

1. Stage A delta-clean is accepted as temporary gate pass criterion for M14 closure.
2. Stage B warning burn-down quota is at least 200 warnings per wave.
3. Stage C full-gate restoration threshold is 300 warnings.

## Stage B Execution Evidence

### Wave 1 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 3816 to 3346.
3. Net burn-down in this wave: -470 warnings (meets quota >= 200).
4. Targeted hotspot deltas:
  - `RuntimeBatchEndpointAdapter.java`: 263 -> 0 (-263).
  - `RuntimeHttpTransportController.java`: 207 -> 0 (-207).

### Wave 2 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 3346 to 2872.
3. Net burn-down in this wave: -474 warnings (meets quota >= 200).
4. Targeted hotspot deltas:
  - `ApiPolicyManagementService.java`: 313 -> 28 (-285).
  - `ApiCredentialLifecycleService.java`: 218 -> 28 (-190).

### Wave 3 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 2872 to 1599.
3. Net burn-down in this wave: -1273 warnings (meets quota >= 200).
4. Targeted hotspot deltas:
  - `ApiPolicyDefaultRouteGovernanceService.java`: 209 -> 20 (-189).

### Wave 4 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count increased from 1599 to 2575.
3. Net change in this wave: +976 warnings.
4. Targeted hotspot deltas:
  - `RuntimeGenerateRequestValidationService.java`: 120 -> 12 (-108).

### Wave 5 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 2575 to 2530.
3. Net burn-down in this wave: -45 warnings.
4. Targeted hotspot deltas:
  - `ContractPublicationReadModel.java`: 202 -> 157 (-45).

### Wave 6 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 2530 to 2413.
3. Net burn-down in this wave: -117 warnings.
4. Targeted hotspot deltas:
  - `TemplateReviewWorkflowService.java`: 132 -> 15 (-117).

### Wave 7 (2026-06-10)

1. Full-scope Checkstyle run completed before and after bulk remediation (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 2413 to 390.
3. Net burn-down in this wave: -2023 warnings.
4. Indentation-specific convergence:
  - IndentationCheck warnings reduced from 193 to 0.
  - Remaining IndentationCheck files reduced from 4 to 0.
5. Automation path:
  - Added script `scripts/fix-checkstyle-indentation.ps1` for report-scoped first-pass normalization.
  - Added script `scripts/fix-checkstyle-indentation-from-report.ps1` for expected-level replay from Checkstyle messages.

### Wave 8 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 390 to 355.
3. Net burn-down in this wave: -35 warnings.
4. Targeted hotspot deltas:
  - `ApiPolicyManagementService.java`: 28 -> 14 (-14).
  - `ApiCredentialLifecycleService.java`: 28 -> 7 (-21).

### Wave 9 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 355 to 313.
3. Net burn-down in this wave: -42 warnings.
4. Targeted hotspot deltas:
  - `ContractPublicationReadModel.java`: 26 -> 11 (-15).
  - `ApiPolicyDefaultRouteGovernanceService.java`: 20 -> 6 (-14).
  - `SyncGenerationOutputDeliveryService.java`: 16 -> 3 (-13).

### Wave 7 (2026-06-10)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 2413 to 2303.
3. Net burn-down in this wave: -110 warnings.
4. Targeted hotspot deltas:
  - `ContractPublicationReadModel.java`: 157 -> 47 (-110).

### Wave 10 (2026-06-11)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 313 to 306.
3. Net burn-down in this wave: -7 warnings.
4. Targeted hotspot deltas:
  - `RuntimeGenerateEndpointAdapter.java`: 15 -> 8 (-7).

### Wave 11 (2026-06-11)

1. Full-scope Checkstyle run completed via Maven task (`mvn -f backend/pom.xml -DskipTests checkstyle:checkstyle`).
2. Repository-wide warning count reduced from 306 to 276.
3. Net burn-down in this wave: -30 warnings.
4. Targeted hotspot deltas:
  - `TemplateLifecycleOperationService.java`: 15 -> 0 (-15).
  - `TemplateReviewWorkflowService.java`: 15 -> 0 (-15).