# Slim R6-backend — TemplateController split + service shrink evidence

Branch: `feat/slim-r6-backend` (base `main` @ `dac7907`)
Date: 2026-07-13

## Approach

- **MUST**: Split monolithic `TemplateController` into sibling `@RestController` classes sharing `@RequestMapping("/api/management/v1/templates")` (same pattern as `TemplateExportController` / `TemplateImportController`); each owns its own `envelope()` helper; HTTP contract unchanged
- **SHOULD**: Package-private collaborators (`*Support`) in same packages — constructed by parent, not Spring beans
- Public constructors / method signatures unchanged on facades and controllers
- High-risk rendering core untouched (`StructuredContentDocxWriteSession`, `ConditionExpressionEvaluator`, `DocxAssembler`)

## LOC before / after (touched god-classes)

Non-blank lines (`Measure-Object -Line`):

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `TemplateController.java` | 418 | 151 | −267 |
| `CollaborationWorkItemWriter.java` | 379 | 195 | −184 |
| `RuntimeTemplateController.java` | 362 | 289 | −73 |
| `TemplateVersionLineService.java` | 352 | 260 | −92 |

## TemplateController split (new siblings)

| Controller | Role | LOC |
|------------|------|----:|
| `TemplateController.java` | CRUD, coverage, change-diff, publish-gate, release-versions | 151 |
| `TemplateLifecycleController.java` | Lifecycle + version governance endpoints | 157 |
| `TemplateAuthoringController.java` | Variables, bindings, rules, paste-clean, content-module refs | 180 |

All `Template*Controller` files are **&lt; 400** non-blank lines (max: `TemplateAuthoringController` @ 180).

## Extracted collaborators (new)

| Collaborator | Role | LOC |
|--------------|------|----:|
| `CollaborationWorkItemPersistSupport` | Create/refresh helpers for work-item upserts | 219 |
| `RuntimeTemplateSyncSupport` | Sync invocation record + HTTP response headers/body | 104 |
| `TemplateVersionLineViewSupport` | Summary enrichment, lifecycle mapping, release-detail overlay | 124 |

## Residuals

- `CollaborationWorkItemWriter` retains `actorSummary` (also used by `resolveOpenWorkItems`) and all public `@Transactional` upsert/resolve entry points
- `RuntimeTemplateController` still owns batch/async endpoints and audit recording orchestration
- `TemplateVersionLineService` retains transactional clone/abandon and version-line guard logic
- Existing sibling controllers unchanged (`TemplateExportController`, `TemplateImportController`, `TemplateVersionLineController`, `TemplateRiskPromptConfigController`)

## HTTP mapping sanity

- `TemplateController` HEAD → split trio (`TemplateController` + `TemplateAuthoringController` + `TemplateLifecycleController`): **31/31** `@*Mapping` annotations identical (paths + HTTP methods)
- `RuntimeTemplateController` mappings unchanged
- Authorization ratchet updated: `ManagementAuthorizationRegistry` registers the two new sibling controllers and splits primary-service maps accordingly

## Verify

- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1351, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
