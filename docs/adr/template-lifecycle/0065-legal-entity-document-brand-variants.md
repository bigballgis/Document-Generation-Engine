---
id: ADR-0065
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, template-governance, api, frontend, rendering, doc-keeper
owners:
  - template-governance
  - api
adrNumber: "0065"
topic: template-lifecycle
related:
  - docs/behavior/ibl-e4-entity-document-brands.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/domain/domain-model.md
  - docs/security/permission-matrix.md
  - docs/product/PRD.md
  - docs/api/contract-outline.md
  - docs/adr/api/0013-api-contract-visibility-audit-and-context.md
  - docs/behavior/ibl-b5-seal-geometry.md
---

# ADR 0065: Per-Legal-Entity Document Brand Variants

## Status

**Accepted** (2026-07-20) — IBL-E4 / Task Master **#131** / F27 (document-brand half) / **PD-9**.

| Gate | Note |
| --- | --- |
| PD-9 user confirmation | **Yes** — per-legal-entity document brands vs UI theming only（**2026-07-19**） |
| BDD lock | [ibl-e4-entity-document-brands.md](../../behavior/ibl-e4-entity-document-brands.md) **ready** — **BDD-IBL-E4-001…017** lock **E4-C\*** defaults; **no remaining product fork** for this leaf |
| File status | **Accepted** (2026-07-20) — doc-keeper stage 3; Decision = E4-C\* |

`sourceOfTruth: true` while Accepted.

This ADR does **not** claim IBL-E4 implementation Done, flip checklist **#3b** / **#5a**, remove SPECIMEN (PD-6), embed licensed fonts (PD-7), activate IBL-E5…E7 / #119, close F27 `effectiveFrom` / bulk re-pin (→ **IBL-E5**), rewrite shell `REDBC`/`GREENBC` as document brands, or claim Wave E / IBL program Done / go-live.

**Amends** [ADR-0013](../api/0013-api-contract-visibility-audit-and-context.md): optional `context.legalEntityCode` on the safe whitelist. Does **not** amend ADR-0062 / ADR-0063 / ADR-0064 / ADR-0061. Orthogonal to IBL-B5 seal geometry and UI BrandPreset (`REDBC`/`GREENBC`).

## Context

International bank letters need **document** brand variants (letterhead / logo / optional default seal) selectable **per legal entity**. Today `REDBC` / `GREENBC` are **management UI themes only** and do not drive rendered letter artifacts (**F27** / **PD-9**).

Today:

- Shell brand switcher toggles `html[data-brand]` chrome + UI logo slots only.
- No group-scoped DocumentBrand or LegalEntity catalogs.
- Runtime `context` whitelist (ADR-0013 + E2 amendment) has no legal-entity axis for document branding.
- Seal placement remains governed by IBL-B5 when a seal is applied.

**PD-9** confirms the product boundary: document brands selectable per legal entity, **orthogonal** to UI theming. F27’s `effectiveFrom` hard block / bulk re-pin remain **IBL-E5**. Management UI for catalogs + entity↔brand binding is **in scope** (`frontend_ui_in_scope=true`).

## Decision

1. **Orthogonal separation**  
   **DocumentBrand ≠ UI BrandPreset.** Shell `REDBC`/`GREENBC` theme switches **must not** change document artifact brand assets. Document brand resolution **must not** rewrite `html[data-brand]` / shell logos.

2. **Dual catalogs (group-scoped)**  
   Introduce governable **DocumentBrand** and **LegalEntity** catalogs (logical-delete conventions aligned with platform). Do **not** hardcode brand codes into template body content.

3. **DocumentBrand minimum contract**  
   `documentBrandCode` (group-unique, trim, case-sensitive stable code, suggested max **64**), display name (English-first i18n key or localized field — one approach fixed in OpenAPI), `status` ∈ {`ACTIVE`,`INACTIVE`}, required `logoObjectRef` (authorized object-storage ref), optional `defaultSealObjectRef`, optional `letterheadLegalName` (non-sensitive short text, max **256**). No full visual design system or licensed font pack in this leaf.

4. **LegalEntity minimum contract**  
   `legalEntityCode` (group-unique, trim, stable, suggested max **64**), display name, `status` ∈ {`ACTIVE`,`INACTIVE`}, required `documentBrandCode` referencing a same-group DocumentBrand. Exactly one brand bound at a time; re-bind = update + audit.

5. **Seed default brand**  
   Each group migration/init has `documentBrandCode=PLATFORM_DEFAULT` (ACTIVE; platform placeholder logo; **not** a REDBC/GREENBC UI code). Calls omitting legal entity must **not** silently borrow UI theme assets.

6. **Group default legal-entity fallback**  
   Group may configure optional `defaultLegalEntityCode`. When runtime omits `context.legalEntityCode`: if default entity exists and is ACTIVE → use its bound brand; else → `PLATFORM_DEFAULT`. **Forbidden** to fall back to UI `REDBC`/`GREENBC`.

7. **Context whitelist extension**  
   ADR-0013 whitelist adds optional string `legalEntityCode`. Unknown fields still `400 REQUEST_BODY_INVALID`. Non-blank values may enter `contextSummary` (non-sensitive). No customer names/accounts in `context`.

8. **Resolve rules (deterministic)**  
   (1) Non-blank request `legalEntityCode` → look up same-group LegalEntity; (2) missing/INACTIVE/unknown → **422** (`LEGAL_ENTITY_UNKNOWN` / `LEGAL_ENTITY_INACTIVE` — stable codes in OpenAPI); (3) bound brand missing/INACTIVE → **422** (`DOCUMENT_BRAND_INACTIVE`); (4) else `ResolvedDocumentBrand`. Omit legalEntity → Decision 6. Match = trim then **exact** (case-sensitive).

9. **Apply to document artifacts**  
   On generate / preview / test-generation **same path**: logo → letterhead/brand image slot; if brand provides `defaultSealObjectRef` and template/binding has no explicit seal → may supply default seal (explicit sealRef **wins**); `letterheadLegalName` into controlled letterhead text slot when declared. No requirement to rewrite all historical master pixel layouts; missing slots → non-blocking fidelity warning (fixed code); **must not** 500 or silently substitute UI chrome.

10. **Seal geometry orthogonal**  
    Applied seals still run IBL-B5 authorized-area checks; out-of-area → existing fail-closed. This leaf does not claim writer absolute positioning Done.

11. **Optional template allow-list**  
    Package-level optional `allowedDocumentBrandCodes: string[]`. Empty/absent = any ACTIVE group document brand (incl. `PLATFORM_DEFAULT`). Non-empty and resolved brand ∉ list → **422** `DOCUMENT_BRAND_NOT_ALLOWED`. Writable window aligns with existing package metadata draft rules (one approach fixed in OpenAPI/impl).

12. **No automatic package selection**  
    Paths remain pinned to concrete template+version (same as E1-C6 / E2-C12). Brand resolve affects artifact brand slots only.

13. **Management API**  
    DocumentBrand and LegalEntity list/create/update (get as needed); group `defaultLegalEntityCode` read/write; template detail echoes `allowedDocumentBrandCodes`; generate/preview/test summaries echo `resolvedLegalEntityCode` + `resolvedDocumentBrandCode` (non-sensitive). OpenAPI synced. Unauthorized 403/404 conventions unchanged.

14. **Permissions**  
    Catalog writes = admins (`GROUP_ADMIN`/`GLOBAL_ADMIN`, group scope). Template allow-list write = existing template authoring write boundary. **No new roles.** Permission matrix gains behavior/capability rows (not a 9th role).

15. **Audit**  
    Brand/entity create, re-bind, deactivate, group-default changes → management audit (codes + non-sensitive diff). Runtime success invocation/audit summary includes `legalEntityCode` (if any) + resolved `documentBrandCode`. Forbidden: variables / customer plaintext / asset binaries.

16. **Import/export**  
    Template export may carry `allowedDocumentBrandCodes`; full brand binary asset packs are **not** required in this leaf (catalogs are group master data). Import allow-list referencing unknown brands → validation failure (422 or publish gate — one stable code).

17. **Management UI (required)**  
    (1) DocumentBrand catalog; (2) LegalEntity catalog + document-brand selector; (3) group default legal entity / fallback copy; (4) optional template allow-list advanced section; (5) preview/detail resolved brand codes. Bank OA + English-first i18n. Shell brand switcher remains UI-only.

18. **UI chrome regression**  
    Existing REDBC/GREENBC switcher and dual-brand golden screenshot semantics must not be broken; E2E must show document brand config and UI theme can change independently.

19. **SPECIMEN / PD-6 / PD-7 / Word**  
    No regenerate watermark change; no licensed fonts; no #119 Word evidence invented.

20. **Out of scope**  
    IBL-E5…E7, checklist **#3b**/**#5a** GO, Wave E / IBL program Done / go-live, treating UI `REDBC`/`GREENBC` as document brand codes, auto-selecting template packages by legal entity, inventing company-global brand MDM / LDAP sync.

Normative behavior scenarios: **BDD-IBL-E4-001…017** in [ibl-e4-entity-document-brands.md](../../behavior/ibl-e4-entity-document-brands.md) (**E4-C1…C22**).

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| **A. Reuse UI `REDBC`/`GREENBC` as document brand codes** | Rejected — conflicts with PD-9 «vs UI theming only»; breaks chrome/document orthogonality. |
| **B. Hardcode brand assets into each template body** | Rejected — not selectable per legal entity; unmaintainable across entities. |
| **C. Auto-select template package by legal entity** | Rejected — violates path pin (E4-C12 / E1/E2). |
| **D. DocumentBrand + LegalEntity catalogs + runtime resolve/apply** (this Decision) | **Accepted** — fail-closed, auditable, UI+API in scope, orthogonal to shell themes. |

## Consequences

- F27 document-brand gap is closed at the **decision** layer; implementation remains IBL-E4 / #131 delivery work (Accepted ≠ impl Done).
- ADR-0013 gains optional `legalEntityCode`; unknown context fields remain fail-closed.
- Shell BrandPreset (`REDBC`\|`GREENBC`) stays a separate FE union — do not merge DocumentBrand into it.
- Management UI + E2E/UIUX are mandatory for this leaf (`frontend_ui_in_scope=true`).
- F27 `effectiveFrom` / bulk re-pin half remains open toward **IBL-E5**.

## Related Documents

- Behavior: [ibl-e4-entity-document-brands.md](../../behavior/ibl-e4-entity-document-brands.md)
- Program: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md) §7–§8 (IBL-E4, PD-9, F27)
- Context baseline: [0013-api-contract-visibility-audit-and-context.md](../api/0013-api-contract-visibility-audit-and-context.md)
- Seal geometry: [ibl-b5-seal-geometry.md](../../behavior/ibl-b5-seal-geometry.md)
- Domain: [domain-model.md](../../domain/domain-model.md)
- Permissions: [permission-matrix.md](../../security/permission-matrix.md)
- PRD: [PRD.md](../../product/PRD.md)
- API: [contract-outline.md](../../api/contract-outline.md), [openapi-v1.yaml](../../api/openapi-v1.yaml)
