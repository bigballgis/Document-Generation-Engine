---
id: ADR-0010
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- security
adrNumber: "0010"
topic: authorization-security
related:
	- docs/security/permission-matrix.md
	- docs/domain/domain-model.md
	- docs/architecture/security-view.md
---

# ADR 0010: AD Group Authorization Resolution

## Status

Accepted

## Context

The dynamic API uses API credential plus AD Group as the confirmed dual authorization model. API credentials identify calling systems or applications, while AD Group membership comes from the actual access account associated with the request.

AD Group authorization must be predictable across generation, task query, cancellation, download, and contract-viewing operations. The platform also needs a clear position on directory outages, caching, external directory synchronization delay, and API management configuration changes.

## Decision

AD Group resolution rules apply to every API operation that requires AD Group authorization, including single generation, batch generation, async task query, async task cancellation, generated document download, API contract viewing, and callable version listing.

Successful AD Group resolution results are cached by `accessAccount` and `environment` for 5 minutes.

AD Group resolution failures are not cached.

If AD Group resolution fails and a non-expired cached result exists, the platform can use the non-expired cached result for authorization.

If AD Group resolution fails and no non-expired cached result exists, the request fails closed with `503 AD_GROUP_RESOLUTION_FAILED` and `retryable=true`.

Expired AD Group cache entries must not be used as an authorization fallback.

AD Group authorization configuration changes in API management take effect immediately and clear related authorization caches. They do not wait for the 5-minute cache TTL to expire naturally.

Directory membership changes take effect after the external directory has synchronized and the platform cache has expired. The platform must explain in the API contract or management UI that there can be up to 5 minutes of platform cache delay and that external directory synchronization delay is outside the platform's control.

AD Group resolution, cache hits, cache expiry, resolution failures, and authorization denials are recorded as audit summaries.

Logs, audit records, API contract displays, and management screens must not expose complete AD Group membership lists or unauthorized group details.

## Consequences

- Authorization stays fail-closed when there is no current trusted group data.
- Short-lived successful-resolution caching reduces dependency load without allowing stale cache authorization.
- API management AD Group configuration changes have immediate effect because related caches are cleared.
- Directory membership changes can still have bounded platform cache delay plus external directory synchronization delay.
- Callers can retry `AD_GROUP_RESOLUTION_FAILED` because it represents a temporary authorization dependency failure.
- Audit records can support authorization troubleshooting without exposing complete group membership or unauthorized group details.

## Alternatives Considered

- Never cache AD Group resolution: rejected because every API call would depend on real-time directory availability and increase dependency load.
- Cache AD Group resolution failures: rejected because transient directory failures should not poison subsequent authorization attempts.
- Use expired cache during directory outages: rejected because stale authorization data can preserve access after a membership or authorization change.
- Let AD Group API management configuration changes wait for cache TTL expiry: rejected because administrator-controlled authorization changes must take effect immediately.
- Treat AD Group resolution failure as 403: rejected because it is a temporary dependency failure, not a confirmed authorization denial.
- Proactively notify callers for AD Group membership changes: rejected because directory membership changes are external to the platform; visibility is handled through contract and management explanations plus audit.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)
- [API Error Model ADR](../api/0006-api-error-model.md)
- [API Management Configuration Change Governance ADR](../api-management/0007-api-management-change-governance.md)
- [API Credential Lifecycle ADR](../api-management/0009-api-credential-lifecycle.md)