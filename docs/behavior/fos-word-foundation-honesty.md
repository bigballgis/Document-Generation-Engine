# Behavior — FOS Word foundation honesty (W15 / #185)

**Status:** Confirmed for delivery  
**Task Master:** `#185`  
**Slice:** `fos-word-foundation-honesty`  
**Plan:** [FOS-W15-word-foundation-honesty.md](../plan/detail/FOS-W15-word-foundation-honesty.md)  
**Audit:** [fos-word-foundation-deep-review-2026-07.md](../plan/detail/fos-word-foundation-deep-review-2026-07.md)

## Actor / goal

Bank template authors and demo operators need Word output that is honest about nesting,
order, clause numbering, table headers, style-manifest fields, typography gates, money
formatting ownership, and letterhead craft (logo media).

## Confirmed behaviors

### WF-1 / W15-1 — Nested table/list

**Given** a `conditionBlock` or `loopBlock` whose children include `tableComponentRef` or `list`  
**When** the condition is true / loop iterates  
**Then** the table or list is rendered into the DOCX (not silently empty).  
**And** if a table/list somehow hits the single-node fallthrough path, assembly fails closed
with an actionable rendering error (never a blank success).

### WF-2 / W15-2 — Multi-child order

**Given** a parent with block children `[paragraph A, table, paragraph B]`  
**When** structured content is assembled  
**Then** DOCX body order is A → table → B.

### WF-3 / W15-3 — Clause numbering honesty

**Given** section headings with `numbering.level` or `displayNumber`  
**When** authors insert or inspect a heading in the structured editor  
**Then** the UI surfaces that clause numbers are **literal prefixes**, not Word automatic
multilevel lists, and continuity across anchors is not guaranteed.  
**And** product docs state the same honesty.

### WF-4 / W15-4 — Demo `headerRows`

**Given** KEEP-8 / FOL table components advertising headers  
**When** overlays are generated or consumed  
**Then** `headerRows` are nested row arrays with non-empty cell `value`s (writer also accepts
ConvertTo-Json flattened `[cell…]` and still populates headers).

### WF-5 / W15-5 — Style-manifest → OOXML

**Given** `demo-bank-style-manifest.json` declares `spacingAfterTwips` / `spacingBeforeTwips`
and `fonts.cjk`  
**When** demo masters apply shared bank styles  
**Then** `styles.xml` contains `w:spacing/@w:after|before` and `w:rFonts/@w:eastAsia` for those
fields (no dead advertising).

### WF-6 / W15-6 — Generated-letter typography gate

**Given** a typography regression for bank letter quality  
**When** the gate runs  
**Then** at least one assertion path runs structured generation and checks spacing/styles on
the **generated** DOCX — not only the empty master shell.

### WF-7 / W15-7 — Money formatter honesty

**Given** KEEP-8 demo packages  
**When** operators read the demo literacy path / catalog notes  
**Then** money formatting is documented as **caller-owned** (preformatted strings) until a
CRCH W2 demo path wires `FORMAT_AMOUNT` / `SPELL_AMOUNT` into KEEP-8. Engine capability
remains proven in golden corpus packages outside KEEP-8.

### WF-8 / W15-8 — Logo letterhead

**Given** the Meridian Wholesale FOL KEEP-8 master  
**When** the package is inspected  
**Then** it contains `word/media/*` logo bytes in the header and the catalog marks FOL
letterhead as `logo` (other KEEP-8 masters remain `text-only` unless upgraded).

## Nesting matrix (supported)

| Parent | Child table/list | Result |
| --- | --- | --- |
| `conditionBlock` (true), all-block children | `tableComponentRef` / `list` | Rendered via `writeBlockNodes` |
| `loopBlock` (non-empty list) | same | Rendered per iteration |
| Mixed inline + table under condition | `tableComponentRef` | Fail-closed (must use block children) |

## Out of scope

- Absolute seals (OD-FOS-1); PDF stamp default ON (OD-FOS-2)
- CRCH W3 table geometry / CRCH W2 compute DSL implementation
- Full Word `numPr` multilevel lists across the document

## Traceability

| Id | BDD / evidence |
| --- | --- |
| W15-1/2 | `StructuredContentNestedTableOrderTest` |
| W15-3 | `docs/behavior` + FE `sectionHeadingNumberingHonesty` |
| W15-4 | FOL overlays nested + writer normalize + PS repair |
| W15-5 | `DemoMasterDocxStyleSupport` + style support test |
| W15-6 | `DemoGeneratedLetterTypographyTest` |
| W15-7 | demo learner walkthrough + catalog note |
| W15-8 | FOL master `word/media` + catalog Letterhead column |
