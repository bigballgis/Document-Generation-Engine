# Slim R8-backend — peel near-line service residuals

Branch: `feat/slim-r8-backend` (base `main` @ `9df16f5`)
Date: 2026-07-13

## Approach

- Behavior-preserving package-private `*Support` collaborators in the same packages (constructed by parent, not Spring beans)
- Public constructors / method signatures unchanged on facades; `@Transactional` entry points stay on Spring services
- Soft budget: Java warn at 400 (already clear); push remaining near-line services toward **&lt;275** (prefer **&lt;260**)
- Skip field-heavy JPA entities (`ApiInvocationRecordEntity`, `ApiPolicyEntity`)

## LOC before / after (touched hotspots)

Non-blank lines (`Where-Object { $_.Trim() -ne '' }`):

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `BatchGenerationService.java` | 298 | 189 | −109 |
| `TemplateLifecycleService.java` | 297 | 251 | −46 |
| `TemplateService.java` | 296 | 256 | −40 |
| `RuntimeTemplateController.java` | 289 | 207 | −82 |
| `AuditQueryService.java` | 288 | 233 | −55 |
| `ChangeDiffService.java` | 288 | 100 | −188 |
| `TemplateBindingConfigurationService.java` | 283 | 208 | −75 |
| `TableComponentValidationSupport.java` | 281 | 177 | −104 |
| `InvocationRecordService.java` | 278 | 204 | −74 |
| `DemoFullFlowCatalogSeeder.java` | 279 | 151 | −128 |
| `ContentModuleService.java` | 275 | 182 | −93 |

## Extracted collaborators (new)

| Collaborator | Role |
|--------------|------|
| `ChangeDiffDimensionSupport` | Dimension diffs + fingerprint/rule helpers |
| `BatchGenerationOutcomeSupport` | Sync persist+audit+invocation; async accepted recording |
| `RuntimeTemplateBatchSupport` | Batch-generate sync/async mode branching |
| `TemplateLifecycleVersionSupport` | Per-version deactivate / restore governance |
| `TemplateMetadataMutationSupport` | Template create + metadata update |
| `AuditQueryLifecycleSupport` | Lifecycle audit query / export |
| `TemplateBindingMutationSupport` | Variable / binding / rules mutations |
| `TableComponentParseSupport` | Column / row / cell parse helpers |
| `InvocationRecordEntitySupport` | Root invocation entity construction |
| `ContentModuleCatalogSupport` | Catalog list + view mapping |
| `DemoFullFlowPublishSupport` | Demo full-flow configure / lifecycle / publish seed path |

Also densified: `RuntimeTemplateSyncSupport.auditRecordAndWrite`, `BatchAsyncTaskPersistenceSupport.createAcceptedTask` / `resolveSyncReplay`.

## Residuals

- Soft program warn (Java ≥400): none
- Target push (&lt;275): all listed near-line facades now under 260 except none of the MUST set remain ≥275
- Facades retain orchestration / public `@Transactional` entry points

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1351, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
- Note: `TemplateService` retains a `GroupAccessService` field for the management-authorization contract ratchet
