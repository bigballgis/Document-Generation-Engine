---
id: ADR-0021
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- template-governance
adrNumber: "0021"
topic: template-lifecycle
related:
	- docs/product/PRD.md
	- docs/domain/domain-model.md
	- docs/domain/lifecycle-review.md
---

# ADR 0021: Template Testing, Approval, and Release Governance

## Status

Accepted

## Context

The platform already has a template lifecycle, release gate, test data sets, previews, difference summaries, approval summaries, and release checklists. The remaining governance questions were how structured test and approval comments should be, whether publication needs a final confirmation step, how group administrator exception intervention should be controlled, and whether v1 needs approval timeout or delegated approval.

These rules affect audit quality, release confidence, and whether exception handling stays visible without turning the approval process into a heavier multi-level workflow.

## Decision

Test decisions record a structured result and test comment using a controlled form template rather than the structured document fragment editor. A passed test requires confirmation of the test evidence summary, batch test summary, coverage summary, generation preview summary, and fidelity warning summary, with an optional note. A failed test requires a reason category, impact scope, and remediation suggestion. Test decisions record the associated test data set and generation preview summary.

Approval decisions record a structured result and approval comment using a controlled form template rather than the structured document fragment editor. Approval requires a rationale summary and confirmation of key evidence summaries. Rejection requires a return reason category, impact scope, and remediation requirement. Approval decisions are associated with test records, change difference summaries, and release-checklist summaries.

Risk prompt copy uses system defaults and optional group-level overrides. Global administrators maintain global defaults, and group administrators maintain override copy and reason categories within their authorized group scope. Risk prompts cover at least unresolved blockers, fidelity warnings, coverage below threshold, preview-comparison differences, API contract or impact-scope changes, and administrator exception intervention. Risk prompt copy changes are audited.

Collaboration notifications use in-platform work items and status indicators only. v1 does not send email or instant-message notifications and does not introduce notification-template editing. Test submission creates a test role-queue work item for the template's group. Test failure or return to draft creates a remediation work item for the submitter or template orchestration user. Approval submission creates an approval role-queue work item. Approval rejection or return to draft creates a remediation work item. Approval into pending release creates a release work item for roles that can publish.

Collaboration work items are assigned by template group and role queue, not to named individuals by default. Work item visibility does not grant extra template-edit, test-decision, approval-decision, or publish permission.

Test work items, approval work items, pending-release work items, and remediation work items support automatic timeout escalation. Timeout thresholds use global defaults with group-level overrides. Global administrators maintain global defaults, and group administrators maintain overrides within their authorized group scope. A timeout creates an in-platform escalation work item and status indicator for the corresponding group administrator.

Timeout escalation does not approve, reject, return to draft, publish, or otherwise change template state. It also does not create delegated approval or proxy approval. If a group administrator later intervenes in a test or approval decision, the existing exception-handler rule still applies.

Collaboration work items, status indicators, and timeout escalation show only non-sensitive summaries and must not include template variable values, customer data, full generated content, or sensitive plaintext. Work item creation, resolution, timeout escalation, and timeout-threshold configuration changes are audited.

When a group administrator intervenes in testing or approval as an exception handler, the operation requires a reason, secondary confirmation, and a separate audit marker for administrator override.

Before publishing from the pending-release state, the platform must show a release summary and require secondary confirmation. The release summary includes at least the release version, change difference summary, batch test summary, coverage summary, final artifact reference, preview-comparison summary, release-checklist result, API contract summary, and impact scope.

v1 introduces timeout escalation only as in-platform visibility for delayed work items. Delegated approval, proxy approval, automatic approval, automatic rejection, and automatic state transition remain out of scope and require separate confirmation.

## Consequences

- Test and approval decisions become easier to audit and review after the fact.
- Failed tests and rejected approvals carry explicit reasons instead of only a state transition.
- Passing tests and approvals leave durable rationale or evidence-confirmation records without requiring long free-text narratives.
- Risk prompt copy can be adapted by authorized administrators while preserving auditability and sensitive-data boundaries.
- Group-role queues make handoffs visible without requiring named individual assignment for every test or approval.
- Timeout escalation gives administrators visibility into blocked work without changing lifecycle state or decision authority.
- Administrator exception handling remains possible for remediation scenarios, but it is distinguishable from normal tester or approver decisions.
- Publication has a final human confirmation point after the release gate passes.
- v1 still avoids delegated approval and proxy approval even though timeout escalation is confirmed.

## Alternatives Considered

- Free-text-only comments: rejected because structured results and required reasons are needed for audit and review.
- Optional approval comments for approvals: rejected because approval should leave a durable review rationale for regulated financial documents.
- Reusing the structured document fragment editor for test or approval comments: rejected because comment templates are governance forms, not generated-document content.
- Fully configurable freeform prompt templates: rejected because risk prompts must preserve consistent required fields and sensitive-data boundaries.
- Email or instant-message notifications in v1: rejected because station-internal work items are sufficient for the confirmed baseline and avoid notification-channel delivery, retry, and sensitive-data rules.
- Named individual assignment as the default: rejected because tester and approver responsibilities are group-scoped role queues in the confirmed model.
- Automatic timeout approval, rejection, return to draft, or publish: rejected because timeout escalation must not create automatic decisions or lifecycle state changes.
- Delegated or proxy approval: rejected because escalation only creates administrator visibility and does not transfer approval authority.
- Publish immediately after release-checklist success: rejected because the release action changes callable versions and should have an explicit final confirmation.
- Multi-person release confirmation: rejected for the confirmed baseline because template approval is already a one-level approval process.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Lifecycle Review](../../domain/lifecycle-review.md)
- [Usability Review](../../product/usability-review.md)