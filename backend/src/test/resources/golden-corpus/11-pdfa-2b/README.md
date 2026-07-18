# pdfa-2b (CE-O01)

ACTIVE golden package for PDF/A-2b archival identifier assertions.

- **DOCX:** structured assemble + XML_CONTAINS body text
- **PDF:** SYNTHETIC PDF with `pdfaid:part=2` / `pdfaid:conformance=B` XMP (cheap `PdfAidXmpAssertor` check)
- **Profile:** `renderProfile.pdfArchivalProfile=PDF_A_2B` (documentation of publish-locked path; orthogonal to `07-encrypted-pdf`)

**IBL-B3:** Machine veraPDF validation lives in `pdfa-fixtures/` + `VeraPdfPdfA2bAssertor`
(not this SYNTHETIC package). Do not set `requireVeraPdf` here until the package uses a
real PDF/A artifact.
