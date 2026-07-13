# Grafana dashboards (LR-D3)

Importable Grafana dashboard JSON for generation, PDF conversion pool, SSE, and rate-limit panels.

| File | Purpose |
| --- | --- |
| [`docgen-ops-overview.json`](./docgen-ops-overview.json) | Ops overview — generation p95, PDF pool/rejections, SSE, 429, DLT depth |

**Import:** Grafana → Dashboards → Import → upload JSON. Select a Prometheus datasource when prompted (`${datasource}` template variable).

**Constraints:** no baked credentials; no vendor APM dependency; thresholds remain **draft** until [NFR §待确认 LR-D5](../../../docs/requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation) confirmation.

Index: [../README.md](../README.md) · Runbook: [docs/operations/runbook.md](../../../docs/operations/runbook.md#observability)
