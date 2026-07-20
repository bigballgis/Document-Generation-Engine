---
id: ADR-0067
type: ADR
status: Accepted
sourceOfTruth: true
date: 2026-07-20
deciders: architecture, template-governance, content-module-governance, api, doc-keeper
owners:
  - content-module-governance
  - template-governance
  - api
adrNumber: "0067"
topic: template-lifecycle
related:
  - docs/behavior/ibl-e6-clause-nesting-governance.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/behavior/ce-g05-annual-review-fts.md
  - docs/domain/domain-model.md
  - docs/api/contract-outline.md
  - docs/api/openapi-v1.yaml
  - docs/adr/template-lifecycle/0066-effectivefrom-publish-and-bulk-repin.md
  - docs/adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md
---

# ADR 0067: Clause Nesting Module-Graph Governance

## Status

**Accepted** (2026-07-20) — IBL-E6 / Task Master **#133** / F28.  
doc-keeper stage 3 — Decision = BDD **E6-C\***; OpenAPI / contract-outline / domain / permission / index pointers synced. **Accepted ≠ IBL-E6 implementation Done.**

| Gate | Note |
| --- | --- |
| Wave E / F28 | **Yes** — governed CM↔CM module graph; depth; deep where-used; cycles fail-closed; transitive pins |
| BDD lock | [ibl-e6-clause-nesting-governance.md](../../behavior/ibl-e6-clause-nesting-governance.md) **ready** — **BDD-IBL-E6-001…018** lock **E6-C1…C21**; **no remaining product fork** for this leaf |
| File status | **Accepted** (2026-07-20) — doc-keeper stage 3; Decision = E6-C\* |

`sourceOfTruth: true` while Accepted.

This ADR does **not** claim IBL-E6 implementation Done, flip checklist **#3b** / **#5a**, remove SPECIMEN (PD-6), embed licensed fonts (PD-7), activate IBL-E7 / #119, rewrite E1–E5 semantics, or claim Wave E / IBL program Done / go-live.

## Context

International bank letters may nest clauses via structured `contentModuleRef` inside content-module `content_structure_json`. Today:

1. Render recursively expands pins (`StructuredContentDocxExpandSupport`) with **no** write-path module graph, max depth, or cycle governance.
2. CE-G05 `GET …/content-modules/{id}/where-used` lists only **direct** `template_content_module_reference` rows — nested usage of Child via Parent is invisible.
3. F28 requires: nesting depth governed; where-used reports deep refs; cycles fail-closed.

## Decision

1. **Governed CM↔CM module graph**  
   Edges are derived from `contentModuleRef` nodes in a CM version’s `content_structure_json`. The graph is maintained on the **successful write path** as a queryable nesting-edge projection (explicit edge table or equivalent — physical schema is an implementation detail). Template pins remain orthogonal template→CM bindings.

2. **Depth**  
   Nesting depth of a saved CM version = length (edge count) of the **longest simple path** from that version’s root along nesting edges. No edges → depth `0`. Platform hard limit **`8`**, isomorphic to `ComputeDslLimits.MAX_NESTING_DEPTH` / `MAX_DEPENDENCY_DEPTH`. `nestingDepth > 8` → fail-closed (`CONTENT_MODULE_NESTING_DEPTH_EXCEEDED`). Depth `== 8` → allowed.

3. **Cycles**  
   Self / mutual / indirect cycles → reject structure write (`CONTENT_MODULE_NESTING_CYCLE`) and fail publish-gate. Render must fail closed (no stack overflow / silent truncate) with the same stable code when a cycle is encountered at expand time.

4. **Nest target resolution on write**  
   Each `contentModuleRef.referenceKey` must resolve to an authorized-visible content module (normalization/uppercase per existing pin-key conventions). Unresolved / cross-group invisible → `422 CONTENT_MODULE_NESTING_TARGET_UNRESOLVED`.

5. **Deep where-used**  
   Extend CE-G05 where-used: include templates that use the module via nesting closure (not only direct pins). Rows expose:
   - `referenceKind` = `DIRECT` \| `NESTED` (required; direct pins = `DIRECT`)
   - `nestingDepth` (integer ≥ 0; `DIRECT` → `0`; `NESTED` → path edge count ≥ 1)
   - `nestingPathSummary` (nullable string; non-sensitive `moduleCode` chain for `NESTED`; null/omit for `DIRECT`; **never** clause body)  
   Auth unchanged (§5.1; tester 403). Authority = module graph + template pins on ancestors — **not** full template binding JSON scan.

6. **Lifecycle impact closure**  
   Content-module lifecycle impact preview must use the **same** nested template closure as deep where-used.

7. **Transitive pins at publish**  
   For each directly pinned CM version, every nested `referenceKey` in its closure must exist as a pin on that template version with non-empty resolvable structure. Missing → publish hard-fail via dedicated `PublishGateCheckCode.CONTENT_MODULE_NESTING_UNPINNED` (**not** overloaded onto `CONTENT_MODULE_REFERENCES`). Publish also fails on cycle / depth exceed in the pinned closure via `CONTENT_MODULE_NESTING_CYCLE` / `CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` (defense in depth).

8. **Runtime**  
   Already-published locked versions are not rewritten; generate/preview still fail closed on cycle / missing nested structure.

9. **API-first**  
   `frontend_ui_in_scope=false`. No new roles/capabilities.

10. **Out of scope**  
    IBL-E7, PD-6/7, #119, per-tenant depth config UI, nesting graph management UI as Done gate, rewriting E1–E5.

### Stable OpenAPI codes (locked)

| Surface | Stable code |
| --- | --- |
| CM structure write (cycle) | `ErrorCode.CONTENT_MODULE_NESTING_CYCLE` (422) |
| CM structure write (depth > 8) | `ErrorCode.CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` (422) |
| CM structure write (unresolved nest target) | `ErrorCode.CONTENT_MODULE_NESTING_TARGET_UNRESOLVED` (422) |
| CM structure write (malformed `contentStructureJson`) | `ErrorCode.CONTENT_MODULE_NESTING_STRUCTURE_INVALID` (422) |
| Publish gate (cycle in pinned closure) | `PublishGateCheckCode.CONTENT_MODULE_NESTING_CYCLE` |
| Publish gate (depth > 8 in pinned closure) | `PublishGateCheckCode.CONTENT_MODULE_NESTING_DEPTH_EXCEEDED` |
| Publish gate (missing nested pin) | `PublishGateCheckCode.CONTENT_MODULE_NESTING_UNPINNED` |
| Render expand cycle | `ErrorCode.CONTENT_MODULE_NESTING_CYCLE` (structured failure) |

Normative scenarios: **BDD-IBL-E6-001…018** in [ibl-e6-clause-nesting-governance.md](../../behavior/ibl-e6-clause-nesting-governance.md) (**E6-C1…C21**).

## Alternatives Considered

| Option | Verdict |
| --- | --- |
| **A. Soft-warn only on deep nesting / cycles** | Rejected — bank fail-closed; F28 requires governance. |
| **B. Render-only checks without write-path graph** | Rejected — F28 “not a governed module graph”. |
| **C. Depth limit 5 (ad hoc)** | Rejected — platform already locks DSL nesting at **8** (`ComputeDslLimits`). |
| **D. FE-only nesting visualizer** | Rejected as sole delivery — owners are backend. |
| **E. Full binding JSON scan as where-used authority** | Rejected — graph/edge projection is authoritative (extends G05 stance). |
| **F. Overload `CONTENT_MODULE_REFERENCES` for missing nested pins** | Rejected — dedicated `CONTENT_MODULE_NESTING_UNPINNED` for observability (same pattern as E5 not-started vs expired). |

## Consequences

- CM structure writes gain nesting validation.  
- where-used and lifecycle impact gain nested template hits.  
- Publish-gate gains transitive pin + nesting hard checks.  
- OpenAPI / contract-outline / domain / permission pointers sync with this Accept; backend implementation remains IBL-E6 / #133 delivery work.

## References

- Behavior SoT: [ibl-e6-clause-nesting-governance.md](../../behavior/ibl-e6-clause-nesting-governance.md)  
- Program: [intl-bank-letter-readiness-program.md](../../plan/intl-bank-letter-readiness-program.md) F28 / IBL-E6  
- Prior where-used: [ce-g05-annual-review-fts.md](../../behavior/ce-g05-annual-review-fts.md)  
- Depth isomorphism: [ADR-0056](../rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md) / `ComputeDslLimits`  
- API: [contract-outline.md](../../api/contract-outline.md), [openapi-v1.yaml](../../api/openapi-v1.yaml)
