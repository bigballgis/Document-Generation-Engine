# Deployment Guide (P15 / ADR-0030)

Canonical operator guide for installing, upgrading, cutting over, and rolling back the
Document Generation Engine. Covers **local Docker Compose** (development and acceptance) and
**Kubernetes** (staging/production) per [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md).

Application workloads deploy **backend** and **frontend** only. Stateful dependencies
(**PostgreSQL, Redis, Kafka, MinIO**) are **externally managed** — the Helm chart references them
via `ConfigMap` / `Secret`; it does not deploy in-cluster StatefulSets for those services.

## Prerequisites

### Kubernetes (staging / production)

| Component | Purpose |
| --- | --- |
| Kubernetes 1.29+ cluster | Target runtime |
| NGINX Ingress Controller | `ingress.className: nginx` |
| cert-manager | TLS `Certificate` resources (staging/prod) |
| Prometheus Adapter (optional) | HPA custom metric `docgen_http_requests_per_second` |
| Cluster Secret or External Secrets Operator | Credentials (`POSTGRES_*`, `MINIO_*`, `JWT_SECRET`) |
| Helm 3.14+ | Chart install/upgrade |
| Container registry | Packaged images from `backend/Dockerfile.packaged`, `frontend/Dockerfile.packaged` |

### Local Docker Compose

| Component | Purpose |
| --- | --- |
| Docker + Docker Compose | Local full stack |
| Host Maven + pnpm | Build artifacts copied into runtime images (no compile inside `docker build`) |
| `scripts/docker-deploy.ps1` | Host build + compose prod profile rollout |

See also [Production runbook](../docs/operations/runbook.md) for release-gate and compose health checks.

## Topic documentation index

| Document | Scope |
| --- | --- |
| [container-hardening.md](./container-hardening.md) | Distroless/minimal base, non-root, read-only root FS (P15-T01) |
| [helm/docgen/README.md](./helm/docgen/README.md) | Chart values, lint/template commands, per-env profiles (P15-T02) |
| [k8s-config-secrets.md](./k8s-config-secrets.md) | ConfigMap / Secret / ExternalSecret wiring (P15-T03) |
| [k8s-ingress-tls.md](./k8s-ingress-tls.md) | Service DNS, NGINX Ingress, cert-manager TLS 1.2+ (P15-T04) |
| [k8s-hpa-autoscaling.md](./k8s-hpa-autoscaling.md) | HPA CPU/memory + custom metrics (P15-T05) |
| [k8s-network-policy.md](./k8s-network-policy.md) | Default-deny NetworkPolicy + explicit allow rules (P15-T06) |
| [k8s-health-probes.md](./k8s-health-probes.md) | Liveness `/healthz` + readiness `/readyz` (P15-T07; F8 deep readiness checks) |
| **[observability/](./observability/README.md)** | **LR-D3** — metrics/alerting as code index; **draft** thresholds (D6/D5); runbook annotation map; no vendor APM; no baked credentials |
| [observability/prometheus-alerts.yaml](./observability/prometheus-alerts.yaml) | Draft Prometheus rules (keep `draft: true`; wire `runbook` → [runbook alert sections](../docs/operations/runbook.md#observability)) |
| [observability/prometheus-scrape.yaml](./observability/prometheus-scrape.yaml) | Reference-only example scrape job (`docgen-backend`) — no credentials; not mounted by compose |
| [observability/grafana/docgen-ops-overview.json](./observability/grafana/docgen-ops-overview.json) | Grafana dashboard JSON (generation / PDF pool / SSE / rate-limit / DLT) |
| [core-fortress-release-checklist.md](../docs/operations/core-fortress-release-checklist.md) | CORE-FORTRESS release evidence checklist |
| `scripts/core-fortress-evidence-bundle.ps1` | Evidence bundle collector (`release-gate.ps1 -EvidenceBundle`) |
| [blue-green-runbook.md](./blue-green-runbook.md) | Production cutover, manual approval, rollback (P15-T08) |
| [Backup & restore runbook](../docs/operations/backup-restore-runbook.md) | Postgres/MinIO backup + scratch restore drill (LR-D2; **EXECUTED** 2026-07-12 — scratch scope only; not production compliance) |
| [ci-k8s-gates.md](./ci-k8s-gates.md) | Blocking CI manifest validation (P15-T09) |

## Local Docker: install and upgrade

**Acceptance / prod-shaped compose — `JWT_SECRET` + `KAFKA_IMAGE` required:** Before `docker-deploy.ps1`,
`docker-deploy-queue.ps1`, or `docker compose … --profile prod`, operators must export:

1. An **explicit** ≥32-byte `JWT_SECRET` that is **not** a known insecure placeholder
   (`local-dev-only-change-me-please-32bytes-min`, `prod-change-me-32-bytes-minimum-secret`).
   `docker-compose.prod.yml` must **not** use `${JWT_SECRET:-…}` bake-in defaults; missing or
   known-insecure values fail closed. Local-only `.env` / `dev`/`local`/`test` may keep the
   documented test secret in [`.env.example`](../.env.example). Behavior SoT:
   [BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md). Checklist
   [#9](../docs/operations/launch-readiness-checklist.md) is **GO** (merge `587cd9a`) — **not** go-live.
2. An **explicit** `KAFKA_IMAGE` full image ref. Compose uses `${KAFKA_IMAGE:?…}` (**no**
   `:-bitnamilegacy…` silent default). Production / acceptance must use a **company-approved**
   coordinate (`<company-registry>/<kafka-image>:<tag>`). Company registry hostname is **UNKNOWN**
   in-repo — **do not** invent one. Local/dev may use the Hub example in `.env.example`
   (**LOCAL/DEV ONLY**). Behavior SoT:
   [BDD-OPS-KAFKA-REGISTRY-001](../docs/behavior/ops-kafka-company-registry.md). Checklist
   [#10](../docs/operations/launch-readiness-checklist.md) is **CONDITIONAL** — **not** go-live;
   overall checklist remains **NO-GO**.

```powershell
$env:JWT_SECRET = '<explicit-non-default-≥32-bytes>'
$env:KAFKA_IMAGE = '<company-registry>/<kafka-image>:<tag>'
.\scripts\docker-deploy.ps1
# Prefer the single-host queue on this machine:
# .\scripts\docker-deploy-queue.ps1
```

Host-compile, then copy artifacts into runtime images:

```powershell
.\scripts\docker-deploy.ps1
```

- Backend health: `http://localhost:8080/healthz`
- Frontend: `http://localhost:4173`
- Compose files: `docker-compose.yml` + `docker-compose.prod.yml` (`prod` profile)

Upgrade = rebuild on the host and rerun the deploy script. Rollback = redeploy a previous known-good
image tag via compose image references; **do not** destroy database volumes (Flyway is forward-only).

## Kubernetes: validate before install

No cluster required:

```powershell
.\scripts\helm-validate.ps1
```

CI blocking gate (same checks):

```powershell
.\scripts\ci-k8s-manifest-gates.ps1
```

GitHub Actions runs **blocking** K8s manifest gates on pull requests and pushes to `main` when
`deploy/**` or gate scripts change.

| Check | Scope |
| --- | --- |
| `helm lint` | Chart syntax and best practices |
| `helm template` | default, dev, staging, prod value profiles + custom security assertions |
| `kubeconform` | Rendered manifest schema validation (K8s 1.29) |

- Workflow: [`.github/workflows/k8s-manifest-gates.yml`](../.github/workflows/k8s-manifest-gates.yml)
- Details: [ci-k8s-gates.md](./ci-k8s-gates.md)

On CI runners, Helm and kubeconform run via **Docker fallback** when native binaries are not installed.
Locally, use `-SkipKubeconform` only for offline lint/template checks — CI always runs the full gate.

## Kubernetes: install

1. Create namespace and provision the application Secret (**never commit plaintext**):

```powershell
kubectl create namespace docgen
# Operator creates docgen-app-secrets-<env> with required keys — see k8s-config-secrets.md.
```

2. Install with environment values:

```powershell
helm upgrade --install docgen ./deploy/helm/docgen `
  -f deploy/helm/docgen/values-dev.yaml `
  --namespace docgen
```

3. For staging/prod: set real `externalServices.*` hosts, `secrets.existingSecretName`, and Ingress
   hostnames in the matching `values-*.yaml` profile.

## Kubernetes: upgrade

Non-production (single Deployment per workload):

```powershell
helm upgrade docgen ./deploy/helm/docgen `
  -f deploy/helm/docgen/values-staging.yaml `
  --namespace docgen `
  --set backend.image.tag=<tag> `
  --set frontend.image.tag=<tag>
```

Production uses blue-green — deploy to the **inactive color** first, smoke-test via preview Services,
then cut over. See [Production cutover](#production-cutover-blue-green) below.

## Secrets and configuration

Runtime configuration follows ADR-0030: **environment variables + Secret Manager** (cluster Secret or
External Secrets Operator sync).

| Layer | Holds | Must not hold |
| --- | --- | --- |
| ConfigMap (`{release}-config`) | Service endpoints, ports, non-sensitive app flags | Passwords, `JWT_SECRET`, API keys |
| Secret (`existingSecretName`) | `POSTGRES_*`, `MINIO_*`, `JWT_SECRET` | Committed in repo or baked into images |

**`JWT_SECRET` (staging / prod / acceptance):** Required key in the application Secret — **never**
ConfigMap, image, or chart plaintext. Value must be operator-generated (≥32 bytes) and **must not**
equal known insecure defaults above. Helm keeps fail-closed `required` / `existingSecretName`
posture ([k8s-config-secrets.md](./k8s-config-secrets.md)); compose/scripts must not reintroduce
silent defaults ([BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md)).

Default posture: **`secrets.create: false`** — operator provisions `docgen-app-secrets-<env>` before
`helm install`. Optional `ExternalSecret` syncs from Vault or cloud Secret Manager.

Full key matrix, ExternalSecret paths, and fail-closed validation: [k8s-config-secrets.md](./k8s-config-secrets.md).

Secret rotation (ADR-0030): automatic 30-day rotation with zero-downtime pod refresh is an operator
process — chart supports rolling restarts via `helm upgrade`; rotation mechanics live in the secret
store, not the chart.

## Production cutover (blue-green)

Production enables `blueGreen` in `values-prod.yaml`. Two color-labelled Deployments run in parallel;
only `blueGreen.activeColor` receives Ingress traffic.

**Gate:** Production cutover requires **manual approval** before flipping `activeColor`. CI/CD must
fail closed if the approval artifact is missing.

| Step | Action |
| --- | --- |
| 1 | Push new images; upgrade **inactive** color only (image tags on inactive color) |
| 2 | Wait for Ready pods; smoke-test via `*-preview` Services |
| 3 | Record approver + ticket ID |
| 4 | Flip `blueGreen.activeColor` via `helm upgrade --set` |
| 5 | Monitor error rates for the observation window (minimum 15 minutes) |

Full commands, preview Service URLs, and approval controls: [blue-green-runbook.md](./blue-green-runbook.md).

## Rollback

Rollback is a **controlled manual operation** (ADR-0030). Applies to both Kubernetes production
cutover and local Docker image redeploy.

| Environment | Rollback action |
| --- | --- |
| Kubernetes prod (blue-green) | Revert `blueGreen.activeColor` to the previous stable color — previous Deployment stays warm |
| Kubernetes non-prod | `helm upgrade` with previous image tags |
| Local Docker | Redeploy previous image digests/tags via compose; verify `/healthz` |

**Do not** delete Deployments, PersistentVolumeClaims, or database volumes on rollback. Flyway
migrations are **forward-only** — reverting traffic does not revert schema.

Detailed prod rollback steps: [blue-green-runbook.md § Manual rollback](./blue-green-runbook.md#manual-rollback-p15-t08c).

## Container hardening

Packaged images run non-root with read-only root filesystem and dropped capabilities. Validation
(prod-shaped smoke — **requires explicit `JWT_SECRET`**; must **not** silently fall back to
`prod-change-me-32-bytes-minimum-secret`):

```powershell
$env:JWT_SECRET = '<explicit-non-default-≥32-bytes>'
.\scripts\container-hardening-smoke.ps1
```

Details: [container-hardening.md](./container-hardening.md). Behavior:
[BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md) S4.

## Chart and script layout

| Path | Contents |
| --- | --- |
| `deploy/helm/docgen/` | Helm chart (Deployments, Services, Ingress, HPA, NetworkPolicy, …) |
| `deploy/helm/docgen/values-*.yaml` | Per-environment overrides (dev, staging, prod) |
| `scripts/helm-validate.ps1` | lint + template + kubeconform |
| `scripts/ci-k8s-manifest-gates.ps1` | CI entry point |
| `scripts/docker-deploy.ps1` | Local host-build + compose prod rollout |
| `backend/Dockerfile.packaged`, `frontend/Dockerfile.packaged` | Hardened runtime images |

## Architecture and plan cross-links

- [ADR-0030 Operational Platform Baseline](../docs/adr/operations/0030-operational-platform-baseline.md) — accepted deployment, hardening, and CD decisions
- [Runtime view](../docs/architecture/runtime-view.md) — workload topology, health endpoints, local compose baseline
- [Security view](../docs/architecture/security-view.md) — container hardening, network isolation, secret handling
- [Data storage view](../docs/architecture/data-storage-view.md) — external data services and retention
- [P15 detailed plan](../docs/plan/detail/P15-kubernetes-deployment-container-hardening.md) — phase tasks and exit criteria
- [Production runbook](../docs/operations/runbook.md) — release gate, local prod compose profile, **LR-D3 alert response sections**, **JWT_SECRET** + **KAFKA_IMAGE** explicit provision
- [BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md) — no compose JWT default; known insecure refused; checklist #9 **GO** (not go-live)
- [BDD-OPS-KAFKA-REGISTRY-001](../docs/behavior/ops-kafka-company-registry.md) — fail-closed `KAFKA_IMAGE`; Hub example LOCAL/DEV ONLY; checklist #10 **CONDITIONAL** (not go-live)
- [Observability as code](./observability/README.md) — LR-D3 draft alert thresholds + `runbook` annotation targets (NOT confirmed SLOs)
- [Backup & restore runbook](../docs/operations/backup-restore-runbook.md) — LR-D2 pg/MinIO restore + confirmation gate + drill evidence (**EXECUTED** 2026-07-12; scratch ≠ production compliance)
