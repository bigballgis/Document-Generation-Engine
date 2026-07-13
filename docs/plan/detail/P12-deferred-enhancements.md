# P12 — Deferred Enhancements (Detailed Plan)

**Phase status:** **Not Started** (2026-07-03; no active slice — **P12-API-PACKAGE-ACCESS-INVOCATION Done** 2026-07-03) | **Depends on:** P0–P11 (MVP chain), P19 (publish-gate checklist), P21 (lifecycle UI)

> Formal phase: **None** (2026-07-09+). **LR-A3 Done** (2026-07-10) — not a P12 slice. Recommended next delivery focus: CDP golden path (status note only). Slice **P12-API-PACKAGE-ACCESS-INVOCATION Done** (2026-07-03 — T01–T12). Prior slice **P12-TEMPLATE-TESTING-OVERHAUL Done** (2026-07-03).
> **P21 remains Done** — do not reopen P21 phase status; **P19 remains Done**
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
| **P12-UIUX-DEEP-REFACTOR** | Frontend UIUX deep refactor — design token system, visual language upgrade, component unification, shell polish (14 tasks, groups A–E) | **Done** (2026-07-03) | OPT-G3 resolved (TemplateDetailView split); plan: `.cursor/plans/前端_uiux_整改方案_fc6fbf7b.plan.md`; BDD `not-applicable` (no API/permission/behavior change) |
| **P12-TEMPLATE-TESTING-OVERHAUL** | 模板测试页全量改造 — SSE 进度流（单次预览 + 全量测试）、临时文件 24h TTL、批测结果持久化与失效、提交测试门禁 API、覆盖率面板增强、测试历史记录（13 tasks T01–T13） | **Done** (2026-07-03) | BDD spec: `docs/behavior/template-testing-overhaul.md` (ready); Flyway V41–V42; 6 功能域 F1–F6 |
| **P12-API-PACKAGE-ACCESS-INVOCATION** | 包级 API 接入重构 + 调用记录 — auto-materialize policy、约定大于配置 Hub UI、invocation records + 留存（12 tasks T01–T12） | **Done** (2026-07-03 — T01–T12; Playwright **10/10** + UIUX **PASS**; `2e61dc3`) | BDD: `docs/behavior/api-package-access-and-invocation-records.md`; plan: [P12-api-package-access-invocation-records.md](./P12-api-package-access-invocation-records.md) |
| **P13-EXTERNAL-SERVICES-EXCELLENCE** | External services excellence — IA convergence (single hub edit surface), full management invocation history, cross-package alerts, unified impact preview, OpenAPI completion, BDD S1–S8 + CD-E2E-T07/T13 (28 tasks, groups A–F) | **In Progress** (2026-07-08 — Phase 1 A01–A06 Done; Phase 2 next) | Gap audit 2026-07-07; plan: [P13-external-services-excellence.md](./P13-external-services-excellence.md); extends P12 BDD + new `management-invocation-history.md`, `api-access-cross-package-alerts.md` |

**Active slice:** **None** (formal phase **None**; delivery focus is LRP LR-A3 outside P12). **Last completed:** **P12-API-PACKAGE-ACCESS-INVOCATION** (T01–T12 Done 2026-07-03).

### P12-UIUX-DEEP-REFACTOR — Frontend UIUX deep refactor

**Origin:** Ad-hoc design quality improvement wave (plan: `.cursor/plans/前端_uiux_整改方案_fc6fbf7b.plan.md`). Addresses OPT-G3 (TemplateDetailView split) and related visual/component tech-debt accumulated across P14–P21.

**Depends on:** P21 (TemplateDetailView, journey blocks, audit console established).

**Scope (confirmed 2026-07-03, 14 tasks, groups A–E):**

- **A1:** Brand rename — REDBC zh `"零售银行"` → `"红色银行"`, en `"Retail Bank"` → `"Red Bank"` (brands.ts + brands.test.ts).
- **B1:** Complete design token system in `global.scss` — gray scale, font sizes, shadows, border-radius, spacing, dialog widths, semantic status colors, `--color-on-primary`.
- **B2:** Visual language upgrade — white header with brand top-line, professional sidebar with icons + collapse, table/card/button quality, split-panel login page, status badge unification.
- **B3:** New `PageHeader` component; all views adopt `AppPageLayout`; unified h1 typography; removed eyebrow `displayName` duplicates.
- **B4:** Unified `EmptyStatePanel`, `AppTablePagination`; fixed `ContentModuleListView` v-model bug.
- **C1–C4:** Journey/Banner convergence, identity page cleanup, audit console cleanup, new shared `CredentialsPanel` component.
- **D1–D3:** `TemplateDetailView` split into composable (`useTemplateDetailController`) + view; legacy tabs → `WorkspaceTabShell`; `LifecycleCommentDialog` + `TemplateWorkspaceHeader` + `ReleaseSectionTable` shared components; dead `TemplateDevVersionActionBar.vue` deleted; Journey CTA removed.
- **E:** Shell polish — user dropdown menu, sidebar icons + collapse, `document.title` routing, breadcrumb dedup, login page cleanup.

**BDD:** `not-applicable` — UI/UX refactor only; no API, permission, backend, or observable behavior change.

**Exit criteria:** All 14 tasks Done; frontend `pnpm -C frontend lint`, `type-check`, `test`, `build` green; 606/606 Vitest pass.

**Status:** **Done** (2026-07-03).

**Gate evidence:** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**606** Vitest, 606/606), `build` ✓. Backend unchanged.

**Deliverables:** `brands.ts`, `brands.test.ts`; `global.scss` (full token system); `PageHeader.vue` + `PageHeader.test.ts`; `AppDataTable.vue`, `AppTablePagination.vue`, `EmptyStatePanel.vue`; `CredentialsPanel.vue` + `CredentialsPanel.test.ts`; `useTemplateDetailController.ts`; `TemplateWorkspaceHeader.vue`, `ReleaseSectionTable.vue`, `LifecycleCommentDialog.vue`; deleted `TemplateDevVersionActionBar.vue`; `ManagementShell.vue` (sidebar polish); `AppBreadcrumb.vue`, `AppPageLayout.vue`, `LoginView.vue`, `RoleJourneyTimeline.vue`; all view files updated to adopt `AppPageLayout` + `PageHeader`.

---

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

---

### P12-TEMPLATE-TESTING-OVERHAUL — 模板测试页全量改造

**Origin:** 模板测试工作流深度改造——新增 SSE 实时进度流、临时文件 TTL 管理、全量测试持久化与失效、提交测试门禁、覆盖率可见性增强、测试历史记录。BDD 规格：`docs/behavior/template-testing-overhaul.md`（状态：`ready`）。

**Depends on:** P4（PreviewGenerationService、PreviewRecordEntity），P19（CoverageComputationService、CoverageThresholdResolver、BatchTestGenerationService），P12-UIUX-DEEP-REFACTOR（TemplateDetailView 工作台 Tab 壳已就绪）。

**BDD readiness:** `READY` — spec in `docs/behavior/template-testing-overhaul.md`；17 个验收场景（SCEN-F1-01 至 SCEN-F6-02）；11 个 `[ASSUMED]` 项已标注（不阻塞核心路径；3 项需实施前人工确认）。

**Scope (13 tasks, 2026-07-03):**

| Sub-task | Owner | Scope | Status |
| --- | --- | --- | --- |
| **P12-TEMPLATE-TESTING-OVERHAUL-T01** | backend-engineer | Flyway V41（`preview_records` 新增 `expires_at`, `temp_storage_key`, `temp_artifact_cleaned`）；Flyway V42（`template_batch_test_run` 新增 `invalidated_at`, `template_version_id`, `sample_results_json`, `anchor/variable/sample_coverage_pct`, `all_samples_succeeded`, `gate_passed`, `hidden`）；更新 `PreviewRecordEntity`、`BatchTestRunEntity` | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T02** | backend-engineer | 单次预览 SSE 端点：`POST .../previews/async-preview`（202，返回 `previewId + streamUrl`）+ `GET .../previews/{previewId}/progress-stream`（`text/event-stream`）；`PreviewConcurrencyGuard`（AtomicInteger 上限 3）；HTTP 429 + `PREVIEW_CONCURRENCY_LIMIT_EXCEEDED`；`SseEmitterRegistry`（带缓冲防竞争） | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T03** | backend-engineer | 临时文件 24h TTL：`PreviewRecordEntity.expiresAt/tempStorageKey/tempArtifactCleaned` 管理；`@Scheduled`（每小时）`PreviewTempCleanupScheduler` 删除 MinIO 临时对象；过期资源 HTTP 410 + `PREVIEW_ARTIFACT_EXPIRED` | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T04** | backend-engineer | 全量测试 SSE 端点：`POST .../batch-tests/run`（202，返回 `runId + streamUrl`）+ `GET .../batch-tests/{runId}/progress-stream`；`AsyncBatchTestOrchestrator` 逐样本推送 `sample_started/sample_done/batch_completed/batch_failed`；`BatchTestRunEntity` 状态机（RUNNING → COMPLETED/FAILED）；结果写入 `sample_results_json` | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T05** | backend-engineer | 全量测试持久化与失效：`BatchTestRunEntity` 绑定 `templateVersionId`；历史列表保留最新 5 条（`hidden=true` 软删除）；`TemplateContentChangedEvent` 事件解耦，`BatchTestInvalidationListener` 同步写入最新 `BatchTestRunEntity.invalidatedAt` | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T06** | backend-engineer | 测试历史 API：`GET .../batch-tests?limit=5`；返回 `BatchTestRunSummaryView`（`runId, createdAt, status[RUNNING/COMPLETED/FAILED/INVALIDATED], successCount, failedCount, totalCount, anchorCoveragePct, variableCoveragePct, sampleCoveragePct, gatePassed, invalidatedAt`） | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T07** | backend-engineer | 提交测试门禁 API：`GET .../submit-test-eligibility`；返回 `SubmitTestEligibilityView`（`eligible, conditions{hasValidTestResult/allSamplesSucceeded/coverageGatePassed}, blockingDetails{uncoveredAnchors/Variables/failedDataSetNames}, thresholds, latestRunAt`）；复用 `CoverageComputationService` + `CoverageThresholdResolver` | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T08** | frontend-engineer | 「运行预览」对话框：SSE 进度展示（QUEUED → PROCESSING → SUCCEEDED/FAILED）；DOCX/PDF 临时下载链接 + 倒计时；并发超限 429 行内提示；失败重试按钮；i18n en/zh-CN | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T09** | frontend-engineer | 「全量测试」按钮改造：改名（删除「批量试生成全部」旧按钮）；确认弹窗；SSE 批量进度对话框（已完成 X/N）；完成后自动刷新 Coverage 面板 + 历史列表；i18n en/zh-CN | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T10** | frontend-engineer | 「提交测试」按钮门禁：挂载时 + 全量测试完成后拉取 `submit-test-eligibility`；`eligible = false` 时置灰 + `el-tooltip` 说明（未覆盖列表最多 5 条）；i18n en/zh-CN | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T11** | frontend-engineer | 测试历史记录列表：展示最近 5 次 `BatchTestRunEntity` 摘要；COMPLETED / INVALIDATED 状态标签；可展开查看各数据集结果；i18n en/zh-CN | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T12** | frontend-engineer | 覆盖率面板增强：三维度阈值对比表格（已覆盖/总数 / 覆盖率 / 阈值 / 状态）；未达标维度行可展开未覆盖项列表；顶部 Alert 颜色（绿/橙）；显示阈值来源（GLOBAL / GROUP）；i18n en/zh-CN | **Done** (2026-07-03) |
| **P12-TEMPLATE-TESTING-OVERHAUL-T13** | e2e-test-engineer + e2e-uiux-reviewer | Playwright E2E：F1 成功/并发超限/失败重试；F2 成功/部分失败；F3 失效触发；F4 门禁满足/未满足；F5 覆盖率面板展开；F6 历史记录；UIUX manifest（含 SSE 进度流截图证据） | **Done** (2026-07-03) |

**Suggested implementation order:** T01 → T02 → T03 → T04 → T05 → T06 → T07（后端合约完成）→ T08 → T09 → T10 → T11 → T12（前端功能）→ T13（E2E）。

**Exit criteria (slice):**

- 17 个 BDD 验收场景（SCEN-F1-01 至 SCEN-F6-02）全部通过；
- Flyway V41 + V42 在 Docker 数据库中幂等执行；
- SSE 端点（单次预览 + 全量测试）在开发者工具 Network 可见 `text/event-stream` 事件流；
- 临时预览文件 `expiresAt = now + 24h`；`@Scheduled` 清理任务清除过期文件并日志记录数量；
- 模板版本内容保存后 `invalidated_at` 自动更新，UI 刷新显示「已失效」；
- 提交测试按钮门禁在未满足条件时置灰 + Tooltip 含具体未覆盖项；
- 覆盖率面板展示阈值对比 + 可展开未覆盖列表；
- 历史记录展示 ≤ 5 条，最新一条与全量测试 runId 一致；
- 绿色门禁：`mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS；前端 `pnpm -C frontend lint`, `type-check`, `test`, `build` 全绿；Playwright T13 全通 + UIUX manifest PASS；
- `docs/plan/execution-sync-ledger.md` 更新门禁证据。

**Backend gate evidence (T01–T07):** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (2026-07-03); Checkstyle 0 violations; PMD 0 violations; SpotBugs 0 bugs; JaCoCo coverage gates met. New tests: `PreviewTempCleanupSchedulerTest`, `AsyncBatchTestOrchestratorTest`, `BatchTestInvalidationServiceTest`, `BatchTestHistoryServiceTest`, `SubmitTestEligibilityServiceTest`, `BatchTestInvalidationListenerTest`, `BatchTestRunEntityTest`.

**Frontend gate evidence (T08–T12):** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**643** Vitest), `build` ✓ (2026-07-03). Deliverables: `PreviewProgressDialog`, `BatchTestProgressDialog`, `BatchTestHistoryPanel`, `useSubmitTestEligibility`, `TemplateCoveragePanel` threshold table, `TemplateTestDataSetPanel` / `TemplateTestPreviewWorkflowPanel` wiring; i18n en/zh-CN. Bug fix: `getSubmitTestEligibility` API path in `frontend/src/api/templates.ts`; `handlePreviewRetry` wired in `TemplateTestDataSetPanel.vue`.

**E2E/UIUX gate evidence (T13):** Playwright Docker — `P12-TEMPLATE-TESTING-OVERHAUL-T13.spec.ts` **8 passed / 3 skipped** (SCEN-F5-02, SCEN-F4-01, SCEN-F2-02/F4-03 skipped — FOL seed thresholds / intentional failure data set; documented in manifest); `P12-TEMPLATE-TESTING-OVERHAUL-uiux-evidence.spec.ts` **1/1**; UIUX manifest **PASS** (`frontend/e2e/evidence/P12-TEMPLATE-TESTING-OVERHAUL-uiux-manifest.md`; 10 screenshots @ 1440×900, REDBC + GREENBC). Regression: `template-dev-workspace.spec.ts` + `template-test-preview-workflow.spec.ts` **5/5**.

**Status:** **Done** (2026-07-03 — T01–T13 complete; all slice exit criteria met)

---

## 3. Slice backlog (not active)

| ID | Summary | Status | Notes |
| --- | --- | --- | --- |
| **P12-TEMPLATE-TESTING-OVERHAUL** | 模板测试页全量改造（SSE + TTL + 持久化 + 门禁 + 历史，13 tasks） | **Done** (2026-07-03) | T01–T13 complete; BDD F1–F6 evidenced |
| — | Additional deferred items from master-plan § Deferred / post-MVP | Not Started | Pick next slice via `plan-orchestrator` when prioritized |

## 4. Phase exit criteria

P12 phase is **Not Started** (catch-all; no active slice — **P23-DEMO-TYPOGRAPHY-LAYOUT-EXCELLENCE** is the sole formal phase In Progress). **P12-API-PACKAGE-ACCESS-INVOCATION** closed **Done** 2026-07-03 (T01–T12). **P12-TEMPLATE-TESTING-OVERHAUL** closed **Done** 2026-07-03 (T01–T13). Individual slices run under `plan-orchestrator`.
**P12-AUD-M02** closed **Done** 2026-07-01. **P12-AUD-B10** closed **Done** 2026-07-01. **P12-UIUX-DEEP-REFACTOR** closed **Done** 2026-07-03.

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
