# CE-U20 Stage 10 — queued deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-17T07:42:52+08:00
- **Slice:** ce-u20-clause-create-structured / task #94
- **Worktree:** D:/working/DGE-ce-u20-clause-create-structured
- **Branch:** feat/ce-u20-clause-create-structured @ da7581e83624a155c4e32d1e57b35fa18ca64587 (working tree dirty=True)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "CE-U20 Stage 10 deploy evidence #94"` (FULL build from feature worktree; not -SkipBuild)
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images:**
  - `documentgenerationengine-docgen-backend:latest` → `sha256:5ba6b88db512e55226019f6f876811571be3bfedd181e9bc4f609c5178c2e4e1`
  - `documentgenerationengine-docgen-frontend:latest` → `sha256:904d275bd0d7b13e24753631283f6f31c20d6505ff465de8fa9242dd7418a289`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **U20 FE markers:**
  - /usr/share/nginx/html/assets/ControlledStructuredContentEditor-Ddpl_eZo.js
  - /usr/share/nginx/html/assets/ControlledStructuredContentEditor-Duuk5yFe.css
  - /usr/share/nginx/html/assets/index-Cl768m0s.js
  - /usr/share/nginx/html/assets/zh-CN-Djg22M9W.js
- **DEPLOY_QUEUE:** acquired → released (idle after)
- **Artifacts:** `latest-summary.json`, `compose-ps.txt`, `images.txt`, `u20-fe-markers.txt`
- **Notes:** First bare queue call without `COMPOSE_PROJECT_NAME` failed on `/docgen-minio` name conflict (directory-derived project). Retried with canonical project name matching Stage 5. Host compile + image package + backend/frontend container recreate completed; health gates green.

## Ready for

Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
