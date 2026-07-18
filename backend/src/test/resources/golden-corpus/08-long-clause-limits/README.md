# 08-long-clause-limits (ACTIVE)

IBL-B4 / F13 golden sample: extreme-length structured clause with **paginate / full retention**.

## Overflow policy (product)

| Policy | Status |
| --- | --- |
| **Paginate / full retention** | **CONFIRMED** — primary path |
| **Truncate** | **REJECTED** — must not silently drop clause tails on success |
| **Fail-closed** | **SECONDARY only** — documented hard limits / unrenderable pathology |

Confirmed in [ibl-b4-long-clause-overflow.md](../../../../../../docs/behavior/ibl-b4-long-clause-overflow.md) §4.1 (B4-C1…C3).

## Package

- **Maturity:** `ACTIVE`
- **renderMode:** `STRUCTURED_ASSEMBLE`
- **pdfSource:** `LIBREOFFICE` (SYNTHETIC forbidden for this theme)
- **Owning slice:** IBL-B4 / Task Master **#116**
- **Markers:** `LONG_CLAUSE_START` … long body (60 filler paragraphs) … `LONG_CLAUSE_END`
- **DOCX:** `XML_CONTAINS` both markers in `word/document.xml`
- **PDF:** `TEXT_CONTAINS` both markers when `soffice` is available; PDF half may `Assumptions.skip` per K07-C9 when LibreOffice is unavailable — **DOCX half still mandatory**
- **Page count:** companion `LongClauseOverflowGoldenCorpusTest` asserts PDFBox `getNumberOfPages() >= 2` when soffice is available (not a golden JSON assertion type)
- **Pixels:** forbidden (`PIXEL_*` not used)

## Fixture sizing

Body uses 60 repeating English paragraphs between the start/end markers so LibreOffice conversion typically yields **≥2 pages** under default letter layout. Exact character count is an implementation detail pinned by this package; do not shorten below the marker-retention + multi-page evidence bar without updating BDD Amendment.
