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
---

# ADR 0028: Backend Platform Stack Baseline

## Status

Accepted

## Context

The project's base technology stack needs a stable backend foundation so architecture views, implementation planning, and future scaffolding stay aligned with the documented runtime, storage, security, and observability boundaries.

The user confirmed a Java/Spring-based backend direction together with relational storage, Redis, MinIO, standard backend testing, and common supporting infrastructure. This ADR records the accepted backend platform baseline so the technology decision log can stop carrying these items as pending session notes.

## Decision

The confirmed backend platform baseline is:

| Area | Decision | Notes |
| --- | --- | --- |
| Backend runtime | Java 21 + Spring Boot 3.x | Core API and backend services run on the Java/Spring baseline. |
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

## Consequences

- The backend implementation baseline is now documented separately from product requirements.
- Architecture views can reference a single backend stack baseline rather than a scattered set of session notes.
- The technology log can move the confirmed backend items from pending session state to accepted ADR-backed status.
- Future changes to these backend baseline components should be made by updating this ADR and the affected architecture views together.

## Alternatives Considered

- Keeping the backend stack only in the technology decision log: rejected because durable decisions need an ADR.
- Delaying backend stack acceptance until implementation starts: rejected because the architecture views already depend on these choices.
- Splitting each backend tool into its own ADR: rejected for this baseline because the selected components form one cohesive backend foundation decision set.

## Related Documents

- [Requirements Plan](../../requirements/requirements-plan.md)
- [PRD](../../product/PRD.md)
- [Domain Model](../../domain/domain-model.md)
- [Permission Matrix](../../security/permission-matrix.md)
- [Runtime View](../../architecture/runtime-view.md)
- [Data and Storage View](../../architecture/data-storage-view.md)
- [Security View](../../architecture/security-view.md)
- [AI Development Guide](../../architecture/ai-development-guide.md)