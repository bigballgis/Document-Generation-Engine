---
id: ADR-0013
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- audit
adrNumber: "0013"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/security/permission-matrix.md
---

# ADR 0013: API Contract Visibility, Audit Summary, and Context Fields

## Status

Accepted

## Context

The v1 dynamic API contract already has confirmed routing, schema format, response envelope, error model, idempotency, async task lifecycle, encryption rules, enum values, and identifier naming. The remaining contract usability questions were how API management policy is displayed, how audit summaries are shaped, what `context` fields callers may send, whether async accepted responses return a task query entry point, and whether v1 needs release-version-level API management overrides.

Financial document generation requires strong visibility for callers and administrators, but contract displays, logs, and audit records must not expose API secrets, encryption passwords, complete AD Group membership, unauthorized group details, full request bodies, document variables, or other sensitive values.

## Decision

The confirmed v1 API policy display fields are:

- `apiPolicy.policyVersion`
- `apiPolicy.updatedAt`
- `apiPolicy.updatedBy`
- `apiPolicy.allowedOutputFormats`
- `apiPolicy.allowedOutputModes`
- `apiPolicy.batchLimits.syncMaxItems`
- `apiPolicy.batchLimits.asyncMaxItems`
- `apiPolicy.encryptionCapabilities`
- `apiPolicy.adGroupAuthorizationSummary`
- `apiPolicy.credentialSummary`

API policy displays must not expose API credential secrets, complete AD Group membership, unauthorized group details, encryption passwords, historical ciphertext, or sensitive configuration plaintext.

v1 does not introduce an independent developer portal. The background API contract page caller view is the v1 contract entry point for authorized API callers.

The caller view shows authorized templates only and includes page-computed, non-sensitive contract version comparison; error-code reference; request and response examples; callable release-version list; API policy summary; the caller's own non-sensitive API credential status; fidelity warning code reference; field descriptions; synchronous file-stream response headers; and non-sensitive fidelity warning summaries with `traceId` or `auditId` location identifiers.

Contract version comparison is computed by the caller view from already authorized contract data, callable versions, request schemas, response schemas, error codes, API policy summaries, route/default target information, and examples. v1 does not add a dedicated `contractVersionComparison` or equivalent field to `ContractResponse`.

API callers cannot self-create, rotate, or revoke API credentials in v1. They may only view their own non-sensitive credential status, expiry, and fingerprint summary. API credential creation, rotation, and revocation remain administrator operations.

API calls and API management configuration changes use a standard audit summary object. The baseline fields include `auditId`, `eventType`, `eventAt`, actor or system-subject summary, API credential or fingerprint summary, access account, environment, template, release version, resolved release version, route type, `requestId`, idempotency key summary, idempotency status, `taskId`, `batchId`, `itemId` (or their safe summaries), `contextSummary`, output summary, encryption summary, batch summary, resource IDs, result summary, error summary, duration, and configuration-difference summary.

Standard audit summaries must not record template variable raw values, encryption passwords, complete request bodies, API credential secrets, full download URLs, complete AD Group membership, unauthorized group details, historical ciphertext, or sensitive configuration plaintext.

ADR 0016 confirms that API management configuration changes use `eventType=API_POLICY_UPDATED`, include `changedAreas`, and record `policyVersion` for contract, audit, preview, and rollback correlation.

The v1 `context` object uses a strict safe whitelist. Allowed fields are `sourceSystem`, `channel`, `businessRequestId`, `upstreamTraceId`, `scenario`, and `locale`; values are strings. Unknown `context` fields return `400 REQUEST_BODY_INVALID`. `context` must not contain customer names, identity document numbers, account numbers, amounts, passwords, template variable raw values, complete request bodies, API secrets, full download URLs, or complete AD Group membership.

Asynchronous accepted responses return `task.queryPath`, a relative task-query path. `task.queryPath` is not an unauthenticated or signed URL; later task query calls still require API credential, AD Group, and template-level authorization.

v1 does not provide release-version-level API management configuration overrides. Template-level API management configuration remains the only confirmed baseline and applies to all non-disabled release versions under the template.

## Consequences

- API callers can inspect usable policy fields without seeing sensitive management data.
- API callers get one contract entry point without adding a separate developer portal surface.
- API callers can compare contract versions, inspect examples and errors, and understand callable versions without receiving credential-management permissions.
- Administrators and template orchestration users have stable field names for policy visibility before formal OpenAPI output is produced.
- Audit records become consistent across generation calls and API management configuration changes.
- `context` stays useful for tracing while avoiding misuse as a channel for sensitive business data or template variables.
- Async integrations can discover the task query path directly from the accepted response without receiving any additional access capability.
- v1 avoids the operational complexity of policy overrides at release-version level.

## Alternatives Considered

- Keep API policy display fields semantic only until OpenAPI generation: rejected because callers and administrators need stable visibility fields before formal schema output.
- Build an independent developer portal in v1: rejected because the confirmed background caller view covers the required contract, example, error, callable-version, policy, and credential-status needs without adding another access surface.
- Allow API caller credential self-service in v1: rejected because credential creation, rotation, and revocation remain administrator-governed operations with one-time secret handling and audit requirements.
- Allow free-form `context`: rejected because strict request validation and a safe whitelist better fit financial document generation.
- Return only `taskId` for async accepted responses: rejected because returning a relative query path improves integration ergonomics without changing authorization.
- Return a signed or unauthenticated task status URL: rejected because task query must remain protected by API credential, AD Group, and template-level authorization.
- Add release-version-level API management overrides in v1: rejected because it would add governance complexity beyond the confirmed template-level policy baseline.

## Related Documents

## OQ Closure Trace

- OQ-2（审计映射未决点）已于 2026-06-16 收敛。
- 回链位置： [API 契约说明 - 开放议题集中清单 / OQ-2 审计映射未决点收敛（已收敛）](../../api/contract-outline.md#开放议题集中清单)
- 关联说明：API 管理配置变更审计采用 `eventType=API_POLICY_UPDATED`，并以 `policyVersion` 进行契约-审计关联，详见 ADR 0016 `Decision`。

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)
- [API Management Template Scope ADR](../api-management/0002-api-management-template-scope.md)
- [API Management Change Governance ADR](../api-management/0007-api-management-change-governance.md)
- [API Async Task Lifecycle ADR](../async-processing/0008-api-async-task-lifecycle.md)
- [API Schema and Response Envelope ADR](0011-api-schema-and-response-envelope.md)