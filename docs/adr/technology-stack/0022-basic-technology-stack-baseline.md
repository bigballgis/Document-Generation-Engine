---
id: ADR-0022
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0022"
topic: technology-stack
related:
  - docs/architecture/README.md
  - docs/architecture/runtime-view.md
  - docs/architecture/async-messaging-view.md
---

# ADR 0022: Basic Technology Stack Baseline

## Status

Accepted

## Context

The project has been defined as an enterprise low-code document generation platform for banking financial letters. Its confirmed capabilities include template lifecycle governance, dynamic API generation, template-level authorization, AD Group checks, asynchronous and batch document generation, DOCX/PDF output, dynamic encryption, and auditability.

The project is now starting technical design for the base technology stack while keeping product, domain, and permission documents technology-neutral. The user confirmed a preference for Vue and Kafka so the platform stays consistent with the team's internal systems.

This ADR records the confirmed base stack decisions and separates them from implementation choices that still require explicit confirmation before scaffolding source code or environment files.

## Decision

The confirmed baseline decisions are:

| Area | Decision | Notes |
| --- | --- | --- |
| Management frontend | Vue 3 + TypeScript + Vite | Applies to the administration, template management, API management, approval, and audit UI. The UI component library remains pending and should follow the team's standard if one exists. |
| Asynchronous task and event backbone | Kafka | Used for document generation tasks, batch orchestration, rendering events, audit event publication, and other asynchronous workflows where event traceability matters. |
| Document rendering boundary | Separate rendering worker or rendering service | The core API service owns authorization, lifecycle, task state, idempotency, audit, and orchestration. Rendering workers handle DOCX assembly, PDF conversion, encryption, and preview generation. |
| Docs-as-code placement | Technology decisions are recorded in ADRs | Product requirements, domain model, and permission matrix remain technology-neutral unless a technology choice changes externally visible behavior or security rules. |

Kafka usage follows these constraints:

- Kafka messages carry resource identifiers, task identifiers, state transitions, safe summaries, and trace metadata only.
- Kafka messages must not contain API credential secrets, AD Group member lists, DOCX/PDF encryption passwords, raw template variable values, full request bodies, generated documents, or full download URLs.
- Generated documents, previews, and large intermediate files are stored in object storage or an equivalent file service; Kafka carries references or resource identifiers.
- Database state changes that need Kafka publication use a transactional outbox pattern or an equivalent reliable publication mechanism.
- Kafka consumers are idempotent and must tolerate retries, duplicate delivery, and out-of-order operational events where applicable.
- Failed asynchronous processing uses bounded retry handling and a dead-letter topic or equivalent failure channel.

The following implementation choices are recommended candidates but are not finalized by this ADR:

| Area | Candidate Direction | Confirmation Needed |
| --- | --- | --- |
| Backend runtime | Java 21 + Spring Boot 3 | Confirm whether this matches internal platform standards. |
| Database | PostgreSQL-compatible baseline, or a bank-approved relational database such as Oracle or a domestic commercial database | Confirm target database and compatibility constraints. |
| Cache | Redis | Confirm internal cache platform and high-availability requirements. |
| Object storage | S3-compatible object storage or internal file storage service | Confirm approved storage platform, retention, and encryption requirements. |
| Deployment | Docker and Kubernetes where available | Confirm whether the target runtime is Kubernetes, virtual machines, or an internal PaaS. |
| Document engine | Separate POC for DOCX assembly, PDF conversion, encryption, and fidelity checks | Confirm whether commercial components such as Aspose are allowed, or whether open-source/internal components are required. |

No framework-specific source tree, build configuration, deployment configuration, or local environment template should be added until the pending implementation choices above are confirmed or covered by follow-up ADRs.

## Consequences

- The frontend and asynchronous/event backbone align with the team's existing technology ecosystem.
- Kafka becomes a platform-level integration and processing backbone, not a place to store sensitive payloads or generated files.
- Rendering can scale independently from the API service and can be replaced after document-engine POC results without rewriting the product lifecycle model.
- The docs-as-code source of truth stays clean: product documents describe user-visible behavior, while ADRs describe technology decisions.
- Additional infrastructure choices remain visible as pending decisions instead of being silently treated as accepted.

## Alternatives Considered

- React or Angular for the frontend: not selected for the baseline because Vue better matches the user's stated team consistency constraint.
- RabbitMQ or RocketMQ for asynchronous processing: not selected for the baseline because Kafka better matches the user's stated internal-stack alignment.
- In-process asynchronous execution only: rejected as the baseline because batch generation, rendering workloads, retries, and audit event publication need stronger operational decoupling.
- Passing full request payloads or files through Kafka: rejected because it conflicts with sensitive-data handling, message-size control, replay safety, and document retention boundaries.
- Embedding rendering directly inside the core API service: rejected as the baseline because rendering engine choice, PDF conversion, encryption, and fidelity checks carry separate scaling and replacement risks.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [API Contract Outline](../../api/contract-outline.md)
- [API Async Task Lifecycle ADR](../async-processing/0008-api-async-task-lifecycle.md)
- [Structured Authoring and Rendering Boundary ADR](../rendering-authoring/0019-structured-authoring-and-rendering-boundary.md)
- [Unified Authorization and Sensitive Data Handling ADR](../authorization-security/0020-unified-authorization-and-sensitive-data-handling.md)