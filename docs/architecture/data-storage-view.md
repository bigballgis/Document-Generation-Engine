---
id: DOC-ARCH-DATA-STORAGE-VIEW
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
related:
  - docs/architecture/runtime-view.md
  - docs/architecture/async-messaging-view.md
  - docs/architecture/security-view.md
---

# Data and Storage View

## Purpose

This view defines storage responsibility boundaries for durable data, cache data, generated assets, and sensitive data handling.

## Storage Responsibilities

| Storage Area | Owns | Must Not Store |
| --- | --- | --- |
| Relational Database | Master metadata, template metadata, lifecycle state, API policies, idempotency records, task metadata, audit records, release summaries | API credential secrets in plaintext, output encryption passwords, raw sensitive template variable values unless explicitly approved and protected |
| Object Storage or File Service | DOCX masters, generated DOCX/PDF files, previews, large rendering artifacts, temporary outputs subject to retention rules | Authorization decisions as the only source of truth, unbounded retention without policy |
| Redis or Cache Platform | AD Group successful resolution cache, short-lived coordination data, short-lived download/task acceleration data where approved | Failed AD Group resolution cache, expired authorization data as an authorization basis, sensitive plaintext passwords |
| Kafka | Task identifiers, event identifiers, safe summaries, policy versions, trace metadata, retry/dead-letter messages | Full request bodies, raw variables, generated documents, full download URLs, API credential secrets, encryption passwords, full AD Group member lists |
| Logs and Traces | Operational diagnostics, trace IDs, safe summaries, error categories | Sensitive plaintext, full request bodies, generated document content, encryption passwords |

## Retention Baseline

- Async tasks and generated results default to 7 days where confirmed by API requirements.
- Download URLs are short-lived and require authorization on use.
- Audit records are durable and must follow future retention and compliance policy.
- Cache entries must not outlive the confirmed authorization cache rules.

## Data Rules

- Sensitive data defaults to protected or summarized handling until explicitly classified otherwise.
- Generated documents are assets, not message payloads.
- Idempotency records store safe hashes and summaries rather than raw sensitive payloads.
- Authorization caches must fail closed when dependencies are unavailable and no valid cache exists.
- During E05 integration hardening, temporary in-memory storage seams may remain for local/test usage only and must be replaced or explicitly fail-closed for production paths.

## Pending Questions

- Whether any template test data may be stored after masking or must always be synthetic.
- Final rollout evidence and owner sequence for replacing remaining in-memory persistence adapters across runtime and governance paths.
