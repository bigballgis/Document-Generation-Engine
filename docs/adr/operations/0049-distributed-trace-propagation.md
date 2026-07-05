---
id: ADR-0049
title: Distributed Trace Propagation Strategy
status: Proposed
date: 2026-07-05
deciders: architecture, backend-engineer
related:
  - docs/adr/operations/0047-distributed-tracing-otlp-baseline.md
  - docs/adr/operations/0030-operational-platform-baseline.md
  - docs/plan/detail/LRP-D-ops-observability.md
---

# ADR-0049 — Distributed Trace Propagation Strategy

## Context

The platform already has a trace-id baseline (ADR-0047): Micrometer Tracing + OTLP export,
`TraceIdProvider` bridges the active span to the `X-Trace-Id` response header, and logback
correlates `%X{traceId}`. What is **not** yet decided is how the trace-id propagates across
the async boundary (Kafka / `@Async` / scheduled jobs) and into MinIO/storage operations.

Without propagation, a generation request that spawns an async batch task loses the trace-id
at the boundary — the async task's logs cannot be correlated back to the triggering request.

## Decision

Propagate the trace-id across all async boundaries:

1. **`@Async` methods**: the `MdcTaskDecorator` (already in place per SOR-A06) copies the
   MDC (including `traceId`) from the caller thread to the worker thread.
2. **Kafka producer**: set the `X-Trace-Id` header on outgoing `AsyncBatchTaskMessage`
   records. Use a `ProducerInterceptor` (or set the header in `KafkaAsyncBatchTaskDispatcher`)
   so the trace-id travels with the message.
3. **Kafka consumer**: `AsyncBatchTaskKafkaConsumer` reads the `X-Trace-Id` header and
   restores it into MDC before processing the message. The consumer creates a child span
   linked to the producer span (Micrometer's KafkaTracing bridges this automatically when
   `spring.kafka.listener` is configured).
4. **Scheduled jobs** (`@Scheduled` + ShedLock): there is no parent trace; the scheduler
   creates a new trace-id at the start of the job and clears MDC at the end. The
   `@SchedulerLock` name is added as a span tag.
5. **MinIO / object storage**: storage operations do NOT propagate the trace-id into object
   metadata (MinIO object metadata is user-controlled and not a tracing channel). The trace
   is logged at the storage call site only.

## Consequences

- **Positive:** end-to-end trace correlation from HTTP request → async task → Kafka consumer
  → audit log. A single trace-id finds every log line for a generation request.
- **Negative:** Kafka messages now carry a header; consumers must tolerate its presence
  (they already do — headers are optional).
- **Neutral:** the OTLP exporter is disabled in local/test (ADR-0047); propagation still
  works via MDC even when OTLP is off.

## Implementation notes

- The `KafkaAsyncBatchTaskDispatcher` already builds the producer record; add the
  `X-Trace-Id` header there.
- The `AsyncBatchTaskKafkaConsumer` already processes messages; add an MDC restore in the
  `@KafkaListener` method (or a `ConsumerInterceptor`).
- The Micrometer KafkaTracing bridge auto-creates child spans when the producer/consumer
  interceptors are registered — verify they are on the classpath.

## Alternatives considered

- **No propagation (status quo):** rejected — async tasks cannot be correlated to the
  triggering request; troubleshooting is blind.
- **Propagate via message body:** rejected — pollutes the message schema; tracing is a
  transport concern, not a payload concern.
- **Propagate via a custom correlation-id (not the trace-id):** rejected — duplicates the
  trace-id; one correlation identifier is enough.
