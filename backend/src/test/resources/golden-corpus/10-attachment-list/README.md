# 10-attachment-list (ACTIVE)

CE-K06c golden sample: `attachmentListRef` → ordered numbered list paragraphs from `string[]` variables.

- **Maturity:** `ACTIVE` — DOCX half executed by golden-corpus harness in `mvn verify`.
- **Behavior:** `ATTACHMENTS` string array written as ordered list items with `w:numPr`.
- **Assertions:** `expected/docx-assertions.json` requires `numPr` and annex text order (structural; no pixel compare).
- **PDF:** deferred (`expected/pdf-assertions.json`); no soffice required for this package.
