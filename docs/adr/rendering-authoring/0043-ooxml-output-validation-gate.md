---
id: ADR-0043
title: OOXML output validation gate for generated DOCX
status: Accepted
date: 2026-07-05
deciders: architecture, backend-engineer
related:
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/adr/rendering-authoring/0041-rendering-font-baseline.md
  - docs/adr/rendering-authoring/0042-pagination-delta-budget.md
  - docs/plan/detail/CDP-industry-pitfall-registry.md
  - docs/plan/detail/LRP-A-rendering-trust-hardening.md
  - docs/api/contract-outline.md
  - docs/behavior/prod-adr-0042-0043-closeout.md
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

Adopt an **OOXML output validation gate** as a post-assembly step. Scope is intentionally
**phased** — **Accepted** covers Decision **slice A** only; slice B remains an honest
residual (see below). Do not equate Accepted with “strict OOXML / LO24-safe.”

### Decision slice A — Accepted Done line (PRR-C01 / LR-A6)

1. After `StructuredContentDocxWriter` assembles a DOCX, open it with POI
   `OPCPackage.open` and run **XML well-formedness** checks on Word XML parts
   (`document.xml`, `styles.xml`, `numbering.xml`, `header*.xml`, `footer*.xml`, and
   related package `.xml` / `.rels` parts as applicable).
2. If validation fails, assembly is **fail-closed**: reject with
   `OOXML_VALIDATION_FAILED` / category `RENDERING` /
   `api.error.rendering.ooxmlValidationFailed` / HTTP **422** / `retryable=false` —
   the document must not be persisted or previewed.
3. Add regression coverage that asserts the gate **accepts** well-formed corpus output
   and **rejects** corrupted / malformed fixtures (CD-PIT-03 class).
4. Centralize XML escaping in `StructuredContentDocxWriter` — never write raw user
   text into XML without escape.

The gate runs in the assembly path, not the conversion path, so it catches the defect
before the document reaches LibreOffice.

**Accepted (2026-07-18 — PRR-C01 / Task Master #103 / BDD OOX-C4):** Wave C rules that
ADR-0043 may be **Accepted** when slice A is delivered, runtime gate remains fail-closed
(default on), and slice B residuals are recorded honestly. Accepted **must not** be read
as full ECMA-376 XSD conformance or LO24 headless-open proof.

### Decision slice B — deferred residual (not in Accepted Done line)

5. **Full ECMA-376 XSD schema validation** for the main document part (strict schema
   pass beyond well-formedness) — **deferred**.
6. **LibreOffice 24+ headless open** as a CI/runtime proof that the package opens
   without Word resave — **deferred** (may remain under CD-HARD-T03 residual /
   LO-equipped host evidence).

Sibling triad: [ADR-0041](./0041-rendering-font-baseline.md)
(font baseline), [ADR-0042](./0042-pagination-delta-budget.md) (pagination delta).

## Consequences

- **Positive (slice A):** Assembled DOCX is guaranteed **OPC-openable + XML
  well-formed** at the gate; fail-closed prevents corrupt packages from being
  persisted or previewed; contract documents `OOXML_VALIDATION_FAILED`.
- **Residual honesty:** Slice A does **not** guarantee full ECMA-376 schema
  conformance or LO24 headless open. Callers and launch gates must not equate
  well-formedness with “strict OOXML / LO24-safe” until slice B lands.
- **Negative (when slice B lands):** Schema validation may add ~10–50 ms per
  document; acceptable for a non-real-time generation API.
- **Neutral:** Runtime check defaults to on; may be disabled via property if
  performance becomes critical (`docgen.rendering.ooxml-validation-enabled`).

## Alternatives considered

- **No validation (trust POI output)** — rejected: POI does not enforce strict OOXML;
  LibreOffice 24 rejects the output.
- **Post-conversion check only (PDF output)** — rejected: catches the symptom too late;
  the DOCX is already persisted and may have reached the caller.
- **Switch to a different DOCX library** — rejected: violates the tech-stack guardrails
  (ADR-0028); POI is the accepted baseline.
- **Keep Proposed until full XSD + LO24 (slice B)** — **superseded by Wave C / OOX-C4
  (2026-07-18):** Rejected as the sole path to Accepted. **Accepted may land on slice A**
  with honest slice B residual; readers must not interpret Accepted as LO24-safe or full
  ECMA-376 XSD. Promoting Accepted **without** recording slice B residual remains
  forbidden.

## Mapping

- Pitfall: CD-PIT-03 (OOXML strictness / escaping).
- LRP task: LR-A6 (OOXML output validation gate) — slice A.
- CDP task: CD-HARD-T03 (executed by LR-A6; LO24 residual may remain after slice A).
- Contract: `docs/api/contract-outline.md` — `RENDERING` / `OOXML_VALIDATION_FAILED`.
- Closeout: PRR-C01 / Task Master **#103** (slice A Accepted; merge `3513ab92`).
