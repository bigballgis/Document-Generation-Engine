# Backup & Restore Runbook

**Status:** Draft (LR-D2) — pending drill validation
**Last updated:** 2026-07-05
**ADR reference:** [ADR-0030 operational platform baseline](../../adr/operations/0030-operational-platform-baseline.md) (RPO ≤ 15 min / RTO ≤ 30 min)

## Purpose

Define the backup and restore procedure for the document generation platform's stateful
components so that recovery is rehearsed, timed, and meets the ADR-0030 RPO/RTO commitments.

## Stateful components

| Component | Storage | Recovery strategy |
| --- | --- | --- |
| PostgreSQL | `docgen-postgres-data` Docker volume | pg_dump + WAL archive (prod); volume snapshot (dev) |
| MinIO | `docgen-minio-data` Docker volume | `mc mirror` to backup bucket; rebuildable from Postgres + master DOCX |
| Redis | in-memory | **Rebuildable** — cache + idempotency + revocation list; no backup needed |
| Flyway migrations | version-controlled SQL | Forward-only; never restore a migration, replay from scratch |

## Backup procedure

### PostgreSQL (daily logical backup + continuous WAL)

```bash
# Daily logical backup (cron 02:00)
docker compose exec docgen-postgres pg_dump -U docgen -Fc docgen > backups/docgen-$(date +%F).dump

# Continuous WAL archive (prod) — configure in postgresql.conf:
# archive_mode = on
# archive_command = 'test ! -f /backups/wal/%f && cp %p /backups/wal/%f'
```

Retention: 30 daily backups + 12 monthly + 7 annual (compliance).

### MinIO (daily mirror)

```bash
# Mirror the artifacts bucket to a backup bucket (cron 02:30)
docker compose exec docgen-minio mc alias set local http://localhost:9000 docgen docgen_local_pwd
docker compose exec docgen-minio mc mirror local/docgen-artifacts local/docgen-artifacts-backup
```

### Secrets

Secrets (`.env`, JWT secret, MinIO keys) are **NOT** backed up here — they live in the
orchestrator's secret store (Kubernetes Secret, Docker Compose `.env`). Restore them from
the secret store, never from a filesystem backup.

## Restore procedure

### PostgreSQL restore (PITR or logical)

```bash
# 1. Stop the backend (graceful)
docker compose stop docgen-backend

# 2. Restore the logical dump (RTO ~10 min for 10GB)
docker compose exec docgen-postgres pg_restore -U docgen -d docgen --clean --if-exists < backups/docgen-2026-07-04.dump

# 3. (PITR) Replay WAL to a specific timestamp:
#    recovery_target_time = '2026-07-04 14:30:00'

# 4. Restart the backend
docker compose up -d docgen-backend
curl -f http://localhost:8080/healthz
```

### MinIO restore

```bash
docker compose exec docgen-minio mc mirror local/docgen-artifacts-backup local/docgen-artifacts
```

### Redis

No restore — Redis rebuilds from PostgreSQL on next request. The idempotency cache and
revocation list repopulate as traffic resumes.

## Drill checklist (LR-D2 acceptance)

- [ ] Restore PostgreSQL from a daily backup to a staging instance; verify row counts match.
- [ ] Time the restore (target: < 10 min for 10GB).
- [ ] Restore MinIO from the backup bucket; verify a sample master DOCX downloads.
- [ ] Verify Flyway migrations replay cleanly on an empty database.
- [ ] Verify the backend starts and `/healthz` returns 200 after restore.
- [ ] Record the actual RPO and RTO; compare to ADR-0030 commitments.

## Forbidden

- `docker compose down -v` during restore — destroys the volume being restored to.
- Restoring Flyway migrations by copying SQL files — migrations replay via `flyway migrate`.
- Backing up `.env` or secrets to the filesystem backup — secrets live in the secret store.
- Skipping the drill — RPO/RTO claims without a timed drill violate ADR-0030.
