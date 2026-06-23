---
id: ADR-0004
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
adrNumber: "0004"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/api/openapi-v1.yaml
---

# ADR 0004: API Idempotency Strategy

## Status

Accepted

## Context

The platform generates financial documents through dynamic APIs. API callers may retry requests after network timeouts, lost asynchronous acceptance responses, upstream retries, or batch submission uncertainty. Without a clear idempotency strategy, a retry can create duplicate documents, duplicate asynchronous tasks, or generate a document against a different release version after the default route target changes.

The API contract also needs to distinguish business traceability from duplicate-submission protection. `requestId` and `idempotencyKey` therefore serve different purposes.

## Decision

All document generation APIs require `requestId` and `idempotencyKey`, including single generation, batch generation, synchronous generation, and asynchronous generation. Asynchronous task query and download-address file retrieval do not require `idempotencyKey` because they do not create new documents.

`requestId` is used for caller-side business tracing. `idempotencyKey` is used for duplicate-submission detection. The two fields do not replace each other.

Batch generation uses a batch-level `idempotencyKey` to identify repeated submission of the whole batch. Each batch item must include `items[].itemId`, and `items[].itemId` must be unique within the same batch. `items[].itemId` is used for per-item response details, audit details, and failed-item location; it does not replace the batch-level `idempotencyKey`.

If a batch request contains duplicate `items[].itemId` values, the whole request fails validation with `400 ITEM_ID_DUPLICATED`. The platform does not create a batch or asynchronous task for that request.

The idempotency uniqueness scope is caller, environment, template, resolved release version, and `idempotencyKey`. Within that scope, the same `idempotencyKey` with different request semantics returns an idempotency conflict.

Idempotency records are retained for 7 days. After a record expires, the same `idempotencyKey` can be treated as a new request.

When a request is made through the default route, the idempotency record includes the resolved release version at the time of the original request. Although the uniqueness scope includes the resolved release version, the default route has an additional conflict guard: if the default route target later changes and a repeated submission hits an old idempotency record for the same caller, environment, and template, the platform returns an idempotency conflict instead of generating a document against the new default target version.

If the original request failed, repeated submission with the same `idempotencyKey` uses a bounded replay rule: only temporary system failures marked `retryable=true` may be automatically re-executed. Validation, authorization, and policy failures are not automatically re-executed and repeated submission returns the original failed result.

When a repeated submission with the same `idempotencyKey` hits a successful synchronous file-stream result, the platform replays the original file stream and core header metadata.

When a repeated submission with the same `idempotencyKey` hits a successful synchronous download-url result, the platform returns the original download URL when it is still valid. If the original URL has expired but the generated result is still retained, the repeated-submission response may issue a new download URL without generating a new document.

When a repeated submission with the same `idempotencyKey` hits a single asynchronous generation request, the platform returns the original task as a full task status object instead of returning only `taskId`.

For synchronous batch generation, if any item fails parameter validation or API management policy validation, the whole batch fails and no files are generated. The response returns per-item failure details. This non-retryable batch failure is recorded as an idempotency result, so repeated submission with the same `idempotencyKey` replays the failed result.

For asynchronous batch generation, retrying failed items after partial success uses a new batch and a new `idempotencyKey`. The retry batch submits only the failed items that need retry and links back to the original batch through `originalBatchId` or an equivalent correlation field. The original batch result is not extended or rewritten by the retry.

Idempotency response fields are expressed under the JSON response `metadata` object. Synchronous file-stream responses expose the same core idempotency metadata through response headers.

For generation APIs, `idempotencyKey` is echoed in both successful and error responses. File-stream success responses echo `idempotencyKey` through response headers.

The idempotency status enum values are `IDEMPOTENCY_NEW`, `IDEMPOTENCY_REPLAYED`, and `IDEMPOTENCY_CONFLICTED`.

Idempotency conflict responses may include a safe request-semantics difference summary in `error.idempotencyConflict`. The safe summary may include `conflictType`, `conflictFields`, `originalRequestAt`, `originalResolvedReleaseVersion`, `requestHash`, `variablesHash`, and `itemsHash`. The baseline `conflictType` values are `REQUEST_SEMANTICS_MISMATCH` and `DEFAULT_ROUTE_CHANGED`. The safe summary must not include old or new business variable values, encryption passwords, full request bodies, or sensitive configuration values.

After an idempotency record expires, reuse of the same `idempotencyKey` is treated as a new request. The API response does not expose historical reuse information. Audit records mark the reuse with `reusedExpiredIdempotencyKey`, `previousIdempotencyExpiredAt`, `previousRequestAt`, and `previousResolvedReleaseVersion`.

Batch audit records include `batchId`, `items[].itemId` or its summary, and `originalBatchId` or equivalent retry correlation when failed items are retried in a new batch.

## Consequences

- Callers get predictable retry behavior across single, batch, synchronous, and asynchronous generation.
- Default route changes cannot silently change the document version produced by a repeated request.
- API responses and audit logs need to expose enough idempotency context for troubleshooting.
- Repeated successful synchronous requests remain predictable: file-stream replay is supported, and download-url replay can return an existing or newly issued URL based on URL validity.
- Repeated asynchronous single-generation submissions remain predictable because idempotent replay returns full task status context rather than only a task identifier.
- Retry behavior for failed idempotent requests stays safe and deterministic by restricting automatic re-execution to temporary system failures.
- Callers can correlate generation success and failure flows consistently because `idempotencyKey` is echoed across both response outcomes.
- The platform must store enough request semantics to detect conflicts without storing sensitive values such as DOCX/PDF encryption passwords.
- Conflict diagnostics are useful to callers, but they remain safe summaries rather than request-value disclosure.
- Expired idempotency key reuse stays invisible to API callers while remaining traceable in audit.
- Required unique `items[].itemId` values make batch responses, failed-item retry, and audit investigation deterministic.
- Synchronous batch validation failures are replayable through idempotency, which avoids repeated validation churn and keeps caller retry behavior predictable.
- Failed-item retry creates a clear new batch history instead of mutating an already completed partial-success batch.

## Alternatives Considered

- Make `idempotencyKey` optional for all generation APIs: rejected because callers could still create duplicate documents during retries.
- Require `idempotencyKey` only for asynchronous and batch APIs: rejected because synchronous single generation can also be retried after client-side timeout.
- Reuse `requestId` for idempotency: rejected because business trace IDs and duplicate-submission keys have different lifecycles and uniqueness rules.
- Make `requestId` optional: rejected because caller-side business tracing is required for financial document generation support and auditability.
- Replay the original default-route result after the default target changes: rejected because the caller should be explicitly told that the stable route now points somewhere else and the repeated key is tied to an old routing decision.
- Generate against the new default target after route changes: rejected because it weakens idempotency and can produce a different document for the same key.
- Return an idempotency-expired error after 7 days: rejected because expired keys should be reusable after the confirmed retention period.
- Put idempotency response fields under `metadata`: accepted by ADR 0011 because the v1 JSON response envelope is `metadata` + `result` or `error`.
- Return no idempotency conflict detail: rejected because callers need enough safe context to distinguish request mismatch from default-route-change conflicts.
- Return full old/new request differences in conflict responses: rejected because request bodies can contain financial variables, passwords, or sensitive configuration values.
- Show expired key reuse in the API response: rejected because expired keys are intentionally reusable after the retention period and the marker is primarily for audit and support investigation.
- Make `items[].itemId` optional: rejected because batch failures and partial-success responses need stable caller-provided item identity.
- Allow duplicate `items[].itemId` values within a batch: rejected because duplicated item identity makes per-item errors, retry selection, and audit ambiguous.
- Retry failed asynchronous batch items inside the original batch: rejected because it would mutate an already completed partial-success result and complicate audit history.

## Related Documents

## OQ Closure Trace

- OQ-1（幂等未决点）已于 2026-06-16 收敛。
- 回链位置： [API 契约说明 - 开放议题集中清单 / OQ-1 幂等未决点收敛（已收敛）](../../api/contract-outline.md#开放议题集中清单)
- 收敛口径：以本 ADR 的 `Decision` 与 `Consequences` 为准，契约文档不再将 OQ-1 作为待决事项。

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)