---
id: ADR-0011
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
adrNumber: "0011"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/api/openapi-v1.yaml
---

# ADR 0011: API Schema and Response Envelope

## Status

Accepted

## Context

The dynamic API contract has accumulated confirmed behavior for routing, idempotency, output modes, batch generation, async tasks, downloads, error handling, authorization, and DOCX/PDF encryption. The contract now needs stable field naming, a formal schema format, a request compatibility rule, and a consistent response envelope before a formal v1 API contract can be produced.

The project is still in requirements and design definition, so the schema format is a documentation and integration contract choice, not an implementation technology stack choice.

## Decision

The v1 request field naming baseline uses `lowerCamelCase` and confirms these fields: `output.format`, `output.mode`, `variables`, `encryption`, `requestId`, `idempotencyKey`, `items[].itemId`, and `context`.

The v1 `context` object uses a strict whitelist: `sourceSystem`, `channel`, `businessRequestId`, `upstreamTraceId`, `scenario`, and `locale`. Unknown `context` fields return `400 REQUEST_BODY_INVALID`.

`templateId` and `releaseVersion` are path parameters only. Generation request bodies must not repeat `templateId` or `releaseVersion`. Repeating them in the request body is treated as a request body field error.

The formal API contract schema will be maintained as OpenAPI 3.1 YAML. Markdown documents remain the place for explanation, navigation, decision context, examples, and open questions.

v1 request validation is strict. Fields outside the confirmed contract schema return `400 REQUEST_BODY_INVALID`. Field-level errors use the established field path and `fieldErrors[].reason` conventions.

JSON responses use a common envelope:

- `metadata` contains audit, trace, caller request, idempotency, template, route, release, and output summary metadata.
- `result` contains successful, accepted, partial-success, task, download, document, or batch result data.
- `error` contains whole-request or whole-batch failure details.

Synchronous file-stream responses are the exception: the response body contains only the file content, while core metadata is returned through response headers.

Idempotency response fields are placed under `metadata` for JSON responses and in response headers for synchronous file-stream responses.

Batch JSON responses return full per-item details in the same order as the request. Each input item has one response item with `itemId`, `status`, final `output`, encryption policy summary, and either successful result fields or an `error` object. Partial-success batch errors use `result.batch.items[].error`. Whole-batch failures that include per-item details place those details under `error.items`.

## Consequences

- API callers can rely on stable request field names before the formal v1 OpenAPI contract is generated.
- OpenAPI 3.1 YAML becomes the machine-readable contract source once the v1 contract is formalized, while Markdown remains the design narrative and index.
- Strict request validation improves integration feedback and avoids silent ignored fields in financial document generation flows.
- Response handling becomes consistent across sync-download, async, batch, and error scenarios.
- Moving idempotency fields into `metadata` supersedes the earlier top-level idempotency response placement from ADR 0004.
- Batch troubleshooting becomes deterministic because response items preserve request order and return one detail per input item.

## Alternatives Considered

- Keep field names as draft placeholders: rejected because downstream API contract work needs stable names.
- Allow `templateId` and `releaseVersion` to repeat in the request body: rejected because path parameters are already the routing source of truth and duplicate fields create mismatch handling complexity.
- Maintain schema only as Markdown tables: rejected because formal API integration needs a machine-readable contract format.
- Use separate JSON Schema files as the primary contract format: not selected for the baseline because OpenAPI can cover paths, operations, request/response schemas, errors, and examples in one contract.
- Silently ignore unknown request fields: rejected because callers could believe unsupported financial document parameters were applied.
- Put batch item details at the top level beside `metadata`, `result`, or `error`: rejected because it weakens the consistency of the response envelope.
- Return only failed batch items: rejected because callers need deterministic one-to-one request/response mapping for audit and retry selection.

## Related Documents

## OQ Closure Trace

- OQ-3（响应与批量字段命名残留开放项）已于 2026-06-20 收敛。
- 回链位置： [API 契约说明 - 开放议题集中清单 / OQ-3 响应与批量字段命名残留开放项收敛（已收敛）](../../api/contract-outline.md#开放议题集中清单)
- 收敛口径：依据本 ADR 的 `Decision` 与 `Consequences`，响应字段命名、批量字段命名与统一 envelope 的残留开放表述已在契约文档中关闭。

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)