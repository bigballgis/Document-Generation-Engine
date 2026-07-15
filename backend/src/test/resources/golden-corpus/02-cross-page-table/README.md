# 02-cross-page-table (ACTIVE)

CE-K06a golden sample: cross-page table header repetition via OOXML `<w:tblHeader/>`.

- **Maturity:** `ACTIVE` — DOCX half executed by golden-corpus harness in `mvn verify`.
- **Behavior:** `repeatHeaderAcrossPages: true` on `tableComponent` → header row carries `w:tblHeader`.
- **Assertions:** `expected/docx-assertions.json` requires `tblHeader` (XML + XPath) and header labels.
- **PDF:** deferred (`expected/pdf-assertions.json`); no soffice required for this package.
