---
id: DOC-ARCH-AI-DEVELOPMENT-GUIDE
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - documentation-governance
dependsOn:
  - docs/document-as-software.md
  - docs/documentation-architecture.md
  - docs/governance.md
related:
  - .github/copilot-instructions.md
  - .github/skills/documentation-governance/SKILL.md
  - docs/architecture/tdd-delivery-workflow.md
  - scripts/validate-doc-structure.ps1
---

# AI Development Guide

## Purpose

This guide tells AI agents how to work in this project so documentation stays authoritative and implementation remains rebuildable from documentation.

## Default Reading Order

For any non-trivial change, read in this order:

1. [Document as Software Charter](../document-as-software.md).
2. [Documentation Architecture](../documentation-architecture.md).
3. [Documentation Governance](../governance.md).
4. For execution-oriented development work, [Orchestration High-Level Plan](./orchestration-high-level-plan.md) to identify active epic, planned direction, and whether a plan refresh is required.
5. The source-of-truth document for the requested behavior.
6. Relevant ADRs and architecture views.
7. Relevant tests or validation scripts once implementation exists.

## Documentation-First Workflow

1. Identify the document type and source-of-truth owner.
2. Decide whether the user input is confirmed, pending, a correction, or a decision candidate.
3. Update the source-of-truth document first.
4. Update dependent product, domain, permission, API, architecture, ADR, or validation documents in the same change.
5. Update indexes and related links.
6. Run `../../scripts/validate-doc-structure.ps1` after documentation structure, indexes, ADRs, governance rules, or document-as-software behavior change.
7. Report remaining pending questions explicitly.

## Implementation Workflow

Implementation must wait until the needed behavior is documented.

Before coding, AI must identify:

- Which active epic or planned direction in [Orchestration High-Level Plan](./orchestration-high-level-plan.md) authorizes this execution path, or whether the plan must be refreshed first.
- Which behavior specification authorizes the task: actor/role, user goal, trigger, preconditions, user journey, system responses, acceptance scenarios, boundary and exception behavior, observable evidence, and traceability.
- Which requirement or ADR authorizes the behavior.
- Which product, domain, permission, API, or architecture document owns the rule.
- Which module boundary should own the implementation.
- Which tests or validation checks should verify it.
- Which pending questions still block implementation.

Implementation execution order must start from [Orchestration High-Level Plan](./orchestration-high-level-plan.md), then confirm the behavior specification, then follow [Fixed TDD Delivery Workflow](./tdd-delivery-workflow.md), including task generation, task sheet generation, skills review, strict Red-Green-Refactor TDD, requirement testing, code review plus static scans, and then scoped commit/push.

If the behavior is unclear, AI must ask the user for confirmation and update the owning requirements, product, domain, permission, API, architecture, or ADR documents before task planning or implementation.

If code and documentation disagree, update or clarify documentation before claiming completion.

## Rebuildability Checklist

Before implementation begins for a capability, documentation should answer:

- What user or API behavior is required?
- Which actor performs the behavior, what goal they have, what triggers the behavior, and what observable outcome proves success?
- Which Given/When/Then or equivalent acceptance scenarios define normal, boundary, and exception behavior?
- Which domain objects and states are involved?
- Which permissions and authorization checks apply?
- Which API contracts, events, storage records, or generated assets are involved?
- Which module owns the behavior?
- Which ADR explains the chosen approach?
- Which tests or scripts should validate the behavior?

If these answers are missing, document them or mark pending questions before implementation.

## AI Constraints

- Do not convert assumptions into confirmed requirements.
- Do not add implementation-specific choices unless explicitly confirmed or recorded as pending.
- Do not hide open questions in prose.
- Do not create broad generic utility modules as a substitute for clear domain boundaries.
- Do not treat generated code as more authoritative than documentation.
- Do not move files without updating links, indexes, and validation rules in the same change.

## Required Validation

Run this command after changing documentation structure, indexes, ADRs, governance rules, or document-as-software behavior:

```powershell
../../scripts/validate-doc-structure.ps1
```

From the repository root, run:

```powershell
./scripts/validate-doc-structure.ps1
```
