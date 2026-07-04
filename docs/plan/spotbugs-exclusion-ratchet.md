# SpotBugs Exclusion Ratchet Plan (SOR-A05)

**Status:** Active — slice 0 complete (REC_CATCH_EXCEPTION removed); **slice 1 Done** (2026-07-04); **slice 2 Done** (2026-07-04); **slice 3 Done** (2026-07-04); **slice 4 first batch Done** (2026-07-04); **slice 5 Done** (2026-07-04).  
**Filter file:** `backend/config/spotbugs/exclude.xml`  
**Automated guard:** `SpotBugsExclusionRatchetTest` (Surefire)

## Baseline (2026-07-04, SOR-A05 slice 0)

| Metric | Value | Notes |
| --- | --- | --- |
| `<Match>` blocks | **3** | `BASELINE_MATCH_COUNT=3` in guard test |
| Deferred `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` | ~52 | −13 in slice 5; −17 in slice 4 first batch; −67 in slice 3; −2 in slice 2; −15 in slice 1; blanket exclude retained |
| Deferred `REC_CATCH_EXCEPTION` | **0** | Removed from exclude.xml; 11 call sites narrowed |
| Framework token exclusions | 1 block | `CT_CONSTRUCTOR_THROW,SE_BAD_FIELD` (Spring auth tokens) |
| Test-class exclusion | 1 block | `~.*Test$` (tests out of gate scope) |

**Banned patterns (must not reappear without a new ratchet slice + guard update):** `REC_CATCH_EXCEPTION`

## Cadence

| Slice | Target | Scope | Exit criteria |
| --- | --- | --- | --- |
| **0 (Done)** | `REC_CATCH_EXCEPTION` | 11 findings across storage, master, rendering, runtime, template | 0 SpotBugs REC_CATCH; pattern removed from exclude.xml |
| **1 (Done)** | `EI_EXPOSE_REP` — security-critical | `ManagementSessionClaims`, runtime credential views, JWT/session DTOs | −15 findings (8 classes); deferred ~166→~151; blanket exclude retained |
| **2 (Done)** | `EI_EXPOSE_REP` — persistence entities | JPA entities with collection getters | Module-by-module; JaCoCo unchanged |
| **3 (Done)** | `EI_EXPOSE_REP` — API views / records | MapStruct-generated + hand-written views | Reduce deferred count below 100 |
| **4+** | Remaining `EI_EXPOSE_REP` | Quarterly −20% of deferred count | Remove blanket `<Match>` when &lt; 10 remain |

## Slice 1 evidence (EI_EXPOSE_REP — security/session DTOs)

Defensive `List.copyOf` in record compact constructors (null-safe → `List.of()`):

| Class | Module | List fields hardened |
| --- | --- | --- |
| `ManagementSessionClaims` | `sharedkernel.security` | `roles`, `authorizedGroupCodes`, `visibleRoutes` |
| `ManagementSessionView` | `authorization.management.api` | `roles`, `authorizedGroupCodes`, `visibleRoutes` |
| `ManagementUserView` | `authorization.management.api` | `roles`, `authorizedGroupCodes` |
| `CreateUserRequest` | `authorization.management.api` | `roles`, `authorizedGroupCodes` |
| `UpdateUserRequest` | `authorization.management.api` | `roles`, `authorizedGroupCodes` |
| `PageView` | `authorization.management.api` | `content` |
| `RuntimeSessionClaims` | `runtime.security` | `callerAdGroups` |
| `ApiPolicySummaryView` | `runtime.api` | `allowedOutputFormats`, `allowedOutputModes` |

**Tests:** `SecuritySessionDtoImmutabilityTest` (7 cases) + `JwtTokenServiceTest` management-token round-trip immutability.

**Exit:** ~15 deferred EI findings addressed; `exclude.xml` blanket `EI_EXPOSE_REP` match unchanged (`BASELINE_MATCH_COUNT=3`).

## Slice 2 evidence (EI_EXPOSE_REP — persistence entities)

Full scan of `backend/src/main/java/**/persistence/*Entity.java` (**31** files): only **4** entities declare collection fields; **29** store collections as JSON strings or scalars (no EI risk on getters).

| Class | Module | Collection getter | Change |
| --- | --- | --- | --- |
| `MasterDocumentEntity` | `master.persistence` | `getAnchors()` | `List.copyOf(anchors)` |
| `MasterRevisionLineEntity` | `master.persistence` | `getAnchors()` | `List.copyOf(anchors)` |
| `ManagementUserEntity` | `authorization.management.persistence` | `getRoles()`, `getAuthorizedGroupCodes()` | **Verified** — already `Set.copyOf` (no change) |

Mutation remains via domain methods (`replaceAnchors`, `assignRoles`, `assignGroupScope`); no callers mutate via getters (grep-confirmed).

**Tests:** `PersistenceEntityCollectionImmutabilityTest` (3 cases).

**Exit:** ~2 deferred EI findings addressed (~151→~149); `exclude.xml` blanket `EI_EXPOSE_REP` match unchanged (`BASELINE_MATCH_COUNT=3`).

## Slice 3 evidence (EI_EXPOSE_REP — API views / records)

Shared utility **`com.bank.docgen.sharedkernel.api.DefensiveCopies`** (`copyList`, `copyStringList`, `copySet`, `copyMap`; null-safe → empty immutable defaults).

Hand-written API records under `**/api/**` with `List`/`Set`/`Map` components use compact-constructor defensive copies. Slice 1 DTOs migrated off per-file `copyStrings` helpers.

| Metric | Value |
| --- | --- |
| API types hardened | **67** Java files (`**/api/**` records + nested record bodies) |
| Collection components | **~82** fields (`List`/`Set`/`Map`) |
| Security claims migrated | `ManagementSessionClaims`, `RuntimeSessionClaims` → shared utility |
| Excluded (non-DTO) | `GlobalExceptionHandler`, `ErrorEnvelopeFactory` |

**Tests:** `ApiDtoImmutabilityTest` (**9** cases: runtime contract, template detail, master detail, batch result, API policy, audit event, content module, preview comparison, error detail) + existing `SecuritySessionDtoImmutabilityTest` (7).

**Exit:** ~67 deferred EI findings addressed (~149→~**82**); below 100 target; `exclude.xml` blanket `EI_EXPOSE_REP` match unchanged (`BASELINE_MATCH_COUNT=3`).

## Slice 4 first batch evidence (EI_EXPOSE_REP — domain / authoring / internal DTOs)

Extended **`DefensiveCopies`** with `copyBytes` (byte[] clone, null-safe) and `copyNestedList` (nested `List<List<T>>`).

| Class | Module | Components hardened |
| --- | --- | --- |
| `TableLoopRowDefinition` | `authoring.structured` | `cells` |
| `TableComponentRenderModel` | `authoring.structured` | `columns`, `headerRows`, `footerRows` |
| `StructuredContentValidationResult` | `authoring.structured` | `blockers`, `warnings` |
| `ReferenceNodeValidationResult` | `authoring.structured` | `attachmentLists` |
| `PasteCleaningSummary` | `authoring.structured` | `items` |
| `NumberingValidationResult` | `authoring.structured` | `sequence` |
| `MasterStyleCatalogEntry` | `authoring.structured` | `applicableNodeTypes` |
| `MasterStyleCatalog` | `authoring.structured` | `stylesByKey` |
| `AuditSearchPage` | `audit.persistence` | `content` |
| `PdfPageNumberStampPlan` | `rendering` | `sectionStartPages` |
| `StructuredContentImageResolver.ResolvedImage` | `rendering` | `bytes` |
| `SyncGenerateResult` | `runtime.api` | `artifactBytes`, `fidelityWarningCodes` |
| `DocumentGenerationEngine.GeneratedDocument` | `runtime.service` | `artifactBytes`, `fidelityWarningCodes` |
| `AdGroupResolverProperties` | `apimgmt.service` | `getAccountGroups()` deep copy |

Factory methods on authoring records delegate to compact constructors (canonical defensive copy path).

| Metric | Value |
| --- | --- |
| Types hardened | **14** classes |
| Collection / byte[] components | **~20** fields |
| Authoring `structured/*` records | **8** of **8** with collection components |

**Tests:** `DomainDtoImmutabilityTest` (**10** cases: authoring validation, style catalog, audit page, PDF stamp plan, resolved image bytes, sync generate bytes, table render model, generated document, paste cleaning summary, AD group properties).

**Exit:** ~17 deferred EI findings addressed (~82→~**65**); −20% target met; `exclude.xml` blanket `EI_EXPOSE_REP` match unchanged (`BASELINE_MATCH_COUNT=3`).

## Slice 5 evidence (EI_EXPOSE_REP — service / infrastructure / remaining authoring)

Extended **`DefensiveCopies`** with `copyStringStringMap` and `copyAccountGroupsMap` (refactored `AdGroupResolverProperties` getter/setter to shared helper).

| Class | Module | Components hardened |
| --- | --- | --- |
| `TemplateExportZipArtifact` | `template.service` | `content` (byte[]) |
| `CacheEntry` | `apimgmt.service` | `allowedGroups` |
| `EffectiveRiskPromptConfig` | `template.service` | `reasonCategories`, `riskPromptCopy` |
| `DocgenAsyncProperties` | `infrastructure.config` | `getKafka()` / `setKafka()` defensive copy |
| `AdGroupResolverProperties` | `apimgmt.service` | `setAccountGroups()` deep copy (getter migrated to `copyAccountGroupsMap`) |
| `PasteCleaningResult` | `authoring.structured` | `summary` re-wrap |
| `TableComponentValidationResult` | `authoring.structured` | `fidelity`, `renderModel` re-wrap |
| `StorageProperties` | `infrastructure.storage` | nested `minio` record copy |
| `LoginSession` | `authorization.management.service` | nested `ManagementSessionView` re-wrap |
| `BatchExecutionOutcome` | `runtime.service` | nested `BatchResultView` re-wrap |

| Metric | Value |
| --- | --- |
| Types hardened | **11** classes (incl. nested records) |
| Collection / byte[] / nested components | **~13** fields |
| Service nested records verified | `BatchExecutionService`, `ManagementAuthService`, `TemplateExportService`, `TemplateAdGroupAuthorizationCache`; stream-only artifacts (`MasterDownloadArtifact`, `PreviewDownloadArtifact`) unchanged |

**Tests:** `DomainDtoImmutabilityTest` (**19** cases; +9 slice 5).

**Exit:** ~13 deferred EI findings addressed (~65→~**52**); −20% target met; `exclude.xml` blanket `EI_EXPOSE_REP` match unchanged (`BASELINE_MATCH_COUNT=3`).

## Slice 0 evidence (REC_CATCH fixes)

Narrow catches with structured logging (fail-closed domain exceptions preserved):

- `MinioObjectStorage.exists` — MinIO declared exceptions
- `MasterDocumentService` — `IOException` on upload / anchor extraction
- `DocxEncryptionService`, `PdfEncryptionService`, `TestPdfConversionService` — `IOException` / `GeneralSecurityException`
- `PreviewArtifactDownloadService`, `PreviewGenerationService`, `DocumentGenerationEngine` — I/O or runtime paths
- `SubmitTestEligibilityService`, `CoverageComputationService` — `JsonProcessingException`

## Guard rules

1. `<Match>` count must equal `BASELINE_MATCH_COUNT` (currently **3**).
2. Banned patterns must not appear in exclude.xml.
3. Increasing `<Match>` count or re-adding a banned pattern requires updating this plan **and** `SpotBugsExclusionRatchetTest` in the same change set.

## Gate

```bash
mvn -B -ntp -f backend/pom.xml verify
```

SpotBugs must report **0** bugs with the tightened filter.

## Related

- OPT-B / B1: `docs/plan/optimization-plan.md`
- SOR-A05: `docs/plan/system-optimization-review-2026-07.md` §8
- Coverage ratchet (parallel): `docs/plan/coverage-ratchet-plan.md`
