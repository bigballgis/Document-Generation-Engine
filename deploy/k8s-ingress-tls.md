# Kubernetes Service Exposure, Ingress & TLS (P15-T04 / ADR-0030)

External and in-cluster traffic routing for **backend** and **frontend** workloads. Implements
ADR-0030 rows: NGINX Ingress Controller, cert-manager TLS automation, Kubernetes DNS service
discovery, and TLS 1.2+ data-in-transit baseline.

## T04 task mapping

| Task | Deliverable | Evidence |
| --- | --- | --- |
| **P15-T04a** | `templates/*-service.yaml` — ClusterIP, port 8080, `docgen.io/cluster-dns` | `Assert-T04IngressTls` in `helm-validate.ps1` |
| **P15-T04b** | `templates/ingress.yaml` — `ingressClassName: nginx`, `/api` → backend, `/` → frontend | Rendered staging/prod manifests; validation script |
| **P15-T04c** | `templates/certificate.yaml` + issuer annotations, TLS 1.2+ protocol floor | cert-manager `Certificate` + `nginx.ingress.kubernetes.io/ssl-protocols` |

## In-cluster DNS (T04a)

Each workload exposes a **ClusterIP** Service aligned with hardened container port **8080**.

| Workload | Service name pattern | DNS FQDN pattern |
| --- | --- | --- |
| Backend | `{release}-backend` | `{release}-backend.{namespace}.svc.cluster.local:8080` |
| Frontend | `{release}-frontend` | `{release}-frontend.{namespace}.svc.cluster.local:8080` |

Example (release `docgen-staging`, namespace `docgen`):

```
docgen-staging-backend.docgen.svc.cluster.local:8080
docgen-staging-frontend.docgen.svc.cluster.local:8080
```

The frontend NGINX ConfigMap proxies `/api/` to the backend Service FQDN. In-cluster callers
resolve Services via native Kubernetes DNS — no hardcoded pod IPs.

Service metadata includes:

- `docgen.io/cluster-dns` — fully qualified cluster DNS name
- `docgen.io/service-port` — Service port (8080)

## NGINX Ingress routing (T04b)

Enabled in `values-staging.yaml` and `values-prod.yaml` (`ingress.enabled: true`).

| Setting | Default | Purpose |
| --- | --- | --- |
| `ingress.className` | `nginx` | NGINX Ingress Controller class (ADR-0030) |
| `ingress.host` | per-env hostname | External host rule |
| `ingress.apiPath` | `/api` | API traffic → backend Service |
| `ingress.apiPathType` | `Prefix` | Path match type for API |
| `ingress.path` | `/` | SPA/static → frontend Service |
| `ingress.pathType` | `Prefix` | Path match type for SPA |

Ingress TLS references the cert-manager-managed Secret (`ingress.tls.secretName` or
`{release}-tls` default via `docgen.tlsSecretName` helper).

When Ingress is enabled, NetworkPolicy allows the NGINX Ingress Controller namespace to reach
both frontend (SPA) and backend (`/api` direct path) pods.

## cert-manager TLS (T04c)

| Resource | Template | When rendered |
| --- | --- | --- |
| Ingress TLS stanza | `templates/ingress.yaml` | `ingress.enabled` + `ingress.tls.enabled` |
| cert-manager Certificate | `templates/certificate.yaml` | `certificate.enabled` |

Certificate spec:

- `issuerRef.name` / `issuerRef.kind` — ClusterIssuer or Issuer (required)
- `dnsNames` — defaults to `ingress.host` when empty
- `secretName` — shared with Ingress TLS block
- `privateKey.algorithm: RSA`, `size: 2048` — compatible with TLS 1.2+ cipher suites

Ingress annotations (ADR-0030 TLS 1.2+ floor):

```yaml
cert-manager.io/cluster-issuer: letsencrypt-prod
nginx.ingress.kubernetes.io/ssl-protocols: "TLSv1.2 TLSv1.3"
```

cert-manager handles certificate issuance and renewal; NGINX enforces the minimum protocol
version at the edge.

## Per-environment posture

| Profile | `ingress.enabled` | `certificate.enabled` | Issuer |
| --- | --- | --- | --- |
| default / dev | `false` | `false` | — |
| staging | `true` | `true` | `letsencrypt-staging` |
| prod | `true` | `true` | `letsencrypt-prod` |

Dev/default profiles still render ClusterIP Services for in-cluster DNS; Ingress and
Certificate are omitted until enabled.

## Validation (render only — no cluster)

```powershell
.\scripts\helm-validate.ps1 -SkipKubeconform
```

`Assert-T04IngressTls` checks:

- Backend/frontend ClusterIP Services on port 8080 with resolvable DNS annotations
- Staging/prod: NGINX ingress class, `/api` + `/` paths, TLS hosts + secretName
- Staging/prod: cert-manager issuer annotation, Certificate with `issuerRef` + `dnsNames`
- TLS 1.2+ `ssl-protocols` annotation present when Ingress enabled

## Related docs

- [deploy/helm/docgen/README.md](./helm/docgen/README.md) — chart layout and lint commands
- [deploy/k8s-config-secrets.md](./k8s-config-secrets.md) — ConfigMap/Secret wiring (P15-T03)
- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — operational baseline
