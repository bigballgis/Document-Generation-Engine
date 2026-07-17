# Path X — Word baseline durable exemption (PRR-C01 / Task #103)

**Date:** 2026-07-18  
**Slice:** `prod-adr-0042-0043-closeout` · Task Master **#103**  
**method:** `ms-word-unavailable-on-host`  
**Checklist #3b:** this exemption **≠ GO** and **≠** proven Word↔LibreOffice delta

## Purpose

Close ADR-0042 runtime enforcement + ADR-0043 slice A acceptance evidence path when
the delivery host has no licensed Microsoft Word for Path E corpus measurement.

## EVD-C2 checklist

| Clause | Status |
| --- | --- |
| 1. Explicit method = `ms-word-unavailable-on-host` | **Yes** |
| 2. Word / delta columns remain **n/a** (no fabricated numbers) | **Yes** — see [pagination-delta-corpus.md](../../plan/pagination-delta-corpus.md) and ADR-0042 residual table |
| 3. Docker PDF baseline remains the drift sentinel (LR-A7) | **Yes** — [docs/evidence/lrp-a7-pagination/](../lrp-a7-pagination/) |
| 4. Runtime enforcement metadata-gated (PAG-C7) | **Yes** — `PaginationDeltaEvaluator` + preview/runtime wiring; skip when `authorWordPageCount` unset |
| 5. ADR-0042 / ADR-0043 Accepted with residuals (doc-sync) | **Yes** — stage 12 MAIN `post-task-doc-sync` after merge `3513ab92` (ADR-0042 Path X residual; ADR-0043 slice A Accepted / slice B residual) |
| 6. Residual owner + retest trigger | Owner: rendering-engineer + doc-keeper; trigger: Word-equipped host available → open Path E measurement leaf (≥5 corpus letters) |
| 7. Explicit non-claims | This exemption **≠ checklist #3b GO**; **≠** Word↔LO proven |

## Residual — ADR-0043 slice B (honest)

Out of scope for Accepted slice A / this leaf:

- Full ECMA-376 XSD schema validation
- LibreOffice 24+ headless open as a production gate

Slice A (OPC open + Word XML well-formedness fail-closed via `OoxmlOutputValidator`) remains the Accepted Done line.

## #3b honesty

Per BDD EVD-C3 / XR-C2: **do not** flip launch-readiness checklist **#3b → GO** from this exemption.
Stage 12 MAIN doc-sync marked **#3b → CONDITIONAL** only (ADR Accepted + enforcement + Path X).
Path E is required before **GO**.

## Links

- Behavior: [prod-adr-0042-0043-closeout.md](../../behavior/prod-adr-0042-0043-closeout.md)
- ADR-0042: [0042-pagination-delta-budget.md](../../adr/rendering-authoring/0042-pagination-delta-budget.md)
- ADR-0043: [0043-ooxml-output-validation-gate.md](../../adr/rendering-authoring/0043-ooxml-output-validation-gate.md)
- Checklist: [launch-readiness-checklist.md](../../operations/launch-readiness-checklist.md)
