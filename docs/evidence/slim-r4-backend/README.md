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
| `TemplateService.java` | 431 | 341 | −90 |
| `ContractAssemblyService.java` | 211 | 59 | −152 |
| `RuntimeGenerationAuditRecorder.java` | 426 | 260 | −166 |

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

- Targeted: `ContractAssemblyService*Test` + `RuntimeGenerationAuditRecorderTest` + `ModuleBoundaryArchTest` — GREEN (19 tests)
- Full: `mvn -B -ntp -f backend/pom.xml verify` — see commit message / CI for final counts
