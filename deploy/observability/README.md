# Metrics & Alerting as Code

**Status:** Draft (LR-D3) — proposed baseline, pending infrastructure deployment
**Last updated:** 2026-07-05
**ADR reference:** [ADR-0030 operational platform baseline](../adr/operations/0030-operational-platform-baseline.md)

## Purpose

Define the metrics, alerting rules, and dashboards that make the platform observable in
production. All artifacts are version-controlled (alerts as Prometheus rules, dashboards as
Grafana JSON) so observability is reproducible and reviewable.

## Metrics (Micrometer → Prometheus)

The backend exposes `/actuator/prometheus`. Key metrics:

| Metric | Type | Source | Alert |
| --- | --- | --- | --- |
| `http_server_requests_seconds` | timer | Spring MVC | p95 > 3s for 5m |
| `docgen_generation_duration_seconds` | timer | `DocumentGenerationEngine` | p95 > 10s for 5m |
| `docgen_pdf_conversion_duration_seconds` | timer | `PdfConversionService` | p95 > 30s for 5m |
| `docgen_pdf_conversion_failures_total` | counter | `PdfConversionService` | rate > 10/min for 5m |
| `docgen_idempotency_conflicts_total` | counter | `IdempotencyService` | rate > 50/min for 10m |
| `docgen_audit_retention_deleted_total` | counter | `AuditRetentionCleanupScheduler` | (info only) |
| `docgen_sse_emitters_active` | gauge | `SseEmitterRegistry` | > 100 for 5m |
| `jvm_memory_used_bytes` | gauge | JVM | > 85% of max for 5m |
| `process_cpu_usage` | gauge | JVM | > 0.8 for 5m |
| `db_connection_pool_active` | gauge | HikariCP | = max for 2m |
| `shedlock_lock_held_seconds` | timer | ShedLock | (info only) |

## Alerting rules (Prometheus)

```yaml
# deploy/observability/prometheus-alerts.yaml
groups:
  - name: docgen-backend
    rules:
      - alert: HighPdfConversionFailureRate
        expr: rate(docgen_pdf_conversion_failures_total[5m]) > 0.17
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "PDF conversion failure rate > 10/min"
          description: "LibreOffice conversion is failing; check LR-A1 profile isolation and font baseline."

      - alert: HighGenerationLatencyP95
        expr: histogram_quantile(0.95, rate(docgen_generation_duration_seconds_bucket[5m])) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Document generation p95 > 10s"

      - alert: HighHttpLatencyP95
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri!~"/actuator.*"}[5m])) > 3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HTTP p95 > 3s"

      - alert: HighJvmMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM heap > 85%"

      - alert: DbConnectionPoolExhausted
        expr: db_connection_pool_active / db_connection_pool_max > 0.9
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "HikariCP connection pool > 90% utilized"

      - alert: HighSseEmitterCount
        expr: docgen_sse_emitters_active > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Active SSE emitters > 100; check for leaked connections"
```

## Dashboards (Grafana JSON)

Dashboards live in `deploy/observability/grafana/`:

- `docgen-overview.json` — request rate, error rate, p95 latency, active SSE.
- `docgen-generation.json` — generation duration histogram, PDF conversion failures, idempotency conflicts.
- `docgen-infrastructure.json` — JVM memory, CPU, DB pool, ShedLock holds.

Each dashboard is a JSON file importable via `grafana-cli dashboards import` or the Grafana
provisioning system.

## Deployment

The observability stack (Prometheus + Grafana + Alertmanager) is **not** part of the v1
application stack. It is deployed separately as part of the platform infrastructure. The
backend exposes metrics; a scrape config and dashboard provisioning are provided as reference.

```yaml
# deploy/observability/prometheus-scrape.yaml (reference)
scrape_configs:
  - job_name: docgen-backend
    metrics_path: /actuator/prometheus
    scrape_interval: 15s
    static_configs:
      - targets: ['docgen-backend:8080']
```

## Open questions (pending)

- **Alert routing**: Alertmanager → Slack? PagerDuty? Email? (pending ops team input)
- **Dashboard ownership**: who maintains the Grafana JSON? (recommendation: backend team)
- **Retention**: how long to keep Prometheus metrics? (recommendation: 15 days hot, 90 days cold)
