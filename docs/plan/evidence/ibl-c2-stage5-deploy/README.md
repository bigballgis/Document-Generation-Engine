# IBL-C2 Stage 5 — E2E stack prep evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-19T12:47:11+08:00
- **Slice:** ibl-c2-rendered-compare-ui / task #121
- **Worktree:** D:/working/DGE-ibl-c2-rendered-compare-ui
- **Branch:** feat/ibl-c2-rendered-compare-ui @ 1c1ea8cf
- **Git tip:** `1c1ea8cf017d3dd746b8392758bc54df8fc63642`
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "IBL-C2 stage5 E2E stack prep feat/ibl-c2-rendered-compare-ui@1c1ea8cf"` (full build from feature worktree)
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images:**
  - `documentgenerationengine-docgen-backend:latest` → `sha256:5c0742027be229ca3dbea7bbd2a4a19db6020011167217da12c074ec4f6f4d12`
  - `documentgenerationengine-docgen-frontend:latest` → `sha256:ae8305d1f9a1a17024f61573d779af2e08d064085dcc41b4ac962ec4bbd9ca49`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **Containers:** backend/frontend healthy; infra (postgres/redis/minio/kafka) healthy
- **DEPLOY_QUEUE:** idle (no lock) | Pending ticket files: 0
- **Stage 10 SkipBuild:** YES — if tip remains `1c1ea8cf` / `1c1ea8cf017d3dd746b8392758bc54df8fc63642` and images above still match running containers
- **Notes:** First attempt failed (`.env` from example → insecure JWT_SECRET). Synced JWT_SECRET from MAIN. Second attempt without COMPOSE_PROJECT_NAME hit `/docgen-minio` name conflict; retried with canonical project. FE-only slice; host `mvn package -Dmaven.test.skip` + `pnpm build` ran as part of docker-deploy (BE verify not re-run). Stage 4 FE gates already green.

## Ready for

Stage **6** — `e2e-test-engineer` (IBL-C2 rendered compare UI @ :4173 / :8080)

## Stage 10 SkipBuild (appended)

- **Result:** DEPLOY_OK
- **When:** 2026-07-19T13:40:58+08:00 (started 2026-07-19T13:38:44+08:00 / ended 2026-07-19T13:40:37+08:00)
- **Evidence:** [ibl-c2-stage10-deploy](../ibl-c2-stage10-deploy/)
- **8080/healthz:** 200
- **4173:** 200
