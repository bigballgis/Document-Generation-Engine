---
id: ADR-0030
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0030"
topic: operations
related:
  - docs/architecture/README.md
  - docs/architecture/runtime-view.md
  - docs/architecture/data-storage-view.md
  - docs/architecture/security-view.md
  - docs/architecture/async-messaging-view.md
  - docs/architecture/ai-development-guide.md
---

# ADR 0030: Operational Platform Baseline

## Status

Accepted

## Context

The backend and frontend application baselines are now fixed. The project also needs a stable operational platform baseline covering delivery, deployment, observability, retention, backups, disaster recovery, configuration, and runtime hardening so the architecture views remain rebuildable and implementation-ready.

This ADR records the confirmed operational platform decisions that were previously held only as session-confirmed ledger items.

## Decision

The confirmed operational platform baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| CI pipeline trigger strategy | Dual triggers: push to main branch + pull requests | CI runs on direct main-branch updates and pull requests. |
| CI parallel execution strategy | Stage-level parallelization (lint/type/test split) | Quality gates can run in parallel by stage. |
| CI quality gate failure policy | Block merge on any failed quality gate | Any blocking gate failure prevents merge. |
| CD release strategy | Blue-green deployment | Production releases use blue-green style cutover. |
| Release approval strategy | Manual approval required for production; automatic for non-production | Production releases require explicit human approval. |
| Release rollback strategy | Manual rollback trigger | Rollback is a controlled manual operation. |
| Database backup strategy | Weekly full backup + daily incremental backups | Backup cadence baseline for durable data. |
| Database recovery drill frequency strategy | Yearly recovery drill | Recovery drills are practiced at least annually. |
| Cross-region disaster recovery strategy | Active-passive cross-region deployment | Disaster recovery baseline uses active-passive topology. |
| RPO target strategy | RPO <= 15 minutes | Recovery point objective baseline. |
| RTO target strategy | RTO <= 30 minutes | Recovery time objective baseline. |
| Disaster recovery failover strategy | Switch after manual confirmation | Failover requires manual confirmation. |
| Data-in-transit encryption protocol strategy | End-to-end TLS 1.2+ | Transport encryption baseline. |
| Data-at-rest encryption strategy | AES-256 | At-rest encryption baseline. |
| Key management strategy | KMS-managed master keys | Key management baseline for operational encryption. |
| Observability log collection strategy | OpenTelemetry + centralized logging platform | Logging baseline for operational diagnostics. |
| Observability metrics collection strategy | OpenTelemetry Metrics + Prometheus | Metrics baseline. |
| Observability tracing strategy | OpenTelemetry Tracing + Jaeger/Tempo | Distributed tracing baseline. |
| Alert notification channel strategy | Email only | Operational alert delivery baseline. |
| Error tracking platform strategy | Sentry | Error tracking baseline. |
| Health check endpoint strategy | Dual endpoints: /healthz (liveness) + /readyz (readiness) | Runtime health-check baseline. |
| Configuration management strategy | Environment variables + Secret Manager | Runtime configuration baseline. |
| Secret rotation strategy | Automatic rotation every 30 days + zero-downtime application refresh | Secret lifecycle baseline. |
| Audit log storage strategy | PostgreSQL audit tables + object storage archival | Audit record storage baseline. |
| Audit log retention strategy | Database retention 180 days + object storage retention 3 years | Audit retention baseline. |
| Container baseline image strategy | Distroless/minimal base image | Container hardening baseline. |
| Container runtime permission strategy | Run as non-root user + read-only root filesystem | Container hardening baseline. |
| Kubernetes resource requests and limits strategy | Enforce both requests and limits | Resource governance baseline. |
| Kubernetes autoscaling strategy | HPA based on CPU/memory + custom metrics | Autoscaling baseline. |
| Kubernetes network policy strategy | Default deny + explicit allow rules as needed | Network isolation baseline. |
| Ingress controller strategy | NGINX Ingress Controller | Ingress baseline. |
| TLS certificate management strategy | cert-manager automatic issuance and renewal | Certificate automation baseline. |
| Service-to-service authentication strategy | Internal network trust only (no mutual TLS) | Baseline service-to-service trust model for the current phase. |
| Service discovery strategy | Kubernetes DNS native service discovery | Service discovery baseline. |

These decisions are accepted as the operational platform foundation. They define the default deployment, runtime, observability, and resilience posture for the system.

## Consequences

- Delivery and runtime behavior now have an explicit operations baseline rather than scattered session notes.
- The architecture views can reference one operational baseline for deployment, backup, DR, and observability assumptions.
- Future changes to operational posture should update this ADR and the affected architecture views together.
- More detailed security and API behavior choices remain available for separate confirmation and ADR synchronization.

## Alternatives Considered

- Keeping operational choices only in the technology decision log: rejected because durable decisions need an ADR.
- Splitting every operational concern into its own ADR: rejected for this baseline because the decisions are tightly coupled around deployment and runtime operations.
- Leaving production deployment, backup, and DR open until implementation starts: rejected because those choices shape architecture views and delivery planning now.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Runtime View](../../architecture/runtime-view.md)
- [Data and Storage View](../../architecture/data-storage-view.md)
- [Security View](../../architecture/security-view.md)
- [Async Messaging View](../../architecture/async-messaging-view.md)
- [AI Development Guide](../../architecture/ai-development-guide.md)