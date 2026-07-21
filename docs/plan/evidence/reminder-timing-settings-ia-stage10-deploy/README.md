# Stage 10 deploy evidence — Reminder timing settings IA (TM #153)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["153"]` |
| **worktree** | `D:/working/DGE-reminder-timing-settings-ia` |
| **branch** | `feat/reminder-timing-settings-ia` @ `2b5f0ac1258c50012771552687244681d57e3513` (+ uncommitted FE tip packaged into images) |
| **local_time** | 2026-07-22T03:02+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -Reason "Stage10 #153 reminder-timing-settings-ia tip rebuild"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | idle after release; no bypass; no second compose project |
| **mvn verify** | **not run** (deploy used `mvn package -Dmaven.test.skip=true` only; no Java behavior change) |
| **ForceRebuild** | not used (host `pnpm build` from worktree + image package refreshed FE layers) |
| **SkipBuild** | **false** — full tip rebuild required after post-stage5 product code changes |

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
| `documentgenerationengine-docgen-backend:latest` | `sha256:936817562fa149727526a59ea39b2d54e2f9ff2f28db4c0c9806a6b18180bdba` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:b51abf6194464350a1e853b877724bc7b5dddee3211120e1947c5cf7678389b7` |

## Tip currency vs Stage 5

| Check | Result |
| --- | --- |
| Stage 5 FE Id | `sha256:0d17c09b7c874dbb2de8a1d8598b00009bd595160a8445b9c50fc812a9e72026` |
| Stage 10 FE Id | `sha256:b51abf6194464350a1e853b877724bc7b5dddee3211120e1947c5cf7678389b7` |
| FE Id changed | **yes** |
| Container serves `index-naEwqKMr.js` | yes |
| Container serves `DashboardView-BJje1P76.js` | yes |

## Slice markers (local `frontend/dist` after host build)

| Marker | Present |
| --- | --- |
| `SystemSettingsReminderTiming` | yes (3 chunk hits) |
| `TeamSettingsReminderTiming` / `TeamSettingsReminderTimingDialog` | yes |
| `reminder-timing` / `system-settings` | yes |
| `CollaborationTimeoutConfigPanel` | yes |
| `DashboardView` | yes (journey CTA tip) |

## Notes

1. **DEPLOY_OK** — host `mvn package` (tests skipped) + `pnpm build` + image package; backend/frontend recreated; stack healthy.
2. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged.
3. No merge. Task #153 not marked Done.

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json` / `compose-ls.txt`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt`
- `backend-runtime.txt` / `frontend-runtime.txt`
- `fe-slice-markers.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt` / `git-status-sb.txt`
