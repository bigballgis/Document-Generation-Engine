# CE-U15 Stage 5 — E2E stack prep evidence

- **When:** 2026-07-17T02:37:35+08:00
- **Slice:** ce-u15-lifecycle-stepper / task #91
- **Worktree:** D:/working/DGE-ce-u15-lifecycle-stepper
- **Branch:** feat/ce-u15-lifecycle-stepper
- **Command:** `.\scripts\docker-deploy-queue.ps1 -Reason "CE-U15 stage5 E2E stack prep #91"` (full build) with `FRONTEND_PORT=4173` / `COMPOSE_PROJECT_NAME=documentgenerationengine`
- **Project:** `documentgenerationengine`
- **Images:** `documentgenerationengine-docgen-backend:latest`, `documentgenerationengine-docgen-frontend:latest`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **DEPLOY_QUEUE:** idle after release
- **Canonical evidence dir:** `docs/plan/evidence/ce-u15-stage5-deploy/`
