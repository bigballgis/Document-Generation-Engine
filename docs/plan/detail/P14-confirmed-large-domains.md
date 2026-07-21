# P14 — Confirmed Large Domains (Detailed Plan)

**Phase status:** **Done** (2026-06-27) | **Depends on:** P2–P8  
**Implementation status:** **P14-T01 Done** (2026-06-26; vertical slice T01a–T01e + architecture remediation); **P14-T02 Done** (2026-06-27; vertical slice T02a–T02d); **P14-T03 Done** (2026-06-27; vertical slice T03a–T03c + E2E **2/2**). All exit criteria met.

## Behavior goal

Deliver three confirmed product domains that were deferred post-P11:

1. **Clause / content module lifecycle** (PRD §6.4.2, permission-matrix §5.1)
2. **Collaboration to-dos + timeout escalation** (PRD §协作, permission-matrix §5)
3. **Template export / import across environments** (PRD §环境迁移, permission-matrix §5)

Each domain is a vertical slice with backend persistence, management API, UI, audit,
and green gates before marking Done.

---

## P14-T01 Clause / content module lifecycle

**Slice status:** **Done** (2026-06-26) — T01a domain + Flyway, T01b management REST + governance, T01c template reference + impact, T01d management UI, T01e architecture remediation; backend verify **469** tests; frontend lint/type-check/test (**224**) / build green; architecture re-review **PASS** (2026-06-26).

### Actor / roles

| Action | GLOBAL | GROUP | MASTER_DESIGNER | TEMPLATE_AUTHOR | TESTER | APPROVER |
| --- | --- | --- | --- | --- | --- | --- |
| Create/edit draft, new version, submit approval | ✓ | scoped | ✓ | ✓ | ✗ | ✗ |
| Approve/reject module version | ✓ | scoped | ✗ | ✗ | ✗ | ✓ |
| Stop/restore/deprecate (logical) | ✓ | scoped | ✗ | ✗ | ✗ | ✗ |
| Reference in template (approved only) | ✓ | scoped | ✓ | ✓ | read-only in test | read-only |

### States

`DRAFT` → `PENDING_APPROVAL` → `APPROVED` → `STOPPED` / `DEPRECATED` (logical delete).

Implementation mapping (`reviewState` + `lifecycleState`, API `SUBMITTED` / `ACTIVE`): see
[`domain-model.md` §2.9.2.1](../../domain/domain-model.md#2921-产品状态--实现映射p14-t01).

### Acceptance scenarios

- **Given** an approved clause module v1.0 in group RETAIL, **When** author references it
  in a template publish candidate, **Then** the publish locks module version 1.0.
- **Given** module v1.1 approved, **When** template still references v1.0, **Then** generation
  uses v1.0 until author upgrades reference and re-runs test→approve→publish.
- **Given** GROUP admin, **When** deprecating a module with referencing templates, **Then**
  impact analysis lists templates; confirm + audit; existing published templates keep locked version.

### Tasks

| ID | Task | Status | Doc / API traceability |
| --- | --- | --- | --- |
| P14-T01a | Domain model + Flyway + repository | Done (2026-06-26) | [`domain-model.md` §2.9.2 / §2.9.2.1](../../domain/domain-model.md#292-条款或内容模块-clause-or-content-module); [`permission-matrix.md` §5.1](../../security/permission-matrix.md#51-条款或内容模块权限矩阵); [ADR-0019](../../adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md); no REST in this slice. **Evidence:** `com.bank.docgen.contentmodule` — `ContentModuleEntity`, `ContentModuleVersionEntity`, `ContentModuleReviewState`, `ContentModuleLifecycleState`, `ContentModuleRepository`, `ContentModuleVersionRepository`; Flyway `V25__content_module.sql`; `ContentModuleRepositoryTest` (7 `@DataJpaTest`); gate `mvn -B -ntp -f backend/pom.xml verify` — **330** tests (+7 from 323 baseline) |
| P14-T01b | Management REST CRUD + lifecycle transitions | Done (2026-06-26) | [`contract-outline.md` §内容模块治理](../../api/contract-outline.md#条款或内容模块治理契约); [OpenAPI v1](../../api/openapi-v1.yaml) — management CRUD (`/api/management/v1/content-modules` list/create/detail, version CRUD) + review/lifecycle governance routes; examples under [`docs/api/examples/content-module-*.json`](../../api/examples/README.md). **Evidence:** `com.bank.docgen.contentmodule` — `ContentModuleController`, `ContentModuleService`, `ContentModuleReviewService`, `ContentModuleLifecycleService`, `ContentModuleAccessSupport`; `GroupAccessService` group scoping; `ManagementAuditRecorder` — 5 `recordContentModule*` events; i18n `api.error` keys in `messages_en.properties`; tests — `ContentModuleControllerTest` (6), `ContentModuleServiceTest` (16), `ContentModuleAccessSupportTest` (15), `ContentModuleReviewServiceTest` (8), `ContentModuleLifecycleServiceTest` (8) = **60** T01b tests; plus T01a `ContentModuleRepositoryTest` (7) = **61** contentmodule tests; gate `mvn -B -ntp -f backend/pom.xml verify` — **456** tests BUILD SUCCESS (2026-06-26) |
| P14-T01c | Template reference + impact analysis integration | Done (2026-06-26) | PRD §6.4.1 reference + lock rules; publish gate blocker «模块引用缺失» in `domain-model.md` §2.9.3; lifecycle `impactSummary` fields in [`content-module-lifecycle-operation-request.json`](../../api/examples/content-module-lifecycle-operation-request.json). **Evidence:** Flyway `V27__template_content_module_reference.sql`; `TemplateContentModuleReferenceEntity` / `TemplateContentModuleReferenceRepository`, `TemplateContentModuleReferenceService` (GET/PUT template content-module-references; publish locks references; `resolvePinnedContentStructures` for generation); `ContentModuleLifecycleImpactService` + `ContentModuleLifecycleImpactSummaryView` (GET lifecycle impact preview); `PublishGateCheckCode.CONTENT_MODULE_REFERENCES` in `PublishGateService`; `DocxAssembler` `contentModuleRef` node + pinned resolve wired in `DocumentGenerationEngine` / `PreviewGenerationService`; tests — `TemplateContentModuleReferenceRepositoryTest` (2), `TemplateContentModuleReferenceServiceTest` (10 incl. `resolvePinnedContentStructures`), `ContentModuleLifecycleImpactServiceTest` (1), `DocxAssemblerTest` `rendersPinnedContentModuleReferenceFromLockedVersion`, `PublishGateServiceTest` CONTENT_MODULE_REFERENCES case; gate `mvn -B -ntp -f backend/pom.xml verify` — **461** tests BUILD SUCCESS (+5 from 456 T01b baseline) |
| P14-T01d | Management UI (list, detail, lifecycle) | Done (2026-06-26) | [`permission-matrix.md` §5.1](../../security/permission-matrix.md#51-条款或内容模块权限矩阵); Bank OA list/detail + lifecycle dialogs. **Evidence:** `ContentModuleListView`, `ContentModuleDetailView`, `ContentModuleLifecycleImpactDialog`, `ContentModuleCreateDialog`, `ContentModuleVersionDialog`, `ContentModuleStatusBadge`, `TemplateContentModuleReferencesPanel`; `contentModules` store/API + `routeKeys` nav/breadcrumb; Vitest — `ContentModuleListView.test.ts` (2), `ContentModuleDetailView.test.ts` (2), `contentModules.test.ts` (4); gates `pnpm -C frontend lint` / `type-check` / `test` (**224**) / `build` green; backend `mvn -B -ntp -f backend/pom.xml verify` — **461** tests (T01a–c baseline) |
| P14-T01e | Architecture critical remediation | Done (2026-06-26) | Architecture review BLOCK follow-up — 4 Critical fixes per `permission-matrix.md` §5.1 + fail-closed. **Evidence:** `GroupAccessService.canBrowseContentModuleCatalog` / `canViewContentModuleStructure`; `ContentModuleService.assertCatalogBrowseAllowed` on list/get (TESTER 403); `ContentModuleVersionView.contentStructureJson` role-gated; list merges shared-into-group modules; `ContentModuleLifecycleImpactService` — `RuntimeGenerationAuditEventRepository` 7d call counts, `templateStopRequired`/`releaseStopRequired`; `ContentModuleLifecycleAuditDetail` + `ManagementAuditRecorder.recordContentModuleLifecycleOperation` impact persistence; tests — `ContentModuleServiceTest` (+3), `ContentModuleControllerTest` (+3), `ContentModuleLifecycleImpactServiceTest` (+1), `GroupAccessServiceTest` (+1); gate `mvn -B -ntp -f backend/pom.xml verify` — **469** tests BUILD SUCCESS (+8 from 461); architecture re-review **PASS** (2026-06-26) |

---

## P14-T02 Collaboration to-dos + timeout escalation

**Slice status:** **Done** (2026-06-27) — T02a work item entity + role-queue API, T02b timeout config API, T02c escalation scheduler (notification-only), T02d management UI (collaboration work items on Dashboard + admin timeout config), E2E **3/3** (`collaboration-todos.spec.ts`); backend verify **481** tests; frontend lint/type-check/test (**235**) / build green.

> **IA supersession (Confirmed 2026-07-21 / TM #153 — not a P14 reopen):** Reminder timing edit UI
> relocates off Dashboard Overview to **System settings** full page (GLOBAL default) + **Team settings**
> dialog on Groups/team surface (GROUP override). Collaboration to-do queues on Dashboard remain.
> API / notification-only semantics from T02b–T02c **unchanged**. Current IA:
> [reminder-timing-settings-ia.md](../../behavior/reminder-timing-settings-ia.md);
> [catalog-navigation-ux.md](../../product/catalog-navigation-ux.md). Historical T02d row below
> records what shipped in 2026-06-27.

### Behavior (confirmed)

- In-app to-do queues by role (author → submit/fix; tester → test queue; approver → approval
  queue; group admin → scoped escalation).
- Timeout thresholds configurable by GLOBAL/GROUP admin; escalation is **notification only**
  (no auto-decision, no proxy approval, no state change).
- To-do payload: non-sensitive summary only (no variable values, customer data, full content).
- **Current UI placement (post-#153 IA):** Global default on System settings page; group override via
  Team settings dialog — **not** Dashboard Overview config chrome.

### Acceptance scenarios

- **Given** template in TESTING, **When** tester logs in, **Then** workbench shows to-do item
  with template name, group, submitter, age.
- **Given** test to-do exceeds threshold, **When** escalation runs, **Then** group admin sees
  escalation item; template status unchanged; audit records escalation.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P14-T02a | To-do entity + query API by role queue | Done (2026-06-26) | Flyway `V28__collaboration_work_item.sql`; `CollaborationWorkItemEntity` / `CollaborationWorkItemRepository` / `CollaborationWorkItemService` / `CollaborationWorkItemController`; `GET /api/management/v1/collaboration-work-items` with role queue filtering; `CollaborationWorkItem*Test` **18**; `mvn -B -ntp -f backend/pom.xml verify` — **471** BUILD SUCCESS (2026-06-26) |
| P14-T02b | Timeout config API (global + group override) | Done (2026-06-26) | Flyway `V29__collaboration_timeout_config.sql`; `CollaborationTimeoutConfigEntity` / `CollaborationTimeoutConfigRepository` / `CollaborationTimeoutConfigService` / `CollaborationTimeoutConfigController`; GET/PUT `/api/management/v1/collaboration-timeout-config` (global + group override); `CollaborationTimeoutResolver`; `GroupAccessService.canMaintainCollaborationTimeoutConfig`; OpenAPI v1 extended; `CollaborationTimeoutConfigServiceTest` (6), `CollaborationTimeoutConfigControllerTest` (4), `GroupAccessServiceTest` collaborationTimeout (1) = **11** T02b tests; `mvn -B -ntp -f backend/pom.xml verify` — **471** BUILD SUCCESS (2026-06-26) |
| P14-T02c | Escalation scheduler (no state mutation) | Done (2026-06-26) | Flyway `V30__collaboration_work_item_escalation_source.sql`; `CollaborationEscalationService` / `CollaborationEscalationScheduler` / `CollaborationSchedulingConfig`; `source_work_item_id` dedup + `findOpenEscalationCandidates` / `existsOpenEscalationForSource`; notification-only escalation (no source state mutation); `ManagementAuditRecorder.recordCollaborationTimeoutEscalation`; `CollaborationEscalationServiceTest` (5), `CollaborationEscalationServiceDataJpaTest` (3), `CollaborationEscalationSchedulerTest` (1), `CollaborationWorkItemRepositoryTest` escalation queries (2) = **11** T02c tests; full `mvn -B -ntp -f backend/pom.xml verify` — **475** BUILD SUCCESS (2026-06-27; environment fix `TEMP`→`D:\temp`, no code change) |
| P14-T02d | UI: collaboration on Dashboard + admin config | Done (2026-06-27) | Collaboration work items on `DashboardView` tasks section (COR-T11); **historical:** `CollaborationTimeoutConfigPanel` on Dashboard (GLOBAL/GROUP admin) — **superseded for current IA by TM #153** (System settings page + Team settings dialog; panel removed from Overview); `collaboration` store/API/types + `useWorkflowTasks`; legacy workbench routes redirect to `/dashboard#tasks-section`; Vitest — `CollaborationTimeoutConfigPanel.test.ts`, `collaboration.test.ts`, `DashboardView.test.ts`, `useWorkflowTasks.test.ts`; gates green (2026-06-27); backend verify **475**→**481** |
| P14-T02 E2E | Collaboration to-do functional journeys | Done (2026-06-27) | `frontend/e2e/collaboration-todos.spec.ts` **3/3** Docker 4173; report `playwright-report/docker/index.html`. **Remediation:** backend — `BatchTestRunEntity` FK alignment, `CollaborationWorkItemWriter` submit-for-test upsert (`mvn verify` **481**); frontend — `roles.ts` `canAccessTesterWorkbench` / `canAccessApproverWorkbench` / `canAccessCollaborationEscalationWorkbench` / `canAccessLogicalRoute` workbench guards (`pnpm test` **235**) |

---

## P14-T03 Template export / import

**Slice status:** **Done** (2026-06-27) — T03a export service + endpoint, T03b import service + DRAFT landing, T03c management UI; OpenAPI v1 + `contract-outline.md` export/import contract; E2E **2/2** (`P14-T03-template-export-import.spec.ts`); backend verify **481** tests; frontend lint/type-check/test (**235+**) / build green.

### Behavior (confirmed)

- Export: approved template bundle (metadata, variables, bindings, rules refs, policy snapshot).
- Import: lands in **DRAFT**; must re-run test→approve→publish in target environment.
- Permissions: GLOBAL all; GROUP scoped; TEMPLATE_AUTHOR own templates only.

### Acceptance scenarios

- **Given** published template in dev, **When** GROUP admin exports, **Then** JSON/ZIP bundle
  downloads without secrets or runtime credentials.
- **Given** bundle imported to staging, **When** import completes, **Then** template status is
  DRAFT; lifecycle actions available per role.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P14-T03a | Export service + management endpoint | Done (2026-06-26) | `TemplateExportService` + GET export JSON/ZIP; `TemplateExport*Test` **15**; verify **443** |
| P14-T03b | Import service + validation + DRAFT landing | Done (2026-06-26) | `POST /templates/import`; `TemplateImport*Test` **13**; verify **456** |
| P14-T03c | UI export/import on template detail + admin bulk | Done (2026-06-27) | `TemplateExportActions`, `TemplateImportDialog`; **+14** Vitest; frontend **235+** tests |
| P14-T03 E2E | Template export/import functional journeys | Done (2026-06-27) | `frontend/e2e/P14-T03-template-export-import.spec.ts` **2/2** Docker 4173; JSON + ZIP export, staging import → DRAFT, no secrets in bundle; backend **481** verify |

---

## Exit criteria (phase)

- All P14-T01…T03 tasks Done with real persistence (not in-memory).
- Role-scoped UI for each domain; audit on mutating actions.
- Green gates: `mvn verify` + frontend lint/type-check/test/build.
- Permission-matrix + PRD cross-links updated; execution-sync-ledger evidence recorded.
