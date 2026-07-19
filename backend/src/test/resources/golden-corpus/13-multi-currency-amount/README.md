# multi-currency-amount (IBL-C3)

Multi-currency golden theme for F19 (beyond Chinese-amount-only).

| Half | Provenance |
| --- | --- |
| DOCX | Structured assemble + `FORMAT_AMOUNT(amount, ISO)` for EUR / USD / CNY |
| PDF | **SYNTHETIC** — PDFBox text projection; non-ASCII currency symbols may be sanitized to spaces (ASCII `$` / digits still asserted) |

`pdfSource` is **SYNTHETIC** by design on hosts without LibreOffice. Do not relabel to
`LIBREOFFICE` unless the PDF half is produced by `soffice`.
