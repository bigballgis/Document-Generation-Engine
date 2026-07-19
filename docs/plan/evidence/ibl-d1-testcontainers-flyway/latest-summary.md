# IBL-D1 / #123 — Testcontainers PostgreSQL + Flyway lane evidence

| Field | Value |
| --- | --- |
| Status | **GATES_OK** (backend-engineer stage 4; not Done / not merged) |
| Timestamp (local) | 2026-07-19 |
| Worktree | `D:/working/DGE-ibl-d1-testcontainers-flyway` |
| Branch | `feat/ibl-d1-testcontainers-flyway` |
| Profile | **`testcontainers`** |
| BOM pin | Testcontainers **2.0.5** (Spring Boot 4.1.0 `testcontainers.version`) — Maven Central |
| Image | `postgres:16-alpine` |
| Flyway | **On** — applied **68** migrations → version **v68** |
| H2 vs TC docs | [test-database-strategy.md](../../../architecture/test-database-strategy.md) |
| frontend_ui_in_scope | false |
| Go-live / #3b / #5a / Wave D Done | **not** claimed |

## Gate commands

### 1. Default H2 verify (must stay green; TC excluded)

```powershell
mvn -B -ntp -f backend/pom.xml verify
```

| Metric | Result |
| --- | --- |
| Result | **BUILD SUCCESS** |
| Tests | **2133** run / **0** fail / **11** skipped |
| `FlywayPostgresqlMigrationSmokeTest` | **not executed** (`excludedGroups=testcontainers`) |
| Log | [default-verify.log](./default-verify.log) |

### 2. Testcontainers + Flyway lane

```powershell
mvn -B -ntp -f backend/pom.xml "-Ptestcontainers,dev-fast" test
```

(Targeted evidence run used `-Dtest=FlywayPostgresqlMigrationSmokeTest`; same profile properties.)

| Metric | Result |
| --- | --- |
| Result | **BUILD SUCCESS** |
| Tests | **1** run / **0** fail / **0** skipped |
| Migrations | **68** applied on PostgreSQL **16.14** |
| Ryuk | `TESTCONTAINERS_RYUK_DISABLED=true` (profile default — Hub pull blocked on this host) |
| Log | [tc-lane-smoke.log](./tc-lane-smoke.log) |

## Notes for architecture review

- Lane is **migration smoke** (Flyway API + TC Postgres), not full `@SpringBootTest` against PG.
- Default verify remains H2 + Flyway-off by design (F20 intentional split).
- Residuals for arch: optional expansion to Spring `@ServiceConnection` PG slices; CI job wiring on company runners; ryuk enable when Hub/mirror available.
