---
id: ADR-0025
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
adrNumber: "0025"
topic: architecture
related:
  - docs/architecture/README.md
  - docs/documentation-architecture.md
  - docs/document-as-software.md
---

# ADR 0025: Architecture Documentation Layer

## Status

Accepted

## Context

The project has adopted document as software and a documentation knowledge architecture. The documentation now needs an architecture fact layer that helps future AI and human contributors rebuild implementation from documentation.

The project should not jump straight from product/domain/security/API documents into source code. It needs architecture views that define system context, module boundaries, runtime responsibilities, storage boundaries, asynchronous messaging, security boundaries, and AI development workflow.

## Decision

Create `docs/architecture/` as the architecture fact layer.

The initial architecture views are:

- [Architecture Documentation](../../architecture/README.md)
- [System Context](../../architecture/system-context.md)
- [Module Boundaries](../../architecture/module-boundaries.md)
- [Runtime View](../../architecture/runtime-view.md)
- [Data and Storage View](../../architecture/data-storage-view.md)
- [Async Messaging View](../../architecture/async-messaging-view.md)
- [Security View](../../architecture/security-view.md)
- [AI Development Guide](../../architecture/ai-development-guide.md)

These documents are accepted as baseline architecture views. They remain implementation-facing but must not replace product, domain, permission, API, or ADR source-of-truth documents.

## Consequences

- Future implementation work gets a documented architecture reading path before code is written.
- AI agents can reason from source-of-truth documents to module and runtime boundaries.
- Architecture facts become indexable and script-checkable instead of being embedded only in conversation or code.
- Some implementation choices remain pending until enterprise environment constraints are confirmed.
- The architecture layer will need to be kept current as technical decisions and implementation details are confirmed.

## Alternatives Considered

- Keep architecture facts only in ADRs: rejected because ADRs explain decisions, while architecture views describe current system shape.
- Wait until implementation begins: rejected because document as software requires architecture facts before code can be rebuilt from documentation.
- Create one large architecture document: rejected because separate views are easier for AI to retrieve, validate, and update without mixing responsibilities.

## Related Documents

- [Document as Software Charter](../../document-as-software.md)
- [Documentation Architecture](../../documentation-architecture.md)
- [Architecture Documentation](../../architecture/README.md)
- [Basic Technology Stack Baseline ADR](../technology-stack/0022-basic-technology-stack-baseline.md)
- [Documentation Knowledge Architecture ADR](../documentation-governance/0024-documentation-knowledge-architecture.md)