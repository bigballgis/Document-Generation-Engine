---
id: ADR-0049
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-05
acceptedDate: 2026-07-12
deciders: architecture, backend-engineer
owners:
  - operations
  - runtime
adrNumber: "0049"
topic: operations
related:
  - docs/adr/operations/0047-distributed-tracing-otlp-baseline.md
  - docs/adr/operations/0030-operational-platform-baseline.md
  - docs/adr/async-processing/0033-async-messaging-and-task-retry-baseline.md
  - docs/behavior/lrp-d4-trace-propagation.md
  - docs/plan/detail/LRP-D-ops-observability.md
  - docs/plan/system-optimization-review-2026-07.md
---

# ADR 0049: Distributed Trace Propagation Strategy

## Status

Accepted (2026-07-12) — LR-D4 / Task Master #41 docs-first gate.
Behavior note: [lrp-d4-trace-propagation.md](../../behavior/lrp-d4-trace-propagation.md)
(`bdd_readiness: not-applicable` — internal observability plumbing).

> **Plan wording note:** LRP-D §LR-D4 historically said «adopt Micrometer Tracing
> bridge now vs defer». That choice is already **Accepted** in
> [ADR-0047](./0047-distributed-tracing-otlp-baseline.md) (SOR-A06 Done).
> This ADR does **not** re-decide the bridge. It decides **cross-boundary
> `traceId` propagation** for v1.

## Context

[ADR-0047](./0047-distributed-tracing-otlp-baseline.md) established the tracing
**baseline**: Micrometer Tracing + OTLP bridge on the classpath,
`TraceIdProvider` precedence (active span → `X-Trace-Id` → UUID),
`TraceIdMdcFilter` for request MDC + response header, profile-gated OTLP export,
and log patterns with `%X{traceId}`.

What remained undecided (program §1 finding 12 / LR-D4) is how the same
`traceId` survives **async and messaging boundaries**:

- `asyncTaskExecutor` worker threads (no MDC task decorator on `AsyncConfig` yet)
- Kafka producer → consumer for async batch tasks (no `X-Trace-Id` record header yet)
- Scheduled jobs and MinIO call sites (correlation policy)

Without propagation, a generation request that spawns an async batch task loses
correlation at the boundary — worker/consumer logs cannot be joined to the
triggering request via MDC `traceId`.

### Relationship to ADR-0047 (explicit, no rewrite)

| Concern | Owning ADR | LR-D4 implication |
| --- | --- | --- |
| Micrometer Tracing + OTLP bridge, sampling, prod endpoint | **0047 Accepted** | Do not re-open; do not edit 0047 decision text |
| Envelope `metadata.traceId` / response `X-Trace-Id` contract | **0047** + API envelope docs | **Preserve** — propagation must not change semantics |
| Cross-boundary MDC / Kafka header / scheduler correlation | **This ADR (0049)** | Implement in LR-D4 |
| Full Zipkin/Tempo (or other) collector stack in Docker | Out of scope for D4 | Remains 0047 profile-gated OTLP only |

## Decision

Propagate the platform **`traceId`** (same identifier used by envelopes, audit,
and `X-Trace-Id`) across async boundaries. **v1 scope = correlation via MDC and
Kafka headers** — not a new span-export backend.

### Confirmed for LR-D4 (v1)

1. **HTTP → MDC (already present):** `TraceIdMdcFilter` continues to put
   `traceId` into MDC and set response `X-Trace-Id` using `TraceIdProvider`.
2. **`asyncTaskExecutor` / `@Async`:** attach a `TaskDecorator` (e.g.
   `MdcTaskDecorator`) that copies the caller-thread MDC (at least `traceId`)
   onto the worker thread and clears MDC after the task. **Gap today:**
   `AsyncConfig` has no decorator — backend must add it.
3. **Kafka producer:** set record header `X-Trace-Id` (same name as the HTTP
   header) on outgoing async-batch messages (prefer setting the header in
   `KafkaAsyncBatchTaskDispatcher` when building/sending the record). Tracing
   is a **transport** concern — do **not** put `traceId` into
   `AsyncBatchTaskMessage` body/schema.
4. **Kafka consumer:** read header `X-Trace-Id` (tolerate absence), restore
   MDC `traceId` before processing, clear MDC in `finally`. Prefer restoring in
   `AsyncBatchTaskKafkaConsumer` (or a dedicated interceptor) so Scenario B is
   testable without a collector.
5. **Envelope / API contract:** do not change `metadata.traceId` or
   `X-Trace-Id` response semantics. Propagation only carries the existing id
   across threads and Kafka.

### Confirmed policy (non-HTTP paths)

6. **Scheduled jobs** (`@Scheduled` + ShedLock): no parent request; create a
   fresh `traceId` at job start (via `TraceIdProvider` / MDC put) and clear MDC
   at end. Adding the `@SchedulerLock` name as a Micrometer span tag is
   **optional** and must not require new dependencies for D4 exit.
7. **MinIO / object storage:** do **not** write `traceId` into object metadata.
   Log at the call site only (existing MDC / provider).

### Explicitly out of scope for LR-D4

- Standing up Zipkin, Tempo, Jaeger UI, or any full collector stack in the
  acceptance compose project.
- Changing OTLP enablement/sampling beyond ADR-0047.
- Product UI “trace explorer” or new public API fields for traces.
- Requiring Micrometer KafkaTracing auto child-span wiring as a D4 exit gate
  (nice-to-have only if already available on the BOM classpath **without** new
  dependencies; acceptance Scenarios A/B are **MDC + header** assertions).

## Consequences

- **Positive:** end-to-end log correlation from HTTP request → async executor →
  Kafka consumer using one `traceId`.
- **Negative:** Kafka records carry an optional header; consumers must tolerate
  presence/absence (headers are already optional).
- **Neutral:** OTLP remains disabled in local/test (ADR-0047); MDC/header
  propagation still works when export is off.
- **Implementer gap:** decorator + Kafka header set/restore + tests (Scenarios A/B)
  are **not** present at acceptance of this ADR — code follows docs-first.

## Dependency policy

- **Reuse ADR-0047 classpath:** `micrometer-tracing-bridge-otel`,
  `opentelemetry-exporter-otlp` (already in `backend/pom.xml`).
- **Default for LR-D4:** implement propagation with **no new Maven dependencies**
  (MDC copy + Kafka `RecordHeader` / consumer header read).
- **If** a future slice needs Micrometer Kafka instrumentation artifacts beyond
  what Spring Boot 3.x BOM already pulls transitively, verify availability in
  company-approved repositories first per
  `.cursor/rules/tech-stack-guardrails.mdc` — do not ad-hoc switch stacks.
- Do **not** add Zipkin/Brave reporters or vendor APM SDKs in this slice.

## Alternatives considered

| Alternative | Why not chosen |
| --- | --- |
| No propagation (status quo) | Rejected — async/Kafka paths remain uncorrelated (finding 12) |
| Put `traceId` in message body | Rejected — pollutes schema; tracing is transport metadata |
| Separate custom correlation-id | Rejected — duplicates envelope/`X-Trace-Id` identifier |
| Re-open «adopt Micrometer bridge?» | Rejected — already Accepted in ADR-0047 (SOR-A06) |
| Full Zipkin/Tempo in D4 | Rejected — plan Do NOT; D4 = decision + minimal propagation |

## Implementation notes (for backend-engineer)

| Seam | Status at ADR accept | Required for D4 |
| --- | --- | --- |
| `TraceIdProvider` + `TraceIdMdcFilter` + log `%X{traceId}` | Present (0047) | Preserve |
| `AsyncConfig` `TaskDecorator` / MDC copy | **Missing** | Add |
| `KafkaAsyncBatchTaskDispatcher` `X-Trace-Id` header | **Missing** | Add |
| `AsyncBatchTaskKafkaConsumer` MDC restore | **Missing** | Add |
| Tests: async MDC carries T; Kafka round-trip MDC | **Missing** | Add (Scenarios A/B) |
| Span-export backend / collector UI | N/A | Do not add |

## Related Documents

- [ADR-0047 — Distributed Tracing OTLP Baseline](./0047-distributed-tracing-otlp-baseline.md)
- [ADR-0030 — Operational Platform Baseline](./0030-operational-platform-baseline.md)
- [ADR-0033 — Async messaging and task retry baseline](../async-processing/0033-async-messaging-and-task-retry-baseline.md)
- [LR-D4 behavior note](../../behavior/lrp-d4-trace-propagation.md)
- [LRP-D §LR-D4](../../plan/detail/LRP-D-ops-observability.md#lr-d4--trace-propagation-decision--minimal-impl)
- [SOR-A06 completion](../../plan/system-optimization-review-2026-07.md)
