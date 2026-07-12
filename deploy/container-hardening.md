# Container Hardening (P15-T01 / ADR-0030)

Implements the ADR-0030 container rows: **distroless/minimal base**, **non-root**, and
**read-only root filesystem** with explicit writable mounts. Kubernetes `securityContext`
and Helm wiring are delivered in P15-T02+.

## Images

| Workload | Base image | Runtime user | Listen port |
| --- | --- | --- | --- |
| Backend | `eclipse-temurin:21-jre-alpine` (minimal; UID 65532 matches distroless `nonroot`) | UID/GID 65532 | 8080 |
| Frontend | `nginx:1.27-alpine` (non-root master via `nginx-main.conf`) | `nginx` (UID 101) | 8080 |

ADR-0030 names **distroless/minimal** bases. Packaged Dockerfiles use the minimal alpine
variants above with the same non-root UID and read-only-root + tmpfs contract. Operators with
registry access may swap to `gcr.io/distroless/java21-debian12:nonroot` and
`nginxinc/nginx-unprivileged:1.27-alpine` without changing mount requirements.

Build artifacts are copied on the host (`scripts/docker-deploy.ps1`); Dockerfiles under
`backend/Dockerfile.packaged` and `frontend/Dockerfile.packaged` only assemble runtime layers.

## Read-only root filesystem — writable paths

Mount these paths as `tmpfs` (Compose) or `emptyDir`/tmpfs (Kubernetes) when
`readOnlyRootFilesystem: true`:

### Backend (`docgen-backend`)

| Mount | Purpose |
| --- | --- |
| `/tmp` | JVM temp (`java.io.tmpdir`), embedded Tomcat work files, short-lived IO |

No other runtime writes to the root filesystem are required for the current Spring Boot
configuration (logging to stdout; no local artifact storage in the app container).

### Frontend (`docgen-frontend`)

| Mount | Purpose |
| --- | --- |
| `/tmp` | NGINX pid, logs, client/proxy temp paths (`nginx-main.conf`) |

Static assets and NGINX config are baked into the image and remain read-only at runtime.

**P15-T01b image contract (`frontend/Dockerfile.packaged`):**

- Base: `nginx:1.27-alpine`; runtime `USER nginx` (UID/GID 101).
- Listen port **8080** (unprivileged; no `CAP_NET_BIND_SERVICE`).
- `nginx-main.conf` relocates pid, logs, and temp paths under `/tmp`.
- `docker-entrypoint-hardened.sh` creates `/tmp/nginx/*` temp dirs before `nginx -g 'daemon off;'`.

**Writable paths under `/tmp` (via tmpfs):**

| Path | Purpose |
| --- | --- |
| `/tmp/nginx.pid` | Master process pid file |
| `/tmp/error.log`, `/tmp/access.log` | NGINX logs |
| `/tmp/nginx/client_temp` | Client body buffer |
| `/tmp/nginx/proxy_temp` | Upstream proxy temp |
| `/tmp/nginx/fastcgi_temp` | FastCGI temp |
| `/tmp/nginx/uwsgi_temp` | uWSGI temp |
| `/tmp/nginx/scgi_temp` | SCGI temp |

## Local verification

After host compile + image build, export an **explicit** ≥32-byte `JWT_SECRET` that is **not**
a known insecure default (`local-dev-only-change-me-please-32bytes-min`,
`prod-change-me-32-bytes-minimum-secret`). The smoke script is prod-shaped: it must **not**
silently fall back to `prod-change-me-32-bytes-minimum-secret` when unset
([BDD-OPS-JWT-SECRET-001](../docs/behavior/ops-jwt-secret-no-default.md) S4).

```powershell
$env:JWT_SECRET = '<explicit-non-default-≥32-bytes>'
.\scripts\container-hardening-smoke.ps1
```

The smoke script runs each image with `docker run --read-only`, non-root user, and the
writable tmpfs mounts above, then checks:

- Backend: `GET /healthz` → HTTP 200
- Frontend: `GET /` and `GET /healthz` → HTTP 200

`Remove-SmokeContainers` in the script suppresses benign `docker rm` stderr so the run
does not abort under `$ErrorActionPreference = Stop` when prior smoke containers are absent.

### P15-T01 smoke evidence (2026-06-27)

**Frontend image:** `documentgenerationengine-docgen-frontend:latest`
(`sha256:b0e6b41dbf7d39bd7ba750cec65d9fb64f72270eba005d6bc5967939e44d7a22`)

**Hardened `docker run` flags (frontend slice):**

```text
--read-only --user nginx --tmpfs /tmp:rw,noexec,nosuid,size=64m --security-opt no-new-privileges:true
```

**Observed (2026-06-27):**

| Check | Result |
| --- | --- |
| `ReadonlyRootfs` | `true` |
| Container user | `nginx` → `uid=101(nginx) gid=101(nginx)` |
| Write to `/etc` | `Read-only file system` (blocked) |
| Write to `/tmp` | OK |
| `GET http://localhost:14173/healthz` | **200** |
| `GET http://localhost:14173/` (SPA) | **200** |
| `.\scripts\container-hardening-smoke.ps1` | **PASSED** (backend + frontend) |

Production-style compose (`docker-compose.prod.yml`) applies the same read-only + tmpfs
posture for day-to-day validation via `.\scripts\docker-deploy.ps1`.

## Kubernetes (P15-T02)

Helm chart: `deploy/helm/docgen/`. Pod and container `securityContext` mirror the UID
and writable-mount contract above (backend UID 65532, frontend UID 101; `/tmp` as
`emptyDir`). External Postgres/Redis/Kafka/MinIO endpoints are supplied via ConfigMap;
credentials via cluster Secret reference (`secrets.existingSecretName`). Validate with
`.\scripts\helm-validate.ps1`.

## Health checks

- **Backend (minimal JRE):** no in-container HTTP probe binary required; compose disables
  in-container healthcheck. Readiness is verified from the host (`docker-deploy.ps1` polls
  `/healthz`). Kubernetes probes (P15-T07) use HTTP `httpGet` on `/healthz` / `/readyz` —
  see [k8s-health-probes.md](./k8s-health-probes.md).
- **Frontend:** in-container probe via `wget` against `/healthz` on port 8080.

## Rollback note

Hardening is image + runtime mount configuration only. Roll back by redeploying the prior
image tag; do not destroy data volumes (Flyway is forward-only).

## Traceability

- [ADR-0030 — Operational Platform Baseline](../docs/adr/operations/0030-operational-platform-baseline.md)
- [P15 detailed plan — T01](../docs/plan/detail/P15-kubernetes-deployment-container-hardening.md)
