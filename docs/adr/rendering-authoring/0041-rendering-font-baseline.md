---
id: ADR-0041
type: ADR
title: Rendering font baseline for DOCX→PDF conversion images
status: Accepted
sourceOfTruth: true
date: 2026-07-10
deciders: architecture, deploy-engineer, backend-engineer, doc-keeper
owners:
  - architecture
  - rendering
adrNumber: "0041"
topic: rendering-authoring
related:
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/adr/rendering-authoring/0042-pagination-delta-budget.md
  - docs/adr/technology-stack/0028-backend-platform-stack-baseline.md
  - docs/plan/detail/LRP-A-rendering-trust-hardening.md
  - docs/plan/detail/CDP-industry-pitfall-registry.md
  - docs/plan/detail/P23-demo-typography-layout-excellence.md
  - backend/Dockerfile
  - backend/Dockerfile.packaged
---

# ADR-0041 — Rendering font baseline for DOCX→PDF conversion images

## Status

**Accepted** (2026-07-10 — architecture-reviewer sign-off `PASS_WITH_NOTES`; slice
`lrp-a5-adr-closeout`)

Implementation evidence from **LR-A2 Done** (2026-07-08; mirrors **P23-T02**; executes
**CD-HARD-T01**) is durable and production-image-backed. This ADR records the decision
and package baseline. Promotion to Accepted is complete via architecture-reviewer
sign-off on 2026-07-10 (slice `lrp-a5-adr-closeout`).

## Context

Headless LibreOffice substitutes missing fonts during DOCX→PDF conversion. Without CJK
and metric-compatible Latin fonts in the conversion image, Chinese glyphs become tofu
boxes and Calibri/Cambria runs reflow — page counts and line breaks drift versus the
author's Word view (**CD-PIT-01**).

Early LRP-A planning text assumed **Alpine** package names (`font-noto-cjk`,
`font-carlito`, …). The runtime images are **Debian jammy** (`eclipse-temurin:21-jre-jammy`
at the time of this ADR’s acceptance),
not Alpine, because Alpine 3.23 lacks `fonts-crosextra-caladea` (Carlito + Caladea are
required metric-compatible substitutes). Build stages may still use Alpine Maven images;
**font packages apply only to the jammy runtime stage**.

> **Historical note (2026-07-13, Task Master #51):** The **runtime JRE pin** moves from
> Temurin **21** to Temurin **25** with the Boot **4.1.0** upgrade slice
> ([ADR-0028](../technology-stack/0028-backend-platform-stack-baseline.md);
> [boot-4-1-upgrade](../../behavior/boot-4-1-upgrade.md); prefer
> `eclipse-temurin:25-jre-jammy` for jammy conversion images). This note does **not**
> change the font package decision above — only the JRE major line.

Licensed Microsoft fonts must not be baked into images (tech-stack / dependency policy).

## Decision

Adopt a **required rendering font baseline** in every backend conversion image
(`backend/Dockerfile` and `backend/Dockerfile.packaged` runtime stages):

1. **CJK:** `fonts-noto-cjk` (Debian jammy apt).
2. **Calibri metric-compatible:** `fonts-crosextra-carlito`.
3. **Cambria metric-compatible:** `fonts-crosextra-caladea`.
4. **Support:** `fontconfig` + `fonts-dejavu` (fallback Latin); LibreOffice writer/core
   remain the conversion engine (ADR-0028 / rendering boundary ADR-0019).
5. **Build gates:** after install, run `fc-cache -f`, assert `fc-list :lang=zh` is
   non-empty, and assert Carlito is present (`fc-list` case-insensitive match). Fail the
   image build if either assertion fails.
6. **Regression:** `RenderingFontSmokeTest` converts a mixed Calibri-styled Latin + CJK
   DOCX → PDF and asserts extracted PDF text contains the Chinese sample (no tofu
   markers). Skip when `soffice` is unavailable on the host (same pattern as other
   LibreOffice-dependent tests).

This baseline does **not** claim pixel-identical Word parity or a pagination delta
budget — those remain [ADR-0042](./0042-pagination-delta-budget.md).

## Consequences

- **Positive:** Chinese and Calibri-styled Latin content render without tofu in Docker
  PDF; build fails closed if fonts regress; CD-PIT-01 / CD-HARD-T01 have a durable
  package contract.
- **Negative:** Runtime image is larger; jammy apt package names differ from Alpine
  candidates in older LRP-A drafts — operators must not copy Alpine names into jammy
  Dockerfiles.
- **Neutral:** Package set is an image concern, not a Java dependency; swapping a
  metric-compatible substitute requires dependency-policy verification + ADR amendment.

### Evidence (2026-07-08 — LR-A2 / P23-T02 / CD-HARD-T01)

| Item | Record |
| --- | --- |
| Packages | `fonts-noto-cjk`, `fonts-crosextra-carlito`, `fonts-crosextra-caladea` (+ `fontconfig`, LibreOffice) |
| Dockerfiles | `backend/Dockerfile`, `backend/Dockerfile.packaged` (Debian jammy runtime) |
| Build assertions | `fc-list :lang=zh` non-empty; Carlito present |
| Test | `backend/src/test/java/com/bank/docgen/rendering/RenderingFontSmokeTest.java` |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` GREEN (ledger: 978 tests at LR-A2 close) |
| BDD | `BDD-DEMO-TYP-009`, `BDD-DEMO-TYP-010` |
| Ledger | [execution-sync-ledger.md](../../plan/execution-sync-ledger.md) — LR-A2 / P23-T02 font baseline evidence (2026-07-08) |
| Pitfall | CD-PIT-01 — [CDP-industry-pitfall-registry.md](../../plan/detail/CDP-industry-pitfall-registry.md) |

### Alpine vs Debian package-name drift (honesty)

| Role | Alpine candidates (early LRP-A draft) | **Actual jammy runtime (shipped)** |
| --- | --- | --- |
| CJK | `font-noto-cjk` | `fonts-noto-cjk` |
| Calibri-metric | `font-carlito` | `fonts-crosextra-carlito` |
| Cambria-metric | (Caladea if available) | `fonts-crosextra-caladea` |

**Authoritative names are the jammy apt packages above.** Do not treat Alpine names as
the production baseline.

## Alternatives considered

- **Ship only `ttf-dejavu` / `fonts-dejavu`** — rejected: CD-PIT-01 tofu + reflow.
- **Bake licensed Microsoft Calibri/Cambria** — rejected: licensing / dependency policy.
- **Alpine runtime with partial font set** — rejected: Caladea unavailable on Alpine 3.23;
  jammy chosen so Carlito + Caladea both install.
- **Host-mounted fonts only** — rejected: conversion must be reproducible in packaged
  images and CI without host font state.

## Mapping

- Pitfall: CD-PIT-01 (missing fonts in conversion container).
- LRP task: LR-A2 (implementation Done); LR-A5 (this ADR).
- CDP task: CD-HARD-T01 (executed-by-LR-A2 / P23-T02).
- Related: ADR-0042 (pagination delta — Proposed); ADR-0019 (rendering boundary).
