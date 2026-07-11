# Production Runbook (v1)

Operational procedures aligned with [ADR-0030](../adr/operations/0030-operational-platform-baseline.md).
For Kubernetes rollout specifics see [deploy/README.md](../../deploy/README.md) and
[deploy/blue-green-runbook.md](../../deploy/blue-green-runbook.md).

## Release gate

Run the automated release gate before tagging a release candidate:

```powershell
./scripts/release-gate.ps1
```

Linux agents:

```bash
./scripts/p0-gate.sh
./scripts/docker-deploy-gate.sh
```

Evidence is written to `artifacts/release-gate/<timestamp>/`.

## Local production profile

Build backend JAR first, then start the prod compose profile:

```powershell
mvn -B -ntp -f backend/pom.xml package -DskipTests
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d --build
```

- Backend liveness: `http://localhost:8080/healthz`
- Backend readiness: `http://localhost:8080/readyz` (Postgres `SELECT 1` only — see **Readiness scope** below)
- Frontend liveness/readiness: `http://localhost:4173/healthz` and `http://localhost:4173/readyz`
- Prometheus metrics (prod profile): `http://localhost:8080/actuator/prometheus`

Compose prod profile enables backend and frontend health checks with `service_healthy` gating (SOR-O05).

## Readiness scope (SOR-O06)

| Probe | Endpoint | Traffic gate | Diagnostic checks |
| --- | --- | --- | --- |
| Liveness | `/healthz` | N/A — process up only | None |
| Readiness | `/readyz` | **PostgreSQL `SELECT 1` only** (503 when down) | `checks.postgres`, `checks.redis`, `checks.minio`, `checks.kafka` |

Redis, MinIO, and Kafka appear in `/readyz` JSON for **diagnostics only** — they do **not** remove the pod from Service endpoints when Postgres is healthy (F8-C2 / SOR-O06 preserved). Optional contributors use `@Profile("!test")` with short timeouts (≤ 2s).

Example response (Postgres up, Redis down):

```json
{
  "status": "UP",
  "checks": {
    "postgres": { "status": "UP" },
    "redis": { "status": "DOWN" },
    "minio": { "status": "UP" },
    "kafka": { "status": "SKIPPED", "detail": "async transport is not kafka" }
  }
}
```

When `ASYNC_TRANSPORT` is not `kafka`, `checks.kafka.status` is `SKIPPED`.

See [deploy/k8s-health-probes.md](../../deploy/k8s-health-probes.md) for probe wiring.

## Observability

- **Structured logs:** `prod` Spring profile emits JSON logs via Logstash encoder (`logback-spring.xml`).
- **Trace propagation:** `X-Trace-Id` request header is echoed on responses and bound to MDC `traceId` for log correlation.
- **Metrics:** Actuator exposes `health`, `info`, `metrics`, and `prometheus` in prod profile. Management security permits unauthenticated scrape of `/actuator/prometheus` for in-cluster collectors (SOR-O01).
- **Kubernetes:** When `observability.serviceMonitor.enabled=true`, Helm renders a Prometheus Operator `ServiceMonitor` scraping `/actuator/prometheus`. Optional `PrometheusRule` alerts on pod restarts and elevated 5xx rates (see `deploy/helm/docgen/templates/`).

### Verify Prometheus scrape (local prod compose)

```bash
curl -sf http://localhost:8080/actuator/prometheus | head
```

Expect `# HELP` lines for JVM and HTTP metrics.

## Required environment variables (production)

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Management JWT signing (min 32 bytes; non-default enforced outside dev) |
| `POSTGRES_*` | Database connection |
| `MINIO_*` | Object storage |
| `APP_ENVIRONMENT` | Runtime environment label |
| `ASYNC_TRANSPORT` | Must be `kafka` in prod profile |

## Backup and restore (ADR-0030)

**Canonical procedure:** [backup-restore-runbook.md](./backup-restore-runbook.md) (LR-D2).

**Cadence (ADR-0030):** weekly full backup + daily incremental (operator-managed Postgres/MinIO tooling on this stack — logical `pg_dump`, WAL/snapshot guidance for prod-shaped Postgres, MinIO bucket mirror). Redis is rebuildable (no backup). Secrets stay in `.env` / cluster Secret patterns — never invent production values here.

**Status:** Scratch drill **executed 2026-07-12** — see [backup-restore-runbook.md § Drill evidence](./backup-restore-runbook.md#drill-evidence-2026-07-12--executed). Observed local RPO ≈ 0.93 min / RTO ≈ 4.75 min (**scratch scope only**; not a production compliance claim — no WAL/PITR).

## Disaster Recovery

Aligned with [ADR-0030](../adr/operations/0030-operational-platform-baseline.md): **RPO ≤ 15 min**, **RTO ≤ 30 min** (**targets**; local scratch drill 2026-07-12 met targets in-scope — see backup-restore evidence; production still needs WAL/PITR path). Failover is **manual**; use [blue-green runbook](../../deploy/blue-green-runbook.md) for app-level color revert. Full steps, confirmation gate, and evidence: [backup-restore-runbook.md](./backup-restore-runbook.md).

### Preconditions

- Isolated namespace/cluster or compose project for drill (never against production traffic). Prefer `-p docgen-scratch`.
- Latest Postgres dump (+ WAL/PITR material when configured) and MinIO backup bucket / volume copy available.
- Flyway is **forward-only** — no down migrations; schema fix = compensating migration; app rollback = blue-green color revert.
- **Explicit confirmation gate** before any destructive restore (`RESTORE-CONFIRM …`) — see backup-restore runbook.

### DR drill checklist

1. **Backup verify** — confirm latest Postgres and MinIO backups within the intended RPO window.
2. **Confirmation gate** — record `RESTORE-CONFIRM <project> <date>` before destructive steps.
3. **Restore Postgres** — restore dump (and WAL/PITR if applicable) to isolated instance; record restore start time.
4. **Restore MinIO** — mirror backup bucket → `STORAGE_BUCKET` (default `docgen-artifacts`); verify bucket head / sample object.
5. **Start stack** — `docker compose -p docgen-scratch -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d` (or Helm to isolated namespace).
6. **Flyway** — application startup runs migrate; expect no-op if backup is post-migration.
7. **Liveness / readiness** — `curl -sf http://localhost:8080/healthz` → 200; `curl -sf http://localhost:8080/readyz` → 200, `checks.postgres.status=UP`.
8. **Smoke** — one generated document round-trip (prior artifact from backup **or** regenerated equivalent).
9. **Record RPO/RTO** — note observed recovery time vs ADR-0030 **targets** (do not claim compliance if unmet).
10. **Archive evidence** — `artifacts/dr-drill/<YYYY-MM-DD>/` with:
   - `restore-log.txt` — commands and timestamps
   - `readyz.json` — post-restore readiness snapshot
   - `smoke-notes.md` — generation / document retrieval outcome
   - `rpo-rto.json` — `{ "rpoObservedMinutes": N, "rtoObservedMinutes": N, "targets": { "rpoMinutes": 15, "rtoMinutes": 30 }, "meetsTargets": null }`

First annual drill **execution** is tracked under **LR-D2** (Task Master #39). **2026-07-12 scratch drill complete** — evidence in [backup-restore-runbook.md § Drill evidence](./backup-restore-runbook.md#drill-evidence-2026-07-12--executed) and `artifacts/dr-drill/2026-07-12/` (gitignored locally; summary in runbook).

### Schema rollback policy

- **No Flyway down migrations.**
- Breaking schema: expand-contract / **compensating migration**; revert traffic via blue-green `activeColor` to previous image if the schema remains compatible — details in [backup-restore-runbook.md § Flyway](./backup-restore-runbook.md#flyway-forward-only-rollback-playbook) and [blue-green-runbook.md](../../deploy/blue-green-runbook.md).

## Incident response

1. **Detect:** Prometheus alert or user report; capture `X-Trace-Id` from UI error.
2. **Triage:** Check `/readyz`, dependency health (Postgres, Redis, MinIO, Kafka), recent deploy.
3. **Mitigate:** Scale replicas, rollback blue-green color, or disable feature flag seam documented in ADR.
4. **Communicate:** Email notification channel per ADR-0030 (operator runbook outside repo).
5. **Post-incident:** Update ledger evidence; file corrective ADR if architecture change required.

## Flyway migration rollout (blue-green)

1. Deploy **inactive** color with new image (migrations run on startup).
2. Run smoke on preview Service (`blueGreen.previewService.enabled`).
3. Manual approval per `blueGreen.requireManualApproval`.
4. Switch `activeColor` in values; verify `/readyz` on new color before draining old pods.
5. **Expand-contract:** backward-compatible migrations only; breaking schema changes require two-phase release notes.

## Rollback

1. Stop prod profile containers: `docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile prod down`
2. Deploy previous image tag (Kubernetes: revert `activeColor` or Helm revision).
3. Flyway migrations are forward-only; rollback requires a new migration if schema changed.

## Secret rotation (ADR-0030)

Target: automatic rotation every 30 days with zero-downtime refresh. Operator rotates `JWT_SECRET`, DB, and MinIO credentials in Secret Manager; rolling restart backend pods after Secret update. Verify login and runtime generation after rotation.
