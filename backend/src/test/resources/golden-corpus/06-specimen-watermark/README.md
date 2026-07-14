# 06-specimen-watermark (ACTIVE)

CE-G02 SPECIMEN watermark golden package.

- **Maturity:** `ACTIVE` — formal assemble path runs in `mvn verify`.
- **Formal path:** assembled DOCX/PDF must contain body text and must **not** contain
  `SPECIMEN` (runtime / formal zero-watermark guardrail).
- **Preview path:** dual-path companion
  `SpecimenWatermarkPreviewPathTest` applies DOCX header/footer + PDF diagonal
  stampers and asserts `SPECIMEN` presence (harness formal runner stays watermark-free).
