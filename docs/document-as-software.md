# Document as Software Charter

## Purpose

This project treats documentation as the durable system definition and implementation code as a replaceable realization of that definition.

The project goal is not only docs-as-code. The project is moving toward document as software: documentation is structured, versioned, validated, indexed, and maintained with the same discipline expected from source code. The system should remain rebuildable from the documentation even if implementation code is discarded and recreated.

## Execution truth (2026-06-23 reset)

- Overall plan: [docs/plan/master-plan.md](plan/master-plan.md)
- Detailed plans: [docs/plan/detail/](plan/detail/)
- Prior task/wave/epic **Done** claims are void until re-earned; see [PROJECT-STATUS-RESET.md](PROJECT-STATUS-RESET.md).

## Core Principle

Documentation is the first-class project asset.

Production-grade real functionality is a constitutional requirement for delivery acceptance. Implementation closure must represent real, durable, deployable behavior instead of demo-only, in-memory-only, or mock-only completion claims.

AI may assist with writing documentation, restructuring documentation, and generating implementation from documentation. That does not reduce the standard for documentation quality. AI-authored documentation must still be structured, traceable, current, and separated into confirmed decisions and pending questions.

Documentation structure is governed by [Documentation Architecture](documentation-architecture.md). Directory layout alone is not considered sufficient structure; documents also need explicit type ownership, metadata, traceability, indexes, and validation rules.

Behaviour Driven Design/Development is a project-level governance principle. Plans, requirements, tests, and implementation must be driven by explicit user behavior: role, goal, trigger, preconditions, user actions, system responses, acceptance scenarios, boundary and exception behavior, observable evidence, and traceability to source-of-truth documents.

## Operating Commitments

- Production-grade delivery is mandatory by default. Behavior completion claims must be backed by durable persistence, real integration paths, and runtime-verifiable deployment evidence.
- Temporary realization seams (in-memory stores, local seed-only identity, stubs, mocks, fake adapters) must be explicitly marked as transitional and must not be reported as production-complete behavior.
- Documentation is updated before implementation when behavior, architecture, API contracts, permissions, security, data handling, or deployment assumptions change.
- Technology selection confirmations are persisted immediately after each user answer in `docs/architecture/technology-stack-decisions.md`; session-only memory is not accepted as durable project truth.
- Documentation is kept structured by responsibility. Requirements, product behavior, domain rules, permissions, API contracts, ADRs, governance, and future architecture documents must not blur responsibilities.
- Documentation is kept current. Stale statements must be corrected when new confirmed decisions supersede them.
- Documentation is kept discoverable. Every active document must be reachable from an index, and new documents must be linked from the appropriate parent index in the same change.
- Documentation is kept traceable. Durable decisions use ADRs; product and domain behavior stay in their source-of-truth documents; implementation follows those documents.
- Documentation is behavior-driven. Behavior-changing work must describe user behavior and acceptance scenarios before task planning or implementation.
- Stable repetitive project helper operations should use script automation first, and the scripts should be updated whenever the workflow changes.
- Frontend automation must use the repository pnpm baseline through a direct `pnpm` executable or the shared script resolver; automation must not call `corepack pnpm` because Corepack may bypass approved npm registry, proxy, and credential configuration in restricted enterprise networks.
- Containerized frontend builds that require npm registry access must pass npm configuration as a BuildKit secret instead of copying `.npmrc` or credentials into repository files or image layers.
- Local deployment service names must use the `docgen-*` prefix so logs, verification scripts, and operational evidence remain unambiguous across worktrees and compose projects.
- Accepted ADR technology stack decisions are authoritative for implementation planning and delivery; incomplete code or build files must not be treated as stack truth when ADRs disagree.
- For all open-source dependencies, implementation must first verify version availability in company-approved artifact repositories; select versions within the already accepted technology baseline whenever possible.
- Company-side quarantine or isolation of specific dependency versions is treated as normal governance behavior, not as justification to change technology selection.
- Documentation is kept verifiable. Links, OpenAPI contracts, examples, permission rules, and future architecture/test expectations should be validated by tools wherever practical.
- Documentation separates confirmed facts from pending questions. AI must not convert recommendations, assumptions, or common patterns into confirmed requirements.
- Documentation separates clear behavior specifications from ambiguity. If a request is not clear enough to produce a behavior specification, AI must ask for user confirmation and persist the confirmed answer in the owning source documents before task planning or implementation.
- Documentation preserves rebuildability. The project should contain enough structured information to rebuild the system from scratch without treating old code as the source of truth.

## Rebuildability Standard

At any point after implementation begins, the project should be able to answer these questions from documentation:

| Area | Documentation Must Explain |
| --- | --- |
| Product scope | What the system does, what it does not do, and which user roles it serves. |
| Domain model | Core objects, relationships, ownership, states, lifecycle rules, and invariants. |
| Permissions | Roles, group isolation, object access, API authorization, and audit access. |
| API contracts | Paths, request/response schemas, examples, error model, idempotency, and versioning rules. |
| Architecture | Module boundaries, runtime responsibilities, integration points, storage responsibilities, and asynchronous processing boundaries. |
| Security | Authentication, authorization, sensitive data handling, audit rules, encryption boundaries, and fail-closed behavior. |
| Data and storage | Durable records, generated assets, retention expectations, cache responsibilities, and object storage boundaries. |
| Testing | Contract tests, permission tests, rendering tests, lifecycle tests, audit tests, and regression expectations. |
| Behavior specifications | Actors, goals, triggers, preconditions, user actions, system responses, acceptance scenarios, boundary and exception behavior, observable evidence, and traceability. |
| Operations | Configuration boundaries, deployment assumptions, observability, retry, replay, and incident investigation expectations. |
| Decisions | Why important choices were made, which alternatives were rejected, and which items remain pending. |

If a future implementation cannot be rebuilt or meaningfully verified from the documentation, the documentation is incomplete.

## AI Development Rules

- AI must default to production-grade real functionality standards and reject demo-only closure for behavior that is claimed complete.
- AI must not claim completion for persistence-sensitive behavior unless durable storage boundaries are implemented and verified.
- AI must not claim deployment completion without redeploy and runtime verification evidence for user-visible changes.
- AI must read the relevant source-of-truth documents before changing requirements, design, architecture, API, permissions, or implementation.
- AI must derive planning and implementation from explicit behavior specifications for behavior-changing work.
- AI must update documentation first when a requested change affects behavior, architecture, permissions, security, API contracts, or operational assumptions.
- AI must ask the user for confirmation when actor, goal, trigger, preconditions, workflow, system response, acceptance scenarios, permissions, API behavior, or error handling are unclear.
- AI must persist confirmed behavior into requirements and affected product, domain, permission, API, architecture, or ADR documents before implementation-ready planning.
- AI must persist each confirmed technology choice in `docs/architecture/technology-stack-decisions.md` in the same turn as the confirmation.
- AI must treat unpersisted session content as non-authoritative; if a choice is not in repository documents, it must be treated as pending.
- AI must update indexes and cross-links whenever adding, moving, splitting, or retiring documents.
- AI must mark uncertain items as pending questions instead of writing them as confirmed behavior.
- AI must preserve user-confirmed requirements even when reorganizing documents.
- AI must not treat generated code as authoritative when it conflicts with documentation. The conflict must be surfaced and resolved through documentation.
- AI must keep implementation changes traceable to requirements, ADRs, API contracts, domain rules, or permission rules.
- AI must read accepted technology-stack ADRs before implementation and align runtime/framework choices with those ADRs unless the user explicitly reopens the decision.
- AI must apply the same repository-availability verification rule to every open-source component and avoid ad-hoc technology switches when individual versions are blocked.
- AI must inspect npm registry/proxy configuration before diagnosing frontend package-manager failures, must not assume a pnpm version problem without checking `.npmrc`, and must prefer the shared pnpm resolver over Corepack in scripts and workflows.
- AI must obtain user confirmation before changing an accepted technology selection (framework/runtime/container or equivalent baseline choice), even when dependency quarantine creates delivery friction.
- AI must prefer small, structured documentation changes that are easy to review, diff, and validate.
- AI must run `./scripts/validate-doc-structure.ps1` after changing documentation structure, indexes, ADRs, governance rules, or document-as-software behavior.

## Code Rebuild Rule

Implementation code is allowed to be replaced.

Documentation is not allowed to drift behind code. When implementation reveals a missing rule, ambiguous behavior, or incorrect assumption, update the documentation and capture any required decision before claiming the implementation is complete.

If code and documentation disagree, do not silently choose one. Identify the conflict, preserve the latest confirmed user decision, update the affected documents, and then align the implementation.

## Document Quality Rules

- Use stable headings and tables for concepts that will be referenced by future code or tests.
- Give each document one primary responsibility.
- Include related-document links when a document depends on requirements, ADRs, API contracts, domain rules, or permission rules.
- Keep functional requirements, non-functional requirements, and technology selections in separate owning documents unless a file is explicitly acting as an index or cross-reference.
- Keep normative statements precise enough that future tests or code reviews can verify them.
- Keep examples consistent with formal contracts and confirmed terminology.
- Remove or update stale wording when a decision changes.
- Do not hide open questions in prose. Keep them in explicit pending or open-question sections.

## Relationship to Other Governance

This charter defines the project operating philosophy. [Documentation Architecture](documentation-architecture.md) defines the documentation knowledge model and validation rules. [Documentation Governance](governance.md) defines the update rules and quality gates. [Git Workflow](git-workflow.md) defines version-control workflow. ADRs record durable decisions.

When these documents appear to conflict, resolve the conflict explicitly and update the affected governance documents in the same change.