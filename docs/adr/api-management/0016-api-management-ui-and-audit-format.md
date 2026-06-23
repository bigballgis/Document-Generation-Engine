---
id: ADR-0016
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- api
	- audit
adrNumber: "0016"
topic: api-management
related:
	- docs/product/PRD.md
	- docs/security/permission-matrix.md
	- docs/api/contract-outline.md
---

# ADR 0016: API Management UI and Audit Format

## Status

Accepted

## Context

The dynamic API already has confirmed template-level API management scope, immediate-only change governance, impact preview, API credential lifecycle, AD Group authorization resolution, formal OpenAPI v1 contract output, and role-based contract visibility.

The remaining design gap was how administrators manage API policy areas in the UI, how API policy versions are identified, how impact preview results block or warn, how warning copy guides administrators, and how API management configuration changes are represented in audit records.

## Decision

API management configuration uses a template-level API management page with configuration-area navigation and a detail area. The navigation contains AD Group authorization, output policy, batch limits, DOCX/PDF dynamic encryption capability, and default route target release version. The detail area shows the current configuration summary, candidate configuration editor, field hints, impact preview, hard blockers and warnings, save confirmation action, current `policyVersion`, last update time, last operator, and audit entry point.

API management configuration uses fixed controls and inline hints. AD Group authorization uses a searchable AD Group selector and authorization-scope summary without exposing full membership or unauthorized group details. Output policy uses output-format and output-mode checkboxes. Batch limits use sync and async numeric inputs with limit meaning. DOCX/PDF dynamic encryption uses an enable toggle and capability selection without storing encryption passwords. Default route target uses a release-version selector with release status, contract summary, and impact hints.

API management configuration is saved by configuration area. Each configuration-area flow is: edit candidate configuration, run impact preview, resolve hard blockers or acknowledge warnings, and administrator confirmation for immediate activation. If the candidate configuration changes, the impact preview must be rerun. A successful save creates a new policy version and audit record.

API management configuration introduces `policyVersion`. Each successful configuration-area change creates a new policy version. `policyVersion` is used in contract display, audit, impact preview, and rollback correlation.

Impact preview distinguishes hard blockers from warnings. Hard blockers prevent saving when the candidate configuration violates confirmed policy or cannot become effective. Warnings highlight risk but allow an administrator to confirm and continue. Hard blocker and warning copy uses a fixed structure: reason, impact, and recommended handling. Impact information includes at least affected release-version or caller-scope summary and expected error codes. Hard blocker copy must state that saving cannot continue. Warning copy must state that continuing will activate immediately and create an audit record.

API management configuration changes use one audit event type: `API_POLICY_UPDATED`. The changed configuration areas are recorded in `changedAreas`.

The `changedAreas` baseline values are `AD_GROUP_AUTHORIZATION`, `OUTPUT_POLICY`, `BATCH_LIMIT`, `ENCRYPTION_CAPABILITY`, and `DEFAULT_ROUTE_TARGET`.

API management configuration change audit records must include `policyVersion`, previous policy version, changed areas, configuration difference summary, impact preview summary, hard blocker and warning summary, confirmation result, rollback flag, and rollback source version when applicable. They must not include API credential secret, full AD Group membership, unauthorized group details, encryption passwords, historical ciphertext, or other sensitive configuration plaintext.

## Consequences

- API management remains template-level and separate from template composition.
- Administrators can change one policy area without resubmitting unrelated policy areas.
- Administrators get a consistent page flow and field controls for high-risk API policy changes.
- `policyVersion` gives callers, auditors, and administrators a stable way to correlate contract display, impact preview, audit records, and rollback.
- Audit queries can use one broad event type while still filtering by changed configuration area.
- Impact preview can prevent invalid or unsafe configuration from taking effect while still allowing administrators to accept non-blocking risk.
- Hard blocker and warning copy remains consistent enough for audit and support review.

## Alternatives Considered

- Separate management pages for each configuration area: rejected because the confirmed model is template-level API management and administrators need one place to understand a template's calling policy.
- Wizard-only configuration: rejected because each configuration area must remain independently saveable and rollbackable.
- Advanced JSON configuration: rejected because fixed controls reduce administrator misconfiguration and prevent sensitive values from being entered into freeform configuration.
- Error-code-only warnings: rejected because administrators need a reason, impact, and recommended handling before accepting a risky immediate change.
- Save all configuration areas as one page-level change: rejected because unrelated areas should not need to change together, and independent saves reduce accidental policy churn.
- Use independent audit event types for each configuration area: rejected because a unified `API_POLICY_UPDATED` event with `changedAreas` keeps audit analysis simpler while preserving filtering detail.
- Omit policy versions and rely only on timestamps or audit IDs: rejected because rollback and contract correlation need a concise policy identifier.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)
- [OpenAPI v1](../../api/openapi-v1.yaml)