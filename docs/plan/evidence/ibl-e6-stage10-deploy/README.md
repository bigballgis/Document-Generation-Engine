# IBL-E6 Stage 10 — Full queued deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-20T10:17:14+08:00
- **Slice:** ibl-e6-clause-nesting-governance
- **Worktree:** D:/working/DGE-ibl-e6-clause-nesting-governance
- **Branch:** feat/ibl-e6-clause-nesting-governance @ 65613c4b
- **Git tip:** `65613c4be12e55ebfbc439078044ae436aa32c28`
- **Mode:** full (docker-deploy-queue.ps1 host package + image build + recreate)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "IBL-E6 Stage 10 deploy evidence"`
- **frontend_ui_in_scope:** false (healthz sufficient)
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images:**
  - backend: `sha256:1f6caae7f638b92b64d3e5cc68418340ac1d9636f5d4fbb60c4bd2bdd13ef192`
  - frontend: `sha256:a1ca7f9542feeba90aa64b87881238c8f87b392703846ee126539d523a3eeee7`
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
