# P4 — Rendering & Preview (Detailed Plan)

**Phase status:** Done | **Depends on:** P3

## Behavior goal

Platform renders DOCX/PDF from template + variables; classifies fidelity blockers vs
warnings; stores preview records as authoritative evidence for test/approval.

## Key tasks

| ID | Task | Status |
| --- | --- | --- |
| P4-D01 | Rendering worker boundary (isolated service/module) | Done |
| P4-D02 | Preview record model (previewId, status, warnings, artifact refs) | Done |
| P4-D03 | Fidelity warning codes + messageKey catalog (English base) | Done |
| P4-T01 | DOCX assembly from structured content + master | Done |
| P4-T02 | PDF conversion (LibreOffice headless; test profile uses PDFBox placeholder) | Done (thin slice) |
| P4-T03 | Blocker vs warning classification pipeline | Done |
| P4-T04 | Test generation trigger from template UI | Done |
| P4-T05 | Side-by-side preview comparison view (minimal for slice) | Done |

**Exit:** Test generation produces stored preview + DOCX/PDF artifacts with warning summary.

**Backend slice evidence:** `com.bank.docgen.rendering` module, Flyway V6, `PreviewController`, `DocxAssemblerTest`, `PdfConversionService`, `PreviewRecordView.comparisonSummary`.

**Frontend slice evidence:** `TemplatePreviewPanel` on `TemplateDetailView`.

## Known gap (P22 — other session)

- **P4-T01** delivered thin DOCX assembly (plain-text structured downgrade path at slice close).
- **Structured content → real Word constructs** (emphasis, lists, styleRef, tables, numbering) is **P22-T01+**, not a reopen of P4 phase status.
- See [P22 demo expansion](./P22-demo-expansion-rendering-fidelity.md) and transitional seam in [execution-sync-ledger.md](../execution-sync-ledger.md).
