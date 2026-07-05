---
id: ADR-0043
title: OOXML output validation gate for generated DOCX
status: Proposed
date: 2026-07-05
deciders: architecture, backend-engineer
related:
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/plan/detail/CDP-industry-pitfall-registry.md
  - docs/plan/detail/LRP-A-rendering-trust-hardening.md
---

# ADR-0043 — OOXML output validation gate for generated DOCX

## Context

The platform assembles DOCX from structured content nodes via POI
(`StructuredContentDocxWriter`). LibreOffice 24+ rejects DOCX packages that contain
invalid XML characters, unescaped ampersands, or malformed relationships (CD-PIT-03) —
the document opens only after a manual resave in Word.

This is a known industry failure class (PHPWord LO24 issues): the generated OOXML is
syntactically valid for POI but not strict enough for LibreOffice 24.

## Decision

Adopt an **OOXML output validation gate** as a post-assembly step:

1. After `StructuredContentDocxWriter` assembles a DOCX, open it with POI
   `OPCPackage.open` and run XML well-formedness checks on every part
   (`document.xml`, `styles.xml`, `numbering.xml`, `header*.xml`, `footer*.xml`).
2. Add a **schema validation** pass against the OOXML strict schema (ECMA-376) for the
   main document part — at minimum, reject documents with:
   - Unescaped `&`, `<`, `>` in text runs.
   - Invalid XML characters (control characters outside the legal XML range).
   - Malformed relationship targets.
3. If validation fails, the assembly is rejected with a fidelity blocker — the document
   must not be persisted or previewed.
4. Add a regression test that assembles a corpus of templates with adversarial content
   (raw `&`, CJK, emoji, control characters) and asserts the validation gate accepts
   only the well-formed output.
5. Centralize XML escaping in `StructuredContentDocxWriter` — never write raw user
   text into XML without escape.

The gate runs in the assembly path, not the conversion path, so it catches the defect
before the document reaches LibreOffice.

## Consequences

- **Positive:** Generated DOCX is guaranteed well-formed OOXML; LibreOffice 24+ opens
  it without resave; eliminates the "corrupt DOCX" failure class.
- **Negative:** Schema validation adds ~10–50 ms per document; acceptable for a
  non-real-time generation API.
- **Neutral:** The gate is a test-time + runtime check; the runtime check can be
  disabled via a property if performance becomes critical, but defaults to on.

## Alternatives considered

- **No validation (trust POI output)** — rejected: POI does not enforce strict OOXML;
  LibreOffice 24 rejects the output.
- **Post-conversion check only (PDF output)** — rejected: catches the symptom too late;
  the DOCX is already persisted and may have reached the caller.
- **Switch to a different DOCX library** — rejected: violates the tech-stack guardrails
  (ADR-0028); POI is the accepted baseline.

## Mapping

- Pitfall: CD-PIT-03 (OOXML strictness / escaping).
- LRP task: LR-A6 (OOXML output validation gate).
- CDP task: CD-HARD-T03 (executed by LR-A6).
