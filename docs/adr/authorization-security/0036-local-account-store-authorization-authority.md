---
id: ADR-0036
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - security
  - architecture
adrNumber: "0036"
topic: authorization-security
related:
  - docs/requirements/requirements-plan.md
  - docs/product/PRD.md
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/authorization-security/0032-identity-and-security-operations-baseline.md
---

# ADR 0036: Local Account Store as Authorization Authority (SSO Authentication-Only)

## Status

Accepted

## Context

The platform's whole isolation model (role + authorized group scope + per-group
isolation) depends on two preconditions: that a user can be assigned roles and an
authorized group scope, and that a group is an operable object. Today the only way
to provision either is to edit Flyway migrations and redeploy, because
`management_user` exposes only sign-in/sign-out/session endpoints (no user CRUD,
role assignment, or group-scope assignment) and `business_group` is only a seed
table (RETAIL/CORP) with no Java entity, repository, or management API.

Earlier decisions already confirmed that the management surface uses a real
local-account authentication baseline and that future company SSO integration is
kept as an extension boundary, not a delivered capability (see ADR 0032 and the
unified authorization ADR 0020). What was still missing is an explicit, durable
decision about where authorization data (roles and authorized group scope) lives
once SSO is introduced for authentication.

Without that decision, an SSO rollout could be (mis)read as moving role and
group-scope mapping into the identity provider, which would break the platform's
fail-closed, per-group isolation guarantees and split the authorization
source-of-truth across two systems.

## Decision

The local account store remains the long-term **authorization authority**. SSO is
responsible only for **authentication (authN)**.

1. **Local account store owns authorization.** Management users and their role set
   and authorized group scope are created and maintained inside the platform's
   local user/group management. Roles and group-scope mappings are never sourced
   from, nor overridden by, an external identity provider.
2. **Groups are first-class, locally governed objects.** Business groups (with a
   `dimension` of `BUSINESS_LINE` or `DEPARTMENT`) are created, edited, and
   enabled/disabled inside the platform, not by an external directory. AD Group
   resolution stays scoped to runtime API authorization and is unrelated to the
   management-plane group object.
3. **SSO is authN-only when introduced.** A future SSO/OIDC integration may assert
   *who* the user is, but the platform still resolves *what the user may do* from
   the local account store. SSO does not provision, mutate, or delete roles or
   authorized group scope.
4. **Account linkage, not authority transfer.** When SSO arrives, an SSO identity
   is linked to an existing local management user (the `auth_source` already
   distinguishes `LOCAL`); the linked user's locally maintained roles and group
   scope continue to drive every authorization decision.
5. **Fail-closed continuity.** If the external authenticator is unavailable or an
   identity cannot be linked to a local account, the unified fail-closed
   authorization baseline applies — no implicit role or scope is granted.

This ADR records the source-of-truth boundary only. The concrete user/group
lifecycle behavior, the group-admin-manages-users permission point, and the
management-plane API contract are confirmed in the requirements plan, PRD, domain
model, and permission matrix; this ADR does not restate them.

## Consequences

- The isolation model has a stable authorization authority that survives an SSO
  rollout; SSO can be added later without redesigning role/group-scope ownership.
- Operators provision users and groups through the platform instead of editing
  Flyway migrations and redeploying.
- A future SSO integration is constrained to authentication concerns, which keeps
  the authorization decision path single-sourced and auditable.
- The platform must keep maintaining local user/group management even after SSO is
  adopted; this is an accepted, deliberate cost.

## Alternatives Considered

- **Delegate roles and group scope to the identity provider after SSO.** Rejected:
  it splits the authorization source-of-truth, weakens per-group isolation, and
  couples fail-closed guarantees to an external system's availability and schema.
- **Keep relying on Flyway seed edits + redeploy for provisioning.** Rejected: it
  is operationally untenable, leaves no audit trail for identity changes, and
  blocks the confirmed isolation model from being usable without redeploys.
- **Treat AD Group membership as the management group model.** Rejected: AD Group
  is a runtime-API authorization input, not a locally governed management object;
  conflating them would break the management-plane group lifecycle and isolation.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Unified Authorization and Sensitive Data Handling ADR](0020-unified-authorization-and-sensitive-data-handling.md)
- [Identity and Security Operations Baseline ADR](0032-identity-and-security-operations-baseline.md)
- [AD Group Authorization Resolution ADR](0010-ad-group-authorization-resolution.md)
