# IBL-E2 Stage 10 - full queued deploy evidence

- **Result:** DEPLOY_OK
- **Verified:** 2026-07-20T03:50:24+08:00
- **Slice:** ibl-e2-jurisdiction-rule-engine / task #129
- **Worktree:** D:/working/DGE-ibl-e2-jurisdiction-rule-engine
- **Branch:** feat/ibl-e2-jurisdiction-rule-engine @ 0a2ee56a
- **Git tip (HEAD):** `0a2ee56a624370a133b481eb44d949a5479b3f2e`
- **Working tree note:** Stage 10 packaged uncommitted IBL-E2 WIP including Flyway `V70__composition_inclusion_rules.sql` + composition inclusion / runtime changes after BE Critical fix (not yet committed on branch tip).
- **Mode:** full build (host `mvn package` + `pnpm build` + compose image package + recreate) via `docker-deploy-queue.ps1` (no `-SkipBuild`)
- **Command:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -Reason "IBL-E2/#129 stage10 full redeploy after BE Critical tip=0a2ee56a COMPOSE_PROJECT_NAME=documentgenerationengine"`
- **Compose project:** `documentgenerationengine` (single acceptance stack; no second project / port offsets)
- **Images:**
  - `documentgenerationengine-docgen-backend:latest` - `sha256:1d2285168ff5e910c00dcbea0719c6635b3d760200d312bd944f281cc2557c32`
  - `documentgenerationengine-docgen-frontend:latest` - `sha256:bb0d4547ba990a73f8fac900877e89f5ce1a13134eff4bc51dfc002b483b3d3f`
- **8080/healthz:** 200 (see `healthz-8080.json`)
- **4173:** 200 (captured for stack completeness; `frontend_ui_in_scope=false`)
- **Containers:** see `compose-ps.txt` / `docker-ps.txt` (backend/frontend healthy; deps healthy)
- **DEPLOY_QUEUE:** idle after release (see `queue-status.txt`)
- **First attempt:** FAIL - default compose project from worktree directory collided with existing `docgen-*` container names. Remediation: retry with canonical `COMPOSE_PROJECT_NAME=documentgenerationengine`.
- **FE UI in scope:** false - healthz + stack up sufficient for this Stage 10
- **Task #129 status (post stage 12):** **Done** on MAIN — merge `81a1ca29` / feature tip `6a96e9ab`; this folder remains Stage 10 deploy evidence only.

## Artifacts

- `deploy-queue.log`
- `digests.txt`
- `images.txt`
- `compose-ps.txt`
- `docker-ps.txt`
- `compose-ls.txt`
- `queue-status.txt`
- `healthz-8080.json`
- `healthz-status.txt`
- `frontend-4173.txt`
- `latest-summary.json`
