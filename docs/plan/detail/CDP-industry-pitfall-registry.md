# CDP Industry Pitfall Registry

**Program:** [competitiveness-deepening-program.md](../competitiveness-deepening-program.md)  
**Wave CD-0:** Spec + ADR drafts (non-code)  
**Wave CD-1/3:** Implementation mitigations  
**Sources:** Industry literature (LibreOffice/PDF conversion, enterprise CCMS, bank doc automation), codebase audit (P22 gap spec, OPT seams)

---

## 1. Registry format

| Column | Meaning |
| --- | --- |
| **ID** | CD-PIT-NN |
| **Symptom** | What users/integration partners see |
| **Root cause** | Technical why |
| **Detection** | Automated test or manual check |
| **Mitigation** | Concrete fix |
| **Owner wave** | CD-0 spec / P22 / CD-HARD |
| **Doc anchor** | ADR or NFR section to create/update |

---

## 2. Pitfalls

### CD-PIT-01 — Font substitution in headless conversion

| Field | Content |
| --- | --- |
| **Symptom** | PDF page count differs from author Word; table columns clip; footer wraps |
| **Root cause** | Calibri/Cambria absent in Linux container; LibreOffice substitutes Liberation/Carlito with different metrics |
| **Detection** | `RenderingFontSmokeTest`: convert fixed-width sample DOCX; assert page count + golden PDF hash tolerance; CI fails if core fonts missing |
| **Mitigation** | Bake `ttf-mscorefonts-installer` or licensed fonts into `backend/Dockerfile` / LibreOffice sidecar; document in `deploy/README.md` |
| **Owner wave** | CD-HARD-T01 |
| **Doc anchor** | NFR §production rendering; new ADR `0041-rendering-font-baseline.md` (draft in CD-0) |

### CD-PIT-02 — Word vs LibreOffice layout engine divergence

| Field | Content |
| --- | --- |
| **Symptom** | Legally paginated documents shift page breaks in PDF |
| **Root cause** | No OSS JS engine reproduces Word layout; LO ≠ Word |
| **Detection** | Corpus of ≥5 demo letters: compare page count Word (manual baseline) vs LO PDF; record delta budget |
| **Mitigation** | (1) Author defensively: explicit table widths, avoid floating objects; (2) Publish **pagination delta budget** in NFR; (3) Fidelity warnings for page-count mismatch vs baseline |
| **Owner wave** | CD-0 doc + CD-HARD-T04 |
| **Doc anchor** | NFR §compatibility; PRD §6.5 footnote |

### CD-PIT-03 — OOXML strictness / escaping (LO 24+ «corrupt» DOCX)

| Field | Content |
| --- | --- |
| **Symptom** | Generated DOCX fails in LibreOffice 24+ until resaved; variables/fields missing |
| **Root cause** | Invalid XML characters, unescaped ampersands, malformed relationships |
| **Detection** | Open output with LO 24 headless; POI `OPCPackage.open` + schema validation; regression from PHPWord LO24 issue class |
| **Mitigation** | Central XML escaping in `StructuredContentDocxWriter`; never write raw user text into XML without escape |
| **Owner wave** | P22-T01, CD-HARD-T03 |
| **Doc anchor** | P22-T01 acceptance; ADR rendering appendix |

### CD-PIT-04 — Word list numbering (`numId` / `ilvl`) corruption

| Field | Content |
| --- | --- |
| **Symptom** | Nested lists restart at 1; mixed bullet/decimal wrong; section restart breaks |
| **Root cause** | POI numbering definitions not aligned with master `numbering.xml`; multiple abstract nums |
| **Detection** | `DocxAssemblerTest` extracts list labels from generated DOCX; FOL demo list anchors |
| **Mitigation** | `DocxListNumberingSupport` single registry; master generators seed numbering.xml; condition/loop re-sequence policy documented |
| **Owner wave** | P22-T01 |
| **Doc anchor** | demo-expansion BDD-001 list scenarios |

### CD-PIT-05 — Dual page number fields

| Field | Content |
| --- | --- |
| **Symptom** | Only global «Page N of M»; missing section «Page X of Y» |
| **Root cause** | Footer uses `PAGE` only; no `SECTIONPAGES`; PDF stamper overwrites section semantics |
| **Detection** | POI field code inspection + PDF text regex for both patterns |
| **Mitigation** | P22-T03 master footers; P22-T04 stamper respects `pageNumberingProfile` |
| **Owner wave** | P22-T03, P22-T04 |
| **Doc anchor** | demo-expansion BDD-005, BDD-006 |

### CD-PIT-06 — Synchronous LibreOffice on request thread

| Field | Content |
| --- | --- |
| **Symptom** | Timeouts under concurrent PDF requests; thread pool exhaustion |
| **Root cause** | `RuntimeGenerationService` inline conversion (OPT-F6) |
| **Detection** | Load test: N concurrent PDF generations; p95 latency |
| **Mitigation** | Bounded async pool + job status OR queue; timeout with retryable error |
| **Owner wave** | CD-HARD-T02 |
| **Doc anchor** | execution-sync-ledger seam; OPT-F6 |

### CD-PIT-07 — Rich-text boundary creep

| Field | Content |
| --- | --- |
| **Symptom** | Authors embed tables/layout in prose; paste brings unsupported HTML |
| **Root cause** | Rich text field asked to carry structure (headless CMS anti-pattern) |
| **Detection** | Paste E2E + binding validator flags; count of `CONTROLLED_STYLE_FALLBACK` |
| **Mitigation** | Enforce node matrix in UI; paste cleaning summary; block publish on disallowed nodes |
| **Owner wave** | P18 (Done UI) + CD-HARD-T05 binding wire |
| **Doc anchor** | authoring-first-principles §4; usability-review L82 |

### CD-PIT-08 — Edit preview mistaken for legal evidence

| Field | Content |
| --- | --- |
| **Symptom** | Testers approve based on fast preview; final DOCX differs |
| **Root cause** | UX does not distinguish preview modes |
| **Detection** | CD-E2E-T10; copy audit in `TemplateTestDataSetPanel` |
| **Mitigation** | Banner on edit preview; test/approve flows require final-path artifact download |
| **Owner wave** | CD-E2E-T08, CD-E2E-T10 |
| **Doc anchor** | usability-review L38–39 |

### CD-PIT-09 — Programmatic template fill fragility at scale

| Field | Content |
| --- | --- |
| **Symptom** | Batch generation drift; table column resize over hundreds of docs |
| **Root cause** | Auto-fit tables; implicit styles |
| **Detection** | Batch test on 50+ samples; byte-size and layout hash variance |
| **Mitigation** | Fixed table widths in `tableComponent`; styleRef mandatory for headings |
| **Owner wave** | P22-T01, demo packages |
| **Doc anchor** | PRD §6.5.1 |

### CD-PIT-10 — Two-UI review fragmentation

| Field | Content |
| --- | --- |
| **Symptom** | SMEs review PDF/email; authors edit in app — feedback loop breaks |
| **Root cause** | No in-app suggest/comment on full publication view |
| **Detection** | UX review checklist; user interviews post-launch |
| **Mitigation** | **v1:** Dashboard queues + inline open to exact anchor; **post-v1:** comment threads (defer — document in usability-review 待确认) |
| **Owner wave** | CD-0 doc decision |
| **Doc anchor** | usability-review §待确认 |

---

## 3. P22 task ↔ pitfall mapping

| P22 task | Must mitigate pits |
| --- | --- |
| P22-T01 | CD-PIT-03, 04, 07, 09 |
| P22-T02 | CD-PIT-04, 09 |
| P22-T03 | CD-PIT-05 |
| P22-T04 | CD-PIT-02, 05 |
| P22-T15 | All P22 pits + regression suite |

---

## 4. CD-0 ADR drafts to create (doc-keeper)

| ADR | Title | Decision outline |
| --- | --- | --- |
| `docs/adr/rendering-authoring/0041-rendering-font-baseline.md` | Font baseline for DOCX/PDF | Required font bundle in deploy images |
| `docs/adr/rendering-authoring/0042-pagination-delta-budget.md` | Word vs LO pagination | Acceptable page-count delta for v1 |
| `docs/adr/rendering-authoring/0043-ooxml-output-validation.md` | OOXML output gate | LO 24 open required in CI |

Status: **Proposed** until architecture-reviewer sign-off.

---

## 5. Exit gate (CD-0 pitfall wave)

- [ ] All CD-PIT-01…10 rows present (this file)
- [ ] Three ADR drafts created with Proposed status
- [ ] NFR §production rendering subsection added (font + pagination reference)
- [ ] P22 task table updated with pitfall column (CD-P22-PLAN)
