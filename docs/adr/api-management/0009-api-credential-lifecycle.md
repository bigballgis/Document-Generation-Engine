---
id: ADR-0009
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- security
adrNumber: "0009"
topic: api-management
related:
	- docs/security/permission-matrix.md
	- docs/api/contract-outline.md
---

# ADR 0009: API Credential Lifecycle

## Status

Accepted

## Context

The dynamic API uses API credential plus AD Group as the confirmed dual authorization model. API credentials identify calling systems or applications, while the access account and its AD Groups provide the second authorization dimension.

Financial document generation requires credential lifecycle rules that avoid long-lived unmanaged secrets, support operational rotation, keep callers from retrieving results after a credential is revoked, and avoid exposing secrets through contract displays, management screens, logs, or audit records.

## Decision

An API credential is a caller-level identity. It can be authorized to multiple template APIs. A template API call still requires API credential authorization, AD Group authorization, and template-level authorization.

Global administrators can manage all API credentials. Group administrators can manage API credentials only within their authorized group scope.

When an API credential is created or rotated, the secret plaintext is displayed only once. The platform stores only an irreversible digest or fingerprint. Administrators cannot view the secret plaintext after creation or rotation.

API credentials must have an expiry date. The default expiry is 180 days. The maximum expiry is 365 days. Administrators can set a shorter expiry.

The confirmed API credential status set is `ACTIVE`, `EXPIRING_SOON`, `EXPIRED`, and `REVOKED`.

`EXPIRING_SOON` is used for the expiry reminder window. Rotation state is represented by the current secret and the retiring old secret during the rotation grace period; v1 does not add a credential-level `ROTATING` status.

During rotation, the new secret becomes usable immediately. The old secret remains usable for a 7-day grace period, then becomes invalid.

Credential revocation takes effect immediately. A revoked credential blocks all subsequent API operations, including new generation, async task query, async task cancellation, and generated document download.

Already accepted background generation tasks can continue to completion after credential revocation, but the caller cannot use the revoked credential to retrieve results.

Requests using an expired API credential return `401 API_CREDENTIAL_EXPIRED`. Requests using a revoked API credential return `401 API_CREDENTIAL_REVOKED`.

The platform reminds global administrators and the corresponding group administrators 30 days, 7 days, and 1 day before credential expiry. API callers do not receive proactive expiry reminders. API callers can view their own credential's non-sensitive status and expiry summary through the API contract or management UI.

API credential lifecycle audit covers creation, rotation, revocation, expiry, expiry reminders, and credential summary views. Audit records include actor, time, operation reason, management scope, status change, expiry time, credential identifier or fingerprint summary, and affected authorization scope. Audit records never include secret plaintext.

API credential expiry reminders are a lifecycle-specific notification rule. They do not change the baseline that other API management configuration changes do not proactively notify callers or administrators.

## Consequences

- Credentials cannot become unmanaged long-lived secrets because expiry is mandatory and capped.
- Callers can rotate credentials without immediate downtime because old secrets remain valid for a 7-day grace period.
- Revocation has a clear security boundary: no future API operation can use the revoked credential, including retrieval of already generated results.
- Secret plaintext exposure is minimized because the platform only shows it once and stores only irreversible summaries.
- Administrators receive expiry reminders before credentials stop working, while API callers rely on contract or management visibility instead of proactive reminders.
- Audit trails can support security review without exposing credential secrets.

## Alternatives Considered

- Do not force credential expiry: rejected because enterprise financial API credentials should not remain valid indefinitely by default.
- Use a fixed 90-day expiry for every credential: rejected because some integrations need operational flexibility while still staying within a maximum lifetime.
- Immediately invalidate the old secret during rotation: rejected because it creates unnecessary integration downtime risk.
- Model rotation as a credential-level `ROTATING` status: rejected because the grace-period state is more accurately represented at secret version level.
- Notify API callers directly before expiry: rejected for the confirmed baseline because callers can inspect their own non-sensitive credential status, while proactive reminders go to administrators.
- Allow administrators to view secret plaintext after creation: rejected because it increases secret exposure risk and weakens audit and operational controls.
- Delay credential revocation: rejected because revocation is a security control and should stop all future API access immediately.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)
- [API Management Template Scope ADR](0002-api-management-template-scope.md)
- [API Management Configuration Change Governance ADR](0007-api-management-change-governance.md)