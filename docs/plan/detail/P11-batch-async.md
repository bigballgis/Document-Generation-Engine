# P11 — Batch & Async Generation (Detailed Plan)

**Phase status:** Done | **Depends on:** P10

## Behavior goal

Runtime callers submit batch generation requests (sync or async task mode),
query task status, and cancel active tasks per OpenAPI v1.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P11-T01 | Sync batch generate (200) with item validation | Done |
| P11-T02 | Duplicate itemId rejection (ITEM_ID_DUPLICATED) | Done |
| P11-T03 | Async batch accept (202) + task query | Done |
| P11-T04 | Task cancel + CANCELLED / 409 rules | Done |

**Maps to:** P7-T04, M3-T01–T05

**Evidence:** `BatchGenerationService`, `AsyncBatchTaskRunner`, `AsyncBatchTaskDispatcher` (in-process + Kafka), `RuntimeTaskController`, Flyway V11, `TemplatePlatformSliceTest` batch/async scenarios.

**Kafka transport:** Set `ASYNC_TRANSPORT=kafka` with `docgen-kafka` running. Topic `generation.async-batch-task.v1`; DLT `generation.async-batch-task.v1.dlt`. Default remains in-process `@Async` for local dev without Kafka.
