---
id: ADR-0037
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
  - implementation
adrNumber: "0037"
topic: technology-stack
amends:
  - docs/adr/technology-stack/0028-backend-platform-stack-baseline.md
related:
  - docs/adr/technology-stack/0028-backend-platform-stack-baseline.md
  - docs/adr/technology-stack/0034-data-and-storage-operations-baseline.md
  - docs/adr/technology-stack/0035-implementation-realization-and-quality-gate-baseline.md
  - docs/architecture/technology-stack-decisions.md
  - docs/plan/optimization-plan.md
---

# ADR 0037: Backend Dependency Realization Sequencing

## Status

Accepted

## Context

ADR 0028 (Backend Platform Stack Baseline) and ADR 0034 mandate a set of backend
libraries. A read-only audit on 2026-06-23 (recorded in
[optimization-plan.md](../../plan/optimization-plan.md), finding F3) found that the
delivered implementation does **not** yet use several mandated components:

| Mandated by | Component | Implemented on 2026-06-23? |
| --- | --- | --- |
| ADR 0028 | MapStruct (object mapping) | No — DTO↔entity mapping is hand-written |
| ADR 0028 | QueryDSL (query composition) | No — Spring Data JPA derived queries + JPQL only |
| ADR 0028 | Resilience4j (retry/timeout/circuit breaker) | No |
| ADR 0028 / 0034 | Bucket4j (rate limiting) | No — only business batch-size limits exist |
| ADR 0028 | Redisson (distributed lock) | No — Lettuce `StringRedisTemplate` for idempotency cache only |

This ADR does not discard the ADR 0028 baseline. It records a **production-driven
realization decision** for these specific rows, so documentation and reality stop
contradicting each other and the remaining work is scheduled rather than silently
divergent. The decision was confirmed by the maintainer on production grounds
(see optimization-plan.md §4 decision D-2).

## Decision

The five mandated-but-unrealized components are split by production criticality.

### 1. Production-critical — reaffirmed mandatory, scheduled to implement

These directly affect availability, abuse protection, and concurrency correctness
of the bank-facing runtime API. ADR 0028 / 0034 remain authoritative; their absence
is a **transitional gap** to be closed in optimization Wave 3 (OPT-F).

| Component | Required behavior | Tracking |
| --- | --- | --- |
| Bucket4j | Token-bucket rate limiting on the runtime API (per ADR 0031/0034: 429 + Retry-After) | OPT-F1 |
| Resilience4j | Timeout + retry + circuit breaker around LibreOffice, MinIO, Kafka | OPT-F2 |
| Redisson | Distributed lock for idempotency `begin` and async-task ownership across instances | OPT-F8 |

### 2. Developer-ergonomics — amended to recommended / incremental

These improve maintainability but are not runtime-critical. The ADR 0028 rows are
**amended** from "mandatory baseline" to "recommended; adopt incrementally during
refactors." Hand-written mappers and Spring Data JPA / JPQL queries are an accepted,
durable implementation choice in the interim and are **not** a gate violation.

| Component | Amended status | Adopt when |
| --- | --- | --- |
| MapStruct | Recommended, incremental | When a mapping-heavy service is refactored (OPT-D3) |
| QueryDSL | Recommended, incremental | When complex/pageable queries are reworked (OPT-D4) |

## Consequences

- ADR 0028 stays Accepted for all other rows; only its MapStruct and QueryDSL rows
  are superseded by this ADR's amended status.
- The technology-stack decision ledger is updated to mark MapStruct/QueryDSL as
  amended (ADR 0037) and to flag Resilience4j/Bucket4j/Redisson as mandated but
  pending implementation (transitional gap, tracked in OPT-F).
- No quality gate fails merely because MapStruct/QueryDSL are absent.
- Closing OPT-F1/F2/F8 satisfies the production-critical reaffirmation; until then the
  gap is documented, not hidden.

## Alternatives Considered

- **Add all five immediately**: rejected — MapStruct/QueryDSL churn across 200+
  classes delivers little runtime value and delays the production-critical resilience
  work.
- **Amend ADR 0028 to drop all five**: rejected — rate limiting, resilience, and
  distributed locking are genuine production requirements for a public API and must
  remain mandatory.
- **Leave the contradiction in place**: rejected — violates the document-as-code
  constitution (docs must match reality; conflicts must be surfaced, not silent).

## Related Documents

- [ADR 0028 Backend Platform Stack Baseline](./0028-backend-platform-stack-baseline.md)
- [ADR 0034 Data and Storage Operations Baseline](./0034-data-and-storage-operations-baseline.md)
- [ADR 0035 Implementation Realization and Quality Gate Baseline](./0035-implementation-realization-and-quality-gate-baseline.md)
- [Technology Stack Decision Log](../../architecture/technology-stack-decisions.md)
- [Optimization Plan & Backlog](../../plan/optimization-plan.md)
