# docgen Helm Chart (P15-T02 / P15-T03 / P15-T04 / P15-T05 / P15-T06 / P15-T07 / P15-T08 / ADR-0030)

Application workloads for **backend** (Spring Boot) and **frontend** (NGINX static SPA).
Stateful dependencies (PostgreSQL, Redis, Kafka, MinIO) are **externally managed** and
referenced via `ConfigMap` / `Secret` only — no in-cluster StatefulSets.

## Chart layout

| Path | Purpose |
| --- | --- |
| `Chart.yaml` | Chart metadata |
| `values.yaml` | Base defaults (securityContext, resources, probes, config) |
| `values-dev.yaml` | Dev overrides (single replica, demo seed, dev endpoints) |
| `values-staging.yaml` | Staging overrides (Ingress + TLS; single backend replica per ADR-0044) |
| `values-prod.yaml` | Claimed production overrides (`springProfilesActive=prod`, `appEnvironment=prod`, `asyncTransport=kafka`; blue-green; single backend replica per ADR-0044) |
| `templates/` | Deployments, Services, Ingress, HPA, NetworkPolicy, ConfigMap, Secret refs |
| `templates/_helpers.tpl` | Shared securityContext helpers |

## Security & resources (ADR-0030)

Every container renders with:

- **Pod** `securityContext`: `runAsNonRoot: true`, `seccompProfile: RuntimeDefault`, `fsGroup`
- **Container** `securityContext`: `readOnlyRootFilesystem: true`, `allowPrivilegeEscalation: false`, `capabilities.drop: [ALL]`
- **Resources**: both `requests` and `limits` for CPU and memory (see `values.yaml`)

Writable paths use `emptyDir` mounts (`/tmp`; frontend also mounts NGINX `conf.d` from ConfigMap).

## Configuration & secrets (P15-T03)

| Resource | Template | Purpose |
| --- | --- | --- |
| Application ConfigMap | `templates/configmap.yaml` | Non-sensitive env + external service **endpoints** |
| Frontend NGINX ConfigMap | `templates/frontend-nginx-configmap.yaml` | Static proxy config |
| Secret ref | `_helpers.tpl` → `docgen.secretName` | Credentials via operator Secret or ExternalSecret |
| ExternalSecret | `templates/external-secret.yaml` | Optional sync from Secret Manager / Vault |

- **`secrets.create: false`** (default) — staging/prod/dev reference `secrets.existingSecretName` only.
- **Fail-closed:** empty `existingSecretName` aborts render; missing Secret blocks pod startup.
- **No plaintext** in committed values — supply credentials out-of-band.

Full key matrix, fail-closed semantics, and validation commands:
[deploy/k8s-config-secrets.md](../../k8s-config-secrets.md).

## Service exposure, Ingress & TLS (P15-T04)

| Resource | Template | Purpose |
| --- | --- | --- |
| Backend Service | `templates/backend-service.yaml` | ClusterIP :8080, `docgen.io/cluster-dns` |
| Frontend Service | `templates/frontend-service.yaml` | ClusterIP :8080, SPA + health probes |
| Ingress | `templates/ingress.yaml` | NGINX class; `/api` → backend, `/` → frontend |
| Certificate | `templates/certificate.yaml` | cert-manager TLS Secret (staging/prod) |

In-cluster callers use Kubernetes DNS (`{release}-backend.{namespace}.svc.cluster.local:8080`).
External traffic enters via NGINX Ingress with cert-manager TLS (TLS 1.2+ via `ssl-protocols`).

Full routing matrix and validation:
[deploy/k8s-ingress-tls.md](../../k8s-ingress-tls.md).

## HPA autoscaling (P15-T05)

| Resource | Template | Purpose |
| --- | --- | --- |
| Backend HPA | `templates/backend-hpa.yaml` | CPU + memory + optional Pods custom metric |
| Frontend HPA | `templates/frontend-hpa.yaml` | CPU + memory (custom metric off by default) |

**Deployment topology (ADR-0044):** v1 launches with a **single backend replica** in every
environment — `backend.replicaCount: 1` and `autoscaling.backend.enabled: false` in all values
files. Backend HPA may be re-enabled only after the scale-out prerequisites in
[ADR-0044](../../../docs/adr/operations/0044-deployment-topology-v1.md) are met (scheduler
mutex LR-B2, SSE sticky routing/relay LR-B3, shared rate limit, `ASYNC_TRANSPORT=kafka` LR-B4).
The frontend is stateless and unconstrained by this decision. Blue-green (below) keeps
**both colors resident** (one replica each) with traffic on the active color only — safe for
traffic-bound components, but schedulers double-run, so the LR-B2 scheduler mutex is
**mandatory before the first blue-green prod deployment** (ADR-0044).

When re-enabled, production blue-green mode scales the **active color** Deployment only
(`scaleTargetRef` follows `blueGreen.activeColor`).

Custom metric `docgen_http_requests_per_second` requires **Prometheus Adapter** (or equivalent)
in the cluster — see [deploy/k8s-hpa-autoscaling.md](../../k8s-hpa-autoscaling.md).

## NetworkPolicy isolation (P15-T06)

| Resource | Template | Purpose |
| --- | --- | --- |
| Default-deny | `templates/networkpolicy.yaml` | Deny all ingress + egress for every pod |
| Allow policies | same template | Ingress controller, frontend→backend, external egress, DNS, metrics |

Enabled by default (`networkPolicy.enabled: true`). Production should restrict
`networkPolicy.externalEgress.cidrs` to managed-service VPC ranges.

Full flow matrix and validation:
[deploy/k8s-network-policy.md](../../k8s-network-policy.md).

## Blue-green release (P15-T08)

Production enables parallel **blue** and **green** Deployments per workload. Main Services
route Ingress traffic to `blueGreen.activeColor`; preview Services target the inactive color
for smoke validation before cutover.

| Resource | Template | Purpose |
| --- | --- | --- |
| Backend color Deployments | `templates/backend-color-deployments.yaml` | `backend-blue` + `backend-green` |
| Frontend color Deployments | `templates/frontend-color-deployments.yaml` | `frontend-blue` + `frontend-green` |
| Preview Services | `templates/bluegreen-preview-services.yaml` | ClusterIP to inactive color |
| Main Services | `backend-service.yaml`, `frontend-service.yaml` | Selector includes `docgen.io/deployment-color: activeColor` |
| HPA | `backend-hpa.yaml`, `frontend-hpa.yaml` | `scaleTargetRef` → active color Deployment only |

Key values (`values-prod.yaml`):

```yaml
blueGreen:
  enabled: true
  activeColor: blue          # flip only after manual approval
  requireManualApproval: true # pipeline gate — chart does not auto-cutover
  previewService:
    enabled: true
  colors:
    blue:
      backendImageTag: ""
      frontendImageTag: ""
    green:
      backendImageTag: ""
      frontendImageTag: ""
```

Cutover, manual approval gate, and rollback steps:
[deploy/blue-green-runbook.md](../../blue-green-runbook.md).

`Assert-T08BlueGreen` in `scripts/helm-validate.ps1` validates prod render: dual color
Deployments, activeColor Service selectors, preview Services, and HPA target alignment.

## Values pattern (dev / staging)

Base defaults live in `values.yaml`. Environment files override only what differs:

```yaml
# values-dev.yaml — minimal dev cluster
global:
  environment: dev
backend:
  replicaCount: 1
  image:
    tag: dev
config:
  appEnvironment: dev
  seedDemoCatalog: true
secrets:
  create: false
  existingSecretName: docgen-app-secrets-dev
```

```yaml
# values-staging.yaml — pre-prod with Ingress + TLS (single backend replica per ADR-0044)
global:
  environment: staging
backend:
  replicaCount: 1
ingress:
  enabled: true
  host: docgen.staging.example.com
autoscaling:
  backend:
    enabled: false # ADR-0044: re-enable only after scale-out prerequisites
secrets:
  create: false
  existingSecretName: docgen-app-secrets-staging
```

Install example (requires cluster — **not** used for local render-only validation):

```powershell
helm upgrade --install docgen . `
  -f values-dev.yaml `
  --namespace docgen
```

## Local validation (render only — no cluster)

### Option A — validation script (recommended)

From repo root:

```powershell
.\scripts\helm-validate.ps1
```

Runs `helm lint`, `helm template` for default/dev/staging/prod, content assertions, fail-closed
secret check, and `kubeconform`.

Skip kubeconform when offline:

```powershell
.\scripts\helm-validate.ps1 -SkipKubeconform
```

### Option B — manual Helm commands

From this directory (`deploy/helm/docgen`):

```powershell
# Lint chart
helm lint .

# Render manifests (stdout only — does not apply to a cluster)
helm template docgen-dev . -f values-dev.yaml --namespace docgen
helm template docgen-staging . -f values-staging.yaml --namespace docgen

# Default + all env profiles
helm template docgen . --namespace docgen
helm template docgen . -f values-dev.yaml --namespace docgen
helm template docgen . -f values-staging.yaml --namespace docgen
helm template docgen . -f values-prod.yaml --namespace docgen
```

Without Helm on PATH, use Docker:

```powershell
$chart = (Resolve-Path .).Path -replace '\\','/'
docker run --rm -v "${chart}:/chart:ro" alpine/helm:3.14.4 lint /chart
docker run --rm -v "${chart}:/chart:ro" alpine/helm:3.14.4 template docgen-dev /chart -f /chart/values-dev.yaml --namespace docgen
```

### Expected lint result

```
1 chart(s) linted, 0 chart(s) failed
```

(INFO about missing `icon` in `Chart.yaml` is informational only.)

## Related docs

- [deploy/k8s-config-secrets.md](../../k8s-config-secrets.md) — ConfigMap/Secret wiring (P15-T03)
- [deploy/k8s-ingress-tls.md](../../k8s-ingress-tls.md) — Service DNS, Ingress, cert-manager TLS (P15-T04)
- [deploy/k8s-hpa-autoscaling.md](../../k8s-hpa-autoscaling.md) — HPA CPU/memory + custom metrics (P15-T05)
- [deploy/k8s-network-policy.md](../../k8s-network-policy.md) — default-deny NetworkPolicy + allow rules (P15-T06)
- [deploy/k8s-health-probes.md](../../k8s-health-probes.md) — liveness/readiness on `/healthz` + `/readyz` (P15-T07)
- [deploy/blue-green-runbook.md](../../blue-green-runbook.md) — cutover, manual approval, rollback (P15-T08)
- [deploy/README.md](../../README.md) — install, upgrade, blue-green runbook
- [deploy/container-hardening.md](../../container-hardening.md) — image hardening evidence
- [ADR-0030](../../../docs/adr/operations/0030-operational-platform-baseline.md) — operational baseline
- [ADR-0044](../../../docs/adr/operations/0044-deployment-topology-v1.md) — v1 deployment topology (single backend replica)
