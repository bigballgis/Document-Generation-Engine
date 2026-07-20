# Stage 5 deploy evidence — `published-template-test-artifacts` / TM #144

- **Status:** DEPLOY_OK
- **Captured at:** 2026-07-21T02:13:10+08:00
- **Worktree:** `d:\working\DGE-published-template-test-artifacts`
- **Branch:** `feat/published-template-test-artifacts` @ `fb56f176`
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "Stage5 E2E prep published-template-test-artifacts TM144 COMPOSE_PROJECT_NAME=documentgenerationengine"` (full build; not `-SkipBuild`)
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)

## Images

| Image | Id | Created |
| --- | --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:7aa468695cf3d163e7e203b4010082247881b296094d279a3f2321dcae157604` | 2026-07-20T18:11:37.314025067Z |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:07d8831815c4cc7c6746e8a5f3ce9d8a72c67087cb161d0f672c6d6cd44cb36b` | 2026-07-20T18:11:30.610554357Z |

## Health

| Probe | URL | Result |
| --- | --- | --- |
| Backend | http://localhost:8080/healthz | **200** |
| Frontend | http://localhost:4173/ | **200** |

## Notes / remediation during Stage 5

1. First attempt: FAIL — `.env` created from `.env.example` with insecure JWT default; deploy script refused (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env`.
2. Second attempt: FAIL — missing `COMPOSE_PROJECT_NAME` caused fixed-name conflict (`docgen-minio`); orphan worktree-named network/volumes removed.
3. Third attempt: SUCCESS — canonical `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173`.

## Artifact files

- `compose-ps.txt` / `compose-ps.json`
- `healthz.json`
- `frontend-4173.txt`
