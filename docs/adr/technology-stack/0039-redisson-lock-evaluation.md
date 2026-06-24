---
id: ADR-0039
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0039"
topic: technology-stack
related:
  - docs/adr/technology-stack/0028-backend-platform-stack-baseline.md
  - docs/adr/technology-stack/0037-backend-dependency-realization-sequencing.md
  - docs/adr/technology-stack/0034-data-and-storage-operations-baseline.md
---

# ADR 0039: Redisson Distributed Lock Evaluation

## Status

Accepted

## Context

ADR 0028 and ADR 0037 mandate Redis (Redisson) for distributed coordination, idempotency
`begin`, and async-task ownership when multiple backend instances run concurrently. The
current implementation uses Lettuce `StringRedisTemplate` for idempotency key storage only,
without distributed locks. Production deployment today is a single backend instance.

## Decision

1. **Single-instance (current):** Accept the transitional risk. Lettuce-only idempotency
   storage is sufficient while exactly one backend instance serves management and runtime
   traffic.
2. **Multi-instance (future):** Redisson distributed locks become **mandatory** before
   horizontal scaling. Lock coverage must include idempotency `begin` and async-task
   ownership paths identified in ADR 0037 (OPT-F8 / COR-P05).
3. **No premature adoption:** Do not add Redisson dependency until multi-instance rollout
   is planned; record the gate in deployment checklists.

## Consequences

- Positive: Avoids dependency and operational cost before it is needed; unblocks
  single-instance delivery.
- Negative: Scaling out without implementing Redisson first risks duplicate idempotency
  processing or async-task double-ownership.
- Mitigation: Block multi-instance go-live until Redisson lock slice is implemented and
  verified.

## Alternatives Considered

| Alternative | Why not chosen now |
| --- | --- |
| Implement Redisson immediately | No multi-instance requirement yet; adds complexity without current benefit |
| Database advisory locks only | Diverges from accepted Redis/Redisson baseline in ADR 0028 |
| Drop lock requirement | Unsafe for multi-instance correctness |

## Related Documents

- [ADR 0028: Backend Platform Stack Baseline](./0028-backend-platform-stack-baseline.md)
- [ADR 0037: Backend Dependency Realization Sequencing](./0037-backend-dependency-realization-sequencing.md)
