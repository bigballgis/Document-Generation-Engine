# IBL-E1 Stage 10 — SkipBuild deploy evidence

- **Result:** DEPLOY_OK
- **Verified:** 2026-07-20T02:15:24+08:00
- **Slice:** ibl-e1-locale-variant-model / task #128
- **Worktree:** D:/working/DGE-ibl-e1-locale-variant-model
- **Branch:** feat/ibl-e1-locale-variant-model @ `3e76b9db`
- **Git tip:** `3e76b9db7c178ba0a99a3e1d01ff65bf7fd0e337`
- **Mode:** `-SkipBuild` (health re-check / host re-package + recreate; not ForceRebuild)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `pwsh ./scripts/docker-deploy-queue.ps1 -SkipBuild -Reason "IBL-E1/#128 stage10 SkipBuild tip=3e76b9db COMPOSE_PROJECT_NAME=documentgenerationengine"`
- **SkipBuild rationale:** Stage 5 full deploy (2026-07-20T01:31:57+08:00) already baked API/UI. Post-Stage-5 mtime scan found **no** `frontend/src` or `backend/src/main` changes after that cutoff — only e2e helpers/specs/evidence + OpenAPI/ADR docs. Docker `COPY dist` / `COPY jar` layers were **CACHED** during SkipBuild packaging (runtime payload unchanged vs Stage 5 content).
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images (after SkipBuild recreate):**
  - `documentgenerationengine-docgen-backend:latest` — `sha256:37e0dc632976c772d47b8657c902aa27ca4f471fee769ef0984547fd7619f63d`
  - `documentgenerationengine-docgen-frontend:latest` — `sha256:3616a5a4ea1e884f006e3cdb557c7b6b29551f5541bdc5c6bae61e602e75162b`
- **Stage 5 image refs (prior full deploy):**
  - backend: `sha256:9e310f3188fa19d1a2e815359c16b4395295eb15a7d60ede8c1f74d744b8f110`
  - frontend: `sha256:86ac804fa4e3cd003bf62739098e0775ca728fce335a402db109ffffc0b69b3f`
- **8080/healthz:** 200 `{"status":"UP"}` (see `healthz-8080.json`)
- **4173:** 200 (see `frontend-4173.txt`)
- **Containers:** see `compose-ps.txt` / `docker-ps.txt` (all services healthy)
- **DEPLOY_QUEUE:** idle after release (see `queue-status.txt`)
- **Does NOT claim:** Task #128 Done (stage 10 evidence only; merge / MAIN doc-sync / commit pending).

## Artifacts

- `skipbuild-queue.log`
- `digests.txt`
- `compose-ps.txt`
- `docker-ps.txt`
- `compose-ls.txt`
- `queue-status.txt`
- `healthz-8080.json`
- `frontend-4173.txt`
- `latest-summary.json`
