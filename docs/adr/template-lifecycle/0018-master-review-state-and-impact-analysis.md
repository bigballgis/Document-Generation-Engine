---
id: ADR-0018
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- template-governance
adrNumber: "0018"
topic: template-lifecycle
related:
	- docs/product/PRD.md
	- docs/domain/domain-model.md
	- docs/domain/lifecycle-review.md
---

# ADR 0018: Master Review State and Impact Analysis

## Status

Accepted

## Context

Master documents are DOCX assets that provide the base structure and anchors for templates. The platform already confirmed that masters do not need the same full lifecycle as templates and do not have enabled, stopped, deprecated, or release-version states.

The remaining governance risk was whether an unreviewed master can be referenced by templates, how master review rejection should behave, and whether master changes should make impacted templates visible before users continue composition or release work.

## Decision

Masters have a lightweight review state model: draft, pending review, review approved, and review rejected.

This state model does not create a full template-like lifecycle. It does not introduce stopped states, deprecated states, or release versions for masters.

Submitting a master for review requires anchor completeness validation and a change description. If anchor completeness validation fails, the master cannot be submitted for review. The change description is included in the review summary and audit record.

Master review is performed by global administrators or group administrators within their authorized group scope.

Only review-approved masters can be referenced when creating or updating templates. Draft, pending-review, and review-rejected masters cannot be referenced by templates.

When master review is rejected, the master returns to draft. The rejection record remains as history. After changes, the master can be submitted again and creates a new review record.

Master updates do not automatically affect existing templates. Master changes require impact analysis. The impact analysis must include a referenced-template list and a retest prompt.

Master review submission, review approval, review rejection, master changes, and master impact analysis must be auditable.

## Consequences

- Template authors can only build on reviewed master structures, reducing the chance of invalid anchors reaching template release.
- Master governance remains lighter than template lifecycle governance.
- Review rejection is traceable without preserving an unusable rejected state as a long-running work state.
- Existing templates are not silently changed by master updates, but users can see which templates may need retesting.
- The exact UI wording for missing or duplicate anchors, change descriptions, and retest prompts remains a product-experience detail.

## Alternatives Considered

- No master review state: rejected because templates could reference unreviewed DOCX structures and anchor definitions.
- A full template-like master lifecycle: rejected because masters do not need stopped, deprecated, release, or version states in the confirmed baseline.
- Allow draft templates to reference unreviewed masters: rejected because it would defer master quality problems to template testing or release gates.
- Force all referenced templates to retest immediately after any master update: rejected because master updates do not automatically affect existing templates in the confirmed baseline.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Lifecycle Review](../../domain/lifecycle-review.md)
- [Usability Review](../../product/usability-review.md)