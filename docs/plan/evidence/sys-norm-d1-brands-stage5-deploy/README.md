# Stage 5 deploy evidence — SYS-NORM Wave 6 (`sys-norm-d1-brands` / TM #150)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["150"]` |
| **worktree** | `D:/working/DGE-sys-norm-d1-brands` |
| **branch** | `feat/sys-norm-d1-brands` @ `cbea637b84c18b19e9199c2d1abb668c2d5e6fd0` |
| **local_time** | 2026-07-21T16:40+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -Reason "SYS-NORM Wave 6 #150 stage5 E2E stack prep (sys-norm-d1-brands) retry after JWT remediation"` |
| **compose_project** | `documentgenerationengine` (canonical; single host :8080/:4173) |
| **queue** | idle after release; no bypass |
| **mvn verify** | **not run** (host sole-active rule; BE already GREEN 2370/0/0/15) |

## Health

| Probe | Result |
| --- | --- |
| `http://localhost:8080/healthz` | **200** `{"status":"UP"}` |
| `http://localhost:4173/` | **200** (len=1202) |
| `docgen-backend` | healthy |
| `docgen-frontend` | healthy |

## Image digests (local Id / manifest list)

| Image | Digest |
| --- | --- |
| `documentgenerationengine-docgen-backend:latest` | `sha256:aef9c6667aa150c31b9c5f34da55c485512762165a8580a5b1e8fd9825fe9929` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:816ddf72dcdec07669a7892eb03189514742a926214140674fb1651221fe7d0f` |

## Slice markers

| Marker | Result |
| --- | --- |
| Flyway `V76__retire_document_brand_legal_entity.sql` | present in worktree |
| FE dist markers | `documentBrand=true`, `SurfaceRetired=true`, `document-brand=true`, `brands=true` (see `fe-slice-markers.txt`) |

## Notes

1. First attempt: FAIL — worktree `.env` from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded); set `FRONTEND_PORT=4173` + `COMPOSE_PROJECT_NAME=documentgenerationengine`.
2. Second attempt: **DEPLOY_OK** — host `mvn package` (tests skipped) + `pnpm build` + image package; backend/frontend recreated; stack healthy.
3. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged.
4. E2E **not** run in this stage (handoff to `e2e-test-engineer` against http://localhost:4173).
5. No merge.

## Artifact files

- `latest-summary.json`
- `compose-ps.txt` / `compose-ps.json`
- `healthz.json` / `healthz-meta.txt`
- `frontend-4173.txt` / `frontend-4173-headers.txt`
- `backend-image.txt` / `frontend-image.txt` / `digests.txt`
- `backend-runtime.txt` / `frontend-runtime.txt`
- `fe-slice-markers.txt` / `v76-migration-files.txt`
- `queue-status.txt` / `git-head.txt` / `git-branch.txt`

### Next

Stage 6 `e2e-test-engineer` against http://localhost:4173 (Wave 6 brand hard-retire BDD).
