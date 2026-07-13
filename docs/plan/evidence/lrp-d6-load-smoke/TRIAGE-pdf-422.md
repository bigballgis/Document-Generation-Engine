# DEF-LRP-D6-001 — Concurrent FOL PDF → `TEMPLATE_VALIDATION_FAILED` / serviceUnavailable

| Field | Value |
| --- | --- |
| **Defect id** | `DEF-LRP-D6-001` |
| **Title** | Concurrent FOL sync PDF rejected as `TEMPLATE_VALIDATION_FAILED` (`api.error.generation.serviceUnavailable`) |
| **Slice** | `lrp-d6-load-smoke` / Task Master #37 / LR-D6 |
| **Observed** | 2026-07-11T17:43:35Z harness run (`latest-summary.json`); reconfirmed 2026-07-12 with 10× parallel PDF probes |
| **Evidence** | Scenario A: n=20 success=12 errorRate=0.4; **8× PDF** HTTP 422 `TEMPLATE_VALIDATION_FAILED`; **DOCX all succeeded**; poolRejections=0 |

---

## One-line cause

Under concurrent sync PDF load, Resilience4j open-circuit / timeout paths map to `TemplateValidationException("api.error.generation.serviceUnavailable")`, which the API surfaces as **`TEMPLATE_VALIDATION_FAILED`** — not a FOL variable/schema validation failure.

---

## Facts

1. **Format skew:** Alternating DOCX/PDF (10 each). All DOCX OK; PDF failures only. Single-shot PDF against the same stack returns **200** (~370KB).
2. **Envelope (reproduced):**
   - `error.code` = `TEMPLATE_VALIDATION_FAILED`
   - `error.messageKey` = `api.error.generation.serviceUnavailable`
   - `error.message` = `The generation service is temporarily unavailable.`
3. **Mapper:** `com.bank.docgen.infrastructure.resilience.ResilienceFailureMapper` converts `CallNotPermittedException` / `TimeoutException` (and unmatched causes) into `TemplateValidationException("api.error.generation.serviceUnavailable")`, which `TemplateExceptionAdvice` always emits as code `TEMPLATE_VALIDATION_FAILED`.
4. **Not capacity-pool 503:** Same wave sometimes also saw `PDF_CONVERSION_CAPACITY_EXCEEDED` (earlier run); the named 8× case had **poolRejections=0** and the serviceUnavailable taxonomy above.
5. **Scenario B:** 5/5 SSE terminals — **PASS** (not an LR-B3 silent-drop regression).

---

## Classification

| Hypothesis | Verdict |
| --- | --- |
| LR-A1 profile isolation regression | **Unlikely** — DOCX sync and management preview paths work; failure is PDF concurrency + resilience mapping |
| LR-B3 SSE hardening regression | **No** — Scenario B zero drops |
| FOL variables / UTF-8 BOM / template seed schema | **Unlikely primary** — DOCX uses the same variables body; single PDF succeeds; BOM was a deploy workaround for parse, not this 422 |
| Concurrent PDF conversion + Resilience4j CB/timeout → mislabeled `TEMPLATE_VALIDATION_FAILED` | **Primary** |

---

## Recommended follow-up (do **not** patch inside D6 to green the smoke)

| Owner | Action |
| --- | --- |
| `backend-engineer` (infrastructure / resilience) | Revisit `ResilienceFailureMapper`: map CB/timeout to a dedicated generation unavailable code (not `TEMPLATE_VALIDATION_FAILED`) so load smoke and ops can triage correctly |
| `rendering-engineer` | Review LibreOffice / PDF conversion pool + CB thresholds under ≥10 concurrent PDF; separate capacity exceeded vs CB open |
| LR-D5 / ops (pending only) | Treat measured p95/p99 and this concurrent-PDF rejection rate as **proposed** NFR inputs — do not promote as confirmed SLO |

**D6 stance:** Record observed reality; accept Scenario A with **named defect triage** per plan acceptance (`errorRate 0` **or** every failure triaged). Do **not** tune product thresholds or weaken CB to make the harness green.

---

## Links

- Harness: `backend/src/test/java/com/bank/docgen/runtime/loadsmoke/`
- Mapper: `backend/src/main/java/com/bank/docgen/infrastructure/resilience/ResilienceFailureMapper.java`
- Plan: `docs/plan/detail/LRP-D-ops-observability.md` §LR-D6
