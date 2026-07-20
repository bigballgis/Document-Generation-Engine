# IBL-E6 Stage 10 — Full queued deploy evidence

- **Result:** DEPLOY_OK
- **When:** 2026-07-20T10:24:04+08:00
- **Slice:** ibl-e6-clause-nesting-governance
- **Worktree (deploy source):** D:/working/DGE-ibl-e6-clause-nesting-governance
- **Branch tip used:** feat/ibl-e6-clause-nesting-governance @ 0e542c03
- **Git tip:** `0e542c03f10b7b47afab392d0378ce0eab301c0b`
- **Mode:** full (docker-deploy-queue.ps1 host package + image build + recreate) — NOT -SkipBuild
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "IBL-E6 stage10 full deploy tip 0e542c03"`
- **frontend_ui_in_scope:** false (healthz sufficient)
- **Compose project:** `documentgenerationengine` (single acceptance stack; QUEUE_ONLY)
- **Images:**
  - backend: `sha256:02245661190b430b1dd7c440d666d3efac5edf86c8294c62883dff20da3e6516`
  - frontend: `sha256:3a81dc62b91a494d6607ab1391dd8dfe82351ae4e5db9bc0c662084d32188f80`
- **8080/healthz:** 200 `{"status":"UP"}`
- **4173:** optional capture (see `frontend-4173.txt`)
- **Flyway V73:** applied — `flyway_schema_history` version `73` / `content module nesting edge` / success=t (installed_on 2026-07-20 02:16:01Z); backend logs: validated 73 migrations, current schema version 73
- **Containers:** see docker-ps.txt / compose-ps.txt
- **DEPLOY_QUEUE:** idle after deploy (see queue-status.txt)

## Notes

1. First queue attempt without `COMPOSE_PROJECT_NAME` failed with fixed-name conflict (`docgen-minio`); lock released; retried with canonical project name (same pattern as prior stage10 capture).
2. Feature worktree path was removed after deploy (no longer present at evidence refresh time); tip `0e542c03` is an ancestor of MAIN `dcc42c81` (merge of feat/ibl-e6-clause-nesting-governance) — stage 11 merge already present on MAIN.

## Artifacts

- `digests.txt` / `images.txt`
- `docker-ps.txt` / `compose-ps.txt` / `compose-ls.txt`
- `healthz-8080.json` / `healthz-8080.status.txt` / `frontend-4173.txt`
- `flyway-v73.txt`
- `queue-status.txt`
- `latest-summary.json`
