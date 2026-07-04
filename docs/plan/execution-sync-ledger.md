# Execution Sync Ledger

**Last synced:** 2026-07-04 (**SOR-F02 Done** — `cursor/sor-full-implementation-1385`; SOR-F02 Done; frontend Vitest **745**; see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md))
**Authoritative gate snapshot (2026-07-04):** backend Surefire **727** (1 skipped); frontend Vitest **745**; Playwright docker smoke tier **8** specs.
**Completion note (2026-07-04, SOR-F02):** **SOR-F02 Done** — decomposed `DashboardView.vue` (**835 → 211** lines) into tab panels (`DashboardOverviewTab`, `DashboardWorkflowTab`, `DashboardTasksTab`) and composables `useDashboardTabs` (107 lines, **10** Vitest tests), `useDashboardJourney` (387 lines, **8** tests), `useDashboardDataLoader` (235 lines, **7** tests). **Gate:** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**745**), `build` ✓. BDD `not-applicable` (refactor only). SOR-4 wave remains **In Progress** (F03/F04 Not Started).
**Completion note (2026-07-04, SOR-F01 slice 3):** **SOR-F01 Done** — extracted `useTemplateDetailNavigation.ts` (journey handlers/display, tab/route sync, load shell, dev-workspace helpers); **16** new Vitest tests; `useTemplateDetailController.ts` **859 → 408** lines. Composables: `useTemplateLifecycleActions` (698 lines, 15 tests), `useTemplatePolicyCredentials` (161 lines, 15 tests), `useTemplateDetailNavigation` (591 lines, 16 tests). **Gate:** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**720**), `build` ✓. BDD `not-applicable` (refactor only).
**Completion note (2026-07-04, SOR-F01 slice 2):** **SOR-F01 In Progress** — extracted `useTemplatePolicyCredentials.ts` (policy load, credentials table, secret dialog, create/rotate/revoke handlers); **15** new Vitest tests; `useTemplateDetailController.ts` **960 → 859** lines. **Gate:** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**704**), `build` ✓. BDD `not-applicable` (refactor only).
**Completion note (2026-07-04, SOR-F01 slice 1):** **SOR-F01 In Progress** — extracted `useTemplateLifecycleActions.ts` (submit/test/approval/publish/governance/delete + publish/submit gate state); **15** new Vitest tests; `useTemplateDetailController.ts` **1538 → 960** lines. **Gate:** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**689**), `build` ✓. BDD `not-applicable` (refactor only).
**Last synced (prior):** 2026-07-04 (**SOR P02 spool slice** — `cursor/sor-full-implementation-1385`; SOR-P02 Done; backend Surefire **727** (1 skipped); see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md))
**Authoritative gate snapshot (prior 2026-07-04):** backend Surefire **727** (1 skipped); frontend Vitest **674**; Playwright docker smoke tier **8** specs.
**Completion note (2026-07-04, SOR-P02):** **SOR-P02 Done** — `SpooledArtifact` + `ArtifactSpoolService`; `DocumentArtifactPipeline` spools finalized artifacts to temp files with file-length size guard; `DocumentGenerationEngine` uploads to MinIO via `InputStream` from spool and returns `artifactBytes` null; preview artifact paths updated. **Gate:** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (Surefire **727**, 1 skipped); Checkstyle/PMD/SpotBugs 0 violations.
**Last synced (prior):** 2026-07-04 (**SOR F06/F07 slice** — `cursor/sor-full-implementation-1385`; SOR-F07 Done; SOR-F06 partial; frontend Vitest **667**; see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md))
**Last synced (prior):** 2026-07-04 (**SOR P06/F06 slice** — `cursor/sor-full-implementation-1385`; SOR-P06 Done; SOR-F06 partial; frontend Vitest **664**; see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md))
**Last synced (prior):** 2026-07-04 (**SOR wave 3/7 slice** — `cursor/sor-full-implementation-1385`; SOR-P03/T02/T03 Done; SOR-P02 size-cap slice In Progress; backend `mvn verify` **723** Surefire; frontend Vitest **656**; docker Playwright subset **8** specs; see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md))
**Last synced (prior):** 2026-07-03 (**SOR full implementation wave** — `cursor/sor-full-implementation-1385`; backend `mvn verify` + frontend **654** Vitest; see [system-optimization-review-2026-07.md](./system-optimization-review-2026-07.md))
**Authoritative gate snapshot (2026-07-03):** backend Surefire **654+** (post-SOR); frontend Vitest **654**; Playwright smoke tier **5** specs in CI (`test:e2e:docker`).
**Completion note (2026-07-03, P12-API-PKG):** **P12-API-PACKAGE-ACCESS-INVOCATION slice Done** — package-first API access + invocation records. **Backend T01–T06:** Flyway V43–V45; materialize/gate; `InvocationRecordService`; retention scheduler; runtime + management invocation APIs. **Frontend T07–T09:** hub External access L1; `ApiPolicyHomeView` overview downgrade; `TemplateRecentInvocationsPanel`. **E2E T11:** `P12-API-PACKAGE-ACCESS.spec.ts` **6/6**, `P12-API-PACKAGE-ACCESS-RUNTIME.spec.ts` **1/1** (BDD S5 generate + caller query); S1–S3/S7 batch flat deferred. **UIUX T12:** `P12-API-PACKAGE-ACCESS-uiux-evidence.spec.ts` **3/3**; manifest `frontend/e2e/evidence/P12-API-PACKAGE-ACCESS-uiux-manifest.md` **PASS** (4 screenshots REDBC/GREENBC). **Gate:** Docker redeploy ✓; Playwright Docker **10/10**; prior `ManagementInvocationQueryServiceTest` ✓; frontend Vitest **646** ✓. **P12 catch-all → Not Started** (slice complete). **Active formal phase: P22** (**P22-T01 next**). Traceability: [P12-api-package-access-invocation-records.md](./detail/P12-api-package-access-invocation-records.md).
**Last synced (prior):** 2026-07-03 (**P22-DEMO-EXPANSION activated → In Progress** — BDD ready; P22-T01–T15 Not Started; **P22-T01 next**; plan-orchestrator only — no implementation code)
**Activation note (2026-07-03):** **P22-DEMO-EXPANSION activated → In Progress** (sole formal phase `In Progress`). **P12-API-PACKAGE-ACCESS-INVOCATION → Done** (2026-07-03 — full slice T01–T12; see completion note above). **P12 catch-all → Not Started**. BDD **ready** (`docs/requirements/demo-expansion-behavior-spec.md`). **P22-T01…T15 Not Started** — **P22-T01 next**. Traceability: [P22-demo-expansion-rendering-fidelity.md](./detail/P22-demo-expansion-rendering-fidelity.md).
**Last synced (prior):** 2026-07-03 (**P12-API-PACKAGE-ACCESS-INVOCATION activated → In Progress** — BDD ready; T01–T02 next; no implementation code yet)
**Last synced (prior):** 2026-07-03 (**P12-TEMPLATE-TESTING-OVERHAUL Done** — T01–T13 complete; backend `mvn verify` BUILD SUCCESS; frontend **643** Vitest; Playwright T13 **8+1** passed, **3** skipped documented; UIUX manifest **PASS**)
**Completion note (2026-07-03, T01–T13):** **P12-TEMPLATE-TESTING-OVERHAUL slice Done** — 模板测试页全量改造 (SSE preview + batch test progress, 24h temp TTL, batch persistence/invalidation, submit-test eligibility API, coverage panel, batch history). **Backend (T01–T07):** Flyway V41/V42; `AsyncPreviewOrchestrator`, `SseEmitterRegistry`, `PreviewConcurrencyGuard`, `PreviewTempCleanupScheduler`, `AsyncBatchTestOrchestrator`, `BatchTestInvalidationService` + `BatchTestInvalidationListener`, `BatchTestHistoryService`, `SubmitTestEligibilityService`. **Frontend (T08–T12):** `PreviewProgressDialog`, `BatchTestProgressDialog`, `BatchTestHistoryPanel`, `useSubmitTestEligibility`, `TemplateCoveragePanel` threshold table, testing tab wiring; bug fix `getSubmitTestEligibility` API path (`templates.ts`), `handlePreviewRetry` in `TemplateTestDataSetPanel.vue`. **E2E/UIUX (T13):** `P12-TEMPLATE-TESTING-OVERHAUL-T13.spec.ts` **8/11 passed**, **3 skipped** (FOL seed — SCEN-F5-02, SCEN-F4-01, SCEN-F2-02/F4-03 documented in manifest); `P12-TEMPLATE-TESTING-OVERHAUL-uiux-evidence.spec.ts` **1/1**; manifest **PASS** (`frontend/e2e/evidence/P12-TEMPLATE-TESTING-OVERHAUL-uiux-manifest.md`). Regression: `template-dev-workspace.spec.ts` + `template-test-preview-workflow.spec.ts` **5/5**. **Gate:** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS; `pnpm -C frontend lint`, `type-check`, `test` (**643**), `build` ✓. **P12 phase → Not Started** (catch-all idle; no active slice). **Queued:** **P12-API-PACKAGE-ACCESS-INVOCATION** (BDD ready). Detail: [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md) § P12-TEMPLATE-TESTING-OVERHAUL.
**Last synced (prior):** 2026-07-03 (**P12-TEMPLATE-TESTING-OVERHAUL T01–T07 backend Done** — `mvn verify` BUILD SUCCESS; PMD/Checkstyle/SpotBugs 0 violations; backend complete)
**Completion note (2026-07-03, T01–T07):** **P12-TEMPLATE-TESTING-OVERHAUL backend slice Done** — Flyway V41/V42 (preview TTL + batch test fields); `AsyncPreviewOrchestrator` + `SseEmitterRegistry` + `PreviewConcurrencyGuard` (AtomicInteger, up to 3 concurrent); `PreviewTempCleanupScheduler` (`@Scheduled` hourly, MinIO temp cleanup); `AsyncBatchTestOrchestrator` (SSE batch progress: `sample_started/sample_done/batch_completed/batch_failed`, hidden soft-delete history); `BatchTestInvalidationService` + `TemplateContentChangedEvent` + `BatchTestInvalidationListener` (content-change invalidation via Spring events); `BatchTestHistoryService` + `GET .../batch-tests?limit=5`; `SubmitTestEligibilityService` + `GET .../submit-test-eligibility`. New API endpoints: `POST .../previews/async-preview`, `GET .../previews/{previewId}/progress-stream`, `POST .../batch-tests/run`, `GET .../batch-tests/{runId}/progress-stream`, `GET .../batch-tests?limit=5`, `GET .../submit-test-eligibility`. New error codes: `PREVIEW_CONCURRENCY_LIMIT_EXCEEDED`, `PREVIEW_ARTIFACT_EXPIRED`, `BATCH_TEST_RUN_NOT_FOUND`. **Gate:** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (2026-07-03); Checkstyle ✓; PMD 0 violations; SpotBugs 0 bugs. New test classes: `PreviewTempCleanupSchedulerTest`, `AsyncBatchTestOrchestratorTest`, `BatchTestInvalidationServiceTest`, `BatchTestHistoryServiceTest`, `SubmitTestEligibilityServiceTest`, `BatchTestInvalidationListenerTest`, `BatchTestRunEntityTest`. **T08–T13 (frontend+E2E) Not Started — awaiting frontend-engineer**.
**Last synced (prior):** 2026-07-03 (**P12-API-PACKAGE-ACCESS-INVOCATION** — doc-only pass **complete**; BDD ready; **no code**)
**Doc-sync note (2026-07-03, pass 2):** Added **ADR-0040** (package-first API access, four-layer retention, invocation records); expanded `contract-outline.md` §调用记录查询; **OpenAPI v1** invocation list/detail paths + schemas; `business-terminology-guide.md` External access / Call history; PRD §804–805 7d semantics; behavior spec §14.3 C10–C15 confirmed; plan **T10 Done** (doc-only). **Queued** behind **P12-TEMPLATE-TESTING-OVERHAUL**. **Gate:** doc-only.
**Doc-sync note (2026-07-03, pass 1):** **P12-API-PACKAGE-ACCESS-INVOCATION** — Behavior spec **ready** (`docs/behavior/api-package-access-and-invocation-records.md`, BDD-API-PACKAGE-ACCESS-INVOCATION-001, decisions C1–C15); plan detail [P12-api-package-access-invocation-records.md](./detail/P12-api-package-access-invocation-records.md) (12 tasks T01–T12, **Not Started**); synced requirements §包级 API、PRD §12、domain §2.12、permission matrix §6–7、contract-outline invocations、catalog-navigation-ux hub IA. **Queued** behind active slice **P12-TEMPLATE-TESTING-OVERHAUL**. **Gate:** doc-only (no `mvn`/`pnpm` code gates).
**Last synced (prior):** 2026-07-03 (**P12-UIUX-DEEP-REFACTOR Done** — frontend UIUX deep refactor, 14 tasks, groups A–E; **606** Vitest)
**Completion note (2026-07-03):** **P12-UIUX-DEEP-REFACTOR Done** — Frontend UIUX deep refactor (14 tasks, groups A–E). Key deliverables: brand rename (REDBC zh "零售银行"→"红色银行", en "Retail Bank"→"Red Bank"); complete design token system in `global.scss` (gray scale, typography, shadows, spacing, dialog widths, semantic status colors, `--color-on-primary`); visual language upgrade (white header + brand top-line, professional sidebar with icons + collapse, login split-panel, status badge unification); new `PageHeader` component + `AppPageLayout` adoption across all views; unified `EmptyStatePanel`/`AppTablePagination` + `ContentModuleListView` v-model fix; journey/banner/identity/audit cleanup + shared `CredentialsPanel`; `TemplateDetailView` split to `useTemplateDetailController` composable + view, legacy tabs → `WorkspaceTabShell`, new `LifecycleCommentDialog`/`TemplateWorkspaceHeader`/`ReleaseSectionTable` components, dead `TemplateDevVersionActionBar.vue` deleted; shell polish (user dropdown, sidebar icons/collapse, `document.title` routing, breadcrumb dedup). **BDD:** `not-applicable` (no API/permission/backend change). **Gate:** `pnpm -C frontend lint` ✓, `type-check` ✓, `test` ✓ (**606** Vitest, 606/606), `build` ✓. Backend unchanged. **OPT-G3 resolved** (TemplateDetailView split). **P12 catch-all → Not Started** (catch-all idle; no active slice). Detail: [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md) § P12-UIUX-DEEP-REFACTOR.
**Last synced (prior):** 2026-07-02 (**Dev workspace tab shell** `8d36cd3` + M9-T02 Step 2 `1aea0a2`; **pushed** to `origin/main`)
**Progress note (2026-07-02):** **Workspace tab shell extended** — master revision detail + content module detail adopt `WorkspaceTabShell` (design/approval and versions/content/lifecycle tabs); journey blocks read-only with workspace deep links. **Gate:** frontend type-check green; targeted Vitest master/content-module workspace tests green. **Prior:** template dev editor tab shell (`8d36cd3`).
**Progress note (prior 2026-07-02):** Ad-hoc WIP **A** — `pdf-page-number-stamping-enabled` (default `false`); `PdfConversionPostProcessor` + `DocxPdfConversionPreprocessor`; FOL master layout v4. **WIP B** — `GET /content-modules` optional `groupCode` + `listAccessible`; list UI drops group picker, adds table group column. **Gate:** backend `mvn verify` BUILD SUCCESS (**614** Surefire, 1 skipped); frontend lint/type-check/test/build green (**571** Vitest). **Prior:** 2026-06-29 **P12-BDD-RISK-PROMPT-UX-001 Done**.
**Activation note (2026-07-02):** **P12-BDD-RISK-PROMPT-UX-001 activated → In Progress** (BDD
**BDD-TEMPLATE-RISK-PROMPT-UX-001** READY; supersedes P19-T08 group override + P19-T10 list-view
panel + hardcoded decision categories). **No formal phase `In Progress`** — P19/P21/P12 catch-all
phase rows unchanged (**Done** / **Not Started**). **Single active slice:** P12-BDD-RISK-PROMPT-UX-001
only. Traceability: [P12 detail](./detail/P12-deferred-enhancements.md) § P12-BDD-RISK-PROMPT-UX-001;
[P19 detail](./detail/P19-verifiability-publish-gate.md) § Residual; **COR-T15**.
**Last synced (prior):** 2026-07-01 (**P3-T06 BDD S6 complete** — 8/8 scenarios, `template-package-nav` 9/9)
**Completion note (2026-07-01):** **P3-T06 BDD S6 cross-group isolation** — Flyway `V34__corp_only_template_author_seed.sql` adds CORP-only template author `10000008`; E2E `assertCrossGroupVersionLineAccessDenied` covers version-lines / dev / release / clone `403 ACCESS_DENIED`. **BDD-TEMPLATE-PACKAGE-NAV-001 all 8 scenarios covered** in Playwright. `template-package-nav.spec.ts` **9/9** Docker. Detail: [detail/P3-template-authoring.md](./detail/P3-template-authoring.md).
**Last synced (prior):** 2026-07-01 (**P3-T06 BDD S5/S7/S8** — boundary E2E + cloneable UI)
**Completion note (2026-07-01):** **P3-T06 BDD boundary scenarios** — `TemplateVersionLinesPanel` respects API `cloneable`; E2E helper `template-version-lines-api.ts`; Playwright **S5** immutable `403`, **S7** clone blocked (`409` + hidden button), **S8** global admin clone. `template-package-nav.spec.ts` **8/8** Docker. Detail: [detail/P3-template-authoring.md](./detail/P3-template-authoring.md).
**Last synced (prior):** 2026-07-01 (**P3-T06 OpenAPI + release/clone E2E** — BDD S3/S4 complete)
**Completion note (2026-07-01):** **P3-T06 follow-up (OpenAPI + release journeys)** — added management paths to `openapi-v1.yaml` (`version-lines`, `version-lines/{id}`, `dev/{devVersionId}`, `releases/{releaseVersion}`, `release-versions/{releaseVersion}/clone`) + schemas; `contract-outline.md` dev/release rows; `catalog-navigation-ux.md` BDD marked **Done**. Playwright `template-package-nav.spec.ts` **5/5** including published read-only detail (S3) and hub clone → dev editor (S4). Detail: [detail/P3-template-authoring.md](./detail/P3-template-authoring.md).
**Last synced (prior):** 2026-07-01 (**P3-T06 E2E remediation** — version-line `devVersionId` fix + Playwright **11/11** Docker)
**Completion note (2026-07-01):** **P3-T06 E2E follow-up** — fixed frontend/API mismatch (`TemplateVersionLineSummary.devVersionId` vs stale `id`); clone response mapping; hub secondary tab E2E uses **Workflow status** label + redirect-to-dev-editor behavior; FOL catalog E2E adapted to hub model + 834-variable table filters. **Gate:** frontend `pnpm build` green; Playwright Docker (`E2E_TARGET=docker`) **11/11** — `template-package-nav.spec.ts` **3/3**, `P21-T06a-template-detail-tabs.spec.ts` **3/3**, `fol-corporate-catalog.spec.ts` **5/5**. Detail: [detail/P3-template-authoring.md](./detail/P3-template-authoring.md) P3-T06 row.
**Last synced (prior):** 2026-07-01 (**OPT-D5-slice-3 activated → In Progress** — OPT-D5 god-service split slice 3)
**Activation note (2026-07-01):** **OPT-D5-slice-3 activated → In Progress** (extract
`TemplateBindingConfigurationService` from `TemplateService` — variable schema CRUD, anchor
bindings, composition rules, binding validation + private helpers; `TemplateService` delegates).
**OPT-D5** remains **In Progress** (Wave 3); slices 1–2 Done. **No formal phase `In Progress`**
— active formal phase stays **None** (optimization backlog slice, not a P-phase). BDD
**`not-applicable`** (refactor only). **No gate evidence yet** for slice 3. Traceability:
[optimization-plan.md](./optimization-plan.md) OPT-D5 row.
**Last synced (prior):** 2026-07-01 (**P2-T06 Done** — Phase B full master revision history)
**Completion note (2026-07-01):** **P2-T06 Done** — Phase B full master revision history:
each file replace persisted as revision row; full paginated history on hub; revision detail
deep-links to any historical line; download historical DOCX. **Backend:** Flyway
`V32__master_revision_line.sql`, `MasterRevisionLineEntity`/`MasterRevisionLineAnchorEntity`,
`MasterRevisionLineService` (append on create/replace, list/get/download all lines),
`MasterDocumentService` (`revisionSequence`, live status for current line); architecture review
M1/M2 remediated. **Frontend:** multi-row `MasterRevisionLinesPanel`, historical read-only
`MasterRevisionDetailView`, `masterRevisionLineLabel.ts` i18n en+zh-CN. **Gate:** backend
`mvn verify` BUILD SUCCESS (**564** Surefire); frontend lint/type-check/test/build green
(**528** Vitest); Playwright `master-revision-two-page.spec.ts` **7/7**; UIUX evidence
`P2-T06-uiux-evidence.spec.ts` **6** screenshots @ 1440×900. **P2 phase remains Done** — do
not reopen. **No active slice.** Traceability: `docs/product/catalog-navigation-ux.md` § Master
revision history — phased delivery **Phase B Done**. Detail:
[detail/P2-master-management.md](./detail/P2-master-management.md).
**Activation note (2026-07-01):** **P2-T06 activated → In Progress** (Phase B full master revision
history — each file replace persisted as revision row; full paginated history on hub; revision
detail deep-links to any historical line; download historical DOCX). **P2 phase remains Done** — do
**not** reopen P2 as formal `In Progress` phase (P2-T06 sub-slice pattern, analogous to
P12-AUD-B10 catch-all activation). User constraint — single active slice only. **No gate evidence
yet** for P2-T06. Traceability: `docs/product/catalog-navigation-ux.md` § Master revision history
— phased delivery **Phase B**. Detail: [detail/P2-master-management.md](./detail/P2-master-management.md).
**Completion note (2026-07-01):** **P12-AUD-M02 Done** — role constants single-source refactor
(AUD-M02 remediation): `MANAGEMENT_ROLES` derived from `MANAGEMENT_ROLE_VALUES` SSOT in
`types/identity.ts`; bare role string literals replaced in `auth/roles.ts`, `auth/identityRoles.ts`,
`auth/contentModuleRoles.ts`, `navigation/navStructure.ts`, `composables/useWorkflowTasks.ts`,
journey utilities, `DashboardView.vue`; `roles.test.ts`, `identityRoles.test.ts` updated.
**Gate:** frontend `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**521** Vitest,
92 files); BDD `not-applicable` (refactor only). **Audit:** AUD-M02 **resolved** (🟢) in
P21 §11.4. **P12 phase → Not Started** (catch-all idle; no active slice). **P21 remains Done** — do not reopen.
Detail: [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md).
**Completion note (2026-07-01):** **P12-AUD-B10 Done** — submit-for-approval evidence checklist gate
(AUD-B10 remediation): `PublishGatePhase.SUBMIT_FOR_APPROVAL`, `PublishGateService.assertReadyForSubmitForApproval`,
`TemplateLifecycleService` PENDING_SUBMIT guard; `TemplateSubmitForApprovalSummaryDialog`, `submitGateReady` SSOT,
lifecycle tab + author journey CTA; i18n en/zh + `api.error.template.submitForApprovalGateBlocked`.
**Gate:** backend `mvn verify` BUILD SUCCESS (**558** Surefire); frontend lint/type-check/test/build green
(**520+** Vitest); Playwright `P12-AUD-B10-submit-approval-gate.spec.ts` **3/3**; UIUX evidence **6** screenshots +
manifest **PASS** (`frontend/e2e/evidence/P12-AUD-B10-uiux-manifest.md`). **Audit:** AUD-B10 **resolved** (🟢) in
P21 §11.3. **P12 phase → Not Started** (catch-all idle; no active slice). **P21 remains Done** — do not reopen.
Detail: [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md).
**Activation note (2026-06-30):** **P12 activated → In Progress** (single-active-phase selection
by `plan-orchestrator`; user constraint — do **not** reopen P21). Active slice **P12-AUD-B10**
(AUD-B10 remediation — submit-for-approval evidence checklist gate): server-side gate on
`submitForApproval` mirroring `PublishGateService`; frontend pre-submit summary dialog; BDD + TDD +
E2E. **P21 remains Done** — confirm-on-behalf UI delivered P21-T09b; checklist gap tracked here.
**No gate evidence yet** for P12-AUD-B10. Detail:
[detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md). *(Superseded by completion note 2026-07-01 above.)*
**Completion note (2026-06-30):** **P21 Done** — role-journey frontend redesign & business-friendly terminology phase closed. **P21-X02 Done** — governance & docs wrap-up: companion docs aligned to **Done** (`permission-matrix` §13.1.2, `catalog-navigation-ux`, `business-terminology-guide`, ADR implementation note); plan layer closed (master-plan, detail §7 evidence map, ledger, index READMEs). **Final gates cited:** backend `mvn verify` **553** (X04); frontend **511** Vitest (X01). **Known gap:** AUD-B10 **partial** → remediated under **P12-AUD-B10** (non-blocking for P21 close). Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-X02 Done** — governance & docs wrap-up: `permission-matrix.md` §13.1.2 implementation **Done** + T02/T07 closed-loop footnote; `catalog-navigation-ux.md` Hybrid IA Implementation **Done**; `business-terminology-guide.md` P21 complete header; ADR `2026-06-29-behavior-typed-ia-business-terminology.md` implementation note footer (decisions unchanged); P21 detail §7 evidence map + phase closure sections. **Gate:** docs-only — cites backend **553**, frontend **511** Vitest. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-X01 Done** — full-system L1 terminology sweep (AUD-Q05): business-friendly L1 copy across `templates.*`, `audit.*`, `contentModules.*`, `packageCatalog.*`, and nav routes; IT terms restricted to L2/L3 surfaces. **Deliverables:** `i18n/locales/en.ts`, `i18n/locales/zh-CN.ts`; `docs/product/business-terminology-guide.md` (P21-X01 mapping table); Vitest (`TemplateStatusBadge.test.ts`, `TemplatePublishSummaryDialog.test.ts`, `TemplateDetailLifecycleTab.test.ts`, `navStructure.test.ts`). **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**511** Vitest). Backend unchanged. **Audit:** AUD-Q05 **resolved**. P21 phase **wrap-up** — next **P21-X02** only per §11.5. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-X06 Done** — i18n parity hardening (AUD-Q04): ~368 zh-CN leaf keys added for primary journeys (`contentModules.*`, `templates.lifecycle/governance/authoring/rules/create/error`, `paste.*`); `collectLeafKeys` utility + en→zh-CN key-parity regression test in `localeRegistry.test.ts` blocks silent English fallback. **Deliverables:** `i18n/collectLeafKeys.ts`, `i18n/localeRegistry.test.ts`, `i18n/locales/zh-CN.ts`. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green. Backend unchanged. **Audit:** AUD-Q04 **resolved**. P21 phase **wrap-up** — next **P21-X01** per §11.5. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-X04 Done** — backend capability + route + contract completeness: `ManagementRoute.CONTENT_MODULE_MANAGEMENT` registered in `RouteVisibilityService` + permission-matrix §13.1; six new `ManagementCapabilitiesView` fields (`exportTemplates`, `viewCollaborationWorkItems`, `maintainCollaborationTimeoutConfig`, `authorContentModules`, `decideContentModuleReviews`, `manageContentModuleLifecycle`); `GET /api/management/v1/collaboration-work-items` in OpenAPI v1; frontend capability alignment. **Deliverables:** `ManagementRoute.java`, `RouteVisibilityService.java`, `ManagementCapabilitiesService.java`, `ManagementCapabilitiesView.java` (+ tests); `openapi-v1.yaml`, `permission-matrix.md`; frontend `auth/roles.ts`, `types/session.ts`, `useWorkflowTasks.ts`, `DashboardView.vue`, `TemplateDetailView.vue` (+ tests). **Gate:** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (**553** Surefire); `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**504** Vitest). **Audit:** AUD-P02 backend registration **resolved**; AUD-P05 backend `exportTemplates` **resolved**; AUD-P06 **resolved**; AUD-C05 **resolved**. P21 phase **wrap-up** — next **P21-X01/X05/X06** per §11.5. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-X03 Done** — cross-cutting P0 permission fail-closed: strict `resolveCapability` (missing capability → deny); unified route guard via single `canAccessRoute` reading `visibleRoutes` only; `canExportTemplates` aligned to `authorTemplates`; `canUploadMasters` fix; `showMetadataEdit` admin-only (GLOBAL/GROUP). **Deliverables:** `auth/roles.ts` (+ `roles.test.ts`), `router/index.ts`, `stores/session.ts`, `composables/useCapabilities.ts`, `TemplateDetailView.vue` (+ test). **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**503** Vitest). Backend unchanged. **Audit:** AUD-P01/P03/P04 + AUD-B04 **resolved** (frontend); AUD-P02/P05 backend portions → **P21-X04 Done**. P21 phase **wrap-up** — next **P21-X01/X04/X05/X06** per §11.5. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T11 Done** — cluster ④ Audit: 5-step audit-admin workflow (`auditAdminJourney.ts`, `auditAdminJourneySteps` in `roleJourneyDefinitions.ts`); `AuditConsoleView.vue` journey timeline + view-only banner + business column headers; `auditEventLabels.ts` business-readable event type labels; i18n en+zh-CN (`audit.*`, `journey.roles.AUDIT_ADMIN.*`); Vitest (`auditAdminJourney.test.ts`, `auditEventLabels.test.ts`, `AuditConsoleView.test.ts`, `roleJourneyDefinitions.test.ts`, `navStructure.test.ts`); Playwright `P21-T11-audit-journey.spec.ts` **3/3**. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**498** Vitest); Playwright **3/3**. Backend unchanged. **Sub-phase D cluster ④ complete** (T11); **all four role clusters (①→④) complete**. P21 phase **wrap-up** — next cross-cutting **P21-X03** (permission fail-closed, P0) or **P21-X01/X05/X06** per §11.5. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T10 Done** — cluster ③ Global admin: 6-step bank-wide administration workflow (`globalAdminJourney.ts`, `globalAdminJourneySteps` in `roleJourneyDefinitions.ts`); DashboardView journey gating fix (`GLOBAL_ADMIN` no longer falls to team-lead); identity/entitlement L1 copy sweep (`en.ts` + `zh-CN.ts`); Vitest (`globalAdminJourney.test.ts`, `roleJourneyDefinitions.test.ts`, `DashboardView.test.ts`, `navStructure.test.ts`); Playwright `P21-T10-global-admin-journey.spec.ts` **4/4**. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**478** Vitest); Playwright **4/4**. Backend unchanged. **Sub-phase C cluster ③ complete** (T10). Next **P21-T11** (now Done). Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T09b Done** — reminder timing L1 copy + overdue-reminder queue (T01a retained) + "confirm on behalf" exception copy; GLOBAL_ADMIN + GROUP_ADMIN see confirm-on-behalf in `TemplateLifecycleDecisionDialog`; `en.ts` + `zh-CN.ts`; Vitest (`TemplateLifecycleDecisionDialog.test.ts`, `CollaborationTimeoutConfigPanel.test.ts`, `navStructure.test.ts`); Playwright `P21-T09b-reminder-exception-l1.spec.ts` + `collaboration-todos.spec.ts` **7/7**. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**457** Vitest); Playwright **7/7**. Backend unchanged. **Audit:** AUD-B10 **partial** (confirm-on-behalf UI fixed; evidence checklist gate still open). **Sub-phase B cluster ② complete** (T07–T09b). Next **P21-T10**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T09a Done** — API management / access-keys L1 copy: `en.ts` + `zh-CN.ts` value sweep (`apiPolicy.*`, `templates.policy.*`, `templates.error.loadPolicy`, publish gate `API_POLICY`, `api.apimgmt.policyImpact.*`, dashboard quick link); Vitest grep audit in `navStructure.test.ts`; Playwright `P21-T09a-api-management-l1.spec.ts` **2/2**. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**452** Vitest); Playwright P21-T09a **2/2**. Backend unchanged. Next **P21-T09b**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T09 Done** — cluster ② B2 Team-lead journey: `templateTeamLeadJourney.ts` (4 steps), `TemplateTeamLeadJourneyBlock.vue`, Dashboard `#journey-section` + `TemplateDetailView` embed (author journey suppressed on `PENDING_RELEASE` for publish-capable users); PENDING_REVIEW master priority + PENDING_RELEASE work items; i18n en+zh-CN. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green (**451** Vitest); Playwright P21-T09 functional **4/4** + UIUX **1/1**; UIUX manifest **PASS**. Backend unchanged. Next **P21-T09a** (now Done). Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T08 Done** — cluster ② B1 Approver journey: `templateApproverJourney.ts` (3 steps), `TemplateApproverJourneyBlock.vue`, Dashboard `#journey-section` + `TemplateDetailView` embed; AUD-B03 dual-substate UI (`TemplateStatusBadge`, `templateWorkflowBannerContext`, `TemplateListView`, `TemplateSummaryView.approvalSubState` + `TemplateViewMapper.toSummary`); i18n en+zh-CN. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green; Playwright P21-T08 functional + UIUX + P21-T01b updated; UIUX manifest **PASS**; `TemplateViewMapperTest` **8/8** BUILD SUCCESS. Next **P21-T09** (now Done). Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T07 Done** — AUD-A01/A02 approval/publish path: `submitForApproval` → OPEN APPROVAL; `recordApprovalDecision` → resolve APPROVAL + upsert REMEDIATION (reject) or PENDING_RELEASE (approve); `publish` → resolve PENDING_RELEASE; +3 i18n keys. **Gate:** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS. Next **P21-T08** (now Done). Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T06b Done** — AUD-B06/B07: publish gate error+retry+bindingGateResult display; policy error/empty; name/AD truncate; semver picker wrap. **Gate:** pnpm lint/type-check/test/build green (**414**); Playwright **1/1**. Next **P21-T07**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T06a Done** — AUD-B01/B02/B08: `normalizeTemplateDetailQuery` clears `focus=lifecycle`; stale-template skeleton guard; default tab `overview` + DOM order aligned. **Gate:** pnpm lint/type-check/test/build green (**404**); Playwright **2/2**. Next **P21-T06b**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T06 Done** — OPT-G3: split `TemplateDetailView` into 5 tab components under `views/templates/detail/`; business L1 tab labels; `templateWorkflowBannerContext.ts` SSOT (AUD-B09 resolved). **Gate:** pnpm lint/type-check/test/build green (**398**). Next **P21-T06a**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T05 Done** — template tester journey: `templateTesterJourney.ts`, `TemplateTesterJourneyBlock`, dashboard + detail wiring, AUD-B05 test-fail remediation fields. **Gate:** pnpm lint/type-check/test/build green (**385**); Playwright **4/4** + UIUX **1/1** PASS; AUD-B05 **resolved**. Cluster ① journeys complete (T03–T05). Next **P21-T06**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T04 Done** — template author journey: `templateAuthorJourney.ts` mappers, `TemplateAuthorJourneyBlock`, timeline on `TemplateDetailView` + dashboard, Spec C remediation copy, team-lead go-live guidance. **Gate:** pnpm lint/type-check/test/build green (**365**); Playwright **4/4** + 1 skipped + UIUX **1/1** PASS. Next **P21-T05**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-30):** **P21-T04 activated → In Progress** (A3 Orchestrator journey — create → design content → trial generate → submit test → submit approval; "waiting on my fixes" entry; PENDING_RELEASE shows "awaiting team-lead go-live"; template views, lifecycle panel). P21 phase remains In Progress; **P21-T03 Done** (2026-06-30). **No gate evidence yet** for P21-T04 (no implementation, no frontend lint/type-check/test/build for this slice); all other P21 tasks unchanged except T04 status. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T03 Done** — master designer journey: `masterDesignerJourney.ts` mappers, timeline on hub/revision/dashboard, Spec C i18n, master-rework fix. **Gate:** pnpm lint/type-check/test/build green (336+); Playwright **5/5** + UIUX PASS. Next **P21-T04**. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-30):** **P21-T03 activated → In Progress** (A2 Master designer journey — upload → layout placeholders → submit review → rework timeline + "master review / master to fix" behavior entries with business titles; `TemplateDetailView.vue` split, masters views, `RoleJourneyTimeline`). P21 phase remains In Progress; **sub-phase A Done** (T01/T01a/T01b/T01c/T02). **No gate evidence yet** for P21-T03 (no implementation, no frontend lint/type-check/test/build for this slice); all other P21 tasks unchanged except T03 status. Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T01c Done** — removed `RoleHomeView.vue` (+test); stripped
workbench logical keys from `routeKeys.ts`/`auth/roles.ts` (legacy redirects retained in router +
`LEGACY_ROUTE_PATH_REDIRECT`); renamed `canAccessCollaborationEscalationWorkbench` →
`canViewEscalationQueue`; removed orphan `workbench.*` / `home.*` i18n + `template-author-draft`
task kind. **Gate:** `pnpm -C frontend lint`, `type-check`, `test`, `build` green; Playwright
`collaboration-todos.spec.ts` **4/4** (legacy workbench redirect regression). **Audit:** AUD-D01..D03
**resolved**. P21 phase remains In Progress; **sub-phase A Done**; active slice **P21-T03 In Progress** (Master designer journey).
Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-30):** **P21-T01c activated → In Progress** (dead-code cleanup — remove
`RoleHomeView.vue` (+test); remove residual workbench logical keys in `routeKeys.ts`, `auth/roles.ts`;
addresses AUD-D01..D03). P21 phase remains In Progress; **P21-T01**, **P21-T01a**, **P21-T01b**,
**P21-T02** stay Done. **No gate evidence yet** for P21-T01c (no implementation, no frontend
lint/type-check/test/build for this slice); all other P21 tasks unchanged. Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-30):** **P21-T01b Done** — `RoleJourneyTimeline` reusable stepper +
cluster-① journey definitions (MASTER_DESIGNER/TEMPLATE_AUTHOR/TEMPLATE_TESTER); Dashboard
`#journey-section` reference integration (onboarding guidance, above `#tasks-section`). **Gate:**
`pnpm -C frontend lint`, `type-check`, `test`, `build` green; Playwright
`P21-T01b-journey-timeline.spec.ts` **3/3**, `P21-T01b-uiux-evidence.spec.ts` **1/1** (dev :5173 +
backend :8080); UIUX manifest `frontend/e2e/evidence/P21-T01b-uiux-manifest.md` — **PASS** (0 🔴).
P21 phase remains In Progress; active slice **P21-T01c In Progress** (dead-code cleanup).
Detail: [detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-30):** **P21-T01b activated → In Progress** (RoleJourneyTimeline reusable
stepper — business-language steps, empty/guidance states; `frontend/src/components/**`). P21 phase
remains In Progress; **P21-T01**, **P21-T01a**, **P21-T02** stay Done (2026-06-29). **No gate
evidence yet** for P21-T01b (no implementation, no frontend lint/type-check/test/build for this
slice); all other P21 tasks unchanged. Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-29):** **P21-T01a Done** — task hub deepening. Queue-partitioned task hub
(`TaskHubPartitionSection.vue`, `useWorkflowTasks.buildTaskPartitions`); URL-driven landing titles;
`fetchWorkItems({ queue })`; restored columns (trigger/summary/age/submitter/Open); overdue badges;
segmented collaboration load/error; ESCALATION → `template-escalation` kind. **Gate:** `pnpm -C
frontend lint`, `type-check`, `test`, `build` green (**280+** Vitest); Playwright
`P21-T01a-task-hub.spec.ts` **5/5**, `P21-T01a-uiux-evidence.spec.ts` **1/1**, regression **11/11**
(dev :5173 + backend :8080); UIUX manifest `frontend/e2e/evidence/P21-T01a-uiux-manifest.md` —
**PASS** (0 🔴). **Audit:** AUD-H01..H07 **resolved**. P21 phase remains In Progress; active
slice **P21-T01b In Progress** (2026-06-30 — RoleJourneyTimeline). Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-29):** **P21-T01a activated → In Progress** (task hub deepening — queue
partitioning + restore `triggerType/summaryText/ageSeconds` + SLA/overdue badges + inline open actions;
addresses AUD-H01..H07). P21 phase remains In Progress; **P21-T01** stays Done (2026-06-29); **P21-T02**
stays Done (2026-06-29). **No gate evidence yet** for P21-T01a (no implementation, no frontend
lint/type-check/test/build for this slice); all other P21 tasks unchanged. Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Completion note (2026-06-29):** **P21-T01 Done** — A0 foundation + terminology baseline.
Behavior-typed **My to-dos / 我的待办** nav group (`navStructure.ts`, `ManagementShell.vue`) with
six capability/queue-driven entries + deep-link URL contract (`?queue=` / `?filter=master-review`
+ `#tasks-section`); L1 value rewrites for nav/dashboard/task-hub/breadcrumb per §12.2 Spec B
(`en.ts`, `zh-CN.ts` — keys stable). **Gate:** `pnpm -C frontend lint`, `type-check`, `test`,
`build` green (**267+** Vitest); Playwright `P21-T01-behavior-nav.spec.ts` **7/7**,
`P21-T01-uiux-evidence.spec.ts` **1/1**, `a11y-smoke` + `collaboration-todos` **9/9** (dev :5173 +
backend :8080); UIUX manifest `frontend/e2e/evidence/P21-T01-uiux-manifest.md` — **PASS** (0 🔴).
**Audit:** AUD-Q05 in-scope L1 resolved (nav/dashboard/tasks); residual IT jargon on
`templates.*` / `apiPolicy.*` / `audit.*` → **P21-X01**. P21 phase remains In Progress; next
recommended slice **P21-T01a Not Started** (task hub deepening). Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-29):** **P21-T01 activated → In Progress** (A0 foundation + terminology
baseline — behavior-typed "My to-dos" nav group, L1 business copy for nav/dashboard/tasks/breadcrumb).
P21 phase remains In Progress; **P21-T02** stays Done (2026-06-29). **No gate evidence yet** for
P21-T01 (no implementation, no frontend lint/type-check/test/build for this slice); all other P21
tasks unchanged. Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**P21-T02 completion note (2026-06-29):** **P21-T02 Done** — A1 backend collaboration work-item
closed loop. `recordTestDecision(PASSED|FAILED)` resolves OPEN TEST work items (`RESOLVED` +
`resolvedAt`) in the same transaction; FAILED→DRAFT upserts one OPEN REMEDIATION
(`triggerType=TEST_FAILURE_OR_RETURN_TO_DRAFT`, orchestrator-targeted, dedup/idempotent);
`submitForTest` accepts DRAFT or APPROVAL+PENDING_SUBMIT (PENDING_DECISION fail-closed);
dedicated non-sensitive create/resolve audit added; `ApprovalSubStateResolver` extracted as the
`approvalSubState` single source of truth (refactor); +1 English i18n key
`api.collaboration.workItem.remediation.summary`. **Gate:** `mvn -B -ntp -f backend/pom.xml verify`
→ BUILD SUCCESS (Surefire all pass, JaCoCo met, Checkstyle/PMD/SpotBugs 0). **Audit findings:**
AUD-A01 resolved for the TEST path (APPROVAL/PUBLISH `RESOLVED` → P21-T07); AUD-A02 partially
resolved (REMEDIATION emitted; `SUBMIT_FOR_APPROVAL`/`APPROVAL_FAILURE`/`APPROVAL_PENDING_RELEASE`
→ P21-T07). Backend-only slice — no frontend/E2E/UIUX gate required. P21 remains In Progress
(phase not complete). Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Activation note (2026-06-29):** (P21 activation — phase In Progress, first slice P21-T02)
**Activation note (2026-06-29):** **P21 activated → In Progress** (single-active-phase selection
by `plan-orchestrator`; user approved start of P21). No other formal phase is In Progress — all
P0–P11, P13–P20 remain Done; P12 deferred catch-all stays non-active. First slice **P21-T02**
(A1 backend collaboration work-item closed loop) set **In Progress**: behavior spec §12.1 is
`ready` → entering backend-engineer TDD. **No gate evidence yet** (no implementation, no `mvn
verify` for this slice); all other P21 tasks remain Not Started. Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Registration note (2026-06-29):** **P21 registered — Not Started** (role-journey frontend
redesign & business-friendly terminology; hybrid IA, 4 role clusters ①→②→③→④, foreign-bank
non-IT persona; ADR Batch B / COR-T11 not violated). No gate evidence yet — this is a planning
container awaiting activation (single-active-phase selection). Detail:
[detail/P21-role-journey-frontend-redesign.md](./detail/P21-role-journey-frontend-redesign.md).
**Audit backlog folded in (2026-06-29):** code-grounded read-only audit (4 lenses) appended to
P21 §11 as findings AUD-A/P/H/B/Q/D with `file:line` evidence + severity, mapped to tasks
P21-T06a/T06b and cross-cutting P21-X03..X06 (permission single-source/fail-closed, backend
capability+route+OpenAPI completeness, UI/a11y fixes, i18n parity). Companion docs created:
`business-terminology-guide.md`, ADR `2026-06-29-behavior-typed-ia-business-terminology.md`;
`catalog-navigation-ux.md` + `permission-matrix.md` §13.1.2 updated.
**Orchestration note (2026-06-28):** **P18 Done** — **P18-T01–T10 Done**.
**Purpose:** Cross-reference plan phases (P0–P11), epics (E01–E12), and milestones (M1–M14) after re-earning Done status with real code and green gates.

## Authority

| Layer | Source of truth | Scope |
| --- | --- | --- |
| Phase delivery | [master-plan.md](./master-plan.md) + [detail/](./detail/) | Exit criteria, task status |
| Epic ordering | [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) | Delivery focus |
| Technical waves | [implementation-task-plan.md](../architecture/implementation-task-plan.md) | OpenAPI wave mapping |
| This ledger | Mirror + evidence pointers | Epic/milestone ↔ phase mapping |

On conflict between this ledger and a stale task-sheet row, **plan layer wins** until the task sheet is refreshed.

## Quality gate evidence (authoritative row)

| Gate | Command | Result | Notes |
| --- | --- | --- | --- |
| Frontend (latest full gates) | `pnpm -C frontend lint`, `type-check`, `test`, `build` | Green — **528** Vitest tests (2026-07-01) | P2-T06 Phase B full master revision history |
| Backend (latest full verify) | `mvn -B -ntp -f backend/pom.xml verify` | Green — BUILD SUCCESS (**564** Surefire, 2026-07-01) | P2-T06 Phase B — V32 migration, revision-line entity + service |
| Frontend lint | `pnpm -C frontend lint` | Green | |
| Frontend type-check | `pnpm -C frontend type-check` | Green | |
| Frontend test | `pnpm -C frontend test` / `vitest run` | Green | **235 tests**, 2026-06-27 (P14-T02 E2E remediation — `roles.ts` workbench guards) |
| Frontend build | `pnpm -C frontend build` | Green | |
| E2E Docker (4173) | `pnpm -C frontend test:e2e:docker` | Green — **7 tests**, 2026-06-25 | post P19 Wave 1 + catalog + master-replace spec fixes |
| Docker manual smoke (4173) | `scripts/docker-deploy.ps1` + author UI | Green — 2026-06-25 | P19-T02/T03: batch test button + coverage panel on template authoring tab |
| E2E master revision two-page | `frontend/e2e/master-revision-two-page.spec.ts` (Docker 4173) | Green — **7/7**, 2026-07-01 | P2-T06 Phase B multi-row hub, historical deep-link, download |
| E2E P2-T06 UIUX (4173) | `pnpm exec playwright test e2e/P2-T06-uiux-evidence.spec.ts --config playwright.docker.config.ts` | Green — **6** screenshots @ 1440×900, 2026-07-01 | P2-T06 hub + historical revision detail — REDBC/GREENBC + zh-CN; evidence `frontend/e2e/evidence/P2-T06/screenshots/` |
| E2E master revision two-page (Phase A) | `frontend/e2e/master-revision-two-page.spec.ts` (Docker 4173) | Green — **4/4**, 2026-06-25 | P2-T05 Phase A hub + revision detail journeys (superseded count by Phase B 7/7) |
| E2E P14-T01 UIUX (4173) | `pnpm exec playwright test e2e/P14-T01-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | Green — **3/3**, 2026-06-26 | P14-T01 content module UI; manifest `frontend/e2e/evidence/P14-T01-uiux-manifest.md` (8 screenshots); e2e-uiux-reviewer **PASS** |
| E2E P14-T02 collaboration (4173) | `pnpm exec playwright test e2e/collaboration-todos.spec.ts --config playwright.docker.config.ts` | Green — **3/3**, 2026-06-27 | P14-T02 collaboration to-do journeys (tester queue, overdue escalation, admin config); report `playwright-report/docker/index.html`; backend remediation (`BatchTestRunEntity` FK, `CollaborationWorkItemWriter`); frontend remediation (`roles.ts` workbench route guards) |
| E2E P14-T02 UIUX (4173) | `pnpm exec playwright test e2e/P14-T02-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | Green — **5/5**, 2026-06-27 | P14-T02 Dashboard collaboration tasks + timeout config UI (COR-T11); manifest `frontend/e2e/evidence/P14-T02-uiux-manifest.md` (8 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** |
| E2E P14-T03 export/import (4173) | `pnpm exec playwright test e2e/P14-T03-template-export-import.spec.ts --config playwright.docker.config.ts` | Green — **2/2**, 2026-06-27 | P14-T03 template export JSON/ZIP + staging import → DRAFT; no secrets in bundle; report `playwright-report/docker/index.html` |
| E2E P14-T03 UIUX (4173) | `pnpm exec playwright test e2e/P14-T03-uiux-evidence.spec.ts e2e/P14-T03-template-export-import.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` | Green — **7/7**, 2026-06-27 | P14-T03 template export/import surfaces — `TemplateExportActions`, `TemplateImportDialog`; manifest `frontend/e2e/evidence/P14-T03-uiux-manifest.md` (9 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** (no critical blockers; 5 suggestions) |
| E2E P18-T10 structured authoring (4173) | `pnpm exec playwright test e2e/P18-T10-structured-authoring.spec.ts --config playwright.docker.config.ts` | Green — **5/5**, 2026-06-29 | P18-T10 controlled editor toolbar + disabled reasons; insert paragraph/variable + save binding; paste-clean summary cancel/accept; test-generate fidelity warnings + `filter-warning-code`; clean binding no warnings (no `CONTROLLED_STYLE_FALLBACK` stub); helper `frontend/e2e/helpers/structured-authoring-api.ts`; report `playwright-report/docker/index.html`; requires redeployed Docker stack (4173 + 8080) with P18-T10 UI/API |
| E2E P21-T01 behavior nav + UIUX (dev 5173) | `pnpm exec playwright test e2e/P21-T01-behavior-nav.spec.ts e2e/P21-T01-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts e2e/collaboration-todos.spec.ts --workers=1` | Green — **17/17**, 2026-06-29 | P21-T01 behavior-typed My to-dos nav + L1 terminology; manifest `frontend/e2e/evidence/P21-T01-uiux-manifest.md` (8 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** (0 🔴) |
| Helm validate (P15-T02/T03/T04/T05/T06/T07/T08) | `.\scripts\helm-validate.ps1 -SkipKubeconform` | Green — 2026-06-27 | helm lint **0 failed**; `helm template` default/dev/staging/prod; T03 ConfigMap/Secret assertions (`Assert-T03ConfigSecrets`); T04 Ingress/TLS assertions (`Assert-T04IngressTls`); T05 HPA assertions (`Assert-T05Hpa` — autoscaling/v2, CPU+memory, custom Pods metric); T06 NetworkPolicy assertions (`Assert-T06NetworkPolicy` — default-deny, allow ingress/backend-egress/DNS/metrics); T07 probe assertions (`Assert-T07Probes`); T08 blue-green assertions (`Assert-T08BlueGreen` — dual color Deployments, activeColor selector, preview Services, HPA target); fail-closed missing-secret test; render-only (no cluster deploy); kubeconform step skipped offline |
| K8s manifest CI gates (P15-T09) | `.\scripts\ci-k8s-manifest-gates.ps1` | Green (local `-SkipKubeconform`) — 2026-06-27 | [`.github/workflows/k8s-manifest-gates.yml`](../.github/workflows/k8s-manifest-gates.yml) — PR + push to `main`, path filters `deploy/**` + gate scripts; helm lint **0 failed** + template default/dev/staging/prod + T03–T08 assertions + blocking kubeconform on CI; [`deploy/ci-k8s-gates.md`](../deploy/ci-k8s-gates.md); local full kubeconform blocked by Docker Hub timeout — CI runner expected green |
| Helm validate (offline) | `.\scripts\helm-validate.ps1 -SkipKubeconform` | Green — 2026-06-27 | Same lint/template/assertions as CI minus kubeconform; use when registry unavailable |

**Test count progression (not conflicting runs):** P13 slice verify **114** backend tests (2026-06-23);
Wave C UX **161** backend / **88** frontend; post OPT-E8/F3 full verify **189** backend;
COR-B02/F05 Batch B slice **193** backend / **104** frontend (2026-06-24);
COR-1/COR-3 sprint **198** backend / **106** frontend (2026-06-24);
COR-B04/B05 + COR-F02/F09 **200** backend / **108** frontend (2026-06-24);
COR-B07/B08 + COR-F08/F15 **201** backend / **112** frontend (2026-06-24);
COR-B09 + COR-F17 + prior uncommitted slice **205** backend / **114** frontend (2026-06-24);
COR-B11/B12 + COR-T01 binding gate + COR-E02 E2E **206** backend / **114** frontend (2026-06-24);
COR-E03/E04 + master file replace **222** backend / **114** frontend (2026-06-24);
COR-F12/E05/E06 template create validation + dashboard/tab/messageKey tests **121** frontend (2026-06-23);
COR-F11/F13 workflow banner CTA + governance 2-step confirm + publish messageKey tests **125** frontend (2026-06-23);
COR-F14/F16 LoadErrorPanel + audit filter validation + COR-T05 publish candidate **223** backend / **129** frontend (2026-06-23);
COR-T06/F21/E06 multi-version callability + table a11y baseline + audit messageKey **227** backend / **131** frontend (2026-06-23);
COR final batch (catalog package UX + publish gate apiPolicy + workflow filters + i18n/a11y polish) **238** backend / **139** frontend (2026-06-24);
COR-P04 + zh-CN api.error catalog + E06/F22 **238** backend / **144** frontend (2026-06-25);
P19-T07 + P20 primary zh-CN + Docker E2E **243** backend / **149** frontend (2026-06-25);
P19-T01 test data set governance **248** backend / **149** frontend (2026-06-25).
P19-T02 batch test generation **252** backend / **149** frontend (2026-06-25).
P19-T03 coverage computation + thresholds **256** backend / **149** frontend (2026-06-25).
P19 Wave 1 verifiability (T04–T10) **281** backend / **169** frontend / E2E **7/7** (2026-06-25).
P17-T01 policyVersion lineage **284** backend (2026-06-25).
P17-T02 per-domain save endpoints **305** backend (2026-06-25).
P14-T01a content module domain + Flyway + JPA **330** backend (2026-06-26).
P14-T01b content module management REST + governance **456** backend verify (2026-06-26; 61 contentmodule tests: T01a 7 + T01b 60).
P14-T01c template content-module reference + lifecycle impact + DocxAssembler pinned resolve **461** backend (2026-06-26).
P14-T01d content module management UI + P14-T01 vertical slice close **224** frontend (2026-06-26).
P14-T01 architecture critical remediation — catalog browse fail-closed, structure readback, shared-module list, real impact + audit persistence **469** backend (+8 from 461, 2026-06-26).
P14-T01e regression tests (retry delegation) **471** backend (+2 from 469, 2026-06-26).
P14-T02a collaboration work item entity + role-queue query API **471** backend; `CollaborationWorkItem*Test` **18**; Flyway `V28__collaboration_work_item.sql`; `GET /api/management/v1/collaboration-work-items` (2026-06-26).
P14-T02b collaboration timeout config API **471** backend; Flyway `V29__collaboration_timeout_config.sql`; GET/PUT `/api/management/v1/collaboration-timeout-config`; `CollaborationTimeoutResolver`; OpenAPI v1 extended; **11** T02b tests (2026-06-26).
P14-T02c collaboration escalation scheduler — Flyway `V30__collaboration_work_item_escalation_source.sql`; `CollaborationEscalationService` / `CollaborationEscalationScheduler`; notification-only (no source state mutation); **11** T02c tests; full verify **475** BUILD SUCCESS (2026-06-27; environment fix `TEMP`→`D:\temp`, no code change).
P14-T02d collaboration UI — collaboration work items on `DashboardView` tasks section (COR-T11); `CollaborationTimeoutConfigPanel` on dashboard; `collaboration` store/API; **232** frontend tests (+8 from 224, 2026-06-27).
P14-T02 collaboration vertical slice close — T02a–T02d Done; backend verify **475**; frontend lint/type-check/test/build green (2026-06-27).
P14-T02 E2E remediation — `collaboration-todos.spec.ts` **3/3** Docker 4173; backend verify **481** (+6 from 475; `BatchTestRunEntity` FK, `CollaborationWorkItemWriter`); frontend **235** (+3 from 232; `roles.ts` workbench guards) (2026-06-27).
P14-T02 UIUX evidence — `P14-T02-uiux-evidence.spec.ts` + `a11y-smoke.spec.ts` **5/5** Docker 4173; manifest `frontend/e2e/evidence/P14-T02-uiux-manifest.md` (8 screenshots); e2e-uiux-reviewer **PASS** (2026-06-27).
P14-T03a template export service + management endpoint — `TemplateExportService` + GET export JSON/ZIP; `TemplateExport*Test` **15**; OpenAPI v1 + `contract-outline.md` export contract (2026-06-27).
P14-T03b template import service + validation + DRAFT landing — `POST /templates/import`; `TemplateImport*Test` **13**; OpenAPI v1 import contract (2026-06-27).
P14-T03c template export/import UI — `TemplateExportActions`, `TemplateImportDialog`; **+14** Vitest; frontend **235+** (2026-06-27).
P14-T03 vertical slice close — T03a–T03c Done; E2E `P14-T03-template-export-import.spec.ts` **2/2** Docker 4173; backend verify **481**; frontend lint/type-check/test/build green (2026-06-27).
P14-T03 UIUX evidence — `P14-T03-uiux-evidence.spec.ts` + `P14-T03-template-export-import.spec.ts` + `a11y-smoke.spec.ts` **7/7** Docker 4173; manifest `frontend/e2e/evidence/P14-T03-uiux-manifest.md` (9 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** (2026-06-27).
P14 phase close — T01–T03 Done; UX Wave D Done; **P15 Done** (2026-06-27; T01–T10).
P15-T01a backend container hardening — `backend/Dockerfile.packaged` alpine JRE UID **65532**; read-only smoke `/healthz` **200**; [`deploy/container-hardening.md`](../deploy/container-hardening.md) writable paths; `scripts/container-hardening-smoke.ps1` PASSED (2026-06-27).
P15-T01b frontend NGINX hardening — `frontend/Dockerfile.packaged` nginx UID **101** port **8080**; read-only smoke `/healthz` + `/` **200**; [`deploy/container-hardening.md`](../deploy/container-hardening.md) frontend writable paths; `scripts/container-hardening-smoke.ps1` PASSED (backend + frontend) (2026-06-27).
P15-T01c container hardening documentation + smoke evidence — [`deploy/container-hardening.md`](../deploy/container-hardening.md) writable paths (backend `/tmp`, frontend `/tmp/nginx/*`); P15-T01 smoke evidence section (ReadonlyRootfs, uid **101**, `/healthz` + SPA **200**); `scripts/container-hardening-smoke.ps1` PASSED (backend + frontend); **P15-T01 vertical slice closed** (2026-06-27).
P15-T02 Helm chart scaffold — `deploy/helm/docgen/` (backend + frontend Deployments/Services; per-env values); pod/container `securityContext` (non-root, read-only root FS, drop ALL caps); CPU/memory requests+limits per ADR-0030; [`deploy/helm/docgen/README.md`](../deploy/helm/docgen/README.md); `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — helm lint **0 failed**, template default/dev/staging/prod, fail-closed secret check; render-only (2026-06-27).
P15-T03 ConfigMap/Secret + external service wiring — [`deploy/k8s-config-secrets.md`](../deploy/k8s-config-secrets.md); `templates/configmap.yaml` + per-env `externalServices`/`config`; `secrets.create: false` in all env values; `Assert-T03ConfigSecrets` + fail-closed missing-secret test in `scripts/helm-validate.ps1`; no `StatefulSet` in rendered manifests; no plaintext secrets in repo; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03 assertions; render-only (2026-06-27).
P15-T04 Service + Ingress + cert-manager TLS + K8s DNS — [`deploy/k8s-ingress-tls.md`](../deploy/k8s-ingress-tls.md); `templates/*-service.yaml` ClusterIP port **8080** + `docgen.io/cluster-dns` annotations; `templates/ingress.yaml` NGINX class `/api`→backend `/`→frontend; `templates/certificate.yaml` cert-manager `Certificate` + TLS **1.2+**; `Assert-T04IngressTls` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04 assertions; render-only (2026-06-27).
P15-T05 HPA CPU/memory + custom metric — [`deploy/k8s-hpa-autoscaling.md`](../deploy/k8s-hpa-autoscaling.md); `templates/backend-hpa.yaml` + `templates/frontend-hpa.yaml` — `autoscaling/v2`, CPU + memory Resource metrics, min/max bounds, blue-green `scaleTargetRef`; backend Pods custom metric `docgen_http_requests_per_second` gated by `customMetric.enabled`; `values-staging.yaml` explicit customMetric; `Assert-T05Hpa` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05 assertions; render-only (2026-06-27).
P15-T06 default-deny NetworkPolicy + explicit allow — [`deploy/k8s-network-policy.md`](../deploy/k8s-network-policy.md); `templates/networkpolicy.yaml` — `podSelector: {}` default-deny (Ingress + Egress); allow policies — ingress controller, frontend→backend :8080, backend→external egress (TCP **5432/6379/9092/443**), DNS, metrics scrape gated by `networkPolicy.monitoring.enabled`; `Assert-T06NetworkPolicy` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06 assertions; render-only (2026-06-27).
P15-T07 health probes `/healthz` + `/readyz` — [`deploy/k8s-health-probes.md`](../deploy/k8s-health-probes.md); backend Deployments liveness `/healthz` readiness `/readyz` port **8080**; frontend NGINX `/healthz` + `/readyz`; `ReadinessProbe` Postgres check; `Assert-T07Probes` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06+T07 assertions; render-only (2026-06-27).
P15-T08 blue-green release — [`deploy/blue-green-runbook.md`](../deploy/blue-green-runbook.md); `templates/backend-color-deployments.yaml` + `frontend-color-deployments.yaml` — dual color Deployments; main Services route to `blueGreen.activeColor`; preview Services target inactive color; HPA `scaleTargetRef` on active color; `values-prod.yaml` — `blueGreen.requireManualApproval: true`; manual rollback steps (no Deployment/PVC/data destroy); `Assert-T08BlueGreen` in `scripts/helm-validate.ps1`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06+T07+T08 assertions; render-only (2026-06-27).
P15-T09 CI manifest validation gates — [`.github/workflows/k8s-manifest-gates.yml`](../.github/workflows/k8s-manifest-gates.yml); local `.\scripts\ci-k8s-manifest-gates.ps1 -SkipKubeconform` PASSED (2026-06-27).
P15-T10 deployment docs + phase close — [`deploy/README.md`](../deploy/README.md); architecture cross-links; **P15 phase Done** (2026-06-27).
P18-T01 structured content v1 node matrix — `com.bank.docgen.authoring.structured` (`StructuredContentNodeType`, `StructuredContentSchemaValidator`); JSON schema `authoring/structured-content-v1.schema.json`; `StructuredContentSchemaTest` **3/3**; wired into `TemplateService.validateStructuredContent` (2026-06-27).

P18-T02 node-matrix fidelity validation — `NodeMatrixValidationService`, `StructuredContentValidationResult`, `StructuredContentFidelityIssue`; `NodeMatrixValidationServiceTest` **4/4**; blockers map to `BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE` → publish gate anchor integrity (2026-06-27).

P18-T03 master style catalog + direct format — `MasterStyleCatalogService`, `default-master-style-catalog-v1.json`; `MasterStyleCatalogServiceTest` **4/4**; style/direct-format blockers wired into `TemplateService.computeBindingStatus` (2026-06-27).

P18-T04 structured table component — `TableComponentService`, `TableComponentRenderModel`; `TableComponentServiceTest` **3/3**; inline table component validation wired into binding status (2026-06-27).

P18-T05 reference nodes — `ReferenceNodeService`, `AttachmentListReferenceModel`; `ReferenceNodeServiceTest` **3/3**; seal placement/scaling blockers + image scaling warning; wired into `TemplateService` (2026-06-27).

P18-T06 controlled numbering — `NumberingService`, deterministic loop re-sequencing; `NumberingServiceTest` **3/3**; duplicate/cross-ref blockers wired into binding status (2026-06-27).

P18-T07 Word/HTML paste cleaning — `PasteCleaningService`, `PasteCleaningSummary`, `PasteCleaningResult`; `PasteCleaningServiceTest` **4/4**; edit-time transform with transformed/removed/warning/blocked summary + `cancelToPrePaste`; i18n `paste.summary.*` (2026-06-28).

P18-T08 publish-locked render profile — `RenderProfileService`, `RenderProfile`, `CallerRenderOverride`, Flyway **V31**; `RenderProfileServiceTest` **3/3**; wired into publish (`TemplateLifecycleService`), preview (`PreviewGenerationService` + `PreviewRecordView.renderProfileVersion`), runtime (`DocumentGenerationEngine` ignores caller override); default `authoring/default-render-profile-v1.json` (`rp-v1`) (2026-06-28).

P18-T09 fidelity warning surfacing — `FidelityValidationService` aggregates T02–T06 warnings across anchor bindings; wired into `PreviewGenerationService`, `DocumentGenerationEngine`, `RuntimeGenerationService` (incl. idempotency replay recompute); removed hardcoded `CONTROLLED_STYLE_FALLBACK` stub; `FidelityValidationServiceTest` **3/3**; `TemplatePlatformSliceTest` `preview_emitsRealWarnings_fromValidationEngine`, `runtimeSuccess_includesFidelityWarnings`, `noHardcodedWarning_whenContentClean` (2026-06-28).

P18-T10 controlled authoring UI — `ControlledStructuredContentEditor`, `PasteCleaningSummaryDialog`, `FidelityWarningList`; management APIs `GET /api/management/v1/templates/{templateId}/master-style-catalog`, `POST .../paste-clean`; extended `FidelityWarningView` (`location`, `artifact`, `viewed`); wired into `TemplateAuthoringPanel` + `TemplatePreviewPanel`; Vitest structuredContentNodes/pasteSummary/fidelityFilters tests (2026-06-28); Playwright functional E2E **5/5** + UIUX evidence **1/1** (12 screenshots, manifest PASS, Docker 4173, 2026-06-29).
OPT-D5 slice 1 — `TemplateViewMapper` extracted from `TemplateService`; backend verify **521** (+7 from 514; `TemplateViewMapperTest` 7/7); `TemplateService` 651→547 L (2026-06-24).
OPT-D5 slice 2 — `TemplateStructuredAuthoringService` extracted (`getMasterStyleCatalog`, `pasteClean` + view mappers); backend verify **524** (+3 from 521; `TemplateStructuredAuthoringServiceTest` 3/3); `TemplateService` 547→495 L (2026-06-29).
P21-T02 A1 backend collaboration work-item closed loop — `CollaborationWorkItemWriter.resolveOpenTestWorkItems` + `upsertRemediationWorkItem`; `TemplateLifecycleService.recordTestDecision` resolves OPEN TEST + emits REMEDIATION on FAILED→DRAFT; `submitForTest` accepts DRAFT or APPROVAL+PENDING_SUBMIT (PENDING_DECISION fail-closed); `ApprovalSubStateResolver` extracted as `approvalSubState` SSOT; non-sensitive create/resolve audit (`ManagementAuditRecorder.recordCollaborationWorkItem*`); +1 i18n key `api.collaboration.workItem.remediation.summary`; `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (Surefire all pass, JaCoCo met, Checkstyle/PMD/SpotBugs 0); AUD-A01 (TEST path) resolved, AUD-A02 partial — approval-path closure → P21-T07 (2026-06-29).
P21-T01 A0 behavior nav + L1 copy round 1 — `navStructure.ts` behavior-typed `myTodos` group + six queue-driven entries; `ManagementShell.vue` nav active-state + deep links; L1 rewrites `en.ts`/`zh-CN.ts` (nav/dashboard/tasks/breadcrumb); `navStructure.test.ts`, `ManagementShell.test.ts`; frontend lint/type-check/test/build green (**267+** Vitest); Playwright `P21-T01-behavior-nav.spec.ts` **7/7**, `P21-T01-uiux-evidence.spec.ts` **1/1**, `a11y-smoke` + `collaboration-todos` **9/9** (dev :5173 + backend :8080); manifest `frontend/e2e/evidence/P21-T01-uiux-manifest.md` PASS; AUD-Q05 in-scope L1 resolved — remainder → P21-X01 (2026-06-29).
P21-X01 full-system L1 terminology sweep — `en.ts`/`zh-CN.ts` business copy across `templates.*`/`audit.*`/`contentModules.*`/`packageCatalog.*`/nav routes; `business-terminology-guide.md` P21-X01 mapping table; Vitest **511** (`navStructure.test.ts`, `TemplateStatusBadge.test.ts`, `TemplatePublishSummaryDialog.test.ts`, `TemplateDetailLifecycleTab.test.ts`); frontend lint/type-check/test/build green; AUD-Q05 **resolved** (2026-06-30).
P2-T06 Phase B full master revision history — Flyway V32 `master_revision_line` + anchor snapshot backfill; `MasterRevisionLineService` append/list/get/download; hub multi-row UI + historical read-only detail; E2E **7/7** + UIUX **6** screenshots; backend verify **564**; frontend **528** Vitest (2026-07-01).
Use the latest full-verify row above for gate claims; milestone blocks below are point-in-time snapshots.

## Phase status (plan layer)

| Phase | Status | Evidence summary |
| --- | --- | --- |
| P0 | Done | Skeleton, compose, CI gates, contract harness |
| P1 | Done | Local auth, session, role landing, guards |
| P2 | Done | Master APIs + list UI + **Phase A revision nav** (`MasterPackageHubView`, `MasterRevisionLineController`, V22); P2-T05 Done — hub/revision two-page, E2E 4/4; **P2-T06 Done** — Phase B full revision history (V32, multi-row hub, historical download, E2E 7/7) |
| P3 | Done | Template wizard, variables, rules, test data sets |
| P4 | Done | DOCX/PDF render, preview records, comparison panel |
| P5 | Done | Lifecycle + publish gate UI (**thin slice** — state machine + checklist UI; live gates → P19) |
| P6 | Done | API policy, credentials, caller contract view |
| P7 | Done | Runtime sync/batch/async, idempotency, encryption |
| P8 | Done | Audit console, masked export |
| P9 | Done | Observability, local security gates, prod compose profile |
| P10 | Done | Secure download with expiry |
| P11 | Done | Batch + async task lifecycle |
| P12 | Not Started (2026-07-03) | Catch-all idle — last slice **P12-API-PACKAGE-ACCESS-INVOCATION Done** (T01–T12); **P22** sole formal phase `In Progress`; see [P12 detail](./detail/P12-deferred-enhancements.md) |
| P13 | Done | Identity & group administration (user + group management plane); green gates 2026-06-23 — see P13 mirror block |
| P14 | Done (2026-06-27) | Confirmed large domains — **P14-T01 Done** (T01a–T01e + architecture remediation); **P14-T02 Done** (T02a–T02d + E2E **3/3** + UIUX **5/5**); **P14-T03 Done** (T03a–T03c + E2E **2/2** + UIUX **7/7**); backend **481**; frontend **235+**; OpenAPI export/import contract; architecture re-review **PASS** (2026-06-26); see [P14 detail](./detail/P14-confirmed-large-domains.md) |
| P15 | Done (2026-06-27) | Kubernetes deployment & container hardening — T01–T10; [`deploy/README.md`](../deploy/README.md); helm validate + CI gates green — see [P15 detail](./detail/P15-kubernetes-deployment-container-hardening.md) |
| P16 | Done (2026-06-23) | Template lifecycle governance + logical delete (T01–T08); gates: `mvn verify` 161 tests |
| P17 | Done (2026-06-25; Wave 3) | T01–T09 + COR-F18 Done; backend **308** tests; frontend **189** tests; `ApiPolicyDetailView` domain UI + impact preview |
| P20 | Done (2026-06-25) | T01–T07 complete incl. T06 (`api.error` en/zh + primary journey zh-CN); gates green — see [P20 detail](./detail/P20-i18n-ui-upgradeability.md) |
| P18 | Done (2026-06-28) | **P18-T01–T10 Done** — controlled authoring UI + management paste/catalog APIs — see [P18 detail](./detail/P18-structured-authoring-fidelity-engine.md) |
| P19 | Done (2026-06-25) | T01–T10 complete — change-diff, preview comparison, live publish gate, decision forms, risk prompts, exception markers, verifiability UI; gates 281 backend / 169 frontend / E2E 7/7 — see [P19 detail](./detail/P19-verifiability-publish-gate.md) |
| P21 | Done (2026-06-30) | Role-journey frontend redesign — T01–T11 + X01–X06 + **X02** governance close; backend **`mvn verify` 553**; frontend **511** Vitest; Playwright/UIUX per sub-phase; **AUD-B10 resolved** (P12-AUD-B10 Done 2026-07-01); **AUD-M02 resolved** (P12-AUD-M02 Done 2026-07-01) — see [P21 detail](./detail/P21-role-journey-frontend-redesign.md) |

**Active phase:** **P22** (2026-07-03 — **P22-DEMO-EXPANSION** sole formal phase `In Progress`; coordination note [P22-coordination-note-2026-07-03.md](./P22-coordination-note-2026-07-03.md)).
**Active slice:** **P22-T01…T15** (P22 session owns status updates — SOR-D03).
**P21 Done** — do not reopen. All formal phases P0–P11, P13–P21 **Done**; **P12 catch-all Not Started**.

## Epic ↔ phase mapping

| Epic | Status | Maps to | Evidence |
| --- | --- | --- | --- |
| E01 | Done | P2, P3, P4 | `MasterDocumentService`, `MasterRevisionLineController`, `MasterPackageHubView`, `TemplateService`, `PreviewGenerationService`, frontend template/master views |
| E02 | Done | P5 | `TemplateLifecycleService`, lifecycle UI in `TemplateDetailView` |
| E03 | Done | P6 | `ApiManagementService`, `ApiPolicyHomeView`, `TemplateCallerContractPanel` |
| E04 | Done | P8 | `AuditQueryService`, `AuditConsoleView` |
| E05 | Done (thin slice) | P0, P7, P9 | Flyway + JPA, Redis idempotency, MinIO, Kafka async; `ConfigAdGroupResolver` stub |
| E06 | Done | P1, P5, P6, P8 | `ManagementShell`, dual-brand tokens, role homes |
| E07 | Done (local gates) | P9 | `scripts/` release gate; external deploy evidence pending |
| E11 | Done | P1 | `routeKeys.ts`, `RoleHomeView`, `ForbiddenView`, router guards |
| E12 | Done | P1, E11 | `useCapabilities`, `DashboardView` (`/dashboard`, `route.dashboard-home`), workflow tasks + collaboration work items on dashboard; legacy workbench routes redirect to `/dashboard#tasks-section` (COR-T11 **Done**, 2026-06-24); see [e12-phase3-task-sheet.md](../architecture/e12-phase3-task-sheet.md) |
| E13 | Done | P13 | Identity & group administration (local account store as authorization authority, ADR 0036); `BusinessGroupService`, `UserManagementService`, `GroupManagementController` (`/api/management/v1/groups`), `UserManagementController` (`/api/management/v1/users`), fail-closed escalation guards, `IdentityAdministrationView`; green gates 2026-06-23 — see P13 mirror block |

### E05 / E07 outstanding (not blocking MVP Done)

| Item | Status | Document |
| --- | --- | --- |
| E05-T06 External validation evidence | Not Started (target env only; local Docker **Pass** 2026-07-02) | [e05-external-validation-evidence.md](../architecture/e05-external-validation-evidence.md) |
| E06 role-journey release evidence | Re-earned via P21 (EV-01–04); EV-05 packaging optional | [e06-role-journey-release-evidence.md](../architecture/e06-role-journey-release-evidence.md) |
| M9-T02 Intranet SCA submission | In Progress — SBOM + runbook + bundle script **Done**; org upload pending | [m9-t02-closure-plan.md](../architecture/m9-t02-closure-plan.md), [intranet-sca-submission-runbook.md](../evidence/security/intranet-sca-submission-runbook.md) |
| M10–M11 Deferred security closure | Not Started | m10/m11 task sheets |

## Milestone ↔ wave mapping

| Milestone | Wave | Status | Phase / evidence |
| --- | --- | --- | --- |
| M1 | 0–1 | Done | P0, P7 contract endpoints |
| M2 | 2 | Done | P7 sync generate |
| M3 | 3 | Done | P11 batch/async |
| M4 | 4 | Done | P10 download |
| M5 | 5 | Done | P6 API management |
| M6 | 6 | Done | P5 lifecycle |
| M7 | 7 | Done | P7 integration tests (`TemplatePlatformSliceTest`) |
| M8 | 8 | Done (local) | P9 Checkstyle/PMD/SpotBugs in verify |
| M9 | 9 | In Progress | SBOM script **Done** (`generate-sbom.ps1`); intranet SCA submission pending |
| M10 | 10 | Not Started | Deferred security closure |
| M11 | 11 | Not Started | Intranet security baseline |
| M12 | 12 | Done | Runtime endpoint adapters |
| M13 | 13 | Done | Runtime HTTP transport + auth filter |
| M14 | 14 | Done | Kafka/in-process async batch transport |

## P13 Identity & group administration (mirror + evidence)

| Item | Status | Evidence (2026-06-23) |
| --- | --- | --- |
| Epic E13 (identity & group administration) | Done | `GroupDimension`, `BusinessGroupEntity`/`BusinessGroupRepository`, mutable `ManagementUserEntity` + extended repository, `BusinessGroupService`, `UserManagementService`, `GroupManagementController` (`/api/management/v1/groups`), `UserManagementController` (`/api/management/v1/users`); migrations `V14__management_role_seeds.sql` + `V15__business_group_management.sql`; frontend **`UserManagementView`** / **`GroupManagementPanel`** at **`/entitlement/users`** and **`/entitlement/groups`** (`route.identity-administration`); legacy `/home/identity` redirects to `/entitlement/users` |
| Behavior + escalation guard | Done | Fail-closed `GROUP_SCOPE_OUT_OF_RANGE` / `ROLE_ASSIGNMENT_NOT_ALLOWED` / `USER_DELETE_NOT_ALLOWED` / `GROUP_MANAGEMENT_NOT_ALLOWED` (+ management-only `NOT_FOUND`/`CONFLICT`) wired via `GlobalExceptionHandler`/`ApiErrorCodes`; `ManagementRoute.IDENTITY_ADMINISTRATION` exposed to GLOBAL_ADMIN/GROUP_ADMIN by `RouteVisibilityService`; audit recorded; covered by `BusinessGroupServiceTest` (8), `UserManagementServiceTest` (12), `IdentityGroupAdministrationSliceTest` (17), `RouteVisibilityServiceTest` (7), `ManagementCapabilitiesServiceTest` (6) |
| Backend gate | Green | P13 slice: `mvn verify` — **114** tests (point-in-time snapshot, 2026-06-23); latest full verify **189** — see authoritative gate row |
| Frontend lint | Green | `pnpm -C frontend lint` — 0 errors |
| Frontend type-check | Green | `pnpm -C frontend type-check` — pass |
| Frontend test | Green | `pnpm -C frontend test` — 24 files / 81 tests passed (43 identity-related all green) |
| Frontend build | Green | `pnpm -C frontend build` — success |
| Post-task doc sync | Done | This sync (2026-06-23): master-plan / plan README / P13 detail / this ledger / PROJECT-STATUS-RESET / docs README updated |

Detailed task rows: [detail/P13-identity-group-administration.md](./detail/P13-identity-group-administration.md).

## UX optimization waves (mirror)

Source: [ux-upgradeability-optimization-plan.md](./ux-upgradeability-optimization-plan.md).

| Wave | Scope | Status | Evidence |
| --- | --- | --- | --- |
| UX Wave A | UX-A + UX-B (role gating, half-built interactions) | Done | Backend: `ManagementCapabilitiesService`, `RouteVisibilityService`, `V14` seeds; Frontend: `useCapabilities`, `roles.ts`, `TemplateCreateDialog`, `apiPolicy` rotate/revoke, `http.ts` 401/403, `useConfirmAction`, authoring persist (UXB5) |
| UX Wave B | UX-C + UX-D + UX-E (lifecycle, workbenches, polish) | Done (2026-06-23) | UXC3–UXC5, UXC4 metadata edit, UXD4 governance dashboard, UXE2–UXE4 polish; gates: `mvn verify`, `pnpm lint/type-check/test/build` |
| UX Wave C | UX-F (upgradeability foundations) | Done (2026-06-23) | UXF1/4/5 + P16-T08 delete + P17 impact-preview seam; backend 161 tests, frontend 88 tests |
| UX Wave D | UX-G → P14 confirmed large domains | Done (2026-06-27) | P14 **Done** — T01 content modules + T02 collaboration + T03 export/import; E2E T01 **3/3**, T02 **3/3** + UIUX **5/5**, T03 **2/2** + UIUX **7/7**; backend **481**, frontend **235+**; arch re-review **PASS** (2026-06-26) |

**Backend gate evidence (UX Wave A/B, 2026-06-23):** `mvn -B -ntp -f backend/pom.xml verify "-Dspring-boot.repackage.skip=true"` — 134 tests green (1 skipped); includes `TemplateLifecycleGovernanceServiceTest`, `TemplatePlatformSliceTest#stopRestoreAndDeprecatePublishedTemplate`.

**Frontend gate evidence (UX Wave A/B, 2026-06-23):** `pnpm -C frontend lint` / `type-check` / `test` (81) / `build` — all green.

## P2 master revision navigation UX (mirror — 2026-06-25)

| Item | Status | Evidence |
| --- | --- | --- |
| P2-T05 Behavior spec traceability | Done | BDD-MASTER-REVISION-NAV-001 → `docs/product/catalog-navigation-ux.md` |
| P2-T05 Phase A hub + revision detail UI | Done | `MasterPackageHubView`, `MasterRevisionDetailView`, `MasterRevisionLinesPanel`; routes `/masters/:masterId`, `/masters/:masterId/revisions/:revisionLineId`; unit tests green |
| P2-T05 Phase A revision-lines API | Done | `MasterRevisionLineController`, `MasterRevisionLineService`, `MasterRevisionLineControllerTest`; Flyway `V22__master_current_revision_line_id.sql`; honest single-line pagination |
| P2-T05 E2E + UIUX | Done | `frontend/e2e/master-revision-two-page.spec.ts` **4/4**; UIUX review pass (minor findings) |
| P2-T06 Phase B full revision history | Done (2026-07-01) | Flyway V32, `MasterRevisionLineEntity`/`MasterRevisionLineService`, multi-row hub UI, historical read-only detail + download; E2E **7/7** + UIUX **6** screenshots; backend **564**, frontend **528** Vitest |
| Phase B multi-revision history | Done (P2-T06, 2026-07-01) | Full persisted revision rows + paginated history API + historical deep-link/download — see `catalog-navigation-ux.md` § phased delivery |

**Gate evidence (P2-T06 slice):** `mvn -B -ntp -f backend/pom.xml verify` — **564** tests; `pnpm -C frontend lint` / `type-check` / `test` (**528**) / `build`; Playwright `master-revision-two-page.spec.ts` **7/7**; UIUX `P2-T06-uiux-evidence.spec.ts` **6** screenshots @ 1440×900.

**Gate evidence (P2-T05 slice):** `mvn -B -ntp -f backend/pom.xml verify`; `pnpm -C frontend lint` / `type-check` / `test` / `build`; Playwright `master-revision-two-page.spec.ts` 4/4 (Phase A baseline; extended to 7/7 in P2-T06).

Task row: [detail/P2-master-management.md](./detail/P2-master-management.md).

## P19 verifiability & publish gate (mirror — 2026-06-25)

| Item | Status | Evidence |
| --- | --- | --- |
| P19-T04 Change-diff | Done | `ChangeDiffService`, `ChangeDiffServiceTest` (3), `TemplateChangeDiffPanel`, template API |
| P19-T05 Preview comparison | Done | `PreviewComparisonService`, `PreviewComparisonServiceTest` (3), preview panel comparison UI |
| P19-T06 Live publish gate | Done | `PublishGateService`, `PublishGateServiceTest` (4), `TemplateLifecycleService.publish` gate |
| P19-T07 Decision forms | Done | `DecisionFormService`, `DecisionFormServiceTest` (3), `TemplateLifecycleDecisionDialog` |
| P19-T08 Risk-prompt config | Done | V21 `risk_prompt_config`, `RiskPromptConfigService`, `RiskPromptConfigServiceTest` (3), config panel |
| P19-T09 Exception intervention | Done | `ExceptionInterventionTest` (3), separate audit marker in `ManagementAuditRecorder` |
| P19-T10 Verifiability UI | Done | Coverage/change-diff/preview/checklist/decision/risk panels; Vitest; Docker E2E **7/7** |
| COR-T01/T02/T03/T08/T14 | Done | Live publish gate, structured decision forms, exception path, batch+coverage wiring, publish summary dialog |

**Gate evidence (P19 Wave 1):** `mvn -B -ntp -f backend/pom.xml verify` — **281** tests;
`pnpm -C frontend lint` / `type-check` / `test` (**169**) / `build`; `pnpm -C frontend test:e2e:docker` — **7/7**.

Task rows: [detail/P19-verifiability-publish-gate.md](./detail/P19-verifiability-publish-gate.md).

## P12-BDD-RISK-PROMPT-UX-001 — template risk-prompt UX (mirror — 2026-07-02)

**BDD:** `BDD-TEMPLATE-RISK-PROMPT-UX-001` (READY). **Residual:** P19-T08R / P19-T10R. **COR:** COR-T15.

| Sub-task | Owner | Status | Scope summary |
| --- | --- | --- | --- |
| P12-BDD-RISK-PROMPT-UX-001-T01 | backend-engineer | Done | Template override Flyway V37/V38 + GET/PUT API; GLOBAL-only global config; resolve chain; audit; auth |
| P12-BDD-RISK-PROMPT-UX-001-T02 | backend-engineer | Done | Decision effective-config by `templateId`; TEST_FAIL / APPROVAL_REJECT wiring |
| P12-BDD-RISK-PROMPT-UX-001-T03 | frontend-engineer | Done | Panel refactor; remove list view; create collapse + detail section; i18n |
| P12-BDD-RISK-PROMPT-UX-001-T04 | frontend-engineer | Done | `TemplateLifecycleDecisionDialog` resolved categories via `getDecisionFormConfig` |
| P12-BDD-RISK-PROMPT-UX-001-T05 | e2e-test-engineer + e2e-uiux-reviewer | Done | Playwright BDD S1–S7, S11–S12; UIUX manifest PASS |

**Gate evidence:** backend `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (**609** Surefire);
frontend `pnpm -C frontend lint`, `type-check`, `test` (**571** Vitest), `build` green.
Playwright **5/5** Docker (`P12-BDD-RISK-PROMPT-UX-001*.spec.ts`); UIUX manifest PASS
(`frontend/e2e/evidence/P12-BDD-RISK-PROMPT-UX-001-uiux-manifest.md`).

Task rows: [detail/P12-deferred-enhancements.md](./detail/P12-deferred-enhancements.md) § P12-BDD-RISK-PROMPT-UX-001.

## P17 per-domain API policy governance (mirror — 2026-06-25)

| Item | Status | Evidence |
| --- | --- | --- |
| P17-T01 policyVersion lineage | Done | Flyway `V23__api_policy_version.sql`; `ApiPolicyVersionEntity`/`ApiPolicyVersionRepository`; `ApiPolicyVersionSnapshotService` snapshot on domain save; `ApiPolicyVersionLineageTest` (3) |
| P17-T02 Per-domain save endpoints | Done | Five domain PUTs (`/ad-groups`, `/output`, `/batch-limits`, `/encryption`, `/default-route`); `Save*Request` DTOs + `confirmed` gate; Flyway V24 batch limits; `ApiPolicyDomainSaveServiceTest` (8/8); monolithic PUT back-compat seam retained |
| P17-T03 Impact-preview engine | Done | `ApiPolicyImpactPreviewService` + `ApiPolicySaveGate` hard-block/warning + UI confirm (minimal matrix seam noted for follow-up) |
| P17-T04 Default-route governance | Done | `DefaultRouteGovernanceTest` (4/4); callable-target validation; immediate effect + audit |
| P17-T05 Rollback | Done | `ApiPolicyRollbackService` + `ApiPolicyRollbackServiceTest` (3/3) |
| P17-T06 AD-group cache invalidation | Done | `AdGroupAuthorizationCacheTest` (2/2); invalidate on AD-group domain save |
| P17-T07 Audit API_POLICY_UPDATED | Done | `ManagementAuditRecorderTest`; `PolicyUpdateAuditDetail` with prev/next `policyVersion`, preview summary |
| P17-T08 Contract policyVersion view | Done | `ContractAssemblyServicePolicyViewTest` (3/3) |
| P17-T09 UI config-domain navigation | Done | `ApiPolicyDetailView` + `ApiPolicyDetailView.test.ts` (4/4); domain nav, hard-block, warning confirm, domain save |
| COR-F18 API policy domain UI | Done | Same as T09; `apiPolicyDomain.test.ts`, `ApiPolicyImpactPreviewPanel` |

**Gate evidence (P17 close):** `mvn -B -ntp -f backend/pom.xml verify` — **308** tests (2026-06-25);
`pnpm -C frontend lint` / `type-check` / `vitest run` (**189**, 53 files) / `build`.

Task rows: [detail/P17-api-policy-domain-governance.md](./detail/P17-api-policy-domain-governance.md).

## COR-4 performance (mirror — 2026-06-25)

| Item | Status | Evidence |
| --- | --- | --- |
| COR-P01 Stream new sync artifact generation | Done | `RuntimeGenerationService.generateSync` new path returns storage stream (aligned with idempotency replay); `RuntimeGenerationServiceGenerateTest` lazy-load assertion |
| COR-P02 Offload LibreOffice from request thread | Done | `PdfConversionOffloadSupport` + bounded `pdfConversionExecutor`; `LibreOfficePdfConversionService` / `DockerExecPdfConversionService`; +5 tests |
| COR-P07 QueryDSL audit list queries | Done | `ManagementAuditEventRepositoryImpl` + `ManagementAuditEventQueryPredicates`; `ManagementAuditEventRepositoryQuerydslTest` 5/5 |
| COR-P08 MapStruct apimgmt mappers | Done | `ApiPolicyViewMapper` + `ApiPolicyViewMapperTest` 4/4; `ApiManagementService` / `ApiPolicyRollbackService` wired |

**Gate evidence (COR-4 close):** `mvn -B -ntp -f backend/pom.xml verify` — **323** tests (2026-06-25).

## P14 confirmed large domains (mirror — 2026-06-26)

| Item | Status | Evidence |
| --- | --- | --- |
| P14-T01 Clause / content module lifecycle (vertical slice) | Done | T01a–T01d complete — domain + REST + template references + management UI; backend **461**; frontend **224** |
| P14-T01a Domain model + Flyway + repository | Done | `contentmodule` package — `ContentModuleEntity`, `ContentModuleVersionEntity`, `ContentModuleReviewState`, `ContentModuleLifecycleState`, `ContentModuleRepository`, `ContentModuleVersionRepository`; Flyway `V25__content_module.sql`; `ContentModuleRepositoryTest` (7 `@DataJpaTest`) |
| P14-T01b Management REST CRUD + lifecycle transitions | Done | `ContentModuleController`, `ContentModuleService`, `ContentModuleReviewService`, `ContentModuleLifecycleService`, `ContentModuleAccessSupport`; CRUD + governance REST; `GroupAccessService` scoping; `ManagementAuditRecorder` — 5 `recordContentModule*` events; i18n + OpenAPI v1; **61** contentmodule tests (T01a 7 + T01b 60) |
| P14-T01c Template reference + impact analysis integration | Done | Flyway `V27__template_content_module_reference.sql`; `TemplateContentModuleReferenceService` (reference lock + `resolvePinnedContentStructures`), `ContentModuleLifecycleImpactService`; REST GET/PUT template content-module-references + GET lifecycle impact preview; `PublishGateCheckCode.CONTENT_MODULE_REFERENCES`; `DocxAssembler` `contentModuleRef` + pinned resolve in `DocumentGenerationEngine` / `PreviewGenerationService`; +5 tests (`TemplateContentModuleReferenceServiceTest` pinned resolve, `DocxAssemblerTest`, `TemplateContentModuleReferenceRepositoryTest`, `ContentModuleLifecycleImpactServiceTest`, `PublishGateServiceTest`) |
| P14-T01d Management UI (list, detail, lifecycle) | Done | `ContentModuleListView`, `ContentModuleDetailView`, `ContentModuleLifecycleImpactDialog`, `ContentModuleCreateDialog`, `ContentModuleVersionDialog`, `ContentModuleStatusBadge`, `TemplateContentModuleReferencesPanel`; `contentModules` store/API + `routeKeys` nav; Vitest `ContentModuleListView.test.ts` (2), `ContentModuleDetailView.test.ts` (2), `contentModules.test.ts` (4); frontend lint/type-check/test (**224**) / build green |
| P14-T01 E2E + UIUX | Done | `frontend/e2e/P14-T01-uiux-evidence.spec.ts` + `a11y-smoke.spec.ts` **3/3**; manifest `frontend/e2e/evidence/P14-T01-uiux-manifest.md` (8 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** (no critical blockers; 2 suggestions) |
| P14-T01 architecture critical remediation | Done | 4 Critical fixes from architecture review BLOCK — `GroupAccessService.canBrowseContentModuleCatalog` fail-closed (TESTER 403 on list/get); `contentStructureJson` role-gated in `ContentModuleVersionView`; list merges shared-into-group modules; `ContentModuleLifecycleImpactService` runtime audit counts + `templateStopRequired`/`releaseStopRequired`; `ContentModuleLifecycleAuditDetail` persisted via `ManagementAuditRecorder`; +8 tests (`ContentModuleServiceTest` +3, `ContentModuleControllerTest` +3, `ContentModuleLifecycleImpactServiceTest` +1, `GroupAccessServiceTest` +1); verify **469** (+8 from 461); architecture re-review **PASS** (2026-06-26) |
| P14-T02a To-do entity + query API by role queue | Done | Flyway `V28__collaboration_work_item.sql`; `com.bank.docgen.collaboration` — `CollaborationWorkItemEntity`, `CollaborationWorkItemRepository`, `CollaborationWorkItemService`, `CollaborationWorkItemController`; `GET /api/management/v1/collaboration-work-items` with role queue filtering; `CollaborationWorkItem*Test` **18** |
| P14-T02b Timeout config API (global + group override) | Done | Flyway `V29__collaboration_timeout_config.sql`; `CollaborationTimeoutConfigEntity`, `CollaborationTimeoutConfigRepository`, `CollaborationTimeoutConfigService`, `CollaborationTimeoutConfigController`, `CollaborationTimeoutResolver`; GET/PUT `/api/management/v1/collaboration-timeout-config`; `GroupAccessService.canMaintainCollaborationTimeoutConfig`; OpenAPI v1 extended; `CollaborationTimeoutConfigServiceTest` (6), `CollaborationTimeoutConfigControllerTest` (4), `GroupAccessServiceTest` collaborationTimeout (1) = **11** T02b tests |
| P14-T02c Escalation scheduler (no state mutation) | Done | Flyway `V30__collaboration_work_item_escalation_source.sql`; `CollaborationEscalationService`, `CollaborationEscalationScheduler`, `CollaborationSchedulingConfig`; `source_work_item_id` dedup; `findOpenEscalationCandidates` / `existsOpenEscalationForSource`; `ManagementAuditRecorder.recordCollaborationTimeoutEscalation`; notification-only — source work item status unchanged; `CollaborationEscalationServiceTest` (5), `CollaborationEscalationServiceDataJpaTest` (3), `CollaborationEscalationSchedulerTest` (1), `CollaborationWorkItemRepositoryTest` escalation queries (2) = **11** T02c tests |
| P14-T02d UI: collaboration on Dashboard + admin config | Done | Collaboration work items on `DashboardView` tasks section (COR-T11); `CollaborationTimeoutConfigPanel` on Dashboard; `collaboration` store/API/types + `useWorkflowTasks`; legacy workbench routes redirect to `/dashboard#tasks-section`; Vitest `DashboardView.test.ts`, `useWorkflowTasks.test.ts`, `collaboration.test.ts`; frontend **235** at E2E close (2026-06-27) |
| P14-T02 Collaboration to-dos + timeout escalation (vertical slice) | Done | T02a–T02d complete — work item API + timeout config + escalation scheduler + management UI; backend verify **481**; frontend **235** (2026-06-27) |
| P14-T02 E2E + functional | Done | `frontend/e2e/collaboration-todos.spec.ts` **3/3** Docker 4173 (2026-06-27); report `playwright-report/docker/index.html`; remediation — `BatchTestRunEntity` FK alignment, `CollaborationWorkItemWriter` submit-for-test upsert, `roles.ts` `canAccess*Workbench` route guards |
| P14-T02 E2E + UIUX | Done | `frontend/e2e/P14-T02-uiux-evidence.spec.ts` + `a11y-smoke.spec.ts` **5/5**; manifest `frontend/e2e/evidence/P14-T02-uiux-manifest.md` (8 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** (no critical blockers; 4 suggestions) |
| P14-T03 Template export / import (vertical slice) | Done | T03a–T03c complete — `TemplateExportService` / `TemplateImportService` + management REST; OpenAPI v1 + `contract-outline.md`; UI `TemplateExportActions` / `TemplateImportDialog`; **28** backend + **14** frontend Vitest; backend verify **481**; frontend **235+** (2026-06-27) |
| P14-T03a Export service + management endpoint | Done | `TemplateExportService` + GET export JSON/ZIP; `TemplateExport*Test` **15**; OpenAPI v1 export routes |
| P14-T03b Import service + validation + DRAFT landing | Done | `POST /templates/import`; `TemplateImport*Test` **13**; import lands DRAFT; conflict policy in OpenAPI |
| P14-T03c UI export/import on template detail + admin bulk | Done | `TemplateExportActions`, `TemplateImportDialog`; **+14** Vitest; frontend lint/type-check/test/build green |
| P14-T03 E2E + functional | Done | `frontend/e2e/P14-T03-template-export-import.spec.ts` **2/2** Docker 4173 (2026-06-27); JSON + ZIP export, staging import → DRAFT, no secrets in bundle |
| P14-T03 E2E + UIUX | Done | `frontend/e2e/P14-T03-uiux-evidence.spec.ts` + `P14-T03-template-export-import.spec.ts` + `a11y-smoke.spec.ts` **7/7**; manifest `frontend/e2e/evidence/P14-T03-uiux-manifest.md` (9 screenshots @ 1440×900); e2e-uiux-reviewer **PASS** (no critical blockers; 5 suggestions) |

**Gate evidence (P14-T03 vertical slice):** T03a–T03c Done — `TemplateExportService` / `TemplateImportService`; **28** backend tests (`TemplateExport*Test` 15 + `TemplateImport*Test` 13); OpenAPI v1 + [`contract-outline.md`](../api/contract-outline.md) export/import contract; UI `TemplateExportActions` / `TemplateImportDialog` (+14 Vitest); E2E `P14-T03-template-export-import.spec.ts` **2/2** Docker 4173; UIUX `P14-T03-uiux-evidence.spec.ts` + functional + `a11y-smoke.spec.ts` **7/7**; manifest `frontend/e2e/evidence/P14-T03-uiux-manifest.md` (9 screenshots); e2e-uiux-reviewer **PASS**; backend verify **481**; frontend lint/type-check/test (**235+**) / build green (2026-06-27).

**Gate evidence (P14 phase close):** All P14-T01…T03 tasks Done with real persistence; role-scoped UI for each domain; audit on mutating actions; green gates cited above; UX Wave D **Done**; **P15 Done** (2026-06-27; T01–T10); **P18 Done** (2026-06-28).

**Gate evidence (P15-T01a slice):** `backend/Dockerfile.packaged` — `eclipse-temurin:21-jre-alpine` non-root UID/GID **65532**; read-only runtime smoke — `docker run --read-only --tmpfs /tmp` serves **`GET /healthz` 200**; writable mount contract — [`deploy/container-hardening.md`](../deploy/container-hardening.md) (`/tmp` tmpfs for backend JVM temp); `scripts/container-hardening-smoke.ps1` PASSED (2026-06-27).

**Gate evidence (P15-T01b slice):** `frontend/Dockerfile.packaged` — `nginx:1.27-alpine` non-root UID **101**, listen **8080**; read-only runtime smoke — `docker run --read-only --user nginx --tmpfs /tmp` serves **`GET /healthz` 200** and **`GET /` 200**; writable mount contract — [`deploy/container-hardening.md`](../deploy/container-hardening.md) (`/tmp/nginx/*` tmpfs for NGINX pid/logs/temp); `scripts/container-hardening-smoke.ps1` PASSED (backend + frontend) (2026-06-27).

**Gate evidence (P15-T01c slice):** [`deploy/container-hardening.md`](../deploy/container-hardening.md) — backend `/tmp` tmpfs + frontend `/tmp/nginx/*` writable paths documented; P15-T01 smoke evidence section (2026-06-27) — `ReadonlyRootfs` **true**, container user uid **101**, `/etc` write blocked, `/healthz` + SPA **200**; `scripts/container-hardening-smoke.ps1` PASSED (backend + frontend) (2026-06-27).

**Gate evidence (P15-T01 vertical slice):** T01a–T01c Done — hardened `backend/Dockerfile.packaged` + `frontend/Dockerfile.packaged`; writable mount contract + smoke evidence in [`deploy/container-hardening.md`](../deploy/container-hardening.md); `scripts/container-hardening-smoke.ps1` PASSED (2026-06-27).

**Gate evidence (P15-T02 slice):** `deploy/helm/docgen/` — Helm chart for backend + frontend workloads; per-env values (`values.yaml`, `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml`); pod/container `securityContext` — `runAsNonRoot`, `readOnlyRootFilesystem`, `capabilities.drop: [ALL]`; CPU + memory `requests`/`limits` on all containers (ADR-0030); [`deploy/helm/docgen/README.md`](../deploy/helm/docgen/README.md) lint/template commands; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — helm lint **0 failed**, `helm template` default/dev/staging/prod, fail-closed secret check; render-only (not deployed to cluster) (2026-06-27).

**Gate evidence (P15-T03 slice):** [`deploy/k8s-config-secrets.md`](../deploy/k8s-config-secrets.md) — ConfigMap keys for non-sensitive runtime + external service endpoints; Secret refs via `secrets.create: false` + `existingSecretName` (all env values); optional `ExternalSecret` template; no credential keys in ConfigMap; no `StatefulSet` in rendered manifests; backend `envFrom` ConfigMap+Secret; `scripts/helm-validate.ps1` — `Assert-T03ConfigSecrets` + fail-closed missing-secret negative test; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03 assertions; render-only; no plaintext secrets in repo (2026-06-27).

**Gate evidence (P15-T04 slice):** [`deploy/k8s-ingress-tls.md`](../deploy/k8s-ingress-tls.md) — ClusterIP Services on port **8080** with `docgen.io/cluster-dns` FQDN annotations (T04a); `templates/ingress.yaml` — `ingressClassName: nginx`, `/api`→backend, `/`→frontend, cert-manager issuer annotation (T04b); `templates/certificate.yaml` — cert-manager `Certificate`, TLS **1.2+** `ssl-protocols` (T04c); `scripts/helm-validate.ps1` — `Assert-T04IngressTls`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04 assertions; render-only (2026-06-27).

**Gate evidence (P15-T05 slice):** [`deploy/k8s-hpa-autoscaling.md`](../deploy/k8s-hpa-autoscaling.md) — `templates/backend-hpa.yaml` + `templates/frontend-hpa.yaml` — `autoscaling/v2`, CPU + memory Resource metrics, min/max replica bounds, blue-green active Deployment `scaleTargetRef` (T05a); backend Pods custom metric `docgen_http_requests_per_second` gated by `autoscaling.backend.customMetric.enabled`; `values-staging.yaml` explicit customMetric; Prometheus Adapter prerequisite documented (T05b); `scripts/helm-validate.ps1` — `Assert-T05Hpa`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05 assertions; render-only (2026-06-27).

**Gate evidence (P15-T06 slice):** [`deploy/k8s-network-policy.md`](../deploy/k8s-network-policy.md) — `templates/networkpolicy.yaml` — `podSelector: {}` default-deny with Ingress + Egress `policyTypes` (T06a); explicit allow policies — ingress controller, frontend→backend :8080, backend→external egress (TCP **5432/6379/9092/443**), DNS, metrics scrape gated by `networkPolicy.monitoring.enabled` (T06b); `scripts/helm-validate.ps1` — `Assert-T06NetworkPolicy`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06 assertions; render-only (2026-06-27).

**Gate evidence (P15-T07 slice):** [`deploy/k8s-health-probes.md`](../deploy/k8s-health-probes.md) — backend Deployments `httpGet` liveness `/healthz`, readiness `/readyz` on port **8080** (T07a); frontend Deployments + NGINX ConfigMap `/healthz` + `/readyz` (T07b); `HealthController` + `ReadinessProbe` Postgres semantics (T07c); `scripts/helm-validate.ps1` — `Assert-T07Probes`; `.\scripts\helm-validate.ps1 -SkipKubeconform` PASSED — lint **0 failed**, template default/dev/staging/prod, T03+T04+T05+T06+T07 assertions; render-only (2026-06-27).

## P15 Kubernetes deployment & container hardening (mirror — 2026-06-27)

| Task | Status | Evidence |
| --- | --- | --- |
| P15-T01a Backend image hardening (distroless/minimal, non-root, read-only root FS) | Done | `backend/Dockerfile.packaged` alpine JRE UID **65532**; read-only smoke `/healthz` **200**; [`deploy/container-hardening.md`](../deploy/container-hardening.md) writable paths |
| P15-T01b Frontend NGINX hardening (non-root, read-only root FS, unprivileged config) | Done | `frontend/Dockerfile.packaged` nginx UID **101** port **8080**; read-only smoke `/healthz` + `/` **200**; [`deploy/container-hardening.md`](../deploy/container-hardening.md) frontend writable paths; `scripts/container-hardening-smoke.ps1` PASSED |
| P15-T01c Writable paths documentation + image-run smoke evidence | Done (2026-06-27) | [`deploy/container-hardening.md`](../deploy/container-hardening.md) — backend `/tmp`, frontend `/tmp/nginx/*`; smoke evidence (ReadonlyRootfs, uid **101**, `/healthz` + SPA **200**); `scripts/container-hardening-smoke.ps1` PASSED (backend + frontend) |
| P15-T01 Container hardening (vertical slice) | Done (2026-06-27) | T01a–T01c complete — distroless/minimal non-root read-only images + documented writable mounts + smoke evidence |
| P15-T02a Chart scaffold + per-env values (backend/frontend) | Done (re-earned 2026-06-27) | `deploy/helm/docgen/`; [`deploy/helm/docgen/README.md`](../deploy/helm/docgen/README.md); helm-validate PASSED |
| P15-T02b Deployment + Service with hardened securityContext | Done (re-earned 2026-06-27) | non-root, read-only root FS, drop ALL caps; `deploy/helm/docgen/templates/` |
| P15-T02c Resource requests + limits (CPU/memory) all containers | Done (re-earned 2026-06-27) | ADR-0030 requests/limits in `values.yaml`; helm-validate fail-closed secret check PASSED |
| P15-T03a ConfigMap for non-sensitive runtime config (per-env values) | Done (re-earned 2026-06-27) | `templates/configmap.yaml` + per-env `externalServices`/`config`; no credential keys in ConfigMap; [`deploy/k8s-config-secrets.md`](../deploy/k8s-config-secrets.md) |
| P15-T03b Secret / external-secret references for credentials + KMS-managed keys | Done (re-earned 2026-06-27) | `secrets.create: false` in all env values; `existingSecretName` + fail-closed `docgen.secretName` helper; helm-validate missing-secret negative test PASSED |
| P15-T03c External managed-service endpoint wiring (no in-cluster StatefulSets) | Done (re-earned 2026-06-27) | External hosts in ConfigMap; credentials in Secret refs; no `StatefulSet` in rendered manifests; backend `envFrom` ConfigMap+Secret |
| P15-T04a Service definitions (backend/frontend) using K8s DNS naming | Done (re-earned 2026-06-27) | `templates/*-service.yaml` ClusterIP port **8080**; `docgen.io/cluster-dns` + FQDN helpers; [`deploy/k8s-ingress-tls.md`](../deploy/k8s-ingress-tls.md) |
| P15-T04b NGINX Ingress resource with class + host/path routing | Done (re-earned 2026-06-27) | `templates/ingress.yaml` — `ingressClassName: nginx`, `/api`→backend, `/`→frontend; staging/prod `ingress.enabled: true` |
| P15-T04c cert-manager Certificate/issuer integration for TLS 1.2+ | Done (re-earned 2026-06-27) | `templates/certificate.yaml`; `nginx.ingress.kubernetes.io/ssl-protocols: TLSv1.2 TLSv1.3`; `Assert-T04IngressTls` PASSED |
| P15-T04 Service exposure (vertical slice) | Done (re-earned 2026-06-27) | T04a–T04c complete — ClusterIP + K8s DNS, NGINX Ingress routing, cert-manager TLS; helm-validate PASSED; render-only |
| P15-T05a HPA (CPU + memory) with min/max bounds for backend/frontend | Done (re-earned 2026-06-27) | `templates/backend-hpa.yaml`, `templates/frontend-hpa.yaml` — `autoscaling/v2`, CPU + memory Resource metrics, min/max bounds, blue-green `scaleTargetRef`; [`deploy/k8s-hpa-autoscaling.md`](../deploy/k8s-hpa-autoscaling.md); `Assert-T05Hpa` PASSED |
| P15-T05b Custom-metric scaling source wiring (metrics adapter documented) | Done (re-earned 2026-06-27) | backend Pods custom metric `docgen_http_requests_per_second` gated by `customMetric.enabled`; `values-staging.yaml` explicit customMetric; Prometheus Adapter prerequisite in [`deploy/k8s-hpa-autoscaling.md`](../deploy/k8s-hpa-autoscaling.md); helm-validate PASSED; render-only |
| P15-T05 Autoscaling (vertical slice) | Done (re-earned 2026-06-27) | T05a–T05b complete — HPA CPU/memory + custom Pods metric; helm-validate PASSED; render-only |
| P15-T06a Default-deny ingress+egress NetworkPolicy for app namespace | Done (re-earned 2026-06-27) | `templates/networkpolicy.yaml` — `podSelector: {}` default-deny; [`deploy/k8s-network-policy.md`](../deploy/k8s-network-policy.md); `Assert-T06NetworkPolicy` PASSED |
| P15-T06b Explicit allow rules (ingress, backend↔external services, DNS, metrics) | Done (re-earned 2026-06-27) | allow policies — ingress controller, frontend→backend, backend external egress, DNS, metrics (`networkPolicy.monitoring.enabled`); helm-validate PASSED; render-only |
| P15-T06 Network isolation (vertical slice) | Done (re-earned 2026-06-27) | T06a–T06b complete — default-deny + explicit allow; helm-validate PASSED; render-only |
| P15-T07a Liveness/readiness on backend Deployment | Done (re-earned 2026-06-27) | `templates/backend-deployment.yaml` + color variants — `/healthz` + `/readyz` port **8080**; [`deploy/k8s-health-probes.md`](../deploy/k8s-health-probes.md); `Assert-T07Probes` PASSED |
| P15-T07b Frontend readiness/liveness probes | Done (re-earned 2026-06-27) | `frontend-nginx-configmap.yaml` NGINX `/healthz` + `/readyz`; frontend Deployments; helm-validate PASSED; render-only |
| P15-T07c Backend `/healthz` + `/readyz` semantics | Done (re-earned 2026-06-27) | `HealthController` + `ReadinessProbe` — Postgres check on `/readyz`; [`deploy/k8s-health-probes.md`](../deploy/k8s-health-probes.md); helm-validate PASSED; render-only |
| P15-T07 Health probes (vertical slice) | Done (re-earned 2026-06-27) | T07a–T07c complete — dual-endpoint probes backend+frontend; helm-validate PASSED; render-only |
| P15-T08a Blue-green deployment manifests/strategy | Done (re-earned 2026-06-27) | dual color Deployments; activeColor Service selector; preview Services; [`deploy/blue-green-runbook.md`](../deploy/blue-green-runbook.md); `Assert-T08BlueGreen` PASSED |
| P15-T08b Production manual-approval gate | Done (re-earned 2026-06-27) | `values-prod.yaml` — `blueGreen.requireManualApproval: true`; cutover gate in runbook |
| P15-T08c Manual rollback runbook | Done (re-earned 2026-06-27) | color revert steps; no data destroy; Flyway forward-only note |
| P15-T08 Blue-green release (vertical slice) | Done (re-earned 2026-06-27) | T08a–T08c complete — helm-validate PASSED; render-only |
| P15-T09a CI helm lint + template all envs | Done (re-earned 2026-06-27) | [`.github/workflows/k8s-manifest-gates.yml`](../.github/workflows/k8s-manifest-gates.yml); [`scripts/ci-k8s-manifest-gates.ps1`](../scripts/ci-k8s-manifest-gates.ps1); path filters `deploy/**` + gate scripts; local `-SkipKubeconform` PASSED |
| P15-T09b CI kubeconform blocking gate | Done (re-earned 2026-06-27) | blocking kubeconform K8s **1.29.0**; Docker fallback on CI; [`deploy/ci-k8s-gates.md`](../deploy/ci-k8s-gates.md); full gate on CI runner (local full kubeconform blocked by Docker Hub timeout) |
| P15-T09 CI manifest validation (vertical slice) | Done (re-earned 2026-06-27) | T09a–T09b complete — ADR-0030 blocking CI gate landed |

Task rows: [detail/P15-kubernetes-deployment-container-hardening.md](./detail/P15-kubernetes-deployment-container-hardening.md).

**Gate evidence (P15-T09 slice):** [`.github/workflows/k8s-manifest-gates.yml`](../.github/workflows/k8s-manifest-gates.yml) — PR + push to `main`, path filters `deploy/**`, `scripts/helm-validate.ps1`, `scripts/ci-k8s-manifest-gates.ps1`; job **Helm lint/template + kubeconform** runs `.\scripts\ci-k8s-manifest-gates.ps1` (helm lint **0 failed**, template default/dev/staging/prod, T03–T08 custom assertions, blocking kubeconform); [`deploy/ci-k8s-gates.md`](../deploy/ci-k8s-gates.md) + [`deploy/README.md`](../deploy/README.md) CI section; local `.\scripts\ci-k8s-manifest-gates.ps1 -SkipKubeconform` PASSED (2026-06-27); full kubeconform expected green on CI runner — local full gate blocked by Docker Hub pull timeout (2026-06-27).

**Gate evidence (P14-T01a slice):** `mvn -B -ntp -f backend/pom.xml verify` — **330** tests (+7 from 323 baseline, 2026-06-26).

**Gate evidence (P14-T01b slice):** `mvn -B -ntp -f backend/pom.xml verify` — **456** tests BUILD SUCCESS (2026-06-26); contentmodule **61** tests (T01a `ContentModuleRepositoryTest` 7 + T01b controller/service/access/review/lifecycle 60).

**Gate evidence (P14-T01c slice):** `mvn -B -ntp -f backend/pom.xml verify` — **461** tests BUILD SUCCESS (+5 from 456 T01b baseline, 2026-06-26).

**Gate evidence (P14-T01d slice):** `pnpm -C frontend lint` / `type-check` / `test` (**224**) / `build` — all green (2026-06-26); `ContentModuleListView`, `ContentModuleDetailView`, `ContentModuleLifecycleImpactDialog`, `TemplateContentModuleReferencesPanel`; backend baseline **461** tests (T01a–c).

**Gate evidence (P14-T01 vertical slice):** T01a–T01d Done — backend **461** + frontend **224**; real persistence + role-scoped management UI + template reference panel; audit on mutating governance actions (T01b).

**Gate evidence (P14-T01 UIUX slice):** `pnpm exec playwright test e2e/P14-T01-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` — **3/3** passed (2026-06-26); e2e-uiux-reviewer **PASS** — manifest `frontend/e2e/evidence/P14-T01-uiux-manifest.md` (8 screenshots: content modules list REDBC/GREENBC, create dialog, draft detail, template references panel, lifecycle impact dialog, brand header both brands); a11y smoke — login heading + form controls, content modules `h1`, primary action visible.

**Gate evidence (P14-T01 architecture remediation slice):** `mvn -B -ntp -f backend/pom.xml verify` — **469** tests BUILD SUCCESS (+8 from 461 T01d baseline, 2026-06-26); Checkstyle/PMD/SpotBugs/JaCoCo green. Fixes: (1) TESTER catalog browse blocked — `canBrowseContentModuleCatalog` + `assertCatalogBrowseAllowed` on list/get; (2) draft `contentStructureJson` readback gated by `canViewContentModuleStructure`; (3) list includes modules shared into query group; (4) real impact analysis — `RuntimeGenerationAuditEventRepository` 7d call counts, `templateStopRequired`/`releaseStopRequired`, `ContentModuleLifecycleAuditDetail` in lifecycle audit. **Architecture re-review:** **PASS** (2026-06-26; agent `16ac320a-56d5-43e7-8094-931ff1540893`; prior BLOCK 4 Critical remediated).

**Gate evidence (P14-T01e regression slice):** `mvn -B -ntp -f backend/pom.xml verify` — **471** tests BUILD SUCCESS (+2 from 469 architecture remediation baseline, 2026-06-26); +3 regression tests from retry delegation; Checkstyle/PMD/SpotBugs/JaCoCo green.

**Gate evidence (P14-T02a slice):** `mvn -B -ntp -f backend/pom.xml verify` — **471** tests BUILD SUCCESS (2026-06-26); Flyway `V28__collaboration_work_item.sql`; `CollaborationWorkItemEntity`, `CollaborationWorkItemRepository`, `CollaborationWorkItemService`, `CollaborationWorkItemController`; `GET /api/management/v1/collaboration-work-items` with role queue filtering; `CollaborationWorkItem*Test` **18** (repository 4, service 6, access 6, controller 2); Checkstyle/PMD/SpotBugs/JaCoCo green.

**Gate evidence (P14-T02b slice):** `mvn -B -ntp -f backend/pom.xml verify` — **471** tests BUILD SUCCESS (2026-06-26); Flyway `V29__collaboration_timeout_config.sql`; `CollaborationTimeoutConfigEntity`, `CollaborationTimeoutConfigRepository`, `CollaborationTimeoutConfigService`, `CollaborationTimeoutConfigController`, `CollaborationTimeoutResolver`; GET/PUT `/api/management/v1/collaboration-timeout-config` (global + group override); `GroupAccessService.canMaintainCollaborationTimeoutConfig`; OpenAPI v1 extended; **11** T02b tests (`CollaborationTimeoutConfigServiceTest` 6, `CollaborationTimeoutConfigControllerTest` 4, `GroupAccessServiceTest` collaborationTimeout 1); Checkstyle/PMD/SpotBugs/JaCoCo green.

**Gate evidence (P14-T02c slice):** P14-T02c targeted tests **11/11** green (2026-06-26) — `CollaborationEscalationServiceTest` (5), `CollaborationEscalationServiceDataJpaTest` (3), `CollaborationEscalationSchedulerTest` (1), `CollaborationWorkItemRepositoryTest` escalation queries (2). Flyway `V30__collaboration_work_item_escalation_source.sql`; `CollaborationEscalationService.processDueEscalations` creates ESCALATION queue notification items linked via `source_work_item_id` (dedup via `existsOpenEscalationForSource`); source work item status/queue unchanged; `ManagementAuditRecorder.recordCollaborationTimeoutEscalation`; `CollaborationEscalationScheduler` `@Scheduled` with `docgen.collaboration.escalation.enabled`. Full `mvn -B -ntp -f backend/pom.xml verify` — **475** tests BUILD SUCCESS (2026-06-27); Checkstyle/PMD/SpotBugs/JaCoCo green. **Environment remediation (no code change):** host `TEMP`/`TMP` redirected to `D:\temp` — prior BUILD FAILURE (2026-06-26) was disk space + `AsyncBatchTaskKafkaConsumerTest` (environment/unrelated).

**Gate evidence (P14-T02d slice):** `pnpm -C frontend lint` / `type-check` / `test` (**232** at slice close) / `build` — all green (2026-06-27); collaboration work items on `DashboardView` tasks section (COR-T11); `CollaborationTimeoutConfigPanel` on Dashboard; `collaboration` store/API/types + `useWorkflowTasks`; Vitest — `CollaborationTimeoutConfigPanel.test.ts`, `collaboration.test.ts`, `DashboardView.test.ts`, `useWorkflowTasks.test.ts`; backend baseline **475** verify (T02a–c). Post-close E2E remediation: frontend **235**, backend **481** (2026-06-27).

**Gate evidence (P14-T02 vertical slice):** T02a–T02d Done — backend work item entity + role-queue API (T02a), timeout config API (T02b), escalation scheduler notification-only (T02c **11** targeted + full verify **475**), management UI on Dashboard + admin timeout config (T02d); frontend **235** tests at E2E close (2026-06-27).

**Gate evidence (P14-T02 E2E slice):** `pnpm exec playwright test e2e/collaboration-todos.spec.ts --config playwright.docker.config.ts` — **3/3** passed (2026-06-27); report `playwright-report/docker/index.html`. **Backend remediation:** `mvn -B -ntp -f backend/pom.xml verify` — **481** tests BUILD SUCCESS (+6 from 475 T02d baseline); fixes — `BatchTestRunEntity` FK constraint alignment for E2E seed data, `CollaborationWorkItemWriter` open-work-item upsert on submit-for-test. **Frontend remediation:** `pnpm -C frontend lint` / `type-check` / `test` (**235**) / `build` green; `roles.ts` — `canAccessTesterWorkbench`, `canAccessApproverWorkbench`, `canAccessCollaborationEscalationWorkbench`, `canAccessLogicalRoute` workbench guards.

**Gate evidence (P14-T02 UIUX slice):** `pnpm exec playwright test e2e/P14-T02-uiux-evidence.spec.ts e2e/a11y-smoke.spec.ts --config playwright.docker.config.ts` — **5/5** passed (2026-06-27); e2e-uiux-reviewer **PASS** — manifest `frontend/e2e/evidence/P14-T02-uiux-manifest.md` (8 screenshots: tester/approver/escalation workbench REDBC/GREENBC, dashboard `CollaborationTimeoutConfigPanel` both brands); a11y smoke — login heading + form controls, content modules `h1`, tester workbench `h1`, timeout config panel heading; no critical UIUX blockers (4 suggestions: GREENBC nav active tint, E2E fixture noise, `lastUpdated` ISO formatting, empty-state evidence).

Task rows: [detail/P14-confirmed-large-domains.md](./detail/P14-confirmed-large-domains.md).

## Transitional implementation seams (2026-06-24)

Known gaps between ADR/guardrails and current code — **not** production-complete closure.
Each row lists exit criteria; remove from this index when closed.

| Seam | Current behavior | Exit criteria | Tracked in |
| --- | --- | --- | --- |
| AD Group resolution | `ConfigAdGroupResolver` — config-file stub, fail-closed | Production LDAP/AD adapter + integration tests | E05-T06, P6 |
| Async batch transport | Default in-process `@Async`; Kafka optional via `ASYNC_TRANSPORT=kafka` | Production profile uses Kafka + DLT; in-process dev-only documented | P11, M14 |
| Security forbidden-route audit | Log-only in some paths | Durable security audit event per matrix §13.3 | COR-P06 |
| QueryDSL / MapStruct / Redisson | QueryDSL audit + MapStruct apimgmt Done (2026-06-25); Lettuce | ADR-0037 opportunistic expansion ongoing | OPT-D3/D4 Done; COR-P05 |
| Publish gate checklist | Server-side live gate blocks publish on blockers/thresholds | **Done** (2026-06-25; `PublishGateService` + UI checklist + publish summary dialog) | COR-T01, P19-T06 |
| Runtime rate limit | Process-local Bucket4j; requests without credential headers bypass filter (auth layer rejects later) | Shared Redis limiter or documented fail-closed at filter; ADR 0031 alignment | COR-B10, OPT-F8 |
| Workbench vs Dashboard | **Done** — COR-T11 (2026-06-24): workflow + collaboration queues on Dashboard; legacy workbench routes redirect; standalone workbench views removed | COR-T11 decision in `docs/adr/decisions/2026-06-23-batch-b-workflow-defaults.md` | COR-T11 |
| zh-CN / `api.error` catalog | **Done (2026-06-25)** — en/zh `api.error` + primary journey zh-CN bundles | Residual non-primary keys may en-fallback until touched | P20-T06 Done |
| P19 verifiability | **Done (2026-06-25)** — T01–T10: batch test, coverage, change-diff, preview comparison, live publish gate, decision forms, risk prompts, exception markers, UI | Residual fidelity depth remains P18 | P19 Done, COR-L03 Done |
| Service-layer authorization | Route visibility not enforced at API filter | **Documented pattern + contract test (2026-06-24)** — ADR-0001 | COR-P06 |
| Redisson multi-instance locks | Lettuce cache only | **ADR-0039 evaluation recorded (2026-06-24)**; implement when multi-instance | COR-P05 |

## Sync maintenance

When any task completes:

1. Update owning `docs/plan/detail/<phase>.md` task row.
2. Update `master-plan.md` if phase status changes.
3. Update epic/milestone row in this ledger (or owning task sheet).
4. Update [orchestration-high-level-plan.md](../architecture/orchestration-high-level-plan.md) / [implementation-task-plan.md](../architecture/implementation-task-plan.md) if backlog order changes.
5. Run post-task doc sync (see `.cursor/agents/post-task-doc-sync.md`).
