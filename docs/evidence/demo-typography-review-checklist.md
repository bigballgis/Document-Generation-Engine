# Demo Typography & Layout — Human Review Checklist

**Document status:** `ready` (template — reviewer sign-off **pending**)  
**Version:** 1.0.0  
**Authored:** 2026-07-08  
**Phase:** P23-DEMO-TYPOGRAPHY-LAYOUT-EXCELLENCE (**P23-T16**)  
**BDD:** `BDD-DEMO-TYP-015` (pilot samples), `BDD-DEMO-TYP-020` (breadth — ≥2 CORP + ≥2 RETAIL)

---

## Purpose

This checklist is the **human typography acceptance gate** for fundraising and demo evidence. It complements automated POI/JUnit assertions (**P23-T15**) and Playwright runtime generate E2E (**P23-T13**). A typography reviewer opens generated DOCX samples in Microsoft Word (or equivalent) and confirms bank-letter-grade visual quality.

**Non-goals:** Management UI review; perfect Word↔PDF pagination parity (see ADR-0042 / LR-A7).

---

## Prerequisites

1. Docker stack healthy (`http://localhost:8080/healthz`, UI `http://localhost:4173`).
2. Demos imported and published:
   ```powershell
   .\deploy\import-all-demos.ps1 -BackendUrl http://localhost:8080
   .\deploy\publish-all-demos.ps1 -BackendUrl http://localhost:8080
   ```
3. Generated samples available:
   ```powershell
   .\deploy\generate-all-demos.ps1 -BackendUrl http://localhost:8080
   ```
4. Evidence manifest: `.tmp/evidence/generated-docx-manifest.json` (SHA-256 per template).

---

## Mandatory sample set (BDD-DEMO-TYP-020)

Complete the full checklist **for each sample** below. All four are **mandatory** before fundraising sign-off.

| # | Segment | externalId | Sample path (after generate) | pageNumberingProfile |
| --- | --- | --- | --- | --- |
| 1 | **CORP** | `CORP-FOL-OFFER` | `.tmp/generated_CORP-FOL-OFFER.docx` | SECTION_AND_GLOBAL |
| 2 | **CORP** | `DEMO-CREDIT-LIMIT-CONFIRM` | `.tmp/generated_DEMO-CREDIT-LIMIT-CONFIRM.docx` | SECTION_AND_GLOBAL |
| 3 | **RETAIL** | `DEMO-MORTGAGE-APPROVAL` | `.tmp/generated_DEMO-MORTGAGE-APPROVAL.docx` | SECTION_AND_GLOBAL |
| 4 | **RETAIL** | `DEMO-RETAIL-ACCOUNT-OPEN` | `.tmp/generated_DEMO-RETAIL-ACCOUNT-OPEN.docx` | GLOBAL_ONLY |

**Optional breadth samples** (recommended for full demo pack; same checklist applies):

| externalId | Segment | Sample path |
| --- | --- | --- |
| `DEMO-FULL-FLOW-LETTER` | RETAIL | `.tmp/generated_DEMO-FULL-FLOW-LETTER.docx` |
| `DEMO-RETAIL-ACCOUNT-BALANCE` | RETAIL | `.tmp/generated_DEMO-RETAIL-ACCOUNT-BALANCE.docx` |
| `DEMO-ANNUAL-REVIEW` | CORP | `.tmp/generated_DEMO-ANNUAL-REVIEW.docx` |
| `DEMO-FACILITY-RENEWAL` | CORP | `.tmp/generated_DEMO-FACILITY-RENEWAL.docx` |
| `DEMO-TRADE-LC-NOTICE` | TRADE | `.tmp/generated_DEMO-TRADE-LC-NOTICE.docx` |
| `DEMO-TRADE-GUARANTEE-NOTICE` | TRADE | `.tmp/generated_DEMO-TRADE-GUARANTEE-NOTICE.docx` |
| `DEMO-RATE-CHANGE-NOTICE` | RETAIL | `.tmp/generated_DEMO-RATE-CHANGE-NOTICE.docx` |
| `DEMO-OVERDUE-COLLECTION` | RETAIL | `.tmp/generated_DEMO-OVERDUE-COLLECTION.docx` |
| `DEMO-WEALTH-STATEMENT` | WEALTH | `.tmp/generated_DEMO-WEALTH-STATEMENT.docx` |

Copy signed-off samples to `.tmp/evidence/generated-docx/<externalId>.docx` for a self-contained bundle.

---

## Reviewer information

| Field | Value |
| --- | --- |
| Reviewer name | _pending_ |
| Review date (UTC) | _pending_ |
| Word version / platform | _e.g. Microsoft Word 365 / Windows 11_ |
| Docker deploy tag / commit | _e.g. git SHA at generate time_ |
| Manifest SHA-256 verified | ☐ Yes — against `.tmp/evidence/generated-docx-manifest.json` |

---

## Checklist — per sample

Mark **Pass**, **Fail**, or **N/A** (with rationale). **Fail** on any mandatory item blocks fundraising sign-off unless waived with documented rationale (see §Waivers).

### A. Fonts & typography

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| A1 | Body text uses a professional serif/sans bank body font (Calibri class or metric substitute); not default Times-only fallback | ☐ | ☐ | ☐ | |
| A2 | Heading1–Heading3 visually distinct from body (size, weight, or font family) | ☐ | ☐ | ☐ | |
| A3 | Mixed Latin + CJK party names/addresses render without tofu (□ boxes) | ☐ | ☐ | ☐ | Skip if sample has no CJK |
| A4 | Defined terms / regulatory emphasis use styled emphasis (bold/underline), not plain undifferentiated text | ☐ | ☐ | ☐ | |
| A5 | No visible placeholder markers (`LOREM`, `{{`, `}}`, `TODO`, `placeholder`) in body | ☐ | ☐ | ☐ | Automated TYP-013 also covers |

### B. Named styles & paragraph layout

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| B1 | Operative clauses use `ClauseBody` (or equivalent bank body style), not Normal-only | ☐ | ☐ | ☐ | |
| B2 | Section headings use `Heading1`–`Heading3` hierarchy in logical order | ☐ | ☐ | ☐ | |
| B3 | Paragraph spacing readable — not cramped single-line blocks; widow/orphan acceptable | ☐ | ☐ | ☐ | |
| B4 | Signature block uses `SignatureBlock` style or dedicated layout with clear separation from body | ☐ | ☐ | ☐ | |
| B5 | Disclaimer / regulatory blocks visually distinct where product requires | ☐ | ☐ | ☐ | |

### C. Margins & page setup

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| C1 | Left/right margins ≥ 2.54 cm (1 inch) — no text crowding page edge | ☐ | ☐ | ☐ | |
| C2 | Top/bottom margins adequate for letterhead and footer clearance | ☐ | ☐ | ☐ | |
| C3 | Multi-page documents: no clipped headers/footers on continuation pages | ☐ | ☐ | ☐ | |

### D. Headers & footers

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| D1 | Header contains appropriate bank branding / letter title (per master asset) | ☐ | ☐ | ☐ | |
| D2 | Footer contains bank-appropriate address, disclaimer, or regulatory lines (not runtime concatenation artifacts) | ☐ | ☐ | ☐ | |
| D3 | **GLOBAL_ONLY** samples: global page numbering only; no inappropriate wholesale dual-page disclaimer | ☐ | ☐ | ☐ | RETAIL GLOBAL_ONLY only |
| D4 | **SECTION_AND_GLOBAL** samples: section-local and document-global page numbers both visible where multi-section | ☐ | ☐ | ☐ | CORP dual-page only |
| D5 | Page numbers update correctly when scrolling (fields refreshed) | ☐ | ☐ | ☐ | |

### E. Tables & schedules

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| E1 | Schedule/checklist tables are real Word tables (not pipe-delimited plain text) | ☐ | ☐ | ☐ | |
| E2 | Table header row visually distinct (`TableHeader` style or equivalent) | ☐ | ☐ | ☐ | |
| E3 | Column alignment and borders professional; data readable | ☐ | ☐ | ☐ | |
| E4 | Multi-page tables: header row repeats on continuation (where configured) | ☐ | ☐ | ☐ | |

### F. Signatures & seals

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| F1 | Signatory name, title, and date appear on separate lines with bank-standard spacing | ☐ | ☐ | ☐ | |
| F2 | Seal/image refs (if present) sit in authorized area without overlapping body text | ☐ | ☐ | ☐ | |

### G. Overall bank-letter grade

| ID | Item | Pass | Fail | N/A | Notes |
| --- | --- | --- | --- | --- | --- |
| G1 | Document reads like **international bank correspondence**, not a prototype or scaffold | ☐ | ☐ | ☐ | |
| G2 | Variable substitution complete — no unfilled merge fields visible | ☐ | ☐ | ☐ | |
| G3 | **Sample approved for fundraising/demo use** | ☐ | ☐ | ☐ | |

---

## Per-sample sign-off summary

| externalId | Segment | Mandatory? | A–G all Pass? | Reviewer initials | Date |
| --- | --- | --- | --- | --- | --- |
| `CORP-FOL-OFFER` | CORP | **Yes** | _pending_ | | |
| `DEMO-CREDIT-LIMIT-CONFIRM` | CORP | **Yes** | _pending_ | | |
| `DEMO-MORTGAGE-APPROVAL` | RETAIL | **Yes** | _pending_ | | |
| `DEMO-RETAIL-ACCOUNT-OPEN` | RETAIL | **Yes** | _pending_ | | |

---

## Waivers

| Item ID | Rationale | Approved by | Date |
| --- | --- | --- | --- |
| _none_ | | | |

> **Rule:** No mandatory item may be waived without documented rationale. Waivers block BDD-DEMO-TYP-015/020 pass until resolved.

---

## Evidence archive

After review, archive:

| Artifact | Path |
| --- | --- |
| Completed checklist (this file, filled) | `docs/evidence/demo-typography-review-checklist.md` or `.tmp/evidence/demo-typography-review-checklist-signed.md` |
| Generated DOCX copies | `.tmp/evidence/generated-docx/<externalId>.docx` |
| SHA-256 manifest | `.tmp/evidence/generated-docx-manifest.json` |
| Audit records | `.tmp/evidence/audit-records/<externalId>.json` |
| Evidence index | [fundraising-demo-summary.md](./fundraising-demo-summary.md) |

---

## Traceability

| Document | Relationship |
| --- | --- |
| [demo-typography-layout-behavior-spec.md](../requirements/demo-typography-layout-behavior-spec.md) | BDD-DEMO-TYP-015, TYP-020 |
| [P23 detail plan](../plan/detail/P23-demo-typography-layout-excellence.md) | P23-T16; exit criterion §3 item 5 |
| [fundraising-demo-summary.md](./fundraising-demo-summary.md) | Artifact index per template |
| [deploy/demo-shared/demo-bank-style-manifest.json](../../deploy/demo-shared/demo-bank-style-manifest.json) | Style key reference |

---

## Change log

| Version | Date | Description |
| --- | --- | --- |
| 1.0.0 | 2026-07-08 | Initial checklist — P23-T16; mandatory ≥2 CORP + ≥2 RETAIL samples |
