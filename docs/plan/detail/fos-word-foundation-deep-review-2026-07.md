# FOS — Word-foundation deep review (2026-07-26)

**Program:** [FOS](../frontline-operability-solidity-program-2026-07.md)
**Type:** Audit evidence (read-only review) — not a delivery leaf by itself
**Status:** **Recorded**
**Audit baseline:** `f29211c5` / branch tip at review time
**Persona question (user):** Demo 数据是否过简？排版/字体/页眉页脚是否足以证明「必须以 Word 为基座」？

---

## 1. Verdict (one paragraph)

The user's doubt is **well founded on typography and layout**. KEEP-8 demos are
**data-rich scaffolds with weak Word craft**: masters are ~4–6 KB text skeletons (no logo,
no seal image, no `numbering.xml`, headers/footers text-only and often only bound on the
last section), body content is rewritten from JSON into catalogue styles that carry font/
size/bold only, and the most complex FOL package reaches "100 pages" via **2,466 flat
paragraphs**, not via bank-grade tables, schedules, or typography. **DOCX-as-output remains
strategically necessary** for bank handoff (editable Word artifact, pinned master revision,
real `sectPr` / header/footer parts / `PAGE` fields surviving in the DOCX). **DOCX-as-layout
foundation is only partly earned today** — the engine under-uses the master, and several
constructs a real facility letter needs either render empty, renumber wrongly, or are
unauthorable in the UI. Closing the gap is mostly **making existing capabilities work and
be demonstrated**, not inventing a new product category (aligns with FOS "no new features"
and CRCH W3 fidelity depth).

---

## 2. Split: strategic necessity vs earned architecture

| Lens | Answer |
| --- | --- |
| **(a) Strategic** — banks need an editable `.docx` that opens in Microsoft Word, with letterhead chrome and legal reproducibility | **Yes — keep Word/DOCX.** ADR-0019 deliberately rejects HTML→DOCX as primary. |
| **(b) Earned today** — does this codebase + KEEP-8 demo *feel* like Word is load-bearing? | **No.** Visible surface is mostly engine-written body + a text-only letterhead a thin HTML template could fake. |

### What the master actually contributes at generate time

| Master feature | Fate | Evidence |
| --- | --- | --- |
| `sectPr` page size/margins/breaks | **Preserved** | `DocxWordCompatibilitySupport`, `DemoMasterDocxLayoutSupport` |
| Header/footer parts | **Preserved** (anchor-scanned only) | `DocxStructuredAnchorSupport.replaceInTablesHeadersAndFooters` |
| `PAGE` / `NUMPAGES` fields in DOCX | **Preserved** in stored DOCX | Footer XML in demo masters |
| Same fields when PDF stamping ON | **Stripped** and replaced by Helvetica stamp | `DocxPdfConversionPreprocessor` + `PdfPageNumberStamper` (default stamp **OFF** per D5) |
| `styles.xml` / `docDefaults` | **Preserved**; used for `styleRef` | `DocxAssembler`, `MasterDocxStyleCatalogParser` |
| Anchor paragraph runs / `rPr` | **Cleared**, not cloned | `StructuredContentDocxWriter.clearParagraph` |
| Structured tables | **Rebuilt bare** (no borders/widths/merges) | CRCH W3-2; `widthPct` parsed then ignored |
| Catalog style if name exists in master | **Silently skipped** | `DocxMasterStyleRegistry` |
| Absolute seal placement | **Out of scope** (D4 inline) | CRCH W0-7 re-scopes dishonest BLOCKER |

---

## 3. KEEP-8 demo honesty table

| Demo | Master size / craft | Body overlay | Header/footer | Verdict |
| --- | --- | --- | --- | --- |
| Annual review / Facility renewal | ~4.4 KB; 0 images; 0 `pStyle`; multi-`sectPr` with hdr/ftr often only on last | Rich nodes (cond/loop/table refs) | Text-only brand | **mid** |
| Commitment / Covenant / Credit / Amendment / Formal demand | ~4.3–4.4 KB scaffolds | Mid overlays | Text-only | **mid** |
| `CORP-FOL-OFFER` | ~5.7 KB; 85 paras of **anchor TOC**, mojibake in headings; 155 vars / 40 anchors / 36 clauses | 592 nodes; 2466 SQL paras **all `paragraph`** | Real PAGE fields; text-only hdr | **mid body / scaffold master** — volume ≠ layout |

P23 "Done" typography: style **presence** is asserted; spacing/widow/CJK from
`demo-bank-style-manifest.json` are **dead fields** (`DemoMasterDocxStyleSupport` never
reads them). Human typography checklist still pending; some checklist samples were purged
by KEEP-8.

---

## 4. New P0/P1 findings (beyond prior FOS/CRCH lists)

| Id | Sev | Finding | Disposition |
| --- | --- | --- | --- |
| **WF-1** | **P0** | `tableComponentRef` / `list` inside `conditionBlock`/`loopBlock` → **silent empty render** (no dispatch branch) | [FOS-W15](FOS-W15-word-foundation-honesty.md) **W15-1** — Remediate (fail-closed or render) |
| **WF-2** | **P0** | ≥3 block children in condition/loop can **reorder** (`writeInlineOrBlockChildren`) | FOS-W15 **W15-2** — Remediate |
| **WF-3** | **P0** | Clause `numbering.level` is a **literal prefix** that **restarts per anchor** — Formal Demand can show seven "1." headings; ADR-0019 duplicate-number gate is blind to assembly | FOS-W15 **W15-3** — Honesty (+ optional continuity; full Word `numPr` may stay CRCH/ADR) |
| **WF-4** | **P1** | FOL/demo `headerRows` PowerShell flatten → blank table headers in shipped JSON | FOS-W15 **W15-4** — Remediate demo |
| **WF-5** | **P1** | Style manifest `spacingAfterTwips` / CJK never applied; demos never use `directFormat` spacing | FOS-W15 **W15-5** (+ CRCH W3 coordination) |
| **WF-6** | **P1** | Typography regression asserts **POI-built master shell**, not generated letter | FOS-W15 **W15-6** — Gate honesty |
| **WF-7** | **P1** | No KEEP-8 package uses `COMPUTED` / `FORMAT_AMOUNT` / `SPELL_AMOUNT` — money is preformatted strings | FOS-W15 **W15-7** — Demo honesty (engine remains CRCH W2) |
| **WF-8** | **P1** | Author never *sees* Word after upload (no thumbnail / style specimen / page setup); KEEP-8 masters are JVM-POI skeletons | FOS-W15 **W15-8** — One live Word-crafted letterhead path |

Already owned elsewhere (do not duplicate): CRCH W3 table geometry / `rPr` / bookmarks;
CRCH W0-7 seal validation honesty; FOS W3 UI for list/contentModule/style scope; CRCH W2
compute DSL.

---

## 5. What must be true for Word-as-base to feel necessary

(Existing capability working + demonstrated — not a feature wishlist.)

1. Nested table/list in condition/loop **render in authored order** (WF-1/WF-2).
2. Document-level clause numbering is continuous and unique (WF-3) — or the product
   stops claiming "controlled numbering" until it is.
3. Declared table geometry (`widthPct`, borders, header repeat) survives or emits
   `PARTIAL_TABLE_LAYOUT_ADJUSTMENT` (CRCH W3-2 + W3-5).
4. At least **one** KEEP-8 letter uses a **real Word-crafted** master (logo in header,
   distinct first-page if claimed) uploaded through the product path — not only POI
   generators.
5. One KEEP-8 letter exercises `FORMAT_AMOUNT`/`SPELL_AMOUNT` (or documents that money
   is caller-owned until CRCH W2).
6. Typography gates assert the **generated** DOCX/PDF, not only the empty master shell.
7. Optional but decisive for stakeholders: designer edits letterhead in Word → re-upload
   → anchors refresh → preview shows the change (Word becomes visible in the journey).

---

## 6. Traceability

- Delivery leaf: [FOS-W15-word-foundation-honesty.md](FOS-W15-word-foundation-honesty.md) (TM **#185**)
- ADR-0019 structured authoring boundary
- CRCH §6 product position + W3 design
- FOS D8/D9 (inline seals; stamp default OFF)
- Demo catalog KEEP-8 detail sheet
- P23 typography plan (Done claims vs dead manifest fields)
