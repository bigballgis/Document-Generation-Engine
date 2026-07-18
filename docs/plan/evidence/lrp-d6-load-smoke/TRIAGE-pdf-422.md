# DEF-LRP-D6-001 — Concurrent FOL PDF → `TEMPLATE_VALIDATION_FAILED` / serviceUnavailable

| Field | Value |
| --- | --- |
| **Defect id** | `DEF-LRP-D6-001` |
| **Title** | Concurrent FOL sync PDF rejected as `TEMPLATE_VALIDATION_FAILED` (`api.error.generation.serviceUnavailable`) |
| **Slice (opened)** | `lrp-d6-load-smoke` / Task Master #37 / LR-D6 |
| **Observed** | 2026-07-11T17:43:35Z harness run (`latest-summary.json`); reconfirmed 2026-07-12 with 10× parallel PDF probes |
| **Evidence (historical)** | Scenario A: n=20 success=12 errorRate=0.4; **8× PDF** HTTP 422 `TEMPLATE_VALIDATION_FAILED`; **DOCX all succeeded**; poolRejections=0 |
| **Status** | **CLOSED** (2026-07-19) — see disposition below |
| **Closing slice** | IBL-B2 / Task Master **#114** (`ibl-b2-pdf-conversion-capacity`) |
| **Closing evidence** | [ibl-b2-pdf-capacity/latest-summary.json](../ibl-b2-pdf-capacity/latest-summary.json) |

---

## Disposition (IBL-B2 / 2026-07-19) — CLOSED

| Concern | Closure |
| --- | --- |
| **Mislabelled taxonomy** (CB/timeout → `TEMPLATE_VALIDATION_FAILED`) | **Closed** by PRR-D01A / #104 — map to `GENERATION_SERVICE_UNAVAILABLE` / `GENERATION_TIMEOUT`; pool saturation → `PDF_CONVERSION_CAPACITY_EXCEEDED` ([prod-ops-resilience-pdf-pool.md](../../../behavior/prod-ops-resilience-pdf-pool.md)) |
| **Sync capacity under LR-D6-class concurrency** (default pool=2 / queue=0 fail-fast surface) | **Closed** by IBL-B2 — product defaults **pool=4 / queue=8** (bounded absorb, AbortPolicy); capacity plan [pdf-conversion-capacity-plan.md](../../../operations/pdf-conversion-capacity-plan.md) |
| **Agreed smoke confirmation** (PDF failures &lt; 8; no Abort storm) | **Closed** — Stage 5/10 queued Docker smoke on tip `36a9821c`: Scenario A n=20 success=**20** errors=**0**; PDF failures **0** (&lt; 8); `poolRejectionCount`=**0**; no `TEMPLATE_VALIDATION_FAILED` samples. Evidence: [ibl-b2-pdf-capacity/](../ibl-b2-pdf-capacity/) |

**Not go-live.** Do **not** flip checklist **#3b** / **#5a**. Do **not** invent confirmed NFR SLOs. Chaos/failover depth (if needed) remains IBL-D4 — **not** a residual of this DEF.

---

## One-line cause (historical)

Under concurrent sync PDF load, Resilience4j open-circuit / timeout paths mapped to
`TemplateValidationException("api.error.generation.serviceUnavailable")`, which the API
surfaced as **`TEMPLATE_VALIDATION_FAILED`** — not a FOL variable/schema validation failure.
Separately, fail-fast pool defaults (2/0) left little sync absorption headroom for ≥10 concurrent PDF.

---

## Facts (historical LR-D6)

1. **Format skew:** Alternating DOCX/PDF (10 each). All DOCX OK; PDF failures only. Single-shot PDF against the same stack returns **200** (~370KB).
2. **Envelope (reproduced at open):**
   - `error.code` = `TEMPLATE_VALIDATION_FAILED`
   - `error.messageKey` = `api.error.generation.serviceUnavailable`
   - `error.message` = `The generation service is temporarily unavailable.`
3. **Mapper (pre-D01A):** `ResilienceFailureMapper` converted `CallNotPermittedException` / `TimeoutException` into `TemplateValidationException`, which advice always emitted as `TEMPLATE_VALIDATION_FAILED`.
4. **Not capacity-pool 503 in the named 8× case:** That wave had **poolRejections=0** and the serviceUnavailable taxonomy above (capacity Abort was a separate earlier signature).
5. **Scenario B:** 5/5 SSE terminals — **PASS** (not an LR-B3 silent-drop regression).

---

## Closing facts (IBL-B2 smoke, 2026-07-18T19:37:40Z)

1. **Scenario A:** n=20 alternating DOCX/PDF; success=20; errorCount=0; poolRejectionCount=0.
2. **PDF failure count:** **0** (hard bar was &lt; 8).
3. **DOCX:** all succeeded (no regression).
4. **Taxonomy:** no `TEMPLATE_VALIDATION_FAILED` / capacity-mask samples in this run.
5. **Latency observed (not SLO):** p95≈41205 ms / p99≈41209 ms under concurrent mix — recorded only; not confirmed NFR.

---

## Classification (historical → closed)

| Hypothesis | Verdict |
| --- | --- |
| LR-A1 profile isolation regression | **Unlikely** — DOCX sync and management preview paths work; failure is PDF concurrency + resilience mapping |
| LR-B3 SSE hardening regression | **No** — Scenario B zero drops (historical) |
| FOL variables / UTF-8 BOM / template seed schema | **Unlikely primary** — DOCX uses the same variables body; single PDF succeeds |
| Concurrent PDF conversion + Resilience4j CB/timeout → mislabeled `TEMPLATE_VALIDATION_FAILED` | **Primary (taxonomy)** — closed by D01A |
| Sync pool/queue too small for agreed smoke | **Primary (capacity)** — closed by IBL-B2 defaults 4/8 + smoke evidence |

---

## Links

- Capacity plan: `docs/operations/pdf-conversion-capacity-plan.md`
- B2 evidence: `docs/plan/evidence/ibl-b2-pdf-capacity/`
- BDD: `docs/behavior/ibl-b2-pdf-conversion-capacity.md`
- Harness: `backend/src/test/java/com/bank/docgen/runtime/loadsmoke/`
- Mapper: `backend/src/main/java/com/bank/docgen/infrastructure/resilience/ResilienceFailureMapper.java`
- Historical plan: `docs/plan/detail/LRP-D-ops-observability.md` §LR-D6
