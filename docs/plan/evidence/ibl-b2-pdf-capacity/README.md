# IBL-B2 PDF capacity evidence

| Field | Value |
| --- | --- |
| **Slice** | `ibl-b2-pdf-conversion-capacity` / Task Master **#114** |
| **BDD** | [ibl-b2-pdf-conversion-capacity.md](../../../behavior/ibl-b2-pdf-conversion-capacity.md) |
| **Capacity plan** | [pdf-conversion-capacity-plan.md](../../../operations/pdf-conversion-capacity-plan.md) |
| **DEF** | [TRIAGE-pdf-422.md](../lrp-d6-load-smoke/TRIAGE-pdf-422.md) (`DEF-LRP-D6-001`) |
| **Harness tip** | `36a9821c` |
| **Compose project** | `documentgenerationengine` (single stack; ports **8080** / **4173**) |

## Stage 4 (code/config)

| Evidence | Status |
| --- | --- |
| Product defaults pool=4 / queue=8 | Delivered on feature tip |
| Binding + absorb/reject unit tests | GREEN (pre-deploy) |
| Capacity plan indexed | Delivered |
| `mvn verify` | GREEN on feature branch (Stage 4) |

## Stage 5/10 — queued Docker deploy

| Field | Value |
| --- | --- |
| **Command** | `COMPOSE_PROJECT_NAME=documentgenerationengine` + `FRONTEND_PORT=4173` + `.\scripts\docker-deploy-queue.ps1 -ForceRebuild -Reason "IBL-B2 #114 Stage 5+10 PDF capacity smoke"` |
| **Result** | **DEPLOY_OK** (exit 0) — 2026-07-19T03:32+08:00 local |
| **healthz** | `GET http://localhost:8080/healthz` → **200** `{"status":"UP"}` |
| **Frontend** | `http://localhost:4173` → **200** |
| **Backend image id** | `sha256:2a361979efda32ad65dc9ebb695d3854b2d8e538e813bca11c98a91c01cf872b` |
| **Frontend image id** | `sha256:c8216fca04c4cb546375ef410fd3fc58f427b3250b70e6fc965c3b9b079075dd` |
| **Post-deploy** | Applied `docker-compose.load-smoke.override.yml.example` (`--no-deps` backend) for `lrp-d6-load-smoke` access-account + preview max-concurrent≥5 |
| **Notes** | Worktree `.env` JWT_SECRET synced from MAIN (insecure defaults refused). First attempts without `COMPOSE_PROJECT_NAME` hit `/docgen-minio` name conflict — retried on canonical project only. |

E2E / UIUX: **N/A** (`frontend_ui_in_scope=false`).

## Agreed smoke (B2-C7 / B2-C8) — PASS

Harness: `LoadSmokeDockerHarnessTest` with `-Ddocgen.loadSmoke=true` against Docker **8080**, FOL `CORP-FOL-OFFER`, `syncConcurrency=20` (DOCX/PDF alternating → **10 PDF**).

| Metric | Historical LR-D6 | This run (IBL-B2) |
| --- | --- | --- |
| Scenario A n | 20 | 20 |
| success / errors | 12 / 8 | **20 / 0** |
| PDF failures | **8** | **0** (&lt; 8 hard bar) |
| DOCX | 10/10 OK | 10/10 OK |
| `poolRejectionCount` | 0 | **0** (no Abort storm) |
| `TEMPLATE_VALIDATION_FAILED` capacity/CB mask | 8× | **0** |
| p95 / p99 (success sample) | ~15939 / ~16065 ms | **41205 / 41209 ms** (observed; **not** an NFR SLO) |

Machine-readable: [latest-summary.json](./latest-summary.json) · [results-20260718T193740Z.json](./results-20260718T193740Z.json)

Human mirror: [latest-summary.md](./latest-summary.md) · [results-20260718T193740Z.md](./results-20260718T193740Z.md)

Harness JSON still stamps legacy `taskIds` (`lrp-d6-load-smoke` / #37) — that is harness writer default, not a claim that LR-D6 reopened. Owning slice for this run is **IBL-B2 / #114**.

### Scenario B (SSE) — honesty note

Harness Scenario B: 5 parallel streams, **droppedStreams=0**, all terminals = `failed` (not silent drop). **Not** used as B2 PDF-capacity pass/fail; recorded as observed. B2 hard bar is Scenario A PDF failure count.

## DEF disposition

With real smoke evidence (PDF failures **0** &lt; 8; pool rejections 0; no `TEMPLATE_VALIDATION_FAILED` mask), update [TRIAGE-pdf-422.md](../lrp-d6-load-smoke/TRIAGE-pdf-422.md) → **CLOSED**.

## Explicit non-claims

- **Not** go-live.
- Do **not** flip checklist **#3b** / **#5a**.
- Do **not** invent confirmed NFR SLOs from p95/p99.
- Do **not** claim Word baselines / Word host measurement.
- Wave B / IBL program remain incomplete (B3/B4/B7 out of scope).
