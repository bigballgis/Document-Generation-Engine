# IBL-C3 Stage 10 — ForceRebuild deploy evidence

- **Result:** DEPLOY_OK
- **Started (successful attempt):** 2026-07-19T14:24:56+08:00
- **Ended:** 2026-07-19T14:30:04+08:00
- **Verified:** 2026-07-19T14:30:04+08:00
- **Slice:** ibl-c3-cross-locale-golden / task #122
- **Worktree:** D:/working/DGE-ibl-c3-cross-locale-golden
- **Branch:** feat/ibl-c3-cross-locale-golden @ 72ae2a4d
- **Git tip:** `72ae2a4d135eeb00d6d662308c84fcd51613447b`
- **Mode:** `-ForceRebuild` (host `mvn package` + `pnpm build` + compose image package + recreate)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -ForceRebuild -Reason "IBL-C3 #122 stage10 deploy evidence"`
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images:**
  - backend: `sha256:93570b3187ba98e56be0024c89d9d22f0cfb56fa85ffdd01c88c0fd894446791`
  - frontend: `sha256:d165a5a0bf75e8e42ed1628b3538bc15b5f133b3a0ccc05aac058b5f40f66b3f`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **Containers:** `docgen-backend` healthy · `docgen-frontend` healthy (see `compose-ps.txt`)
- **First attempt:** FAIL — worktree `.env` was created from `.env.example` with insecure JWT default; deploy script refused per BDD-OPS-JWT-SECRET-001. Remediation: synced acceptance `JWT_SECRET` from MAIN `.env` (length 64; value not recorded). Retry succeeded.
- **FE UI in scope:** false (no Stage 5 E2E prep); Stage 10 for acceptance surface / docker-only validation only
- **DEPLOY_QUEUE:** idle after release
- **Not Done:** Stage 10 evidence only — do not claim Done

## Ready for

Stage **11** — `integration-merger` (merge + worktree cleanup) then MAIN doc-sync / commit-review
