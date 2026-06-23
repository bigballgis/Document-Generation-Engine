---
id: ADR-0034
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0034"
topic: technology-stack
related:
  - docs/architecture/README.md
  - docs/architecture/data-storage-view.md
  - docs/architecture/runtime-view.md
  - docs/architecture/security-view.md
  - docs/adr/technology-stack/0028-backend-platform-stack-baseline.md
  - docs/adr/authorization-security/0001-output-encryption.md
---

# ADR 0034: Data and Storage Operations Baseline

## Status

Accepted

## Context

The platform already has accepted backend, operational, and security ADRs. The remaining ledger items need a stable baseline for storage, persistence, transaction handling, time handling, caching, upload/download behavior, and related support infrastructure.

## Decision

The confirmed data and storage operations baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Object storage SDK | MinIO Java SDK | Java integration baseline for object storage. |
| File anti-virus scanning | File type and size checks only, no virus scan | Upload validation baseline. |
| Database connection pool | HikariCP | JDBC pool baseline. |
| Rate limiting | Bucket4j | Rate limiting baseline. |
| Configuration management | Spring Profiles + environment variables | Application configuration baseline. |
| Database audit field auto-fill | Spring Data Auditing | Automatic audit field population baseline. |
| Database soft delete strategy | Logical delete field + global query filter | Soft-delete baseline. |
| Database primary key strategy | UUID | Primary key baseline. |
| Time field storage strategy | UTC for all time fields | Time storage baseline. |
| API idempotency key storage | Redis + DB dual-write | Idempotency persistence baseline. |
| Database transaction management | Spring declarative transaction (@Transactional) | Transaction management baseline. |
| Default database isolation level | Read Committed | Transaction isolation baseline. |
| Distributed transaction strategy | Event-driven eventual consistency / Outbox | Cross-boundary transaction baseline. |
| Exception code layering | Business codes + platform codes layering | Error layering baseline. |
| Exception response i18n strategy | Fixed error code + locale-based message mapping | Message localization baseline. |
| Auth token refresh strategy | Short-lived Access Token + rotating Refresh Token | Session token lifecycle baseline. |
| Password/key hashing strategy | Argon2id | Hashing baseline. |
| Key management strategy | Unified KMS managed keys (cloud or self-hosted) | KMS baseline. |
| Large file upload strategy | Chunked upload + resumable transfer | Upload baseline. |
| Download link security strategy | Presigned URL + short expiration | Download link baseline. |
| File content encryption algorithm strategy | AES-256-GCM | File encryption baseline. |
| Object storage selection strategy | MinIO (self-hosted) | Object storage baseline. |
| Object storage encryption strategy | Server-side encryption (SSE) + KMS managed keys | Object storage encryption baseline. |
| Object storage bucket partition strategy | Separate buckets per environment (dev/test/prod) + business prefixes | Bucket layout baseline. |
| Object storage access control strategy | Least-privilege IAM + short-lived STS credentials | Object storage access baseline. |
| Object storage pre-signed URL validity strategy | 5 minutes | Signed URL lifetime baseline. |
| Cache TTL baseline strategy | Default 5 minutes | Cache lifetime baseline. |
| Cache penetration protection strategy | Null-value cache + Bloom filter | Cache miss protection baseline. |
| Cache breakdown protection strategy | Mutex lock + hot keys never expire | Cache stampede protection baseline. |
| Cache avalanche protection strategy | TTL random jitter + multi-level cache | Cache avalanche protection baseline. |
| Cache strategy for hot read data | Cache-Aside (application-managed read-through fallback) | Read-cache baseline. |
| Cache invalidation strategy | TTL-first + active invalidation on key business events | Invalidation baseline. |
| API rate limiting strategy | Token Bucket (dual dimensions: tenant + user) | Rate limiting baseline. |
| Rate limit counter storage strategy | Redis (centralized counters) | Counter storage baseline. |
| Rate limit exceed response strategy | Return 429 with standard Retry-After header | Rate-limit response baseline. |

These decisions are accepted as the data and storage operations foundation.

## Consequences

- Persistence, upload/download, caching, and transaction behavior are now stable and explicit.
- Implementation teams can build around one storage/operations baseline instead of scattered session notes.
- Future changes should update this ADR and the runtime/data/security views together.

## Alternatives Considered

- Keep the choices only in the technology decision log: rejected because durable decisions need an ADR.
- Split each storage and persistence rule into a separate ADR: rejected because these choices are tightly coupled in the runtime and storage views.
- Leave transaction, cache, and upload behavior open until implementation starts: rejected because those choices shape implementation architecture now.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Data and Storage View](../../architecture/data-storage-view.md)
- [Runtime View](../../architecture/runtime-view.md)
- [Security View](../../architecture/security-view.md)