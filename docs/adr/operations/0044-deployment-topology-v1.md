---
id: ADR-0044
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0044"
topic: operations
related:
  - docs/adr/technology-stack/0039-redisson-lock-evaluation.md
  - docs/adr/operations/0030-operational-platform-baseline.md
  - docs/adr/api/0031-api-platform-hardening-baseline.md
  - docs/adr/async-processing/0033-async-messaging-and-task-retry-baseline.md
  - docs/plan/launch-readiness-program.md
  - docs/plan/detail/LRP-B-runtime-scaleout-session.md
  - deploy/helm/docgen/values.yaml
  - docker-compose.prod.yml
---

# ADR 0044: Deployment Topology for v1 Launch (Single Backend Replica)

## Status

Accepted (2026-07-04)

Activation confirmed by user 2026-07-04 (LRP Wave LR-B). Refines the ADR-0039
single-instance assumption into an explicit v1 launch decision; it does not change the
accepted ADR-0039 baseline. CD-PIT-14 is **mitigated by this decision for the compose
topology**; K8s blue-green closure additionally requires **LR-B2** (see Decision).

## Context

P15 delivered Kubernetes HPA autoscaling for the backend (P15-T05): `values-prod.yaml`
shipped `autoscaling.backend.enabled: true` with `minReplicas: 3` (and
`backend.replicaCount: 3`); `values-staging.yaml` shipped `enabled: true` with
`minReplicas: 2` (and `replicaCount: 2`). At the same time, accepted
[ADR-0039](../technology-stack/0039-redisson-lock-evaluation.md) explicitly deferred
Redisson distributed locks on the assumption that **exactly one backend instance**
serves traffic, and made them mandatory before horizontal scaling. The Helm values
therefore contradicted the accepted architecture assumption — recorded as **CD-PIT-14**
(2026-07-03) and [Launch Readiness Program](../../plan/launch-readiness-program.md)
§1 finding 4.

The contradiction is not theoretical. Four backend components are process-local today
and are only correct with a single replica (2026-07-03 inventory evidence; program §1
findings 3/4):

1. **Scheduled jobs without distributed mutex** — `InvocationRetentionCleanupScheduler`,
   `CollaborationEscalationScheduler`, `PreviewTempCleanupScheduler` (`@Scheduled`, no
   lock): N replicas run every job N times (duplicate cleanup / duplicate escalations).
2. **In-process rate limiting** — `RuntimeRateLimitService` holds Bucket4j buckets in a
   `ConcurrentHashMap`: each replica enforces its own quota, so the effective limit
   multiplies by replica count.
3. **In-process SSE registry** — `SseEmitterRegistry` keeps progress-stream emitters in
   instance memory: with multiple replicas, progress events can be published on a
   replica that does not hold the subscriber's connection.
4. **In-process async transport (default)** — `docgen.async.transport` defaults to
   `in-process` (`ASYNC_TRANSPORT` env switch): async batch tasks execute inside the
   accepting instance; the Kafka path exists but is opt-in.

In addition, ADR-0039 already gates idempotency `begin` and async-task ownership on
Redisson locks before any multi-instance rollout.

v1 capacity pressure is low: the confirmed v1 boundary has **no customer-facing
generation portal** — upstream systems invoke the runtime API at low frequency, and the
management UI serves internal users only.

## Decision

**v1 launches with a single serving backend replica in every environment (LR-B1
branch (a)): one replica per Deployment, backend HPA off, traffic served by exactly one
instance.**

1. **Helm:** `backend.replicaCount: 1` in every values file; backend HPA disabled in
   all environments (`autoscaling.backend.enabled: false` — `values-prod.yaml` and
   `values-staging.yaml` corrected in this change set). Inert HPA tuning fields
   (`minReplicas`, `maxReplicas`, metric targets) remain in the values for the future
   scaled state; they render nothing while `enabled: false`.
2. **Docker compose (prod profile):** exactly one `docgen-backend` container; do not
   scale the service (`docker-compose.prod.yml` header records this constraint).
3. **Frontend out of scope:** the nginx SPA container is stateless, so frontend
   `replicaCount` and frontend HPA remain per-environment choices unconstrained by this
   decision (staging/prod keep their current frontend settings).
4. The four process-local components above are **accepted as correct for v1 on the
   traffic path**; LR-B5 graceful shutdown/drain covers restart behavior. Exception:
   schedulers under K8s blue-green dual-color residency — see the blue-green section
   (LR-B2 mandatory before that deployment shape).

### Scale-out prerequisites (all mandatory before backend replicas > 1 or HPA re-enable)

Row 1 has an earlier trigger: **K8s blue-green dual-color residency already counts as a
multi-instance scenario for schedulers** (see the blue-green section), so its
prerequisite is mandatory before the first K8s blue-green prod deployment — not only
before replicas > 1.

| # | Component | v1 single-serving-replica posture | Prerequisite before scale-out | Tracked in |
| --- | --- | --- | --- | --- |
| 1 | `@Scheduled` jobs (3) | Correct under compose (one container); **double-runs under K8s blue-green** (both colors resident) | Distributed mutex (ShedLock JDBC or DB lock; see Appendix) | **LR-B2** — recommended under compose; **MANDATORY before first K8s blue-green prod deployment** |
| 2 | SSE progress streams | Correct — subscriber and publisher share the traffic-serving process | Sticky routing at the ingress **or** Redis pub/sub relay | **LR-B3** records the constraint (no relay in v1) |
| 3 | Runtime rate limiting | Correct — single quota holder on the traffic path (ADR-0031 Redis counters deferred, see below) | Shared limiter (bucket4j-redis per ADR-0031) or gateway-level enforcement | ADR-0031 follow-up (LR-B7 stays in-process scope) |
| 4 | Async batch transport | Correct — the traffic-accepting instance owns the task | `ASYNC_TRANSPORT=kafka` in the scaled environment | **LR-B4** — Kafka stays an optional switch for v1 |
| 5 | Idempotency `begin` / async-task ownership | Correct — no concurrent owner | Redisson distributed locks | ADR-0039 (mandatory before multi-instance) |

### LR-B4 branch decision — in-process async transport accepted for v1 (branch (b))

- Operational constraints recorded: **single backend replica** (this ADR) + **graceful
  shutdown drain** (LR-B5) so in-flight async batch work completes or fails cleanly on
  restart.
- Kafka remains available behind `ASYNC_TRANSPORT=kafka`; it is **not** forced into the
  prod compose profile for v1.

### Blue-green (P15-T08 / ADR-0030): single **serving** replica, but two resident pods

Blue-green mode as implemented keeps **both color Deployments resident**: when
`blueGreen.enabled: true`, `templates/backend-color-deployments.yaml` renders `blue`
**and** `green` unconditionally at `replicas: 1` each, and every `helm upgrade` resets a
manually scaled-down color back to 1. The runbook (`deploy/blue-green-runbook.md`) has
no decommission step; its rollback procedure explicitly requires **leaving the failed
color running for forensics until explicitly scaled down**. The steady state of the K8s
prod profile is therefore **two backend pods**, not one.

This is compatible with the decision for every **traffic-bound** component, because the
main Service selector routes all traffic to `blueGreen.activeColor` only: SSE
subscribers connect to the active color, rate-limit quota is enforced by the single
traffic-receiving instance, async batch tasks are accepted and executed by the active
color, and idempotency `begin` has no concurrent owner. The inactive color receives no
production traffic (preview Services carry smoke checks only).

**Schedulers are the exception.** `@Scheduled` jobs fire regardless of traffic, so with
both colors resident the three cleanup/escalation jobs **run twice in steady state** —
dual-color residency is itself a multi-instance scenario for schedulers. Consequently
**LR-B2 (scheduler distributed mutex) is MANDATORY before the first K8s blue-green prod
deployment**; under the Docker Compose single-container topology it remains a
recommended low-cost insurance (restart overlap). Until LR-B2 lands, the K8s prod
profile must not be considered scheduler-safe.

### Relationship to the ADR-0030 autoscaling baseline

ADR-0030's Kubernetes autoscaling strategy (HPA on CPU/memory + custom metrics) remains
the target posture for the **scaled future state**; this ADR gates backend HPA
activation behind the prerequisites table. The HPA templates and tuning values stay in
the chart, disabled.

### Deferred for v1: ADR-0031 Redis centralized rate-limit counters

ADR-0031 accepted **«Rate limit counter storage strategy: Redis (centralized
counters)»**. The current implementation is process-local Bucket4j
(`RuntimeRateLimitService`, in-memory `ConcurrentHashMap`). This ADR records an
explicit **v1 deferral** of that storage decision, not a reversal:

- Under a single serving replica the process-local limiter is **semantically
  equivalent** to centralized counters — there is exactly one quota holder on the
  traffic path.
- Known limitation accepted for v1: counters **do not survive a restart** (a restart
  resets buckets, allowing brief over-admission up to one bucket refill).
- The deferral **must be closed before scale-out** (bucket4j-redis per ADR-0031, or
  gateway-level enforcement) — prerequisites table row 3.
- Tracked in the execution-sync-ledger transitional seam **«Runtime rate limit»**
  (shared Redis limiter / ADR-0031 alignment; LR-B7 covers the filter fail-closed
  alignment only, not the storage move).

## Consequences

Positive:

- CD-PIT-14 is **mitigated by decision for the compose topology** (Helm values, prod
  compose, and ADR-0039 now agree); **K8s blue-green closure requires LR-B2** because
  both colors stay resident and schedulers double-run until the mutex lands.
- No new dependencies or component rework required before compose-topology launch; the
  four in-process components stay correct as shipped on the traffic path.
- Lower operational surface at launch (no HPA tuning, no mandatory broker in the prod
  compose profile).

Negative / accepted risks:

- Capacity is vertical-only (one pod's resources + conversion pool). Accepted for v1
  given no customer generation portal and low-frequency upstream API invocation;
  revisit with LR-D6 load smoke evidence.
- Single-replica availability: planned releases cut over via blue-green; unplanned
  restarts drain via LR-B5 graceful shutdown; orchestrator restart policy covers
  crashes. Brief unavailability windows are accepted for v1.
- Scaling out without completing the prerequisites table reintroduces the CD-PIT-14
  defect class — treat the table as a hard gate in deployment checklists.

### Honesty residual (PRR-D01b / 2026-07-18) — not a new Decision

The Accepted Decision core above is unchanged: **v1 launches with a single serving
backend replica**. Readers must also treat the following as **open residuals**, not as
delivered multi-instance correctness:

| Residual | Current authority | Not complete until |
| --- | --- | --- |
| SSE progress | Process-local `SseEmitterRegistry`; sticky sessions required across pods | Redis pub/sub relay **or** sticky routing proven at every ingress hop |
| Runtime rate-limit | Process-local Bucket4j; `RUNTIME_RATE_LIMIT_DISTRIBUTED` defaults **`false`** in `application-prod.yml` | Shared Redis limiter (ADR-0031) enabled and verified on the traffic path |
| Multi-instance overall | **Incomplete** — single-replica topology is the v1 gate | Prerequisites table rows 1–5 closed |

Companion SOR-S07 note [0044-multi-instance-correctness-baseline.md](./0044-multi-instance-correctness-baseline.md): Decision §1 «distributed default in prod» wording is **aspirational / residual** — do **not** read Accepted there as Redis rate-limit delivered. Ops: [runbook § Multi-instance residuals](../../operations/runbook.md#multi-instance-residuals-adr-0044). Behavior SoT: [prod-ops-security-hardening.md](../../behavior/prod-ops-security-hardening.md) (D01B-C9).

Branch directives for Wave LR-B tasks:

- **LR-B2** — **recommended low-cost insurance under the Docker Compose
  single-container topology; MANDATORY before the first K8s blue-green prod
  deployment** (the chart keeps blue+green resident, so schedulers double-run without
  a mutex). Still scheduled this wave.
- **LR-B3** — implement heartbeat/headers/nginx SSE location as planned; the
  multi-replica section **records the sticky-routing-or-relay constraint only** (no
  relay implementation in v1).
- **LR-B4** — follow **branch (b)**: in-process accepted for v1 with the constraints
  above; the ledger seam «Async batch transport» is re-annotated accepted-for-v1 by
  LR-B4.

## Alternatives Considered

| Alternative | Why not chosen |
| --- | --- |
| Multi-replica v1 (keep P15 HPA as shipped) | All four process-local components must be reworked first (scheduler mutex, shared rate limit, SSE sticky/relay, Kafka transport) plus ADR-0039 Redisson locks; that launch risk outweighs the single-replica capacity risk — v1 has no customer-facing portal and upstream API calls are low-frequency |
| Sticky sessions to keep multi-replica without an SSE relay | Sticky routing addresses SSE only; schedulers, rate limits, and in-process async tasks still misbehave across replicas |
| Disable backend HPA but keep `replicaCount: 3` in prod | Same multi-instance defect class without autoscaling benefits; values would silently contradict this decision |
| Force Kafka transport in the prod compose for v1 | Adds a mandatory broker + operational surface without a v1 requirement; in-process is correct under a single replica with graceful drain (LR-B5) |

## Appendix — ShedLock dependency verification record (LR-B2)

Dependency-policy verification record (tech-stack guardrails). **ShedLock 6.10.0
(`shedlock-spring` + `shedlock-provider-jdbc-template`) was introduced with LR-B2 on
2026-07-04.** The 6.x line is the ShedLock line tested with Spring Boot 3.3 (7.x
targets Boot 3.4+/Spring 6.2):

| Item | Value |
| --- | --- |
| Coordinates | `net.javacrumbs.shedlock:shedlock-spring:6.10.0`, `net.javacrumbs.shedlock:shedlock-provider-jdbc-template:6.10.0` |
| Maven Central availability | **Verified** (2026-07-04, with LR-B2) |
| Company intranet repository availability | **Open checkpoint** — intranet SCA verification via the M9 submission flow is still pending and **must be closed before launch** |
| Fallback if unavailable | Flyway-managed lock table + `SELECT … FOR UPDATE SKIP LOCKED` guard (LR-B2 task sheet) |

### Historical note — ShedLock pin after Boot 4 (2026-07-13)

The table above is the **original LR-B2 verification record** and is **not** rewritten.

| Field | Value |
| --- | --- |
| **LR-B2 pin (2026-07-04)** | ShedLock **6.10.0** (Boot **3.3** line) — as recorded above |
| **Co-upgrade (Task Master #51)** | Slice `boot-4-1-upgrade` co-upgraded ShedLock to **7.7.0** for Spring Boot **4.1** / Spring Framework **7** compatibility (`shedlock-spring` + `shedlock-provider-jdbc-template`) |
| **Does not change** | Topology decision body (single serving replica, scale-out prerequisites, blue-green scheduler mutex requirement) — mutex technology remains ShedLock JDBC; only the **dependency major line** moved with the Boot parent |

## Related Documents

- [ADR 0039: Redisson Distributed Lock Evaluation](../technology-stack/0039-redisson-lock-evaluation.md) — single-instance assumption refined by this ADR
- [ADR 0030: Operational Platform Baseline](./0030-operational-platform-baseline.md) — blue-green + autoscaling baseline
- [ADR 0031: API Platform Hardening Baseline](../api/0031-api-platform-hardening-baseline.md) — Redis centralized rate-limit counters (deferred for v1 by this ADR; required on scale-out)
- [ADR 0033: Async Messaging and Task Retry Baseline](../async-processing/0033-async-messaging-and-task-retry-baseline.md) — Kafka transport baseline
- [0044 multi-instance correctness baseline (honesty residual)](./0044-multi-instance-correctness-baseline.md) — SOR-S07 companion; Accepted ≠ complete
- [PRR-D01b behavior — ADR-0044 honesty](../../behavior/prod-ops-security-hardening.md) — D01B-C9 / BDD-PRR-D01B-010…011
- [Operations runbook — multi-instance residuals](../../operations/runbook.md#multi-instance-residuals-adr-0044)
- [Launch Readiness Program](../../plan/launch-readiness-program.md) — §1 findings 3/4/5
- [LRP Wave LR-B detail](../../plan/detail/LRP-B-runtime-scaleout-session.md) — LR-B1/B2/B3/B4/B5 task sheets
- `deploy/helm/docgen/values*.yaml`, `deploy/helm/docgen/README.md`, `docker-compose.prod.yml` — synced in the same change set
