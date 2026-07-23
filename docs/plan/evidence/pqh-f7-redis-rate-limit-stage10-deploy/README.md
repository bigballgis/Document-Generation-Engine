# Stage 10 deploy evidence — PQH-F7 Redis rate-limit (TM #163)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["163"]` |
| **worktree** | `D:/working/DGE-pqh-f7-redis-rate-limit` |
| **branch** | `feat/pqh-f7-redis-rate-limit` @ `850b51c9` |
| **local_time** | 2026-07-23T23:45:27+08:00 |
| **utc_time** | 2026-07-23T15:45:27Z |
| **command** | `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "PQH-F7 Redis rate-limit stage10 deploy evidence COMPOSE_PROJECT_NAME=documentgenerationengine"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **rebuild** | full (`mvn package` + `pnpm build` + image package) |

## HEAD note

Feature tip SHA equals MAIN tip `850b51c9` at leaf start. PQH-F7 implementation was present as **uncommitted worktree changes** and was packaged into the new backend image via host `mvn package`.

## Previous known-good (rollback)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:7a76976af1eae43ec924d908de68507ebeb3ead75e47da209fc2bb253254ca7f` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:3a09d46600ae0abbd1171515767a9153db18997c12e541c420827e67fa437cd3` |

## Post-redeploy digests

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:2dfa77960ba95aa47d1eaaf9f89616c4fd2a1ee726378abc3e1b1f2337196934` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:b6fe5e5b1c43d215f06be9e64850b8a2077fa9b17f356f3af6ce7da52d7c750f` |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Rate-limit default-off honesty (PQH-F7)

| Check | Evidence |
| --- | --- |
| `RUNTIME_RATE_LIMIT_DISTRIBUTED` in container | **unset** (`rate-limit-env.txt`) |
| Packaged YAML default | `distributed: ${RUNTIME_RATE_LIMIT_DISTRIBUTED:false}` in `application.yml` + `application-prod.yml` (`rate-limit-packaged-yml.txt`) |
| Distributed=true enabled in stack | **No** (default-off honesty) |
| Runtime path smoke | `GET /api/v1/system/health` → **401** (auth filter chain alive; no Redis rate-limit fail-closed) |

RPM/BURST env present (`120`/`120`) with process-local default backend — stack healthy without forcing Redis shared limiter.

## Remediation (same session)

1. **FAIL** — worktree `.env` from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded).
2. **FAIL** — missing `COMPOSE_PROJECT_NAME` created project `dge-pqh-f7-redis-rate-limit` and collided on fixed `/docgen-minio` name. Removed orphan network/volumes; set `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173`.
3. **SUCCESS** — full queued redeploy; lock released; health green.

## Artifacts

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json` / `compose-ls.txt`
- `digests.txt` / `images.txt` / `backend-image.txt` / `frontend-image.txt`
- `healthz.json` / `frontend-4173.txt`
- `rate-limit-env.txt` / `rate-limit-packaged-yml.txt` / `runtime-path-smoke.txt`
- `queue-status.txt` / `git-*.txt`

## Notes

1. Single acceptance stack only — no second compose project / port offsets.
2. Backend image rebuilt with PQH-F7 jar; frontend digest unchanged (backend-only leaf).
3. `docker buildx imagetools inspect` failed (Hub unreachable for local-only tags); digests taken from local `docker images --digests`.
4. KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).

## Ready for

Stage **11** — `integration-merger` (merge worktree → main + cleanup). Do **not** mark #163 Done from this stage.
