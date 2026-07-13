# Slim R9-backend — peel remaining near-250 service residuals

Branch: `feat/slim-r9-backend` (base `main` @ `cf02c24`)
Date: 2026-07-13

## Approach

- Behavior-preserving package-private `*Support` collaborators in the same packages (constructed by parent, not Spring beans)
- `ApiManagementController` split into sibling `@RestController`s sharing the same `/api/management/v1/templates/{templateId}/api` prefix; envelope helpers in `ApiManagementWebEnvelopeSupport`
- Public constructors / method signatures unchanged on Spring facades; `@Transactional` entry points stay on services
- Soft budget: Java warn at 400 (already clear); push remaining near-line services toward **&lt;250** where practical
- `ManagementAuthorizationRegistry` updated for the three ApiManagement sibling controllers (class-name registry + primary services); `RouteVisibilityService` remains route-key based (no change)

## LOC before / after (MUST hotspots)

Total lines (PowerShell `(Get-Content).Count`), including blanks:

| File / split | Before | After | Notes |
|--------------|-------:|------:|-------|
| `ApiManagementController.java` | 299 | — | **deleted**; split below |
| → `ApiManagementPolicyController.java` | — | 189 | policy / contract / routes / rollback |
| → `ApiManagementInvocationController.java` | — | 94 | invocations |
| → `ApiManagementCredentialController.java` | — | 81 | credentials |
| → `ApiManagementWebEnvelopeSupport.java` | — | 24 | shared envelopes |
| `TemplateImportService.java` | 293 | 128 | |
| `RuntimeGenerationService.java` | 281 | 185 | |
| `ApiPolicyDomainSaveSupport.java` | 274 | 223 | + `ApiPolicyDomainSaveExecutorSupport` |
| `StructuredContentDocxWriteSession.java` | 273 | 231 | |
| `TemplateVersionLineService.java` | 287 | 227 | |
| `TemplateService.java` | 282 | 268 | still ≥250 residual |
| `AuditEventViewMapper.java` | 269 | 142 | |
| `MasterDocumentService.java` | 267 | 186 | |
| `TemplateLifecycleService.java` | 266 | 219 | |

## Extracted collaborators (new)

| Collaborator | Role |
|--------------|------|
| `ApiManagementPolicyController` | Policy / contract / routes-summary / impact / rollback HTTP |
| `ApiManagementInvocationController` | Recent / filtered / detail invocation HTTP |
| `ApiManagementCredentialController` | Credential list / create / rotate / revoke HTTP |
| `ApiManagementWebEnvelopeSupport` | Success envelope + trace helpers |
| `ApiPolicyDomainSaveExecutorSupport` | Shared JSON write + default-route assert + persist helpers |
| `AuditManagementEventViewSupport` | Management audit event view mapping |
| `MasterDocumentFileMutationSupport` | Master file create / replace / download mutations |
| `StructuredContentDocxBlockDispatchSupport` | Structured block → DOCX dispatch |
| `RuntimeGenerateRequestSupport` | Runtime generate request preparation |
| `RuntimeGenerationIdempotencySupport` | Idempotency claim / release path |
| `TemplateImportApplySupport` | Import apply / persist path |
| `TemplateImportTargetResolutionSupport` | Import target template resolution |
| `TemplateInFlightContentMutationSupport` | In-flight template content mutations |
| `TemplateLifecycleApprovalFlowSupport` | Approval / publish flow orchestration peel |
| `TemplateVersionLineMutationSupport` | Version-line mutation helpers |

Also: `ApiPolicyDomainSaveSupport.writeJson` delegates to executor (keeps `ApiPolicyCommandSupport` callers compiling). `MasterDocumentService` / `TemplateLifecycleService` retain `GroupAccessService` fields for the management-authorization contract ratchet.

## Residuals

Non-entity `*.java` under `backend/src/main/java` with **≥250** total lines:

| Lines | File |
|------:|------|
| 268 | `template/service/TemplateService.java` |
| 266 | `rendering/service/AsyncBatchTestOrchestrator.java` |
| 263 | `authorization/management/service/UserManagementService.java` |
| 262 | `collaboration/persistence/CollaborationWorkItemRepository.java` |
| 261 | `rendering/DocxAssembler.java` |
| 260 | `runtime/service/RuntimeGenerationAuditRecorder.java` |
| 254 | `runtime/service/AsyncBatchTaskRunner.java` |
| 252 | `apimgmt/service/ApiPolicyRollbackService.java` |
| 252 | `template/service/PublishGateCheckItemSupport.java` |

- Soft program warn (Java ≥400): none
- Facades retain orchestration / public `@Transactional` entry points

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` → **BUILD SUCCESS** (Tests run: 1355, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
