---
id: ADR-0032
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - security
  - architecture
adrNumber: "0032"
topic: authorization-security
related:
  - docs/architecture/README.md
  - docs/architecture/security-view.md
  - docs/architecture/data-storage-view.md
  - docs/security/permission-matrix.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/api-management/0009-api-credential-lifecycle.md
---

# ADR 0032: Identity and Security Operations Baseline

## Status

Accepted

## Context

The project already has accepted ADRs for unified authorization, AD Group resolution, and API credential lifecycle. The technology decision log still contains several session-confirmed security operations items that are broader than a single API or credential rule: masking, high-risk confirmation, audit integrity, sign-in/session policy, password policy, account lifecycle, permission model, and permission-cache behavior.

This ADR records those operational identity and security baseline decisions so the technology log can stop treating them as pending session notes.

## Decision

The confirmed identity and security operations baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Application-layer sensitive field masking strategy | Unified masking middleware before response | Responses are masked or summarized before reaching callers or UI surfaces. |
| Sensitive operation secondary confirmation strategy | Secondary confirmation required for high-risk operations | High-risk operations require an explicit second confirmation step. |
| Audit log tamper-proof strategy | Append-only writes + hash-chain verification | Audit records are append-only and tamper-evident. |
| Audit log time synchronization strategy | Full-node NTP synchronization + UTC timestamps on write | Audit timestamps are synchronized and written in UTC. |
| Permission change auditing strategy | Record before/after diffs and operator for all permission changes | Permission modifications must be audit-visible with change diffs. |
| Login session management strategy | Short-lived Access Token + revocable Refresh Token | User session management baseline. |
| Concurrent session control strategy | Maximum 3 active sessions per user | Concurrency limit for active sign-in sessions. |
| Session expiration strategy | Auto-expire after 30 minutes of inactivity | Inactivity expiration baseline. |
| Authentication failure protection strategy | Lockout after consecutive failures (e.g., 5 failures -> 15-minute lockout) | Brute-force protection baseline. |
| MFA policy | Mandatory MFA for administrators, optional MFA for regular users | MFA baseline by role. |
| Password policy | Length >= 12 + complexity requirements + password history non-reuse | Password hygiene baseline. |
| Account lifecycle strategy | Auto-disable inactive accounts after 90 days | Inactive-account governance baseline. |
| Permission model strategy | Hybrid RBAC + ABAC | Access control model baseline. |
| Permission cache consistency strategy | Short TTL (e.g., 5 minutes) + active invalidation on permission change events | Permission cache must reflect changes promptly. |

These decisions are accepted as the identity and security operations foundation. They complement the unified authorization ADR and the permission matrix rather than replacing them.

## Consequences

- Security operations now have a stable baseline for masking, sessions, MFA, passwords, and permission governance.
- Audit integrity and permission-change traceability are explicitly documented instead of implied.
- Future identity or security-operation changes should update this ADR and the affected security/architecture views together.

## Alternatives Considered

- Keeping these choices only in the technology decision log: rejected because durable decisions need an ADR.
- Splitting each security operation into its own ADR: rejected for this baseline because the decisions are tightly coupled around identity and access lifecycle.
- Leaving session and password policies open until implementation starts: rejected because these choices shape the security posture now.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Security View](../../architecture/security-view.md)
- [Data and Storage View](../../architecture/data-storage-view.md)
- [Unified Authorization and Sensitive Data Handling ADR](0020-unified-authorization-and-sensitive-data-handling.md)
- [API Credential Lifecycle ADR](../api-management/0009-api-credential-lifecycle.md)