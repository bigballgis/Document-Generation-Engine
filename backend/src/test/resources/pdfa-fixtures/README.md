# PDF/A fixtures (IBL-B3 / F12)

Machine-validation inputs for `VeraPdfPdfA2bAssertor` (veraPDF Greenfield).

| File | Origin | Role |
| --- | --- | --- |
| `pdfa-2b-corpus-pass.pdf` | [veraPDF corpus](https://github.com/veraPDF/veraPDF-corpus) `PDF_A-2b/6.1 File structure/6.1.2 File header/veraPDF test suite 6-1-2-t02-pass-a.pdf` ([CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)) | Positive PDF/A-2b fixture for the verify gate |

These are **test fixtures only** — not product archival output baselines. CE-O01 / ADR-0058 product behavior (LibreOffice `SelectPdfVersion=2` export) is unchanged by this leaf.

## Honesty note — LibreOffice export vs veraPDF

A spot-check with LibreOffice 7.3.7 in the platform backend image (`writer_pdf_Export` + `SelectPdfVersion=2` from a minimal ODT) **did not** pass veraPDF PDF/A-2b (failed ISO 19005-2:2011 checks including **6.2.4.3** and **6.6.2.1**). That gap is **out of scope for IBL-B3** (gate wiring / F12). Closing full LO→veraPDF conformance on production letters is a separate hardening follow-up — do **not** treat CE-O01 as reopened by this note.

Cheap XMP checks remain in `PdfAidXmpAssertor` for synthetic golden packages; they are **not** the F12 machine gate.

## Run / skip

- Default verify (gate required): `mvn -B -ntp -f backend/pom.xml verify`
- Explicit profile: `mvn -B -ntp -f backend/pom.xml -Pverapdf-pdfa verify`
- Local skip only (never CI):  
  `mvn -Pdev-fast test -Dtest=VeraPdfPdfA2bAssertorTest -Ddocgen.verapdf.skip=true -Ddocgen.verapdf.required=false`

When `docgen.verapdf.required=true` (verify default) and skip is set, the gate **fails** (fail-not-skip).
