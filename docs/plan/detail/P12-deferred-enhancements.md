# P12 — Deferred Enhancements (Detailed Plan)

**Phase status:** **Not Started** (2026-07-01; catch-all idle — no active slice) | **Depends on:** P0–P11 (MVP chain), P19 (publish-gate checklist), P21 (lifecycle UI)

> Single-active-phase invariant: **No formal phase `In Progress`** — **no active P12 slice**
> (2026-06-29; **P12-BDD-RISK-PROMPT-UX-001 Done**). **P21 remains Done** — do not reopen P21 phase status; **P19 remains Done**
> — corrective residual only (P19-T08/T10 group UX superseded). AUD-M02 **resolved** via **P12-AUD-M02 Done**
> (cross-reference P21 §11.4).

## 1. Purpose

Catch-all for deferred/post-MVP enhancements that do not warrant a new numbered phase.
Each slice is activated individually via `plan-orchestrator`; only one slice should be
`In Progress` at a time within P12.

## 2. Slice registry

| ID | Task | Status | Traceability |
| --- | --- | --- | --- |
| **P12-AUD-B10** | Submit-for-approval evidence checklist gate (AUD-B10 remediation) | **Done** (2026-07-01) | P21 audit finding AUD-B10 **resolved**; P21-T09b confirm-on-behalf UI retained |
| **P12-AUD-M02** | Role constants single-source (AUD-M02 remediation) | **Done** (2026-07-01) | P21 audit finding AUD-M02 **resolved**; P21 §11.4 |
| **P12-BDD-RISK-PROMPT-UX-001** | Template-scoped risk-prompt config UX redesign (BDD-TEMPLATE-RISK-PROMPT-UX-001) | **Done** (2026-06-29) | Supersedes P19-T08 group override + list-view panel + hardcoded decision categories; **COR-T15** mirror |

**Active slice:** **None** (2026-06-29 — **P12-BDD-RISK-PROMPT-UX-001 Done**; P12 catch-all idle at slice level).

### P12-AUD-M02 — Role constants single-source

**Origin:** Code-grounded audit **AUD-M02** (🟢 minor) — `MANAGEMENT_ROLES` in `auth/roles.ts`
defines only four roles while `MANAGEMENT_ROLE_VALUES` in `types/identity.ts` is the eight-role
SSOT; `TEMPLATE_TESTER`, `TEMPLATE_APPROVER`, and `MASTER_DESIGNER` appear as bare string
literals across capability-check and identity-assignment paths.

**Depends on:** P21 (`auth/roles.ts`, `auth/identityRoles.ts`, `types/identity.ts`, capability
composables and journey utilities established during role-journey redesign).

**Scope (confirmed 2026-07-01):**

1. **Refactor (TDD):** Extend `MANAGEMENT_ROLES` in `frontend/src/auth/roles.ts` to all eight
   management roles **or** derive role constants from `MANAGEMENT_ROLE_VALUES` SSOT in
   `frontend/src/types/identity.ts` — one authoritative source, no duplicate role string literals.
2. **Replace bare strings:** Update `roles.ts`, `identityRoles.ts`, and other frontend
   capability-check files that compare roles via raw `'MASTER_DESIGNER'`, `'TEMPLATE_TESTER'`,
   `'TEMPLATE_APPROVER'`, etc., to use the SSOT constants.
3. **Tests:** Update `roles.test.ts` (and affected unit tests) to assert against SSOT constants;
   no new user-facing surfaces.
4. **BDD:** `not-applicable` — refactor only; no behavior, API, permission, or L1 copy change.

**Exit criteria (slice):**

- All eight management roles reachable from a single SSOT (`MANAGEMENT_ROLES` or
  `MANAGEMENT_ROLE_VALUES` derivation); no bare role string literals in capability-check paths
  covered by this slice.
- `roles.test.ts` and related unit tests green; frontend lint/type-check/test/build pass.
- AUD-M02 **resolved** in P21 audit table (cross-reference); evidence recorded in ledger on close.
- No user-visible behavior change.
- P21 phase status unchanged (**Done**).

**Status:** **Done** (2026-07-01).

**Gate evidence:** frontend `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**521** Vitest,
92 files); BDD `not-applicable` (refactor only; no behavior change).

**Deliverables:** `MANAGEMENT_ROLES` derived from `MANAGEMENT_ROLE_VALUES` SSOT in `types/identity.ts`;
`auth/roles.ts`, `auth/identityRoles.ts`, `auth/contentModuleRoles.ts`, `navigation/navStructure.ts`,
`composables/useWorkflowTasks.ts`, journey utilities (`masterDesignerJourney.ts`, `globalAdminJourney.ts`,
`auditAdminJourney.ts`), `DashboardView.vue`; `roles.test.ts`, `identityRoles.test.ts` updated.

### P12-AUD-B10 — Submit-for-approval evidence checklist gate

**Origin:** Code-grounded audit **AUD-B10** (🟡) — `submitForApproval` has no evidence
checklist gate; publish path already uses `PublishGateService.assertReady` (P19-T06).
P21-T09b delivered confirm-on-behalf exception UI only; checklist gate remains open.

**Depends on:** P19 (`PublishGateService`, publish-gate checklist model), P21 lifecycle UI
(`TemplateDetailView`, `TemplateLifecycleDecisionDialog`, `TemplatePublishSummaryDialog` pattern).

**Scope (confirmed 2026-06-30):**

1. **Backend (TDD):** Server-side gate on `TemplateLifecycleService.submitForApproval`
   mirroring `PublishGateService` — evaluate live checklist (anchor integrity, variable schema,
   rule bounds, test results, preview, change-diff, coverage thresholds, approval-evidence
   readiness as applicable) and **fail-closed** with structured blocker response when
   unresolved blockers exist; allow submit only when checklist passes or user confirms
   documented exceptions per policy.
2. **Frontend (TDD + E2E):** Pre-submit summary dialog before submit-for-approval (reuse
   publish-summary dialog pattern — `TemplatePublishSummaryDialog` / checklist panel) showing
   gate results; block or warn per backend contract; business-friendly L1 copy (en/zh, keys
   stable).
3. **BDD:** Behavior spec — actor (template author/orchestrator), trigger (submit for approval
   from APPROVAL+PENDING_SUBMIT), preconditions (test passed, coverage/evidence state),
   acceptance Given/When/Then for pass/block/warn paths; boundary — below-threshold coverage,
   missing preview, open blockers.
4. **Gates:** Backend `mvn verify`; frontend lint/type-check/test/build; Playwright functional
   + UIUX evidence for submit-for-approval checklist journey.

**Exit criteria (slice):**

- `submitForApproval` invokes publish-gate-equivalent checklist server-side; API returns
  blockers when fail-closed.
- UI shows evidence checklist summary before submit; user cannot bypass unresolved hard blockers.
- AUD-B10 **resolved** in P21 audit table (cross-reference); green gates + E2E/UIUX evidence
  recorded in ledger.
- P21 phase status unchanged (**Done**).

**Status:** **Done** (2026-07-01).

**Gate evidence:** backend `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (**558** Surefire);
frontend `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**520+** Vitest);
Playwright `P12-AUD-B10-submit-approval-gate.spec.ts` **3/3**; UIUX
`P12-AUD-B10-uiux-evidence.spec.ts` **6** screenshots + manifest **PASS**
(`frontend/e2e/evidence/P12-AUD-B10-uiux-manifest.md`).

**Deliverables:** `PublishGatePhase.SUBMIT_FOR_APPROVAL`, `PublishGateService.assertReadyForSubmitForApproval`,
`TemplateLifecycleService` PENDING_SUBMIT guard; `TemplateSubmitForApprovalSummaryDialog`,
`submitGateReady` SSOT, lifecycle tab + author journey CTA wiring; i18n en/zh +
`api.error.template.submitForApprovalGateBlocked`.

### P12-BDD-RISK-PROMPT-UX-001 — Template-scoped risk-prompt config UX redesign

**Origin:** Confirmed BDD **BDD-TEMPLATE-RISK-PROMPT-UX-001** (2026-07-02) — P19-T08/T10 delivered
group-scoped override on `TemplateListView` and hardcoded categories in
`TemplateLifecycleDecisionDialog`; product intent is **global default + optional template override**,
decision dialog wired to effective resolve chain.

**Depends on:** P19 (`RiskPromptConfigService`, V21 `risk_prompt_config`, `DecisionFormService`,
`TemplateLifecycleDecisionDialog`), P21 template detail/create UX, permission-matrix §5, ADR-0021
consequences update.

**BDD readiness:** **READY** — spec in `docs/requirements/requirements-plan.md` §
BDD-TEMPLATE-RISK-PROMPT-UX-001; PRD §7; domain-model lifecycle §4; permission-matrix §5.

**Traceability:** P19-T08R / P19-T10R (residual); **COR-T15** (COR-2 template lifecycle residual).

| Sub-task | Owner | Scope | Status |
| --- | --- | --- | --- |
| **P12-BDD-RISK-PROMPT-UX-001-T01** | backend-engineer | Flyway template override storage; `GET/PUT …/templates/{templateId}/risk-prompt-config`; global API GLOBAL-only (deprecate user-facing GROUP upsert); `resolve(templateId)` chain (override → global); audit `RISK_PROMPT_CONFIG_UPDATED`; fail-closed group auth; validation (≥1 category when `useDefault: false`); OpenAPI; unit tests S1–S3, S5–S6, S9–S10 | Done |
| **P12-BDD-RISK-PROMPT-UX-001-T02** | backend-engineer | Decision/load path: effective categories by `templateId` for TEST_FAIL / APPROVAL_REJECT (lifecycle or dedicated resolve); extend `RiskPromptConfigServiceTest`; no change to submit/publish gate semantics (S8) | Done |
| **P12-BDD-RISK-PROMPT-UX-001-T03** | frontend-engineer | Refactor `TemplateRiskPromptConfigPanel` (inherit global, no scope radio/groupCode); **remove** from `TemplateListView`; `TemplateCreateDialog` collapsed `el-collapse` 「测试与审批退回原因（可选）」; template detail dedicated section; template-scoped API client; i18n labels/tooltips/section intro (BINDING_ISSUE = return reason); Vitest S3–S4, S11–S12 | Done |
| **P12-BDD-RISK-PROMPT-UX-001-T04** | frontend-engineer | Wire `TemplateLifecycleDecisionDialog` to resolved effective config (not hardcoded `TEMPLATE_DECISION_REASON_CATEGORIES`); human-readable labels + negative-decision-only tooltips; Vitest S7 | Done |
| **P12-BDD-RISK-PROMPT-UX-001-T05** | e2e-test-engineer + e2e-uiux-reviewer | Playwright: create without expand → global categories in decision; override subset filters decision; list view has no config panel; BINDING_ISSUE copy distinct from binding gate panel; UIUX manifest | Done |

**Suggested implementation order:** T01 → T02 → T03 → T04 → T05 (backend contract before frontend wiring).

**Exit criteria (slice):**

- All 12 BDD scenarios (S1–S12) evidenced; legacy GROUP override not in user-facing API/UI resolution chain.
- `TemplateListView` has no risk-prompt panel; decision dialog uses template-effective categories.
- Green gates: `mvn verify` + frontend lint/type-check/test/build + Playwright slice + UIUX review.
- P19-T08/T10 historical Done rows unchanged; residual note in [P19 detail](./P19-verifiability-publish-gate.md) § Residual.
- Ledger + post-task-doc-sync on close.

**Status:** **Done** (2026-06-29 — T01–T05 complete; Playwright **4/4** functional + UIUX **1/1** PASS; manifest PASS).

## 3. Slice backlog (not active)

| ID | Summary | Status | Notes |
| --- | --- | --- | --- |
| — | Additional deferred items from master-plan § Deferred / post-MVP | Not Started | Pick next slice via `plan-orchestrator` when prioritized |

## 4. Phase exit criteria

P12 phase is **Not Started** (catch-all idle) while individual slices run under `plan-orchestrator`.
**P12-AUD-M02** closed **Done** 2026-07-01. **P12-AUD-B10** closed **Done** 2026-07-01.

## 5. Behavior specification (BDD) — P12-AUD-B10

**BDD readiness:** `ready` (confirmed against permission matrix §5 提交审批, ADR-0021 test/approval
evidence governance, P19 `PublishGateService` pattern, P21 AUD-B10 gap; no blocking pending
questions).

**Traceability:**

| Source | Reference |
| --- | --- |
| Audit | P21 §11.3 **AUD-B10** — `handleSubmitForApproval` calls API directly without evidence gate |
| Plan | This doc §2 P12-AUD-B10; P21 §11.3 remediation owner |
| ADR | [ADR-0021](../../adr/template-lifecycle/0021-template-testing-approval-release-governance.md) — test pass requires evidence confirmation; approval submission creates approval work item only after author action; publication has separate final confirmation |
| Permission | [permission-matrix.md](../../security/permission-matrix.md) §5 提交审批 — submit materials must include test records, batch test summary, coverage summary, generation preview, final artifact reference, preview-comparison summary, change-diff summary, and pre-release checklist results |
| Publish gate (pattern) | P19-T06 `PublishGateService.evaluate` / `assertReady`; `api.error.template.publishGateBlocked` |
| Terminology | [business-terminology-guide.md](../../product/business-terminology-guide.md) §4.2/§4.4 — L1 copy; no "gate" on L1 |
| Code gap | `TemplateDetailView.vue` `handleSubmitForApproval` (direct API); contrast `handlePublish` → `publishGateReady` → `TemplatePublishSummaryDialog` → `publishGateService.assertReady` on server |

**Out of scope (this slice):** P21-T09b confirm-on-behalf UI in approval **decision** dialog;
administrator exception intervention on test/approval **decisions**; delegated/proxy approval;
new checklist check codes beyond lifecycle-phase filtering of existing `PublishGateCheckCode` values.

### 5.1 Actor / role

- **Primary:** `TEMPLATE_AUTHOR` (template orchestrator) with `authorTemplates` on the template's
  group scope.
- **Also eligible:** `MASTER_DESIGNER`, `GROUP_ADMIN`, `GLOBAL_ADMIN` when matrix §5 提交审批
  permits submit in scope (same lifecycle action, same gate).
- **Not eligible:** `TEMPLATE_TESTER`, `TEMPLATE_APPROVER` for submit-for-approval (they execute
  test/approval decisions, not submission).

### 5.2 User goal

Before creating an approval-queue work item, the submitter **reviews a pre-submit evidence
checklist** and **confirms** submission. Unresolved **hard blockers** prevent submit (UI and API).
The flow mirrors publish pre-release checks (P19/P21) but at the **submit-for-approval**
lifecycle phase.

### 5.3 Trigger

User activates **Submit for approval** (`templates.lifecycle.submitApproval`) from:

- Template detail lifecycle panel (`TemplateDetailLifecycleTab`), or
- Template author journey CTA (`handleJourneySubmitForApproval` → same handler).

### 5.4 Preconditions

| Condition | Required |
| --- | --- |
| `lifecycleStatus === 'APPROVAL'` | Yes |
| `approvalSubState === 'PENDING_SUBMIT'` | Yes (business: "Ready to submit for approval") |
| Prior test decision `PASSED` | Yes (implicit in status/substate) |
| `authorTemplates` (or matrix-equivalent write + submit) | Yes |
| Live checklist evaluable (`PublishGateService` phase `SUBMIT_FOR_APPROVAL`) | Yes |
| `APPROVAL` + `PENDING_DECISION` | **Not eligible** — already submitted |

### 5.5 Checklist scope (lifecycle-phase filter)

Reuse `PublishGateService` evaluation with phase **`SUBMIT_FOR_APPROVAL`** (same live data
sources as publish; different blocker set):

| `PublishGateCheckCode` | Submit-for-approval role |
| --- | --- |
| `ANCHOR_INTEGRITY`, `VARIABLE_SCHEMA`, `RULE_BOUNDS`, `TEST_RESULTS`, `PREVIEW_PRESENT`, `COVERAGE_THRESHOLDS`, `CONTENT_MODULE_REFERENCES`, `BLOCKER_STATUS` | **Hard blocker** when not ready |
| `CHANGE_DIFF` | **Informational** (warn/display only; same as publish) |
| `APPROVAL_SUMMARY`, `API_POLICY` | **Excluded** — publish-only (approval not yet recorded; API contract summary is publish-summary concern per matrix §5 发布模板) |

**Submit gate ready (frontend SSOT):** `checklist.ready === true` for the submit phase (no
`releaseVersion` or semver conflict requirement — those are publish-only).

### 5.6 Primary journey

1. User opens template detail in `APPROVAL` / `PENDING_SUBMIT`.
2. Lifecycle panel loads live checklist (same fetch pattern as publish gate; phase-aware response).
3. User clicks **Submit for approval**.
4. System opens **submit-for-approval summary dialog** (mirror `TemplatePublishSummaryDialog` layout:
   checklist progress, coverage / change-diff / preview-comparison summaries; **no** release-version row).
5. User reviews items; **Confirm submit** enabled only when no hard blockers (`hasBlockers === false`).
6. On confirm, client calls `POST …/submit-for-approval` with optional `commentSummary`.
7. Server: authz → status/substate guard → **`PublishGateService.assertReady(…, SUBMIT_FOR_APPROVAL)`**
   (fail-closed) → lifecycle transition `SUBMIT_FOR_APPROVAL` → `approvalSubState` becomes
   `PENDING_DECISION` → `CollaborationWorkItemWriter.upsertSubmitForApprovalWorkItem`.
8. UI success toast (`templates.lifecycle.submitApprovalSuccess`); refresh detail / journey step.

### 5.7 System responses

**Success path**

- HTTP 200 envelope with updated `TemplateDetailView`; `lifecycleStatus=APPROVAL`,
  `approvalSubState=PENDING_DECISION`; OPEN APPROVAL collaboration work item created (existing P21-T07 behavior).

**Fail-closed — hard blockers (API bypass or stale UI)**

- HTTP 422 (or existing validation status) with `error.code` / `error.messageKey`:
  **`api.error.template.submitForApprovalGateBlocked`** (parallel to `publishGateBlocked`).
- English base message: stable, safe summary (e.g. "Submit for approval is blocked until pre-release checks pass.").
- **No** lifecycle transition; **no** approval work item created.

**Fail-closed — authorization / status**

- Wrong status/substate, missing capability, or cross-group access: existing fail-closed lifecycle
  errors (unchanged).

**Checklist load failure**

- Same degrade pattern as publish gate (AUD-B06 remediation): show `LoadErrorPanel` with retry;
  submit CTA disabled while checklist unavailable.

### 5.8 Acceptance scenarios (Given / When / Then)

**(Pass)** Given a template in `APPROVAL` with `approvalSubState=PENDING_SUBMIT` and all submit-phase
checklist hard items ready, When the author clicks **Submit for approval** and confirms the summary
dialog, Then the summary dialog opened before the API call, And `submitForApproval` succeeds, And
status becomes `APPROVAL` / `PENDING_DECISION`, And an OPEN APPROVAL work item exists.

**(Hard blocker — UI)** Given a submit-phase hard blocker (e.g. coverage below threshold, missing
batch test run, anchor integrity blocking), When the author clicks **Submit for approval**, Then
the summary dialog shows the blocker item(s) as pending, And **Confirm submit** is disabled, And
no `submitForApproval` API call occurs until blockers clear.

**(Hard blocker — API fail-closed)** Given hard blockers present but a client bypasses the dialog,
When `submitForApproval` is invoked, Then the API returns `submitForApprovalGateBlocked`, And
lifecycle status remains `APPROVAL` / `PENDING_SUBMIT`, And no new approval work item is created.

**(Warn-only / informational)** Given all hard items ready but informational items show warnings
(e.g. `CHANGE_DIFF` has changes, preview-comparison non-blocker diffs), When the author opens the
summary dialog, Then warnings are visible with business L1 copy, And **Confirm submit** remains
enabled, And submit succeeds when confirmed.

**(Wrong substate)** Given `APPROVAL` / `PENDING_DECISION`, When user attempts submit-for-approval,
Then submit CTA is not shown (existing capability matrix), And direct API call is rejected by
status guard.

**(Checklist unavailable)** Given publish-gate fetch failed, When lifecycle panel renders, Then
load error is shown with retry, And submit-for-approval CTA does not proceed without checklist data.

### 5.9 Boundary and exception behavior

- Below-threshold coverage, missing preview, open binding blockers, failed batch test blockers,
  invalid content-module references: **hard block** (same thresholds as publish gate items).
- Administrator exception intervention applies to **test/approval decisions** (P21-T09b), **not**
  to bypass submit-for-approval checklist blockers in v1.
- Direct API submission without prior test pass / wrong substate: existing lifecycle guards; no
  work item side effects on gate failure.

### 5.10 Observable evidence

| Layer | Evidence |
| --- | --- |
| UI | Summary dialog visible before submit; confirm disabled with blockers; success toast + substate badge → "Awaiting approval" |
| API | `submitForApproval` response or `submitForApprovalGateBlocked` with `messageKey` |
| Backend test | `TemplateLifecycleService` / gate service tests: gate invoked before transition |
| E2E | Playwright journey: pass path + blocker path on template detail lifecycle tab |
| Audit | Existing submit-for-approval audit trail (matrix §10); no new audit event type required |

### 5.11 L1 copy requirements (en primary, zh-CN additive)

| Surface | en (base) | zh-CN (additive) | Key namespace (stable) |
| --- | --- | --- | --- |
| Summary dialog title | Review before submit for approval | 提交审批前确认 | `templates.submitApprovalSummary.title` |
| Summary dialog intro | Confirm test and preview evidence before sending for approval. | 提交审批前请确认测试与预览证据。 | `templates.submitApprovalSummary.description` |
| Checklist section heading | Pre-release checks | 上线前检查 | Reuse `templates.publishGate.title` or scoped alias |
| Confirm button | Submit for approval | 提交审批 | `templates.submitApprovalSummary.confirm` |
| Blockers note | Fix pending items before submitting. | 请先处理待完成项再提交。 | `templates.submitApprovalSummary.blockersPresent` |
| API error (base bundle) | Submit for approval is blocked until pre-release checks pass. | （zh backend bundle additive） | `api.error.template.submitForApprovalGateBlocked` |

Constraints: no user-facing "gate" / "checklist gate" on L1; reuse existing
`templates.publishGate.checkCodes.*` labels for shared check rows; keys stable across en/zh-CN.

### 5.12 Pending questions

None blocking implementation.
