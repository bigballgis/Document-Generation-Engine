# IBL-D5 Stage 10 — SkipBuild health re-check evidence

- **Result:** DEPLOY_OK
- **Verified:** 2026-07-19T19:15:35+08:00
- **Slice:** ibl-d5-legalhold-depth / IBL-D5
- **Worktree:** D:/working/DGE-ibl-d5-legalhold-depth
- **Branch:** feat/ibl-d5-legalhold-depth @ 23bd6da9
- **Git tip:** `23bd6da9dd90138a494b20343a057d2891e418e0`
- **Mode:** `-SkipBuild` health re-check (stack already current; no restart)
- **Command attempted:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild`
- **SkipBuild note:** queue acquired then refused (BDD-OPS-JWT-SECRET-001) — worktree auto-created `.env` from `.env.example` with insecure default. Did **not** restart stack.
- **Health path:** single acceptance stack already up from prior IBL-D2 ForceRebuild (`working_dir` label: `D:\working\DGE-ibl-d2-lo-mandatory-lane`); re-probed 8080/4173 + compose health.
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images (unchanged):**
  - backend: `sha256:de73ac7f08e8e2b635bbe152f6805919302875be42edb6ebd40dbbda55e74a25`
  - frontend: `sha256:d7657314570d1bbe138549d865724129b064c2c2e884c95c641da009bdc2ab5b`
- **8080/healthz:** 200 `{"status":"UP"}` (see `healthz-8080.json`)
- **4173:** 200 (see `frontend-4173.txt`)
- **Containers:** see `compose-ps.txt` (all services healthy)
- **FE UI in scope:** false (Stage 10 acceptance-surface health only)
- **Done closeout:** not claimed (Stage 10 evidence only)

## Artifacts

- `skipbuild-queue.log`
- `digests.txt`
- `compose-ps.txt`
- `healthz-8080.json`
- `frontend-4173.txt`
- `latest-summary.json`
