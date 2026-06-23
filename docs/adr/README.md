# ADR Index

Architecture Decision Records capture durable product, architecture, governance, and implementation decisions.

## ADR Metadata

New ADRs should include YAML frontmatter before the title so humans, AI agents, and scripts can classify decisions without relying only on filenames.

Recommended fields for new ADRs:

| Field | Purpose |
| --- | --- |
| `id` | Stable ADR identifier in the form `ADR-0000` |
| `type` | Document type, normally `ADR` |
| `status` | `Draft`, `Proposed`, `Accepted`, `Deprecated`, or `Superseded` |
| `sourceOfTruth` | Whether the ADR currently owns an accepted decision |
| `owners` | Owning area for review and future updates |
| `adrNumber` | Four-digit ADR number matching the filename |
| `topic` | Classification metadata from the topic taxonomy |
| `related` | Documents that should be checked together with the ADR |

Existing ADRs have been backfilled with metadata frontmatter. Numbered ADRs are physically organized under `docs/adr/<topic>/` by [ADR 0027](./documentation-governance/0027-adr-topic-directory-organization.md). The ADR root keeps this index and the template.

## ADR Topic Taxonomy

Topic metadata is used for classification and ADR placement. It does not replace related-document links or source-of-truth ownership.

| Topic | Use For |
| --- | --- |
| `api` | API schema, response model, error model, route behavior, idempotency, and contract visibility |
| `api-management` | API credential, policy, template-level API configuration, and API management UI behavior |
| `architecture` | System, module, runtime, storage, messaging, and security architecture views |
| `async-processing` | Kafka, asynchronous task lifecycle, retry, replay, batch, and DLQ behavior |
| `authorization-security` | AD Group, authorization, permission isolation, sensitive data, audit, and encryption decisions |
| `documentation-governance` | Document as software, documentation architecture, validation, indexes, and AI workflow |
| `operations` | Deployment, environment, observability, retention, and operational recovery decisions |
| `rendering-authoring` | Structured authoring, rendering boundaries, DOCX/PDF fidelity, and document output rules |
| `technology-stack` | Framework, language, infrastructure, database, cache, object storage, and platform choices |
| `template-lifecycle` | Master/template lifecycle, release gates, testing, approval, import, recovery, and deprecation |

## When to Create an ADR

Create an ADR when a decision affects future design or implementation, including:

- Template lifecycle.
- Versioning model.
- Dynamic API contract.
- API authorization.
- AD Group integration.
- Environment migration.
- DOCX/PDF encryption.
- Rendering strategy.
- Permission and group isolation.

## ADR List

| ADR | Status | Topic |
| --- | --- | --- |
| [0001-output-encryption.md](./authorization-security/0001-output-encryption.md) | Accepted | DOCX/PDF dynamic output encryption |
| [0002-api-management-template-scope.md](./api-management/0002-api-management-template-scope.md) | Accepted | Template-level API management configuration scope |
| [0003-api-routing-and-batch-overrides.md](./api/0003-api-routing-and-batch-overrides.md) | Accepted | API route versioning, default route, and batch item overrides |
| [0004-api-idempotency-strategy.md](./api/0004-api-idempotency-strategy.md) | Accepted | API idempotency strategy |
| [0005-api-response-delivery-and-download-security.md](./api/0005-api-response-delivery-and-download-security.md) | Accepted | API response delivery and download security |
| [0006-api-error-model.md](./api/0006-api-error-model.md) | Accepted | API error model |
| [0007-api-management-change-governance.md](./api-management/0007-api-management-change-governance.md) | Accepted | API management configuration change governance |
| [0008-api-async-task-lifecycle.md](./async-processing/0008-api-async-task-lifecycle.md) | Accepted | API async task lifecycle |
| [0009-api-credential-lifecycle.md](./api-management/0009-api-credential-lifecycle.md) | Accepted | API credential lifecycle |
| [0010-ad-group-authorization-resolution.md](./authorization-security/0010-ad-group-authorization-resolution.md) | Accepted | AD Group authorization resolution |
| [0011-api-schema-and-response-envelope.md](./api/0011-api-schema-and-response-envelope.md) | Accepted | API schema format, request field naming, and response envelope |
| [0012-api-enum-and-identifier-naming.md](./api/0012-api-enum-and-identifier-naming.md) | Accepted | API enum values and identifier naming |
| [0013-api-contract-visibility-audit-and-context.md](./api/0013-api-contract-visibility-audit-and-context.md) | Accepted | API contract visibility, audit summary, and context fields |
| [0014-api-openapi-v1-contract-scope.md](./api/0014-api-openapi-v1-contract-scope.md) | Accepted | OpenAPI v1 contract scope, discovery paths, auth headers, and trace ID handling |
| [0015-template-release-verifiability.md](./template-lifecycle/0015-template-release-verifiability.md) | Accepted | Template verifiability and release gate |
| [0016-api-management-ui-and-audit-format.md](./api-management/0016-api-management-ui-and-audit-format.md) | Accepted | API management UI structure, policy versioning, preview blocking, and audit format |
| [0017-template-lifecycle-recovery-deprecation-import.md](./template-lifecycle/0017-template-lifecycle-recovery-deprecation-import.md) | Accepted | Template lifecycle recovery, deprecation, and import conflict rules |
| [0018-master-review-state-and-impact-analysis.md](./template-lifecycle/0018-master-review-state-and-impact-analysis.md) | Accepted | Master review state and impact analysis |
| [0019-structured-authoring-and-rendering-boundary.md](./rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | Accepted | Structured authoring, controlled rich text, and DOCX rendering boundary |
| [0020-unified-authorization-and-sensitive-data-handling.md](./authorization-security/0020-unified-authorization-and-sensitive-data-handling.md) | Accepted | Unified authorization and sensitive data handling |
| [0021-template-testing-approval-release-governance.md](./template-lifecycle/0021-template-testing-approval-release-governance.md) | Accepted | Template testing, approval, and release governance |
| [0022-basic-technology-stack-baseline.md](./technology-stack/0022-basic-technology-stack-baseline.md) | Accepted | Basic technology stack baseline |
| [0023-document-as-software-operating-model.md](./documentation-governance/0023-document-as-software-operating-model.md) | Accepted | Document as software operating model |
| [0024-documentation-knowledge-architecture.md](./documentation-governance/0024-documentation-knowledge-architecture.md) | Accepted | Documentation knowledge architecture and validation |
| [0025-architecture-documentation-layer.md](./architecture/0025-architecture-documentation-layer.md) | Accepted | Architecture documentation layer |
| [0026-adr-metadata-taxonomy-and-migration-plan.md](./documentation-governance/0026-adr-metadata-taxonomy-and-migration-plan.md) | Accepted | ADR metadata, topic taxonomy, and migration plan |
| [0027-adr-topic-directory-organization.md](./documentation-governance/0027-adr-topic-directory-organization.md) | Accepted | ADR topic directory organization |
| [0028-backend-platform-stack-baseline.md](./technology-stack/0028-backend-platform-stack-baseline.md) | Accepted | Backend platform stack baseline |
| [0029-frontend-application-stack-baseline.md](./technology-stack/0029-frontend-application-stack-baseline.md) | Accepted | Frontend application stack baseline |
| [0030-operational-platform-baseline.md](./operations/0030-operational-platform-baseline.md) | Accepted | Operational platform baseline |
| [0031-api-platform-hardening-baseline.md](./api/0031-api-platform-hardening-baseline.md) | Accepted | API platform hardening baseline |
| [0032-identity-and-security-operations-baseline.md](./authorization-security/0032-identity-and-security-operations-baseline.md) | Accepted | Identity and security operations baseline |
| [0033-async-messaging-and-task-retry-baseline.md](./async-processing/0033-async-messaging-and-task-retry-baseline.md) | Accepted | Async messaging and task retry baseline |
| [0034-data-and-storage-operations-baseline.md](./technology-stack/0034-data-and-storage-operations-baseline.md) | Accepted | Data and storage operations baseline |
| [0035-implementation-realization-and-quality-gate-baseline.md](./technology-stack/0035-implementation-realization-and-quality-gate-baseline.md) | Accepted | Implementation realization and quality gate baseline |

Use [0000-template.md](./0000-template.md) when creating new ADRs. Place new numbered ADRs in the directory matching their `topic` frontmatter.
