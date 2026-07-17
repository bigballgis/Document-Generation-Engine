---
id: ADR-0042
title: Pagination delta budget for DOCX→PDF conversion
status: Accepted
date: 2026-07-05
deciders: architecture, backend-engineer, doc-keeper
related:
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/adr/rendering-authoring/0041-rendering-font-baseline.md
  - docs/plan/detail/CDP-industry-pitfall-registry.md
  - docs/plan/detail/LRP-A-rendering-trust-hardening.md
  - docs/behavior/prod-adr-0042-0043-closeout.md
  - docs/evidence/prod-adr-0042-0043-closeout/word-baseline-exemption.md
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
   Word page count (stored in the template metadata as `authorWordPageCount`).
3. If `|pdfPages - wordPages| > budget`, emit a fidelity warning (not a blocker — the
   document is still usable, but the author should review).
4. If `|pdfPages - wordPages| > 2 * budget`, emit a fidelity blocker — the template
   should not be published until the layout is adjusted.
5. Document the budget in the NFR and PRD so users understand the rendering contract.
6. Build a **corpus** of ≥5 demo letters (mortgage, credit-limit, trade-LC, collection,
   annual-review) and record the baseline Word vs PDF page counts. The corpus is the
   regression test for layout drift.

**Accepted (2026-07-18 — PRR-C01 / Task Master #103):** Runtime enforcement is
**metadata-gated** (`PaginationDeltaEvaluator` + preview/runtime / PublishGate wiring):
when `authorWordPageCount` is set, Decision items 3–4 apply; when unset, comparison is
skipped (no invented Word counts). Docker PDF corpus remains the drift sentinel (LR-A7).

## Consequences

- **Positive:** Explicit rendering contract; authors know the tolerance; fidelity warnings
  catch layout drift early.
- **Negative:** ±1 page may be too tight for complex templates with floating objects; may
  need to relax to ±2 after corpus validation.
- **Neutral:** The budget is a configuration property, not hardcoded — can be tuned per
  deployment.

### Measurement note (2026-07-10 — LR-A7 / CD-HARD-T04)

Docker PDF corpus measured on git SHA `9a40b48` (runtime `SYNC_STREAM` + host `pypdf`):

| externalId | Docker PDF pages | Word pages | Delta |
| --- | --- | --- | --- |
| `DEMO-CREDIT-LIMIT-CONFIRM` | 6 | n/a | n/a |
| `DEMO-MORTGAGE-APPROVAL` | 6 | n/a | n/a |
| `DEMO-TRADE-LC-NOTICE` | 9 | n/a | n/a |
| `DEMO-OVERDUE-COLLECTION` | 8 | n/a | n/a |
| `DEMO-RETAIL-ACCOUNT-OPEN` | 8 | n/a | n/a |
| `CORP-FOL-OFFER` (optional) | 86 | n/a | n/a |

Required corpus aggregates: **max = 9** / **median = 8** Docker PDF pages. Word method =
`ms-word-unavailable-on-host` — Word pages and deltas remain **n/a** (not fabricated).

### Residual — Path X / Word n/a (Accepted with honesty)

**This ADR is `Accepted` with an honest Word residual — not a Path E proof.**

True Word-vs-LibreOffice page delta remains **unproven** on hosts without Microsoft Word.
PRR-C01 closed via **Path X** durable exemption
([word-baseline-exemption.md](../../evidence/prod-adr-0042-0043-closeout/word-baseline-exemption.md)):

1. Word / Delta columns stay **n/a** (`method=ms-word-unavailable-on-host`) — do **not**
   invent numbers.
2. Docker PDF baseline under [docs/evidence/lrp-a7-pagination/](../../evidence/lrp-a7-pagination/)
   remains the drift sentinel (procedure:
   [pagination-delta-corpus.md](../../plan/pagination-delta-corpus.md)).
3. Runtime enforcement landed (metadata-gated) — checklist **#3b** may be **CONDITIONAL**
   only; Path X **≠** checklist **#3b GO** and **≠** proven Word↔LO delta.
4. Retest trigger: Word-equipped host → open a Path E measurement leaf (≥5 corpus letters),
   fill Word/Delta columns, then consider promoting **#3b → GO**.

Font baseline for conversion images is [ADR-0041](./0041-rendering-font-baseline.md)
(**Accepted**; LR-A2 implemented; architecture-reviewer PASS_WITH_NOTES 2026-07-10).

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
- Closeout: PRR-C01 / Task Master **#103** (Path X + enforcement; merge `3513ab92`).
