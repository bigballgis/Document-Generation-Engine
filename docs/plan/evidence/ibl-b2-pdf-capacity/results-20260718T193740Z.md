# LR-D6 Load Smoke Evidence

**measuredAt:** 2026-07-18T19:37:40.146006100Z
**stackVersion:** 36a9821c
**hardwareNote:** Windows host; Docker Desktop acceptance 8080/4173; COMPOSE_PROJECT_NAME=documentgenerationengine; IBL-B2 pool/queue defaults 4/8
**baseUrl:** http://localhost:8080
**templateExternalId:** CORP-FOL-OFFER

## Scenario A — Concurrent sync generation

```
{requestedConcurrency=20, formats=[DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF, DOCX, PDF], sampleCount=20, successCount=20, errorCount=0, errorRate=0.0, poolRejectionCount=0, errorCodeCounts={}, messageKeyCounts={}, errorSamples=[], p50Ms=14607, p95Ms=41205, p99Ms=41209, summaryLine=n=20 success=20 errors=0 errorRate=0.0000 poolRejections=0 p50=14607ms p95=41205ms p99=41209ms, triageNote=error rate 0}
```

## Scenario B — Parallel SSE preview streams

```
{requestedParallelStreams=5, startedStreams=5, terminalReceived=5, droppedStreams=0, terminalEvents=[failed, failed, failed, failed, failed], startErrors=[], zeroDropped=true, metParallelTarget=true, blockerNote=null}
```

## Notes

- Do not tune thresholds to pass; record observed reality.
- Pool rejections surface as `PDF_CONVERSION_CAPACITY_EXCEEDED`.
- SSE terminal events: `completed` | `failed`.
