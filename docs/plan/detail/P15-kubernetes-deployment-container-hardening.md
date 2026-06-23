# P15 — Kubernetes Deployment & Container Hardening (Detailed Plan)

**Phase status:** Not Started | **Depends on:** P9
**Created:** 2026-06-23 (maintainer decision — Docker/K8s operational baseline not yet implemented)

> Single-active-phase invariant: P13 completed **Done** (2026-06-23); there is currently no
> single active phase. P15 stays `Not Started` and must not be activated until it is
> selected as the next active phase.

## Source-of-truth & traceability

- **ADR-0030 — Operational Platform Baseline** (Accepted, sourceOfTruth):
  [0030-operational-platform-baseline.md](../../adr/operations/0030-operational-platform-baseline.md).
  This phase implements the Kubernetes/container rows of ADR-0030; it does **not** change
  any accepted decision.
- Architecture views cross-referenced:
  - [runtime-view.md](../../architecture/runtime-view.md) — workload topology, health endpoints, scaling.
  - [security-view.md](../../architecture/security-view.md) — container hardening, network isolation, TLS, secrets.
  - [data-storage-view.md](../../architecture/data-storage-view.md) — at-rest/in-transit encryption, external data services.

## Scope decision (confirmed assumption)

- **Stateful dependencies (PostgreSQL, Redis, Kafka, MinIO) are treated as externally
  managed services** for this phase. K8s manifests cover only the application workloads
  (`backend`, `frontend`) plus their `ConfigMap`/`Secret`/`Service`/`Ingress`/
  `NetworkPolicy`/`HPA`/probes. Data services are referenced via configuration and Secret
  references (e.g. external Secret / `ExternalName` or env-injected endpoints); no in-cluster
  database/broker/storage StatefulSets are authored here.
- Already implemented (do **not** re-plan): `docker-compose.yml`, `docker-compose.prod.yml`,
  `backend/Dockerfile` (non-root `docgen` user), `frontend/Dockerfile` (node build + nginx).

## Behavior goal

Make the platform deployable to Kubernetes with the ADR-0030 hardening posture: minimal/
distroless non-root read-only containers, declarative manifests/Helm chart for application
workloads, externalized configuration and secrets, NGINX Ingress with cert-manager TLS,
default-deny network policy, liveness/readiness probes on `/healthz` and `/readyz`, CPU/
memory + custom-metric autoscaling, and a blue-green release path with manual approval and
manual rollback — all validated in CI.

### Operator behavior (actor / goal / trigger)

- **Actor:** Platform operator / release engineer (cluster-admin scoped; outside the
  in-app `ManagementRole` model).
- **Goal:** Deploy and operate `backend` + `frontend` on Kubernetes per ADR-0030.
- **Trigger:** Promotion of a built image to a target environment (non-prod auto, prod gated).
- **Preconditions:** Externally managed Postgres/Redis/Kafka/MinIO reachable; cluster has
  NGINX Ingress Controller, cert-manager, and a metrics pipeline available.
- **Observable evidence:** `kubeconform`/`helm lint`/`helm template` green in CI; pods run
  non-root with read-only root FS; probes gate traffic; HPA scales under load; default-deny
  NetworkPolicy blocks unlisted traffic; blue-green cutover + manual rollback runbook executes.

---

## P15-T01 Container hardening (distroless, read-only root, non-root frontend)

### Behavior

- Backend and frontend images use a distroless/minimal base, run as a non-root user, and
  run with a read-only root filesystem (writable paths via explicit `emptyDir`/tmpfs mounts).
- Frontend NGINX runs as non-root (unprivileged NGINX image or reconfigured to bind >1024
  and write only to mounted tmp/cache paths).

### Acceptance scenarios

- **Given** the built backend image, **When** it runs with `readOnlyRootFilesystem: true`
  and `runAsNonRoot: true`, **Then** the app starts and serves `/healthz` without writing to
  the root FS.
- **Given** the frontend image, **When** it runs as non-root with read-only root FS, **Then**
  NGINX serves static assets and the SPA loads.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T01a | Backend image to distroless/minimal base, non-root, read-only root FS + writable mounts | Not Started |
| P15-T01b | Frontend NGINX non-root + read-only root FS (unprivileged config, tmp/cache mounts) | Not Started |
| P15-T01c | Document required writable paths + image-run smoke evidence | Not Started |

**Exit / evidence:** Both images start under hardened runtime flags locally (docker run with
`--read-only` + non-root) and serve health/SPA; writable mount list documented.

---

## P15-T02 Kubernetes manifests / Helm chart for application workloads

### Behavior

- A Helm chart (or kustomize base) under a new `deploy/` (e.g. `deploy/helm/docgen/`) renders
  `Deployment` + `Service` for `backend` and `frontend`, parameterized by environment values.
- Pod security context enforces non-root + read-only root FS + dropped capabilities.
- Both `requests` and `limits` (CPU + memory) are set for every container (ADR-0030).

### Acceptance scenarios

- **Given** chart values for an environment, **When** `helm template` runs, **Then** valid
  manifests render with security context and resource requests+limits on every container.
- **Given** rendered manifests, **When** `kubeconform` validates them, **Then** they pass
  against the target Kubernetes schema.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T02a | Chart scaffold + values (per-env overrides) for backend/frontend | Not Started |
| P15-T02b | Deployment + Service with pod securityContext (non-root, read-only, drop caps) | Not Started |
| P15-T02c | Resource requests + limits (CPU/memory) on all containers | Not Started |

**Exit / evidence:** `helm lint`, `helm template`, and `kubeconform` pass; every container
has requests+limits and hardened security context.

---

## P15-T03 Configuration & secrets (ConfigMap / Secret + external references)

### Behavior

- Non-sensitive runtime configuration is supplied via `ConfigMap`; sensitive values
  (DB/Redis/Kafka/MinIO endpoints + credentials, JWT/KMS-managed keys) via `Secret` or
  external-secret references — never baked into images or values committed in plaintext.
- Externally managed data services are referenced via Secret/config, aligning with the
  ADR-0030 KMS-managed key and at-rest/in-transit encryption posture.

### Acceptance scenarios

- **Given** an environment, **When** the chart renders, **Then** app config comes from
  `ConfigMap` and all credentials from `Secret`/external-secret refs (no plaintext secrets
  in the repo).
- **Given** a missing required secret, **When** the pod starts, **Then** it fails closed with
  a clear startup error (no silent insecure default).

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T03a | ConfigMap for non-sensitive runtime config (per-env values) | Not Started |
| P15-T03b | Secret / external-secret references for credentials + KMS-managed keys | Not Started |
| P15-T03c | External managed-service endpoint wiring (config/Secret, no in-cluster StatefulSets) | Not Started |

**Exit / evidence:** No plaintext secrets committed; rendered pods consume ConfigMap + Secret;
fail-closed on missing secret documented/tested via render.

---

## P15-T04 Service exposure: Service + Ingress (NGINX) + cert-manager TLS + K8s DNS

### Behavior

- `Service` objects expose backend/frontend in-cluster; service-to-service uses native
  Kubernetes DNS.
- NGINX Ingress Controller routes external traffic; cert-manager issues and auto-renews TLS
  certificates (TLS 1.2+).

### Acceptance scenarios

- **Given** the Ingress + cert-manager `Certificate`/issuer annotations, **When** rendered,
  **Then** manifests reference the NGINX ingress class and a cert-manager issuer for TLS.
- **Given** in-cluster callers, **When** they resolve a Service by DNS name, **Then** routing
  works without hardcoded IPs.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T04a | Service definitions (backend/frontend) using K8s DNS naming | Not Started |
| P15-T04b | NGINX Ingress resource with class + host/path routing | Not Started |
| P15-T04c | cert-manager Certificate/issuer integration for TLS 1.2+ | Not Started |

**Exit / evidence:** Rendered Ingress uses NGINX class + cert-manager annotations; TLS host(s)
configured; `kubeconform` passes.

---

## P15-T05 Autoscaling: HPA (CPU/memory + custom metrics)

### Behavior

- `HorizontalPodAutoscaler` scales backend (and frontend where applicable) on CPU and memory
  utilization plus at least one custom metric (e.g. request rate / queue depth) per ADR-0030.

### Acceptance scenarios

- **Given** HPA config, **When** rendered, **Then** it targets the workload with CPU + memory
  metrics and a custom metric source, with min/max replica bounds.
- **Given** sustained load (documented test), **When** utilization exceeds targets, **Then**
  replicas scale up and back down.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T05a | HPA (CPU + memory) with min/max bounds for backend | Not Started |
| P15-T05b | Custom-metric scaling source wiring (metrics adapter assumption documented) | Not Started |

**Exit / evidence:** HPA manifest validates; scaling behavior demonstrated or documented with
the metrics-adapter prerequisite stated.

---

## P15-T06 Network isolation: default-deny NetworkPolicy + explicit allow

### Behavior

- A default-deny `NetworkPolicy` applies to the application namespace; explicit allow rules
  permit only required flows (Ingress→frontend/backend, backend→external data services,
  DNS, metrics scrape) per ADR-0030.

### Acceptance scenarios

- **Given** the default-deny policy plus allow rules, **When** rendered, **Then** ingress and
  egress are denied by default and only listed peers/ports are allowed.
- **Given** an unlisted flow, **When** attempted (documented test), **Then** it is blocked.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T06a | Default-deny ingress+egress NetworkPolicy for app namespace | Not Started |
| P15-T06b | Explicit allow rules (ingress, backend↔external services, DNS, metrics) | Not Started |

**Exit / evidence:** Policies validate; allow-list documented and traced to required flows.

---

## P15-T07 Health probes wired to /healthz and /readyz

### Behavior

- Deployment probes use the dual-endpoint baseline: liveness → `/healthz`, readiness →
  `/readyz` (ADR-0030). Readiness gates traffic during startup/dependency outages.

### Acceptance scenarios

- **Given** the backend Deployment, **When** rendered, **Then** liveness probe hits `/healthz`
  and readiness probe hits `/readyz` with sane timing.
- **Given** dependencies unavailable, **When** `/readyz` reports not-ready, **Then** the pod
  is removed from Service endpoints; liveness keeps the container running.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T07a | Liveness probe → `/healthz`, readiness probe → `/readyz` on backend Deployment | Not Started |
| P15-T07b | Frontend readiness/liveness probes (static-serving health) | Not Started |
| P15-T07c | Confirm/expose `/healthz` + `/readyz` semantics align with probes | Not Started |

**Exit / evidence:** Probes reference the correct endpoints; readiness gating demonstrated.

---

## P15-T08 Blue-green release: manifests + manual approval + manual rollback

### Behavior

- Blue-green deployment manifests/strategy for application workloads; production cutover
  requires manual approval; rollback is a controlled manual operation (ADR-0030).

### Acceptance scenarios

- **Given** a new image, **When** a blue-green deploy runs, **Then** the new color is brought
  up and validated before Service traffic switches.
- **Given** a production release, **When** approval is pending, **Then** cutover does not
  proceed until a human approves.
- **Given** a regression, **When** rollback is triggered, **Then** traffic returns to the
  previous color via documented manual steps.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T08a | Blue-green deployment manifests/strategy (color selector + traffic switch) | Not Started |
| P15-T08b | Production manual-approval gate (pipeline/release control) | Not Started |
| P15-T08c | Manual rollback runbook (color revert steps) | Not Started |

**Exit / evidence:** Blue-green render validates; approval gate documented; rollback runbook
steps verified in a dry run.

---

## P15-T09 CI/CD integration & manifest validation

### Behavior

- CI validates K8s artifacts on push-to-main + PRs (ADR-0030 CI strategy): `helm lint`,
  `helm template`, and `kubeconform` (and optional policy checks) as blocking gates.

### Acceptance scenarios

- **Given** a PR touching `deploy/`, **When** CI runs, **Then** chart lint + render +
  `kubeconform` execute and block merge on failure.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T09a | CI job: `helm lint` + `helm template` for all envs | Not Started |
| P15-T09b | CI job: `kubeconform` validation of rendered manifests (blocking) | Not Started |

**Exit / evidence:** CI logs show K8s validation gates green and blocking on failure.

---

## P15-T10 Deployment docs / runbook + post-task doc sync

### Behavior

- A deployment guide / runbook documents prerequisites (external services, NGINX Ingress,
  cert-manager, metrics adapter), install/upgrade, blue-green cutover, rollback, and secret
  handling; indexes and cross-links updated. All user-facing strings (if any) stay
  English-first.

### Tasks

| ID | Task | Status |
| --- | --- | --- |
| P15-T10a | Deployment guide + runbook (install/upgrade/cutover/rollback/secrets) | Not Started |
| P15-T10b | Cross-link ADR-0030 + runtime/security/data-storage views; update `docs/README.md` | Not Started |
| P15-T10c | Post-task doc sync + execution-sync-ledger evidence backfill | Not Started |

**Exit / evidence:** Runbook reachable from `docs/README.md`; ADR/view cross-links present;
ledger evidence recorded.

---

## Exit criteria (phase)

- All P15-T01…T10 tasks Done with real, durable manifests/chart (not snippets-only).
- Containers run non-root + read-only root FS on a distroless/minimal base; frontend NGINX
  non-root.
- Application workloads render valid manifests with requests+limits, hardened security
  context, ConfigMap/Secret config, NGINX Ingress + cert-manager TLS, default-deny
  NetworkPolicy, HPA (CPU/memory + custom metric), and `/healthz`/`/readyz` probes.
- Blue-green release path with production manual approval and manual rollback runbook.
- CI K8s validation gates (`helm lint` + `helm template` + `kubeconform`) green and blocking.
- Stateful dependencies remain externally managed (referenced via config/Secret).
- Docs/runbook updated and cross-linked; execution-sync-ledger evidence recorded.
