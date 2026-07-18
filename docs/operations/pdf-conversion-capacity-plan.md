# PDF conversion capacity plan (IBL-B2)

| Field | Value |
| --- | --- |
| **Status** | Active ops plan — IBL-B2 / Task Master **#114** → **Done** (`29d022b6`) |
| **Date** | 2026-07-19 |
| **Behavior SoT** | [ibl-b2-pdf-conversion-capacity.md](../behavior/ibl-b2-pdf-conversion-capacity.md) |
| **Taxonomy prior** | [prod-ops-resilience-pdf-pool.md](../behavior/prod-ops-resilience-pdf-pool.md) (PRR-D01A / #104) |
| **Defect** | [DEF-LRP-D6-001](../plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md) → **CLOSED** |
| **Evidence** | [ibl-b2-pdf-capacity/](../plan/evidence/ibl-b2-pdf-capacity/) |

This document is the **documented capacity plan** required by IBL-B2. It describes single-host
bounded sync absorption, fail-closed saturation, async client overflow, and metrics. It does
**not** confirm NFR p95 / error-rate SLOs, does **not** claim go-live, and does **not** flip
checklist **#3b** / **#5a**.

---

## 1. Defaults (product)

| Property | Env | Default | Notes |
| --- | --- | --- | --- |
| `docgen.rendering.conversion-pool-size` | `PDF_CONVERSION_POOL_SIZE` | **4** | `core = max = poolSize` |
| `docgen.rendering.conversion-queue-capacity` | `PDF_CONVERSION_QUEUE_CAPACITY` | **8** | Bounded queue only |
| Rejected-execution handler | — | `AbortPolicy` | Fail-closed when saturated |
| Sync slots | — | **pool + queue = 12** | Covers agreed smoke **10** concurrent PDF |

**Revision note:** Prior D01A-C9 / F4-C5 product defaults were **2 / 0** (fail-fast). IBL-B2
revises them to **4 / 8** (bounded absorb). Ops may still set `2` / `0` for intentional
fail-fast environments. **Unbounded** queues (`Integer.MAX_VALUE` / unbounded
`LinkedBlockingQueue`) remain forbidden.

Code: `DocgenRenderingProperties`, `application.yml`, `PdfConversionExecutorConfig`.

---

## 2. Sync path behavior

1. Sync PDF (`SYNC_STREAM` / `SYNC_DOWNLOAD_URL`) runs LibreOffice conversion via
   `PdfConversionOffloadSupport` on `pdfConversionExecutor`.
2. When `active < max` **or** queue has remaining capacity → task is accepted; caller waits on
   `future.get(timeout)` (`conversion-timeout-seconds` + 5s buffer).
3. When `active == max` **and** queue remaining == 0 → immediate
   `PDF_CONVERSION_CAPACITY_EXCEEDED` (HTTP **503**, `retryable=true`). No unbounded wait for a slot.

---

## 3. Async offload (client overflow — no new API)

| Mode | HTTP | Conversion capacity |
| --- | --- | --- |
| Sync | Blocks until complete/fail | Occupies pool/queue slot while converting |
| `ASYNC_TASK` | **202** + task id (ADR-0008) | **Shares** the same `pdfConversionExecutor` / LO path |

Async releases the HTTP thread; it does **not** create a second LibreOffice cluster and cannot
silently bypass pool limits. The platform does **not** rewrite saturated sync requests into async.

---

## 4. Fail-closed envelope (unchanged from D01A)

| Condition | HTTP | `error.code` |
| --- | --- | --- |
| Pool + queue saturated | 503 | `PDF_CONVERSION_CAPACITY_EXCEEDED` |
| Circuit open | 503 | `GENERATION_SERVICE_UNAVAILABLE` |
| Generation timeout | 504 | `GENERATION_TIMEOUT` |

Capacity / CB / timeout must **not** surface as `TEMPLATE_VALIDATION_FAILED`.

---

## 5. Metrics (stable names)

| Metric | Type |
| --- | --- |
| `docgen.pdf.conversion.pool.rejections` | Counter |
| `docgen.pdf.conversion.pool.active` | Gauge |
| `docgen.pdf.conversion.pool.queue.size` | Gauge |
| `docgen.pdf.conversion.pool.queue.remaining` | Gauge |
| `docgen.pdf.conversion.outcome` | Counter (existing) |
| `docgen.pdf.conversion.duration` | Timer (existing) |

Alert thresholds remain draft / pending NFR confirmation (see runbook).

---

## 6. Docker acceptance pin

Queued `docker-deploy-queue` stacks load these defaults from `application.yml` unless
`PDF_CONVERSION_POOL_SIZE` / `PDF_CONVERSION_QUEUE_CAPACITY` are overridden. Agreed smoke
(B2-C7/C8) **must** run with **4+8** (or equivalent ≥10 PDF slots). Do not weaken Resilience4j
CB/timeout solely to green the smoke.

Evidence directory: [docs/plan/evidence/ibl-b2-pdf-capacity/](../plan/evidence/ibl-b2-pdf-capacity/).

---

## 7. What this plan is not

- Not a confirmed concurrent-PDF or p95 SLO.
- Not Wave B / IBL program Done.
- Not go-live; not checklist #3b / #5a GO.
- Not IBL-B3 (veraPDF) / B4 (long-clause) / B7 (Word Path E).
