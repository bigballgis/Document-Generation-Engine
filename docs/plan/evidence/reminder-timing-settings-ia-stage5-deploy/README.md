# Stage 5 deploy evidence — Reminder timing settings IA (TM #153)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["153"]` |
| **worktree** | `D:/working/DGE-reminder-timing-settings-ia` |
| **branch** | `feat/reminder-timing-settings-ia` @ `2b5f0ac1258c50012771552687244681d57e3513` (+ uncommitted FE WIP packaged into images) |
| **local_time** | 2026-07-22T02:05+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -Reason "stage5 reminder-timing-settings-ia E2E prep (retry after JWT sync)"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | idle after release; no bypass; no second compose project |
| **mvn verify** | **not run** (deploy used `mvn package -Dmaven.test.skip=true` only; no Java behavior change) |
| **ForceRebuild** | not used (host `pnpm build` from worktree + image package refreshed FE layers) |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Image digests (local Id)

| Image | Id |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:4d772edda7d40842ae15584ccaa8a0c83ee0fe7d32aa9ad06177ee91cc5d5c0d` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:0d17c09b7c874dbb2de8a1d8598b00009bd595160a8445b9c50fc812a9e72026` |

## Slice markers (local `frontend/dist` after host build)

| Marker | Present |
| --- | --- |
| `SystemSettingsReminderTiming` | yes (2 chunk hits) |
| `TeamSettingsReminderTiming` / `TeamSettingsReminderTimingDialog` | yes |
| `reminder-timing` / `system-settings` | yes |
| `CollaborationTimeoutConfigPanel` | yes (shared panel component reused in new IA) |

## Notes

1. First attempt: **FAIL** — worktree `.env` created from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded); set `FRONTEND_PORT=4173` + `COMPOSE_PROJECT_NAME=documentgenerationengine`.
2. Second attempt: **DEPLOY_OK** — host `mvn package` (tests skipped) + `pnpm build` + image package; backend/frontend recreated; stack healthy.
3. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged.
4. E2E **not** run in this stage (handoff to `e2e-test-engineer` against http://localhost:4173).
5. No merge. Task #153 not marked Done.

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json` / `compose-ls.txt`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt`
- `backend-runtime.txt` / `frontend-runtime.txt`
- `fe-slice-markers.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt` / `git-status-sb.txt`

### Next

Stage 6 `e2e-test-engineer` against http://localhost:4173 (Reminder timing settings IA BDD).
