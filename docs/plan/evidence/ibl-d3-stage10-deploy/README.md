# IBL-D3 Stage 10 — SkipBuild health re-check evidence

- **Result:** DEPLOY_OK
- **Verified:** 2026-07-19T17:21:54+08:00
- **Slice:** ibl-d3-k6-nfr-path / task #125
- **Worktree:** D:/working/DGE-ibl-d3-k6-nfr-path
- **Branch:** feat/ibl-d3-k6-nfr-path @ 2bfb8e40
- **Git tip:** `2bfb8e4034920c01055824606aa2d678a26bfe37`
- **Mode:** `-SkipBuild` health re-check (docs/perf leaf; stack already current)
- **Command attempted:** `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -SkipBuild`
- **SkipBuild note:** queue acquired then refused (BDD-OPS-JWT-SECRET-001) — worktree had no `.env` and auto-created from `.env.example` with insecure default. Did **not** restart stack.
- **Health path:** single acceptance stack already up from prior IBL-D2 ForceRebuild (`working_dir` label: `D:\working\DGE-ibl-d2-lo-mandatory-lane`); re-probed 8080/4173 + compose health.
- **Compose project:** `documentgenerationengine` (single acceptance stack)
- **Images (unchanged):**
  - backend: `sha256:de73ac7f08e8e2b635bbe152f6805919302875be42edb6ebd40dbbda55e74a25`
  - frontend: `sha256:d7657314570d1bbe138549d865724129b064c2c2e884c95c641da009bdc2ab5b`
- **8080/healthz:** 200 `{"status":"UP"}` (see `healthz-8080.json`)
- **4173:** 200 (see `frontend-4173.txt`)
- **Containers:** see `compose-ps.txt` (all services healthy)
- **FE UI in scope:** false (docs/perf leaf; Stage 10 acceptance-surface health only)
- **Done closeout:** not claimed — Stage 11+ remains for parent pipeline

## Artifacts

- `digests.txt`
- `compose-ps.txt`
- `healthz-8080.json`
- `frontend-4173.txt`
- `latest-summary.json`
