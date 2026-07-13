---
id: ADR-0028
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - architecture
adrNumber: "0028"
topic: technology-stack
related:
  - docs/architecture/README.md
  - docs/architecture/runtime-view.md
  - docs/architecture/data-storage-view.md
  - docs/architecture/security-view.md
  - docs/architecture/ai-development-guide.md
  - docs/behavior/boot-4-1-upgrade.md
  - docs/architecture/technology-stack-decisions.md
---

# ADR 0028: Backend Platform Stack Baseline

## Status

Accepted (amended 2026-07-13 — Spring Boot **3.x → 4.x**; second amendment 2026-07-13 — Java **21 → 25**)

## Context

The project's base technology stack needs a stable backend foundation so architecture views, implementation planning, and future scaffolding stay aligned with the documented runtime, storage, security, and observability boundaries.

The user confirmed a Java/Spring-based backend direction together with relational storage, Redis, MinIO, standard backend testing, and common supporting infrastructure. This ADR records the accepted backend platform baseline so the technology decision log can stop carrying these items as pending session notes.

## Decision

The confirmed backend platform baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Backend runtime | Java 25 + Spring Boot 4.x (target pin **4.1.0**) | Core API and backend services run on the Java/Spring baseline. Compile with Maven `release 25`. Boot 4.1 supports Java 17–26; **this project targets Java 25**. |
| Database | PostgreSQL | Relational primary store for durable business state and audit data. |
| Cache | Redis | Short-lived cache, idempotency coordination, and distributed coordination where approved. |
| Object storage | MinIO | Object storage for masters, generated documents, previews, and large artifacts. |
| Backend build tool | Maven | Backend build and dependency management baseline. |
| Database migration tool | Flyway | Database schema evolution baseline. |
| Backend ORM / data access | Spring Data JPA + QueryDSL | Primary data-access baseline for relational persistence and query composition. |
| Backend auth framework | Spring Security + JWT | Authentication and authorization baseline for backend APIs. |
| Backend testing | JUnit 5 + Mockito + Testcontainers + RestAssured | Backend testing and API verification baseline. |
| Backend observability | Micrometer + Prometheus + Grafana + OpenTelemetry | Metrics, monitoring, and tracing baseline. |
| Logging stack | Logback + JSON structured logs | Structured operational logging baseline. |
| API docs and debug tooling | springdoc-openapi + Swagger UI | API documentation and debug discovery baseline. |
| JSON serialization | Jackson | JSON serialization and deserialization baseline. |
| Object mapping | MapStruct | Mapper generation baseline. |
| Input validation | Jakarta Bean Validation (Hibernate Validator) | Request and domain input validation baseline. |
| Distributed lock | Redis (Redisson) | Distributed coordination and locking baseline. |
| Task scheduling | Quartz | Scheduled job baseline. |
| Cache serialization | Jackson JSON | Cache value serialization baseline. |
| HTTP client | Spring WebClient | Outbound HTTP client baseline. |
| Resilience and retry | Resilience4j | Retry, timeout, and resilience baseline. |
| Database connection pool | HikariCP | Connection pooling baseline. |
| Rate limiting | Bucket4j | Rate-limiting baseline. |
| Configuration management | Spring Profiles + environment variables | Environment-specific configuration baseline. |

These decisions are accepted as the backend foundation. More specialized operational decisions that are not listed above remain pending until they are explicitly confirmed and synchronized into a follow-up ADR or architecture view.

## Amendment — 2026-07-13 (Spring Boot 3.x → 4.x)

**Prior decision (2026-06-08):** Backend runtime = **Java 21 + Spring Boot 3.x**.

**Amended decision (first amendment):** Backend runtime = **Java 21 + Spring Boot 4.x**, project target pin **`spring-boot-starter-parent` 4.1.0**. (Java later moved to **25** in the second amendment below.)

| Field | Value |
| --- | --- |
| **Date** | 2026-07-13 |
| **Rationale** | Explicit user confirmation to upgrade the Spring Boot parent baseline to **4.1.0** (user said “4.10”; interpreted as **4.1.0**). Tracked as Task Master **#51** (`boot-4-1-upgrade`). |
| **Behavior note** | [boot-4-1-upgrade.md](../../behavior/boot-4-1-upgrade.md) — `bdd_readiness: not-applicable` (platform/ops baseline, not product journey). |
| **Java (at this amendment)** | Maven compile **`release 21`** retained in the first amendment. Boot 4.1 supports Java 17–26. |
| **Scope of amendment** | Runtime major line only (Boot **3.x → 4.x** / pin **4.1.0**). Does **not** invent unrelated framework switches (PostgreSQL, Redis, MinIO, Vue, Kafka, etc. unchanged). |
| **Implementation surface** | `backend/pom.xml` parent bump + companion co-upgrades are owned by **backend-engineer** (pipeline stage 4). This ADR records the **accepted target** baseline before/with that change. |

**Consequences of this amendment:**

- Spring Framework major line moves to **7.x** via the Boot 4 BOM.
- Companion dependency co-upgrades (BOM-managed starters, springdoc, test stack, third-party Boot-aligned artifacts) are required for green `mvn verify` and clean app start.
- Migration risks: Boot 4 / Framework 7 breaking changes, property renames, security/test helper deltas, Checkstyle/PMD/SpotBugs/JaCoCo on new bytecode — see the behavior note.
- Rollback path for the upgrade slice: restore Boot **3.3.13** parent + companion pins and redeploy the prior image via the single-host deploy queue.

**Does not supersede:** [ADR-0037](./0037-backend-dependency-realization-sequencing.md) MapStruct/QueryDSL sequencing amendments; those remain in force for realization status.

## Amendment — 2026-07-13 (Java 21 → 25; same Task #51)

**Prior decision (after first 2026-07-13 amendment):** Backend runtime = **Java 21 + Spring Boot 4.x** (pin **4.1.0**).

**Amended decision:** Backend runtime = **Java 25 + Spring Boot 4.x**, project target pin **`spring-boot-starter-parent` 4.1.0**. Compile with Maven **`release 25`**.

| Field | Value |
| --- | --- |
| **Date** | 2026-07-13 (second amendment, same calendar day) |
| **Rationale** | Explicit user confirmation of Java **25** (user said “25”) for the same Boot **4.1.0** upgrade slice. Tracked as Task Master **#51** (`boot-4-1-upgrade`) — folded into the existing slice; **not** a new formal phase. |
| **Behavior note** | [boot-4-1-upgrade.md](../../behavior/boot-4-1-upgrade.md) — acceptance = **Java 25 + Boot 4.1.0**. |
| **Java** | Maven compile **`release 25`**. Runtime / container JRE baseline moves to Temurin **25** (see [container-hardening.md](../../../deploy/container-hardening.md)). |
| **Boot** | Unchanged: Spring Boot **4.x** / pin **4.1.0** (first amendment retained). |
| **Scope of amendment** | Java language/runtime level only (**21 → 25**). Does **not** invent unrelated framework switches. |
| **Implementation surface** | `backend/pom.xml` toolchain (`release` / compiler) + Docker/runtime image pins owned by **backend-engineer**. Docs record the accepted target before/with that change. |

### Transitional seam — Jackson 2 via `spring-boot-jackson2` (2026-07-13)

Spring Boot **4** defaults to **Jackson 3**. This upgrade slice **defers** that default:

| Field | Value |
| --- | --- |
| **Runtime posture** | Explicit `org.springframework.boot:spring-boot-jackson2` + application code on `com.fasterxml.jackson.*` (Jackson **2**) |
| **Confirmed** | Jackson remains the JSON baseline (decision table row above). The **major line** stays Jackson **2** for this slice. |
| **Deferred** | Migrating to Boot 4’s default Jackson **3** / `tools.jackson` packages — **not** in scope for Task Master **#51** |
| **Why deferred** | Avoid a second major migration (package renames, module APIs, companion lib alignment) inside the Boot 4.1 parent bump |
| **Exit criteria** | Dedicated follow-up: remove `spring-boot-jackson2`, align code/tests to Jackson 3, green `mvn verify` — optional Task Master note only; **do not** invent a formal phase |
| **Ledger** | Transitional seam «Jackson 2 via spring-boot-jackson2» in [execution-sync-ledger.md](../../plan/execution-sync-ledger.md); behavior note [boot-4-1-upgrade.md](../../behavior/boot-4-1-upgrade.md) |

This is a **documented transitional bridge**, not a silent stack switch and not a reversal of the Jackson baseline.

## Consequences

- The backend implementation baseline is now documented separately from product requirements.
- Architecture views can reference a single backend stack baseline rather than a scattered set of session notes.
- The technology log can move the confirmed backend items from pending session state to accepted ADR-backed status.
- Future changes to these backend baseline components should be made by updating this ADR and the affected architecture views together.
- After the 2026-07-13 amendments, stack mirrors (tech-stack guardrails, architecture decision log, agent/README stack blurbs) must say **Java 25 + Spring Boot 4.x / 4.1.0**, not Java 21 / Boot 3.x.

## Alternatives Considered

- Keeping the backend stack only in the technology decision log: rejected because durable decisions need an ADR.
- Delaying backend stack acceptance until implementation starts: rejected because the architecture views already depend on these choices.
- Splitting each backend tool into its own ADR: rejected for this baseline because the selected components form one cohesive backend foundation decision set.
- Remaining on Spring Boot 3.3.x after Task #49 ops hygiene: rejected for the active baseline after explicit user confirmation of Boot **4.1.0** (Task #51). Historical #49 evidence remains valid for that slice.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Runtime View](../../architecture/runtime-view.md)
- [Data and Storage View](../../architecture/data-storage-view.md)
- [Security View](../../architecture/security-view.md)
- [AI Development Guide](../../architecture/ai-development-guide.md)
- [Technology Stack Decision Log](../../architecture/technology-stack-decisions.md)
- [boot-4-1-upgrade behavior note](../../behavior/boot-4-1-upgrade.md) (Task Master **#51**)
