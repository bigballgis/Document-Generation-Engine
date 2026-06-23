---
id: DOC-ARCH-ASYNC-MESSAGING-VIEW
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/adr/async-processing/0008-api-async-task-lifecycle.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
  - docs/adr/authorization-security/0020-unified-authorization-and-sensitive-data-handling.md
related:
  - docs/architecture/runtime-view.md
  - docs/architecture/data-storage-view.md
  - docs/api/contract-outline.md
---

# Async Messaging View

## Purpose

This view defines how Kafka supports asynchronous document generation, rendering events, retry behavior, and audit/event fan-out.

## Kafka Responsibilities

Kafka is the asynchronous task and event backbone. It is used for:

- Accepted asynchronous generation tasks.
- Batch generation work item coordination.
- Rendering task dispatch and completion events.
- Retry and dead-letter flows.
- Audit or operational event fan-out where a durable audit store remains the query source.

Kafka is not used for:

- Storing generated documents.
- Carrying full request bodies or raw variables.
- Carrying API credential secrets or output encryption passwords.
- Carrying full AD Group membership lists.
- Acting as the durable audit query store.

## Message Payload Rules

Messages may contain:

- `taskId`, `batchId`, `itemId`, `documentId`, `templateId`, and release/version references.
- Policy version, route type, resolved release version, output format/mode summaries.
- Safe hashes such as request hash, variables hash, or items hash.
- Trace ID, audit ID, request ID, and safe context summary.
- Object storage references or resource identifiers that still require authorization before use.

Messages must not contain sensitive plaintext or generated file content.

## Reliability Rules

- Database state changes that must publish Kafka messages should use transactional outbox or an equivalent reliable publication pattern.
- Consumers must be idempotent and tolerate duplicate delivery.
- Retried messages must not bypass authorization, policy, or task state checks.
- Dead-letter messages must preserve enough safe metadata for investigation without leaking sensitive values.
- Replay procedures must be designed before production use.
- During E05 integration hardening, local or partial async seams must remain explicitly non-production until reliable publication and processing boundaries are fully wired.

## Pending Questions

- Schema governance mechanism, such as Schema Registry or an internal equivalent.
- Whether batch items are dispatched as one message per item or grouped messages.
- Operational replay approval and audit process.
- Final outbox/consumer rollout sequence and evidence checkpoints for full enterprise async integration closure.
