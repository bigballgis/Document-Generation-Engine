---
id: ADR-0062
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-19
deciders: architecture, template-governance, content-module-governance, doc-keeper
owners:
  - template-governance
  - content-module-governance
adrNumber: "0062"
topic: template-lifecycle
related:
  - docs/behavior/ibl-e1-locale-variant-model.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/domain/domain-model.md
  - docs/product/PRD.md
  - docs/api/contract-outline.md
  - docs/adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md
  - docs/adr/template-lifecycle/0021-template-testing-approval-release-governance.md
---

# ADR 0062: Locale-Variant Template and Clause Model

## Status

**Accepted** (2026-07-19) — IBL-E1 / Task Master **#128** / F24 / **PD-4**.

Formerly drafted as **ADR-0061** (`0061-locale-variant-template-clause-model.md`); **renumbered to ADR-0062** on **2026-07-19** due to collision with documentation-governance **ADR-0061** (audience manuals SoT derivatives). Decision content and Accepted status are unchanged under the new number.

| Gate | Note |
| --- | --- |
| PD-4 user confirmation | **Yes** — proceed with locale-variant model via ADR + IBL-E1（2026-07-19） |
| BDD lock | [ibl-e1-locale-variant-model.md](../../behavior/ibl-e1-locale-variant-model.md) **ready** — **BDD-IBL-E1-001…018** lock **E1-C\*** defaults; **no remaining product fork** for this leaf |
| File status | **Accepted** (2026-07-19) — doc-keeper stage-3 follow-up after PD-4 + BDD readiness; renumbered 0061→0062 (collision clear) |

`sourceOfTruth: true` while Accepted.

This ADR does **not** claim IBL-E1 implementation Done, flip checklist **#3b** / **#5a**, or change SPECIMEN / PD-6 watermark policy.

## Context

International bank letters need **distinct authored bodies** per language (for example `zh-CN` vs `en-US`). Today:

- A template package and a content module each carry **one** body with **no** locale declaration (`CreateTemplateRequest` / `CreateContentModuleRequest` lack `locale`) — finding **F24**.
- Runtime `context.locale` (PRD / ADR-0011 / ADR-0056) localizes **compute** (amounts, dates, amount-in-words language) and is replayed on regenerate (IBL-A6). It does **not** select a language-specific content asset.
- Content-module **jurisdiction** / effective dating (CE-K08) is orthogonal legal metadata on **versions**, not body language.
- Template package versioning (hub + dev/release lines, ADR-0021) and content-module review/lifecycle already provide bank-grade per-asset governance.

**PD-4** confirms the product boundary: introduce a locale-variant template/clause model. Wave E siblings (jurisdiction engine, multi-stage legal approval, entity brands, RTL, SPECIMEN removal, licensed fonts) remain **out of IBL-E1**.

## Decision

1. **Body locale declaration**  
   Each **Template** package and each **Content Module** package stores a required BCP-47 `locale` string declaring the language of its authored body. Locale is **not** hung on a single version row; versions evolve that language body under existing lifecycle rules.

2. **Optional locale variant family**  
   Optional `localeVariantFamilyId` (UUID) groups translation siblings. Within the same `groupCode`, `(localeVariantFamilyId, locale)` is unique when the family id is non-null. Empty family id means a standalone asset.

3. **Identity and runtime addressing unchanged**  
   Callers continue to address a **specific** template via path (`externalId` / id + release version). The family does **not** provide silent runtime “pick package by locale” routing. `externalId` / `moduleCode` uniqueness rules remain.

4. **Language compatibility**  
   Compatibility compares **primary language subtags** (case-insensitive): `en` ≡ `en-US` ≡ `en-GB`; `zh` ≡ `zh-CN`; `en` ↛ `zh`.

5. **Publish gate**  
   Publishing a template fail-closes if any pinned content-module reference is not language-compatible with the template locale (new publish gate code, e.g. `CONTENT_MODULE_LOCALE_MISMATCH`), orthogonal to `CONTENT_MODULE_EFFECTIVE_EXPIRED`.

6. **Runtime check**  
   When `context.locale` is non-blank, it must be language-compatible with the pinned template’s locale or the call fails closed (e.g. `422 TEMPLATE_LOCALE_MISMATCH`). When locale is omitted/blank, skip this check; compute defaults remain per ADR-0056 (`zh-CN`).

7. **No automatic translation**  
   The platform does not machine-translate or merge multi-language bodies into one package.

8. **Migration**  
   Existing rows backfill `locale = zh-CN` to match the compute default, documented as a migration default rather than a historical business declaration.

9. **API + UI**  
   Management create/update/list/detail expose `locale` and `localeVariantFamilyId`; catalogs support optional `locale` filter. Management UI requires locale on create, supports catalog filter, and navigates family siblings. Permissions reuse existing template/CM roles (fail-closed).

10. **Out of scope for this ADR**  
    Jurisdiction composition engine (IBL-E2), multi-stage legal approval (IBL-E3), legal-entity brands (IBL-E4), `effectiveFrom` bulk tools (IBL-E5), nesting governance (IBL-E6), RTL (IBL-E7), SPECIMEN removal (PD-6), licensed font embedding (PD-7), Word baselines (#119), checklist #3b/#5a GO.

Normative behavior scenarios: **BDD-IBL-E1-001…018** in [ibl-e1-locale-variant-model.md](../../behavior/ibl-e1-locale-variant-model.md).

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| **A. Locale on version only** (same moduleCode, en/zh as versions) | Rejected — conflates translation siblings with revision history; breaks “each version is an evolution of one body” and approval semantics. |
| **B. Multi-body JSON inside one package** keyed by locale | Rejected — fights hub/dev/release IA, publish locking, and per-language audit; high blast radius. |
| **C. Runtime-only routing** (one package, select body by `context.locale`) | Rejected — reintroduces silent body selection; weak audit; conflicts with path-pinned template identity in PRD. |
| **D. Separate packages + family + declared locale** (this Decision) | **Accepted** — reuses package/CM governance; explicit; fail-closed; matches invocation locale tag space. |
| **E. Soft-warn on locale mismatch** (no 422 / no publish hard gate) | Rejected for bank-grade default — silent cross-language generation is worse than fail-closed. |

## Consequences

- F24 “single undocumented body” ends: every template/CM has an explicit locale.
- Authors maintain EN/ZH (etc.) as sibling assets with independent test/approval/release.
- Upstream systems must call the correct published template; optional `context.locale` both drives compute and validates language fit when present.
- Management UI and OpenAPI gain locale fields/filters; E2E required for IBL-E1.
- Migration labels all legacy rows `zh-CN`; teams that already authored English-only bodies under the old model must **correct** locale (and optionally re-family) after upgrade — honest residual, not silent dual-body.
- IBL-E2+ may later compose by jurisdiction **without** redefining body locale (orthogonal axes).

## Related Documents

- Behavior: [ibl-e1-locale-variant-model.md](../../behavior/ibl-e1-locale-variant-model.md)
- Program: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md) §7–§8 (IBL-E1, PD-4, F24)
- Domain: [domain-model.md](../../domain/domain-model.md) §2.7 Template, §2.9.2 Content Module
- Compute locale: [0056-whitelist-variable-compute-dsl-bounds.md](../rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md)
- Lifecycle: [0021-template-testing-approval-release-governance.md](./0021-template-testing-approval-release-governance.md)
