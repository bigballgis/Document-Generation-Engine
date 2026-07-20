# Stage 10 deploy evidence — `published-template-test-artifacts` / TM #144

- **Status:** DEPLOY_OK
- **Captured at:** 2026-07-21T03:07:48+08:00
- **Worktree:** `d:\working\DGE-published-template-test-artifacts`
- **Branch:** `feat/published-template-test-artifacts` @ `ade18bdb5b04941225542a10798e36c377b932e7`
- **Mode:** Stage 10 health recheck (first `-SkipBuild` without `COMPOSE_PROJECT_NAME` FAILED with fixed-name conflict; remediated with canonical project + full queued redeploy)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "TM#144 published-template-test-artifacts stage10 health recheck COMPOSE_PROJECT_NAME=documentgenerationengine"`
- **Note on SkipBuild:** Queue acquired lock, but the underlying `docker-deploy.ps1` path executed a **full host compile + image package + recreate** (not restart-only). Treat as full queued redeploy evidence; stack healthy after recreate.
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)

## Images (post Stage 10 recreate)

| Image | Id | Created |
| --- | --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:d0c2b8f9bc5cbdefaf0c0c63634e13c0b4195890254525dc69da5d24856d3955` | 2026-07-20T19:02:43.81921465Z |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:9d462f7cb6292c347d94fa05f8c4985e97fe46ed1c07f638ecceb4e24ebd7dbf` | 2026-07-20T18:11:30.610554357Z |

## Stage 5 image refs (prior E2E prep)

| Image | Id |
| --- | --- |
| backend | `sha256:7aa468695cf3d163e7e203b4010082247881b296094d279a3f2321dcae157604` |
| frontend | `sha256:07d8831815c4cc7c6746e8a5f3ce9d8a72c67087cb161d0f672c6d6cd44cb36b` |

## Health

| Probe | URL | Result |
| --- | --- | --- |
| Backend | http://localhost:8080/healthz | **200** `{"status":"UP"}` |
| Frontend | http://localhost:4173/ | **200** |

## Remediation during Stage 10

1. First attempt: FAIL — missing `COMPOSE_PROJECT_NAME` caused fixed-name conflict (`docgen-minio`); orphan worktree-named network/volumes removed.
2. Second attempt: SUCCESS — canonical `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173`; containers recreated; health green; queue lock released.

## Artifact files

- `compose-ps.txt` / `compose-ps.json`
- `compose-ls.txt`
- `docker-ps.txt`
- `digests.txt`
- `healthz.json`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `queue-status.txt`
- `latest-summary.json`
