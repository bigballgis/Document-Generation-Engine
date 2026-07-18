# Golden corpus (CE-K07)

Regression fixtures for DOCX keypath + PDF text / layout-metric assertions
(no pixel/visual compare; no Word baselines).

| Theme ID | Maturity | Filled by |
| --- | --- | --- |
| `dual-font-master` | ACTIVE | CE-K02 |
| `cross-page-table` | ACTIVE | CE-K06a |
| `nested-clauses` | ACTIVE | CE-K07 sample (+ IBL-C1 layout metrics) |
| `compute-variables` | ACTIVE | CE-K03 |
| `chinese-uppercase-amount` | ACTIVE | CE-K03 |
| `specimen-watermark` | ACTIVE | CE-G02 |
| `encrypted-pdf` | ACTIVE | CE-K07 sample (+ IBL-C1 layout metrics) |
| `pdfa-2b` | ACTIVE | CE-O01 (lightweight pdfaid XMP on SYNTHETIC PDF; + IBL-C1) |
| `long-clause-limits` | ACTIVE | IBL-B4 |

Harness: `com.bank.docgen.rendering.goldencorpus` (executed by `mvn verify`).

## Assertion types

### DOCX (`expected/docx-assertions.json`)

| Type | Purpose |
| --- | --- |
| `XML_CONTAINS` / `XML_NOT_CONTAINS` | Substring in a named OOXML part |
| `XPATH_EXISTS` | XPath present in a named OOXML part |

### PDF (`expected/pdf-assertions.json`)

| Type | Purpose |
| --- | --- |
| `TEXT_CONTAINS` / `TEXT_NOT_CONTAINS` | PDFBox text extract |
| `PAGE_COUNT` | PDFBox page count — `equals` **or** `min`/`max` |
| `TEXT_POSITION` | PDFBox `TextPosition` anchor for `substring` on `pageIndex` inside `xMin`/`xMax`/`yMin`/`yMax` using stripper `XDirAdj`/`YDirAdj` (**Y is top-down** on the page, not PDF bottom-left origin) |

Optional package flags: `deferred`, `requireEncrypted`, `requirePdfA2b`, `requireVeraPdf`.

**Forbidden:** any `PIXEL_*` / screenshot / visual-diff kind unless §Pending **PD-2**
pixel/visual PDF regression ADR is **Accepted**. Loader rejects them fail-closed.

**IBL-C1 / F17:** Layout regression uses **page count + text-position** only (not pixels).
SYNTHETIC packages carry metrics that stay green without LibreOffice; `LIBREOFFICE`
PDF half still **Assumptions.skip** when `soffice` is unavailable (DOCX path must stay green).

**IBL-B3 / F12:** Machine PDF/A-2b validation is **veraPDF** via
`VeraPdfPdfA2bAssertor` + fixtures under `src/test/resources/pdfa-fixtures/` (not XMP-only).
See [verapdf-pdfa-verify-gate.md](../../../../../docs/operations/verapdf-pdfa-verify-gate.md)
and [ADR-0059](../../../../../docs/adr/rendering-authoring/0059-verapdf-pdfa-verify-gate.md).
Optional JSON flag `requireVeraPdf` on real (non-SYNTHETIC) PDF packages.

**IBL-B6 / F16 (legal reproducibility freeze):** Day-to-day harness stays XPath + PDF text
+ layout metrics (no `PIXEL_*`, no Word baselines). SHA-256 **content-hash** baseline
procedure for legal cuts is governed by
[ADR-0060](../../../../../docs/adr/rendering-authoring/0060-legal-reproducibility-freeze.md)
and ops [legal-reproducibility-freeze.md](../../../../../docs/operations/legal-reproducibility-freeze.md)
— overlay evidence under `docs/plan/evidence/…/content-hash-baselines/`; do **not** invent
baseline PDF binaries or Word Path-E baselines here.
