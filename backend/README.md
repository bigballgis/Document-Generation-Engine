# Backend

Java 25 + Spring Boot 4 module-first layout under `com.bank.docgen`.

## Packages

| Package | Responsibility |
| --- | --- |
| `sharedkernel.api` | Error envelope, metadata, global exception handling |
| `sharedkernel.health` | `/healthz`, `/readyz` |
| `sharedkernel.security` | Argon2 password hashing, JWT skeleton |
| `infrastructure.*` | Spring configuration, i18n |

## Run

```powershell
mvn -B -ntp verify
mvn spring-boot:run
```

Requires PostgreSQL for `spring-boot:run` (see root `docker-compose.yml`).

### Test database lanes (IBL-D1 / F20)

| Lane | Command | Notes |
| --- | --- | --- |
| **Default (H2)** | `mvn -B -ntp verify` | In-memory H2 + Flyway **off** (`application-test.yml`). Fast; no Docker. |
| **Testcontainers + Flyway** | `mvn -B -ntp -Ptestcontainers,dev-fast test` | Real `postgres:16-alpine` via Testcontainers; Flyway applies `db/migration`. Requires Docker. Broken migrations **fail** this lane. |

Full strategy: [docs/architecture/test-database-strategy.md](../docs/architecture/test-database-strategy.md).
