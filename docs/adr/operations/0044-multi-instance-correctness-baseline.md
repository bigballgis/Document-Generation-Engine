# ADR-0044: Multi-Instance Correctness Baseline

**Status:** Accepted  
**Date:** 2026-07-03  
**Context:** SOR-S07 — horizontal scale requires distributed rate limiting, idempotency locks, and SSE progress routing.

## Decision

1. **Rate limiting (target / scale-out):** Redis-backed Bucket4j proxy (`RedisRuntimeRateLimitService`) when `docgen.rate-limit.distributed=true`. **v1 runtime authority is process-local** Bucket4j; see Honesty residual below.
2. **Idempotency begin:** Continue DB unique-constraint authority; Redisson lock deferred until multi-replica rollout (ADR-0039).
3. **SSE progress:** Sticky sessions required for multi-pod SSE until Redis pub/sub registry lands (documented operational constraint).

## Honesty residual (PRR-D01b — not multi-instance complete)

This Accepted decision records the **target baseline for scale-out**, not a claim that multi-instance correctness is delivered today.

| Topic | v1 truth (authoritative) |
| --- | --- |
| Serving topology | **Single serving backend replica** (see [0044-deployment-topology-v1.md](./0044-deployment-topology-v1.md)) |
| Runtime rate-limit | **Process-local** Bucket4j is authoritative. `application-prod.yml` defaults `RUNTIME_RATE_LIMIT_DISTRIBUTED` / `docgen.runtime.rate-limit.distributed` to **`false`**. Enabling the distributed flag is a **deferred / aspirational** switch — **not** “Redis distributed rate-limit delivered”. |
| SSE | **Sticky sessions required** across pods; without Redis pub/sub relay, multi-pod SSE is incomplete. |
| Completeness | **Not** multi-instance correctness complete. Do not treat this ADR or the distributed config key as evidence that horizontal scale is safe by default. |

Cross-check: topology ADR deferral of ADR-0031 Redis centralized counters for v1; `ProductionMultiInstanceGuard` startup warning.

## Consequences

- Prod profile **does not** default-enable distributed rate limit (`distributed: false` unless operators set the env).
- `RuntimeRateLimitFilter` unchanged API; Redis service swaps via `@ConditionalOnProperty` when operators deliberately enable distributed mode after prerequisites.
- Runbook §Multi-instance notes must stay aligned with the honesty residual above.

## Alternatives rejected

- Full Redisson lock + Redis SSE registry in one slice — scope split; SOR-S07 delivers rate-limit path first.
