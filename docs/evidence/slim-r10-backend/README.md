# Slim R10-backend — peel remaining near-240 service residuals

Branch: `feat/slim-r10-backend` (base `main` @ `0c58a0c`)
Date: 2026-07-13

## Approach

- Behavior-preserving package-private `*Support` collaborators in the same packages (constructed by parent, not Spring beans)
- Public constructors / method signatures unchanged on Spring facades; `@Transactional` entry points stay on services
- Soft budget: Java warn at 400 (already clear); push remaining near-line services toward **&lt;220** where practical
- `DocxAssembler` light peel only (catalog load + filler + table/header/footer loops); OOXML replace semantics unchanged
- `CollaborationWorkItemRepository` default methods call shared query helpers; Spring Data `@Query` contracts unchanged
- `TemplateService` retains a `GroupAccessService` field for the management-authorization contract ratchet

## LOC before / after (touched hotspots)

Total lines (PowerShell `(Get-Content).Count`), including blanks:

| File | Before | After | Notes |
|------|-------:|------:|-------|
| `RuntimeGenerationAuditRecorder.java` | 260 | 169 | typed records → `RuntimeGenerationAuditRecordSupport` |
| `DocxAssembler.java` | 261 | 162 | light peel; catalog / filler / periphery loops |
| `TemplateService.java` | 268 | 197 | release / display / display / display-query supports |
| `AsyncBatchTaskRunner.java` | 254 | 120 | completion + JSON helpers |
| `UserManagementService.java` | 263 | 154 | access + view supports |
| `AsyncBatchTestOrchestrator.java` | 266 | 109 | execution support |
| `PublishGateCheckItemSupport.java` | 252 | 197 | content-item support |
| `ApiPolicyRollbackService.java` | 252 | 179 | snapshot support |
| `CollaborationWorkItemRepository.java` | 262 | 251 | wildcard / escalation helpers only |

## Extracted collaborators (new)

| Collaborator | Role |
|--------------|------|
| `RuntimeGenerationAuditRecordSupport` | Typed sync / batch / download audit record helpers |
| `DocxMasterStyleCatalogSupport` | Default master style catalog load |
| `DocxMasterLayoutFillerSupport` | Master-layout filler paragraph removal |
| `TemplateReleaseVersionListSupport` | Release-version listing + enrichment |
| `TemplateDisplayLookupSupport` | Display-info lookup by template ids |
| `TemplateReadQuerySupport` | Binding validate / style catalog / paste-clean reads |
| `AsyncBatchTaskCompletionSupport` | Async batch outcome / failure / invocation completion |
| `UserManagementAccessSupport` | Visibility / role / scope guards |
| `UserManagementViewSupport` | User view mapping + actor summaries |
| `AsyncBatchTestExecutionSupport` | Batch test sample loop / coverage / prune |
| `PublishGateCheckItemContentSupport` | Content-module / structured-node / paste / blocker items |
| `ApiPolicyRollbackSnapshotSupport` | Rollback snapshot JSON helpers |
| `CollaborationWorkItemQuerySupport` | Wildcard scope + escalation queue helpers |

Also densified: `DocxPlainAnchorParagraphSupport` / `DocxStructuredAnchorSupport` table+header+footer replace helpers; `BatchGenerationJsonSupport.readRequestPayload`.

## Residuals

Non-entity `*.java` under `backend/src/main/java` with **≥230** total lines:

| Lines | File |
|------:|------|
| 251 | `collaboration/persistence/CollaborationWorkItemRepository.java` |
| 248 | `rendering/service/PreviewGenerationService.java` |
| 245 | `template/web/TemplateVersionLineController.java` |
| 245 | `template/service/RiskPromptConfigService.java` |
| 243 | `sharedkernel/document/expression/ConditionExpressionParser.java` |
| 243 | `audit/service/AuditQueryService.java` |
| 241 | `runtime/service/RuntimeGenerationAuditPersistSupport.java` |
| 239 | `runtime/service/DocumentGenerationEngine.java` |
| 236 | `template/service/CoverageComputationService.java` |
| 231 | `rendering/StructuredContentDocxWriteSession.java` |
| 231 | `collaboration/service/CollaborationWorkItemPersistSupport.java` |
| 230 | `template/service/TemplateContentModuleReferenceService.java` |
| 230 | `template/service/ChangeDiffDimensionSupport.java` |
| 230 | `authoring/structured/MasterStyleCatalogService.java` |
| 230 | `apimgmt/service/ApiManagementService.java` |

- Soft program warn (Java ≥400): none
- Facades retain orchestration / public `@Transactional` entry points

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` → **BUILD SUCCESS** (Tests run: 1355, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)