---
id: ADR-0007
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- audit
adrNumber: "0007"
topic: api-management
related:
	- docs/product/PRD.md
	- docs/security/permission-matrix.md
	- docs/api/contract-outline.md
---

# ADR 0007: API Management Configuration Change Governance

## Status

Accepted

## Context

API management configuration is attached to a template and applies to all non-disabled release versions under that template. Confirmed configuration areas include AD Group authorization, output mode, batch limits, DOCX/PDF dynamic encryption capability, and the default route target release version.

The default route target already has dedicated governance because changing it can shift which release version is served by a stable path. The remaining API management configuration areas still need consistent rules for contract display, impact preview, effective timing, notifications, rollback, and audit.

API credential lifecycle, rotation, revocation, expiry, expiry reminders, and related audit behavior are decided separately by ADR 0009 and are not governed by this ADR's configuration-change rules.

## Decision

AD Group authorization, output mode, batch limit, and DOCX/PDF dynamic encryption capability changes require impact preview before confirmation.

The API contract displays API management configuration by role. Authorized API callers see the current usable policy summary for templates they can access. Administrators and template orchestration users can see management configuration details, current status, last update time, last operator, impact preview, and audit entry points.

The confirmed v1 API policy display fields are `apiPolicy.policyVersion`, `apiPolicy.updatedAt`, `apiPolicy.updatedBy`, `apiPolicy.allowedOutputFormats`, `apiPolicy.allowedOutputModes`, `apiPolicy.batchLimits.syncMaxItems`, `apiPolicy.batchLimits.asyncMaxItems`, `apiPolicy.encryptionCapabilities`, `apiPolicy.adGroupAuthorizationSummary`, and `apiPolicy.credentialSummary`.

ADR 0016 extends this governance baseline with the template-level API management page, configuration-area independent saves, `policyVersion`, hard blockers and warnings in impact preview, and the `API_POLICY_UPDATED` audit event with `changedAreas`.

API contract displays, management views, logs, and audit records must not expose API credential secrets, complete AD Group membership, unauthorized group details, encryption passwords, historical ciphertext, or other sensitive values.

Changes to these API management configuration areas are immediate-only. Future scheduled activation, pending configuration changes, and pending-change cancellation are not supported in the confirmed baseline.

AD Group authorization configuration changes also clear related authorization caches according to ADR 0010.

These configuration changes do not proactively notify callers or administrators. Traceability is provided through contract visibility and audit records.

Impact preview includes the current and candidate configuration difference, affected template and non-disabled release versions, authorized caller or AD Group scope summary, recent usage summary, likely rejected output-mode, batch-limit, or encryption requests, and expected error-code impact.

Rollback is handled as a new controlled change. An administrator selects a historical configuration as the candidate configuration, reviews the impact preview, confirms the change, and the rollback takes effect immediately with an audit record. Rollback does not proactively notify callers or administrators.

The default route target release version continues to follow its dedicated route-governance rules. API credential lifecycle governance follows ADR 0009. AD Group authorization resolution and cache behavior follow ADR 0010.

## Consequences

- API callers can inspect the current usable policy summary without seeing sensitive management details.
- Administrators must review impact before changing authorization, output, batch, or encryption policy because one template-level change can affect all non-disabled release versions.
- Operational behavior stays simple because configuration changes and rollbacks are immediate-only and have no scheduled or pending state.
- Caller-facing notifications are not required for these configuration changes; contract visibility and audit records are the confirmed traceability mechanisms.
- API credential lifecycle decisions remain separate from these API management configuration-change rules and follow ADR 0009.

## Alternatives Considered

- Show full API management configuration to every contract viewer: rejected because API callers do not need secrets, full AD Group membership, or sensitive management details.
- Hide all API management configuration from API callers: rejected because callers need a usable policy summary to understand allowed output modes, batch limits, and encryption capability.
- Require impact preview only for high-risk changes: rejected because all confirmed configuration areas can affect callable behavior across non-disabled release versions.
- Support scheduled or pending API management changes: rejected for the current baseline because immediate-only changes keep state and audit easier to reason about.
- Proactively notify callers or administrators after every configuration change: rejected because contract visibility and audit records are the confirmed traceability baseline.
- Treat rollback as a special one-click operation outside normal governance: rejected because rollback can also change callable behavior and should use the same preview and audit controls as any other change.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)
- [API Management Template Scope ADR](0002-api-management-template-scope.md)
- [API Routing and Batch Overrides ADR](../api/0003-api-routing-and-batch-overrides.md)
- [API Credential Lifecycle ADR](0009-api-credential-lifecycle.md)
- [AD Group Authorization Resolution ADR](../authorization-security/0010-ad-group-authorization-resolution.md)