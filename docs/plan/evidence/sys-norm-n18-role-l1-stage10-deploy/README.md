# sys-norm-n18-role-l1 Stage 10 — SkipBuild deploy evidence

- **Result:** DEPLOY_OK
- **Verified:** 2026-07-22T18:14:45+08:00
- **Slice:** sys-norm-n18-role-l1
- **task_ids:** 157, 158
- **Worktree:** D:/working/DGE-sys-norm-n18-role-l1
- **Branch:** feat/sys-norm-n18-role-l1 @ `c4af526d`
- **Mode:** `-SkipBuild` (queued health recheck; layers CACHED from Stage 5; containers recreated)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "stage10-sys-norm-n18-role-l1-deploy-evidence COMPOSE_PROJECT_NAME=documentgenerationengine"`
- **Pre-check:** Stage 5 stack already healthy on :8080/:4173; source mtimes ≤ Stage 5 image Created (2026-07-22T09:50Z); queue idle
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images (after SkipBuild recreate):**
  - backend: `sha256:49e7d4d5c26183558a65d2ff2dad6f78b9b942a59417d68bff975de0bc81eae0`
  - frontend: `sha256:5f8a1496c2a658bfdd43a2d439c541a884f00e32d4f514c1acfc0f8f3c85da0d`
- **8080/healthz:** 200 `{"status":"UP"}` (see `healthz-8080.json`)
- **4173:** 200 (see `frontend-4173.txt`)
- **Containers:** see `docker-ps.txt` (all services healthy)
- **DEPLOY_QUEUE:** idle after release
- **Notes:**
  1. First attempt without `COMPOSE_PROJECT_NAME` FAILED — directory-derived project `dge-sys-norm-n18-role-l1` collided with fixed `/docgen-minio` name; orphan network/volumes removed.
  2. Retry with canonical `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` → SUCCESS; app containers recreated; health green; queue lock released.
  3. No ForceRebuild; Docker build layers CACHED (Stage 5 artifacts still current).

## Artifacts

- `digests.txt`
- `docker-ps.txt`
- `queue-status.txt`
- `healthz-8080.json`
- `frontend-4173.txt`
- `latest-summary.json`
