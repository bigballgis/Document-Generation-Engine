---
id: ADR-0051
title: Side-by-side Authoring Preview Strategy
status: Proposed
date: 2026-07-05
deciders: architecture, frontend-engineer
related:
  - docs/plan/detail/LRP-C-usability-deepening.md
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
---

# ADR-0051 — Side-by-side Authoring Preview Strategy

## Context

Authors currently edit structured content in `ControlledStructuredContentEditor.vue` and
trigger a manual preview refresh (`TemplatePreviewPanel.vue` L50–63). There is no live
side-by-side view, so authors context-switch between edit and preview tabs to see how a
node change affects the rendered DOCX/PDF.

Industry baseline: structured authoring tools (Notion, Confluence, Word Online) show a live
preview pane beside the editor. For DOCX/PDF rendering, a true live preview is expensive
(each keystroke triggers a LibreOffice conversion), so a debounced side-by-side is the v1
compromise.

## Decision

Deliver a **side-by-side preview pane** in the structured authoring editor:

1. **Layout** — the editor splits into two columns: structured editor (left) + preview
   iframe / image (right). Collapsible for narrow viewports.
2. **Debounced refresh** — preview fires 1.5s after the last edit (configurable). Authors
   can also click "Refresh now" to force an immediate render.
3. **Final-chain artifact** — the preview uses the same render path as the published
   document (DOCX → PDF via LibreOffice), not a simplified HTML approximation. This keeps
   the "what you see is what you publish" contract (CD-PIT-08 boundary).
4. **LR-A1/A2 dependency** — the preview quality depends on the LibreOffice profile
   isolation (LR-A1) and the font baseline (LR-A2) being in place; otherwise concurrent
   previews can fail intermittently.
5. **Cost guard** — the preview is rate-limited per author (max 1 in-flight preview per
   template) to avoid hammering the conversion pool.

## Consequences

- **Positive:** authors see the effect of edits immediately; fewer context switches; the
  preview is the final-chain artifact so no fidelity surprise at publish.
- **Negative:** each preview consumes a conversion slot; with the default pool size of 2,
  concurrent authors may queue. Mitigation: the debounce + per-author limit caps demand.
- **Neutral:** the preview pane is collapsible; authors who prefer the full-width editor
  can hide it.

## Alternatives considered

- **HTML approximation preview** — rejected: violates CD-PIT-08; the HTML view does not
  match the LibreOffice-rendered PDF, leading to publish-time surprises.
- **No live preview (status quo)** — rejected: known usability gap (LRP §1 finding 10).
- **WebSocket-pushed live preview** — rejected: adds transport complexity for v1; the
  debounced poll is sufficient.
