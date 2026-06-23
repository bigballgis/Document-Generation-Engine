---
id: ADR-0015
type: ADR
status: Accepted
sourceOfTruth: true
owners:
	- template-governance
adrNumber: "0015"
topic: template-lifecycle
related:
	- docs/product/PRD.md
	- docs/domain/domain-model.md
	- docs/domain/lifecycle-review.md
---

# ADR 0015: Template Verifiability and Release Gate

## Status

Accepted

## Context

The platform lets business users compose financial letter templates without development. Template content, anchors, variables, rules, previews, approvals, and generated API contracts must be reliable before a template is published for API use.

Earlier requirements confirmed template testing, approval, release versions, anchor validation, variable and rule capabilities, audit requirements, and strict separation between template composition and API management. The remaining high-priority risk was whether template release candidates must be verifiable through test data, preview, difference summaries, validation checks, and release blocking rules.

## Decision

v1 treats template verifiability as a required release baseline.

Template release candidates must have test data sets that can drive DOCX/PDF test generation, generation preview, variable Schema validation, and rule validation.

Testing and approval views must provide generation preview and change difference summaries. Difference summaries cover template content, anchor usage, template variables, composition rules, and release-candidate API contract summary changes.

The release checklist must cover anchor completeness, variable Schema validity, rule execution boundary, test generation result, batch test summary, sample coverage summary, template coverage summary, generation preview, final generated artifact references, preview comparison summary, change difference summary, approval summary, and blocking-item status.

A release candidate can keep multiple named test data sets. Each test data set remains bound to one template or release candidate and can be marked as required or optional with scenario metadata, coverage tags, and data set version.

The platform supports batch test generation for selected release-candidate samples. Batch test results include per-sample generation records, preview artifact references, warning summaries, blocker summaries, coverage summaries, batch status, and non-sensitive statistics.

Sample coverage and template coverage cover at least variable values and required fields, condition/loop/rule branches, anchors or sections, controlled components such as tables, and DOCX/PDF output formats. Coverage summaries must not persist raw template variable test values.

All required samples must pass without unresolved blockers. Fidelity warnings follow the confirmed acknowledgement rules. Template coverage and critical-dimension coverage must meet configurable thresholds; coverage below threshold is a publication blocker. Optional samples are included in batch testing and coverage evidence, but do not independently block publication unless configured as required or needed to meet a coverage threshold.

Anchor completeness confirms that anchors referenced by the template exist in the selected master document. Missing anchors are blocking items.

Variable Schema validation confirms that template variable definitions, types, required flags, enum values, nested structures, defaults, and related rules match the release-candidate contract. Schema validation failure is a blocking item.

Rule execution boundary validation confirms that calculation expressions, aggregate functions, conditional display, loop rendering, and cross-field validation only use API input and template-internal configuration. Rules that call external data or external APIs are blocking items.

Approval summaries show test records, generation preview, difference summaries, release-checklist results, and blocking-item status to approvers.

If the release checklist contains unresolved blocking items, the template cannot be published.

Template verifiability records, including test data set summaries, test generation records, batch test summaries, coverage summaries, generation preview summaries, final generated artifact references, preview comparison summaries, difference summaries, release-checklist results, approval summaries, warning acknowledgements, and blocking-item status, must be auditable or traceable. They must not store sensitive plaintext from template variable test values; sensitive content follows the established audit masking rules.

## Consequences

- Template publication becomes gated by verifiable evidence instead of only by manual status transitions.
- Testers and approvers can review generated output evidence, difference summaries, and release-checklist results before making a decision.
- Missing anchors, invalid variable schemas, out-of-bound rules, missing evidence, failed required samples, coverage below configured thresholds, failed approval, and unresolved blocking items prevent publication.
- The platform must preserve enough traceability for template testing, approval, and release decisions without recording sensitive test values in plaintext.
- Test or approval comment templates and remaining interaction details stay separate product-experience decisions.

## Alternatives Considered

- Keep release validation as a non-blocking checklist: rejected because unresolved anchor, schema, rule, test, approval, or evidence issues can produce incorrect financial documents.
- Require only manual tester and approver judgment: rejected because low-code template changes need repeatable evidence for release decisions.
- Treat preview and difference summary as future usability enhancements: rejected because testers and approvers need these artifacts to assess release candidates.
- Store full test inputs and generated evidence without masking: rejected because template variable values can contain sensitive financial or customer information.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Lifecycle Review](../../domain/lifecycle-review.md)
- [Usability Review](../../product/usability-review.md)