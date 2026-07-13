# Slim R4-backend — TemplateService + ContractAssembly + RuntimeGenerationAudit shrink evidence

Branch: `feat/slim-r4-backend` (base `main` @ `5350d39`)
Date: 2026-07-13

## Approach

- Dead private methods: **none** (catalog/enrichment/view/persist helpers all had in-file call sites)
- Behavior-preserving package-private collaborators in the same service packages
- Public constructors / method signatures unchanged (helpers constructed internally)
- Catalog list filters, contract view assembly, and runtime generation audit persist semantics unchanged

## LOC before / after (touched god-classes)

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `TemplateService.java` | 466 | 341 | −125 |
| `ContractAssemblyService.java` | 451 | 59 | −392 |
| `RuntimeGenerationAuditRecorder.java` | 446 | 260 | −186 |

(ContractAssembly before includes sparse blank-line formatting already present on tree; logical content shrink is ~211→59.)

## Extracted collaborators (new)

| Collaborator | Role | LOC |
|--------------|------|----:|
| `TemplateCatalogSupport` | Catalog page/listAll + lifecycle/approval filter parse | 134 |
| `TemplateDisplayEnrichmentSupport` | Display-name enrichment for summaries + release versions | 70 |
| `ContractAssemblyViewSupport` | Paths / callable versions / default route / policy / error codes | 195 |
| `RuntimeGenerationAuditPersistSupport` | Persist helpers, batch-from-task, rate-limit, idempotency hash | 236 |

## Residuals

- `TemplateController` (~454) left untouched — already thin endpoint wrappers over services
- Facades still own orchestration / public `@Transactional` entry points
- High-risk rendering core untouched (`StructuredContentDocxWriteSession`, `ConditionExpressionEvaluator`, `DocxAssembler`)

## Verify

- Targeted: `ContractAssemblyService*Test` + `RuntimeGenerationAuditRecorderTest` + `TemplateServiceCatalog*` + `ModuleBoundaryArchTest` — GREEN (26 tests)
- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1347, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
