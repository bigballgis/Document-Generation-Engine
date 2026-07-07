# BDD Behavior Specification: Demo Document Typography & Layout Excellence

**Document status:** `ready`  
**Version:** 1.0.0  
**Authored:** 2026-07-08  
**BDD ID prefix:** `BDD-DEMO-TYP`  
**Delivery phase:** **P23-DEMO-TYPOGRAPHY-LAYOUT-EXCELLENCE** (extends [demo-expansion-behavior-spec.md](./demo-expansion-behavior-spec.md); does **not** duplicate `BDD-DEMO-EXP-001`…`015`)

---

## Table of contents

1. [Overview](#1-overview)
2. [Actor / roles](#2-actor--roles)
3. [North star and non-goals](#3-north-star-and-non-goals)
4. [Typography and layout acceptance dimensions](#4-typography-and-layout-acceptance-dimensions)
5. [Per demo type rules](#5-per-demo-type-rules)
6. [Trigger](#6-trigger)
7. [Preconditions](#7-preconditions)
8. [Primary journey](#8-primary-journey)
9. [System responses](#9-system-responses)
10. [Acceptance scenarios (Given / When / Then)](#10-acceptance-scenarios-given--when--then)
11. [Boundary and exception behavior](#11-boundary-and-exception-behavior)
12. [Observable evidence](#12-observable-evidence)
13. [BDD readiness declaration](#13-bdd-readiness-declaration)
14. [Traceability](#14-traceability)

---

## 1. Overview

P22 closed the **rendering engine** and **deploy scaffolds** for eight demo families (`deploy/demo-*`). User review (2026-07-08) confirms generated output still lacks **international large-bank letter-grade** Word typography, layout, fonts, and rich structured bindings — content exists but reads like a prototype.

**P23** closes that gap. Word is the **authoritative typesetting surface**. Generated DOCX must look and read like correspondence from an international wholesale/retail bank; only customer data remains mock.

| # | Capability domain | P22 baseline | P23 completes |
|---|-------------------|--------------|---------------|
| T1 | **Master style catalog** | Styles exist in engine catalog | Every demo master DOCX defines and applies bank-grade named styles |
| T2 | **Rich structured bindings** | Writer supports nodes; scaffolds use placeholders | All demo bindings use P18/P22 node matrix — no flat placeholder paragraphs |
| T3 | **Typography** | Single-style downgrade path removed at engine | Consistent body/heading fonts, point sizes, spacing, emphasis as Word runs |
| T4 | **Layout** | Section/dual page numbering mechanism | Per-letter-type margins, headers, footers, signature blocks, schedule tables |
| T5 | **Font baseline (Docker)** | Host dev fonts | CJK + metric-compatible Latin (Carlito/Caladea class) — **LR-A2** / P23-T02 |
| T6 | **Verifiable acceptance** | Contract/regression tests | POI/XML assertions + Playwright runtime generate + human typography checklist |

**Relationship to P22 BDD:** `BDD-DEMO-EXP-*` proves the **engine** (writer, dual page numbers, import chain). `BDD-DEMO-TYP-*` proves **demo package content and Word polish** at bank-letter grade. Where both apply, TYP scenarios add **content-depth and visual-quality** assertions beyond EXP.

---

## 2. Actor / roles

| Actor | Description | Scope / permissions |
|-------|-------------|---------------------|
| **Demo author (Platform Engineer)** | Maintains `deploy/demo-*` packages: master assets, binding overlays, SQL seeds, import scripts | Repository `deploy/`; build-time `*MasterDocxAssetGeneratorTest` |
| **Master designer** | Reviews bank-grade master DOCX: styles, margins, header/footer slots | `MASTER_DESIGNER`; group scope CORP / RETAIL / TRADE / WEALTH |
| **Template author** | Configures anchor structured content, variables, executive test datasets | `canAuthorTemplates`; demo group alignment |
| **Runtime caller** | Invokes published demo templates via runtime generate API | Template-level API credential + AD Group |
| **Typography reviewer** | Signs human checklist for fundraising/demo evidence | Read-only on generated samples; no production secrets |
| **System (Rendering pipeline)** | `StructuredContentDocxWriter`, `DocxAssembler`, PDF conversion, Docker font stack | Internal; `renderProfile` publish-locked |

---

## 3. North star and non-goals

### North star

Every generated demo document must **look and read like correspondence from an international wholesale/retail bank** — not a prototype. Specifically:

- Professional **Word typography** (named styles, fonts, line spacing, widow/orphan-safe paragraph spacing).
- **Bank-grade layout** (margins, branded headers/footers, page numbering profiles per letter type, signature blocks, styled schedule tables).
- **Rich structured bindings** rendered via the P22 writer (`styleRef`, lists, tables, conditions, loops, module refs) — **not** placeholder plain text.
- **Font-faithful Docker output** — mixed CJK/Latin content without tofu; PDF preview aligned with intent (**LR-A2**).
- **Verifiable acceptance** — automated POI/XML + Playwright E2E + optional signed human checklist.

### Non-goals

| Item | Rationale |
|------|-----------|
| Reopen P22 phase status | P22 honestly closed engine + scaffold; track under P23-T01…T16 |
| Real customer data | All names, accounts, amounts remain mock variables |
| Runtime caller style overrides | `renderProfile` remains publish-locked per ADR |
| Management UI typography redesign | P23 targets **generated DOCX**, not the admin shell |
| Perfect Word↔PDF pagination parity | ADR-0042 budget applies via **LR-A7** after corpus exists; P23 enables corpus |
| Replacing Word with HTML/CSS layout | Word remains authoritative typesetting surface |

---

## 4. Typography and layout acceptance dimensions

### 4.1 Named styles (master catalog)

Each demo master DOCX **must** define at minimum:

| Style key | Purpose | Acceptance hints |
|-----------|---------|------------------|
| `Heading1`–`Heading3` | Document and clause hierarchy | Outline levels; distinct font size/weight vs body |
| `ClauseBody` | Operative clause paragraphs | Default body font; first-line indent or spacing per bank convention |
| `DefinedTerm` | Defined terms blocks (CORP) | Bold or small-caps emphasis via style, not ad-hoc runs only |
| `TableHeader` | Schedule/checklist header rows | Distinct from body; repeat on table page breaks where configured |
| `ScheduleTitle` | Schedule section titles | Between heading and table |
| `SignatureBlock` | Closing signature area | Fixed spacing; alignment suitable for wet-sign / stamp area |
| Product variants | e.g. `RegulatoryEmphasis`, `DisclaimerBody` | Per demo README |

Shared manifest documented under `deploy/demo-shared/` (**P23-T03**).

### 4.2 Fonts

| Context | Requirement | Verification |
|---------|-------------|--------------|
| Latin body | Metric-compatible with Calibri class (Carlito or verified substitute) | POI `w:rFonts` in `document.xml`; Docker `fc-list` |
| Latin headings | Metric-compatible with Cambria class (Caladea or verified substitute) | POI heading vs body font names differ (**BDD-DEMO-TYP-014**) |
| CJK mixed content | Noto Sans CJK SC (or approved substitute) in Docker images | No tofu in PDF smoke; `RenderingFontSmokeTest` |
| Fallback | No silent downgrade to single undifferentiated 10pt run for styled paragraphs | POI run property assertions |

### 4.3 Margins and page setup

- Default side margins **≥ 2.54 cm** (1 inch) unless product README documents an approved bank variant.
- Header/footer distance from edge documented per demo package.
- Section breaks and `pageNumberingProfile` per [demo-expansion-behavior-spec §12](./demo-expansion-behavior-spec.md#12-演示包结构契约).

### 4.4 Headers and footers

| Profile | Semantics | Typical demo families |
|---------|-----------|----------------------|
| `GLOBAL_ONLY` | Document-global page fields only | Retail account, collection, trade LC, wealth |
| `SECTION_AND_GLOBAL` | Section `PAGE`/`SECTIONPAGES` + document `PAGE`/`NUMPAGES` | FOL, credit-limit, mortgage, annual review |
| `SECTION_ONLY` | Section-local only (rare) | Not default for P23 demos |

Footer **content** (bank address, disclaimers, regulatory lines) lives in **master assets**, not runtime string concatenation.

### 4.5 Tables

- Schedule/checklist tables use `tableComponent` / `tableComponentRef` with `TableHeader` styled header row.
- Column alignment and borders visible in Word (not pipe-delimited text).
- Repeat header row on continuation pages where `repeatHeader` metadata is set.

### 4.6 Signature blocks

- Dedicated `SignatureBlock` style or anchored table with authorized signatory lines, title, date placeholder variables.
- Seal/image refs remain within master authorized seal area (**extends BDD-DEMO-EXP-015**).

---

## 5. Per demo type rules

Priority order matches import chain. `externalId` is the import contract.

| Priority | Package | externalId(s) | P23 task | Min variables | Header/footer / typography notes |
|----------|---------|---------------|----------|---------------|----------------------------------|
| 1 | `deploy/demo-credit-limit/` | `DEMO-CREDIT-LIMIT-CONFIRM` | T04 | ≥20 | CORP dual-page; defined terms block; covenant table; `SECTION_AND_GLOBAL` |
| 2 | `deploy/demo-mortgage/` | `DEMO-MORTGAGE-APPROVAL` | T05 | ≥20 | Amortization schedule table; `SECTION_AND_GLOBAL` |
| 3 | `deploy/demo-trade-lc/` | `DEMO-TRADE-LC-NOTICE`, `DEMO-TRADE-GUARANTEE-NOTICE` | T06 | ≥20 | Document checklist table; logo/seal refs; `GLOBAL_ONLY` + attachment section |
| 4 | `deploy/demo-collection/` | `DEMO-RATE-CHANGE-NOTICE`, `DEMO-OVERDUE-COLLECTION` | T07 | ≥15 | Regulatory emphasis; disclaimer footer; `GLOBAL_ONLY` |
| 5 | `deploy/demo-annual-review/` | `DEMO-ANNUAL-REVIEW`, `DEMO-FACILITY-RENEWAL` | T08 | ≥20 | Covenant loop/table; `SECTION_AND_GLOBAL` |
| 6 | `deploy/demo-wealth/` | `DEMO-WEALTH-STATEMENT` | T09 | ≥20 | Multi-table holdings; footer totals; `GLOBAL_ONLY` |
| 7 | `deploy/demo-retail-account/` | `DEMO-RETAIL-ACCOUNT-OPEN`, `DEMO-RETAIL-ACCOUNT-BALANCE` | T10 | ≥15 | Fee schedule table; branch address footer; `GLOBAL_ONLY` |
| 8 | `deploy/demo-fol/` | `DEMO-FOL-WHOLESALE`, `CORP-FOL-OFFER` | T11 | (existing scale) | Style manifest alignment; dual-page; ≥100 pages executive |
| 9 | `deploy/demo-full-flow/` | `DEMO-FULL-FLOW-LETTER` | T11 | (existing) | Retail letter polish; same style manifest as family demos |

**Rich binding minimum per package (post-rewrite):** at least three anchors must use **non-trivial** structured nodes (`sectionHeading`, `styleRef`, `emphasis`, `tableComponentRef`, `conditionBlock`, or `loopBlock`) — not sole `textRun`/`variable` flat paragraphs.

---

## 6. Trigger

| Trigger | Description |
|---------|-------------|
| **T1 — Build-time master generation** | `mvn test` → `*MasterDocxAssetGeneratorTest` writes/regenerates `deploy/demo-*/assets/*.docx` |
| **T2 — Demo import** | `deploy/import-all-demos.ps1` or per-package `import-*-demo.ps1` |
| **T3 — Publish orchestration** | `deploy/publish-all-demos.ps1` after rewrites (**P23-T12**) |
| **T4 — Runtime generate** | `POST .../templates/{id}/versions/{release}/generate` with executive test variables |
| **T5 — Acceptance suite** | JUnit POI typography suite (**P23-T15**); Playwright `demo-runtime-generate.spec.ts` (**P23-T13**) |
| **T6 — Human review** | Typography reviewer completes checklist (**P23-T16**) |

---

## 7. Preconditions

1. P22 **Done** — `StructuredContentDocxWriter`, dual page numbering, eight `deploy/demo-*` scaffolds, `import-all-demos.ps1`.
2. P18 node matrix and default master style catalog exist; `renderProfile` publish-locked.
3. Docker stack healthy for acceptance (`backend /healthz`, UI `4173`) when running E2E/import.
4. Demo AD groups seeded: `CORP`, `RETAIL`, `TRADE`, `WEALTH` (or manifest equivalents).
5. **P23-T03 style manifest** available before package rewrites (Wave 1); font baseline (**P23-T02** / LR-A2) before tofu-sensitive PDF assertions.

---

## 8. Primary journey

### 8.1 Demo author — style manifest and package rewrite

1. Author or update shared style manifest in `deploy/demo-shared/` (**T03**).
2. Extend `*MasterDocxAssetGeneratorTest` to assert bank style set, margins, footer field codes.
3. Rewrite binding overlays + SQL for each demo family (**T04–T10**); bump `catalogMarker` / `masterLayoutVersion`.
4. Align FOL and full-flow packages to manifest (**T11**).
5. Run `import-all-demos.ps1` → `publish-all-demos.ps1` (**T12**).

### 8.2 Runtime caller — generate bank-grade sample

1. Call runtime generate with package executive test variables and authorized credential.
2. Receive DOCX (and PDF if configured); verify no placeholder markers in extracted text.
3. Optional: download via manifest script with SHA-256 evidence (**T14**).

### 8.3 Typography reviewer — human evidence

1. Open ≥2 CORP + ≥2 RETAIL generated samples from evidence bundle.
2. Complete checklist in `docs/evidence/demo-typography-review-checklist.md` (**T16**).
3. Sign off for fundraising/demo use.

---

## 9. System responses

### 9.1 Master assets

- Build-time generators produce master DOCX with full style catalog, margin `sectPr`, and header/footer slots matching `pageNumberingProfile`.
- Package README documents style keys and layout decisions.

### 9.2 Rich bindings

- Import binds structured overlays; validation passes with **zero errors** before publish.
- Generated DOCX reflects node types — not consolidated placeholder paragraphs.

### 9.3 Font baseline (Docker)

- Backend and rendering Docker images include CJK + Latin metric-compatible fonts.
- `RenderingFontSmokeTest` and demo mixed-script samples pass without tofu.

### 9.4 Runtime generate

- HTTP 200 for all published demo templates with executive fixtures.
- DOCX size floor per template (anti-empty-output guard in E2E).
- Audit records `SUCCESS` for generate script driver (**T14**).

### 9.5 Acceptance artifacts

- POI JUnit suite green (**T15**).
- Playwright runtime generate spec green (**T13**).
- Human checklist archived with sample paths under `.tmp/evidence/` (**T16**).

---

## 10. Acceptance scenarios (Given / When / Then)

> **Note:** Scenarios **001–015** map to the P23 plan outline; **016–020** extend coverage for catalog presence, rich bindings matrix, margins, FOL/full-flow alignment, and human checklist breadth.

### 10.1 Styles — ClauseBody

```gherkin
Scenario: BDD-DEMO-TYP-001 generated DOCX applies ClauseBody to operative paragraphs
  Given a P23-rewritten demo template (e.g. credit-limit) with binding paragraphs using styleRef "ClauseBody"
  When  DOCX is generated with the package executive test dataset
  Then  operative clause paragraphs use Word style ID linked to "ClauseBody"
  And   POI inspection shows ClauseBody paragraphs are not styled as Normal-only fallback
  And   extracted text does not contain "LOREM" or "{{placeholder}}"
```

### 10.2 Styles — headings

```gherkin
Scenario: BDD-DEMO-TYP-002 headings use Heading1–Heading3 with correct hierarchy
  Given a demo master with Heading1, Heading2, and Heading3 in its style catalog
    And bindings use sectionHeading nodes mapped to those style keys
  When  DOCX is generated
  Then  heading paragraphs use distinct Word style IDs for H1, H2, H3 respectively
  And   heading outline levels increase monotonically in document order
  And   heading font size or weight differs from ClauseBody per POI run/style inspection
```

### 10.3 Tables — TableHeader

```gherkin
Scenario: BDD-DEMO-TYP-003 table rows use TableHeader style
  Given a mortgage or trade-LC demo binding with a schedule or checklist tableComponent
  When  DOCX is generated with sample variables
  Then  the table header row cells use TableHeader style or equivalent table header formatting
  And   the table is a real XWPFTable with row count >= header + data rows
  And   the table is not pipe-delimited plain text in the anchor
```

### 10.4 Signature block

```gherkin
Scenario: BDD-DEMO-TYP-004 signature block uses dedicated style and spacing
  Given a credit-limit or annual-review demo with a signature anchor using SignatureBlock style
  When  DOCX is generated
  Then  the signature area uses SignatureBlock style or a dedicated signature table layout
  And   signatory name and title variables appear in separate lines with bank-standard spacing
  And   the signature block is visually separated from preceding clause body (spacing or rule)
```

### 10.5 Emphasis and underline as Word runs

```gherkin
Scenario: BDD-DEMO-TYP-005 emphasis and underline render as Word runs not plain text
  Given a collection or credit-limit binding with emphasis(bold) and underline on amount or defined terms
  When  DOCX is generated
  Then  the target paragraphs contain runs with bold and underline properties respectively
  And   the paragraph is not a single undifferentiated default-font run for that content
  And   fidelity warnings omit CONTROLLED_STYLE_FALLBACK for those nodes
```

### 10.6 styleRef resolution

```gherkin
Scenario: BDD-DEMO-TYP-006 styleRef resolves to master catalog style ID
  Given an approved demo master listing "DefinedTerm" in its style catalog
    And a binding paragraph with styleRef "DefinedTerm"
  When  DOCX is generated
  Then  the paragraph Word style matches the catalog entry for DefinedTerm
  And   POI styles.xml contains the DefinedTerm style definition
  And   publish validation reports no missing-style blocker
```

### 10.7 Retail footer — GLOBAL_ONLY

```gherkin
Scenario: BDD-DEMO-TYP-007 retail demo uses GLOBAL_ONLY footer layout
  Given the retail account or collection demo with pageNumberingProfile GLOBAL_ONLY
  When  DOCX is generated
  Then  the footer contains retail-appropriate address and customer service lines per master asset
  And   the footer does not contain wholesale FOL-specific dual-page disclaimer text
  And   page fields reflect global numbering only (no SECTIONPAGES in retail footer when profile is GLOBAL_ONLY)
```

### 10.8 CORP dual-page footer

```gherkin
Scenario: BDD-DEMO-TYP-008 CORP demo exposes dual page number fields
  Given a credit-limit, mortgage, or annual-review demo with pageNumberingProfile SECTION_AND_GLOBAL
    And a master footer configured for dual page numbers
  When  DOCX is generated with enough content to span multiple pages and sections
  Then  footer XML contains both section-local and document-global page field constructs
  And   a page in section 2 shows section-local page index >= 1
  And   the same page shows document-global page index greater than section-1 page count
```

### 10.9 CJK font — no tofu

```gherkin
Scenario: BDD-DEMO-TYP-009 CJK sample renders without tofu in Docker
  Given Docker images include Noto Sans CJK SC (or approved substitute) per LR-A2 / P23-T02
    And a demo binding includes mixed Chinese and Latin party names or addresses
  When  DOCX is generated inside Docker and converted to PDF
  Then  PDF text extraction shows intended CJK characters
  And   RenderingFontSmokeTest passes
  And   no replacement glyph boxes (tofu) appear in the rendered PDF sample
```

### 10.10 Latin metric-compatible font

```gherkin
Scenario: BDD-DEMO-TYP-010 Latin body uses metric-compatible font
  Given Docker images include Carlito or verified Calibri-metric substitute
  When  DOCX is generated for any P23-rewritten demo
  Then  POI inspection of document.xml shows Latin body runs reference the approved Latin font family
  And   the font name is not a missing-font fallback sentinel in Docker acceptance tests
```

### 10.11 All demo families import and generate

```gherkin
Scenario: BDD-DEMO-TYP-011 all eight demo families import and generate after rewrite
  Given a fresh Docker deployment with P23-rewritten packages
  When  import-all-demos.ps1 and publish-all-demos.ps1 complete successfully
  Then  each row in §5 per demo type matrix has a published template with listed externalId
  And   each template has an executive test dataset
  And   preview or runtime generate succeeds for DOCX for each type
```

### 10.12 Runtime E2E — DOCX size floor

```gherkin
Scenario: BDD-DEMO-TYP-012 runtime E2E generates DOCX above size floor per template
  Given published demo templates with P23 executive fixtures in Playwright test data
  When  demo-runtime-generate.spec.ts calls runtime generate for each externalId
  Then  each response is HTTP 200 with DOCX content type
  And   each DOCX byte size is >= the configured floor for that template (anti-scaffold-empty guard)
  And   response metadata includes traceId or auditId for correlation
```

### 10.13 No placeholder markers

```gherkin
Scenario: BDD-DEMO-TYP-013 generated text contains no placeholder markers
  Given P23-rewritten bindings and executive test variables
  When  DOCX is generated for each demo family
  Then  plain-text extraction of document body does not contain "LOREM", "TODO", "{{", "}}", or "placeholder"
  And   no anchor is bound solely to a single static placeholder sentence
  And   generate script manifest lists SHA-256 per output file without placeholder failures
```

### 10.14 POI — heading vs body fonts differ

```gherkin
Scenario: BDD-DEMO-TYP-014 POI asserts w:rFonts differ for heading vs body
  Given a generated demo DOCX from P23 acceptance suite
  When  POI parses document.xml for a Heading1 paragraph and an adjacent ClauseBody paragraph
  Then  w:rFonts entries for heading runs differ from body runs in ascii or eastAsia theme
  And   the assertion fails if both use identical undifferentiated default font only
```

### 10.15 Human checklist — pilot samples

```gherkin
Scenario: BDD-DEMO-TYP-015 human checklist passes for CORP-FOL and credit-limit samples
  Given generated CORP-FOL-OFFER and DEMO-CREDIT-LIMIT-CONFIRM samples in the evidence bundle
  When  a typography reviewer completes demo-typography-review-checklist.md
  Then  all mandatory checklist items pass for both samples
  And   signed checklist is archived under docs/evidence/ or .tmp/evidence/
  And   no mandatory item is waived without documented rationale
```

### 10.16 Master style catalog presence

```gherkin
Scenario: BDD-DEMO-TYP-016 master DOCX embeds full bank style catalog at build time
  Given *MasterDocxAssetGeneratorTest for a P23 demo package
  When  the test generates the master DOCX asset
  Then  styles.xml contains Heading1, Heading2, Heading3, ClauseBody, TableHeader, and SignatureBlock
  And   the test asserts margin pgMar values meet >= 2.54 cm side baseline
  And   footer field codes match the package pageNumberingProfile
```

### 10.17 Rich binding nodes — not placeholder

```gherkin
Scenario: BDD-DEMO-TYP-017 demo bindings use rich structured nodes not flat placeholder text
  Given P23-rewritten binding-overlays.json for any demo package
  When  the binding overlay is validated and imported
  Then  at least three anchors use sectionHeading, styleRef, tableComponentRef, conditionBlock, or loopBlock
  And   no anchor binding consists only of a single textRun with static lorem ipsum
  And   variable count meets the §5 minimum for that package
```

### 10.18 Page margins baseline

```gherkin
Scenario: BDD-DEMO-TYP-018 document margins conform to bank baseline
  Given a generated DOCX from any P23-rewritten demo
  When  POI reads sectPr pgMar for the primary document section
  Then  left and right margins are >= 1440 twips (2.54 cm) unless package README documents approved variant
  And   top and bottom margins are >= 1440 twips unless documented otherwise
  And   margin assertion is included in P23-T15 JUnit suite
```

### 10.19 FOL and full-flow manifest alignment

```gherkin
Scenario: BDD-DEMO-TYP-019 FOL and full-flow align with shared style manifest
  Given deploy/demo-shared/ style manifest from P23-T03
  When  FOL and full-flow masters are regenerated and imported
  Then  both packages use the same Heading and ClauseBody style keys as the shared manifest
  And   catalogMarker or masterLayoutVersion is bumped to reflect P23 content change
  And   FOL retains executive scale (>= 100 pages or folPageTarget) with rich nodes in multiple anchors
```

### 10.20 Human checklist — breadth

```gherkin
Scenario: BDD-DEMO-TYP-020 human typography review covers >=2 CORP and >=2 RETAIL samples
  Given generated samples from at least two CORP templates (e.g. FOL, credit-limit)
    And generated samples from at least two RETAIL templates (e.g. mortgage, retail-account)
  When  typography reviewer completes the full checklist for all four samples
  Then  all mandatory typography, layout, font, and footer items pass
  And   evidence bundle indexes sample paths, SHA-256, and reviewer sign-off date
  And   checklist satisfies P23 exit criterion §3 item 5 for fundraising evidence
```

---

## 11. Boundary and exception behavior

| Scenario | Expected behavior |
|----------|-------------------|
| Missing style in master catalog | Publish/import blocked; build-time generator test fails first |
| Scaffold-only binding imported without P23 rewrite | POI/TYP-013 fails; E2E size floor may fail |
| Single-section retail letter | `GLOBAL_ONLY` — dual-page footer **not** required (**extends EXP §9**) |
| Font missing in Docker | `RenderingFontSmokeTest` fails; block LR-A2 / P23-T02 Done |
| Runtime unauthorized | `403 AUTHORIZATION`; fail-closed |
| Placeholder text in binding | TYP-013 fails; must not pass publish gate for demo evidence |
| PDF stamper disabled | PDF page numbers may degrade per renderProfile; DOCX TYP scenarios still required |
| Human checklist mandatory item fails | T16 evidence incomplete; phase exit criterion §3 item 5 not met |

---

## 12. Observable evidence

| Evidence type | Content | Primary tasks |
|---------------|---------|---------------|
| **Style manifest** | `deploy/demo-shared/` README + style key list | T03 |
| **Master assets** | `deploy/demo-*/assets/*.docx`; generator tests green | T03–T11 |
| **Font baseline** | Docker `fc-list`; `RenderingFontSmokeTest`; sample PDF | T02 |
| **Import/publish logs** | `import-all-demos.ps1`, `publish-all-demos.ps1` SUCCESS | T12 |
| **POI suite** | JUnit: styles, fonts, margins, footers, no placeholders | T15 |
| **E2E** | `demo-runtime-generate.spec.ts` HTTP 200 + size floors | T13 |
| **Generate manifest** | `.tmp/generated_*.docx` + SHA-256 manifest; audit SUCCESS | T14 |
| **Human checklist** | `docs/evidence/demo-typography-review-checklist.md` + signed samples | T16 |
| **Pagination corpus** | ≥5 letter-grade demos for LR-A7 | T04–T08 + doc-keeper |
| **Gates** | `mvn verify`; frontend lint/type-check/test/build; Docker deploy smoke | All |

---

## 13. BDD readiness declaration

| Item | Value |
|------|-------|
| **BDD readiness** | `ready` |
| **Blocking questions** | None (user strategic direction confirmed 2026-07-08) |
| **Handoff** | `plan-orchestrator` → Wave 0 parallel **P23-T02** (font) + **P23-T03** (style manifest) → **P23-T04** pilot rewrite |
| **Plan anchor** | [P23-demo-typography-layout-excellence.md](../plan/detail/P23-demo-typography-layout-excellence.md) |

---

## 14. Traceability

### 14.1 Source documents

| Document | Relationship |
|----------|--------------|
| [requirements-plan.md](./requirements-plan.md) | §已确认：演示文档排版与版式卓越 (2026-07-08) |
| [demo-expansion-behavior-spec.md](./demo-expansion-behavior-spec.md) | P22 engine BDD — extended, not duplicated |
| [P23 detail plan](../plan/detail/P23-demo-typography-layout-excellence.md) | Task breakdown T01–T16 |
| [P22 detail plan](../plan/detail/P22-demo-expansion-rendering-fidelity.md) | Engine baseline — **Done**; scaffold carry-forward |
| [LRP-A rendering trust](../plan/detail/LRP-A-rendering-trust-hardening.md) | LR-A2 font, LR-A7 pagination corpus |
| [domain-model.md](../domain/domain-model.md) | StructuredContentDocxWriter, demo package contract |
| [PRD.md](../product/PRD.md) | §6.5.1 anchors, §6.7 demo packages |

### 14.2 BDD scenario → P23 task mapping

| BDD ID | Primary tasks |
|--------|---------------|
| BDD-DEMO-TYP-001 | T03, T04, T15 |
| BDD-DEMO-TYP-002 | T03, T04…T11, T15 |
| BDD-DEMO-TYP-003 | T03, T05, T06, T09, T15 |
| BDD-DEMO-TYP-004 | T03, T04, T08, T15 |
| BDD-DEMO-TYP-005 | T04…T11, T15 |
| BDD-DEMO-TYP-006 | T04…T11, T15 |
| BDD-DEMO-TYP-007 | T07, T10, T15 |
| BDD-DEMO-TYP-008 | T04, T05, T08, T11, T15 |
| BDD-DEMO-TYP-009 | T02, T15 |
| BDD-DEMO-TYP-010 | T02, T15 |
| BDD-DEMO-TYP-011 | T12, T13 |
| BDD-DEMO-TYP-012 | T13 |
| BDD-DEMO-TYP-013 | T04…T11, T14, T15 |
| BDD-DEMO-TYP-014 | T03, T15 |
| BDD-DEMO-TYP-015 | T16 |
| BDD-DEMO-TYP-016 | T03, T15 |
| BDD-DEMO-TYP-017 | T04…T11 |
| BDD-DEMO-TYP-018 | T03, T15 |
| BDD-DEMO-TYP-019 | T11 |
| BDD-DEMO-TYP-020 | T16 |

### 14.3 P23 task → BDD coverage

| Task | BDD scenarios |
|------|---------------|
| P23-T01 | (this spec) |
| P23-T02 | 009, 010 |
| P23-T03 | 001, 002, 003, 004, 014, 016, 018 |
| P23-T04 | 001, 004, 005, 006, 008, 013, 017 |
| P23-T05 | 003, 005, 008, 013, 017 |
| P23-T06 | 003, 013, 017 |
| P23-T07 | 005, 007, 013, 017 |
| P23-T08 | 004, 006, 008, 013, 017 |
| P23-T09 | 003, 013, 017 |
| P23-T10 | 007, 013, 017 |
| P23-T11 | 008, 013, 019 |
| P23-T12 | 011 |
| P23-T13 | 011, 012 |
| P23-T14 | 013 |
| P23-T15 | 001–010, 013, 014, 016, 018 |
| P23-T16 | 015, 020 |

---

## Change log

| Version | Date | Description |
|---------|------|-------------|
| 1.0.0 | 2026-07-08 | Initial P23-T01 behavior spec — BDD-DEMO-TYP-001…020; readiness `ready` |
