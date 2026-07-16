# CE-U14 Stage 10 — Deploy evidence (ForceRebuild)

- **Result:** DEPLOY_OK
- **When:** 2026-07-16T23:58:06+08:00
- **Slice:** ce-u14-dashboard-lifecycle-todos / task #90
- **Worktree:** D:/working/DGE-ce-u14-dashboard-lifecycle-todos
- **Branch:** feat/ce-u14-dashboard-lifecycle-todos @ `12f489de` (`12f489de104bf11c7b84db6854a36326dbe5776e`)
- **Why rebuild:** Architecture Yellow #1 — `enrichDevVersionIdsForWorkflow` no longer falls back to `content[0]`; Stage 5 images were stale.
- **Command:** `.\scripts\docker-deploy-queue.ps1 -ForceRebuild -Reason "CE-U14 stage10 deploy evidence ForceRebuild after Arch Yellow#1 FE fix"`
- **Compose project:** `documentgenerationengine`
- **FRONTEND_PORT:** `4173`
- **JWT_SECRET:** present in worktree `.env` (len=64, non-insecure; not logged)
- **Images:**
  - `documentgenerationengine-docgen-frontend:latest` → `sha256:8f01c3be4c28ccffd7d3fdbb68d8babf0b0400730911adf9329ed3bbc7023ea5` (created 2026-07-16T15:53:14Z)
  - `documentgenerationengine-docgen-backend:latest` → `sha256:5da65e945d10a9bcec5e5a4db9d4e6b04fa40ac5cbc3a6e81c5bdd7df6df54e8` (created 2026-07-16T15:10:45Z)
- **FE rebuild marker:** host `vite build` emitted `DashboardView-DIGCf4oR.js`; served at `:4173` with HTTP 200
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173/:** 200
- **4173/assets/DashboardView-DIGCf4oR.js:** 200
- **DEPLOY_QUEUE:** idle after release (no lock; pending tickets: 0)

## Compose ps (healthy)

```
NAME              IMAGE                                      STATUS                    PORTS
docgen-backend    documentgenerationengine-docgen-backend    Up (healthy)              0.0.0.0:8080->8080/tcp
docgen-frontend   documentgenerationengine-docgen-frontend   Up (healthy)              0.0.0.0:4173->8080/tcp
docgen-kafka      bitnamilegacy/kafka:3.7                    Up (healthy)              0.0.0.0:9092->9092/tcp
docgen-minio      minio/minio:RELEASE.2024-12-18T13-15-44Z   Up (healthy)              0.0.0.0:9000-9001->9000-9001/tcp
docgen-postgres   postgres:16-alpine                         Up (healthy)              0.0.0.0:5432->5432/tcp
docgen-redis      redis:7-alpine                             Up (healthy)              0.0.0.0:6379->6379/tcp
```

## Notes

- Not merge. Not Done claim. Stage 10 evidence only.
- Frontend image layers rebuilt from fresh `frontend/dist` (new `DashboardView-DIGCf4oR.js`).
