---
id: ADR-0008
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- architecture
adrNumber: "0008"
topic: async-processing
related:
	- docs/api/contract-outline.md
	- docs/architecture/async-messaging-view.md
	- docs/architecture/runtime-view.md
---

# ADR 0008: API Async Task Lifecycle

## Status

Accepted

## Context

The dynamic API supports asynchronous single-document and batch document generation. Callers need a predictable way to query task state, understand progress, cancel long-running work, and handle cancellation failures.

Asynchronous batch generation can involve many records. A cancellation model that exposes partially generated financial documents could be confusing for callers and risky for downstream processing. Progress percentages can also be misleading when generation work has uneven item complexity, encryption, conversion, or retry behavior.

## Decision

Asynchronous task query uses `GET /api/{environment}/v1/templates/{templateId}/tasks/{taskId}`.

Task query may accept an optional `requestId` as an additional caller-side trace identifier. This value is audited for troubleshooting correlation only and is not used for task lookup or idempotency.

Asynchronous accepted responses return `task.queryPath`, a relative task-query path. It is not an unauthenticated or signed URL; later task query calls still perform API credential, AD Group, and template-level authorization checks.

Asynchronous task cancellation uses `POST /api/{environment}/v1/templates/{templateId}/tasks/{taskId}/cancel`.

Task query and task cancellation both execute API credential, AD Group, and template-level authorization checks. Task cancellation must be audited.

The confirmed task status set is `ACCEPTED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `PARTIAL_SUCCEEDED`, `EXPIRED`, and `CANCELLED`.

`PARTIAL_SUCCEEDED` is only used for asynchronous batch tasks. The API does not add a separate `QUEUED` state in v1; queued or created-but-not-started work is represented as `ACCEPTED`.

Only `ACCEPTED` or `PROCESSING` tasks that have not expired can be cancelled. Terminal, expired, already cancelled, or otherwise non-cancellable tasks return `409 ASYNC_TASK_CANCELLATION_NOT_ALLOWED`.

A successfully cancelled task reaches the final status `CANCELLED`.

Cancelled tasks do not return generated results, download URLs, or asynchronous batch per-item success results, even if some items had completed before cancellation. The platform may keep audit and operational cleanup records, but those records do not expose usable generated results to the API caller.

Task query does not return `progressPercent` or any other percentage field in v1. Single asynchronous tasks express progress through task status and task timestamps. Asynchronous batch tasks express progress through `batch.summary`, including total, processed, succeeded, failed, and skipped item counts.

## Consequences

- Callers get a clear cancellation operation without overloading task update semantics.
- Callers can distinguish task lookup or expiry from a task-state cancellation conflict by handling `ASYNC_TASK_CANCELLATION_NOT_ALLOWED`.
- The API avoids inaccurate progress percentages and uses batch counts where progress can be represented more reliably.
- Optional query-time `requestId` improves cross-system traceability without changing task identity semantics.
- Cancelled batch tasks do not expose partial generated documents, which reduces downstream ambiguity for financial document workflows.
- Callers that cancel a task and still need documents must submit a new generation request with a new valid idempotency context.
- The platform must audit cancellation attempts and ensure cancelled tasks do not expose result downloads.

## Alternatives Considered

- Do not support cancellation in v1: rejected because long-running asynchronous batch work needs an operational cancellation capability.
- Allow cancellation to return partial generated results: rejected because the confirmed behavior is to avoid exposing partial documents after cancellation.
- Allow cancellation of any task state: rejected because terminal and expired tasks should not move back into an active lifecycle.
- Use `PATCH /tasks/{taskId}` for cancellation: rejected because a dedicated cancellation subresource makes the command explicit and easier to authorize and audit.
- Add a separate `QUEUED` status: rejected because `ACCEPTED` is sufficient for v1 to represent created or queued work.
- Return `progressPercent`: rejected because percentages can be misleading for uneven document generation workloads; batch summary counts are more predictable.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)
- [API Response Delivery and Download Security ADR](../api/0005-api-response-delivery-and-download-security.md)
- [API Error Model ADR](../api/0006-api-error-model.md)