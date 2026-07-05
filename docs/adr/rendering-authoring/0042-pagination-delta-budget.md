---
id: ADR-0042
title: Pagination delta budget for DOCX→PDF conversion
status: Proposed
date: 2026-07-05
deciders: architecture, backend-engineer, doc-keeper
related:
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/adr/rendering-authoring/0041-rendering-font-baseline.md
  - docs/plan/detail/CDP-industry-pitfall-registry.md
  - docs/plan/detail/LRP-A-rendering-trust-hardening.md
---

# ADR-0042 — Pagination delta budget for DOCX→PDF conversion

## Context

Bank correspondence documents are legally paginated — the author's Word view defines the
"correct" page count. LibreOffice's rendering engine does not reproduce Word's layout
pixel-for-pixel (CD-PIT-02): differences in line breaking, table column widths, and
floating-object placement cause page counts to drift by ±1–3 pages in edge cases.

This is an inherent limitation of OSS rendering engines — no JS or LibreOffice-based
engine reproduces Word layout exactly. The platform must therefore publish an explicit
**pagination delta budget** so that:

- Authors know the acceptable tolerance.
- Fidelity warnings fire when the delta exceeds the budget.
- Integration partners understand the rendering contract.

## Decision

Adopt a **pagination delta budget** of **±1 page** for bank correspondence templates:

1. Define the budget in `DocgenRenderingProperties` as `paginationDeltaBudgetPages = 1`.
2. After DOCX→PDF conversion, compare the PDF page count against the author's declared
   Word page count (stored in the template metadata).
3. If `|pdfPages - wordPages| > budget`, emit a fidelity warning (not a blocker — the
   document is still usable, but the author should review).
4. If `|pdfPages - wordPages| > 2 * budget`, emit a fidelity blocker — the template
   should not be published until the layout is adjusted.
5. Document the budget in the NFR and PRD so users understand the rendering contract.
6. Build a **corpus** of ≥5 demo letters (mortgage, credit-limit, trade-LC, collection,
   annual-review) and record the baseline Word vs PDF page counts. The corpus is the
   regression test for layout drift.

The budget is a **pending proposal** per the document-as-code constitution — it must be
confirmed by the user before enforcement. Until then, the system logs the delta but does
not block or warn.

## Consequences

- **Positive:** Explicit rendering contract; authors know the tolerance; fidelity warnings
  catch layout drift early.
- **Negative:** ±1 page may be too tight for complex templates with floating objects; may
  need to relax to ±2 after corpus validation.
- **Neutral:** The budget is a configuration property, not hardcoded — can be tuned per
  deployment.

## Alternatives considered

- **Zero-tolerance (exact page match)** — rejected: impossible with OSS engines; would
  block all templates with floating objects.
- **No budget (silent drift)** — rejected: authors have no signal when layout drifts;
  integration partners cannot reason about the rendering contract.
- **Pixel-exact rendering** — rejected: requires a proprietary engine; violates the
  tech-stack guardrails (ADR-0028).

## Mapping

- Pitfall: CD-PIT-02 (Word vs LibreOffice layout engine divergence).
- LRP task: LR-A7 (pagination delta budget + corpus).
- CDP task: CD-HARD-T04 (executed by LR-A7).
