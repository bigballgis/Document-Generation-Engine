# ADR-0044: Multi-Instance Correctness Baseline

**Status:** Accepted  
**Date:** 2026-07-03  
**Context:** SOR-S07 — horizontal scale requires distributed rate limiting, idempotency locks, and SSE progress routing.

## Decision

1. **Rate limiting (accepted scale-out contract):** Redis-backed Bucket4j proxy (`RedisRuntimeRateLimitService` or equivalent) when `docgen.runtime.rate-limit.distributed=true` / `RUNTIME_RATE_LIMIT_DISTRIBUTED=true`. Canonical property path is under `docgen.runtime.rate-limit.*` (older wording `docgen.rate-limit.distributed` is a doc alias only). **v1 default authority remains process-local** Bucket4j (`distributed` defaults **`false`**). Leaf **PQH-F7** / TM **#163** owns delivering and verifying the shared limiter when the flag is enabled — see Honesty residual below. Behavior SoT: [pqh-f7-redis-rate-limit.md](../../behavior/pqh-f7-redis-rate-limit.md).
2. **Idempotency begin:** Continue DB unique-constraint authority; Redisson lock deferred until multi-replica rollout (ADR-0039). **Orthogonal** to PQH-F7 shared rate-limit (prerequisite #3) — this leaf does **not** adopt Redisson locks.
3. **SSE progress:** Sticky sessions required for multi-pod SSE until Redis pub/sub registry lands (documented operational constraint).

## Honesty residual (PRR-D01b — not multi-instance complete)

This Accepted decision records the **target baseline for scale-out**, not a claim that multi-instance correctness is delivered overall.

| Topic | v1 truth (authoritative) |
| --- | --- |
| Serving topology | **Single serving backend replica** (see [0044-deployment-topology-v1.md](./0044-deployment-topology-v1.md)) |
| Runtime rate-limit | **Default** remains process-local Bucket4j (`RUNTIME_RATE_LIMIT_DISTRIBUTED` / `docgen.runtime.rate-limit.distributed` = **`false`**). **Accepted contract (PQH-F7 / #163):** when operators opt in with healthy Redis, shared Redis-coordinated quota is the **deliverable of that leaf** (fail-closed **503** `RATE_LIMIT_BACKEND_UNAVAILABLE` if Redis coordination fails while `distributed=true`; **429** `RATE_LIMIT_EXCEEDED` unchanged for true quota exhaustion). **Do not** read default-off as “dead aspirational config” for the rate-limit row once the leaf is verified; **do not** claim implementation Done from this ADR alone (code + gates land in the same leaf’s later stages). |
| SSE | **Sticky sessions required** across pods; without Redis pub/sub relay, multi-pod SSE is incomplete. |
| Completeness | **Not** multi-instance correctness complete (SSE / Redisson locks / Kafka ownership remain open). Do not treat this ADR or enabling the distributed flag as evidence that horizontal scale is safe overall. |

Cross-check: topology ADR scale-out prerequisite row **#3**; ADR-0031 Redis centralized counters; ADR-0039 locks still deferred; `ProductionMultiInstanceGuard` messaging for rate-limit only after PQH-F7 verify.

## Consequences

- Prod profile **does not** default-enable distributed rate limit (`distributed: false` unless operators set the env) — single-replica v1 honesty preserved.
- `RuntimeRateLimitFilter` keeps the 429 envelope; Redis-backed service is selected via `@ConditionalOnProperty` (or equivalent) when operators deliberately enable distributed mode after Redis is healthy.
- Runbook §Multi-instance notes must stay aligned with the honesty residual above (enable flag, fail-closed Redis-down, default single-replica).

## Alternatives rejected

- Full Redisson lock + Redis SSE registry in one slice — scope split; SOR-S07 / PQH-F7 delivers the **shared rate-limit** path first (not locks).
