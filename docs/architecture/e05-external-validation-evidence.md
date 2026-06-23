# E05 External Validation Evidence

> **Sync status (2026-06-23):** Outstanding external/deferred evidence — not blocking MVP. Mirror: [execution-sync-ledger.md](../plan/execution-sync-ledger.md).

## Purpose

Track **deployment-time** validation for enterprise dependencies that cannot be
fully proven inside the repository alone.

## Dependency evidence matrix

| Dependency | Required evidence | Owner | Cadence | Status |
| --- | --- | --- | --- | --- |
| PostgreSQL (HA cluster) | Connectivity + migration smoke in target env | TBD | Per release candidate | Not Started |
| Redis cluster | Cache + lock + idempotency smoke | TBD | Per release candidate | Not Started |
| Kafka cluster | Topic ACLs + consumer lag check | TBD | Per release candidate | Not Started |
| MinIO tenancy | Bucket policy + SSE verification | TBD | Per release candidate | Not Started |
| AD / LDAP directory | Group resolution spot-check | TBD | Weekly | Not Started |
| Secrets provider | Secret mount + rotation drill | TBD | Per release candidate | Not Started |

## Pass/fail rule

E05 epic may be marked **Done** in-repo when adapter code and tests are complete.
**Release readiness** additionally requires all rows above marked **Pass** with
linked evidence artifacts.

## Execution log

See [e05-external-evidence-execution-log.md](./e05-external-evidence-execution-log.md).
