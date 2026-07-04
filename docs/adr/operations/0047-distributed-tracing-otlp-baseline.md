---
id: ADR-0047
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0047"
topic: operations
related:
  - docs/adr/operations/0030-operational-platform-baseline.md
  - docs/plan/system-optimization-review-2026-07.md
  - backend/src/main/java/com/bank/docgen/sharedkernel/api/TraceIdProvider.java
  - backend/src/main/resources/application.yml
  - backend/src/main/resources/application-prod.yml
---

# ADR 0047: Distributed Tracing OTLP Baseline

## Status

Accepted

## Context

[ADR 0030](./0030-operational-platform-baseline.md) records OpenTelemetry tracing with Jaeger/Tempo as the operational baseline. The backend previously exposed Prometheus metrics only (`micrometer-registry-prometheus`) and resolved trace IDs locally via `TraceIdProvider` (UUID or `X-Trace-Id` header) without a Micrometer/OpenTelemetry bridge. Audit records and API response envelopes must share one trace identifier for correlation across logs, metrics, and traces.

SOR-A06 requires Micrometer Tracing with OTLP export while keeping local and test environments free of collector dependencies.

## Decision

The backend distributed tracing baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Tracing bridge | Micrometer Tracing with OpenTelemetry bridge (`micrometer-tracing-bridge-otel`) | Spring Boot 3.3 BOM-managed dependency |
| Span export | OTLP over HTTP (`opentelemetry-exporter-otlp`) | Spring Boot `management.otlp.tracing` auto-configuration |
| Default (local/dev/test) | Tracing **disabled**; sampling probability **0.0** | No collector required for unit tests or local runs |
| Production profile | Tracing **enabled**; sampling probability configurable | Default `0.1`; override via `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` |
| OTLP endpoint | Environment variable only | `OTEL_EXPORTER_OTLP_ENDPOINT`; no secrets or collector URLs baked into images |
| Trace ID resolution | `TraceIdProvider` precedence: active Micrometer span → `X-Trace-Id` header → UUID | Shared by API envelopes, audit recorders, and MDC filter |
| Log correlation | MDC key `traceId`; prod JSON logs include MDC; non-prod console pattern includes `%X{traceId}` | Pairs with existing `TraceIdMdcFilter` |

Configuration keys:

```yaml
management.tracing.enabled          # false by default; true in prod profile
management.tracing.sampling.probability
management.otlp.tracing.endpoint      # ${OTEL_EXPORTER_OTLP_ENDPOINT}
```

Production override environment variables:

- `OTEL_EXPORTER_OTLP_ENDPOINT` — OTLP HTTP traces endpoint (e.g. `http://otel-collector:4318/v1/traces`)
- `MANAGEMENT_TRACING_SAMPLING_PROBABILITY` — sample rate (0.0–1.0)

## Consequences

- Production deployments can export spans to any OTLP-compatible backend (Tempo, Jaeger OTLP ingest, vendor collectors) without code changes.
- Local and CI test runs remain collector-free with tracing disabled.
- API `metadata.traceId`, audit `traceId` fields, response header `X-Trace-Id`, and log MDC align when an active span exists or when callers supply `X-Trace-Id`.
- Operators must provision an OTLP collector endpoint in production and set `OTEL_EXPORTER_OTLP_ENDPOINT`; missing endpoint configuration falls back to the documented default host (non-secret).

## Alternatives Considered

| Alternative | Why not chosen |
| --- | --- |
| Jaeger agent direct export | ADR 0030 names Jaeger/Tempo as backends; OTLP is the vendor-neutral export path supported by Spring Boot 3.3 |
| Always-on tracing in all profiles | Would require collectors in local/test CI; rejected |
| Keep header/UUID-only trace IDs | Does not satisfy ADR 0030 tracing baseline or cross-service correlation |

## Related Documents

- [ADR 0030: Operational Platform Baseline](./0030-operational-platform-baseline.md)
- [SOR-A06 — system optimization review](../plan/system-optimization-review-2026-07.md)
- [API contract visibility (trace ID handling)](../api/0013-api-contract-visibility-audit-and-context.md)
