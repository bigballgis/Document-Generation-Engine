# pqh-f8-format-date-tz Stage 10 — deploy evidence

- **Result:** DEPLOY_OK
- **Verified:** 2026-07-23T10:44:04+08:00
- **Slice:** pqh-f8-format-date-tz / tasks #159 #160
- **Worktree:** D:/working/DGE-pqh-f8-format-date-tz
- **Branch:** feat/pqh-f8-format-date-tz @ ee0893fe
- **Git tip:** `ee0893fe485dda675e7ee50e97d232dbafbb7604`
- **Mode:** full queued deploy (host package + compose image build + recreate)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "stage10 pqh-f8-format-date-tz deploy evidence"`
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images:**
  - backend: `sha256:6d720d37c11d1feef2c51bcddd88ce0b26badd2200c674cf83f337a5ab8a0fc0`
  - frontend: `sha256:bb332937bebc8497ff8be6139313b4aabfc34322a79b03f0459af41b80be86f7`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **Containers:** see `compose-ps.txt` (backend/frontend healthy)
- **First attempt:** FAIL — worktree `.env` from `.env.example` had insecure JWT default (BDD-OPS-JWT-SECRET-001). Remediation: synced non-default `JWT_SECRET` from MAIN `.env` (len=64; value not recorded); set `FRONTEND_PORT=4173` + `COMPOSE_PROJECT_NAME=documentgenerationengine`. Retry succeeded.
- **FE UI in scope:** false (stages 5–7 skipped); Stage 10 for acceptance-current backend image after FORMAT_DATE zoneId/UTC compute-path change
- **Gate precondition:** `mvn -B -ntp -f backend/pom.xml verify` GREEN (2414/0/0/15)
- **DEPLOY_QUEUE after:** `DEPLOY_QUEUE: idle (no lock)` / Pending ticket files: 0
- **Not Done:** Stage 11 merge / Stage 12–13 doc-sync+commit not in this agent scope

## Closed

Stage **10** only — do not claim Done from this evidence file.
