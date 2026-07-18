# veraPDF PDF/A-2b verify gate (IBL-B3 / F12)

## Purpose

Run **veraPDF** (Greenfield library) during backend `mvn verify` so PDF/A artifacts are
machine-validated — not only `pdfaid` XMP (`PdfAidXmpAssertor`).

Authority: [ADR-0059](../adr/rendering-authoring/0059-verapdf-pdfa-verify-gate.md) ·
behavior [ibl-b3-verapdf-pdfa-gate.md](../behavior/ibl-b3-verapdf-pdfa-gate.md).

## How it is wired

| Piece | Location |
| --- | --- |
| Dependency | `org.verapdf:validation-model-jakarta:${verapdf.version}` (`test` scope) in `backend/pom.xml` |
| Maven profile | `verapdf-pdfa` (**activeByDefault**) — sets `docgen.verapdf.required=true`, `docgen.verapdf.skip=false` |
| Surefire | Forwards those properties into the test JVM |
| Assertor | `com.bank.docgen.rendering.goldencorpus.VeraPdfPdfA2bAssertor` |
| Gate tests | `VeraPdfPdfA2bAssertorTest` |
| Fixture | `backend/src/test/resources/pdfa-fixtures/pdfa-2b-corpus-pass.pdf` |
| Optional golden flag | `requireVeraPdf: true` in PDF assertion JSON (in addition to `requirePdfA2b` XMP) |

## Commands

```powershell
# Default verify — gate required (CI)
mvn -B -ntp -f backend/pom.xml verify

# Explicit profile (same defaults; useful in docs/CI matrices)
mvn -B -ntp -f backend/pom.xml -Pverapdf-pdfa verify

# Fast inner loop (still runs veraPDF tests unless skipped)
mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=VeraPdfPdfA2bAssertorTest

# Local optional skip ONLY (never CI) — requires required=false
mvn -B -ntp -f backend/pom.xml -Pdev-fast test `
  -Ddocgen.verapdf.skip=true -Ddocgen.verapdf.required=false
```

**Fail-not-skip:** if `docgen.verapdf.required=true` (verify default) and skip is set,
the gate fails with an explicit error. Do not set `DOCGEN_VERAPDF_SKIP=true` in CI.

## Dependency availability

Pinned to Maven Central (`validation-model-jakarta` **1.30.2**, verified 2026-07-19).
If company repos quarantine the artifact, stop and record **Blocked** — do not fake a gate.

## Related residuals

- Spot-check: LibreOffice 7.3.7 `SelectPdfVersion=2` from a minimal ODT **failed** veraPDF
  checks **6.2.4.3** / **6.6.2.1**. Not closed by IBL-B3; see fixture README.
- CE-O01 / ADR-0058 archival product path unchanged.
