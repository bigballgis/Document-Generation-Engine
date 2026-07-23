# Stage 5 deploy evidence — PQH Leaf 3 N22 catalog row actions (TM #162)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["162"]` |
| **worktree** | `D:/working/DGE-pqh-n22-catalog-row-actions` |
| **branch** | `feat/pqh-n22-catalog-row-actions` @ `c094d5136f243407c5f221983bdce10527daa8b6` |
| **local_time** | 2026-07-23T18:58+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -Reason "Stage5 PQH N22 #162 E2E stack prep (retry after FE build OK)"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | idle after release; no bypass; no second compose project |
| **mvn verify** | **not run** (deploy used `mvn package -Dmaven.test.skip=true` only; no Java behavior change in this leaf) |
| **ForceRebuild** | not used (host `pnpm build` from worktree + image package refreshed FE/BE layers) |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (ContentLength=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Image digests (local Id / manifest list)

| Image | Id |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:68a8b087d52d889ddb9351c5162fa4aa2bf05ad95eff3e041f66d67377c75876` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:365b77edf223c62d1c74d0db920b68baf7c75e2697852f034649d7cf3c174bd1` |

## Slice markers (local `frontend/dist` after host build)

| Marker | Present |
| --- | --- |
| `TableEditMoreActions` | yes (see `fe-slice-markers.txt`) |
| `AssetLibraryListView` | yes |
| `LegalHoldListView` | yes |
| `ApiInvocationsView` | yes |

## Notes

1. First attempt: **FAIL** — worktree `.env` created from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded); set `FRONTEND_PORT=4173` + `COMPOSE_PROJECT_NAME=documentgenerationengine`.
2. Second attempt: **FAIL** — host `mvn package` **BUILD SUCCESS**; `pnpm build` exited non-zero under the deploy script shortly after `vue-tsc`/`vite build` started (no error body captured). Standalone `pnpm -C frontend build` afterward **PASS** (exit 0).
3. Third attempt: **DEPLOY_OK** — full queued deploy from feature worktree; backend/frontend images rebuilt; containers recreated; stack healthy on :8080/:4173.
4. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged.
5. E2E **not** run in this stage (handoff to `e2e-test-engineer` against http://localhost:4173).
6. No merge. Task **#162** not marked Done.

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json` / `compose-ls.txt`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt` / `images.txt`
- `backend-runtime.txt` / `frontend-runtime.txt`
- `backend-imagetools.txt` / `frontend-imagetools.txt`
- `fe-slice-markers.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt` / `git-status-sb.txt` / `git-log-1.txt`

### Next

Stage 6 `e2e-test-engineer` against http://localhost:4173 (PQH N22 BDD-PQH-N22-001…014).
