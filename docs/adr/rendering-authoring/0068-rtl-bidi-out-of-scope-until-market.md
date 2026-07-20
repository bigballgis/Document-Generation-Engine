---
id: ADR-0068
type: ADR
title: RTL / bidirectional scripts out of scope until market confirmation
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, rendering, product, doc-keeper
owners:
  - rendering
  - architecture
adrNumber: "0068"
topic: rendering-authoring
related:
  - docs/plan/evidence/ibl-e7-rtl-bidi-spike/SPIKE-REPORT.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/adr/rendering-authoring/0041-rendering-font-baseline.md
  - docs/adr/rendering-authoring/0042-pagination-delta-budget.md
  - docs/adr/rendering-authoring/0060-legal-reproducibility-freeze.md
  - docs/adr/template-lifecycle/0062-locale-variant-template-clause-model.md
  - backend/src/test/java/com/bank/docgen/rendering/RtlBidiInventoryProbeTest.java
---

# ADR-0068 — RTL / bidirectional scripts out of scope until market confirmation

## Status

**Accepted** (2026-07-20) — IBL-E7 / Task Master **#134** / finding **F15** spike closeout.

Spike evidence: [SPIKE-REPORT.md](../../plan/evidence/ibl-e7-rtl-bidi-spike/SPIKE-REPORT.md) — verdict **DESCOPE**.  
BDD: **not-applicable** (spike). `sourceOfTruth: true` while Accepted.

| Gate | Note |
| --- | --- |
| Wave E / F15 | **Closed by descope** — product RTL/bidi OUT of IBL and v1 go-live; finding remains documented |
| Spike lock | [SPIKE-REPORT.md](../../plan/evidence/ibl-e7-rtl-bidi-spike/SPIKE-REPORT.md) — LO/POI/font/fidelity gaps; no product RTL impl |
| File status | **Accepted** (2026-07-20) — doc-keeper stage 3; Decision = DESCOPE + reopen criteria |

**IBL-E7 / #134 → Done** (`37239d68` / `68abc7c3`) — Wave E closed by DESCOPE; **no** full RTL product implementation.

**Product reconfirmation (2026-07-20):** User explicitly confirmed **不做 RTL** — reinforces this ADR Decision and IBL-E7 DESCOPE. **Do not** reopen a product RTL implementation leaf without a new market-gated program (see Decision §4 reopen criteria).

This ADR does **not** flip checklist **#3b** / **#5a**, remove SPECIMEN (**PD-6**), embed licensed fonts (**PD-7**), invent Word/pixel baselines, claim Wave E / IBL program Done, or authorize a product RTL implementation leaf.

## Context

International bank letter readiness finding **F15** recorded that the rendering package has no RTL / bidirectional script support. Wave **IBL-E7** was scheduled as an **exploration spike** (not product delivery). Inventory shows:

1. Structured DOCX writing emits LTR-oriented OOXML only (no `w:bidi` / `w:rtl`).
2. ADR-0019 direct-formatting whitelist has no writing-direction / bidi marks.
3. ADR-0041 conversion images lack Arabic/Hebrew font packages and gates.
4. LibreOffice PDF fidelity for RTL vs Word is unproven; Word host (**IBL-B7**) remains Blocked.
5. Market need for RTL locales remains **product-gated** (not confirmed for v1 go-live).

## Decision

1. **Descope** RTL / bidirectional script **product** support from the IBL program and from v1 go-live scope.
2. Treat F15 as **closed by descope** (finding remains documented; not “silently fixed”).
3. Unicode RTL codepoints may appear in variables/content as opaque text; the platform **does not** claim correct bidi layout, mirrored punctuation, RTL lists/tables, or bank-grade Arabic/Hebrew letter fidelity.
4. **Reopen** only via a new program leaf after:
   - Explicit product confirmation naming scripts/locales;
   - Accepted amendments (or successor ADRs) for structured direction marks (ADR-0019), fonts (ADR-0041 / repro freeze ADR-0060), and fidelity policy;
   - Golden corpus + LO CI evidence; Word residual honesty if still Blocked.
5. Apache POI low-level `CTPPr` bidi seams may be used in future implementation work; they are **not** a product commitment today.

## Consequences

- **Positive:** Honest go-live boundary; avoids half-RTL that would fail bank letter review; amortizes future work behind market confirmation.
- **Negative:** Arabic/Hebrew (and other RTL) markets cannot be sold as supported letter output until a dedicated wave lands.
- **Neutral:** Locale-variant model (ADR-0062) remains valid for LTR locales; direction is orthogonal to locale codes.

## Alternatives considered

- **Implement full RTL in Wave E** — rejected: spike charter forbids full product impl; gaps are multi-surface; market unconfirmed.
- **Design-only full RTL ADR now (implement later without descope)** — rejected as primary close: would keep F15 “open” and invent schema/API without product gate; reopen criteria above are sufficient when market arrives.
- **Font-only add Noto Arabic** — rejected: fonts without OOXML bidi + structured marks create false confidence.

## Related documents

- [IBL-E7 spike report](../../plan/evidence/ibl-e7-rtl-bidi-spike/SPIKE-REPORT.md)
- [IBL program](../../plan/intl-bank-letter-readiness-program.md) — F15 / IBL-E7
- [ADR-0019](./0019-structured-authoring-and-rendering-boundary.md)
- [ADR-0041](./0041-rendering-font-baseline.md)
- [ADR-0042](./0042-pagination-delta-budget.md)
- [ADR-0060](./0060-legal-reproducibility-freeze.md)
