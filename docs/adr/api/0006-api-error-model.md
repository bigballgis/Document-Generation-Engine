---
id: ADR-0006
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
adrNumber: "0006"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/api/openapi-v1.yaml
---

# ADR 0006: API Error Model

## Status

Accepted

## Context

The dynamic API needs stable error semantics for callers, administrators, support teams, audit review, retry behavior, and batch failure handling. Earlier drafts left open whether errors should use a top-level code with a separate reason or detailed stable error codes.

The idempotency strategy also depends on every error clearly expressing whether retry is allowed.

## Decision

API errors use detailed stable `error.code` values plus `error.category` for grouping.

The v1 baseline uses these fixed `error.category` values: `AUTHENTICATION`, `AUTHORIZATION`, `VERSION_ROUTING`, `API_POLICY`, `IDEMPOTENCY`, `VALIDATION`, `TEMPLATE_CONTRACT`, `GENERATION`, `ENCRYPTION`, and `BATCH`.

The v1 baseline error code list covers authentication, authorization, version routing, API management policy, idempotency, validation, template contract, generation, encryption, batch, asynchronous task, and generated-document download scenarios. The complete per-code list, default `retryable` values, `messageKey` values, and English messages are maintained in the API contract draft.

Every API error response must include `error.retryable` with an explicit `true` or `false` value.

`error.message` uses an English business-readable message. `error.messageKey` is returned so callers can map messages to other languages.

`error.messageKey` uses the pattern `api.error.<category>.<camelCaseCode>`, for example `api.error.versionRouting.defaultRouteNotConfigured`.

English messages must be concise, business-readable, and safe: they must not expose API credentials, passwords, internal configuration, or unauthorized resource details.

`error.message` remains a generic safe English message at the `error.code` level. The same `error.code` must not return different `error.message` values for different business scenarios.

The v1 error response contract does not add `resolutionHint`, `developerMessage`, or similar hint fields. More specific business context is expressed through `fieldErrors[].message`, safe conflict summaries, or API contract examples.

Worked examples focus on high-value scenarios instead of providing one example for every error code. The focused examples cover authorization and AD Group issues, version/default routing, API management policy rejections, asynchronous task and generated-result expiry, generation and encryption failures, and batch item failures.

Field-level validation errors use dot paths with array indexes, such as `variables.customerName` and `items[0].variables.amount`.

Field-level `fieldErrors[].reason` values use the common enum set `REQUIRED`, `INVALID_TYPE`, `INVALID_FORMAT`, `OUT_OF_RANGE`, `TOO_LONG`, `TOO_SHORT`, `ENUM_NOT_ALLOWED`, `PATTERN_MISMATCH`, `RULE_FAILED`, `DUPLICATED`, and `UNKNOWN_FIELD`.

Batch partial failures use `result.batch.items[].error` as the primary place for per-item errors. Top-level `error` is reserved for whole-request or whole-batch failures. If a whole-batch failure must include per-item details, those details are nested under `error.items` to preserve the v1 JSON response envelope.

HTTP status codes express the broad response class while `error.code` remains the primary machine-readable field for precise failure handling.

Authentication errors return 401. Authorization errors return 403, except AD Group resolution failure returns 503 because it represents a temporary authorization dependency failure. API management policy rejections such as disallowed output format, output mode, batch limit, or encryption capability return 400.

Request structure, missing required fields, and format validation errors return 400. Encryption parameter errors, including weak passwords, missing required password fields, unsupported permission combinations, and inconsistent `enabled` usage, return `400 ENCRYPTION_PARAMETER_INVALID`. Template variable or business rule validation failures return 422.

Missing resources return 404. Expired download URLs, expired async tasks, and expired generated results return 410. Disabled, deprecated, unavailable, or unconfigured version/default/template states return 409. Async task cancellation attempts against terminal, expired, already cancelled, or otherwise non-cancellable tasks return 409 with `ASYNC_TASK_CANCELLATION_NOT_ALLOWED`.

Idempotency conflicts and non-retryable repeated submissions return 409. Temporary idempotency store unavailability returns 503.

Template contract failures, missing anchors, document generation failures, PDF conversion failures, encryption processing failures after valid parameters, and whole-batch processing failures return 500. Encryption processing failures use `500 ENCRYPTION_FAILED` with `retryable=true`. Temporary generation service unavailability returns 503. Generation timeouts return 504.

Asynchronous task acceptance returns 202. Asynchronous batch partial success returns 200, with per-item failures represented through `result.batch.items[].status` and `result.batch.items[].error` instead of a top-level `error`.

## Consequences

- Callers can branch on stable detailed error codes while still grouping behavior by category.
- Retry behavior is explicit and can be used by the idempotency strategy.
- Error messages remain business-readable for API integrations while allowing localization outside the core API contract.
- Error messages remain stable per error code, which avoids callers depending on scenario-specific prose.
- Focused worked examples keep the contract useful for integration troubleshooting without making the API contract a duplicate example for every baseline code.
- Non-cancellable async task cancellation attempts have a stable conflict error, so callers can distinguish task lookup or expiry from a task-state cancellation conflict.
- Batch partial success responses can keep each failed item close to its item identifier and item status.
- The API contract can now publish a stable v1 error code baseline, HTTP status mapping, and initial worked examples.
- Error responses align with the v1 JSON response envelope confirmed in ADR 0011.

## Alternatives Considered

- Use top-level `error.code` plus `error.reason`: rejected because callers would need to combine multiple fields to branch on stable failure causes.
- Return `retryable` only for retryable errors: rejected because omission would be ambiguous for callers and for idempotency handling.
- Return bilingual message fields: rejected because message localization is better handled through `messageKey` mapping.
- Vary `error.message` for the same `error.code` by business scenario: rejected because callers need stable code-level semantics and safe, predictable messages.
- Add `resolutionHint` or `developerMessage` fields in v1: rejected because current troubleshooting needs can be handled through field errors, safe summaries, audit IDs, and contract examples without expanding the response contract.
- Provide one worked example for every error code: rejected because focused high-value examples are easier to maintain and avoid duplicating the baseline error-code table.
- Use JSON Pointer for field paths: rejected in favor of dot paths with array indexes, which are more readable in examples and support tooling.
- Put all per-item errors in top-level `error.itemErrors`: rejected because partial batch responses should keep each item error near the corresponding item result.
- Return business errors with HTTP 200 only: rejected because callers and gateways need broad transport-level error semantics.
- Use HTTP 207 Multi-Status for asynchronous batch partial success: rejected because 200 with explicit item statuses and item errors is more compatible for common enterprise API clients.

## Related Documents

## OQ Closure Trace

- OQ-4（错误字段命名残留开放项）已于 2026-06-20 收敛。
- 回链位置： [API 契约说明 - 开放议题集中清单 / OQ-4 错误字段命名残留开放项收敛（已收敛）](../../api/contract-outline.md#开放议题集中清单)
- 收敛口径：依据本 ADR 的 `Decision` 与 `Consequences`，错误字段命名残留开放表述已在契约文档中关闭。

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)