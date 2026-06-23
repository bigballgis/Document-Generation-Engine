---
id: ADR-0012
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
adrNumber: "0012"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/api/openapi-v1.yaml
---

# ADR 0012: API Enum and Identifier Naming

## Status

Accepted

## Context

The v1 dynamic API contract needs final enum names and identifier encoding rules before formal OpenAPI schemas and examples can be produced. Earlier drafts already used `UPPER_SNAKE_CASE` enum examples and readable template IDs such as `TPL-LOAN-NOTICE`, but the final naming rules were still pending.

Financial document APIs must also avoid leaking sensitive business information, generation volume, or timing through resource identifiers.

## Decision

v1 API enum values use English `UPPER_SNAKE_CASE`.

The confirmed output format enum values are `DOCX` and `PDF`.

The confirmed output mode enum values are `SYNC_STREAM`, `SYNC_DOWNLOAD_URL`, and `ASYNC_TASK`.

The confirmed route type enum values are `EXPLICIT_VERSION` and `DEFAULT_ROUTE`.

The confirmed async task status enum values are `ACCEPTED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `PARTIAL_SUCCEEDED`, `EXPIRED`, and `CANCELLED`.

The confirmed batch item status enum values are `SUCCEEDED`, `FAILED`, and `SKIPPED`.

The confirmed v1 encryption permission enum values are `ALLOW_PRINT`, `ALLOW_COPY`, `ALLOW_EDIT`, `ALLOW_ANNOTATE`, and `ALLOW_FORM_FILL`. Opening or viewing a protected document is controlled by `openPassword`, not by the `permissions` enum.

`templateId` uses a readable stable template key: `TPL-` plus English letters, numbers, and hyphens. A released `templateId` is immutable. It must not contain customer, personal, account, amount, contract, or other sensitive business information.

`taskId`, `batchId`, and `documentId` are platform-generated resource IDs using `TASK-`, `BATCH-`, and `DOC-` prefixes followed by opaque random tokens. These tokens must not encode date, sequence number, template identity, customer identity, business variables, environment, or generation volume.

## Consequences

- API callers can use stable enum values in integrations and generated clients.
- Formal OpenAPI schemas can use concrete enum lists instead of placeholders for output, route, task, batch item, and encryption permission values.
- `templateId` remains human-readable enough for API contracts and operations, while sensitive business details remain excluded.
- Generated resource IDs are safe to return in API responses, logs, and audit records without exposing timing, sequence, or business meaning.
- Existing illustrative examples that used date or sequence-like document and batch IDs should be replaced with opaque-token examples.

## Alternatives Considered

- Use `lowerCamelCase` enum values: rejected because the existing error codes, status values, and contract examples already use `UPPER_SNAKE_CASE` consistently.
- Shorten output mode values to `STREAM`, `DOWNLOAD_URL`, and `ASYNC`: rejected because `SYNC_STREAM`, `SYNC_DOWNLOAD_URL`, and `ASYNC_TASK` state the execution model more clearly.
- Prefix batch item statuses separately, such as `ITEM_SUCCEEDED`: rejected because the context already makes item status clear and reusing `SUCCEEDED`, `FAILED`, and `SKIPPED` keeps responses simple.
- Use `DISALLOW_*` encryption permission values: rejected because the API field represents allowed capabilities, while unsupported combinations are rejected explicitly.
- Use fully opaque template IDs: rejected because authorized API callers and administrators benefit from stable readable template keys.
- Use date and sequence based generated resource IDs: rejected because they can reveal timing and approximate generation volume.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)