# k6 load suite (IBL-D3 / F22)

| Field | Value |
| --- | --- |
| **Program leaf** | **IBL-D3** (`ibl-d3-k6-nfr-path`) / Task Master **#125** |
| **Finding** | **F22** — industry load tool + NFR confirmation path |
| **Tool** | [k6](https://k6.io/) (Grafana Labs) — OSS load tool; install via company-approved channel when required |
| **Architecture** | [k6-nfr-confirmation-path.md](../../docs/architecture/k6-nfr-confirmation-path.md) |
| **NFR sink** | [non-functional-requirements.md](../../docs/requirements/non-functional-requirements.md) § LR-D5 / 待确认 |
| **Evidence** | [docs/plan/evidence/ibl-d3-k6-nfr-path/](../../docs/plan/evidence/ibl-d3-k6-nfr-path/) |

## Honesty (non-negotiable)

- Results feed NFR §待确认 as **measured-input / proposed** only.
- **Do not** promote any latency / error-rate / concurrency number to a **confirmed** SLO from a green k6 run.
- Soft `thresholds` in scripts are **smoke usability gates**, not product SLOs.
- Prefer **acceptance Docker stack** (`:8080` / `:4173`) — never shared/production.
- Smoke scripts must not destroy data (prefer `/healthz` or other safe public endpoints).

## Scripts

| Script | Purpose | Mutates data? |
| --- | --- | --- |
| [`smoke-healthz.js`](./smoke-healthz.js) | 2 VU × 15s against `GET /healthz` | **No** |

Heavier generation/capacity scenarios remain historical under LR-D6 / IBL-B2 JUnit harness — do **not** invent confirmed concurrent-PDF SLOs from either path.

## Prerequisites

1. Acceptance stack healthy: `GET http://localhost:8080/healthz` → `{"status":"UP"}`  
   Deploy via `.\scripts\docker-deploy-queue.ps1` (single host; ports **8080** / **4173**).
2. k6 on `PATH`, **or** Docker image `grafana/k6` (see runner).

### Install k6 (host)

```powershell
# Windows (winget — preferred when available)
winget install --id GrafanaLabs.k6 -e

# Verify
k6 version
```

```bash
# Linux / macOS examples (use company-approved package channel when required)
# https://grafana.com/docs/k6/latest/set-up/install-k6/
k6 version
```

### Docker (when host binary unavailable)

```powershell
docker run --rm -i grafana/k6:latest version
```

On Docker Desktop (Windows/macOS), target the host stack with
`BASE_URL=http://host.docker.internal:8080`.

## How to run (acceptance `:8080`)

From **repository root** (worktree or MAIN after merge):

```powershell
# Wrapper (resolves host k6 or docker fallback; writes evidence under docs/plan/evidence/…)
.\scripts\k6-smoke.ps1

# Or direct host k6
k6 run -e BASE_URL=http://localhost:8080 `
  --summary-export docs/plan/evidence/ibl-d3-k6-nfr-path/k6-summary-export.json `
  perf/k6/smoke-healthz.js
```

```bash
pwsh ./scripts/k6-smoke.ps1
# or
k6 run -e BASE_URL=http://localhost:8080 \
  --summary-export docs/plan/evidence/ibl-d3-k6-nfr-path/k6-summary-export.json \
  perf/k6/smoke-healthz.js
```

Dry-run / presence check when k6 cannot execute (network / policy / missing binary):

```powershell
.\scripts\k6-smoke.ps1 -DryRun
```

## Feeding NFR §待确认

1. Run smoke (or record honest dry-run if k6 unavailable).
2. Copy/update summary under [docs/plan/evidence/ibl-d3-k6-nfr-path/](../../docs/plan/evidence/ibl-d3-k6-nfr-path/).
3. Reference the evidence from LR-D5 as **measured-input** only — leave proposal table status as **proposed — awaiting confirmation**.
4. User/PRD confirmation of any number is a **separate** governance act.

## Out of scope here

- Inventing confirmed SLOs; flipping checklist **#3b/#5a GO**; go-live; Wave D Done; IBL-D4 chaos; IBL-D5 legalhold; Word/pixel baselines.
