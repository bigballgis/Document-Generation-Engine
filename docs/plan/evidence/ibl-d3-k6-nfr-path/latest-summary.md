# IBL-D3 k6 smoke — latest summary

| Field | Value |
| --- | --- |
| Timestamp (UTC) | 20260719T091747Z |
| Mode | `dry-run` |
| Base URL | `http://localhost:8080` |
| k6 available | False |
| Runner | `docker-missing-image` |
| Exit code | 0 |
| healthz preflight | OK 200 |
| Script | `perf/k6/smoke-healthz.js` |
| NFR status | **measured-input path only — proposed awaiting confirmation (no confirmed SLO)** |

## Honesty

- Numbers are **measured-input / proposed** for LR-D5 only.
- **Do not** promote to confirmed SLO from this run.
- Soft k6 thresholds ≠ product SLOs.
- **k6 load was not executed** on this host during Stage 4 — no latency/error metrics invented.

## Host residuals (2026-07-19)

| Attempt | Result |
| --- | --- |
| Host `k6` on PATH | Absent |
| `winget install GrafanaLabs.k6` | Download OK; MSI install **cancelled** (exit 1602) |
| `docker pull grafana/k6` | Registry dial timeout (Docker Hub unreachable from this host) |
| Portable GitHub zip | Download hung at 0 bytes; aborted |
| Acceptance `/healthz` | **200** `{"status":"UP"}` (stack reachable; smoke target ready) |

## Notes

- Suite **is** checked in under `perf/k6/`; runner `scripts/k6-smoke.ps1` supports host / portable `.tools/k6` / Docker.
- Re-run without `-DryRun` after `winget install --id GrafanaLabs.k6 -e` **or** successful `docker pull grafana/k6:latest`.
- Feeds [NFR §待确认 / LR-D5](../../../requirements/non-functional-requirements.md#lr-d5-nfr-数值提案proposed--awaiting-confirmation) as confirmation-path infrastructure only.
- See [HOST-NOTES.md](./HOST-NOTES.md) for install pointers.
