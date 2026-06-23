---
id: ADR-0017
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- template-governance
adrNumber: "0017"
topic: template-lifecycle
related:
	- docs/product/PRD.md
	- docs/domain/domain-model.md
	- docs/domain/lifecycle-review.md
---

# ADR 0017: Template Lifecycle Recovery, Deprecation, and Import Conflict Rules

## Status

Accepted

## Context

The platform has confirmed template lifecycle states, release versions, API callable-version rules, template verification gates, and template export/import between environments. The remaining lifecycle risk was how to handle stopped templates or release versions, permanent deprecation, and production imports when the target environment already contains the same template identity.

These rules affect API availability, administrator permissions, auditability, and whether callers can rely on stable template IDs and API addresses.

## Decision

Templates and individual release versions can be restored from the stopped state.

Restore operations are administrator-controlled. A restore requires impact preview, secondary confirmation, and audit record.

Restore impact preview covers affected API callers or AD Group scope, default-route impact, callable-version changes after restore, and recent-call summary.

When a template is restored, release versions under the template that are not stopped and not deprecated re-enter the callable candidate set according to the template-level API management configuration.

When an individual release version is restored, only that release version re-enters the callable candidate set. It still depends on the template state and template-level API management configuration.

Deprecated templates are permanently offline and cannot be restored. A template must be stopped and have no callable release versions before it can be deprecated. Deprecation requires secondary confirmation, reason capture, impact preview, and audit record. Deprecation does not require proactive caller notification in the confirmed baseline.

When a production import finds an existing template with the same template ID, the platform keeps the template ID and creates a new development version in the target environment. The release version is chosen later during the release flow. The import does not regenerate the template ID or API address.

Imported production templates still start from draft and must pass testing, approval, pending-release, and release before a new release version becomes callable.

When testing or approval fails and the template returns to draft, prior test and approval records remain as history. A later release candidate must fully repeat testing and approval and create new test and approval records.

## Consequences

- Stopped templates and release versions remain operationally recoverable, but recovery is visible, confirmed, and auditable.
- API callable-version behavior is deterministic after recovery and still constrained by template status, release-version status, and API management configuration.
- Deprecation is protected against accidental permanent shutdown because a template must already be stopped and have no callable release versions.
- Stable template IDs and API addresses are preserved during production import, reducing downstream API churn.
- Production imports cannot bypass template verification or release governance.
- Test and approval rejection loops preserve evidence while requiring a fresh complete review cycle for later release candidates.

## Alternatives Considered

- Disallow restore after stopping: rejected because stopped is already defined as a temporary unavailable state, and administrators need a controlled recovery path.
- Allow direct deprecation without stopping first: rejected because deprecation is permanent and should not surprise API callers or bypass impact review.
- Regenerate template IDs on production import conflict: rejected because changing template identity would also change API identity and make downstream integration harder to reason about.
- Block import on template ID conflict: rejected because it creates unnecessary manual migration work when the desired behavior is to keep the same template identity and continue through a new development version.
- Allow failed testing or approval to reuse prior records automatically: rejected because a changed release candidate needs a fresh evidence chain before publication.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Lifecycle Review](../../domain/lifecycle-review.md)
- [API Contract Outline](../../api/contract-outline.md)