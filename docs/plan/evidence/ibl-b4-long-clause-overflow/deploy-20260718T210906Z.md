# IBL-B4 / #116 — Stage 10 queued Docker deploy evidence

| Field | Value |
| --- | --- |
| Status | **DEPLOY_OK** |
| Timestamp (UTC) | 20260718T210906Z |
| Worktree | `D:/working/DGE-ibl-b4-long-clause-overflow` |
| Tip | `d6b389d17102b5f6a6006ce95d2c08b28cb130b6` (`d6b389d1`) |
| Tip subject | feat(rendering): activate IBL-B4 long-clause overflow golden theme 08 |
| Command | `.\scripts\docker-deploy-queue.ps1 -ForceRebuild` |
| Compose project | `documentgenerationengine` (single host stack) |
| frontend_ui_in_scope | false — E2E N/A |
| Go-live | **not** claimed |
| Word baselines | **not** invented |

## Health

| Check | Result |
| --- | --- |
| `GET http://localhost:8080/healthz` | HTTP 200 body `{"status":"UP"}` |
| `http://localhost:4173/` | **200** |

## Images / containers

- backend image: `sha256:9e373b00ab62ff453d1ede1f2bf4aa74e31d61869b00d20fd0aec1c55952200c` created `2026-07-18T21:07:42.544193984Z`
- frontend image: `sha256:6728efd48ce70f15be0bfe53a53b9a1a4436aa42a905065045aee7c3d4a2e451` created `2026-07-18T07:12:54.610252806Z`
- backend inspect: `sha256:9e373b00ab62ff453d1ede1f2bf4aa74e31d61869b00d20fd0aec1c55952200c|2026-07-18T21:07:51.422387091Z|healthy`
- frontend inspect: `sha256:6728efd48ce70f15be0bfe53a53b9a1a4436aa42a905065045aee7c3d4a2e451|2026-07-18T21:07:53.319014585Z|healthy`

See also `compose-ps.txt`, `images-backend.txt`, `images-frontend.txt`.

## LibreOffice / golden PDF page-count

| Probe | Result |
| --- | --- |
| `docker exec docgen-backend soffice --version` | `LibreOffice 7.3.7.2 30(Build:2)` |
| Host `soffice` | unavailable |
| `docker cp` into running backend | **blocked** (rootfs read-only hardening) |
| Stage-10 LO golden page-count smoke | **SKIPPED** (honest) |

Notes: see `lo-smoke-notes.txt`. Durable page-count lock remains surefire `bddIblB4_003_and_004` when soffice is available on the verify host; this deploy evidence does **not** claim live page-count PASS.

## Deploy notes

- First queue attempt refused insecure JWT default from freshly created worktree `.env`; `JWT_SECRET` synced from MAIN (len=64, non-insecure; not logged).
- Second attempt without `COMPOSE_PROJECT_NAME` hit `/docgen-minio` name conflict (directory-derived project); retried with `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173`.
- `KAFKA_IMAGE` Hub bitnamilegacy warning retained (LOCAL/DEV ONLY) — not production coordinate claim.
- Slice is rendering/golden-corpus (F13 long-clause overflow); `frontend_ui_in_scope=false`.
