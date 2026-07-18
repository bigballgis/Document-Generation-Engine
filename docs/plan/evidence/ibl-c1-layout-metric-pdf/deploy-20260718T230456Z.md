# IBL-C1 / #120 — Stage 10 queued Docker deploy evidence

| Field | Value |
| --- | --- |
| Status | **DEPLOY_OK** |
| Timestamp (UTC) | 20260718T230456Z |
| Worktree | `D:/working/DGE-ibl-c1-layout-metric-pdf` |
| Tip | `d2492fc4f419c82dd0620b05629fe8140e881a2a` (`d2492fc4`) |
| Tip subject | feat(IBL-C1): PDFBox page-count and text-position golden assertions |
| Command | `.\scripts\docker-deploy-queue.ps1 -ForceRebuild` |
| Compose project | `documentgenerationengine` (single host stack) |
| frontend_ui_in_scope | false — E2E N/A |
| Go-live | **not** claimed |
| Word / pixel baselines | **not** invented |

## Health

| Check | Result |
| --- | --- |
| `GET http://localhost:8080/healthz` | HTTP 200 body `{"status":"UP"}` |
| `http://localhost:4173/` | **200** |

## Images / containers

- backend image: `sha256:10bf9b7b016d6a977765053972f28d6dc2aa093cb055568cce11c74cd8cb1655`
- frontend image: `sha256:961ff881261df9b391f07c77dd7d029e8825c7bc3969d25bdf16fb0bcdcbd3f0`
- backend inspect: `sha256:10bf9b7b016d6a977765053972f28d6dc2aa093cb055568cce11c74cd8cb1655|2026-07-18T23:03:31.070252974Z|healthy`
- frontend inspect: `sha256:961ff881261df9b391f07c77dd7d029e8825c7bc3969d25bdf16fb0bcdcbd3f0|2026-07-18T23:03:32.837239535Z|healthy`

See also `compose-ps.txt`, `images-backend.txt`, `images-frontend.txt`.

## LibreOffice / layout-metric smoke

| Probe | Result |
| --- | --- |
| `docker exec docgen-backend soffice --version` | `LibreOffice 7.3.7.2 30(Build:2)` |
| Host `soffice` | NO |
| LO PDF layout-metric live smoke | **SKIPPED** |

Backend acceptance container rootfs is read-only (hardening); docker cp of probe DOCX into running docgen-backend fails. Host has no soffice binary for surefire LIBREOFFICE_COMMAND. IBL-C1 layout metrics are PDFBox assertions on LO-produced PDFs in CI/surefire when soffice available — Stage 10 does not invent Word baselines or claim live LO→PDF layout-metric PASS.

No Word baseline regeneration and no pixel golden invention in this Stage 10 run.

## Deploy notes

- First queue attempt refused insecure JWT default from freshly created worktree `.env`; `JWT_SECRET` synced from MAIN (len=64, non-insecure; not logged).
- Retried with `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173`.
- `KAFKA_IMAGE` Hub bitnamilegacy warning retained (LOCAL/DEV ONLY) — not production coordinate claim.
- Slice is backend/rendering layout-metric (IBL-C1); `frontend_ui_in_scope=false`.
