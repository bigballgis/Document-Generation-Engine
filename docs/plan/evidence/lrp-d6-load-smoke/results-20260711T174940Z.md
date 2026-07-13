# LR-D6 Load Smoke Evidence

**measuredAt:** 2026-07-11T17:49:40.420919600Z
**stackVersion:** a262706
**hardwareNote:** Windows host; Docker Desktop acceptance stack 8080/4173
**baseUrl:** http://localhost:8080
**templateExternalId:** CORP-FOL-OFFER

## Scenario A — Concurrent sync generation

```
{requestedConcurrency=20, formats=[DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF], sampleCount=20, successCount=12, errorCount=8, errorRate=0.4, poolRejectionCount=0, errorCodeCounts={TEMPLATE_VALIDATION_FAILED=8}, messageKeyCounts={api.error.generation.serviceUnavailable=8}, errorSamples=[PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false, PDF -> TEMPLATE_VALIDATION_FAILED messageKey=api.error.generation.serviceUnavailable http=422 poolRejection=false], p50Ms=7979, p95Ms=15939, p99Ms=16065, summaryLine=n=20 success=12 errors=8 errorRate=0.4000 poolRejections=0 p50=7979ms p95=15939ms p99=16065ms, triageNote=Non-zero error rate — named defect DEF-LRP-D6-001 (docs/plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md). Concurrent FOL PDF → TEMPLATE_VALIDATION_FAILED / api.error.generation.serviceUnavailable via ResilienceFailureMapper. Do not tune product thresholds to pass.}
```

## Scenario B — Parallel SSE preview streams

```
{requestedParallelStreams=5, startedStreams=5, terminalReceived=5, droppedStreams=0, terminalEvents=[completed, completed, completed, completed, completed], startErrors=[], zeroDropped=true, metParallelTarget=true, blockerNote=null}
```

## Notes

- Do not tune thresholds to pass; record observed reality.
- Pool rejections surface as `PDF_CONVERSION_CAPACITY_EXCEEDED`.
- SSE terminal events: `completed` | `failed`.
