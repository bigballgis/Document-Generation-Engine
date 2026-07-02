# E05 External Validation Evidence

> **Sync status (2026-07-02):** Re-aligned with document-as-code + docker-only validation constitution. Mirror: [execution-sync-ledger.md](../plan/execution-sync-ledger.md).

## Purpose

Track **target-environment** validation for enterprise dependencies that cannot be fully proven inside the repository alone.

**In-repo scope (Done):** E05-T01–T05 adapter code + integration tests + local Docker compose smoke (`.\scripts\docker-deploy.ps1`, `/healthz`, catalog API).

**Out-of-repo scope (E05-T06):** Production / staging HA cluster evidence — **release readiness**, not feature-delivery blocking.

## Dependency evidence matrix

| Dependency | Required evidence | Owner | Cadence | Status |
| --- | --- | --- | --- | --- |
| Local Docker compose (dev acceptance) | `docker-deploy.ps1` green; backend `/healthz` 200; UI `4173` 200; FOL import idempotent | Platform | Per behavior-changing slice | **Pass** (2026-07-02 ledger) |
| PostgreSQL (HA cluster) | Connectivity + migration smoke in target env | TBD | Per release candidate | Not Started |
| Redis cluster | Cache + lock + idempotency smoke | TBD | Per release candidate | Not Started |
| Kafka cluster | Topic ACLs + consumer lag check | TBD | Per release candidate | Not Started |
| MinIO tenancy | Bucket policy + SSE verification | TBD | Per release candidate | Not Started |
| AD / LDAP directory | Group resolution spot-check | TBD | Weekly | Not Started |
| Secrets provider | Secret mount + rotation drill | TBD | Per release candidate | Not Started |

## Pass/fail rule

- **Epic E05 in-repo:** **Done** when adapter code and tests are complete (current baseline).
- **E05-T06 release readiness:** Mark **Done** only when all **target-environment** rows above (excluding local Docker) are **Pass** with linked artifacts.
- Local Docker evidence does **not** substitute for HA/production rows.

## Execution log

See [e05-external-evidence-execution-log.md](./e05-external-evidence-execution-log.md).
