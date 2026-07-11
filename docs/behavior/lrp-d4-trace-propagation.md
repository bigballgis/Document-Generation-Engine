# LR-D4 — Trace propagation decision + minimal impl (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `lrp-d4-trace-propagation` |
| **Plan** | [LRP-D §LR-D4](../plan/detail/LRP-D-ops-observability.md#lr-d4--trace-propagation-decision--minimal-impl) |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-12 |
| **Formal phase** | None (Wave LR-D **Done**) |
| **Task Master** | **#41** (`done`) — plan id **LR-D4** |
| **ADR** | **[ADR-0049 Accepted](../adr/operations/0049-distributed-trace-propagation.md)** (2026-07-12) |

---

## Why BDD is not-applicable

LR-D4 delivers **internal observability plumbing** (traceId correlation across async/Kafka boundaries), not a product behavior change:

- No new user-facing journey, management UI surface, permission rule, or audit semantics.
- No change to the public API **envelope `traceId` contract**, `X-Trace-Id` header contract, or generation/authoring response shapes — existing correlation identifiers must keep working.
- Outcomes are a **propagation decision (ADR)** plus **proven MDC/header flow** with unit/integration tests — not product acceptance journeys.
- Preferred deliverables are **ADR + executor decorator / Kafka header wiring + tests** — inventing Zipkin/Tempo backends, new product APIs, or UI “trace viewers” is out of scope.

Plan authority: [LRP-D-ops-observability.md](../plan/detail/LRP-D-ops-observability.md) §LR-D4 — **BDD: not-applicable — internal observability plumbing.**

Program row: [launch-readiness-program.md](../plan/launch-readiness-program.md) § wave map — LR-D4 BDD column **`not-applicable`**.

---

## What is in scope (ops / observability only)

| Deliverable | Intent |
| --- | --- |
| **Propagation ADR** | **[ADR-0049 Accepted](../adr/operations/0049-distributed-trace-propagation.md)** — v1 = **traceId propagation only** (MDC + async decorator + Kafka headers; no new span-export backend) |
| **Minimal impl** | `traceId` flows: inbound request → MDC → `asyncTaskExecutor` (task decorator) → Kafka headers → consumer MDC |
| **Tests** | Async worker logs carry originating traceId (MDC); Kafka round-trip preserves producer → consumer MDC `traceId` |

**Upstream baseline (do not re-invent):**

- **[ADR-0047 Accepted](../adr/operations/0047-distributed-tracing-otlp-baseline.md)** (SOR-A06 Done) — Micrometer Tracing + OTLP bridge already on classpath; `TraceIdProvider` precedence (active span → `X-Trace-Id` → UUID); local/test tracing disabled; prod OTLP via env. LR-D4 does **not** re-decide whether to adopt the Micrometer bridge.
- **[ADR-0049 Accepted](../adr/operations/0049-distributed-trace-propagation.md)** — async / Kafka / scheduled / MinIO **propagation** strategy. Indexed in [`docs/adr/README.md`](../adr/README.md). Relation: **0047 = baseline**; **0049 = cross-boundary propagation**.

**Environment / policy constraints (from plan + ADR-0049):**

- Do **not** adopt a full tracing backend (Zipkin/Tempo) in this slice — decision + minimal propagation only.
- Do **not** break the existing envelope `traceId` / `X-Trace-Id` contract.
- Prefer **no new Maven dependencies** for D4 (reuse ADR-0047 classpath).
- Leave **LR-E / CD-3** inactive; do **not** touch `DGE-audit-governance`.
- Formal phase remains **None**.

---

## Acceptance scenarios (plan §LR-D4 G/W/T)

These are **ops / instrumentation acceptance** criteria for MDC + Kafka header propagation tests — **not** product BDD Given/When/Then for TDD Red of new user-facing behavior. No product actor journeys are invented here.

### Scenario A — Async worker retains request traceId

- **Given** a sync request with trace id **T**
- **When** it spawns an async batch task
- **Then** worker logs for that task carry **T** (MDC assertion)

### Scenario B — Kafka round-trip preserves traceId

- **Given** Kafka transport is active
- **When** a message round-trips
- **Then** the consumer-side MDC `traceId` equals the producer's

---

## Explicit non-goals

- No product requirement inventing a management-UI “trace explorer” or new runtime API fields.
- No full Zipkin/Tempo (or other collector) stack in this slice — OTLP export remains ADR-0047 profile-gated.
- No changing envelope `metadata.traceId` / response `X-Trace-Id` semantics.
- No activating **LR-E** / **CD-3** from this readiness record.
- No marking LR-D4 **Done** in this readiness record alone — Done requires ADR accepted + propagation proven + `mvn verify` + doc sync + commit review (plan §LR-D4).

---

## Docs-first gate (stage 3) — complete

| Item | Status |
| --- | --- |
| Primary artifact | **[ADR-0049 Accepted](../adr/operations/0049-distributed-trace-propagation.md)** (2026-07-12); indexed in `docs/adr/README.md` |
| Relation to ADR-0047 | 0047 = OTLP / Micrometer **baseline**; 0049 = **cross-boundary propagation**. 0047 Accepted decision text **not** rewritten |
| Plan wording drift | Stale «adopt Micrometer Tracing bridge now vs defer» reframed in LRP-D §LR-D4 + ADR-0049 to **propagation scope v1** |
| Dependency policy | Default: **no new deps**; reuse 0047 classpath; any extra artifact must pass tech-stack guardrails |
| Next | **backend-engineer** gap-close (`TaskDecorator` + Kafka `X-Trace-Id` header set/restore + Scenarios A/B tests) |

---

## Traceability

| Artifact | Role |
| --- | --- |
| [LRP-D §LR-D4](../plan/detail/LRP-D-ops-observability.md) | Authoritative task row + G/W/T |
| [ADR-0047](../adr/operations/0047-distributed-tracing-otlp-baseline.md) | Accepted OTLP / Micrometer / TraceIdProvider baseline (SOR-A06) |
| [ADR-0049](../adr/operations/0049-distributed-trace-propagation.md) | **Accepted** propagation strategy (2026-07-12) |
| [SOR-A06](../plan/system-optimization-review-2026-07.md) | Prior Done — bridge already present |
| `TraceIdProvider` / `TraceIdMdcFilter` / `AsyncConfig` / Kafka batch dispatcher+consumer | Existing seams implementer extends |
| Program §1 finding 12 | Ops gap: no trace propagation into async/Kafka paths |

```
bdd_readiness: not-applicable
task_ids: [LR-D4 / lrp-d4-trace-propagation]
adr: ADR-0049 Accepted
```
