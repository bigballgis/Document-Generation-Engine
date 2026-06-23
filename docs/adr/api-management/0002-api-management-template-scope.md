---
id: ADR-0002
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- template-governance
adrNumber: "0002"
topic: api-management
related:
	- docs/product/PRD.md
	- docs/domain/domain-model.md
	- docs/api/contract-outline.md
---

# ADR 0002: Template-Level API Management Configuration Scope

## Status

Accepted

## Context

The platform generates dynamic APIs from published templates. A template can have multiple release versions that coexist, and API calls must explicitly provide a release version number.

API authorization is confirmed as template-level authorization, and API management is performed by global administrators and group administrators. API management includes API credentials, AD Group authorization, output mode, batch limits, and DOCX/PDF dynamic encryption capability.

The design needed to decide whether API management configuration is attached to a template or to individual release versions.

## Decision

API management configuration is attached to the template, not to individual release versions.

One template has one API management configuration set. That configuration applies to all non-disabled release versions under the template.

Release versions continue to lock template content, variables, composition rules, and the release-version API contract. API management configuration is maintained as a separate calling-side policy and does not require republishing the template.

Callable version lists are derived from release versions under the template. If the template is disabled or deprecated, all release versions become non-callable. If a single release version is disabled, only that release version becomes non-callable.

API management configuration changes must be audited. They do not change the template content, variables, rules, or contract locked by an existing release version.

Release-version-level API management override is not provided in v1. Template-level API management configuration remains the only confirmed baseline.

## Consequences

- API management stays simpler because administrators maintain calling-side policy once per template.
- API callers with template-level authorization can call all non-disabled release versions under the template, subject to the current template-level API management configuration.
- A configuration change can affect multiple active release versions, so API management UX should show impact preview and audit history.
- Dynamic API contract documentation must distinguish release-version contract locking from template-level API management policy.
- Permission documentation must continue to grant API management only to global administrators and authorized-scope group administrators.

## Alternatives Considered

- Attach API management configuration to each release version: rejected for v1 because it adds governance and operational complexity, while the confirmed template-level policy is sufficient for the current contract baseline.
- Attach API management configuration to output format or output mode: not selected because output format and output mode are already controlled within the template-level API management configuration.
- Treat API management changes as requiring template republish: rejected because API management is a calling-side policy and is intentionally separate from template composition and release-version content locking.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)