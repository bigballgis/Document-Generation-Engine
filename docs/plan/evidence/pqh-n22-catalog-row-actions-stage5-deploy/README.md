# Stage 5 deploy evidence — PQH Leaf 3 N22 catalog row actions (TM #162)

| Field | Value |
| --- | --- |
| **Result** | **DEPLOY_OK** |
| **task_ids** | `["162"]` |
| **worktree** | `D:/working/DGE-pqh-n22-catalog-row-actions` |
| **branch** | `feat/pqh-n22-catalog-row-actions` |
| **deploy_tip_at_start** | `8757f5ec` (FE gates Done; N22 product UI) |
| **evidence_tip** | `f69c3f0c` (includes subsequent E2E spec commit; does not change packaged FE dist) |
| **local_time** | 2026-07-23T19:09+08:00 |
| **command** | `.\scripts\docker-deploy-queue.ps1 -Reason "Stage5 PQH N22 #162 E2E stack prep tip 8757f5ec"` |
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
| `documentgenerationengine-docgen-backend:latest` | `sha256:ac50ebecdf42191b7f975d61b307ddeed7281785a2c72ed017ba918971c2b454` |
| `documentgenerationengine-docgen-frontend:latest` | `sha256:fec2e9a139da5b7dc816899d4c575a533be83e5402ce95fca533b0e1ceaf3668` |

## Slice markers (local `frontend/dist` after host build)

| Marker | Present |
| --- | --- |
| `TableEditMoreActions` | yes (see `fe-slice-markers.txt`) |
| `AssetLibraryListView` | yes |
| `LegalHoldListView` | yes |
| `ApiInvocationsView` | yes |

## Notes

1. Full queued deploy from feature worktree after Stage 4 tip `8757f5ec`; JWT non-default already present (len=64); `FRONTEND_PORT=4173`; `COMPOSE_PROJECT_NAME=documentgenerationengine`.
2. Host `mvn package` + `pnpm build` + image package + container recreate — **DEPLOY_OK**.
3. During evidence capture, tip advanced to `f69c3f0c` (E2E journeys + prior Stage 5 artifacts). Product FE in the running image was built from the worktree dist at deploy time (N22 markers present).
4. KAFKA_IMAGE warning (bitnamilegacy) — local/dev only; unchanged.
5. E2E **not** run by build-deploy-agent in this stage (handoff to `e2e-test-engineer` against http://localhost:4173).
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
