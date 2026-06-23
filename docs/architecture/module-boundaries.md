---
id: DOC-ARCH-MODULE-BOUNDARIES
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/domain/domain-model.md
  - docs/product/PRD.md
  - docs/security/permission-matrix.md
  - docs/api/contract-outline.md
related:
  - docs/architecture/runtime-view.md
  - docs/architecture/data-storage-view.md
  - docs/architecture/security-view.md
---

# Module Boundaries

## Purpose

This view defines implementation-facing module boundaries. It is a planning baseline, not source code scaffolding.

## Boundary Principles

- Modules follow business capability boundaries before technical layers.
- Product behavior, domain rules, permissions, API contracts, and ADRs remain the source of truth.
- Shared utilities must not become a place for hidden product rules.
- Rendering implementation remains isolated from lifecycle, authorization, and API governance logic.
- API management and template composition remain separate capabilities.

## Current Code Realization Baseline

- Canonical backend path: `backend/`.
- Canonical frontend path: `frontend/`.
- Backend is currently implemented as one Maven module, with documented business boundaries expressed as package boundaries under `backend/src/main/java`.
- Frontend is currently implemented as one application under `frontend/`; it should split into feature modules only when independent lifecycle, ownership, or build/runtime isolation is required.

## Candidate Modules

| Module | Owns | Does Not Own |
| --- | --- | --- |
| Master Document | DOCX master assets, anchors, master review state, master impact analysis summaries | Template release lifecycle, API policy, generated output delivery |
| Template Composition | Template structure, variables, composition rules, structured content references, test generation requests | API credential lifecycle, AD Group authorization policy, output encryption policy |
| Template Release Governance | Test records, approval records, release checklist, release summary, publication transition | Rendering engine internals, API credential secret handling |
| API Management | Template-level API policy, default route target, allowed output formats/modes, encryption capability policy, credential authorization summaries | Template content authoring, template lifecycle transitions except policy impact previews |
| API/UI Adapters | HTTP endpoint routing, request/response mapping, header and envelope translation, transport-level validation, UI action dispatch | Business workflow ownership, domain rule decisions, authorization policy source of truth |
| Generation Orchestration | Idempotency handling, generation request validation, task state coordination, synchronous/asynchronous result coordination | DOCX/PDF rendering internals, final permission source definitions |
| Rendering Worker or Service | DOCX assembly, PDF conversion, output encryption execution, preview generation, rendering diagnostics | API authorization, API credential validation, release approval decisions |
| Authorization | API credential validation, AD Group resolution use, template-level authorization decision orchestration, fail-closed behavior | External directory synchronization internals, UI-specific permission display |
| Audit | Security audit summaries, lifecycle audit records, generation audit records, policy change audit records | Sensitive plaintext storage, business variable raw-value retention |
| Contract Publication | API contract summaries, callable version lists, OpenAPI/example publication workflow | Runtime request processing beyond contract accuracy checks |
| Shared Kernel | Stable value objects, identifiers, error envelope concepts, time/trace abstractions | Business workflow ownership |

## Dependency Direction

Preferred dependency direction:

```text
API/UI adapters
    -> application orchestration modules
        -> domain capability modules
            -> infrastructure adapters
```

Rendering workers consume explicit rendering tasks and safe references. They should not reach back into UI or API adapter logic.

Authorization and audit are cross-cutting capabilities, but their rules remain explicit and testable rather than hidden in generic helpers.

## Pending Questions

- Whether contract publication is part of the main API service or a separate build/publishing workflow.
