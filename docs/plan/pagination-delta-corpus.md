# Pagination Delta Corpus — LR-A7 / ADR-0042

**Status:** Proposed baseline (pending user confirmation per document-as-code constitution)
**Last updated:** 2026-07-05

## Purpose

Record the Word vs LibreOffice-PDF page-count delta for each demo letter in the corpus. Per
ADR-0042, the v1 budget is **±1 page**; entries exceeding the budget trigger a fidelity
warning, and entries exceeding ±2 trigger a fidelity blocker (both gated on user confirmation).

The corpus is the regression test for layout drift — if a rendering change shifts any entry's
delta, the change must be reviewed.

## Baseline measurements

Measurements are taken with: master DOCX authored in Word → assembled by the platform →
DOCX→PDF via LibreOffice headless (cli mode, font baseline per ADR-0041). Page counts are
counted on the rendered PDF.

| Demo package | Template | Word pages (author) | PDF pages (LO) | Delta | Status |
| --- | --- | --- | --- | --- | --- |
| `deploy/demo-fol` | FOL corporate letter | _pending_ | _pending_ | _pending_ | baseline not yet measured |
| `deploy/demo-mortgage` | Mortgage approval letter | _pending_ | _pending_ | _pending_ | baseline not yet measured |
| `deploy/demo-credit-limit` | Credit-limit adjustment letter | _pending_ | _pending_ | _pending_ | baseline not yet measured |
| `deploy/demo-trade-lc` | Trade letter of credit | _pending_ | _pending_ | _pending_ | baseline not yet measured |
| `deploy/demo-collection` | Collection notice | _pending_ | _pending_ | _pending_ | baseline not yet measured |
| `deploy/demo-annual-review` | Annual review letter | _pending_ | _pending_ | _pending_ | baseline not yet measured |

> The corpus is seeded with the eight demo packages shipped in P22. Concrete measurements
> land when the Docker stack with the LR-A2 font baseline is redeployed; the table above is
> the schema, not the data. Each row is filled in by a `docker-deploy.ps1` + manual page
> count (or an automated `PdfPageCountTest` once the corpus is wired).

## Budget enforcement (pending confirmation)

- **±1 page** → no action (within budget).
- **±2 pages** → fidelity warning logged; author should review but may publish.
- **±3+ pages** → fidelity blocker; template must not publish until layout is adjusted.

The enforcement is a **pending proposal** — until the user confirms the budget, the system
logs the delta but does not warn or block. This file records the baseline so the delta is
visible even before enforcement is wired.

## Drift detection

When a rendering change (LR-A1 profile isolation, LR-A2 font baseline, POI upgrade,
LibreOffice upgrade) lands, re-measure every row. Any delta change > 0 must be called out
in the commit message and reviewed by the architecture-reviewer.
