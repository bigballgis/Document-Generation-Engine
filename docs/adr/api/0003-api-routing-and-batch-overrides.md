---
id: ADR-0003
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
adrNumber: "0003"
topic: api
related:
	- docs/api/contract-outline.md
	- docs/api/openapi-v1.yaml
	- docs/domain/domain-model.md
---

# ADR 0003: API Route Versioning, Default Route, and Batch Item Overrides

## Status

Accepted

## Context

The platform generates APIs from templates, and a template can have multiple published versions. Earlier decisions confirmed that API authorization and API management configuration are template-level, while release versions lock template content, variables, rules, and the release-version contract.

The API contract needed a routing model that supports stable upstream integration while still allowing explicit release-version calls. The batch request model also needed to decide whether output and encryption configuration are batch-level only or can vary per item.

## Decision

API routing supports explicit release-version routes. The caller selects the target template and release version through the route path.

Dynamic API paths use the `/api/{environment}/v1` prefix. The platform still reads the current deployment environment from environment variables and validates that the path `{environment}` matches the current deployment environment.

The confirmed single-generation paths are:

- `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/generate`
- `/api/{environment}/v1/templates/{templateId}/default/generate`

The `releaseVersion` path parameter uses semantic versioning, for example `1.0.0`, `1.1.0`, or `2.0.0`.

API routing also supports a default route. The default route is configured in API management and explicitly points to one non-disabled release version under the template. It is intended to reduce forced upstream/downstream changes when the platform changes the version served by a stable route.

The default route must not implicitly point to the latest version. It must be explicitly configured by a global administrator or authorized-scope group administrator. API contract displays for the default route show the stable default path, current target release version, target status, last update time, last operator, and corresponding explicit-version path.

Changes to the default route target release version must be audited and must provide impact preview. The default route target change is immediate-only: future scheduled activation, pending default targets, and pending-change cancellation are not supported.

Default route target changes do not proactively notify callers or administrators. Callers inspect the API contract to see the current default target.

The impact preview includes the current and candidate target release versions, authorized caller scope, recent default-route usage summary, contract-difference summary, and idempotency impact notice.

Rollback is handled as a new controlled change: an administrator selects a historical target release version as the new target, reviews the impact preview, confirms the change, and the rollback takes effect immediately with an audit record.

Batch requests support both batch-level unified output/encryption configuration and item-level overrides. Item-level overrides may set output format, output mode, and encryption parameters per item.

Batch generation uses independent paths instead of sharing the single-generation path:

- `/api/{environment}/v1/templates/{templateId}/versions/{releaseVersion}/batch-generate`
- `/api/{environment}/v1/templates/{templateId}/default/batch-generate`

Item-level overrides must still be validated against the template-level API management configuration. They cannot bypass configured output modes, batch limits, or DOCX/PDF dynamic encryption capability.

Asynchronous task query uses `/api/{environment}/v1/templates/{templateId}/tasks/{taskId}` so that task reads remain naturally scoped to template-level authorization.

Generated document downloads use `/api/{environment}/v1/documents/{documentId}/download`. Download authorization still resolves the generated document back to its template and applies API credential, AD Group, and template-level authorization checks.

## Consequences

- Upstream systems can call a stable default route without changing integration paths on every release-version change.
- Administrators must manage default route target changes carefully because one configuration change can shift downstream behavior.
- Audit records need to distinguish explicit version route calls from default route calls and record the resolved release version.
- API management UX must show impact preview before changing or rolling back the default route target version.
- Default route target changes are simple operationally because there is no scheduled or pending state, but administrators cannot stage a future target in advance.
- Caller-facing notifications are not required for default target changes; contract visibility and audit records are the confirmed traceability mechanisms.
- Batch response and audit models need to identify batch-level configuration, item-level overrides, and per-item validation/generation results.
- API contract documentation now publishes stable generation, batch, task-query, download, contract-discovery, and callable-version paths, with formal OpenAPI output and examples maintained under the API documentation folder.

## Alternatives Considered

- Require every caller to always use an explicit release-version path: rejected because it forces upstream/downstream changes for version switching scenarios.
- Make default route automatically point to latest release: rejected because it introduces uncontrolled regression risk.
- Support scheduled default route activation: rejected for the current baseline because immediate-only changes keep default route state simpler.
- Proactively notify callers for default target changes: rejected because audit records and API contract visibility are the confirmed traceability baseline.
- Treat rollback as a special one-click operation outside normal governance: rejected because rollback can shift downstream behavior and should use the same preview and audit controls as any target change.
- Allow only batch-level output and encryption configuration: rejected because callers need per-item flexibility in batch generation.
- Allow item-level overrides without API management validation: rejected because it would bypass administrator-controlled API policy.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Draft](../../api/contract-outline.md)