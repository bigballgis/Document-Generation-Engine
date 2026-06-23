---
id: DOC-GOV-DOCUMENTATION-ARCHITECTURE
type: Governance Spec
status: Accepted
sourceOfTruth: true
owners:
  - documentation-governance
related:
  - docs/document-as-software.md
  - docs/governance.md
  - scripts/validate-doc-structure.ps1
---

# Documentation Architecture

## Purpose

This document defines how project documentation is structured as a knowledge system, not just as folders and files.

The goal is to make documentation understandable to humans, searchable by AI, traceable across decisions, and verifiable by scripts. Directory layout is only one implementation detail. The real structure comes from document types, stable metadata, source-of-truth ownership, traceability relationships, indexes, and validation rules.

## Core Model

Documentation structure is defined by six layers:

| Layer | Purpose | Primary Mechanism |
| --- | --- | --- |
| Document type | Defines what kind of knowledge a document owns | Type taxonomy |
| Metadata | Makes documents machine-readable | YAML frontmatter or structured headings |
| Source-of-truth map | Defines which document owns which facts | Governance table |
| Traceability | Connects requirements, decisions, contracts, architecture, and tests | Stable IDs and related links |
| Indexes | Make active documents discoverable | Root, area, and ADR indexes |
| Validation | Detects drift automatically | Scripts and quality gates |

Adding folders without these layers is not considered structured documentation.

AI-friendly documentation should keep one primary responsibility per document, stable headings, and explicit ownership boundaries for functional requirements, non-functional requirements, and technology selections.

## Document Type System

Each active document should have one primary type.

| Type | Purpose | Typical Location |
| --- | --- | --- |
| Charter | Project principles that should not be casually violated | `docs/document-as-software.md` |
| Governance Spec | Documentation process, structure, validation, and AI behavior rules | `docs/governance.md`, `docs/documentation-architecture.md` |
| Requirement Record | Raw confirmed requirements and pending questions | `docs/requirements/` |
| Product Spec | User-visible behavior, product scope, and product workflow | `docs/product/` |
| Behavior Spec | Actor-goal-trigger scenarios, Given/When/Then acceptance, boundaries, exceptions, evidence, and traceability for behavior-changing work | Owned inside requirements/product/domain/API/security docs or task sheets when traceable |
| Domain Model | Domain objects, states, relationships, invariants, and lifecycle rules | `docs/domain/` |
| Permission Spec | Roles, group isolation, authorization, and audit access | `docs/security/` |
| API Contract | OpenAPI, examples, error model, schemas, and API semantics | `docs/api/` |
| Architecture View | Current or proposed module, runtime, data, messaging, security, and operational views | `docs/architecture/` |
| ADR | Durable decision, context, alternatives, and consequences | `docs/adr/` |
| Review | Analysis, risk review, usability review, or first-principles review | Area-specific review documents |
| Validation Spec | Rules and scripts used to check documentation or contracts | `scripts/`, future `docs/validation/` if needed |

If a document appears to need multiple primary types, split it or make one document authoritative and link to supporting documents.

## Metadata Schema

New governance, architecture, validation, and future ADR documents should use frontmatter where practical.

Recommended metadata fields:

| Field | Required | Meaning |
| --- | --- | --- |
| `id` | Yes for new governed docs | Stable identifier used by AI, scripts, and traceability records |
| `type` | Yes for new governed docs | One value from the document type system |
| `status` | Yes for new governed docs | `Draft`, `Proposed`, `Accepted`, `Deprecated`, or `Superseded` |
| `sourceOfTruth` | Recommended | Whether the document owns facts rather than only supporting them |
| `sourceOfTruthScope` | Recommended for scoped source-of-truth docs | Clarifies which facts the document owns when ownership is limited |
| `orchestrationSourceOfTruth` | Recommended for the execution-entry plan | Marks the document that owns development execution ordering, active epic, and orchestration intake |
| `owners` | Recommended | Owning governance or domain area |
| `adrNumber` | Yes for new ADRs | Four-digit ADR number matching the filename |
| `topic` | Yes for new ADRs | ADR classification metadata used for retrieval and validation |
| `dependsOn` | Optional | Documents that must be read first |
| `related` | Recommended | Documents that should be updated or checked together |
| `supersedes` | Optional | Documents or decisions replaced by this document |

Existing documents may be migrated gradually. Lack of metadata in older documents is not itself drift until the project explicitly raises the strictness level.

ADR metadata is governed by [ADR Index](adr/README.md). ADR topic metadata must not be used as a substitute for related links, source-of-truth ownership, or update responsibility.

## Source-of-Truth Map

Each fact type has a primary owner. Other documents may summarize or link to it, but should not silently redefine it.

| Fact Type | Source of Truth | Supporting Documents |
| --- | --- | --- |
| Project operating principles | `docs/document-as-software.md` | `docs/governance.md`, `.github/copilot-instructions.md` |
| Documentation structure and validation | `docs/documentation-architecture.md` | `docs/governance.md`, `scripts/validate-doc-structure.ps1` |
| Raw confirmed requirements | `docs/requirements/requirements-plan.md` | Product, domain, permission, API docs |
| Non-functional requirements | `docs/requirements/non-functional-requirements.md` | Product, architecture, governance, and implementation-quality documents |
| Product behavior | `docs/product/PRD.md` | Requirements, reviews, ADRs |
| Behavior specifications for confirmed requirements | `docs/requirements/requirements-plan.md` and affected product/domain/security/API documents | Task sheets, tests, ADRs, architecture views |
| Domain objects and lifecycle rules | `docs/domain/domain-model.md` | Requirements, PRD, lifecycle review, ADRs |
| Role and authorization rules | `docs/security/permission-matrix.md` | Requirements, PRD, domain model, security ADRs |
| API schemas and examples | `docs/api/openapi-v1.yaml`, `docs/api/examples/` | `docs/api/contract-outline.md`, API ADRs |
| Architecture module, runtime, storage, messaging, security, and AI workflow views | `docs/architecture/` | Requirements, PRD, Domain Model, Permission Matrix, API docs, ADRs |
| Development execution entry, epic ordering, active delivery focus, and orchestration intake | `docs/architecture/orchestration-high-level-plan.md` | Task sheets, task-planning, AI development guide, internal tool selection matrix |
| Durable decisions | `docs/adr/` | All affected source-of-truth documents |
| Technical stack baseline | `docs/adr/technology-stack/0022-basic-technology-stack-baseline.md` | Architecture views under `docs/architecture/` |
| Document validation rules | `scripts/validate-doc-structure.ps1` | This document and `docs/governance.md` |

When two documents disagree, use the source-of-truth map to identify the owner, then update all affected references.

## Traceability Model

Traceability should make it possible to answer why a behavior exists, where it is specified, and how it should be verified.

Target traceability chain:

```text
User confirmation
    -> Requirement Record
    -> Behavior Specification
        -> Product Spec
        -> Domain Model
        -> Permission Spec
        -> API Contract
        -> ADR, when a durable decision is involved
        -> Architecture View, when implementation design is involved
        -> Validation or test expectation, when behavior must be verified
```

Minimum traceability rules:

- Product behavior should trace back to a confirmed requirement or an accepted ADR.
- Behavior-changing tasks should trace to actor/goal/trigger scenarios, acceptance scenarios, boundary/exception behavior, observable evidence, and owning source documents.
- Domain states and lifecycle rules should trace to requirements, PRD sections, or ADRs.
- Permission rules should trace to requirements, product roles, domain ownership, or security ADRs.
- API contract changes should trace to requirements, API ADRs, or accepted technical decisions.
- Architecture views should trace to accepted requirements, ADRs, API contracts, security rules, or pending questions.
- Validation scripts should trace to this document, governance rules, or formal contracts.

## Knowledge Graph Rules

Documentation is a graph, not a tree. A topic may appear in multiple places, but only one document should own each fact.

If a topic starts to mix functional requirements, non-functional requirements, and technology selection, split it or add explicit cross-references so one document remains the owner.

Graph edges are represented by:

- Markdown links in related-document sections.
- ADR related-document lists.
- Frontmatter `related` and `dependsOn` fields where present.
- Index entries in `docs/README.md` and area `README.md` files.
- Script-enforced required references.

High-impact topics such as Kafka, AD Group authorization, encryption, document rendering, template release gates, and audit handling should always link to the related requirements, ADRs, API contracts, and security documents.

## Index Rules

- Every active documentation file must be discoverable from `docs/README.md`, an area `README.md`, `docs/adr/README.md`, or another indexed parent document.
- Every new document must update its nearest index in the same change.
- Every ADR except the template must be listed in `docs/adr/README.md`.
- New ADRs should use metadata defined by `docs/adr/README.md`; older ADRs may be migrated gradually.
- Major governance documents must be listed in the Core Documents table in `docs/README.md`.
- Review documents must be listed as supporting documents or from their area README.
- Generated or example contract files should be linked from the relevant API index.

## AI Reading Paths

AI should read documents by task type rather than by folder order.

| Task | Required Reading Path |
| --- | --- |
| Change documentation structure | `docs/document-as-software.md` -> `docs/documentation-architecture.md` -> `docs/governance.md` -> affected indexes |
| Change requirements | `docs/document-as-software.md` -> `docs/governance.md` -> `docs/requirements/requirements-plan.md` -> affected product/domain/security/API docs |
| Change non-functional requirements | `docs/document-as-software.md` -> `docs/governance.md` -> `docs/requirements/non-functional-requirements.md` -> affected product/architecture/security/docs |
| Change product behavior | Requirements Plan -> PRD -> Domain Model -> Permission Matrix when roles or access are affected |
| Change API behavior | Requirements Plan -> PRD -> Domain Model -> Permission Matrix -> API Contract Outline -> OpenAPI -> relevant API ADRs |
| Change authorization or sensitive data handling | Permission Matrix -> Domain Model -> API Contract Outline -> relevant security ADRs |
| Change architecture or technology | Document as Software Charter -> Documentation Architecture -> accepted ADRs -> relevant architecture views |
| Change implementation code | Source-of-truth docs for the behavior -> relevant ADRs -> relevant architecture views -> tests or validation expectations |
| Execute development work | Orchestration High-Level Plan -> source-of-truth docs for behavior -> relevant ADRs and architecture views -> task sheet -> tests or validation expectations |
| Plan behavior-changing work | Orchestration High-Level Plan -> requirements/product/domain/security/API source docs -> behavior specification -> task sheet -> BDD scenarios and TDD tests |

When the reading path reveals a conflict, AI must stop treating the implementation as authoritative and resolve the documentation conflict first.

## Validation Rules

The baseline validation command is:

```powershell
./scripts/validate-doc-structure.ps1
```

The script checks:

- Required governance, source-of-truth, index, and script files exist.
- Required files are discoverable from indexes.
- Markdown links point to existing local files.
- ADR filenames are numbered and indexed.
- Metadata-bearing ADRs use valid ADR identifiers, numbers, status values, and topic taxonomy values.
- Key governance documents reference each other.
- Known stale statements do not reappear.
- This documentation architecture document contains the required structural sections.

The script supports a stricter future metadata mode:

```powershell
./scripts/validate-doc-structure.ps1 -StrictMetadata
```

Strict metadata mode is intended for gradual adoption after older documents are migrated to frontmatter.

## Migration Principles

- Do not move files just to make the tree look tidy.
- Move or split documents only when it improves source-of-truth ownership, traceability, AI reading paths, or validation.
- Introduce validation before large migrations whenever possible.
- Preserve links and update indexes in the same change as a move.
- Keep accepted requirements intact during any restructuring.
- Prefer a migration plan ADR before moving a large number of documents.

## Adding New Documents

Before adding a document, answer:

- What type is this document?
- Which facts does it own?
- Which source-of-truth documents does it depend on?
- Which index will make it discoverable?
- Which validation rule should catch drift in it?
- Could an existing document be updated instead?

If those answers are unclear, create or update a pending question rather than adding an unowned document.

## Rebuildability Check

A documentation area is rebuildable when it can answer:

- What behavior or rule must exist?
- Who or what owns the behavior?
- Which states, permissions, APIs, and data are involved?
- Which decision explains the chosen approach?
- Which implementation boundary should own it?
- How should tests or scripts verify it?
- Which open questions still block implementation?

If any answer depends mainly on existing code, the documentation is incomplete.