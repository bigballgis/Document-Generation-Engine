# Kubernetes Health Probes (P15-T07 / ADR-0030)

Dual-endpoint health checks for **backend** (Spring Boot) and **frontend** (NGINX static
serving). Implements ADR-0030 row: **`/healthz` (liveness) + `/readyz` (readiness)**.

## T07 task mapping

| Task | Deliverable | Evidence |
| --- | --- | --- |
| **P15-T07a** | Backend Deployment — `httpGet` liveness `/healthz`, readiness `/readyz` on port **8080** | `Assert-T07Probes` in `helm-validate.ps1` |
| **P15-T07b** | Frontend Deployment — same probe paths; NGINX ConfigMap serves `/healthz` and `/readyz` | Rendered manifests; this doc |
| **P15-T07c** | Backend Spring endpoints — distinct liveness vs readiness semantics | `HealthController`, `ReadinessProbe` |

## Probe semantics (Kubernetes)

| Probe | Endpoint | Purpose | Failure action |
| --- | --- | --- | --- |
| **Liveness** | `/healthz` | JVM / NGINX process is alive | kubelet **restarts** the container |
| **Readiness** | `/readyz` | Workload can accept traffic | Pod **removed** from Service endpoints; no restart |

Readiness gates traffic during startup and dependency outages. Liveness must stay lightweight
so transient dependency blips do not cause restart loops.

## Backend (Spring Boot)

Implementation: `backend/src/main/java/com/bank/docgen/sharedkernel/health/`.

| Endpoint | Handler | HTTP | Body | Checks |
| --- | --- | --- | --- | --- |
| `/healthz` | `HealthController.liveness()` | **200** always (when process up) | `{"status":"UP"}` | Process alive only — **no** dependency checks |
| `/readyz` | `HealthController.readiness()` | **200** when Postgres up, **503** when Postgres down | Structured JSON — see below | Traffic gate: Postgres `SELECT 1`; optional Redis/MinIO/Kafka in `checks.*` |

### `/readyz` response (F8-B2)

Traffic gating remains **Postgres-only** (SOR-O06). Optional dependency probes report status without affecting HTTP 200 when Postgres is healthy.

```json
{
  "status": "UP",
  "checks": {
    "postgres": { "status": "UP" },
    "redis": { "status": "UP" },
    "minio": { "status": "UP" },
    "kafka": { "status": "SKIPPED", "detail": "async transport is not kafka" }
  }
}
```

| `checks.*.status` | Meaning |
| --- | --- |
| `UP` | Component reachable |
| `DOWN` | Probe failed (diagnostic only unless `postgres`) |
| `SKIPPED` | Not configured (e.g. Kafka when `ASYNC_TRANSPORT!=kafka`) |

Implementation: `ReadinessProbe`, `ReadinessReport`, `*ReadinessContributor` (`@Profile("!test")`).

Both paths are permit-all in `SecurityConfig` (no JWT).

**Readiness does not delegate to `/healthz`.** Liveness and readiness are separate endpoints
with different failure semantics, matching ADR-0030 and Kubernetes best practice.

When Postgres is unavailable, `/readyz` returns **503** → pod goes NotReady → removed from
Service endpoints while liveness keeps the container running until recovery or manual intervention.

## Frontend (NGINX)

Static SPA container; probes hit in-container NGINX on port **8080** (unprivileged).

ConfigMap `templates/frontend-nginx-configmap.yaml`:

```nginx
location /healthz {
    access_log off;
    default_type text/plain;
    return 200 'ok\n';
}

location /readyz {
    access_log off;
    default_type text/plain;
    return 200 'ok\n';
}
```

For static file serving, liveness and readiness both confirm NGINX is listening and can
serve responses. No upstream dependency checks are required on the frontend tier.

## Helm values (`backend.probes` / `frontend.probes`)

Defaults in `deploy/helm/docgen/values.yaml`:

| Workload | Probe | Path | `initialDelaySeconds` | `periodSeconds` | `timeoutSeconds` | `failureThreshold` |
| --- | --- | --- | --- | --- | --- | --- |
| Backend | Liveness | `/healthz` | 30 | 10 | 5 | 3 |
| Backend | Readiness | `/readyz` | 10 | 10 | 5 | 3 |
| Frontend | Liveness | `/healthz` | 5 | 10 | 3 | 3 |
| Frontend | Readiness | `/readyz` | 5 | 10 | 3 | 3 |

Templates wire probes as:

```yaml
livenessProbe:
  httpGet:
    path: /healthz
    port: http          # named port → containerPort 8080
readinessProbe:
  httpGet:
    path: /readyz
    port: http
```

Blue-green Deployments (`backend-color-deployments.yaml`, `frontend-color-deployments.yaml`)
use the same probe block from values.

## Validation (render only — no cluster)

```powershell
.\scripts\helm-validate.ps1 -SkipKubeconform
```

`Assert-T07Probes` checks:

- Every backend Deployment: liveness `/healthz`, readiness `/readyz`, `httpGet` on named port `http` / **8080**
- Every frontend Deployment: same probe wiring
- Frontend NGINX ConfigMap includes `location /healthz` and `location /readyz`
- Sensible `initialDelaySeconds` and `failureThreshold` minimums per workload

## Local Docker smoke (non-Kubernetes)

| Target | Command | Expected |
| --- | --- | --- |
| Backend liveness | `curl -sf http://localhost:8080/healthz` | HTTP 200 |
| Backend readiness | `curl -sf http://localhost:8080/readyz` | HTTP 200 (deps up) |
| Frontend | `curl -sf http://localhost:4173/healthz` | HTTP 200 |

See also `scripts/container-hardening-smoke.ps1` and [container-hardening.md](./container-hardening.md).

## Related docs

- [deploy/README.md](./README.md) — Kubernetes deployment guide
- [deploy/blue-green-runbook.md](./blue-green-runbook.md) — preview cutover uses `/readyz` on backend
- [ADR-0030](../docs/adr/operations/0030-operational-platform-baseline.md) — dual health endpoint baseline
- [Runtime view](../docs/architecture/runtime-view.md) — workload topology
- [P15 detailed plan](../docs/plan/detail/P15-kubernetes-deployment-container-hardening.md)
