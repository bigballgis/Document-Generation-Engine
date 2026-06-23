---
id: ADR-0020
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- security
adrNumber: "0020"
topic: authorization-security
related:
	- docs/security/permission-matrix.md
	- docs/architecture/security-view.md
	- docs/domain/domain-model.md
---

# ADR 0020: Unified Authorization and Sensitive Data Handling

## Status

Accepted

## Context

The platform has confirmed API credential authentication, AD Group authorization, template-level API authorization, group-scoped back-office permissions, generated-document download authorization, audit visibility rules, API contract visibility rules, and multiple masking rules for credentials, encryption passwords, download URLs, context fields, and template verification records.

Those rules need one shared baseline so future implementation, API contract work, management screens, audit exports, and support troubleshooting do not diverge by entry point.

## Decision

v1 uses a unified authorization decision baseline across document generation APIs, batch generation, async task query, async task cancellation, generated-document download, API contract discovery, callable-version listing, API management, audit viewing and export, back-office template operations, and back-office master document operations.

Authorization decisions happen before protected operations execute or sensitive responses are returned.

If an authorization dependency is unavailable and there is no confirmed valid cache or equivalent confirmed fallback, the platform fails closed.

Authorization decisions combine the factors required by each entry point, including identity, role, group scope, API credential, access account, AD Group, template-level authorization, object ownership, environment, resource state, and API management configuration.

Async task query, async task cancellation, and generated-document download resolve the task or document back to its template and then execute template-level secondary authorization.

API contract discovery and callable-version listing return only the current authorization view. They must not expose unauthorized templates, unauthorized callers, full AD Group membership, or unauthorized group details.

API management, audit viewing and export, back-office template operations, and back-office master document operations require role permission, group scope, and object ownership checks.

Authorization denial and authorization dependency failure return only confirmed safe error codes and generic safe messages. They must not reveal unauthorized resource existence, unauthorized group details, full membership lists, API secrets, encryption passwords, or internal configuration details.

Authorization decisions and denials are auditable through safe summaries. The audit summary includes subject summary, entry point, environment, object-scope summary, decision result, denial reason code, or dependency failure reason. It does not include sensitive plaintext.

v1 uses a three-part sensitive data handling baseline:

- Plaintext persistence or display is forbidden.
- Summary or fingerprint is allowed.
- Authorized response exceptions are allowed only where explicitly confirmed.

Plaintext persistence or display is forbidden for API credential secrets, DOCX/PDF encryption passwords, template variable raw values, sensitive template test data values, full request bodies, full download URLs, full AD Group membership, unauthorized group details, historical ciphertext, sensitive configuration plaintext, and unauthorized generated-document content.

Summary or fingerprint representation is allowed for API credential identifiers or fingerprints, `idempotencyKey` summaries, request semantic hashes, `variablesHash`, `itemsHash`, encryption policy summaries, AD Group authorization summaries, masked download URLs, `contextSummary`, `policyVersion`, `changedAreas`, and configuration-difference summaries.

Authorized response exceptions are limited to confirmed safe cases: API credential secret plaintext is shown once during creation or rotation; authorized API responses can return usable `download.url`; synchronous file-stream and generated-document download responses can return document content after authorization succeeds; `task.queryPath` can be returned because it is only a relative path and grants no additional access.

The masking baseline applies to logs, audit records, management screens, API contract displays, contract examples, error responses, exported files, and support troubleshooting material.

Unknown or unclassified fields default to sensitive handling. They can be downgraded to summary or displayable fields only after explicit confirmation.

## Consequences

- API and back-office entry points share one authorization and masking vocabulary.
- Fail-closed behavior reduces accidental access during dependency outages.
- Contract discovery, callable-version listing, task query, cancellation, and download behavior stay consistent with template-level authorization.
- Audit and support workflows receive enough context to investigate decisions without exposing secrets, raw variables, full request bodies, or unauthorized details.
- Future API schemas, examples, and management screens need to align with the sensitive data classification instead of inventing local masking rules.

## Alternatives Considered

- Keep authorization and masking rules local to each API or screen: rejected because local rules can drift and expose different details for the same resource.
- Let dependency failures fall back to stale authorization data: rejected because stale authorization can preserve access after membership or policy changes.
- Return detailed denial diagnostics to callers: rejected because unauthorized callers should not learn resource existence, group membership, or configuration details.
- Store full request and variable values for support troubleshooting: rejected because financial document inputs can contain customer, account, amount, password, or other sensitive data.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)
- [API Contract Visibility, Audit Summary, and Context ADR](../api/0013-api-contract-visibility-audit-and-context.md)
- [API Management UI and Audit Format ADR](../api-management/0016-api-management-ui-and-audit-format.md)