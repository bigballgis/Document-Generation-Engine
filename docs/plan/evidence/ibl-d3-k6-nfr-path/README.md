# IBL-D3 / #125 — k6 + NFR confirmation path evidence

| Field | Value |
| --- | --- |
| **Slice** | `ibl-d3-k6-nfr-path` |
| **Task Master** | **#125** |
| **Finding** | **F22** |
| **BDD** | [ibl-d3-k6-nfr-path.md](../../../behavior/ibl-d3-k6-nfr-path.md) (`not-applicable`) |
| **Architecture** | [k6-nfr-confirmation-path.md](../../../architecture/k6-nfr-confirmation-path.md) |
| **Suite** | [`perf/k6/`](../../../../perf/k6/) |
| **Runner** | [`scripts/k6-smoke.ps1`](../../../../scripts/k6-smoke.ps1) |
| **NFR sink** | [non-functional-requirements.md](../../../requirements/non-functional-requirements.md) § LR-D5 |
| **Compose project** | single acceptance stack ports **8080** / **4173** |
| **frontend_ui_in_scope** | false |
| **Go-live / #3b / #5a / Wave D Done** | **not** claimed |

## Honesty

- Evidence here is **measured-input / proposed** for LR-D5 only.
- **Do not** promote k6 latency / error-rate / check results to **confirmed** SLOs.
- Soft thresholds inside `smoke-healthz.js` are smoke usability gates, not product SLAs.
- If host `k6` / Docker image is unavailable, record **dry-run / presence** honestly — do not invent metrics.

## Artifacts

| File | Role |
| --- | --- |
| [latest-summary.md](./latest-summary.md) | Human mirror of last runner invocation |
| [latest-summary.json](./latest-summary.json) | Machine-readable last invocation |
| `run-*.json` | Timestamped copies of summary payloads |
| `k6-summary-export.json` | Present only after a successful host/docker `k6 run` |
| `k6-run-*.log` | Present only after an executed run |

## How to reproduce

```powershell
# From repo / worktree root — acceptance stack must answer /healthz
.\scripts\k6-smoke.ps1

# When k6 cannot execute
.\scripts\k6-smoke.ps1 -DryRun
```

See [`perf/k6/README.md`](../../../../perf/k6/README.md) for install + Docker Desktop `host.docker.internal` notes.

## Stage 10

Queued Docker deploy for live-stack re-evidence (if orchestrator schedules stage 10) uses
`.\scripts\docker-deploy-queue.ps1` only — not required to change default `mvn verify`.
