# ADR-0044: Multi-Instance Correctness Baseline

**Status:** Accepted  
**Date:** 2026-07-03  
**Context:** SOR-S07 — horizontal scale requires distributed rate limiting, idempotency locks, and SSE progress routing.

## Decision

1. **Rate limiting:** Redis-backed Bucket4j proxy (`RedisRuntimeRateLimitService`) when `docgen.rate-limit.distributed=true` (default in `prod` profile); process-local buckets remain for dev/single-node.
2. **Idempotency begin:** Continue DB unique-constraint authority; Redisson lock deferred until multi-replica rollout (ADR-0039).
3. **SSE progress:** Sticky sessions required for multi-pod SSE until Redis pub/sub registry lands (documented operational constraint).

## Consequences

- Prod profile enables distributed rate limit flag in `application-prod.yml`.
- `RuntimeRateLimitFilter` unchanged API; service swaps via `@ConditionalOnProperty`.
- Runbook §Multi-instance notes added.

## Alternatives rejected

- Full Redisson lock + Redis SSE registry in one slice — scope split; SOR-S07 delivers rate-limit path first.
