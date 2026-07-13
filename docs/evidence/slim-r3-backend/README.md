# Slim R3-backend — InvocationRecord + BatchGeneration shrink evidence

Branch: `feat/slim-r3-backend` (base `main` @ `9131486`)
Date: 2026-07-12

## Approach

- Dead private methods: **none** (every private helper had in-file call sites)
- Behavior-preserving package-private collaborators in the same `runtime.service` package
- Public constructors / method signatures unchanged (helpers constructed internally)
- Invocation / batch semantics unchanged (status mapping, retention, idempotent replay, sync/async mode gates, silent async complete-when-missing)

## LOC before / after (touched god-classes)

| File | Before | After | Delta |
|------|-------:|------:|------:|
| `InvocationRecordService.java` | 468 | 287 | −181 |
| `BatchGenerationService.java` | 511 | 311 | −200 |

## Extracted collaborators (new)

| Collaborator | Role | LOC |
|--------------|------|----:|
| `InvocationStatusMappingSupport` | Pure status maps (outcome / item / batch-root / task) | 54 |
| `InvocationRecordMetadataSupport` | Live-root lookup, idempotency link, retention expiry, INV- ids | 71 |
| `InvocationBatchItemPersistenceSupport` | BATCH_ITEM persist (sync + async-from-record) | 132 |
| `BatchGenerationJsonSupport` | Request/result JSON + policy string-list read | 68 |
| `BatchGenerationPolicySupport` | Policy / sync-async mode / request validation / version resolve | 109 |
| `BatchAsyncTaskPersistenceSupport` | Replay lookup, sync-task persist, expiry gate, task summary | 103 |

## Residuals

- Facades still own multi-step orchestration (audit + invocation recording + execute/dispatch)
- `TemplateService` (~466) / `TemplateController` (~454) skipped this wave (controller already thin; catalog extract deferred)
- `MasterDocumentService` / `ManagementAuditRecorder` not touched (already slimmed)
- High-risk rendering core untouched (`StructuredContentDocxWriteSession`, `ConditionExpressionEvaluator`)

## Verify

- Targeted: `InvocationRecordServiceTest` + `AsyncBatchTaskRunnerTest` + `RuntimeInvocationControllerTest` + `ModuleBoundaryArchTest` — GREEN (25 tests)
- Full: `mvn -B -ntp -f backend/pom.xml verify` — **BUILD SUCCESS** (Tests run: 1347, Failures: 0, Errors: 0, Skipped: 7; PMD/SpotBugs clean)
