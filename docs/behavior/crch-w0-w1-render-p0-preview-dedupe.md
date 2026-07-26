# Behavior — CRCH W0+W1 (`render-p0-preview-dedupe`)

**Slice id:** `render-p0-preview-dedupe`  
**Task ids:** `CRCH-W0`, `CRCH-W1`  
**Status:** confirmed (user 2026-07-26 — full-auto remediation)  
**Traceability:** `docs/plan/core-render-compute-hardening-program.md`,  
`docs/plan/detail/CRCH-W0-rendering-correctness.md`,  
`docs/plan/detail/CRCH-W1-preview-consolidation.md`

## Actor / role

- Template author / publisher (management UI, group-scoped)
- Document generation pipeline (backend rendering / PDF conversion)

## Goals

1. Generated DOCX/PDF must not crash or silently corrupt body anchors, image/seal geometry, or page numbers.
2. LibreOffice / docker-exec conversion must not cross-contaminate concurrent jobs or leave orphan processes.
3. Publish-gate seal geometry must not dishonestly BLOCK on absolute coordinates the renderer does not honour.
4. Template authoring preview must show exactly one PDF viewer and one regeneration action with clear semantics.

## Confirmed product decisions (user)

| Id | Decision |
| --- | --- |
| D4 / OD-1 | Seals remain **inline** (no `CTAnchor` / absolute page placement). Authorized-area geometry mismatch is **not** a publish BLOCKER. |
| D5 / OD-2 | PDF page-number stamping **default stays OFF**. |
| D1 | Deliver W0+W1 as **one merged leaf**. |

## Acceptance scenarios

### W0-1 Body anchor after table

Given a master whose body is `[table, {{anchor:BODY}}]`  
When structured assemble replaces BODY  
Then the body text contains the replacement, the table survives, and no `IndexOutOfBoundsException` occurs.

### W0-2 Image aspect ratio

Given an `imageRef` whose PNG is 200×100  
When rendered inline  
Then extent is 48×24 pt (fit inside 48×48 box, ratio preserved).

### W0-3 Seal declared size

Given a `sealRef` with `sealBox.widthPt=120` and `heightPt=90`  
When rendered inline  
Then extent is 120×90 pt (not forced to 48×48).

### W0-4 Section page plan honesty

Given a footer requesting `SECTIONPAGES` and multiple `<w:sectPr>`  
When the stamp plan is resolved  
Then the plan is `globalOnly()` (no invented section start pages) and a fidelity warning may surface via the existing stamp channel when plumbing allows.

### W0-5 docker-exec path uniqueness

Given two conversion invocations with distinct temp dir tokens  
When container input/output paths are derived  
Then paths differ per token and include the token in both input and output names.

### W0-6 Process termination on timeout

Given an external process that exceeds the conversion timeout  
When `ExternalProcessRunner.runToCompletion` returns  
Then a `RenderingOperationException` is thrown and the child process is not alive.

### W0-7 Seal authorized-area honesty

Given a seal whose `sealBox` would formerly raise `SEAL_OUTSIDE_AUTHORIZED_AREA` as BLOCKER  
When publish-gate / fidelity validation runs  
Then that geometry mismatch is warning-only (or skipped) — not a BLOCKER — and copy does not claim absolute page placement.

### W1-1 / W1-2 Single PDF viewer + single fetch

Given authoring preview with a succeeded PDF preview  
When `AuthoringPreviewPane` mounts  
Then exactly one `InlinePdfPreviewViewer` is present and `downloadPreviewArtifact` is called once.

### W1-3 One refresh meaning

Given embedded preview (`embedded=true`)  
When the panel renders  
Then it hides its reload-details action; the host regenerate button remains the only re-render control.

### W1-4 Batch vs preview history

Given the Testing workspace  
When the user opens Testing sub-tabs  
Then batch history lives under `batchRuns` and preview history under `previewRuns`.

### W1-5 Ghost authoring sub-tab removed

Given `TEMPLATE_AUTHORING_SUB_TABS`  
When inspected  
Then `testPreview` is absent, while legacy `?authoringTab=testPreview` still remaps to Testing.

## Boundary / non-goals

- No absolute seal positioning (`CTAnchor`).
- No change to stamping default (`false`).
- No `downloadPreviewArtifact` caching.
- W1-6 is investigate/report only — no legacy authoring shell refactor.
- Do not flip checklist #3b/#5a; do not mark #53/#106 Done; do not activate #119 / CE-O02.

## Observable evidence

- Backend: `mvn -B -ntp -f backend/pom.xml verify` (+ `-Plibreoffice-ci` with honest skip notes if `soffice` absent).
- Frontend: `pnpm -C frontend lint|type-check|test|build`.
- E2E: authoring binding preview shows exactly one PDF viewer.
- Deploy: queued `docker-deploy-queue.ps1` when Docker host available.

## BDD readiness

`bdd_readiness: ready`  
`delivery_lane: full` (runtime/rendering + management UI preview)
