# Kubernetes HPA Autoscaling (P15-T05 / ADR-0030)

Horizontal Pod Autoscaler (HPA) for **backend** and **frontend** workloads. Implements
ADR-0030 row: **HPA based on CPU/memory + custom metrics**.

## T05 task mapping

| Task | Deliverable | Evidence |
| --- | --- | --- |
| **P15-T05a** | `templates/backend-hpa.yaml`, `templates/frontend-hpa.yaml` — CPU + memory Resource metrics, min/max replica bounds, blue-green active Deployment target | `Assert-T05Hpa` in `helm-validate.ps1` |
| **P15-T05b** | Pods custom metric (`docgen_http_requests_per_second`) gated by `customMetric.enabled`; Prometheus Adapter prerequisite documented | Rendered staging/prod manifests; this doc |

## Chart templates

| Resource | Template | When rendered |
| --- | --- | --- |
| Backend HPA | `templates/backend-hpa.yaml` | `backend.enabled` + `autoscaling.backend.enabled` |
| Frontend HPA | `templates/frontend-hpa.yaml` | `frontend.enabled` + `autoscaling.frontend.enabled` |

Both use **`autoscaling/v2`** and scale on:

1. **CPU** — `Resource` metric, `averageUtilization` target
2. **Memory** — `Resource` metric, `averageUtilization` target
3. **Custom metric** (backend only, when enabled) — `Pods` metric via metrics adapter

## Values (autoscaling block)

Base defaults in `values.yaml`:

```yaml
autoscaling:
  backend:
    enabled: false          # true in values-staging.yaml / values-prod.yaml
    minReplicas: 2
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70
    targetMemoryUtilizationPercentage: 80
    customMetric:
      enabled: true       # toggle custom Pods metric (P15-T05b)
      name: docgen_http_requests_per_second
      targetType: AverageValue
      averageValue: "100"
  frontend:
    enabled: false
    minReplicas: 2
    maxReplicas: 5
    targetCPUUtilizationPercentage: 80
    targetMemoryUtilizationPercentage: 80
    customMetric:
      enabled: false
```

| Profile | Backend HPA | Frontend HPA | Backend custom metric |
| --- | --- | --- | --- |
| default / dev | off | off | n/a |
| staging | on (2–8) | on (2–4) | on (inherits base) |
| prod | on (3–12) | on (2–6) | on (explicit in values-prod) |

## scaleTargetRef and blue-green (T05a)

When `blueGreen.enabled: false` (staging), HPA targets the single Deployment:

```
{release}-backend
{release}-frontend
```

When `blueGreen.enabled: true` (production), HPA targets the **active color** Deployment only:

```
{release}-backend-{activeColor}   # e.g. docgen-prod-backend-blue
{release}-frontend-{activeColor}
```

After a blue-green cutover (`blueGreen.activeColor` switch), upgrade the release so HPA
`scaleTargetRef.name` follows the new active color. The preview (inactive) color is not
autoscaled.

## Custom metric prerequisite (T05b)

The backend HPA custom metric uses **`type: Pods`** — Kubernetes resolves it through a
**metrics adapter** (not built into the kube-apiserver).

| Prerequisite | Purpose |
| --- | --- |
| **Prometheus** (or compatible TSDB) | Scrapes application / Ingress metrics |
| **Prometheus Adapter** (or equivalent custom-metrics adapter) | Exposes Prometheus series as `custom.metrics.k8s.io` / `metrics.k8s.io` Pod metrics |
| Metric rule for `docgen_http_requests_per_second` | Maps Prometheus query → HPA Pods metric name |

Example adapter rule (illustrative — tune query and labels for your scrape config):

```yaml
rules:
  - seriesQuery: 'docgen_http_requests_per_second{namespace!="",pod!=""}'
    resources:
      overrides:
        namespace: { resource: "namespace" }
        pod: { resource: "pod" }
    name:
      matches: "docgen_http_requests_per_second"
      as: "docgen_http_requests_per_second"
    metricsQuery: 'sum(<<.Series>>{<<.LabelMatchers>>}) by (<<.GroupBy>>)'
```

**Without the adapter**, HPA v2 still scales on CPU and memory; the custom metric line appears
in the manifest but the metric will be **missing** at runtime until the adapter is installed.

Toggle off custom scaling when the adapter is not deployed:

```yaml
autoscaling:
  backend:
    customMetric:
      enabled: false
```

## Validation (render only — no cluster)

From repo root:

```powershell
.\scripts\helm-validate.ps1 -SkipKubeconform
```

`Assert-T05Hpa` checks for staging/prod:

- `apiVersion: autoscaling/v2`
- Backend + frontend HPA present
- CPU and memory `Resource` utilization metrics
- Backend Pods custom metric when `customMetric.enabled: true`
- `scaleTargetRef` → `apps/v1` `Deployment` (blue-green `-blue` suffix in prod)
- `minReplicas` ≤ `maxReplicas`

Default/dev profiles assert **no** HPA is rendered.

## Operational notes

- Container **requests** must be set (chart enforces this per ADR-0030) for CPU/memory
  utilization metrics to behave predictably.
- HPA respects Deployment pod template changes; image tag updates do not require HPA edits.
- For load-based scale tests, document sustained load above target utilization and observe
  replica count — requires a live cluster with metrics-server (Resource metrics) and adapter
  (custom metrics).

## Cross-links

- [deploy/helm/docgen/README.md](./helm/docgen/README.md) — chart layout and validation
- [deploy/blue-green-runbook.md](./blue-green-runbook.md) — active color cutover (HPA follows `activeColor`)
- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — autoscaling baseline
- [P15 detailed plan](../docs/plan/detail/P15-kubernetes-deployment-container-hardening.md) — P15-T05 acceptance
