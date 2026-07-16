---
id: ADR-0058
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - rendering
  - architecture
adrNumber: "0058"
topic: rendering-authoring
related:
  - docs/behavior/ce-o01-pdfa-output.md
  - docs/plan/core-excellence-program-2026-07.md
  - docs/adr/authorization-security/0001-output-encryption.md
  - docs/domain/domain-model.md
---

# ADR-0058 — PDF/A-2b archival output (render profile)

## Status

Accepted (2026-07-16 — CE-O01 / product decision D6 confirmed 2026-07-14)

## Context

Some bank-letter PDF consumers require long-term archival PDF/A. The platform converts
DOCX→PDF via LibreOffice headless and already supports request-time PDF encryption
([ADR-0001](../authorization-security/0001-output-encryption.md)). PDF/A and password
encryption are incompatible in practice. CE program decision **D6** (2026-07-14)
chose a single archival level for v1 rather than leaving A-1b vs A-2b open.

## Decision

1. **Archival level:** Support **PDF/A-2b only** in this program slice. Do **not**
   implement PDF/A-1b (or A-2a / A-3*) in CE-O01.
2. **Configuration surface:** Publish-locked `RenderProfile.pdfArchivalProfile` with
   enum `NONE` | `PDF_A_2B` (`UPPER_SNAKE_CASE`). Missing field defaults to `NONE`.
   Callers cannot override via request body or `CallerRenderOverride`.
3. **Conversion:** When effective profile is `PDF_A_2B` and `output.format=PDF`,
   LibreOffice uses the **PDF/A-2b** writer export filter. `NONE` keeps the existing
   conventional PDF conversion path.
4. **Mutex with encryption:** `PDF_A_2B` + request `encryption.enabled=true` on PDF
   generation is **rejected** fail-closed (`400` / `PDF_ARCHIVAL_ENCRYPTION_MUTEX`).
   Do not encrypt a PDF/A artifact and do not silently drop either requirement.
5. **Verification:** Golden-corpus ACTIVE package(s) assert archival conformance via
   veraPDF when feasible, otherwise a lightweight PDF/A-2b identifier check (e.g.
   `pdfaid` XMP part=2 + conformance=B).

## Consequences

- Archival PDF becomes an opt-in, publish-locked render-profile dimension — not an
  API-management encryption toggle and not a caller override.
- Templates/releases that need archival PDF must ship with `pdfArchivalProfile=PDF_A_2B`
  in the locked profile JSON (platform default asset and/or version snapshot).
- Callers who need both password protection and archival must choose one per
  generation; the platform will not combine them.
- Management UI for editing the archival flag is **out of scope** for CE-O01
  (API/render-only).
- CE-O02 (addressBlock / multi-doc pack) remains skipped per D5.

## Alternatives Considered

| Alternative | Why not |
| --- | --- |
| PDF/A-1b | Stricter subset; weaker LibreOffice support vs A-2b for this stack — rejected by D6 |
| Caller request flag for PDF/A | Breaks publish-locked render-profile model; rejected |
| Allow encryption after PDF/A | Breaks archival conformance; rejected |
| Defer ADR until implementation-only | D6 already product-confirmed; ADR records the durable choice |

## Related Documents

- Behavior SoT: [ce-o01-pdfa-output.md](../../behavior/ce-o01-pdfa-output.md)
- CE plan §8 / §10 D6: [core-excellence-program-2026-07.md](../../plan/core-excellence-program-2026-07.md)
- Output encryption: [ADR-0001](../authorization-security/0001-output-encryption.md)
- Domain render profile: [domain-model.md](../../domain/domain-model.md) §2.6.8
