---
id: DOC-ARCH-SECURITY-VIEW
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - security
dependsOn:
  - docs/security/permission-matrix.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/authorization-security/0010-ad-group-authorization-resolution.md
  - docs/api/contract-outline.md
related:
  - docs/architecture/runtime-view.md
  - docs/architecture/data-storage-view.md
  - docs/architecture/async-messaging-view.md
---

# Security View

## Purpose

This view summarizes architecture-level security boundaries. Detailed permissions remain owned by the permission matrix and security ADRs.

## Security Boundary Rules

- API generation, batch generation, async task query, async task cancellation, document download, API contract view, callable version list, and API management must complete authorization before protected operations or sensitive responses.
- Authorization failures must not disclose whether unauthorized resources exist.
- AD Group authorization uses the confirmed resolution and cache rules.
- API credentials are caller identities and must not expose secret plaintext after creation or rotation display.
- Output encryption passwords are request-time secrets and must not be persisted, logged, audited in plaintext, or sent through Kafka.
- Generated documents require authorization at delivery time, including download URL use.
- Audit summaries must be durable and must not contain sensitive plaintext.
- During E05 integration hardening, enterprise identity and secret-provider integrations must remain fail-closed until complete rollout evidence is captured.
- Management UI users must complete authenticated management-login session establishment before protected management surfaces rely on role or group context.
- Current management-login baseline is local account authentication with password-hash verification; future enterprise SSO remains an extension seam, not a delivered fact in this iteration.

## Sensitive Data Handling

| Data | Handling Rule |
| --- | --- |
| API credential secret | One-time plaintext display only; stored as irreversible digest or equivalent fingerprint. |
| Output encryption password | Used only for current generation; no plaintext persistence, logging, Kafka, or audit. |
| Template variable values | Avoid raw persistence in logs/audit; use hashes or safe summaries where tracing is needed. |
| AD Group membership | Do not expose full member lists or unauthorized group details. |
| Download URL | Treat as sensitive; log and audit only masked or summarized values. |
| Generated document content | Return only after authorization; store according to retention and access rules. |

## Fail-Closed Baseline

- If API credential validation fails, deny access.
- If AD Group resolution fails and no valid unexpired cache exists, deny according to the confirmed API error model.
- If policy state cannot be loaded, deny protected operations rather than using stale or assumed policy.
- If task or document ownership cannot be resolved for query/download, return only safe errors.

## Pending Questions

- Final enterprise SSO/OIDC rollout plan, trust boundary, and cutover criteria for management UI users.
- Security review requirements before implementation begins.
- Final enterprise AD resolver and secret-provider rollout closure criteria for E05 completion.
