---
id: DOC-ARCH-RUNTIME-VIEW
type: Architecture View
status: Accepted
sourceOfTruth: true
owners:
  - architecture
dependsOn:
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
  - docs/api/contract-outline.md
  - docs/architecture/module-boundaries.md
related:
  - docs/architecture/data-storage-view.md
  - docs/architecture/async-messaging-view.md
  - docs/architecture/security-view.md
---

# Runtime View

## Purpose

This view describes the runtime shape needed to support dynamic API calls, template governance, asynchronous generation, and rendering isolation.

## Baseline Runtime Components

| Component | Responsibility |
| --- | --- |
| Management Frontend | Vue-based management UI for master documents, templates, API management, release governance, and audit views. |
| Core API Service | Management APIs, dynamic generation APIs, authorization orchestration, idempotency, task state, policy checks, audit publication, and contract discovery. |
| Rendering Worker or Service | Executes rendering tasks, DOCX assembly, PDF conversion, encryption execution, preview generation, and rendering diagnostics. |
| Relational Database | Stores durable business state, template lifecycle data, API policy state, idempotency records, task metadata, and audit records. |
| Redis or Cache Platform | Supports short-lived authorization cache, idempotency coordination, short-lived task metadata acceleration, and download metadata acceleration where approved. |
| Object Storage or File Service | Stores DOCX masters, generated documents, previews, and large intermediate artifacts. |
| Kafka | Carries asynchronous generation tasks, rendering events, audit/event fan-out, retry flows, and dead-letter flows. |
| AD / Directory Integration Adapter | Resolves AD Group membership according to confirmed cache and fail-closed rules. |

## Local Deployment Baseline

The local fullstack compose topology uses standardized service names so operational commands, verification output, and logs stay consistent across worktrees:

| Service | Responsibility |
| --- | --- |
| `docgen-backend` | Spring backend application. |
| `docgen-frontend` | Vite development server for the management frontend. |
| `docgen-postgres` | Local PostgreSQL database. |
| `docgen-redis` | Local Redis cache. |
| `docgen-kafka` | Single-node KRaft Kafka broker. |
| `docgen-minio` | Local MinIO object-storage API and console. |

Local stack startup is script-first: `scripts/local-stack-up.ps1` prepares backend runtime artifacts, starts compose with image builds, and `scripts/local-stack-verify.ps1` validates running services and reachable ports. Frontend image builds must consume npm registry configuration through BuildKit secrets and must not embed `.npmrc` or credentials in tracked files or image layers.

## Runtime Flow Baseline

Synchronous generation:

```text
API caller
    -> Core API Service
        -> authorization and policy checks
        -> generation orchestration
        -> Rendering Worker or Service
        -> file stream or download result
        -> audit summary
```

Asynchronous generation:

```text
API caller
    -> Core API Service
        -> authorization, idempotency, and policy checks
        -> task accepted
        -> Kafka task message
        -> Rendering Worker or Service
        -> object storage result
        -> task state update
        -> audit summary
```

## Runtime Rules

- Rendering isolation is mandatory at the architecture level even if the first implementation deploys core and worker code together.
- Local deployment automation uses direct pnpm or the shared pnpm resolver, not Corepack, so restricted enterprise registry/proxy configuration is honored.
- Kafka messages carry safe identifiers and summaries, not sensitive payloads or generated files.
- Authorization must happen before protected operations or sensitive responses.
- Generated files and previews use object storage or equivalent file service references.
- Audit records are durable and queryable; operational logs do not replace audit records.

## Kubernetes deployment (P15 / ADR-0030)

Production and staging application workloads deploy to Kubernetes via the Helm chart under
`deploy/helm/docgen/`. Stateful data services remain external; the chart wires endpoints through
ConfigMap and credentials through Secret references only.

| Topic | Guide |
| --- | --- |
| Canonical install / upgrade / cutover / rollback | [Deployment guide](../../deploy/README.md) |
| Operational baseline decisions | [ADR-0030](../adr/operations/0030-operational-platform-baseline.md) |
| Health probes (`/healthz`, `/readyz`) | [k8s-health-probes.md](../../deploy/k8s-health-probes.md) |
| HPA autoscaling | [k8s-hpa-autoscaling.md](../../deploy/k8s-hpa-autoscaling.md) |
| Blue-green production cutover | [blue-green-runbook.md](../../deploy/blue-green-runbook.md) |
| Local acceptance (Docker Compose) | [Production runbook](../operations/runbook.md), `scripts/docker-deploy.ps1` |

## Pending Questions

- Whether the first implementation deploys as modular monolith plus worker process, or separate services from the beginning.
- Production runtime platform and deployment topology.
