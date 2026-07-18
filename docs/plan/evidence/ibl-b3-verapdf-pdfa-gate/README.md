# IBL-B3 / #115 — Stage 10 deploy evidence

Queued Docker acceptance deploy for tip `c81054b0` (veraPDF PDF/A-2b machine gate / test-scope tooling).

| Item | Value |
| --- | --- |
| Latest | [latest-deploy.md](./latest-deploy.md) / [latest-deploy.json](./latest-deploy.json) |
| Status | `DEPLOY_OK` |
| healthz | `{"status":"UP"}` @ `:8080` |
| UI publish | host `:5173` via `.env` `FRONTEND_PORT` (compose `${FRONTEND_PORT:-4173}`) |
| E2E | N/A (`frontend_ui_in_scope=false`) |
| Go-live | not claimed |
| Word baselines | not invented |
