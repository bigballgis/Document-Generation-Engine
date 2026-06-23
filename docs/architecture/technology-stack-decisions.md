---
id: DOC-ARCH-TECH-STACK-DECISIONS
type: Architecture View
status: Proposed
sourceOfTruth: false
owners:
  - architecture
  - implementation
dependsOn:
  - docs/adr/technology-stack/0022-basic-technology-stack-baseline.md
  - docs/adr/api/0006-api-error-model.md
  - docs/adr/api/0011-api-schema-and-response-envelope.md
related:
  - docs/architecture/README.md
  - docs/architecture/implementation-task-plan.md
  - docs/architecture/m1-task-sheet.md
---

# Technology Stack Decision Log

## Purpose

This document is the persistent ledger for technology selection progress, so decisions are not lost in chat sessions.

Source-of-truth boundary:

- Durable accepted decisions are owned by ADRs.
- This log tracks confirmation progress, pending items, and ADR synchronization status.

## Status Definitions

- `ADR Accepted`: decision is accepted and recorded in ADR.
- `Session Confirmed, ADR Pending`: user confirmed in chat and must be synchronized into ADRs/docs.
- `Pending Confirmation`: no accepted decision yet.

## Current Decision Ledger

| Area | Current Selection | Status | Evidence | Last Updated |
| --- | --- | --- | --- | --- |
| Management frontend core | Vue 3 + TypeScript + Vite | ADR Accepted | ADR 0022 | 2026-06-08 |
| Async messaging backbone | Kafka | ADR Accepted | ADR 0022 | 2026-06-08 |
| Rendering boundary | Separate rendering worker/service | ADR Accepted | ADR 0022, ADR 0019 | 2026-06-08 |
| Runtime split strategy | Day-1 multi-service split | ADR Accepted | ADR 0035 | 2026-06-09 |
| Task state and output metadata persistence strategy | PostgreSQL for durable state + Redis for hot-status acceleration | ADR Accepted | ADR 0035 | 2026-06-09 |
| Module/package naming convention | Business-module-first packaging (module-first, layered inside module) | ADR Accepted | ADR 0035 | 2026-06-09 |
| Java static scan baseline | Checkstyle + SpotBugs + PMD | ADR Accepted | ADR 0035 | 2026-06-09 |
| Dependency security scan baseline | OWASP Dependency-Check, block high/critical | ADR Accepted | ADR 0035 | 2026-06-09 |
| Coverage gate baseline | JaCoCo >= 85% changed lines, >= 90% core/security-critical modules | ADR Accepted | ADR 0035 | 2026-06-09 |
| Frontend quality command baseline | Fix now: pnpm lint/type-check/test/build | ADR Accepted | ADR 0035 | 2026-06-09 |
| Error handling + response envelope | Unified error codes + unified response envelope | ADR Accepted | ADR 0006, ADR 0011 | 2026-06-08 |
| Backend runtime | Java 21 + Spring Boot 3.x | ADR Accepted | ADR 0028 | 2026-06-08 |
| Database | PostgreSQL | ADR Accepted | ADR 0028 | 2026-06-08 |
| Cache | Redis | ADR Accepted | ADR 0028 | 2026-06-08 |
| Object storage | MinIO | ADR Accepted | ADR 0028 | 2026-06-08 |
| Frontend UI stack | Element Plus + Pinia + Vue Router 4 + Axios + pnpm | ADR Accepted | ADR 0029 | 2026-06-08 |
| Frontend styling | SCSS + CSS Modules | ADR Accepted | ADR 0029 | 2026-06-08 |
| Frontend testing | Vitest + Vue Test Utils + Playwright | ADR Accepted | ADR 0029 | 2026-06-08 |
| Backend testing | JUnit 5 + Mockito + Testcontainers + RestAssured | ADR Accepted | ADR 0028 | 2026-06-08 |
| Backend build tool | Maven | ADR Accepted | ADR 0028 | 2026-06-08 |
| Database migration tool | Flyway | ADR Accepted | ADR 0028 | 2026-06-08 |
| Backend ORM / data access | Spring Data JPA + QueryDSL | ADR Accepted | ADR 0028 | 2026-06-08 |
| Backend auth framework | Spring Security + JWT | ADR Accepted | ADR 0028 | 2026-06-08 |
| Backend observability | Micrometer + Prometheus + Grafana + OpenTelemetry | ADR Accepted | ADR 0028 | 2026-06-08 |
| Logging stack | Logback + JSON structured logs | ADR Accepted | ADR 0028 | 2026-06-08 |
| API docs and debug tooling | springdoc-openapi + Swagger UI | ADR Accepted | ADR 0028 | 2026-06-08 |
| JSON serialization | Jackson | ADR Accepted | ADR 0028 | 2026-06-08 |
| Object mapping | MapStruct | ADR Accepted | ADR 0028 | 2026-06-08 |
| Input validation | Jakarta Bean Validation (Hibernate Validator) | ADR Accepted | ADR 0028 | 2026-06-08 |
| Distributed lock | Redis (Redisson) | ADR Accepted | ADR 0028 | 2026-06-08 |
| Task scheduling | Quartz | ADR Accepted | ADR 0028 | 2026-06-08 |
| Cache serialization | Jackson JSON | ADR Accepted | ADR 0028 | 2026-06-08 |
| HTTP client | Spring WebClient | ADR Accepted | ADR 0028 | 2026-06-08 |
| Resilience and retry | Resilience4j | ADR Accepted | ADR 0028 | 2026-06-08 |
| Message consumption concurrency model | Kafka consumer group + partition concurrency | ADR Accepted | ADR 0033 | 2026-06-08 |
| Message deserialization format | JSON | ADR Accepted | ADR 0033 | 2026-06-08 |
| Kafka topic naming convention | business-domain.event-type.v1 | ADR Accepted | ADR 0033 | 2026-06-08 |
| Kafka delivery semantics | At-least-once | ADR Accepted | ADR 0033 | 2026-06-08 |
| Kafka consumer failure handling | Retry + DLT (Dead Letter Topic) | ADR Accepted | ADR 0033 | 2026-06-08 |
| Object storage SDK | MinIO Java SDK | ADR Accepted | ADR 0034 | 2026-06-08 |
| File anti-virus scanning | File type and size checks only, no virus scan | ADR Accepted | ADR 0034 | 2026-06-08 |
| Database connection pool | HikariCP | ADR Accepted | ADR 0034 | 2026-06-08 |
| Rate limiting | Bucket4j | ADR Accepted | ADR 0034 | 2026-06-08 |
| Configuration management | Spring Profiles + environment variables | ADR Accepted | ADR 0034 | 2026-06-08 |
| Database audit field auto-fill | Spring Data Auditing | ADR Accepted | ADR 0034 | 2026-06-08 |
| Database soft delete strategy | Logical delete field + global query filter | ADR Accepted | ADR 0034 | 2026-06-08 |
| Database primary key strategy | UUID | ADR Accepted | ADR 0034 | 2026-06-08 |
| Time field storage strategy | UTC for all time fields | ADR Accepted | ADR 0034 | 2026-06-08 |
| API idempotency key storage | Redis + DB dual-write | ADR Accepted | ADR 0034 | 2026-06-08 |
| Database transaction management | Spring declarative transaction (@Transactional) | ADR Accepted | ADR 0034 | 2026-06-08 |
| Default database isolation level | Read Committed | ADR Accepted | ADR 0034 | 2026-06-08 |
| Distributed transaction strategy | Event-driven eventual consistency / Outbox | ADR Accepted | ADR 0034 | 2026-06-08 |
| Exception code layering | Business codes + platform codes layering | ADR Accepted | ADR 0034 | 2026-06-08 |
| Exception response i18n strategy | Fixed error code + locale-based message mapping | ADR Accepted | ADR 0034 | 2026-06-08 |
| Auth token refresh strategy | Short-lived Access Token + rotating Refresh Token | ADR Accepted | ADR 0032 | 2026-06-08 |
| Password/key hashing strategy | Argon2id | ADR Accepted | ADR 0032 | 2026-06-08 |
| Key management strategy | Unified KMS managed keys (cloud or self-hosted) | ADR Accepted | ADR 0030 | 2026-06-08 |
| Audit log storage strategy | Database as primary storage + object storage archive | ADR Accepted | ADR 0030 | 2026-06-08 |
| Log retention strategy | Online 90 days + archive 2 years | ADR Accepted | ADR 0030 | 2026-06-08 |
| Audit log immutability strategy | WORM / object lock | ADR Accepted | ADR 0030 | 2026-06-08 |
| Trace ID standard | W3C Trace Context (traceparent) | ADR Accepted | ADR 0014 | 2026-06-08 |
| API versioning strategy | URI explicit versioning (/v1) | ADR Accepted | ADR 0031 | 2026-06-08 |
| API timeout baseline strategy | Read requests 3s / write requests 5s | ADR Accepted | ADR 0031 | 2026-06-08 |
| Cache TTL baseline strategy | Default 5 minutes | ADR Accepted | ADR 0034 | 2026-06-08 |
| Cache penetration protection strategy | Null-value cache + Bloom filter | ADR Accepted | ADR 0034 | 2026-06-08 |
| Cache breakdown protection strategy | Mutex lock + hot keys never expire | ADR Accepted | ADR 0034 | 2026-06-08 |
| Cache avalanche protection strategy | TTL random jitter + multi-level cache | ADR Accepted | ADR 0034 | 2026-06-08 |
| Large file upload strategy | Chunked upload + resumable transfer | ADR Accepted | ADR 0034 | 2026-06-08 |
| Download link security strategy | Presigned URL + short expiration | ADR Accepted | ADR 0034 | 2026-06-08 |
| File content encryption algorithm strategy | AES-256-GCM | ADR Accepted | ADR 0001 | 2026-06-08 |
| Template rendering engine strategy | DOCX template engine + separate rendering service | ADR Accepted | ADR 0019 | 2026-06-08 |
| PDF conversion engine strategy | LibreOffice headless mode | ADR Accepted | ADR 0019 | 2026-06-08 |
| Document preview generation strategy | Asynchronous pre-generated preview files | ADR Accepted | ADR 0019 | 2026-06-08 |
| Frontend package manager lock strategy | Enforce pnpm-lock.yaml | ADR Accepted | ADR 0029 | 2026-06-08 |
| Task retry and backoff strategy | Exponential backoff + max retries + dead-letter queue | ADR Accepted | ADR 0033 | 2026-06-08 |
| Task idempotency deduplication storage strategy | Redis with TTL idempotency keys | ADR Accepted | ADR 0033 | 2026-06-08 |
| Object storage selection strategy | MinIO (self-hosted) | ADR Accepted | ADR 0034 | 2026-06-08 |
| Object storage encryption strategy | Server-side encryption (SSE) + KMS managed keys | ADR Accepted | ADR 0034 | 2026-06-08 |
| Object storage bucket partition strategy | Separate buckets per environment (dev/test/prod) + business prefixes | ADR Accepted | ADR 0034 | 2026-06-08 |
| Object storage access control strategy | Least-privilege IAM + short-lived STS credentials | ADR Accepted | ADR 0034 | 2026-06-08 |
| Object storage pre-signed URL validity strategy | 5 minutes | ADR Accepted | ADR 0034 | 2026-06-08 |
| Cache strategy for hot read data | Cache-Aside (application-managed read-through fallback) | ADR Accepted | ADR 0034 | 2026-06-08 |
| Cache invalidation strategy | TTL-first + active invalidation on key business events | ADR Accepted | ADR 0034 | 2026-06-08 |
| API rate limiting strategy | Token Bucket (dual dimensions: tenant + user) | ADR Accepted | ADR 0031 | 2026-06-08 |
| Rate limit counter storage strategy | Redis (centralized counters) | ADR Accepted | ADR 0031 | 2026-06-08 |
| Rate limit exceed response strategy | Return 429 with standard Retry-After header | ADR Accepted | ADR 0031 | 2026-06-08 |
| API idempotency key transport strategy | Request header: Idempotency-Key | ADR Accepted | ADR 0031 | 2026-06-08 |
| API idempotency conflict response strategy | Return first successful result (idempotent replay) | ADR Accepted | ADR 0004 | 2026-06-08 |
| Audit log storage strategy | PostgreSQL audit tables + object storage archival | ADR Accepted | ADR 0030 | 2026-06-08 |
| Audit log retention strategy | Database retention 180 days + object storage retention 3 years | ADR Accepted | ADR 0030 | 2026-06-08 |
| Observability log collection strategy | OpenTelemetry + centralized logging platform | ADR Accepted | ADR 0030 | 2026-06-08 |
| Observability metrics collection strategy | OpenTelemetry Metrics + Prometheus | ADR Accepted | ADR 0030 | 2026-06-08 |
| Observability tracing strategy | OpenTelemetry Tracing + Jaeger/Tempo | ADR Accepted | ADR 0030 | 2026-06-08 |
| Alert notification channel strategy | Email only | ADR Accepted | ADR 0030 | 2026-06-08 |
| Error tracking platform strategy | Sentry | ADR Accepted | ADR 0030 | 2026-06-08 |
| Health check endpoint strategy | Dual endpoints: /healthz (liveness) + /readyz (readiness) | ADR Accepted | ADR 0030 | 2026-06-08 |
| Configuration management strategy | Environment variables + Secret Manager | ADR Accepted | ADR 0030 | 2026-06-08 |
| Secret rotation strategy | Automatic rotation every 30 days + zero-downtime application refresh | ADR Accepted | ADR 0030 | 2026-06-08 |
| Container baseline image strategy | Distroless/minimal base image | ADR Accepted | ADR 0030 | 2026-06-08 |
| Container runtime permission strategy | Run as non-root user + read-only root filesystem | ADR Accepted | ADR 0030 | 2026-06-08 |
| Kubernetes resource requests and limits strategy | Enforce both requests and limits | ADR Accepted | ADR 0030 | 2026-06-08 |
| Kubernetes autoscaling strategy | HPA based on CPU/memory + custom metrics | ADR Accepted | ADR 0030 | 2026-06-08 |
| Kubernetes network policy strategy | Default deny + explicit allow rules as needed | ADR Accepted | ADR 0030 | 2026-06-08 |
| Ingress controller strategy | NGINX Ingress Controller | ADR Accepted | ADR 0030 | 2026-06-08 |
| TLS certificate management strategy | cert-manager automatic issuance and renewal | ADR Accepted | ADR 0030 | 2026-06-08 |
| Service-to-service authentication strategy | Internal network trust only (no mutual TLS) | ADR Accepted | ADR 0030 | 2026-06-08 |
| Service discovery strategy | Kubernetes DNS native service discovery | ADR Accepted | ADR 0030 | 2026-06-08 |
| CI pipeline trigger strategy | Dual triggers: push to main branch + pull requests | ADR Accepted | ADR 0030 | 2026-06-08 |
| CI parallel execution strategy | Stage-level parallelization (lint/type/test split) | ADR Accepted | ADR 0030 | 2026-06-08 |
| CI quality gate failure policy | Block merge on any failed quality gate | ADR Accepted | ADR 0030 | 2026-06-08 |
| CD release strategy | Blue-green deployment | ADR Accepted | ADR 0030 | 2026-06-08 |
| Release approval strategy | Manual approval required for production; automatic for non-production | ADR Accepted | ADR 0030 | 2026-06-08 |
| Release rollback strategy | Manual rollback trigger | ADR Accepted | ADR 0030 | 2026-06-08 |
| Database backup strategy | Weekly full backup + daily incremental backups | ADR Accepted | ADR 0030 | 2026-06-08 |
| Database recovery drill frequency strategy | Yearly recovery drill | ADR Accepted | ADR 0030 | 2026-06-08 |
| Cross-region disaster recovery strategy | Active-passive cross-region deployment | ADR Accepted | ADR 0030 | 2026-06-08 |
| RPO target strategy | RPO <= 15 minutes | ADR Accepted | ADR 0030 | 2026-06-08 |
| RTO target strategy | RTO <= 30 minutes | ADR Accepted | ADR 0030 | 2026-06-08 |
| Disaster recovery failover strategy | Switch after manual confirmation | ADR Accepted | ADR 0030 | 2026-06-08 |
| Data-in-transit encryption protocol strategy | End-to-end TLS 1.2+ | ADR Accepted | ADR 0030 | 2026-06-08 |
| Data-at-rest encryption strategy | AES-256 | ADR Accepted | ADR 0030 | 2026-06-08 |
| Key management strategy | KMS-managed master keys | ADR Accepted | ADR 0030 | 2026-06-08 |
| Application-layer sensitive field masking strategy | Unified masking middleware before response | ADR Accepted | ADR 0030 | 2026-06-08 |
| Sensitive operation secondary confirmation strategy | Secondary confirmation required for high-risk operations | ADR Accepted | ADR 0030 | 2026-06-08 |
| Audit log tamper-proof strategy | Append-only writes + hash-chain verification | ADR Accepted | ADR 0030 | 2026-06-08 |
| Audit log time synchronization strategy | Full-node NTP synchronization + UTC timestamps on write | ADR Accepted | ADR 0030 | 2026-06-08 |
| Permission change auditing strategy | Record before/after diffs and operator for all permission changes | ADR Accepted | ADR 0030 | 2026-06-08 |
| Login session management strategy | Short-lived Access Token + revocable Refresh Token | ADR Accepted | ADR 0030 | 2026-06-08 |
| Concurrent session control strategy | Maximum 3 active sessions per user | ADR Accepted | ADR 0030 | 2026-06-08 |
| Session expiration strategy | Auto-expire after 30 minutes of inactivity | ADR Accepted | ADR 0030 | 2026-06-08 |
| Authentication failure protection strategy | Lockout after consecutive failures (e.g., 5 failures -> 15-minute lockout) | ADR Accepted | ADR 0030 | 2026-06-08 |
| MFA policy | Mandatory MFA for administrators, optional MFA for regular users | ADR Accepted | ADR 0030 | 2026-06-08 |
| Password policy | Length >= 12 + complexity requirements + password history non-reuse | ADR Accepted | ADR 0030 | 2026-06-08 |
| Account lifecycle strategy | Auto-disable inactive accounts after 90 days | ADR Accepted | ADR 0030 | 2026-06-08 |
| Permission model strategy | Hybrid RBAC + ABAC | ADR Accepted | ADR 0030 | 2026-06-08 |
| Permission cache consistency strategy | Short TTL (e.g., 5 minutes) + active invalidation on permission change events | ADR Accepted | ADR 0030 | 2026-06-08 |

## Operating Rules

1. Every newly confirmed technology choice must be appended or updated in this file in the same turn.
2. When a row changes to `ADR Accepted`, add or update the ADR in the same change set when possible.
3. If a chat confirmation exists but exact option text is unclear, keep `Pending Confirmation` and do not infer.
4. Keep this file and ADR 0022 aligned; do not let this file override accepted ADR content.

## Synchronization Maintenance

1. Future technology confirmations should be recorded in this log and synchronized into ADRs in the same change set.
