---
id: DOC-ARCH-SYSTEM-CONTEXT
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/product/PRD.md
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
  - docs/api/contract-outline.md
related:
  - docs/architecture/README.md
  - docs/architecture/security-view.md
  - docs/architecture/runtime-view.md
---

# System Context

## Purpose

This view defines the system boundary and external actors for architecture and future implementation planning.

## System Boundary

The platform is an enterprise low-code document generation platform for banking financial letters.

Inside the system boundary:

- Master document asset management.
- Template composition and release governance.
- API management configuration.
- Dynamic generation API contract publication.
- Synchronous, asynchronous, and batch document generation orchestration.
- DOCX/PDF output generation coordination.
- Dynamic output encryption coordination.
- Template-level authorization, API credential checks, AD Group authorization checks, and audit records.

Outside the system boundary:

- Business lifecycle management for individual financial letters.
- External customer, loan, account, and transaction systems.
- External directory synchronization mechanics beyond platform AD Group resolution and cache behavior.
- External document archive or downstream retention systems unless explicitly integrated later.

## External Actors and Systems

| Actor or System | Relationship |
| --- | --- |
| 信贷客户经理 | Core business persona; in v1 consumes generated financial letters through upstream business-system workflows that call platform APIs, not through a platform-hosted formal generation portal. |
| 管理员 | Performs global or group-scoped management operations. |
| 母版设计人员 | Creates and maintains DOCX master documents and anchors. |
| 模板编排人员 | Creates and maintains templates, variables, and composition rules. |
| 测试人员 | Executes test pass/fail decisions for release governance. |
| 审批人员 | Executes approval pass/fail decisions for release governance. |
| 审计管理员 | Views audit records according to permission rules. |
| API 调用方 | Calls authorized dynamic generation APIs using API credentials and access account context. |
| AD / directory service | Provides AD Group membership data used for API authorization resolution. |
| Object storage or file service | Stores master documents, generated documents, previews, and large rendering artifacts. |
| Kafka | Carries asynchronous task and event messages according to the async messaging view. |

## Context Rules

- API callers do not bypass template-level authorization.
- Directory data is not treated as owned by the platform; the platform owns only resolution, caching, authorization decisions, and audit summaries.
- Generated documents and large artifacts move through object storage or equivalent file services, not Kafka messages.
- Product behavior, domain ownership, permission rules, and API contracts remain defined by their source-of-truth documents.

## Pending Questions

- Whether external archive or records-management integration is in scope for v1.
