---
id: ADR-0023
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - documentation-governance
adrNumber: "0023"
topic: documentation-governance
related:
  - docs/document-as-software.md
  - docs/governance.md
  - .github/copilot-instructions.md
---

# ADR 0023: Document as Software Operating Model

## Status

Accepted

## Context

The project is still early enough that documentation structure, AI collaboration rules, and implementation discipline can be shaped before source code becomes large. The user wants documentation to be written with AI assistance, but also wants documentation structure and continuous documentation updates to become the project's first priority.

The project should progress beyond docs-as-code toward document as software: documentation should be treated as the durable system definition, while implementation code remains replaceable. The long-term goal is that the system can be rebuilt from documentation if implementation code is discarded and recreated.

## Decision

Adopt document as software as a project operating model.

The project will maintain [Document as Software Charter](../../document-as-software.md) as a first-class governance document. The charter defines the rebuildability standard, AI development rules, code rebuild rule, and document quality rules.

The documentation governance workflow, project instructions, and documentation indexes must reference this operating model. Future implementation work must remain traceable to requirements, ADRs, API contracts, domain rules, permission rules, architecture documents, or other accepted documentation sources.

When implementation and documentation disagree, the conflict must be surfaced and resolved through documentation before completion is claimed. Code is not allowed to silently become the source of truth.

## Consequences

- Documentation becomes the primary durable project asset rather than a companion artifact.
- AI-authored documentation is allowed, but must follow the same confirmed-vs-pending, indexing, traceability, and anti-drift rules as human-authored documentation.
- Future code generation and refactoring can rely on a stronger source-of-truth chain.
- Documentation updates may take priority over immediate implementation speed when a change affects behavior, security, permissions, APIs, architecture, or operations.
- The project will need documentation validation scripts and stronger architecture documentation as implementation approaches.

## Alternatives Considered

- Keep the existing docs-as-code model only: rejected because it does not fully express the rebuild-from-documentation goal.
- Treat generated code as the practical source of truth once implementation begins: rejected because it would weaken rebuildability and increase drift risk.
- Rely only on Copilot instructions without a project document: rejected because the principle should be visible to humans, AI agents, and future review processes.

## Related Documents

- [Document as Software Charter](../../document-as-software.md)
- [Documentation Governance](../../governance.md)
- [Documentation Index](../../README.md)
- [Git Workflow](../../git-workflow.md)
- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)