# Golden corpus (CE-K07 + IBL-C3)

Regression fixtures for DOCX keypath + PDF text / layout-metric assertions
(no pixel/visual compare; no Word baselines).

| Theme ID | Maturity | Filled by |
| --- | --- | --- |
| `dual-font-master` | ACTIVE | CE-K02 |
| `cross-page-table` | ACTIVE | CE-K06a |
| `nested-clauses` | ACTIVE | CE-K07 sample (+ IBL-C1 layout metrics) |
| `compute-variables` | ACTIVE | CE-K03 |
| `chinese-uppercase-amount` | ACTIVE | CE-K03 (**zh** locale / Chinese amount-in-words) |
| `english-locale-letter` | ACTIVE | IBL-C3 (**en-US** letter + `SPELL_AMOUNT` USD) |
| `multi-currency-amount` | ACTIVE | IBL-C3 (**EUR / USD / CNY** `FORMAT_AMOUNT`) |
| `specimen-watermark` | ACTIVE | CE-G02 |
| `encrypted-pdf` | ACTIVE | CE-K07 sample (+ IBL-C1 layout metrics) |
| `pdfa-2b` | ACTIVE | CE-O01 (lightweight pdfaid XMP on SYNTHETIC PDF; + IBL-C1) |
| `long-clause-limits` | ACTIVE | IBL-B4 |

Harness: `com.bank.docgen.rendering.goldencorpus` (executed by `mvn verify`).

## IBL-C3 / F19 — cross-locale matrix + PDF provenance honesty

| Requirement | How this corpus satisfies it |
| --- | --- |
| en + zh themes | `english-locale-letter` (`en-US`) + `chinese-uppercase-amount` (`zh-CN`) |
| Multi-currency | `multi-currency-amount` (EUR / USD / CNY) — not Chinese-amount-only |
| `LIBREOFFICE` honesty | PDF half labeled `LIBREOFFICE` is converted via `soffice` in the harness **or** the PDF half is **Assumptions.skip** when `soffice` is absent — never a forged LO binary |
| `SYNTHETIC` honesty | PDFBox text projection from assembled DOCX; packages that cannot claim LO keep `pdfSource: SYNTHETIC` + `harnessSelfTest: true` (**FOS-W13-2** — harness self-test, not product PDF proof) |
| `productPdf` | Optional honesty label (e.g. `pending-CRCH-W5`) when multi-page/QR/attachment PDF product claims await CRCH W5 |
| condition-inside-loop | Theme `condition-inside-loop` (**FOS-W13-4**) — real `conditionBlock` inside `loopBlock` |
| No invented LO PDFs | No `expected/*.pdf` baselines checked in; do not relabel `SYNTHETIC` → `LIBREOFFICE` without real LO conversion |

**Host / CI note:** without `soffice` on `PATH`, `LIBREOFFICE` PDF halves **SKIP** under default `mvn verify` (DOCX half still runs). New IBL-C3 themes use **honest SYNTHETIC** PDF halves so the locale/currency matrix stays green without inventing LO PDFs. Mandatory LO CI lane: **`-Plibreoffice-ci`** (IBL-D2 / F21) — fail-closed when soffice is absent; see [libreoffice-ci-lane.md](../../../../../docs/architecture/libreoffice-ci-lane.md).

Evidence: `docs/plan/evidence/ibl-c3-cross-locale-golden/`.

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
