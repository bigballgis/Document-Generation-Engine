# Test database strategy — H2 vs Testcontainers PostgreSQL

| Field | Value |
| --- | --- |
| **Slice** | IBL-D1 / F20 (`ibl-d1-testcontainers-flyway`) |
| **Task Master** | **#123** |
| **Recorded** | 2026-07-19 |
| **Coordinates** | Testcontainers **2.0.5** (Spring Boot **4.1.0** BOM `testcontainers.version`) — Maven Central verified |

## Why two lanes

Production uses **PostgreSQL + Flyway on** (`spring.flyway.enabled=true`, `ddl-auto=validate`).
Default local/CI `mvn verify` intentionally stays **fast and Docker-free** by using **H2** with
Flyway **off**. That gap is **F20**. Closing it does **not** mean every developer must pull
Postgres for every verify — it means a **documented opt-in lane** exercises real PG + Flyway.

| Lane | Command | Database | Flyway | When to use |
| --- | --- | --- | --- | --- |
| **Default verify (H2)** | `mvn -B -ntp -f backend/pom.xml verify` | In-memory H2 (`MODE=PostgreSQL`) via `application-test.yml` | **Off** (`spring.flyway.enabled=false`); schema from Hibernate `ddl-auto=create-drop` | Everyday TDD, PR default gate, no Docker required |
| **Testcontainers + Flyway** | `mvn -B -ntp -f backend/pom.xml -Ptestcontainers,dev-fast test` | Testcontainers `postgres:16-alpine` (same image family as compose) | **On** — applies `classpath:db/migration` | Migration/SQL realism; CI optional/required job; before shipping Flyway changes |

Dedicated profile (without skipping static analysis):

```powershell
mvn -B -ntp -f backend/pom.xml -Ptestcontainers verify
```

(`-Ptestcontainers` alone runs **only** `@Tag("testcontainers")` tests; combine with default
verify in CI as a **second job**, not a replacement for the H2 gate.)

## What each lane catches

| Risk | H2 default | TC + Flyway |
| --- | --- | --- |
| Unit / slice logic, API contracts, most Spring tests | Yes | Not the focus (lane is migration smoke) |
| PostgreSQL-only SQL (`gen_random_uuid()`, `tsvector`, …) | Often silent or H2-dialect drift | **Fails** on migrate |
| Broken / incompatible Flyway scripts | Not applied | **Fails** the lane |
| Full app wiring against real PG | No (Redis/Kafka excluded; H2 DDL) | Smoke only today — expand later if needed |

## Implementation pointers

- Config: `backend/src/test/resources/application-test.yml` (H2 + Flyway off).
- Smoke test: `FlywayPostgresqlMigrationSmokeTest` (`@Tag("testcontainers")`).
- Maven: profile `testcontainers` sets Surefire `groups=testcontainers`; default
  `excludedGroups=testcontainers` keeps H2 verify free of Docker.
- Dependencies: `org.testcontainers:testcontainers`, `testcontainers-postgresql`,
  `testcontainers-junit-jupiter` (BOM-managed; no ad-hoc version pin).
- Image: `postgres:16-alpine` (pre-pull or reuse local compose image). Profile sets
  `TESTCONTAINERS_RYUK_DISABLED=true` so the lane does not require pulling
  `testcontainers/ryuk` from Docker Hub (common intranet / proxy constraint). Re-enable
  with `-Dtestcontainers.ryuk.disabled=false` when ryuk is available.

## Explicit non-goals (this leaf)

- Does **not** replace default H2 verify.
- Does **not** implement IBL-D2 LibreOffice CI lane.
- Does **not** invent SLOs (IBL-D3 — see [k6-nfr-confirmation-path.md](./k6-nfr-confirmation-path.md)) or flip go-live / checklist **#3b/#5a**.

## Traceability

- Behavior readiness: [ibl-d1-testcontainers-flyway.md](../behavior/ibl-d1-testcontainers-flyway.md)
- Program: [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) § IBL-D1 / F20
- Evidence: [../plan/evidence/ibl-d1-testcontainers-flyway/](../plan/evidence/ibl-d1-testcontainers-flyway/)
