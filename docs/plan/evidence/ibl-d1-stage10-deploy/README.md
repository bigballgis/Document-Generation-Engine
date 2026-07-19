# IBL-D1 Stage 10 — ForceRebuild deploy evidence

- **Result:** DEPLOY_OK
- **Started (successful attempt):** 2026-07-19T15:35:07+08:00
- **Ended / Verified:** 2026-07-19T15:39:16+08:00
- **Slice:** ibl-d1-testcontainers-flyway / task #123
- **Worktree:** D:/working/DGE-ibl-d1-testcontainers-flyway
- **Branch:** feat/ibl-d1-testcontainers-flyway @ 97488945
- **Git tip:** `97488945f69aab18bdba3d23993503e63438da66`
- **Mode:** `-ForceRebuild` (host `mvn package` + `pnpm build` + compose image package + recreate)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -ForceRebuild -Reason "IBL-D1 stage10 deploy evidence #123"`
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images:**
  - backend: `sha256:783e4af53842cee37c8c4259e6ab7fe21c314a68adfef44605526590adde874a`
  - frontend: `sha256:75586aaafc956b96d5b9c3813632728f45c66ed229099e487a25c505f5e3b710`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **Containers:** see `compose-ps.txt` (backend/frontend healthy expected)
- **First attempt:** FAIL — worktree `.env` created from `.env.example` with insecure JWT default; deploy script refused per BDD-OPS-JWT-SECRET-001. Remediation: synced acceptance `JWT_SECRET` from MAIN `.env` (length 64; value not recorded). Retry succeeded.
- **FE UI in scope:** false (FE/E2E N/A); Stage 10 for docker-only-validation / acceptance surface only
- **DEPLOY_QUEUE after:** 
- **Not Done:** Stage 10 evidence only — do not claim Done

## Ready for

Stage **11** — `integration-merger` (merge + worktree cleanup) then MAIN doc-sync / commit-review
