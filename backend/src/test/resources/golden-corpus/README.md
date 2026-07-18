# Golden corpus (CE-K07)

Regression fixtures for DOCX keypath + PDF text assertions (no pixel/visual compare).

| Theme ID | Maturity | Filled by |
| --- | --- | --- |
| `dual-font-master` | ACTIVE | CE-K02 |
| `cross-page-table` | ACTIVE | CE-K06a |
| `nested-clauses` | ACTIVE | CE-K07 sample |
| `compute-variables` | ACTIVE | CE-K03 |
| `chinese-uppercase-amount` | ACTIVE | CE-K03 |
| `specimen-watermark` | ACTIVE | CE-G02 |
| `encrypted-pdf` | ACTIVE | CE-K07 sample |
| `pdfa-2b` | ACTIVE | CE-O01 (lightweight pdfaid XMP on SYNTHETIC PDF) |
| `long-clause-limits` | PLACEHOLDER | follow-on |

Harness: `com.bank.docgen.rendering.goldencorpus` (executed by `mvn verify`).

**IBL-B3 / F12:** Machine PDF/A-2b validation is **veraPDF** via
`VeraPdfPdfA2bAssertor` + fixtures under `src/test/resources/pdfa-fixtures/` (not XMP-only).
See [verapdf-pdfa-verify-gate.md](../../../../../docs/operations/verapdf-pdfa-verify-gate.md)
and [ADR-0059](../../../../../docs/adr/rendering-authoring/0059-verapdf-pdfa-verify-gate.md).
Optional JSON flag `requireVeraPdf` on real (non-SYNTHETIC) PDF packages.
