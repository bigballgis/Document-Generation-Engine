# LRP-E1 — SSE-through-proxy incremental E2E Manifest

**Slice:** `lrp-e1-sse-proxy-e2e` (Task Master **#42**)  
**Stage:** 6 — e2e-test-engineer (functional)  
**Date:** 2026-07-12  
**Placement:** ISOLATED `D:/working/DGE-lrp-e1-sse-proxy-e2e` / `feat/lrp-e1-sse-proxy-e2e`  
**BDD readiness:** `not-applicable` ([docs/behavior/lrp-e1-sse-proxy-e2e.md](../../../docs/behavior/lrp-e1-sse-proxy-e2e.md))  
**Spec:** `frontend/e2e/LRP-E1-sse-incremental-progress.spec.ts`  
**Helper:** `frontend/e2e/helpers/sse-stream-capture.ts` (fetch+ReadableStream tee; not `page.on('response')`)  
**Verdict:** **PASS** (2/2) — Scenario A incremental + Scenario B heartbeat survival

## Environment

| Item | Value |
| --- | --- |
| UI | `http://127.0.0.1:4173` (nginx proxy) |
| API / healthz | `http://127.0.0.1:8080` **200** |
| Stage 5 | **DEPLOY_OK** 2026-07-12T08:41:50+08:00 · `COMPOSE_PROJECT_NAME=documentgenerationengine` |
| Fixture | `prepareCdpMvpGoldenDraft` (CDP MVP golden DRAFT + `CDP-MVP-DATASET-01`) |
| Role | Template Author (`10000003`) |
| Upstream | LR-B3 SSE hardening (heartbeat `:keep-alive` ~20s, nginx `proxy_buffering off`) |

## Command

```powershell
$env:E2E_TARGET='docker'; $env:E2E_SKIP_CATALOG_CLEANUP='true'
pnpm exec playwright test e2e/LRP-E1-sse-incremental-progress.spec.ts `
  --config playwright.docker.config.ts --workers=1
```

**Result:** **2 passed** (≈1.3 min) — 2026-07-12T00:47:03Z wall clock for evidence JSON.

Raw timestamps: [`LRP-E1-sse-timestamps.json`](./LRP-E1-sse-timestamps.json)

## Scenario A — Incremental preview progress through proxy

| Assertion | Result | Evidence |
| --- | --- | --- |
| Drive preview via UI Testing tab on :4173 | **PASS** | CDP golden → Run preview → success dialog |
| ≥2 distinct SSE **chunk** arrival times (not one terminal burst) | **PASS** | `chunkCount=2`; offsets **18 ms**, **1883 ms** |
| Meaningful gap threshold (≥100 ms) | **PASS** | `maxGapMs=1864` |
| Events observed | **PASS** | `event:progress` (GENERATING_DOCX 10%) → `event:completed` |
| Capture method | fetch body **tee** | Proves nginx does not buffer into a single flush |

UI phase wall-clock (complementary, not sole proof): first progress label @371 ms → success downloads @2766 ms.

Closes **CD-PIT-12** browser-level incremental proof (LR-B3 curl already showed ~524 ms progress→completed).

## Scenario B — Heartbeat survival ≥60s idle through nginx

| Assertion | Result | Evidence |
| --- | --- | --- |
| Batch probe (program §7 wants one batch run) | Attempted | Duration **2540 ms** (`exceeded60s=false`) — too fast for 60s idle |
| Dedicated idle progress-stream through :4173 | **PASS** | Non-terminal `previewId`; browser `fetch` + `Accept: text/event-stream` |
| Stream open ≥60 s without premature close | **PASS** | `durationMs=65075`; `closedBeforeObserveEnd=false` |
| `:keep-alive` heartbeats | **PASS** | 4 comments @ **68 / 20064 / 40062 / 60060** ms |
| Cadence ≈ LR-B3 20 s | **PASS** | Gaps **19996 / 19998 / 19998** ms |

Batch under 60s is expected for CDP golden (single dataset). Idle path is the documented LR-B3-equivalent proof (curl smoke had 78 s idle ×3 keep-alives).

## Key assertions (summary)

1. **Not** `page.on('response')` — that only timestamps header arrival once; tee of `ReadableStream` chunks proves incremental delivery.
2. Progress `event:progress` and `event:completed` arrived **1864 ms** apart through nginx.
3. Idle stream survived **65 s** with strict **~20 s** `:keep-alive` cadence (4 heartbeats).
4. No product / nginx / LR-B3 header changes required.

## Artifacts

| Path | Role |
| --- | --- |
| `frontend/e2e/LRP-E1-sse-incremental-progress.spec.ts` | Journey + assertions |
| `frontend/e2e/helpers/sse-stream-capture.ts` | Fetch tee + idle heartbeat observer |
| `frontend/e2e/evidence/LRP-E1-sse-timestamps.json` | Machine-readable timestamps |
| `frontend/e2e/evidence/LRP-E1-sse-manifest.md` | This manifest |
| `frontend/e2e/evidence/LRP-E1-uiux-manifest.md` | Stage 7 UIUX (PASS_WITH_NOTES) |
| `frontend/e2e/evidence/LRP-E1/screenshots/` | PreviewProgressDialog in-flight + success |
| `frontend/playwright-report/docker/` | HTML report (docker config) |

## Product code changes

**None** (test-only). LR-B3 transport left intact.

## Stage 7 UIUX (appended)

**Verdict:** **PASS_WITH_NOTES** — see [`LRP-E1-uiux-manifest.md`](./LRP-E1-uiux-manifest.md). PreviewProgressDialog in-flight + success captured @1440 REDBC; no Critical blockers; pre-existing T08 title/underline notes only.

## Blockers

None.
