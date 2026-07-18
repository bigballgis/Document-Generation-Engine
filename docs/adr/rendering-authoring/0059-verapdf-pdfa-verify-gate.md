---
id: ADR-0059
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - rendering
  - architecture
adrNumber: "0059"
topic: rendering-authoring
related:
  - docs/adr/rendering-authoring/0058-pdfa-2b-archival-output.md
  - docs/behavior/ibl-b3-verapdf-pdfa-gate.md
  - docs/behavior/ce-o01-pdfa-output.md
  - docs/operations/verapdf-pdfa-verify-gate.md
  - docs/plan/intl-bank-letter-readiness-program.md
---

# ADR-0059 — veraPDF PDF/A-2b verify gate (IBL-B3 / F12)

## Status

Accepted (2026-07-19 — IBL-B3 / Task Master #115)

## Context

CE-O01 / [ADR-0058](./0058-pdfa-2b-archival-output.md) delivered publish-locked
`pdfArchivalProfile=PDF_A_2B` and LibreOffice PDF/A-2b export. Golden-corpus verification
used a lightweight `pdfaid` XMP check (`PdfAidXmpAssertor`) so `mvn verify` stayed
self-contained. IBL finding **F12** requires a **machine** PDF/A validator in verify/CI —
not XMP-only.

## Decision

1. **Tool:** Use **veraPDF Greenfield** (`org.verapdf:validation-model-jakarta`) as the
   PDF/A-2b machine validator in the **test/verify** classpath (not runtime production
   classpath).
2. **Version pin:** `1.30.2` (Maven Central; resolved on this host 2026-07-19).
3. **Wiring:** `VeraPdfPdfA2bAssertor` validates PDF bytes against flavour `2b`. Default
   `mvn verify` (profile `verapdf-pdfa`, active by default) sets
   `docgen.verapdf.required=true` / `docgen.verapdf.skip=false` via Surefire. Fixture
   `backend/src/test/resources/pdfa-fixtures/pdfa-2b-corpus-pass.pdf` (veraPDF corpus,
   CC BY 4.0) must pass; plain non-PDF/A PDFs must fail.
4. **Keep XMP assertor:** `PdfAidXmpAssertor` remains for cheap synthetic golden checks;
   it must not be the only gate.
5. **Local skip (honest):** Developers may set
   `-Ddocgen.verapdf.skip=true -Ddocgen.verapdf.required=false` for local inner loops when
   the tool/deps are unavailable. **CI / default verify must not set skip**; when
   `required=true` and skip is requested, the gate **fails** (fail-not-skip).
6. **Does not amend ADR-0058 decision text** — archival product behavior stays as Accepted;
   this ADR adds verification evidence only.

## Dependency policy

| Check | Result (2026-07-19) |
| --- | --- |
| Artifact | `org.verapdf:validation-model-jakarta:1.30.2` |
| Repository | Maven Central (`https://repo.maven.apache.org/maven2`) — same default used by this project's Maven settings |
| Scope | `test` only |
| License | GPLv3+ **or** MPLv2+ (veraPDF dual license) — acceptable for **test/verify tooling**; not bundled into production runtime images via this dependency |

If a future company-internal repository **quarantines** veraPDF, treat the gate as
**Blocked** until an approved equivalent is pinned — do not invent fake validation.

## Consequences

- F12 closes when verify/CI runs veraPDF on PDF/A fixtures (machine profile), not XMP-only.
- LibreOffice export quality vs veraPDF may still diverge (spot-check residual documented in
  ops); fixing LO→veraPDF production conformance is **out of scope** for this ADR.
- Ops runbook: [verapdf-pdfa-verify-gate.md](../../operations/verapdf-pdfa-verify-gate.md).

## Alternatives Considered

| Alternative | Why not |
| --- | --- |
| Keep XMP-only | Does not close F12 |
| veraPDF CLI subprocess only | Extra host tooling; library on Maven Central is enough for verify |
| Runtime validation of every archival PDF | Out of scope for IBL-B3; product path remains LO export per ADR-0058 |
| Invent a custom “PDF/A checker” | Forbidden — not a real validator |

## Related Documents

- Behavior: [ibl-b3-verapdf-pdfa-gate.md](../../behavior/ibl-b3-verapdf-pdfa-gate.md)
- Product archival: [ADR-0058](./0058-pdfa-2b-archival-output.md)
- Ops: [verapdf-pdfa-verify-gate.md](../../operations/verapdf-pdfa-verify-gate.md)
