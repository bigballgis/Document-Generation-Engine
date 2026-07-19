# IBL-E3 Stage 10 — SkipBuild deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-20T05:53:56+08:00
- **Slice:** ibl-e3-legal-approval-matrix / task #130
- **Worktree:** D:/working/DGE-ibl-e3-legal-approval-matrix
- **Branch:** feat/ibl-e3-legal-approval-matrix @ e66a3bbf
- **Git tip:** `e66a3bbfde70d1e6be0456524403172cd413b969`
- **Mode:** `-SkipBuild` (queue host package + recreate; FE/BE Docker COPY layers CACHED)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "IBL-E3/#130 stage10 SkipBuild after E2E FE routeCapabilities redeploy"`
- **SkipBuild rationale:** Acceptance stack already carried latest FE with `routeCapabilities` LEGAL_REVIEWER fix from Stage 6 E2E redeploy (FE image CreatedAt **2026-07-20 05:39**, newer than Stage 5 `ba1ed85d…`). Packaging confirmed `COPY dist` / `COPY jar` **CACHED**.
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images (after SkipBuild recreate):**
  - backend: `sha256:eaf639db7a4ec735a631d78487b598cfd9e388dda57a3c37d369ddd409e57beb`
  - frontend: `sha256:0b7debb4df1af52153279cf914c596867233614dada577e2f98217c7fa435076`
- **8080/healthz:** 200 {"status":"UP"}
- **4173:** 200
- **Containers:** see docker-ps.txt / compose-ps.txt
- **DEPLOY_QUEUE:** see queue-status.txt
- **UIUX:** `frontend/e2e/evidence/IBL-E3-uiux-manifest.md` — Verdict PASS, Critical=0, merge_go=true, Stage 6 4/4
- **Does NOT claim:** Task #130 Done (stage 10 evidence only; merge/doc-sync pending).

## Artifacts

- `skipbuild-queue.log`
- `digests.txt` / `images.txt`
- `docker-ps.txt` / `compose-ps.txt`
- `healthz-8080.json` / `frontend-4173.txt`
- `queue-status.txt`
- `latest-summary.json`
