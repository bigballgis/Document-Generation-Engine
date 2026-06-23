---
id: ADR-0033
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0033"
topic: async-processing
related:
  - docs/architecture/README.md
  - docs/architecture/async-messaging-view.md
  - docs/architecture/runtime-view.md
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
  - docs/adr/async-processing/0008-api-async-task-lifecycle.md
---

# ADR 0033: Async Messaging and Task Retry Baseline

## Status

Accepted

## Context

Kafka is already accepted as the async backbone. The remaining decision log items need a stable baseline for message shape, delivery semantics, topic naming, consumer concurrency, and retry/DLT handling so the async messaging view can be implemented consistently.

## Decision

The confirmed async messaging and task retry baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Message consumption concurrency model | Kafka consumer group + partition concurrency | Consumer scaling follows partition concurrency. |
| Message deserialization format | JSON | Messages are serialized as JSON. |
| Kafka topic naming convention | business-domain.event-type.v1 | Topic names use the business-domain/event-type/version pattern. |
| Kafka delivery semantics | At-least-once | Duplicate delivery must be tolerated by consumers. |
| Kafka consumer failure handling | Retry + DLT (Dead Letter Topic) | Failed consumption uses bounded retry and a dead-letter topic. |
| Task retry and backoff strategy | Exponential backoff + max retries + dead-letter queue | Retry behavior is bounded and observable. |
| Task idempotency deduplication storage strategy | Redis with TTL idempotency keys | Redis is the deduplication storage for retry-safe task handling. |

These decisions are accepted as the messaging and task-retry foundation. They complement the existing async task lifecycle ADR and the technology stack baseline rather than replacing them.

## Consequences

- Consumers can be scaled and retried in a predictable way.
- Duplicate delivery becomes a design assumption instead of an error condition.
- Retry behavior and dead-letter handling are explicit and auditable.
- Topic naming and payload format are stable enough for future producer/consumer implementation.

## Alternatives Considered

- Use at-most-once delivery: rejected because generation and audit workflows need resilience over lossless retry safety.
- Use ad hoc topic names per feature: rejected because a consistent naming convention improves operations and discoverability.
- Avoid DLT handling: rejected because failed consumption needs bounded recovery and investigation paths.
- Store full request bodies in Kafka: rejected because the async backbone must not carry sensitive payloads.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Async Messaging View](../../architecture/async-messaging-view.md)
- [Runtime View](../../architecture/runtime-view.md)
- [Basic Technology Stack Baseline ADR](../technology-stack/0022-basic-technology-stack-baseline.md)