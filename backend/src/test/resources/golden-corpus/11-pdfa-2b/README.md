# pdfa-2b (CE-O01)

ACTIVE golden package for PDF/A-2b archival identifier assertions.

- **DOCX:** structured assemble + XML_CONTAINS body text
- **PDF:** SYNTHETIC PDF with `pdfaid:part=2` / `pdfaid:conformance=B` XMP (lightweight check; no veraPDF dependency in `mvn verify`)
- **Profile:** `renderProfile.pdfArchivalProfile=PDF_A_2B` (documentation of publish-locked path; orthogonal to `07-encrypted-pdf`)
