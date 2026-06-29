# Kubernetes NetworkPolicy (P15-T06 / ADR-0030)

Default-deny network isolation for **backend** and **frontend** workloads with explicit
allow rules for required flows only. Implements ADR-0030 row: **Default deny + explicit allow
rules as needed**.

## T06 task mapping

| Task | Deliverable | Evidence |
| --- | --- | --- |
| **P15-T06a** | `templates/networkpolicy.yaml` — `podSelector: {}` default-deny with Ingress + Egress `policyTypes` | `Assert-T06NetworkPolicy` in `helm-validate.ps1` |
| **P15-T06b** | Explicit allow policies — ingress controller, frontend→backend, backend→external services, DNS, metrics | Rendered manifests; this doc |

## Policy model

When `networkPolicy.enabled: true` (default), the chart renders a **stack** of NetworkPolicy
resources. Kubernetes merges policies additively — the default-deny policy blocks all traffic;
subsequent allow policies carve out required flows.

```
┌─────────────────────────────────────────────────────────────┐
│  default-deny (all pods) — Ingress ✗  Egress ✗             │
└─────────────────────────────────────────────────────────────┘
         │ allow policies add permitted flows only
         ▼
┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐
│ ingress-nginx →  │  │ frontend →       │  │ backend →     │
│ frontend/backend │  │ backend :8080    │  │ Postgres/Redis│
│ (when Ingress)   │  │ (in-cluster)     │  │ Kafka/MinIO   │
└──────────────────┘  └──────────────────┘  └───────────────┘
         │                      │                      │
         ▼                      ▼                      ▼
┌──────────────────┐  ┌──────────────────────────────────┐
│ kube-system DNS  │  │ monitoring → backend/frontend    │
│ UDP/TCP :53      │  │ metrics scrape (when enabled)    │
└──────────────────┘  └──────────────────────────────────┘
```

## Rendered policies

| Policy name suffix | Direction | Selector | Peers / ports |
| --- | --- | --- | --- |
| `default-deny` | Ingress + Egress | all pods (`{}`) | none — deny by default |
| `allow-ingress-controller` | Ingress | frontend pods | NGINX Ingress Controller namespace + pod labels → `:8080` |
| `allow-ingress-to-backend` | Ingress | backend pods | NGINX Ingress Controller (when `ingress.enabled`) → `:8080` |
| `allow-frontend-to-backend` | Ingress | backend pods | frontend pods → backend `:8080` |
| `allow-backend-external-egress` | Egress | backend pods | `ipBlock` CIDRs → Postgres **5432**, Redis **6379**, Kafka **9092**, MinIO **443** |
| `allow-dns` | Egress | all pods | `kube-system` namespace → UDP/TCP **53** |
| `allow-metrics` | Ingress | all release pods | monitoring namespace (when `monitoring.enabled`) → `:8080` |

## Values (`networkPolicy` block)

Base defaults in `values.yaml`:

```yaml
networkPolicy:
  enabled: true
  ingressController:
    namespace: ingress-nginx
    podLabels:
      app.kubernetes.io/name: ingress-nginx
  monitoring:
    enabled: true
    namespace: monitoring
    podLabels: {}
  dns:
    namespace: kube-system
  externalEgress:
    cidrs:
      - 0.0.0.0/0          # dev/default — port-restricted only
    ports:
      - port: 5432
        protocol: TCP
      - port: 6379
        protocol: TCP
      - port: 9092
        protocol: TCP
      - port: 443
        protocol: TCP
```

| Setting | Purpose |
| --- | --- |
| `ingressController.namespace` / `podLabels` | Match NGINX Ingress Controller pods |
| `dns.namespace` | CoreDNS / kube-dns namespace (default `kube-system`) |
| `monitoring.enabled` | Gate Prometheus scrape allow policy |
| `monitoring.namespace` / `podLabels` | Match metrics scraper pods |
| `externalEgress.cidrs` | Backend egress destination CIDRs |
| `externalEgress.ports` | Required ports only — **never** open `0.0.0.0/0` without ports |

### Per-environment posture

| Profile | `externalEgress.cidrs` | Allow policy count (with metrics) |
| --- | --- | --- |
| default / dev | `0.0.0.0/0` (port-restricted) | 6 (no ingress-to-backend) |
| staging / prod | `0.0.0.0/0` or restricted CIDR | 7 (includes ingress-to-backend) |
| prod (recommended) | e.g. `10.64.0.0/16` managed-service VPC | 7 |

Production should replace `0.0.0.0/0` with the managed-service VPC CIDR (`values-prod.yaml`
example: `10.64.0.0/16`). The validation script rejects `0.0.0.0/0` egress rules that omit
port restrictions.

## Required traffic flows (traceability)

| Flow | Allowed by | ADR / architecture |
| --- | --- | --- |
| Internet → SPA | ingress controller → frontend | ADR-0030 NGINX Ingress |
| Internet → `/api` | ingress controller → backend | P15-T04b direct API path |
| SPA → API proxy | frontend → backend | Runtime view in-cluster routing |
| Backend → Postgres/Redis/Kafka/MinIO | backend external egress | External managed services (P15-T03) |
| Pod → cluster DNS | allow-dns → kube-system:53 | Kubernetes DNS service discovery |
| Prometheus → workloads | allow-metrics (optional) | ADR-0030 OpenTelemetry + Prometheus |

All other ingress/egress is **blocked** by the default-deny policy.

## Validation (render only — no cluster)

```powershell
.\scripts\helm-validate.ps1 -SkipKubeconform
```

`Assert-T06NetworkPolicy` checks:

- Default-deny NetworkPolicy with `podSelector: {}` and both Ingress + Egress types
- Expected allow policy count per profile (ingress enabled/disabled, metrics enabled)
- Required allow policy name suffixes present
- Backend external egress includes ports 5432, 6379, 9092, 443
- DNS allow targets `kube-system` on port 53
- No `0.0.0.0/0` ipBlock egress without explicit `ports` list

## Related docs

- [deploy/helm/docgen/README.md](./helm/docgen/README.md) — chart layout and lint commands
- [deploy/k8s-ingress-tls.md](./k8s-ingress-tls.md) — Ingress controller integration (P15-T04)
- [deploy/k8s-config-secrets.md](./k8s-config-secrets.md) — external service endpoints (P15-T03)
- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — operational baseline
- [Security view](../docs/architecture/security-view.md) — network isolation posture
