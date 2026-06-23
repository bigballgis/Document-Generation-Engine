---
id: ADR-0026
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - documentation-governance
adrNumber: "0026"
topic: documentation-governance
related:
  - docs/adr/README.md
  - docs/documentation-architecture.md
  - scripts/preview-adr-frontmatter-backfill.ps1
  - scripts/validate-doc-structure.ps1
---

# ADR 0026: ADR Metadata, Topic Taxonomy, and Migration Plan

## Status

Accepted

## Context

The ADR set grew from a flat list of decision files into the durable decision layer for product, API, security, lifecycle, architecture, and documentation governance. A flat filename list alone is not enough for AI retrieval, validation, ownership, or future rebuildability.

The project needs structured ADR metadata and a stable topic taxonomy before physical organization or stricter validation can be trusted.

## Decision

Adopt ADR frontmatter metadata and the ADR topic taxonomy defined in [ADR Index](../README.md).

Each numbered ADR should carry YAML frontmatter with these fields: `id`, `type`, `status`, `sourceOfTruth`, `owners`, `adrNumber`, `topic`, and `related`.

The accepted topics are:

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

## Migration Plan

The metadata migration path is:

1. Add metadata and validation support while keeping ADR files flat.
2. Backfill frontmatter into existing ADRs without changing filenames or paths.
3. Run [ADR Frontmatter Backfill Preview](../../../scripts/preview-adr-frontmatter-backfill.ps1) to inspect generated metadata without writing files.
4. Review the topic distribution and confirm whether physical subdirectories are still needed.
5. If physical migration is confirmed, move ADRs by topic in one traceable change and update all links and indexes.
6. Enable stricter validation only after the migration has no broken links and no unindexed ADRs.

ADR 0027 records the separate confirmed decision to perform the physical topic-directory migration after this metadata plan was accepted.

## Frontmatter Backfill Map

This map is accepted for metadata backfill. It does not change the accepted meaning of existing ADRs. Physical movement is governed separately by [ADR 0027](0027-adr-topic-directory-organization.md).

| ADR | Topic | Owners | Source-of-Truth Anchors |
| --- | --- | --- | --- |
| [0001-output-encryption.md](../authorization-security/0001-output-encryption.md) | `authorization-security` | `security`, `api` | `docs/security/permission-matrix.md`, `docs/api/contract-outline.md`, `docs/domain/domain-model.md` |
| [0002-api-management-template-scope.md](../api-management/0002-api-management-template-scope.md) | `api-management` | `api`, `template-governance` | `docs/product/PRD.md`, `docs/domain/domain-model.md`, `docs/api/contract-outline.md` |
| [0003-api-routing-and-batch-overrides.md](../api/0003-api-routing-and-batch-overrides.md) | `api` | `api` | `docs/api/contract-outline.md`, `docs/api/openapi-v1.yaml`, `docs/domain/domain-model.md` |
| [0004-api-idempotency-strategy.md](../api/0004-api-idempotency-strategy.md) | `api` | `api` | `docs/api/contract-outline.md`, `docs/api/openapi-v1.yaml` |
| [0005-api-response-delivery-and-download-security.md](../api/0005-api-response-delivery-and-download-security.md) | `api` | `api`, `security` | `docs/api/contract-outline.md`, `docs/security/permission-matrix.md`, `docs/architecture/security-view.md` |
| [0006-api-error-model.md](../api/0006-api-error-model.md) | `api` | `api` | `docs/api/contract-outline.md`, `docs/api/openapi-v1.yaml` |
| [0007-api-management-change-governance.md](../api-management/0007-api-management-change-governance.md) | `api-management` | `api`, `audit` | `docs/product/PRD.md`, `docs/security/permission-matrix.md`, `docs/api/contract-outline.md` |
| [0008-api-async-task-lifecycle.md](../async-processing/0008-api-async-task-lifecycle.md) | `async-processing` | `api`, `architecture` | `docs/api/contract-outline.md`, `docs/architecture/async-messaging-view.md`, `docs/architecture/runtime-view.md` |
| [0009-api-credential-lifecycle.md](../api-management/0009-api-credential-lifecycle.md) | `api-management` | `api`, `security` | `docs/security/permission-matrix.md`, `docs/api/contract-outline.md` |
| [0010-ad-group-authorization-resolution.md](../authorization-security/0010-ad-group-authorization-resolution.md) | `authorization-security` | `security` | `docs/security/permission-matrix.md`, `docs/domain/domain-model.md`, `docs/architecture/security-view.md` |
| [0011-api-schema-and-response-envelope.md](../api/0011-api-schema-and-response-envelope.md) | `api` | `api` | `docs/api/contract-outline.md`, `docs/api/openapi-v1.yaml` |
| [0012-api-enum-and-identifier-naming.md](../api/0012-api-enum-and-identifier-naming.md) | `api` | `api` | `docs/api/contract-outline.md`, `docs/api/openapi-v1.yaml` |
| [0013-api-contract-visibility-audit-and-context.md](../api/0013-api-contract-visibility-audit-and-context.md) | `api` | `api`, `audit` | `docs/api/contract-outline.md`, `docs/security/permission-matrix.md` |
| [0014-api-openapi-v1-contract-scope.md](../api/0014-api-openapi-v1-contract-scope.md) | `api` | `api` | `docs/api/openapi-v1.yaml`, `docs/api/README.md` |
| [0015-template-release-verifiability.md](../template-lifecycle/0015-template-release-verifiability.md) | `template-lifecycle` | `template-governance` | `docs/product/PRD.md`, `docs/domain/domain-model.md`, `docs/domain/lifecycle-review.md` |
| [0016-api-management-ui-and-audit-format.md](../api-management/0016-api-management-ui-and-audit-format.md) | `api-management` | `api`, `audit` | `docs/product/PRD.md`, `docs/security/permission-matrix.md`, `docs/api/contract-outline.md` |
| [0017-template-lifecycle-recovery-deprecation-import.md](../template-lifecycle/0017-template-lifecycle-recovery-deprecation-import.md) | `template-lifecycle` | `template-governance` | `docs/product/PRD.md`, `docs/domain/domain-model.md`, `docs/domain/lifecycle-review.md` |
| [0018-master-review-state-and-impact-analysis.md](../template-lifecycle/0018-master-review-state-and-impact-analysis.md) | `template-lifecycle` | `template-governance` | `docs/product/PRD.md`, `docs/domain/domain-model.md`, `docs/domain/lifecycle-review.md` |
| [0019-structured-authoring-and-rendering-boundary.md](../rendering-authoring/0019-structured-authoring-and-rendering-boundary.md) | `rendering-authoring` | `template-authoring`, `rendering` | `docs/product/authoring-rendering-first-principles-review.md`, `docs/domain/domain-model.md`, `docs/architecture/module-boundaries.md` |
| [0020-unified-authorization-and-sensitive-data-handling.md](../authorization-security/0020-unified-authorization-and-sensitive-data-handling.md) | `authorization-security` | `security` | `docs/security/permission-matrix.md`, `docs/architecture/security-view.md`, `docs/domain/domain-model.md` |
| [0021-template-testing-approval-release-governance.md](../template-lifecycle/0021-template-testing-approval-release-governance.md) | `template-lifecycle` | `template-governance` | `docs/product/PRD.md`, `docs/domain/domain-model.md`, `docs/domain/lifecycle-review.md` |
| [0022-basic-technology-stack-baseline.md](../technology-stack/0022-basic-technology-stack-baseline.md) | `technology-stack` | `architecture` | `docs/architecture/README.md`, `docs/architecture/runtime-view.md`, `docs/architecture/async-messaging-view.md` |
| [0023-document-as-software-operating-model.md](0023-document-as-software-operating-model.md) | `documentation-governance` | `documentation-governance` | `docs/document-as-software.md`, `docs/governance.md`, `.github/copilot-instructions.md` |
| [0024-documentation-knowledge-architecture.md](0024-documentation-knowledge-architecture.md) | `documentation-governance` | `documentation-governance` | `docs/documentation-architecture.md`, `docs/governance.md`, `scripts/validate-doc-structure.ps1` |
| [0025-architecture-documentation-layer.md](../architecture/0025-architecture-documentation-layer.md) | `architecture` | `architecture`, `documentation-governance` | `docs/architecture/README.md`, `docs/documentation-architecture.md`, `docs/document-as-software.md` |

Backfill rule of thumb:

- Use `sourceOfTruth: true` for accepted ADRs unless an ADR is superseded or explicitly demoted to supporting context.
- Use the existing ADR status heading as the metadata `status` value.
- Keep `adrNumber` synchronized with the filename and `id` in the form `ADR-0000`.
- Treat source-of-truth anchors as the starting `related` set, then add any ADR-specific links already present in the document.
- Do not add new product behavior, permissions, or architecture facts while backfilling metadata.

## Consequences

- AI agents can classify and retrieve ADRs without relying only on filenames.
- Existing ADRs can be searched and validated by metadata after backfill.
- Future ADR migration becomes a controlled operation instead of cosmetic folder reshuffling.
- Physical ADR reorganization is now governed by [ADR 0027](0027-adr-topic-directory-organization.md).

## Deferred Decisions

- Whether ADR owner labels need a separately governed vocabulary beyond the owner labels used in this metadata backfill.
- Whether future topic additions require a dedicated ADR or can be handled through ADR index governance.

## Alternatives Considered

- Move ADRs into topic folders immediately: rejected at the time because it risked link churn before metadata and validation were ready.
- Keep only the flat directory forever: rejected after metadata was accepted and the user confirmed physical topic-directory migration.
- Use filename prefixes only: rejected because filenames do not express status, ownership, related documents, or source-of-truth scope.
- Require full strict metadata immediately: rejected because many existing documents predate the metadata standard.

## Related Documents

- [Document as Software Charter](../../document-as-software.md)
- [Documentation Architecture](../../documentation-architecture.md)
- [ADR Index](../README.md)
- [ADR Template](../0000-template.md)
- [ADR Topic Directory Organization](0027-adr-topic-directory-organization.md)
- [Documentation Structure Validation Script](../../../scripts/validate-doc-structure.ps1)
- [ADR Frontmatter Backfill Preview](../../../scripts/preview-adr-frontmatter-backfill.ps1)