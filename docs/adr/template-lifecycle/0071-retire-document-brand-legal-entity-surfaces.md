---
id: ADR-0071
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-21
deciders: product, template-governance, architecture, frontend, doc-keeper
owners:
  - template-governance
  - frontend
adrNumber: "0071"
topic: template-lifecycle
related:
  - docs/behavior/system-normalization-program.md
  - docs/plan/system-normalization-program-2026-07.md
  - docs/adr/template-lifecycle/0065-legal-entity-document-brand-variants.md
  - docs/behavior/ibl-e4-entity-document-brands.md
  - docs/product/catalog-navigation-ux.md
  - docs/product/business-terminology-guide.md
---

# ADR 0071: Retire DocumentBrand / LegalEntity Product Surfaces (D1)

## Status

**Accepted** (2026-07-21) — user-confirmed System Normalization Program **D1** decision lock.

| Gate | Note |
| --- | --- |
| User confirmation | **Yes** — 2026-07-21 (LOCKED — do not reopen) |
| Behavior SoT | [system-normalization-program.md](../../behavior/system-normalization-program.md) §2.5 / §7 |
| Supersedes | [ADR-0065](./0065-legal-entity-document-brand-variants.md) **management UX / product surfaces** going forward |
| Runtime simplify | **Wave 6** `sys-norm-d1-brands` |
| Nav removal | May start **Wave 1** after this ADR Accepted |
| Checklist | Does **not** flip **#3b** / **#5a** |

`sourceOfTruth: true` while Accepted.

**Accepted ≠ Wave 6 Done.** Runtime simplify, catalog deletion, and migration timing remain
Wave 6 (with Wave 1 FE nav hide allowed once this decision is Accepted).

## Context

ADR-0065 (IBL-E4 / PD-9) introduced group-scoped **DocumentBrand** and **LegalEntity**
catalogs and management UX so letterhead/logo/seal could vary per legal entity, orthogonal
to shell UI themes (`REDBC`/`GREENBC`).

For this internal bank OA surface, the understanding cost of separate brand/entity catalogs
is too high. Letterhead/logo/seal governance belongs in **Letterhead (master)** assets and
workflows. Dual-catalog product surfaces are withdrawn going forward.

ADR-0065 remains the **historical Accepted** decision for IBL-E4 delivery evidence. This ADR
**does not silently rewrite** ADR-0065 Decision body; it **supersedes the product-surface
requirement** for ongoing DocumentBrand/LegalEntity management UX.

## Decision

1. **Retire Document brands + Legal entities product surfaces**  
   Management navigation and catalogs for DocumentBrand / LegalEntity are **not** required
   product surfaces going forward. Operators manage letterhead / logo / seal via
   **Letterhead (master)** flows.

2. **Supersedes / withdraws IBL-E4 management UX (ADR-0065 product surface)**  
   New work must not reintroduce DocumentBrand/LegalEntity management UX as a required
   surface. Historical IBL-E4 implementation evidence and ADR-0065 Decision text remain for
   audit/history; product direction follows D1.

3. **Keep Legal holds**  
   Legal hold navigation and behavior are **out of scope for retirement** under D1.

4. **Shell themes orthogonal**  
   Shell `REDBC` / `GREENBC` remain **UI-only** chrome. They are not DocumentBrand MDM and
   must not be reintroduced as such.

5. **Promotion / export packs**  
   UAT→PROD promotion packs **must not** require DocumentBrand/LegalEntity sidecar catalogs.
   Letterhead/master inclusion follows two-phase P2 (no skip of APPROVED master state).

6. **Sequencing**  
   - Decision lock = this ADR Accepted (Wave 0).  
   - FE nav removal **may** begin in Wave 1.  
   - Runtime simplify / hard delete / transitional fail-closed rules = Wave 6 BDD + code.

## Acceptance scenarios (decision lock)

| ID | Summary |
| --- | --- |
| [BDD-SYS-NORM-D1-001](../../behavior/system-normalization-program.md#bdd-sys-norm-d1-001--product-surfaces-retired) | Product surfaces retired |
| [BDD-SYS-NORM-D1-002](../../behavior/system-normalization-program.md#bdd-sys-norm-d1-002--supersede-adr-0065-management-ux-requirement) | Supersede ADR-0065 management UX requirement |
| [BDD-SYS-NORM-D1-003](../../behavior/system-normalization-program.md#bdd-sys-norm-d1-003--shell-themes-orthogonal) | Shell themes orthogonal |
| [BDD-SYS-NORM-D1-004](../../behavior/system-normalization-program.md#bdd-sys-norm-d1-004--runtime-simplify-fail-closed-wave-6-stub-pointer) | Runtime simplify fail-closed (Wave 6 stub) |
| [BDD-SYS-NORM-D1-005](../../behavior/system-normalization-program.md#bdd-sys-norm-d1-005--export-packs-omit-brandentity-sidecar) | Export packs omit brand/entity sidecar |

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| Keep dual catalogs + polish UX | Rejected — understanding cost too high (user 2026-07-21) |
| Merge brand into UI BrandPreset | Rejected — shell themes stay UI-only |
| Remove Legal holds with D1 | Rejected — Legal holds kept |
| Silent rewrite of ADR-0065 Decision | Rejected — supersession banner + this ADR |

## Consequences

- ADR-0065 index/status notes mark **product surface superseded** by this ADR; Decision body
  preserved.
- Wave 1 may hide nav entries; Wave 6 owns runtime/API/data simplify.
- Promotion pack design (Wave 7) omits brand/entity sidecars.
- Missing nav icons for brands/entities remain defects only while those surfaces briefly
  remain; after D1 removal they are moot.

## Related Documents

- Behavior: [system-normalization-program.md](../../behavior/system-normalization-program.md)
- Program: [system-normalization-program-2026-07.md](../../plan/system-normalization-program-2026-07.md)
- Superseded surface: [0065-legal-entity-document-brand-variants.md](./0065-legal-entity-document-brand-variants.md)
- Historical BDD: [ibl-e4-entity-document-brands.md](../../behavior/ibl-e4-entity-document-brands.md)
- Terminology: [business-terminology-guide.md](../../product/business-terminology-guide.md)
