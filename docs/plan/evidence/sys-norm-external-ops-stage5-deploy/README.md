# Stage 5 deploy evidence — SYS-NORM Wave 3 (`sys-norm-external-ops`)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["147"]` |
| **worktree** | `D:/working/DGE-sys-norm-external-ops` |
| **branch** | `feat/sys-norm-external-ops` |
| **local_time** | 2026-07-21T07:46:58+08:00 |
| **utc_time** | 2026-07-20T23:46:58Z |
| **command** | `pwsh .\scripts\docker-deploy-queue.ps1 -Reason "SYS-NORM Wave 3 Stage 5 E2E stack prep (task 147) canonical project"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | idle after release; no bypass |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Image digests (manifest list) — initial Stage 5

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:9af8a6b5234e6ce3bc1d5ffd80aa4343e4feda5e116385b88cedd6a512805d92` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:77a42edad4cf3d8c79eb4e72fea04900e4a63192712f11a1d5f08c79fe9db931` |

## Notes

1. First attempt: FAIL — worktree `.env` from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded); set `FRONTEND_PORT=4173`.
2. Second attempt: FAIL — compose project defaulted to directory name → `/docgen-minio` name conflict. Remediation: `COMPOSE_PROJECT_NAME=documentgenerationengine`; removed orphan `dge-sys-norm-external-ops_*` network/volumes.
3. Third attempt: **DEPLOY_OK** — host `mvn package` (tests skipped) + `pnpm build` + image package; backend/frontend recreated; stack healthy.
4. FE-only Wave 3 slice; frontend rebuild served on :4173. E2E not run in this stage.
5. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged from prior acceptance stack.

---

## Stage 5b / Stage 10 prep — Redeploy after nginx SPA `/api/invocations` fix

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **local_time** | 2026-07-21T08:04:07+08:00 |
| **utc_time** | 2026-07-21T00:04:07Z |
| **command** | `pwsh .\scripts\docker-deploy-queue.ps1 -Reason "SYS-NORM-W3 nginx SPA /api/invocations"` |
| **compose_project** | `documentgenerationengine` (QUEUE_ONLY; single host :8080/:4173) |
| **queue** | acquired → released; idle after; no bypass |
| **E2E** | not run (explicit Stage 5b/10 prep only) |

### Previous known-good (rollback)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:9af8a6b5234e6ce3bc1d5ffd80aa4343e4feda5e116385b88cedd6a512805d92` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:77a42edad4cf3d8c79eb4e72fea04900e4a63192712f11a1d5f08c79fe9db931` |

### Post-redeploy digests (manifest list)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:aa2d5d8f4937681f42d5b0a74ce0b0b2cfcbbf179cf0cab57536ee790872da3b` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:2e1a331ade71a437dc2ce7ee8ad84bf2de193fa9bb9263b5fc022a25323ac928` |

### Health (post-redeploy)

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `http://localhost:4173/api/invocations` | **200** (SPA `try_files` → `index.html`) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

### Image build notes

- Frontend image rebuilt with updated `nginx.conf` (`location = /api/invocations` + `^~ /api/invocations/` → `try_files … /index.html`).
- Confirmed inside running container: `/etc/nginx/conf.d/default.conf` contains SPA locations for `/api/invocations`.
- Backend layers largely cached; new manifest digest published.
- KAFKA_IMAGE bitnamilegacy warning unchanged (local/dev only).

### Next

Stage 6 `e2e-test-engineer` against http://localhost:4173 (BDD-SYS-NORM-W3-001…018), or Stage 10 evidence reuse if reviews already green.
