---
id: ADR-0060
type: ADR
title: Legal reproducibility freeze for LibreOffice PDF conversion
status: Accepted
sourceOfTruth: true
date: 2026-07-19
deciders: architecture, rendering, doc-keeper, deploy-engineer
owners:
  - rendering
  - architecture
adrNumber: "0060"
topic: rendering-authoring
related:
  - docs/adr/rendering-authoring/0041-rendering-font-baseline.md
  - docs/adr/rendering-authoring/0042-pagination-delta-budget.md
  - docs/adr/rendering-authoring/0058-pdfa-2b-archival-output.md
  - docs/adr/rendering-authoring/0059-verapdf-pdfa-verify-gate.md
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/operations/legal-reproducibility-freeze.md
  - docs/behavior/ibl-b6-repro-freeze.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - backend/Dockerfile
  - backend/Dockerfile.packaged
  - backend/src/test/resources/golden-corpus/README.md
---

# ADR-0060 — Legal reproducibility freeze for LibreOffice PDF conversion

## Status

**Accepted** (2026-07-19 — IBL-B6 / Task Master **#118** / finding **F16**)

This ADR records the **governance freeze** for deterministic legal reproducibility of
DOCX→PDF conversion. It does **not** claim go-live, flip checklist **#3b** / **#5a**,
or invent Microsoft Word baselines (those remain **IBL-B7** / Path E — Blocked until a
licensed Word host exists).

## Context

International bank-letter readiness (**F16**) requires a durable record of:

1. Which **LibreOffice** build converts DOCX→PDF for legal/golden paths.
2. Which **font set** the conversion image must carry (already decided in
   [ADR-0041](./0041-rendering-font-baseline.md)).
3. How **content-hash baselines** (PDF, optionally DOCX) are produced, stored, and
   compared when proving byte-stable regeneration under that freeze.

Today’s golden corpus ([CE-K07](../../behavior/ce-k07-golden-corpus-skeleton.md)) asserts
**DOCX XPath + PDF text extract** only — `PIXEL_*` is rejected and baseline PDF binaries
are not the day-to-day `mvn verify` contract ([F17](../../plan/intl-bank-letter-readiness-program.md)).
That stance remains. F16 adds an **ops/ADR freeze procedure** for content-hash evidence
when legal reproducibility must be demonstrated — without inventing Word page-delta numbers
or pixel compares.

Runtime images install `libreoffice-core` / `libreoffice-writer` from Debian jammy apt
without a hard version pin in the Dockerfile. Operators therefore need an explicit
**record-and-compare** procedure whenever a legal baseline is cut or revalidated.

## Decision

### 1. LibreOffice version pin / recording

1. **Conversion engine** remains headless LibreOffice Writer in the backend conversion
   image (`backend/Dockerfile`, `backend/Dockerfile.packaged`) per ADR-0019 / ADR-0028.
2. **Authoritative pin for a legal baseline** is the version string recorded from the
   **same image/container** that produced the baseline artifacts, not a prose guess.
3. **Required record fields** (store with every legal baseline cut — see ops freeze):
   - `soffice --version` full stdout (trim trailing whitespace).
   - Debian package versions: `dpkg-query -W -f='${Package}\t${Version}\n' libreoffice-core libreoffice-writer`.
   - Image identity used for the cut: backend image digest (`sha256:…`) and/or compose
     build id when available.
   - UTC timestamp of the cut.
4. **How to pin going forward:** when a legal baseline must survive apt drift, the image
   Dockerfile **may** pin apt package versions (`libreoffice-core=<version>`,
   `libreoffice-writer=<version>`) to the recorded jammy versions from the cut. Until a
   pin lands in Dockerfiles, treat the freeze as **record-at-cut-time** — revalidate
   baselines after any LO package upgrade.
5. **Do not** claim a LO major/minor as “frozen forever” without an evidence record from
   the converting image. Spot observations (e.g. jammy LO **7.3.7** seen in prior ops notes)
   are historical hints only until re-recorded via the procedure above.

### 2. Font set (reaffirm ADR-0041 — no re-decision)

Legal reproducibility uses the **Accepted** rendering font baseline in
[ADR-0041](./0041-rendering-font-baseline.md):

| Role | Debian jammy package |
| --- | --- |
| CJK | `fonts-noto-cjk` |
| Calibri metric-compatible | `fonts-crosextra-carlito` |
| Cambria metric-compatible | `fonts-crosextra-caladea` |
| Support | `fontconfig`, `fonts-dejavu` |

Build gates (`fc-cache`, `fc-list :lang=zh`, Carlito presence) and
`RenderingFontSmokeTest` remain the regression contract. Licensed Microsoft fonts stay
out of images. This ADR does **not** amend ADR-0041 decision text.

### 3. Content-hash baseline procedure (PDF primary; DOCX optional)

1. **Algorithm:** SHA-256 over the **raw artifact bytes** (hex lowercase, no basename
   salt). Primary legal artifact = **PDF** produced by the frozen LO+font image; optional
   companion = rendered **DOCX** bytes before conversion.
2. **Produce:** Render the fixed input set (master + template release + variables) through
   the production conversion path (or the same image via docker-exec / cli) under the
   recorded LO version + ADR-0041 fonts. Do not use host-installed soffice unless it is
   proven identical to the image record.
3. **Store:** Persist a small JSON sidecars under
   `docs/plan/evidence/<slice-or-theme>/content-hash-baselines/` (or a theme-local
   `expected/content-hashes.json` when a golden theme opts in). Required keys:
   `artifact` (`pdf`|`docx`), `sha256`, `loVersionRecord`, `fontBaselineRef`
   (`ADR-0041`), `inputFingerprint` (hashes of master/template/variables inputs),
   `imageDigest` (when known), `recordedAt` (UTC ISO-8601). **Do not** commit large PDF
   binaries into `golden-corpus/` unless a future leaf explicitly changes that policy.
4. **Compare:** Re-render under the same freeze; recompute SHA-256; require exact match.
   Mismatch → fail the legal/repro check and investigate LO/font/input drift — do **not**
   silently refresh baselines.
5. **Relation to golden corpus:** Day-to-day `mvn verify` stays XPath + PDF text (+ veraPDF
   where required). Content-hash is an **overlay procedure** for legal freeze evidence and
   optional future harness flags — it does **not** authorize `PIXEL_*` asserts and does
   **not** invent Word baselines (OUT **IBL-B7**).
6. **PDF/A:** When the artifact is archival (`pdfArchivalProfile=PDF_A_2B`), content-hash
   complements — it does not replace — [ADR-0058](./0058-pdfa-2b-archival-output.md) /
   [ADR-0059](./0059-verapdf-pdfa-verify-gate.md) machine validation.

## Consequences

- **Positive:** F16 has a single Accepted decision for LO recording, font authority, and
  content-hash procedure; operators can cut/compare legal baselines without inventing
  Word or pixel contracts.
- **Negative:** Apt-unpinned LO in Dockerfiles means baselines may need re-cut after image
  rebuilds until optional apt pins land; content-hash is stricter than text extract and
  will fail on any LO filter metadata churn.
- **Neutral:** Golden-corpus no-pixel stance unchanged; checklist **#3b** / **#5a** unchanged;
  IBL-B7 remains Blocked for Word Path E.

## Alternatives considered

| Alternative | Why not |
| --- | --- |
| Ops-only freeze without ADR | LO pin + content-hash policy is a durable design contract across CE/IBL leaves — ADR is appropriate |
| Re-decide fonts here | Already Accepted in ADR-0041 |
| Pixel / visual PDF baselines | Forbidden by golden-corpus / F17 until a future ADR revises stance |
| Invent Word page-delta baselines | Forbidden — IBL-B7 / Path E; host missing |
| Hash only extracted PDF text | Weaker than legal byte freeze; text extract remains the CE-K07 verify contract, not this freeze |
| Claim Dockerfile apt packages are forever-pinned today | Dishonest — packages are currently unpinned; record-at-cut + optional future pin is the truth |

## Related documents

- Ops freeze runbook: [legal-reproducibility-freeze.md](../../operations/legal-reproducibility-freeze.md)
- Behavior readiness (BDD N/A): [ibl-b6-repro-freeze.md](../../behavior/ibl-b6-repro-freeze.md)
- Font baseline: [ADR-0041](./0041-rendering-font-baseline.md)
- Pagination / Word residual: [ADR-0042](./0042-pagination-delta-budget.md)
- Golden corpus: [backend/.../golden-corpus/README.md](../../../backend/src/test/resources/golden-corpus/README.md)
- IBL program F16 / IBL-B6: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md)
