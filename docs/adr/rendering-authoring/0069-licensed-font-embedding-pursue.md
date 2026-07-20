---
id: ADR-0069
type: ADR
title: Licensed font embedding pursue path (procurement gate)
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, rendering, deploy-engineer, doc-keeper, product
owners:
  - rendering
  - architecture
adrNumber: "0069"
topic: rendering-authoring
related:
  - docs/adr/rendering-authoring/0041-rendering-font-baseline.md
  - docs/adr/rendering-authoring/0060-legal-reproducibility-freeze.md
  - docs/operations/licensed-font-embedding-procurement.md
  - docs/operations/legal-reproducibility-freeze.md
  - docs/operations/launch-readiness-checklist.md
  - docs/behavior/pd7-licensed-font-embedding.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/plan/launch-readiness-program.md
  - backend/Dockerfile
  - backend/Dockerfile.packaged
---

# ADR-0069 — Licensed font embedding pursue path (procurement gate)

## Status

**Accepted** (2026-07-20) — IBL §8 **PD-7** / Task Master **#139** / slice `pd7-licensed-font-embedding` (B2 pursue leaf).

Product confirmation **2026-07-19**: pursue licensed font embedding (true Calibri etc.)
via **procurement + LRP pairing**. This ADR records that **pursue path** and the
**procurement gate**. It does **not** claim licensed fonts are installed, embedded, or
shipped.

| Gate | Note |
| --- | --- |
| Shipped baseline | Remains **[ADR-0041 Accepted](./0041-rendering-font-baseline.md)** (Carlito/Caladea + CJK) |
| Legal freeze fonts | **[ADR-0060](./0060-legal-reproducibility-freeze.md)** still reaffirms ADR-0041 — unchanged |
| Embedding ship | **Blocked** until procured licensed assets + approved redistribution into images |
| LRP / checklist | Pairing notes only — **do not** flip **#3b GO** / **#5a GO** from this ADR |
| Font binaries in git | **Forbidden** — no invent/commit `.ttf` / `.otf` / vendor packs |

**Accepted ≠ licensed embedding Done.** Accepting this ADR means the pursue / procurement
governance path is durable — not that Calibri/Cambria (or other licensed faces) are in
production conversion images.

## Context

1. Conversion images today ship **metric-compatible substitutes** under ADR-0041
   (`fonts-crosextra-carlito`, `fonts-crosextra-caladea`, `fonts-noto-cjk`). That
   decision explicitly **rejected** baking licensed Microsoft Calibri/Cambria into images
   under then-current licensing / dependency policy.
2. International bank-letter readiness (**PD-7**) confirmed product intent to **pursue**
   true licensed embedding where bank letter fidelity requires named Microsoft faces —
   without inventing font files in the repository.
3. Legal reproducibility (**ADR-0060**) freezes the **ADR-0041** font set. Switching the
   freeze to licensed faces requires procured assets, image rebuild evidence, and a
   future freeze re-cut — not a silent rewrite of ADR-0041 or ADR-0060.
4. LRP Wave LR-A already closed the substitute baseline (**LR-A2** / **LR-A5**). Launch
   checklist overall remains **CONDITIONAL**; **#3b** / **#5a** stay non-GO for their
   own residuals (Word Path E; LDAP/AD). Licensed fonts are a **separate** residual and
   must not be used to flip those rows.

## Decision

### 1. Pursue licensed embedding; keep ADR-0041 as shipped authority

1. **Pursue** acquisition of a company-approved licensed font pack suitable for
   **embedding / redistribution in Docker conversion images** (true Calibri and, where
   required, Cambria — exact SKU/vendor TBD by procurement).
2. Until that pack is procured and approved for image redistribution, the **authoritative
   shipped** conversion font baseline remains **ADR-0041** (Carlito/Caladea + CJK).
3. This ADR **does not supersede** ADR-0041. It **does not** amend ADR-0041 Decision
   text to claim Calibri is shipped. A future leaf may amend ADR-0041 (or add a narrow
   successor) **only after** procurement gates below are met and architecture review
   accepts the image change.

### 2. Procurement gate (cannot ship without)

Licensed font embedding **cannot ship** until **all** of the following are true
(checklist detail: [licensed-font-embedding-procurement.md](../../operations/licensed-font-embedding-procurement.md)):

| # | Gate (confirmed requirement for ship) | Status today |
| --- | --- | --- |
| G1 | Company legal / procurement issues a redistributable license covering **server / container embedding** for DOCX→PDF conversion | **Pending** |
| G2 | Vendor pack identity recorded (SKU, version, license instrument, entitlement scope) | **Pending** |
| G3 | Approved secure delivery path for font files into build (private artifact store / mounted secret volume — **not** public git) | **Pending** |
| G4 | Dependency-policy verification that baking the pack into jammy runtime images is allowed | **Pending** |
| G5 | Dockerfile / image change + `fc-list` / smoke evidence that licensed faces are present | **Pending** (follow-on leaf) |
| G6 | ADR-0041 amendment or successor Accepted for the **new shipped** package set; ADR-0060 freeze re-cut if legal baselines must use the new set | **Pending** (follow-on) |

### 3. Honest cannot-ship / forbidden actions

Until G1–G6 clear:

- **Do not** claim true Calibri/Cambria embedding is production-ready.
- **Do not** invent, download-and-commit, or “demo” licensed `.ttf` / `.otf` binaries into
  this repository.
- **Do not** treat Carlito/Caladea as licensed Calibri/Cambria.
- **Do not** flip launch checklist **#3b GO** or **#5a GO** because of PD-7 docs.
- Optional fail-closed **code hooks** (refuse a licensed-embedding mode when the pack is
  absent) are **OUT of this leaf** — schedule a follow-on with `bdd_readiness: ready`.

### 4. LRP pairing (no GO flip)

| LRP / ops surface | Pairing note |
| --- | --- |
| LR-A2 / LR-A5 / ADR-0041 | Substitute baseline **Done** and remains shipped authority |
| ADR-0060 / legal freeze | Continues to cite ADR-0041 fonts until a post-procurement freeze re-cut |
| [launch-readiness-checklist.md](../../operations/launch-readiness-checklist.md) | Row **#3** (LR-A fonts) stays **GO** for the **substitute** baseline; **#3b** / **#5a** unchanged (**CONDITIONAL**, ≠ GO). PD-7 is an **additional residual**, not a flip trigger |
| Overall checklist | Remains **CONDITIONAL** — PD-7 pursue docs ≠ go-live |

## Consequences

- **Positive:** Durable, honest separation between shipped substitutes and the licensed
  pursue path; procurement has a concrete gate list; LRP/IBL cross-links stay truthful.
- **Negative:** Bank letters that require named Microsoft faces remain metric-substitute
  only until procurement completes; page/line fidelity vs Word may still drift for
  Calibri-styled content (same CD-PIT-01 class residual as before for *named* faces).
- **Neutral:** ADR-0041 / ADR-0060 Accepted decisions stay intact; Wave IBL-E stays Done
  without owning PD-7 as an E-leaf.

## Alternatives considered

| Alternative | Why not |
| --- | --- |
| Amend ADR-0041 Decision to “ship Calibri” now | Dishonest — licensed assets not procured; would rewrite Accepted shipping baseline |
| Bake unlicensed / scraped Microsoft fonts into images | Forbidden by licensing and dependency policy |
| Commit font binaries to git “for CI” | Forbidden — redistribution and secret/asset hygiene |
| Treat PD-7 as closed because Carlito is metric-compatible | Rejected — product confirmed **pursue licensed** embedding, not “substitutes equal Calibri” |
| Flip checklist #3b/#5a as part of PD-7 | Forbidden — unrelated residuals (Word Path E; LDAP/AD) |

## Related documents

- Shipped baseline: [ADR-0041](./0041-rendering-font-baseline.md)
- Legal freeze (still ADR-0041 fonts): [ADR-0060](./0060-legal-reproducibility-freeze.md)
- Procurement checklist: [licensed-font-embedding-procurement.md](../../operations/licensed-font-embedding-procurement.md)
- Behavior readiness (BDD N/A): [pd7-licensed-font-embedding.md](../../behavior/pd7-licensed-font-embedding.md)
- IBL §8 PD-7: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md)
- LRP fonts: [launch-readiness-program.md](../../plan/launch-readiness-program.md) (LR-A2 / LR-A5)
- Launch checklist: [launch-readiness-checklist.md](../../operations/launch-readiness-checklist.md)
