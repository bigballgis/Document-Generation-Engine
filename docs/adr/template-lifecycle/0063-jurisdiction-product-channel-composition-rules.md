---
id: ADR-0063
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, template-governance, content-module-governance, api, doc-keeper
owners:
  - template-governance
  - api
adrNumber: "0063"
topic: template-lifecycle
related:
  - docs/behavior/ibl-e2-jurisdiction-rule-engine.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/domain/domain-model.md
  - docs/product/PRD.md
  - docs/api/contract-outline.md
  - docs/adr/api/0013-api-contract-visibility-audit-and-context.md
  - docs/adr/template-lifecycle/0062-locale-variant-template-clause-model.md
  - docs/behavior/ce-k08-clause-legal-metadata.md
---

# ADR 0063: Jurisdiction / Product / Channel Composition Inclusion Rules

## Status

**Accepted** (2026-07-20) — IBL-E2 / Task Master **#129** / F25 / **PD-5**.

| Gate | Note |
| --- | --- |
| PD-5 user confirmation | **Yes** — proceed with jurisdiction/product/channel composition engine via ADR + IBL-E2（**2026-07-19**） |
| BDD lock | [ibl-e2-jurisdiction-rule-engine.md](../../behavior/ibl-e2-jurisdiction-rule-engine.md) **ready** — **BDD-IBL-E2-001…016** lock **E2-C\*** defaults; **no remaining product fork** for this leaf |
| File status | **Accepted** (2026-07-20) — doc-keeper stage 3; Decision = E2-C\* |

`sourceOfTruth: true` while Accepted.

This ADR does **not** claim IBL-E2 implementation Done, flip checklist **#3b** / **#5a**, remove SPECIMEN (PD-6), activate IBL-E3…E7 / #119, or claim Wave E / IBL program Done / go-live.

**Amends** [ADR-0013](../api/0013-api-contract-visibility-audit-and-context.md) context whitelist (adds optional `jurisdiction` / `product`; clarifies composition-control use of the three axes). Does **not** amend ADR-0062 locale semantics or CE-K08 field meanings.

## Context

International bank letters need the **same published template version** to include different pinned content-module references depending on jurisdiction, product, and channel — deterministically and with an auditable inclusion summary (**F25** / **PD-5**).

Today:

- CE-K08 provides optional **version** legal metadata (`jurisdiction`, effective dating) plus catalog filters and a publish expiry gate — not a composition engine.
- Existing composition rules are anchor-visibility rules (`conditionExpression` → `targetAnchorId`) evaluated against **variables**, not against runtime `context` dimensions.
- Runtime `context` whitelist (ADR-0013) includes `channel` and `locale` but not `jurisdiction` / `product`; `channel` was documented primarily for tracing/stats.
- ADR-0062 locale-variant packages are orthogonal (body language ≠ jurisdiction).

**PD-5** confirms the product boundary: introduce a jurisdiction / product / channel composition inclusion engine. Wave E siblings (multi-stage legal approval, entity brands, effectiveFrom bulk, nesting, RTL, SPECIMEN removal, licensed fonts, Word) remain **out of IBL-E2**. Management UI rule editor is **out of this leaf** (`frontend_ui_in_scope=false`).

## Decision

1. **Composition axes**  
   Composition dimensions are optional non-sensitive short strings: `jurisdiction`, `product`, `channel`. They are **not** E1 `locale`, UI i18n, or outbound delivery channel (PD-1).

2. **Context whitelist (amends ADR-0013)**  
   Add optional `context.jurisdiction` and `context.product` (strings). Existing `channel` **additionally** participates in composition matching (field name unchanged). Unknown `context` fields still return `400 REQUEST_BODY_INVALID`. Audit `contextSummary` includes non-blank values of the three axes. `sourceSystem` / `businessRequestId` / `upstreamTraceId` / `scenario` do **not** enter inclusion matching. Values remain non-PII / non-variable (trim; blank → absent; suggested max length **128**; match = case-insensitive exact after trim; no fuzzy/prefix/regex).

3. **Composition Inclusion Rule (new rule kind)**  
   Structured inclusion rules coexist with, and are orthogonal to, anchor-visibility composition rules. Inclusion rules are **not** evaluated by `ConditionExpressionEvaluator` / variables.

4. **Mount point**  
   Inclusion rules hang on the **template version** (same version line as CM references / visibility rules). Drafts are writable; publish locks; published versions are immutable.

5. **Minimum rule contract**  
   Each rule has: `ruleId` (unique non-empty string within the version), `referenceKey` (must point at a CM reference declared on the same version), `match.jurisdiction?` / `match.product?` / `match.channel?` (**at least one** axis non-empty), optional `priority` (integer, default `0`), optional `requiredInclusion` (boolean, default `false`).

6. **Match semantics**  
   Declared axes on one rule are **AND**. Undeclared axes are wildcards. Missing/blank request axis → that rule does **not** match (no `400` solely for omission).

7. **Deterministic inclusion algorithm**  
   For each pinned `referenceKey`:  
   (1) if **no** inclusion rule targets it → **INCLUDE** (backward compatible);  
   (2) else evaluate rules ordered by (`priority` ascending, then `ruleId` lexicographic ascending); **any** match → **INCLUDE** (OR), recording the **first** matching `ruleId`;  
   (3) if none match and any targeting rule has `requiredInclusion=true` → **422** `COMPOSITION_INCLUSION_UNSATISFIED` (no generation);  
   (4) if none match and none required → **EXCLUDE** (skip CM expand; not `500`).  
   The same evaluator is used for runtime generate, preview, and test generation.

8. **Optional CE-K08 consistency (fail-closed when both sides set)**  
   When a CM is **INCLUDE**d, its pinned version `jurisdiction` is non-null, request `context.jurisdiction` is non-blank, and they differ (case-insensitive) → **422** `CONTENT_MODULE_JURISDICTION_MISMATCH`. Either side blank → skip. Does not change K08 expiry / catalog filter semantics. **No** product/channel fields on `content_module_version` in this leaf.

9. **No automatic template-package selection**  
   Callers continue to path-pin a specific template + version (same as E1-C6 / PRD). The engine only decides in-version CM inclusion.

10. **Management API (API-first)**  
    Draft versions expose `GET` + `PUT` at  
    `/api/management/v1/templates/{templateId}/composition-inclusion-rules`  
    (parallel to existing visibility `/rules`; not conflated with `conditionExpression` payloads).  
    PUT validation failures → **422** `COMPOSITION_INCLUSION_RULE_INVALID` (unknown `referenceKey`, empty `match`, duplicate `ruleId`). Detail/export views echo the rule set. Permissions reuse existing template-authoring / `authorTemplates` boundaries (no new roles); unauthorized writes → `403`/`404` per existing convention.

11. **Publish gate**  
    Hard gate: every inclusion rule `referenceKey` must resolve to a declared CM reference — `PublishGateCheckCode.COMPOSITION_INCLUSION_REFERENCE_INVALID`. Not every CM reference must have an inclusion rule.

12. **Audit**  
    Successful runtime paths write non-sensitive `compositionInclusionSummary`: per evaluated `referenceKey` → `INCLUDE`|`EXCLUDE` + `matchedRuleId` (`NONE_DEFAULT` when included by the no-rule default). No clause body, no variable plaintext. Management rule changes use existing template-update audit payloads (include `ruleId` list when practical).

13. **Import / export**  
    Export packages carry inclusion rules; import preserves them and re-runs PUT validation.

14. **Frontend**  
    This leaf does **not** deliver a management UI rule editor (`frontend_ui_in_scope=false`). Residual: later leaf may wire `TemplateRuleConfigurator`. Authors configure via management API / fixtures.

15. **Out of scope**  
    IBL-E3…E7, PD-6/PD-7, #119 Word, company jurisdiction/product master-data catalogs, LDAP mapping, outbound delivery, replacing visibility expression rules with this engine, checklist **#3b**/**#5a** GO.

Normative behavior scenarios: **BDD-IBL-E2-001…016** in [ibl-e2-jurisdiction-rule-engine.md](../../behavior/ibl-e2-jurisdiction-rule-engine.md) (**E2-C1…C22**).

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| **A. Encode axes in variable `conditionExpression`** | Rejected — mixes PII/variables with composition control; weak audit; fights fail-closed bank defaults. |
| **B. Soft-warn / best-effort include on mismatch** | Rejected — silent wrong-jurisdiction clauses are worse than fail-closed. |
| **C. Auto-select template package by jurisdiction** | Rejected — conflicts with path-pinned template identity (PRD / E1-C6). |
| **D. Extend CM version with product/channel metadata** | Rejected for this leaf — product/channel stay on rule `match.*` vs `context.*` only. |
| **E. Structured Composition Inclusion Rules on template version** (this Decision) | **Accepted** — deterministic, auditable, orthogonal to visibility rules / CE-K08 / locale. |

## Consequences

- F25 “no jurisdiction/product/channel composition engine” is closed at the **decision** layer; implementation remains IBL-E2 / #129 delivery work.
- Upstream callers may pass optional composition axes; templates without inclusion rules keep current “always include pinned CMs” behavior.
- Authors govern inclusion via management API; publish fails closed on dangling rule references.
- Runtime inclusion sets are repeatable for the same published version + context axes and appear in audit summaries.
- ADR-0013 / contract-outline / OpenAPI context whitelist expand by two optional fields; `channel` gains a documented composition role.
- Management UI for rule editing remains a residual (not a Done gate for this leaf).

## Related Documents

- Behavior: [ibl-e2-jurisdiction-rule-engine.md](../../behavior/ibl-e2-jurisdiction-rule-engine.md)
- Program: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md) §7–§8 (IBL-E2, PD-5, F25)
- Domain: [domain-model.md](../../domain/domain-model.md) §2.9 Composition Rule / inclusion, §2.9.2 Content Module, §3.2 context whitelist
- Context baseline: [0013-api-contract-visibility-audit-and-context.md](../api/0013-api-contract-visibility-audit-and-context.md)
- Locale (orthogonal): [0062-locale-variant-template-clause-model.md](./0062-locale-variant-template-clause-model.md)
- CE-K08 (orthogonal + optional mismatch hook): [ce-k08-clause-legal-metadata.md](../../behavior/ce-k08-clause-legal-metadata.md)
- API: [contract-outline.md](../../api/contract-outline.md), [openapi-v1.yaml](../../api/openapi-v1.yaml)
