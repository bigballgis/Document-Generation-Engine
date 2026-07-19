# IBL-D2 Stage 10 — ForceRebuild deploy evidence

- **Result:** DEPLOY_OK
- **Started:** 2026-07-19T16:27:06+08:00
- **Ended / Verified:** 2026-07-19T16:30:25+08:00
- **Slice:** ibl-d2-lo-mandatory-lane / task #124
- **Worktree:** D:/working/DGE-ibl-d2-lo-mandatory-lane
- **Branch:** feat/ibl-d2-lo-mandatory-lane @ c5387826
- **Git tip:** `c5387826c87492b613159012939e57cfd9e88c51`
- **Mode:** `-ForceRebuild` (host `mvn package` + `pnpm build` + compose image package + recreate)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -ForceRebuild -Reason "IBL-D2 stage10 deploy evidence #124"`
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **JWT:** synced full `.env` from MAIN before deploy (value not recorded)
- **Images:**
  - backend: `sha256:de73ac7f08e8e2b635bbe152f6805919302875be42edb6ebd40dbbda55e74a25`
  - frontend: `sha256:d7657314570d1bbe138549d865724129b064c2c2e884c95c641da009bdc2ab5b`
- **8080/healthz:** 200 (see `healthz-8080.json`)
- **4173:** 200
- **Containers:** see `compose-ps.txt` (backend/frontend healthy expected)
- **FE UI in scope:** false (FE/E2E N/A); Stage 10 for docker-only-validation / acceptance surface only
- **Done closeout:** not claimed — Stage 11+ remains for parent pipeline

## Artifacts

- `digests.txt`
- `compose-ps.txt`
- `healthz-8080.json`
- `frontend-4173.txt`
- `latest-summary.json`
