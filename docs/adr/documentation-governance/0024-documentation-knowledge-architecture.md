---
id: ADR-0024
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - documentation-governance
adrNumber: "0024"
topic: documentation-governance
related:
  - docs/documentation-architecture.md
  - docs/governance.md
  - scripts/validate-doc-structure.ps1
---

# ADR 0024: Documentation Knowledge Architecture

## Status

Accepted

## Context

The project adopted document as software as its operating model. The next question is how to make documentation structure scientific enough for AI-assisted development and future rebuildability.

Adding one more directory layer is not enough. The project needs a model that defines document types, metadata, source-of-truth ownership, traceability, indexes, validation rules, and AI reading paths.

## Decision

Adopt [Documentation Architecture](../../documentation-architecture.md) as the source of truth for documentation structure.

The project will treat documentation as a knowledge system. Directory layout remains useful, but it is secondary to document type, ownership, metadata, traceability, and validation.

The project will add `scripts/validate-doc-structure.ps1` as the baseline self-check script for documentation structure. AI agents should run this script after changing documentation structure, indexes, ADRs, governance rules, or document-as-software behavior.

Strict metadata checks will be adopted gradually. Existing documents do not need immediate frontmatter migration, but new governance, architecture, validation, and future ADR documents should follow the metadata schema where practical.

## Consequences

- The project has a scientific structure model instead of relying on folders alone.
- AI agents get explicit reading paths and a scriptable quality gate.
- The project can gradually evolve toward stronger metadata and traceability without blocking current documentation work.
- Large physical migrations, such as ADR topic-directory migration, can be planned against a stable model instead of done as cosmetic cleanup.
- Documentation changes now have a clearer validation target before implementation begins.

## Alternatives Considered

- Add only a new `docs/architecture/` directory: rejected as insufficient because folders do not define ownership, metadata, traceability, or validation.
- Immediately move all ADRs into topic directories: deferred because current working tree already has multiple documentation changes and a migration should follow the new architecture model.
- Require frontmatter on every existing document immediately: rejected because it would create large mechanical churn before the schema is validated in practice.
- Keep manual review only: rejected because document as software requires scriptable quality gates wherever practical.

## Related Documents

- [Document as Software Charter](../../document-as-software.md)
- [Documentation Architecture](../../documentation-architecture.md)
- [Documentation Governance](../../governance.md)
- [Documentation Index](../../README.md)
- [Validation Script](../../../scripts/validate-doc-structure.ps1)
- [ADR 0023: Document as Software Operating Model](0023-document-as-software-operating-model.md)