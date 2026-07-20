# Legal reproducibility freeze (IBL-B6 / F16)

## Purpose

Operational procedure for a **deterministic legal-reproducibility freeze** of DOCX→PDF
conversion: LibreOffice version record, ADR-0041 font set, and SHA-256 content-hash
baselines for PDF (optional DOCX).

Authority: [ADR-0060](../adr/rendering-authoring/0060-legal-reproducibility-freeze.md) ·
behavior readiness [ibl-b6-repro-freeze.md](../behavior/ibl-b6-repro-freeze.md) ·
program [IBL-B6](../plan/intl-bank-letter-readiness-program.md).

**Not in scope:** Microsoft Word baselines / Path E page deltas (**IBL-B7** — Blocked);
pixel/visual PDF compare (`PIXEL_*` remains rejected); go-live or checklist **#3b** /
**#5a** flips; licensed Calibri/Cambria embedding (**PD-7** pursue —
[ADR-0069](../adr/rendering-authoring/0069-licensed-font-embedding-pursue.md) /
[licensed-font-embedding-procurement.md](./licensed-font-embedding-procurement.md) —
freeze fonts remain **ADR-0041** until a post-procurement re-cut).

## Freeze triad

| Pillar | Authority | What to record |
| --- | --- | --- |
| LibreOffice version | ADR-0060 §1 | `soffice --version` + `dpkg-query` for `libreoffice-core` / `libreoffice-writer` + image digest |
| Font set | [ADR-0041](../adr/rendering-authoring/0041-rendering-font-baseline.md) | jammy packages: `fonts-noto-cjk`, `fonts-crosextra-carlito`, `fonts-crosextra-caladea`, `fontconfig`, `fonts-dejavu` |
| Content-hash baselines | ADR-0060 §3 | SHA-256 of PDF bytes (optional DOCX) + input fingerprints + LO/font/image record |

## 1. Record LibreOffice version (from the converting image)

Run **inside** the backend conversion container (or an equivalent jammy runtime stage),
not on an unrelated host soffice:

```powershell
# Example — adjust container name/id from docker compose ps
docker exec <docgen-backend-or-lo-container> soffice --version
docker exec <docgen-backend-or-lo-container> dpkg-query -W -f='${Package}\t${Version}\n' libreoffice-core libreoffice-writer
docker image inspect <backend-image-ref> --format '{{index .RepoDigests 0}} {{.Id}}'
```

Persist the four fields from ADR-0060 §1.3 with the baseline cut (UTC).

**Historical hint (not a freeze claim):** prior ops notes observed jammy LibreOffice
**7.3.7** failing some veraPDF checks on a minimal ODT — see
[verapdf-pdfa-verify-gate.md](./verapdf-pdfa-verify-gate.md). Always re-record from the
image that produces the legal artifacts.

**Optional Dockerfile pin:** after a cut that must survive apt drift, pin
`libreoffice-core=<Version>` / `libreoffice-writer=<Version>` in
`backend/Dockerfile` and `backend/Dockerfile.packaged` to the recorded packages. Until
then, revalidate baselines after LO upgrades.

## 2. Confirm font set (ADR-0041)

In the same image:

```powershell
docker exec <docgen-backend-or-lo-container> sh -c 'fc-list :lang=zh | head -n 5'
docker exec <docgen-backend-or-lo-container> sh -c 'fc-list | grep -i carlito | head -n 5'
docker exec <docgen-backend-or-lo-container> sh -c 'fc-list | grep -i caladea | head -n 5'
```

Build-time assertions already fail closed if CJK or Carlito are missing
(`backend/Dockerfile*`). Regression: `RenderingFontSmokeTest` (skips without soffice).

## 3. Content-hash baseline procedure

### 3.1 Produce

1. Fix inputs: master DOCX bytes, published template JSON, variables JSON (same as the
   theme or legal sample under test).
2. Render DOCX via the platform writer path; convert to PDF via the **frozen** LO image
   (cli / docker-exec — same path as acceptance).
3. Compute SHA-256 of the PDF file bytes (hex lowercase). Optionally hash the DOCX too.

```powershell
# Windows (PowerShell)
Get-FileHash -Algorithm SHA256 .\artifact.pdf | Format-List
# Linux / Git Bash
sha256sum artifact.pdf
```

### 3.2 Store

Write a sidecars JSON (example shape — do not invent Word metrics):

```json
{
  "artifact": "pdf",
  "sha256": "<64-hex>",
  "loVersionRecord": {
    "sofficeVersion": "<soffice --version stdout>",
    "packages": {
      "libreoffice-core": "<dpkg Version>",
      "libreoffice-writer": "<dpkg Version>"
    }
  },
  "fontBaselineRef": "ADR-0041",
  "inputFingerprint": {
    "masterSha256": "<64-hex>",
    "templateSha256": "<64-hex>",
    "variablesSha256": "<64-hex>"
  },
  "imageDigest": "sha256:<…>",
  "recordedAt": "2026-07-19T00:00:00Z"
}
```

Preferred locations:

- `docs/plan/evidence/<slice>/content-hash-baselines/*.json` for program/legal cuts
- Theme-local `expected/content-hashes.json` only when a future leaf opts a golden theme
  into hash checks

**Do not** add baseline PDF binaries to `backend/src/test/resources/golden-corpus/` under
this leaf. Day-to-day golden harness remains DOCX XPath + PDF text + non-pixel layout
metrics (`PAGE_COUNT` / `TEXT_POSITION` — IBL-C1 / F17; still no `PIXEL_*`) (+ veraPDF when
required) — see [golden-corpus README](../../backend/src/test/resources/golden-corpus/README.md)
and [ADR-0060](../adr/rendering-authoring/0060-legal-reproducibility-freeze.md) Relation note.

### 3.3 Compare

1. Re-render with the same inputs under the recorded LO + fonts + image class.
2. Recompute SHA-256; require **exact** equality with the stored `sha256`.
3. On mismatch: stop — check LO package drift, font regression, input fingerprint drift,
   or PDF filter flags (`pdfArchivalProfile`, encryption). Do **not** auto-overwrite the
   baseline without an explicit re-cut and ADR/ops note.

## Coordination with existing docs

| Doc | Relationship |
| --- | --- |
| [ADR-0041](../adr/rendering-authoring/0041-rendering-font-baseline.md) | Font set authority (reaffirmed) |
| [ADR-0042](../adr/rendering-authoring/0042-pagination-delta-budget.md) | Pagination budget / Word residual — **not** closed here; **OUT B7** |
| [ADR-0058](../adr/rendering-authoring/0058-pdfa-2b-archival-output.md) / [ADR-0059](../adr/rendering-authoring/0059-verapdf-pdfa-verify-gate.md) | Archival export + veraPDF machine gate — complementary to content-hash |
| [pdf-conversion-capacity-plan.md](./pdf-conversion-capacity-plan.md) | Pool/queue capacity — orthogonal to freeze |
| [verapdf-pdfa-verify-gate.md](./verapdf-pdfa-verify-gate.md) | PDF/A validation ops — complementary |
| Golden corpus | XPath/text verify contract unchanged; content-hash is overlay |

## Honesty residuals

- Dockerfiles currently install **unpinned** jammy LibreOffice packages — freeze is
  **record-at-cut-time** until optional apt pins land.
- This leaf authors the freeze **procedure**; it does **not** claim a full bank-letter
  content-hash corpus is already checked into evidence/.
- No Word baselines invented; checklist **#3b** / **#5a** unchanged; not go-live.
