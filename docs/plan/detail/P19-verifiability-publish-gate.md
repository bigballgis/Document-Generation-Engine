# P19 — Template Verifiability, Publish Gate & Decision Forms (Detailed Plan)

**Phase status:** Not Started | **Depends on:** P3, P4, P5 (and P18 for fidelity inputs)
**Confirmed:** 2026-06-23 (deep-review gaps G4 + G5)

> Single-active-phase invariant: P13 completed **Done** (2026-06-23); there is currently no
> single active phase. P19 stays `Not Started` until selected as the next active phase.

## Source-of-truth & traceability

- PRD §6.5 (模板可验证性与发布门禁), §7 (模板生命周期 — 测试/审批意见、风险提示、例外干预) — confirmed.
- permission-matrix §5 (维护测试数据集 / 维护测试审批意见模板和风险提示文案 / 测试通过/不通过 /
  审批通过/不通过 / 发布) — confirmed.
- requirements-plan 已确认：模板可验证性与发布门禁 / 模板测试与审批.
- Backlog origin: UXC1 (live publish-gate checklist) Not Started; UXB5 (persist authoring).

## Gap evidence

- Test-data-set CRUD exists (`TestDataSetService`), but **multi-sample coverage thresholds,
  batch test generation, change-diff summary, side-by-side preview comparison, and a live
  publish-gate checklist** are thin-slice / static (publish-gate "API policy" item is static
  text — UXC1).
- No controlled test/approval opinion forms; no risk-prompt copy configuration; no
  admin-exception-intervention reason + secondary-confirm + separate audit marker.

## Behavior goal

Make template verifiability a real publish-gating capability: required vs optional named
test samples, batch test generation with per-sample records, sample + template coverage
against configurable thresholds, change-diff summary, preview-vs-final side-by-side
comparison, and a live publish-gate checklist that blocks publish on unresolved blockers
or below-threshold coverage. Capture test/approval decisions via controlled opinion forms
(not the rich-text editor), configurable risk-prompt copy (global default + group override),
and mark group-admin exception interventions with reason + secondary confirm + separate audit.

### Acceptance scenarios

- **Given** a publish candidate, **When** an author runs batch test over multiple samples,
  **Then** per-sample test-generation records, preview artifacts, warning/blocker summaries,
  and coverage summary are produced; coverage summary stores no variable plaintext.
- **Given** template + key-dimension coverage below the configured threshold, **When**
  publish is attempted, **Then** it is a **blocker**; publish is disabled with reasons shown.
- **Given** test/approval views, **When** opened, **Then** generation preview, change-diff
  summary (content, anchors, variables, rules, contract summary), preview-vs-final
  comparison, and the publish-gate checklist result are visible per role/group scope.
- **Given** a tester decision, **When** pass/fail is recorded, **Then** it uses a controlled
  form (pass: confirm evidence/coverage/preview/fidelity summaries; fail: reason category +
  impact + fix), structured result + audit; fail returns template to DRAFT.
- **Given** an approver decision, **When** approve/reject is recorded via controlled form,
  **Then** approve captures rationale + key-evidence confirmation; reject captures category +
  impact + remediation linked to test record / change-diff / checklist; audited.
- **Given** a GROUP admin exception intervention on test/approval, **When** executed, **Then**
  a mandatory reason + secondary confirmation is captured and a separate exception marker is
  written to audit.
- **Given** an admin, **When** risk-prompt copy / reason categories are configured (global
  default or group override), **Then** changes are audited; copy carries no sensitive data.

## Tasks

| ID | Task | Status |
| --- | --- | --- |
| P19-T01 | Multi-sample test data sets: required/optional flags, scenario name, coverage tags, dataset version; lock-on-evidence + copy/derive | Not Started |
| P19-T02 | Batch test generation (multi-sample) → per-sample records + batch test summary | Not Started |
| P19-T03 | Sample + template coverage computation (variables/required, condition/loop/rule branches, anchors/sections, table components, DOCX/PDF) + configurable thresholds | Not Started |
| P19-T04 | Change-diff summary (content, anchors, variables, rules, release-candidate contract summary) | Not Started |
| P19-T05 | Preview-vs-final side-by-side comparison (page/anchor/section/component diff → warning or blocker) | Not Started |
| P19-T06 | Live publish-gate checklist (anchor integrity, variable schema, rule bounds, test results, preview, change-diff, approval summary, blocker status) — blocks publish | Not Started |
| P19-T07 | Controlled test/approval opinion forms (structured result + comment), not rich-text editor | Not Started |
| P19-T08 | Risk-prompt copy + reason-category configuration (global default + group override) with audit | Not Started |
| P19-T09 | Group-admin exception intervention: reason + secondary confirm + separate audit marker | Not Started |
| P19-T10 | UI: verifiability panels, coverage/threshold display, live checklist, decision forms, risk-prompt config | Not Started |

## Exit criteria (phase)

- Multi-sample coverage + batch test + change-diff + preview comparison implemented; live
  publish-gate checklist blocks publish on unresolved blockers / below-threshold coverage.
- Controlled test/approval opinion forms + configurable risk prompts + exception-intervention
  audit markers implemented; no variable/customer plaintext persisted in evidence/audit.
- Group-scoped fail-closed authorization; audit on decisions and config changes.
- Green gates: `mvn verify` + frontend lint/type-check/test/build.
- PRD §6.5/§7 + permission-matrix §5 cross-links verified; ledger evidence recorded.

---

## Task-level behavior specs & test-first plan (2026-06-23)

> Anchored to current code. Activation requires P13 Done + plan-orchestrator selection
> (single-active-phase). Depends on P18 fidelity outputs for real warning/blocker data; until
> P18 lands, fidelity inputs use the current `PreviewGenerationService` warning shape.
> Each task: failing test first → smallest implementation → refactor → doc sync.

### Current-state anchors (verified)

- `TestDataSetEntity` — minimal (id, templateId, externalId, name, description,
  `variablesJson`, timestamps). **No** required/optional flag, scenario, coverage tags,
  dataset version, or lock-on-evidence.
- `TestDataSetService` — CRUD only; `requireReadableTemplate` gate (note: matrix restricts
  maintenance to GLOBAL/GROUP/MASTER_DESIGNER/TEMPLATE_AUTHOR — tighten in T01).
- `PreviewGenerationService.testGenerate` — single DOCX preview, **hardcoded single
  fidelity warning** (`CONTROLLED_STYLE_FALLBACK`), trivial `comparisonSummary`
  (`"anchorsConfigured=..;warnings=.."`). No batch, no coverage, no real change-diff, no
  real preview-vs-final comparison.
- `TemplateLifecycleService.publish` — sets PUBLISHED directly; **no publish-gate checklist**.
- `LifecycleDecisionRequest` — only `decision` + `commentSummary`; no controlled opinion form,
  risk prompts, or exception-intervention marker.

### Role sets (permission-matrix §5)

- Maintain test data sets / batch test / author: GLOBAL, GROUP (scoped), MASTER_DESIGNER,
  TEMPLATE_AUTHOR. Tester/approver: read-only in test/approval materials.
- Maintain opinion-form templates + risk-prompt copy: GLOBAL (global default), GROUP (scoped
  override). Exception intervention on test/approval: GROUP admin (scoped) with reason +
  secondary confirm + separate audit marker.

### P19-T01 — Multi-sample test data sets (required/optional, scenario, coverage tags, version, lock)

- **Behavior:** Extend test data set with `required` flag, `scenarioName`, `coverageTags`,
  `datasetVersion`, and a `locked` flag set once the set enters a test record / approval /
  publish evidence chain (then immutable; edits require copy/derive → new version). Tighten
  maintenance authorization to the confirmed role set.
- **Failing tests first** (`TestDataSetGovernanceServiceTest`):
  1. `create_withRequiredFlagAndScenario_persists`
  2. `update_lockedDataSet_throwsImmutable`
  3. `derive_fromLocked_createsNewVersion_unlocked`
  4. `maintain_byTesterRole_throwsAccessDenied`
- **Impl:** add columns (Flyway) + lock-on-evidence transition + copy/derive; authorization tighten.

### P19-T02 — Batch test generation (multi-sample)

- **Behavior:** Run test generation over a selected set of samples → per-sample preview record
  + per-sample warning/blocker summary + a batch test summary (totals, pass/fail, blockers).
- **Failing tests first** (`BatchTestGenerationServiceTest`):
  1. `batchTest_overThreeSamples_createsThreePreviewRecords`
  2. `batchTest_summary_aggregatesWarningsAndBlockers`
  3. `batchTest_oneSampleFails_summaryReflectsFailure`
- **Impl:** orchestrate `PreviewGenerationService` per sample; persist a batch-test summary.

### P19-T03 — Coverage computation + configurable thresholds

- **Behavior:** Compute sample coverage + template aggregate coverage across variables/required
  fields, condition/loop/rule branches, anchors/sections, table components, DOCX/PDF formats;
  compare to configurable thresholds (global default + group override). Coverage summary stores
  no variable plaintext.
- **Failing tests first** (`CoverageComputationServiceTest`):
  1. `coverage_countsRequiredVariablesExercised`
  2. `coverage_belowThreshold_isFlaggedAsBlocker`
  3. `coverage_summary_excludesVariablePlaintext`
- **Impl:** coverage calculator + threshold config (entity + global/group resolution).

### P19-T04 — Change-diff summary

- **Behavior:** Produce a change-diff over content, anchors, variables, rules, and
  release-candidate contract summary vs the previous published version.
- **Failing tests first** (`ChangeDiffServiceTest`):
  1. `diff_detectsAddedVariable`
  2. `diff_detectsAnchorBindingChange`
  3. `diff_noChange_returnsEmptyDiff`
- **Impl:** diff service comparing candidate vs last published snapshot.

### P19-T05 — Preview-vs-final side-by-side comparison

- **Behavior:** Replace the trivial `comparisonSummary` string with a structured comparison
  locating diffs to page/anchor/section/component, classified into warning vs blocker.
- **Failing tests first** (`PreviewComparisonServiceTest`):
  1. `comparison_locatesAnchorLevelDiff`
  2. `comparison_semanticDiff_classifiedAsBlocker`
  3. `comparison_lowRiskDiff_classifiedAsWarning`
- **Impl:** structured comparison model + classifier; supersede `comparisonSummary` string.

### P19-T06 — Live publish-gate checklist (blocks publish)

- **Behavior:** Before publish, evaluate a live checklist — anchor integrity, variable schema,
  rule bounds, test results, preview present, change-diff, approval summary, blocker status,
  coverage thresholds. Any unresolved blocker / below-threshold coverage prevents publish; the
  checklist result is returned and audited.
- **Failing tests first** (`PublishGateServiceTest`):
  1. `publish_withUnresolvedBlocker_isRejected_withChecklist`
  2. `publish_belowCoverageThreshold_isRejected`
  3. `publish_allGreen_succeeds_andRecordsChecklist`
  4. `publishGate_isEvaluatedLive_notStaticText`
- **Impl:** `PublishGateService` invoked by `TemplateLifecycleService.publish`; wire real checks.

### P19-T07 — Controlled test/approval opinion forms

- **Behavior:** Structured opinion forms (not the rich-text editor). Test pass: confirm
  evidence/coverage/preview/fidelity summaries (+ optional note). Test fail: reason category +
  impact + fix. Approve: rationale + key-evidence confirmation. Reject: category + impact +
  remediation linked to test record / change-diff / checklist.
- **Failing tests first** (`DecisionFormServiceTest`):
  1. `testPass_requiresFidelityViewedConfirmation`
  2. `testFail_requiresReasonCategoryAndImpact`
  3. `approvalReject_requiresCategoryAndRemediation_linksEvidence`
- **Impl:** structured decision form model extending `LifecycleDecisionRequest`; persist + audit.

### P19-T08 — Risk-prompt + reason-category configuration

- **Behavior:** Global default + group override for risk-prompt copy and reason categories
  (covering unresolved blockers, fidelity warnings, below-threshold coverage, preview-comparison
  diff, contract/scope change, admin exception intervention); changes audited; no sensitive data.
- **Failing tests first** (`RiskPromptConfigServiceTest`):
  1. `globalDefault_appliesWhenNoGroupOverride`
  2. `groupOverride_takesPrecedence_inScope`
  3. `configChange_isAudited`

### P19-T09 — Group-admin exception intervention markers

- **Behavior:** When a GROUP admin makes an exception test/approval decision, capture a
  mandatory reason + secondary confirm and write a separate exception marker to audit.
- **Failing tests first** (`ExceptionInterventionTest`):
  1. `groupAdminExceptionDecision_requiresReasonAndConfirm`
  2. `exceptionDecision_writesSeparateAuditMarker`
  3. `normalTesterDecision_hasNoExceptionMarker`

### P19-T10 — UI

- **Behavior:** Verifiability panels (sample list with required/scenario/coverage tags,
  batch-test trigger + summary), coverage/threshold display, live publish-gate checklist with
  per-item readiness + reasons (publish disabled until green), structured decision forms,
  risk-prompt config screen. Capability-gated.
- **Failing tests first** (Vitest): publish button disabled with blockers; decision form
  validation; coverage threshold display; batch-test summary rendering.

### Migration & sequencing

- Flyway: extend `template_test_data_set` (required/scenario/coverageTags/version/locked);
  add `template_batch_test_summary`, `coverage_threshold_config`, `risk_prompt_config`, and
  decision-form/exception-marker columns or tables.
- Suggested order: T01 → T02 → T03 → T05 → T04 → T06 (gate) → T07 → T09 → T08 → T10.
- Publish gating + decision capture are core → JaCoCo changed-line ≥90%; evidence/audit must
  never persist variable/customer plaintext (store hashes + non-sensitive summaries only).
