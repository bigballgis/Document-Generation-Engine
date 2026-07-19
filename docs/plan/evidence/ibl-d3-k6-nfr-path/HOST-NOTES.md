# Host notes — k6 install / execute (IBL-D3)

Acceptance stack on this machine answered `GET http://localhost:8080/healthz` → **200** during Stage 4.
Industry tool **k6** could not be executed here yet; suite + runner are checked in for re-run when the binary or image is available.

## Install options

### A. winget (Windows)

```powershell
winget install --id GrafanaLabs.k6 -e --accept-package-agreements --accept-source-agreements
# Refresh PATH / new shell
k6 version
.\scripts\k6-smoke.ps1
```

Observed 2026-07-19: MSI download verified; interactive install returned **1602** (cancelled) — retry with elevated/approved MSI if policy requires.

### B. Docker image

```powershell
docker pull grafana/k6:latest
.\scripts\k6-smoke.ps1
# Desktop maps localhost → host.docker.internal inside the runner
```

Observed 2026-07-19: Docker Hub pull timed out from this host.

### C. Portable binary (gitignored)

Place `k6.exe` under `.tools/k6/` (already gitignored). The runner auto-discovers it.

```powershell
# After placing k6.exe under .tools/k6/...
.\scripts\k6-smoke.ps1
```

## Dry-run (presence)

```powershell
.\scripts\k6-smoke.ps1 -DryRun
```

Records script presence + healthz preflight without inventing load metrics.
