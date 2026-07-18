# IBL-B5 / #117 — Stage 10 queued Docker deploy evidence

| Field | Value |
| --- | --- |
| Status | **DEPLOY_OK** |
| Timestamp (UTC) | 20260718T220534Z |
| Worktree | `D:/working/DGE-ibl-b5-seal-geometry` |
| Tip | `d74594054080cd58360032ee944477f554a57418` (`d7459405`) |
| Tip subject | feat(authoring): validate seal placement against authorized area geometry |
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

- backend image: `sha256:af0225960ce78157eb3282689a22fb5f8f44e28107c7b707e8a5f937935c94f7`
- frontend image: `sha256:82adc33444d7b98e61e1be977e5ab0368a619cfe127620657968fbf03d9f596e`
- backend inspect: `sha256:af0225960ce78157eb3282689a22fb5f8f44e28107c7b707e8a5f937935c94f7|2026-07-18T22:04:19.793324412Z|healthy`
- frontend inspect: `sha256:82adc33444d7b98e61e1be977e5ab0368a619cfe127620657968fbf03d9f596e|2026-07-18T22:04:57.820711993Z|healthy`

See also `compose-ps.txt`, `images-backend.txt`, `images-frontend.txt`.

## LibreOffice

| Probe | Result |
| --- | --- |
| `docker exec docgen-backend soffice --version` | `LibreOffice 7.3.7.2 30(Build:2)` |

No Word baseline regeneration in this Stage 10 run.

## Deploy notes

- First queue attempt refused insecure JWT default from freshly created worktree `.env`; `JWT_SECRET` synced from MAIN (len=64, non-insecure; not logged).
- Retried with `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173`.
- `KAFKA_IMAGE` Hub bitnamilegacy warning retained (LOCAL/DEV ONLY) — not production coordinate claim.
- Slice is rendering/seal geometry (IBL-B5); `frontend_ui_in_scope=false`.
