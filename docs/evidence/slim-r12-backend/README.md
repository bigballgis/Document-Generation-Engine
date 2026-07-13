# Slim R12-backend — peel remaining near-200 service residuals

Branch: `feat/slim-r12-backend` (base `main` @ `cccf4f9`)
Date: 2026-07-13

## Approach

- Behavior-preserving package-private `*Support` collaborators in the same packages (constructed by parent, not Spring beans)
- Public constructors / method signatures unchanged on Spring facades; `@Transactional` entry points stay on services
- Soft budget: push remaining near-line services toward **&lt;190** where practical
- `StructuredContentDocxWriteSession` light peel only (list / module expand / heading helpers) — OOXML write path unchanged
- `ApiManagementService` retains a `GroupAccessService` field for the management-authorization contract ratchet

## LOC before / after (MUST hotspots)

Total lines (PowerShell `(Get-Content).Count`), including blanks:

| File | Before | After | Notes |
|------|-------:|------:|-------|
| `CollaborationWorkItemPersistSupport.java` | 231 | 142 | create/refresh → shared helpers |
| `RiskPromptConfigService.java` | 245 | 174 | JSON/views → `RiskPromptConfigMappingSupport` |
| `StructuredContentDocxWriteSession.java` | 231 | 168 | list/module/heading → `StructuredContentDocxExpandSupport` |
| `CoverageComputationService.java` | 236 | 134 | dimensions → `CoverageDimensionComputeSupport` |
| `ConditionExpressionParser.java` | 243 | 162 | lexer → `ConditionExpressionLexSupport` |
| `MasterStyleCatalogService.java` | 230 | 92 | validate/load → `MasterStyleCatalogValidationSupport` |
| `ApiManagementService.java` | 230 | 160 | contract/routes → `ApiManagementContractQuerySupport` |
| `ApiPolicyDomainSaveSupport.java` | 223 | 118 | preview/special → `ApiPolicyDomainPreviewSaveSupport` / `ApiPolicyDomainSpecialSaveSupport` |
| `TemplateContentModuleReferenceService.java` | 230 | 159 | resolve/validate → `TemplateContentModuleReferenceSupport` |

## SHOULD peel

| File | Before | After | Notes |
|------|-------:|------:|-------|
| `ChangeDiffDimensionSupport.java` | 230 | 147 | maps/fingerprint → `ChangeDiffDimensionHelperSupport` |

## Extracted collaborators (new)

| Collaborator | Role |
|--------------|------|
| `RiskPromptConfigMappingSupport` | Risk-prompt JSON + view mapping |
| `StructuredContentDocxExpandSupport` | List / content-module expand / section heading |
| `CoverageDimensionComputeSupport` | Per-dimension coverage calculations |
| `ConditionExpressionLexSupport` | Expression lexer primitives |
| `MasterStyleCatalogValidationSupport` | Catalog load + style/direct-format validation |
| `ApiManagementContractQuerySupport` | Caller contract + routes summary |
| `ApiPolicyDomainPreviewSaveSupport` | Preview / confirm / save orchestration |
| `ApiPolicyDomainSpecialSaveSupport` | Retention + default-route domain saves |
| `TemplateContentModuleReferenceSupport` | Reference resolve / validate / view |
| `ChangeDiffDimensionHelperSupport` | Change-diff maps / fingerprints |

## Residuals

Non-entity `*.java` under `backend/src/main/java` with **≥200** total lines:

| Lines | File |
|------:|------|
| 251 | `collaboration/persistence/CollaborationWorkItemRepository.java` |
| 245 | `template/web/TemplateVersionLineController.java` |
| 227 | `template/service/TemplateVersionLineService.java` |
| 225 | `contentmodule/service/ContentModuleLifecycleService.java` |
| 221 | `template/service/TemplateBindingConfigurationService.java` |
| 221 | `runtime/security/ApiCredentialAuthenticationFilter.java` |
| 221 | `master/service/MasterRevisionLineService.java` |
| 221 | `audit/service/ManagementAuditRecorder.java` |
| 220 | `runtime/web/RuntimeTemplateController.java` |
| 219 | `template/service/TemplateLifecycleService.java` |
| 216 | `runtime/service/RuntimeGenerationAuditRecordSupport.java` |
| 216 | `contentmodule/service/ContentModuleReviewService.java` |
| 214 | `template/service/TestDataSetService.java` |
| 213 | `runtime/service/InvocationRecordService.java` |
| 211 | `audit/service/ManagementAuditEventWriter.java` |
| 210 | `collaboration/service/CollaborationWorkItemWriter.java` |
| 209 | `runtime/service/InvocationQueryService.java` |
| 203 | `runtime/service/BatchGenerationService.java` |
| 201 | `runtime/service/InvocationRecordEntitySupport.java` |
| 201 | `authoring/structured/NodeMatrixValidationService.java` |
| 201 | `audit/service/SecurityManagementAuditRecorder.java` |
| 200 | `contentmodule/service/ContentModuleService.java` |
| 200 | `authoring/structured/NumberingService.java` |

- Soft program warn (Java ≥400): none
- All nine MUST hotspots now **&lt;190**

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` → **BUILD SUCCESS** (Tests run: 1355, Failures: 0, Errors: 0, Skipped: 7; Checkstyle/PMD/SpotBugs clean)
