---
id: ADR-authorization-0001
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0001"
topic: authorization
related:
  - docs/adr/authorization-security/0010-ad-group-authorization-resolution.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/authorization-security/0036-local-account-store-authorization-authority.md
  - docs/security/permission-matrix.md
---

# ADR authorization-0001: Management API Service-Layer Authorization

## Status

Accepted

## Context

The management UI uses `ManagementRoute` keys for navigation visibility and landing-route
selection. Backend management REST APIs under `/api/management/v1/**` must enforce access
independently of which routes the UI shows, using role and group-scoped capability checks.

## Decision

1. **`ManagementRoute` is UI-only.** Route keys (`route.dashboard-home`, etc.) drive
   frontend navigation and session metadata. They are **not** authorization primitives for
   API access control.
2. **API authorization lives in the service layer** via `GroupAccessService` capability
   methods (`canManageMasters`, `canPublishTemplates`, `canManageApiPolicy`, etc.) combined
   with group scoping (`canAccessGroup`, `accessibleGroupCodes`).
3. **Controllers stay thin.** Management controllers delegate to domain services; services
   invoke `GroupAccessService` before reads or mutations. Controllers do not reference
   `ManagementRoute` for permission decisions.
4. **Fail closed.** Missing capability or out-of-scope group access yields forbidden or
   not-found responses per the unified error model; never infer permission from visible
   routes alone.

## Consequences

- Positive: UI route refactors cannot silently widen or narrow API security; permissions
  stay aligned with the permission matrix and AD Group roles.
- Negative: New management endpoints require an explicit capability check in the owning
  service, not only a frontend route guard.
- Contract: `ManagementAuthorizationContractTest` guards that management services retain
  `GroupAccessService` delegation and controllers avoid `ManagementRoute` imports.

## Related Documents

- [Permission Matrix](../../security/permission-matrix.md)
- [ADR 0010: AD Group Authorization Resolution](../authorization-security/0010-ad-group-authorization-resolution.md)
- [ADR 0020: Unified Authorization and Sensitive Data Handling](../authorization-security/0020-unified-authorization-and-sensitive-data-handling.md)
