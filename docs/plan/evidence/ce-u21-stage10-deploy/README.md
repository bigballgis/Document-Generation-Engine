# CE-U21 Stage 10 — queued deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-17T08:44:22+08:00
- **Slice:** ce-u21-draft-anchor-concurrency / task #95
- **Worktree:** D:/working/DGE-ce-u21-draft-anchor-concurrency
- **Branch:** feat/ce-u21-draft-anchor-concurrency @ 701b2b94 (working tree dirty; product source unchanged since Stage 5)
- **Drift check:** HEAD unchanged vs Stage 5; post-Stage5 file mtimes limited to evidence/E2E specs/helpers — **no product runtime source drift** → -SkipBuild path
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild -Reason "CE-U21 Stage 10 deploy evidence #95 SkipBuild (tip unchanged since stage5)"`
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images (post-recreate; layers CACHED from Stage 5 content):**
  - `documentgenerationengine-docgen-backend:latest` → `sha256:08c9f98ef82cacb0b79a66809272e153b499bdf612cd0227f6f22e56a13578e5`
  - `documentgenerationengine-docgen-frontend:latest` → `sha256:4757787c4e75017bdc79a81aacc4f48cb75b720d24d75a6dc3bfabe7fdaef503`
- **Stage 5 image IDs (reference):** backend `sha256:08c2132d…2165` / frontend `sha256:fc5b074e…90f9`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** 200
- **U21 FE markers:** ControlledStructuredContentEditor-Duuk5yFe.css, ControlledStructuredContentEditor-sZqPC9LX.js, TemplateDevVersionEditorView-IcTLaRNX.css, TemplateDevVersionEditorView-nxHgwOp-.js, index-BDUeFUXj.css, index-C1o2l6Gl.js, zh-CN-CJPwySzZ.js
- **DEPLOY_QUEUE:** DEPLOY_QUEUE: idle (no lock)
Pending ticket files: 0
- **Artifacts:** `latest-summary.json`, `compose-ps.txt`, `images.txt`, `u21-fe-markers.txt`

## Ready for

Stage **11** — `integration-merger` (merge worktree → main + cleanup), then MAIN post-task doc-sync / commit-review.
