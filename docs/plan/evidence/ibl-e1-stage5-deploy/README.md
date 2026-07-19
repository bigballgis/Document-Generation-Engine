# IBL-E1 Stage 5 — E2E stack prep evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-20T01:31:57+08:00
- **Slice:** ibl-e1-locale-variant-model / task #128
- **Worktree:** D:/working/DGE-ibl-e1-locale-variant-model
- **Branch:** feat/ibl-e1-locale-variant-model @ 3e76b9db
- **Git tip:** `3e76b9db7c178ba0a99a3e1d01ff65bf7fd0e337`
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "IBL-E1/#128 stage5 E2E tip=3e76b9db COMPOSE_PROJECT_NAME=documentgenerationengine"` (full build from feature worktree; not -SkipBuild)
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images:**
  - `documentgenerationengine-docgen-backend:latest` → `sha256:9e310f3188fa19d1a2e815359c16b4395295eb15a7d60ede8c1f74d744b8f110`
  - `documentgenerationengine-docgen-frontend:latest` → `sha256:86ac804fa4e3cd003bf62739098e0775ca728fce335a402db109ffffc0b69b3f`
- **8080/healthz:** 200 {"status":"UP"}
- **4173:** 200
- **Containers:** see docker-ps.txt / compose-ps.txt
- **DEPLOY_QUEUE:** see queue-status.txt
- **JWT_SECRET:** synced from MAIN (len=64, non-insecure; value not recorded)
- **Notes:**
  - Attempt 1: FAIL — worktree `.env` from `.env.example` insecure JWT default (BDD-OPS-JWT-SECRET-001).
  - Attempt 2: FAIL — default compose project name collided with `/docgen-minio`; orphan `dge-ibl-e1-locale-variant-model_*` network/volumes removed.
  - Attempt 3: SUCCESS — canonical `COMPOSE_PROJECT_NAME=documentgenerationengine`.
- **Does NOT claim:** Task #128 Done (stage 5 only; E2E/reviews/merge/doc-sync pending).

## Ready for

Stage **6** — `e2e-test-engineer` (IBL-E1 locale create/filter/family nav @ :4173 / :8080)
