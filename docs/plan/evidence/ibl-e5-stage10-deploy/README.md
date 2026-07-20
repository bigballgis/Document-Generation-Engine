# IBL-E5 Stage 10 — Full queued deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-20T08:59:52+08:00
- **Slice:** ibl-e5-effectivefrom-bulk-repin
- **Worktree:** D:/working/DGE-ibl-e5-effectivefrom-bulk-repin
- **Branch:** feat/ibl-e5-effectivefrom-bulk-repin @ d9f02036
- **Git tip:** `d9f02036f81622090ef5cb1742996b405ff13d22`
- **Mode:** full (docker-deploy-queue.ps1 host package + image build + recreate)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "IBL-E5 stage10 full deploy"`
- **frontend_ui_in_scope:** false (healthz sufficient)
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images:**
  - backend: `sha256:eb8f23a15ea10f4607022647ef86168beaaeaf7f156c2eec215abf0d47adc3ae`
  - frontend: `sha256:2e7700e777a71ff5a62a1ce712988fd82a742e380a0988817fd3832edb0dd162`
- **8080/healthz:** 200 {"status":"UP"}
- **4173:** 200 (optional capture)
- **Containers:** see docker-ps.txt / compose-ps.txt
- **DEPLOY_QUEUE:** see queue-status.txt

## Artifacts

- `digests.txt` / `images.txt`
- `docker-ps.txt` / `compose-ps.txt` / `compose-ls.txt`
- `healthz-8080.json` / `healthz-8080.status.txt` / `frontend-4173.txt`
- `queue-status.txt`
- `latest-summary.json`
