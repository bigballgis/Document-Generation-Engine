# IBL-B4 / #116 Stage 10 deploy evidence

- **Status:** DEPLOY_OK
- **Tip:** `d6b389d1`
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -ForceRebuild`
- **healthz:** HTTP 200 `{"status":"UP"}`
- **UI:** http://localhost:4173 → 200 (frontend_ui_in_scope=false; E2E N/A)
- **LO smoke:** SKIPPED — Docker has soffice; running container rootfs read-only blocks inject+convert; host soffice absent; no Word baselines invented
- **Artifacts:** `latest-deploy.md`, `latest-deploy.json`, `lo-smoke-notes.txt`, `compose-ps.txt`
