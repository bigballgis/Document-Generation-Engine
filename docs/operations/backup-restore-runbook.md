# Backup & Restore Runbook

**Status:** Drill executed (LR-D2) — dated scratch evidence below; ADR-0030 RPO/RTO remain **targets**; **local drill met targets in-scope** but does **not** alone prove production compliance (no WAL/PITR)
**Last updated:** 2026-07-12
**Slice / Task Master:** LR-D2 · `lrp-d2-backup-restore` · Task Master **#39**
**BDD readiness:** [not-applicable](../behavior/lrp-d2-backup-restore.md) (ops docs + rehearsal; no product behavior)
**ADR reference:** [ADR-0030 operational platform baseline](../adr/operations/0030-operational-platform-baseline.md) — **targets** RPO ≤ 15 min / RTO ≤ 30 min (commitments; production compliance still requires operator WAL/PITR path)
**Related:** [Production runbook § DR](./runbook.md#disaster-recovery) · [Blue-green runbook](../../deploy/blue-green-runbook.md) · [deploy/README.md](../../deploy/README.md) · [k8s ConfigMap/Secret](../../deploy/k8s-config-secrets.md)
**Helper scripts:** `scripts/backup-stack.ps1` (non-destructive) · `scripts/dr-scratch-restore-drill.ps1` (requires `-ConfirmPhrase "RESTORE-CONFIRM <project> <date>"`)

## Purpose

Define backup and restore procedures for the document generation platform’s **stateful** components on the **documented Docker Compose / Helm stack**, so recovery can be rehearsed, timed, and compared to ADR-0030 RPO/RTO **targets**. This runbook is complete enough for a **deploy-engineer** scratch-stack drill; it does **not** invent cloud services outside that stack.

## Confirmed vs pending

| Kind | Content |
| --- | --- |
| **Confirmed (docs)** | Service/volume/bucket names below match `docker-compose.yml`, `docker-compose.prod.yml`, Helm `config.storageBucket`, and `application.yml` (`STORAGE_BUCKET`). Redis is rebuildable. Flyway is forward-only. Destructive restore requires an explicit confirmation gate. |
| **Drill (local scratch, 2026-07-12)** | Measured RPO ≈ **0.93 min**, RTO ≈ **4.75 min** on `docgen-scratch` (see evidence). **Meets targets in this drill scope** — still **not** a production compliance claim (logical dump + volume tar; no WAL/PITR). |

## Stack inventory (authoritative names)

Compose project files: `docker-compose.yml` (deps) + `docker-compose.prod.yml` (`--profile prod` for `docgen-backend` / `docgen-frontend`).

| Component | Compose service / container | Volume or state | Recovery strategy |
| --- | --- | --- | --- |
| PostgreSQL 16 | `docgen-postgres` | Volume **`docgen-postgres-data`** → `/var/lib/postgresql/data` | Logical `pg_dump` / `pg_restore`; prod adds WAL archive + scheduled volume/snapshot guidance |
| MinIO | `docgen-minio` | Volume **`docgen-minio-data`** → `/data` | Mirror primary bucket to a **backup bucket** on the same MinIO (or offline dump of the volume); objects are rebuildable from Postgres metadata + master DOCX where applicable |
| Redis 7 | `docgen-redis` | **No Docker volume** (ephemeral) | **Rebuildable** — cache, idempotency, revocation; **do not back up** |
| Kafka | `docgen-kafka` | Ephemeral broker (compose) | Async transport; not a durable business-data backup target for v1 local stack |
| Backend / frontend | `docgen-backend`, `docgen-frontend` | Stateless app containers | Redeploy images; secrets from `.env` / cluster Secret — not from DB/object dumps |
| Flyway SQL | version-controlled under `backend/` | Applied on startup | **Forward-only** — never “restore” migration files; roll forward with a compensating migration |

**Object bucket (app):** `STORAGE_BUCKET` default **`docgen-artifacts`** (`backend/src/main/resources/application.yml`; Helm `config.storageBucket`).  
**Note:** `.env.example` lists `MINIO_BUCKET=docgen-dev`; the running app reads **`STORAGE_BUCKET`**, not `MINIO_BUCKET`. Prefer `STORAGE_BUCKET=docgen-artifacts` for drills that match Helm/app defaults.

**DB credentials (local compose defaults from `.env.example` / compose):** `POSTGRES_DB=docgen`, `POSTGRES_USER=docgen`, `POSTGRES_PASSWORD` from env (default local only: `docgen_local_pwd`). **Do not invent or commit production secrets.**

**MinIO credentials:** `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` from env (local defaults in compose/`.env.example` only).

## Explicit confirmation gate (mandatory)

Destructive steps (overwrite a live volume, `pg_restore --clean`, mirror that replaces objects, `docker compose down -v`, color cutover after data restore) **must not** run silently.

Before any destructive restore command, the operator **must**:

1. State aloud (or in the ticket) the **target compose project / namespace** and confirm it is a **scratch** stack, **not** shared acceptance/`main` traffic.
2. Type an explicit confirmation phrase when prompted, e.g. `RESTORE-CONFIRM <project-name> <YYYY-MM-DD>`.
3. Record who confirmed and when in the drill evidence section.

**Forbidden:** scripts that auto-confirm, pipe `yes` into destructive restore, or run `down -v` without the gate. Prefer documenting commands for deploy-engineer; any helper script must refuse unless `-ConfirmPhrase` matches.

---

## Backup procedure

### PostgreSQL — local / scratch (logical dump)

ADR-0030 cadence baseline: **weekly full + daily incremental**. On this Compose stack, the practical local equivalent is a **scheduled logical dump** (custom format `-Fc`) plus, for production-shaped ops, WAL/archive or volume snapshots (next subsection).

```powershell
# From repo root. Ensure docgen-postgres is healthy.
# Prefer dump-inside-container + docker cp — PowerShell `>` can corrupt -Fc binary dumps.
New-Item -ItemType Directory -Force -Path backups | Out-Null
$stamp = Get-Date -Format 'yyyy-MM-dd'
docker compose exec -T docgen-postgres pg_dump -U docgen -Fc -f /tmp/docgen-$stamp.dump docgen
docker compose cp "docgen-postgres:/tmp/docgen-$stamp.dump" "backups/docgen-$stamp.dump"
```

Retention guidance (operator-managed; not automated in-repo): keep enough daily dumps to cover the RPO window plus weekly fulls aligned with ADR-0030 (e.g. 30 daily + 12 monthly as a compliance-oriented starting point — **not** a confirmed legal retention requirement in this runbook).

### PostgreSQL — production guidance (WAL / scheduled snapshot)

Do **not** invent a managed cloud DBaaS. On the **same stack family** (self-managed Postgres used by Compose or external Postgres pointed at by Helm `externalServices.postgres`):

| Mechanism | Intent | Notes |
| --- | --- | --- |
| **Daily logical dump** | Portable full restore | Same `pg_dump -Fc` pattern; store dumps **off-host** (operator backup store). |
| **Continuous WAL archive** | Support RPO ≤ 15 min | Enable `archive_mode` / `archive_command` (or equivalent) so WAL segments copy to durable backup storage; use PITR (`recovery_target_time`) when restoring. Exact paths are operator-owned. |
| **Scheduled volume / disk snapshot** | Fast RTO for large data dirs | Snapshot the volume backing Postgres data (Compose volume host path, or PV/disk for external Postgres) on a schedule complementary to logical dumps. Snapshots alone do not replace tested `pg_restore` drills. |

Weekly full + daily incremental (ADR-0030) maps to: weekly base dump (or snapshot) + daily dump/incremental + WAL for the last ≤15 minutes of change when PITR is configured.

### MinIO — bucket backup strategy

Primary app bucket: **`docgen-artifacts`** (unless `STORAGE_BUCKET` overrides).

Strategy:

1. Create a **separate backup bucket** on the same MinIO instance (e.g. `docgen-artifacts-backup`) — still within this stack; no external object cloud required for the drill.
2. Mirror primary → backup on a schedule (e.g. after the DB dump window).
3. Optionally copy the Docker volume `docgen-minio-data` offline as a cold spare (confirmation-gated restore).

The MinIO **server** image does **not** ship `mc`. Prefer a one-shot **mc client** container on the Compose network when the image is pullable:

```powershell
# Create backup bucket + mirror (idempotent create). Credentials from .env / compose defaults only.
docker run --rm --network <compose-network> minio/mc `
  alias set local http://docgen-minio:9000 $env:MINIO_ROOT_USER $env:MINIO_ROOT_PASSWORD
# Or interactively with known local defaults — never paste production keys into git history.

docker run --rm --network <compose-network> minio/mc mb --ignore-existing local/docgen-artifacts-backup
docker run --rm --network <compose-network> minio/mc mirror --overwrite local/docgen-artifacts local/docgen-artifacts-backup
```

Discover the Compose network name with `docker network ls` (typically `<project>_default`). For the default project name matching the repo folder, services resolve as `docgen-minio`.

**Fallback (used in LR-D2 drill when `minio/mc` registry pull failed):** tar the Docker volume with a local `alpine` image via `scripts/backup-stack.ps1` / restore path in `scripts/dr-scratch-restore-drill.ps1` (stop MinIO → extract into scratch volume → start). Confirmation gate still required before destructive extract onto the target volume.

### Redis

**No backup.** After restore, Redis starts empty; cache / idempotency / session-adjacent entries rebuild from traffic and Postgres. Do not dump `REDIS_*` as a recovery dependency.

### Secrets

Secrets are **not** part of Postgres/MinIO dumps:

| Source | What |
| --- | --- |
| Local | Copy of `.env` from `.env.example` patterns only — operator workstation / vault, **never** committed |
| Kubernetes | Cluster Secret / External Secrets (`POSTGRES_*`, `MINIO_ROOT_*`, `JWT_SECRET`) per [deploy/k8s-config-secrets.md](../../deploy/k8s-config-secrets.md) and Helm `secrets.existingSecretName` |

Restore secrets from the orchestrator secret store, then start the stack. **Do not** invent production secret values in this document.

### Kafka / LibreOffice

Not backup targets for business durability on the local stack. Redeploy Compose services as needed after data restore.

---

## Flyway forward-only rollback playbook

Flyway migrations are **forward-only**. There are **no** down migrations and **no** “restore an old SQL file over the schema” path.

| Situation | Action |
| --- | --- |
| Bad **application** release, schema compatible | **Blue-green color revert** — flip `blueGreen.activeColor` to the previous stable color. See [deploy/blue-green-runbook.md](../../deploy/blue-green-runbook.md) § Manual rollback. Does **not** roll back schema. |
| Bad **schema** change already applied | **Roll forward** with a new **compensating** Flyway migration (expand/contract). Deploy the fixed image; let startup migrate. |
| Need older **data** | Restore Postgres (+ MinIO) from backup into an isolated stack; app version must be compatible with the schema version in that backup (startup Flyway is no-op if already at that version). |

**Do not** delete PVCs / Compose volumes as a “rollback” shortcut. Aligns with [deploy/README.md](../../deploy/README.md) and blue-green runbook warnings.

---

## Restore procedure (scratch stack)

### 0. Preconditions

- Scratch Compose project (recommended: `-p docgen-scratch`) or isolated namespace — **never** shared Docker acceptance traffic without change control.
- Fresh or empty target volumes for the scratch project (or explicitly confirmed overwrite).
- Backup dump file + MinIO backup bucket (or volume copy) available.
- `.env` for scratch copied from `.env.example` patterns (ports may need offsets if the main stack still holds 8080/4173 — prefer stopping the main stack or using a separate project **only when** the host is free; this machine runs **one** acceptance stack — see deploy queue constitution).

### 1. Confirmation gate

```text
RESTORE-CONFIRM docgen-scratch <YYYY-MM-DD>
```

Record confirmer in evidence. Abort if phrase mismatches.

### 2. Stop app containers (if already up)

```powershell
docker compose -p docgen-scratch -f docker-compose.yml -f docker-compose.prod.yml --profile prod stop docgen-backend docgen-frontend
```

### 3. Restore PostgreSQL (logical)

```powershell
# Destructive: --clean. Confirmation gate must already have passed.
docker compose -p docgen-scratch cp "backups/docgen-YYYY-MM-DD.dump" docgen-postgres:/tmp/restore.dump
docker compose -p docgen-scratch exec -T docgen-postgres `
  pg_restore -U docgen -d docgen --clean --if-exists /tmp/restore.dump
```

(PITR / WAL replay, when configured in prod:) set `recovery_target_time` and follow PostgreSQL recovery docs for the operator’s archive layout — not scripted here.

### 4. Restore MinIO

```powershell
docker run --rm --network <scratch-compose-network> minio/mc mirror --overwrite `
  local/docgen-artifacts-backup local/docgen-artifacts
```

(Alias setup same as backup section.)

### 5. Start stack and verify

```powershell
docker compose -p docgen-scratch -f docker-compose.yml -f docker-compose.prod.yml --profile prod up -d
# Wait for backend healthcheck (start_period up to 90s for Flyway).
curl -sf http://localhost:8080/healthz
curl -sf http://localhost:8080/readyz
```

Expect HTTP 200 on `/healthz`. Prefer also recording `/readyz` JSON (`checks.postgres.status=UP`) per [runbook.md](./runbook.md).

### 6. Document round-trip (acceptance Scenario A)

Pick **one**:

- Download a **previously generated** artifact object from MinIO / via management or runtime API that existed in the backup, **or**
- Run **one** sync generation (DOCX or PDF) against a known template and confirm the artifact is retrievable.

Record object key / template id and outcome in evidence.

### Redis after restore

No restore step. Empty Redis is expected.

---

## Drill procedure (LR-D2 — deploy-engineer executes)

Goal: timed rehearsal on a **scratch** stack; fill the evidence section; feed **LR-E2** launch checklist.

1. **Backup verify** — take or locate a fresh `pg_dump` and MinIO mirror; note backup completion time (for RPO calculation).
2. **Confirmation gate** — `RESTORE-CONFIRM …` before destructive steps.
3. **Restore Postgres + MinIO** into scratch project; wall-clock start.
4. **Bring up** prod profile compose; wait healthy.
5. **Verify** `/healthz` 200 (+ `/readyz` recommended).
6. **Document round-trip** — prior artifact or regenerated equivalent retrievable.
7. **Stop clock** — compute observed RTO (restore start → healthy + document OK) and observed RPO (time from last committed change included in backup → failure/drill cut point, or backup age as proxy when no injected failure).
8. **Compare** to ADR-0030 **targets** (≤15 / ≤30 min) — record pass/fail **without** updating ADR text.
9. **Archive** under `artifacts/dr-drill/<YYYY-MM-DD>/` (gitignored local evidence; summary may later sync into plan evidence by post-task doc-sync):
   - `restore-log.txt`
   - `readyz.json` / `healthz` note
   - `smoke-notes.md` (document round-trip)
   - `rpo-rto.json` — template below

### Drill checklist

- [x] Confirmation gate recorded (phrase, operator, timestamp)
- [x] Postgres restored from dump on scratch project
- [x] MinIO restored (volume tar fallback); sample object + regenerated DOCX succeed
- [x] Redis left empty / rebuildable (no restore attempted)
- [x] `/healthz` → 200
- [x] One generated document retrievable (prior object key + regenerated FOL DOCX)
- [x] Durations recorded vs ADR-0030 targets
- [x] Evidence folder populated; this runbook evidence section filled
- [x] No overclaim of **production** RPO/RTO compliance (scope note recorded)

---

## Drill evidence (2026-07-12 — executed)

> **Status:** **EXECUTED** (local Docker scratch). ADR-0030 figures below are **observed on this drill only** — not a production SLO certification.

| Field | Value |
| --- | --- |
| Drill date (ISO) | **2026-07-12** (cutover window UTC 2026-07-11T18:38Z–18:44Z) |
| Verifier (name / agent) | deploy-engineer (LR-D2 / Task Master #39) |
| Scratch project / environment | `docgen-scratch` / local Docker (host ports freed; acceptance project `dge-lrp-d6-load-smoke` stopped **without** `-v`) |
| Backup artifact id / path | `backups/docgen-2026-07-12.dump` (173 106 B) + `backups/minio-2026-07-12.tgz` (≈13.2 MB); manifest `backups/backup-manifest-2026-07-12.json` |
| Confirmation phrase used | `RESTORE-CONFIRM docgen-scratch 2026-07-12` @ 2026-07-11T18:39:11Z |
| Postgres restore duration | **2.71 s** (`pg_restore --clean --if-exists`) |
| MinIO restore duration | **13.27 s** (volume tar extract) |
| Time to `/healthz` 200 | **101.44 s** from RTO start (includes image retag for compose project-prefixed names) |
| Document round-trip duration / outcome | **PASS** — prior key `generated/DOC-00F8EABA/output.docx` present after restore; FOL `test-generate` → DOCX download **30 891 B** (PK zip) preview `fe82150d-34ee-4fdf-8c18-5b1cd63d45e3` |
| **Observed RPO (minutes)** | **0.933** (backup age at restore/cutover start; no post-backup writes injected) |
| **Observed RTO (minutes)** | **4.751** (restore start → healthz + document round-trip complete) |
| ADR-0030 targets | RPO ≤ 15 · RTO ≤ 30 |
| Meets targets? | **Yes — in local scratch scope only** (see honesty note). **Not** a production compliance claim. |
| Evidence path | `artifacts/dr-drill/2026-07-12/` (`restore-log.txt`, `healthz.txt`, `readyz.json`, `smoke-notes.md`, `rpo-rto.json`, `roundtrip-preview.docx`, …) |
| Notes / exceptions | `minio/mc` pull blocked — used volume tar; compose `-p` required retag of `dge-lrp-d6-load-smoke-docgen-*` → `docgen-scratch-docgen-*`; acceptance stack restored afterward (`/healthz` 200). No WAL/PITR. Scripts: `backup-stack.ps1`, `dr-scratch-restore-drill.ps1`. |

### `rpo-rto.json` (drill record)

```json
{
  "drillDate": "2026-07-12",
  "verifier": "deploy-engineer (LR-D2 / Task Master #39)",
  "rpoObservedMinutes": 0.933,
  "rtoObservedMinutes": 4.751,
  "targets": { "rpoMinutes": 15, "rtoMinutes": 30 },
  "meetsTargets": true,
  "healthzHttpStatus": 200,
  "documentRoundTrip": "PASS: prior DOC-00F8EABA + regenerated FOL DOCX 30891 bytes",
  "confirmationGate": "RESTORE-CONFIRM docgen-scratch 2026-07-12",
  "scope": "local-docker-scratch; pg_dump+volume-tar; no-WAL-PITR; not-production"
}
```

---

## Forbidden

- `docker compose down -v` (or deleting PVCs) during restore without the confirmation gate — destroys the volume being restored to or shared data.
- Silent / auto-confirmed destructive restore scripts.
- Restoring Flyway by copying old SQL over the DB — use migrate / compensating migration only.
- Backing up `.env` or production secrets into the dump/mirror store as the recovery path.
- Claiming ADR-0030 RPO/RTO **compliance** without a filled evidence section and dated drill.
- Inventing AWS RDS / S3 / managed backup products not present in this repo’s Compose/Helm stack.
- Running the drill against the shared host acceptance stack without isolating project/ports and queue discipline.

## Index / handoff

| Consumer | Use |
| --- | --- |
| [docs/README.md](../README.md) § Deployment & operations | Index entry |
| [docs/operations/runbook.md](./runbook.md) | Summary + link here |
| LR-E2 launch checklist | Dated drill row → [§ Drill evidence 2026-07-12](#drill-evidence-2026-07-12--executed); linked from [launch-readiness-checklist.md](./launch-readiness-checklist.md) row #6 (**CONDITIONAL** — scratch ≠ production compliance; Wave LR-E2 In Progress — not launch go) |
| **architecture-reviewer** | PASS_WITH_NOTES (Critical 0; merge_go true) — merge `3d78bc5` |
| **deploy-engineer** | Scratch drill executed 2026-07-12; evidence + gated scripts; MAIN doc-sync next → commit-review |
