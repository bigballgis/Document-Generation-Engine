# Slim R11-backend — peel remaining near-220 service residuals

Branch: `feat/slim-r11-backend` (base `main` @ `7effe3b`)
Date: 2026-07-13

## Approach

- Behavior-preserving package-private `*Support` collaborators in the same packages (constructed by parent, not Spring beans)
- Public constructors / method signatures unchanged on Spring facades; `@Transactional` entry points stay on services
- Soft budget: Java warn at 400 (already clear); push remaining near-line services toward **&lt;200** where practical
- Rendering-adjacent light peels only for preview / runtime DOCX+artifact assembly paths
- `AuditQueryService` retains a `GroupAccessService` field for the management-authorization contract ratchet

## LOC before / after (MUST hotspots)

Total lines (PowerShell `(Get-Content).Count`), including blanks:

| File | Before | After | Notes |
|------|-------:|------:|-------|
| `PreviewGenerationService.java` | 248 | 196 | assembly → `PreviewGenerationAssemblySupport` |
| `AuditQueryService.java` | 243 | 143 | management/generation → `AuditQueryManagementSupport` |
| `DocumentGenerationEngine.java` | 239 | 145 | assemble/store → `DocumentGenerationAssemblySupport` |
| `RuntimeGenerationAuditPersistSupport.java` | 241 | 188 | special persists → `RuntimeGenerationAuditSpecialPersistSupport` |

## Extracted collaborators (new)

| Collaborator | Role |
|--------------|------|
| `PreviewGenerationAssemblySupport` | Preview DOCX assemble + PDF finalize/store |
| `AuditQueryManagementSupport` | Management / generation audit query + export bodies |
| `DocumentGenerationAssemblySupport` | Runtime DOCX assembly + artifact finalize/store |
| `RuntimeGenerationAuditSpecialPersistSupport` | Batch-async-from-task + rate-limit-denied persists |

## Residuals

Non-entity `*.java` under `backend/src/main/java` with **≥220** total lines:

| Lines | File |
|------:|------|
| 251 | `collaboration/persistence/CollaborationWorkItemRepository.java` |
| 245 | `template/web/TemplateVersionLineController.java` |
| 245 | `template/service/RiskPromptConfigService.java` |
| 243 | `sharedkernel/document/expression/ConditionExpressionParser.java` |
| 236 | `template/service/CoverageComputationService.java` |
| 231 | `collaboration/service/CollaborationWorkItemPersistSupport.java` |
| 231 | `rendering/StructuredContentDocxWriteSession.java` |
| 230 | `template/service/TemplateContentModuleReferenceService.java` |
| 230 | `template/service/ChangeDiffDimensionSupport.java` |
| 230 | `authoring/structured/MasterStyleCatalogService.java` |
| 230 | `apimgmt/service/ApiManagementService.java` |
| 227 | `template/service/TemplateVersionLineService.java` |
| 225 | `contentmodule/service/ContentModuleLifecycleService.java` |
| 223 | `apimgmt/service/ApiPolicyDomainSaveSupport.java` |
| 221 | `master/service/MasterRevisionLineService.java` |
| 221 | `audit/service/ManagementAuditRecorder.java` |
| 221 | `template/service/TemplateBindingConfigurationService.java` |
| 221 | `runtime/security/ApiCredentialAuthenticationFilter.java` |
| 220 | `runtime/web/RuntimeTemplateController.java` |

- Soft program warn (Java ≥400): none
- Facades retain orchestration / public `@Transactional` entry points
- All four MUST hotspots now **&lt;200**

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` → **BUILD SUCCESS** (Tests run: 1355, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
