---
name: code-quality-review
description: Zero-waste code cleanliness audit — dead code, DRY, naming, minimalism, file size, test smells, backend/frontend style consistency. Use when the user asks for code quality review, cleanliness audit, refactor hygiene, structural consistency, or invokes code-quality-reviewer.
---

# Code Quality Review

## Quick invoke

Launch one readonly `code-quality-reviewer` subagent:

```text
Full Repository Path: d:\working\Document Generation Engine
Scope: full repo | branch changes | uncommitted changes | module:rendering | files:path1,path2
Custom Instructions: <optional>
```

Parent summarizes findings in a severity table; does **not** fix unless user asks.

## Review order

1. **Mechanical scan** — large files, `target/` tracked, duplicate `*Support` / `*Utils`, generated artifacts in git
2. **Module coupling** — cross-package imports vs `docs/architecture/module-boundaries.md`; flag
   `rendering`↔`template`↔`authoring` cycles, rendering throwing `template.service.*Exception`,
   shared enums in wrong module; recommend ArchUnit if no automated boundary test exists
3. **Dead code** — unreferenced private methods, unused imports, orphaned tests, commented-out blocks
4. **DRY** — copy-pasted demo tests, parallel `*AccessSupport` patterns, PDF conversion twins,
   E2E `test.skip` + stack-readiness copy-paste, repeated `mountWithApp` i18n setup
5. **Structure** — package placement, single responsibility, extract vs inline threshold
6. **Naming** — module vocabulary (`Service` / `Support` / `Mapper` / `Entity`), frontend composable `use*` convention;
   `components/template` vs `components/templates` drift
7. **Minimalism** — one-line wrappers, unnecessary interfaces, redundant null checks, defensive layers without callers
8. **Comments** — explain *why* only; delete restated code and stale TODOs
9. **Tests** — `TestSupport` justified, assertion helpers not duplicated, test class name matches SUT

## Size budgets (soft limits)

**SoT alignment:** hard file/function thresholds and default targets come from
[quality-gate-threshold-baseline.md](../../../docs/architecture/quality-gate-threshold-baseline.md)
§ Complexity and Size (function ≤80 soft / >120 hard; file ≤500 soft / >800 hard).
Agent conventions: [ai-scale-docs-conventions.md](../../../docs/behavior/ai-scale-docs-conventions.md).
Rule cite: `.cursor/rules/soft-size-budgets.mdc`.

| Artifact | Warning | Critical |
| --- | --- | --- |
| Java `@Service` / orchestrator | >400 LOC | >600 LOC |
| Java controller | >300 LOC | >450 LOC |
| Vue SFC | >400 LOC | >550 LOC |
| Composable `.ts` | >300 LOC | >450 LOC |
| Any source file (baseline) | >500 LOC | >800 LOC (baseline hard / split plan) |
| Generated `openapi-v1.ts` | N/A (regen only) | manual edits |

These warn/critical bands are **review signals**. On conflict with baseline hard thresholds,
**baseline wins**. When soft targets are exceeded, prefer queuing a **separate peel leaf**
(do not silently grow mega-files).

Hotspots to always inspect: `useTemplateDetailController.ts`, `TemplateLifecycleService.java`, `StructuredContentDocxWriter.java`, `ManagementAuditRecorder.java`, `DashboardView.vue`.

Also prefer [module-map.md](../../../docs/architecture/module-map.md) before unscoped
repo-wide coupling greps when asking which package owns a smell.

## Backend signals (Java)

- `*Support` classes: must be stateless helpers; if >200 LOC or injected with repos → candidate for `Service` or inline private methods
- MapStruct mappers vs hand mapping — one pattern per module
- Exception types: prefer domain exceptions in module `service` package; no stringly-typed errors
- Lombok: consistent with module peers; no `@Data` on entities if others use explicit accessors
- Test naming: `methodUnderTest_condition_expected` or BDD-style already in module

## Frontend signals (Vue 3 + TS)

- Composables own logic; views own layout — no 500-line views with business rules
- Pinia stores: no duplicate fetch/error parsing (use `http.ts` + `errorEnvelope.ts`)
- i18n: keys only in templates/script; `en.ts` is source — no duplicate English in `zh-CN` that diverges
- CSS: tokens from `theme/tokens.ts`; no magic numbers duplicated across SFCs
- `openapi-v1.ts` / `en.ts` / `zh-CN.ts` size — flag only if **manual** edits detected in diff

## Dead code verification

Before marking 🔴 dead:

```bash
# Java symbol (adjust pattern)
rg "ClassName" backend/src --glob "!**/target/**"

# TS export
rg "exportName" frontend/src
```

No references in `src/` + `test/` → candidate removal. References only in `target/` → ignore.

## DRY patterns to hunt

| Pattern | Where seen | Fix |
| --- | --- | --- |
| Demo master generator tests | `backend/.../demo/*MasterDocxAssetGeneratorTest.java` | Shared fixture builder |
| `*AccessSupport` triplet | template, contentmodule, collaboration | Base access helper or documented intentional split |
| API error parsing | frontend views | Centralize in composable |
| DOCX assertion helpers | `demo/support/*` | Single `DemoDocxAssertions` |

## Output contract

Same as agent: score / 🔴🟡🟢 / Top 5 / module table.

## After review

| User intent | Delegate |
| --- | --- |
| Fix 🔴 items | `backend-engineer` or `frontend-engineer` (TDD; behavior unchanged) |
| Governance overlap | `architecture-reviewer` |
| Defect suspicion | `bugbot` or `explore` |
| Done after fixes | gates → `post-task-doc-sync` → `post-task-commit-review` |

## Reference

Stack-specific conventions: [STANDARDS.md](STANDARDS.md)
