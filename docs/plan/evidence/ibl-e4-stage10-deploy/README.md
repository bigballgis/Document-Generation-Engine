# IBL-E4 Stage 10 — SkipBuild deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-20T08:12:06+08:00
- **Slice:** ibl-e4-entity-document-brands
- **Worktree:** D:/working/DGE-ibl-e4-entity-document-brands
- **Branch:** feat/ibl-e4-entity-document-brands @ 3bd2cd87
- **Git tip:** `3bd2cd87b78341dc28f3918603f9c8cdbc1f23e8`
- **Mode:** `-SkipBuild` (queue host package + recreate; FE/BE Docker COPY layers CACHED)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "IBL-E4 stage10 SkipBuild health evidence (no FE/BE change after Stage5)"`
- **SkipBuild rationale:** No `backend/src` or `frontend/src` changes after Stage 5 (`2026-07-20T07:47:09+08:00`). Post-Stage5 FE touchpoints are e2e evidence/spec/helpers only. Packaging confirmed `COPY jar` / `COPY dist` **CACHED**. Stage 5 image IDs were backend `sha256:7a6c9eaf…` / frontend `sha256:4bfee769…`; SkipBuild re-export produced new attestation manifest IDs with cached layers.
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images (after SkipBuild recreate):**
  - backend: `sha256:3532a8ecb5c83a0451e0ce14a57af5431fa0c275a1d26ecc55565bbd69d24cb1`
  - frontend: `sha256:31bee0f6e3e2bc4571cc3e22f296bf2477ec8f69f50c17f87b6c9afc07f1d66d`
- **8080/healthz:** 200 {"status":"UP"}
- **4173:** 200
- **Containers:** see docker-ps.txt / compose-ps.txt
- **DEPLOY_QUEUE:** see queue-status.txt
- **Stage 12:** Task #131 → **Done** (MAIN merge `4d810395` / tip `212c6be9`; sole-active cleared).

## Artifacts

- `digests.txt` / `images.txt`
- `docker-ps.txt` / `compose-ps.txt` / `compose-ls.txt`
- `healthz-8080.json` / `healthz-8080.status.txt` / `frontend-4173.txt`
- `queue-status.txt`
- `latest-summary.json`