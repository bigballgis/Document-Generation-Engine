# prod-audit-p2-hygiene — dead-code / folder / Knip hygiene (BDD readiness)

| Field | Value |
| --- | --- |
| **Slice** | `prod-audit-p2-hygiene` |
| **bdd_readiness** | **`not-applicable`** |
| **Recorded** | 2026-07-18 |
| **Formal phase** | **None** (do **not** activate IBL) |
| **Task Master** | **#137** **Done** (MAIN merge `baaf16cc` / feature tip `09cf85ce`; stage 12 doc-sync 2026-07-18) |
| **Placement** | Was ISOLATED `D:/working/DGE-prod-audit-p2-hygiene` · `feat/prod-audit-p2-hygiene` (**removed** after merge) |
| **Analogous** | [slim-knip-scan](./slim-knip-scan.md) · [cursor-scaffold-hygiene](./cursor-scaffold-hygiene.md) |

---

## Why BDD is not-applicable

This slice is **engineering hygiene only** — dead-code removal, import-path consolidation, and high-confidence Knip unused-export cleanup. It does **not** introduce or change product behavior:

- No actor / role journey, management UI contract, API envelope, permission rule, or audit semantics.
- No change to template catalog pagination (`list` / page APIs), authoring, publish, generation, or dashboard summary paths.
- Removing uncalled `TemplateService.listAll` / `TemplateCatalogSupport.listAll` deletes unreachable service methods; controllers already use paginated `list` (and related APIs) — no user-visible API surface is removed or altered.
- Merging `frontend/src/components/template` → `components/templates` is a **path/import rewrite** of existing preview/batch-test components; runtime UI behavior is unchanged when gates stay green.
- Knip unused-export cleanup is limited to **reasonable high-confidence** removals (zero importers / clearly unused symbols) — not a product acceptance contract.

**Do not invent product Given/When/Then journeys** for this leaf. Regression gates (`mvn verify` / frontend lint·type-check·test·build) are the acceptance surface.

---

## Confirmed scope (hygiene deliverables)

| Item | Intent | Product behavior? |
| --- | --- | --- |
| Remove `TemplateService.listAll` + `TemplateCatalogSupport.listAll` | Dead code: no controller / service callers (only self-delegate `TemplateService` → `catalogSupport`) | No — unreachable API |
| Merge `components/template` → `components/templates` | Single folder; rewrite `@/components/template/...` imports | No — path hygiene |
| Knip unused exports | High-confidence cleanup only (no mass-delete of ambiguous exports/types) | No — tooling hygiene |

### Delivered facts (MAIN, 2026-07-18)

- Backend dead `TemplateService.listAll` / `TemplateCatalogSupport.listAll` **removed** (`49b4d9e1`).
- Frontend singular `components/template/` **merged** into `components/templates/` with import rewrites (`09cf85ce`).
- Knip unused **exports** **31→0** (exported types deferred; see [evidence](../evidence/prod-audit-p2-hygiene/README.md)).

---

## Explicit non-goals

- No product UI/API/permission/audit behavior change; no OpenAPI contract edits for catalog list.
- No inventing dashboard / template-catalog user journeys (already covered elsewhere, e.g. [prod-dashboard-summary-api](./prod-dashboard-summary-api.md)).
- Do **not** activate **IBL** / Task Master IBL queue (#106 / #107).
- Do **not** flip launch checklist **#3b GO** / **#5a GO**.
- Do **not** mass-delete Knip unused exports/types without high-confidence review.
- Do **not** change paginated `TemplateService.list` or frontend `listAllTemplates` / store fetch-all semantics in this leaf (out of scope unless a high-confidence dead symbol is proven unused and unrelated to product paths).
- Formal phase remains **None**.

---

## Delivery acceptance (engineering — not product G/W/T)

1. **Given** dead `listAll` methods with zero external callers  
   **When** they are removed  
   **Then** backend compiles; `mvn -B -ntp -f backend/pom.xml verify` green; no controller/OpenAPI regression for paginated catalog list.

2. **Given** files under `frontend/src/components/template/`  
   **When** moved into `components/templates/` with import rewrites  
   **Then** no broken imports; `pnpm -C frontend lint` · `type-check` · `test` · `build` green; preview/batch-test UI still resolves the same components.

3. **Given** Knip residual unused exports  
   **When** only high-confidence unused symbols are removed  
   **Then** frontend gates stay green; ambiguous exports are left for a later triage (no CI-blocking Knip gate required in this leaf).

---

## Traceability

| Artifact | Role |
| --- | --- |
| This doc | Owning BDD readiness record (`not-applicable`) |
| [slim-knip-scan.md](./slim-knip-scan.md) | Prior Knip tooling / Wave-1 orphan delete pattern |
| Task Master **#137** | **Done** (merge `baaf16cc` / tip `09cf85ce`; sole-active cleared) |
| Launch checklist | Unchanged — this slice is not a go-live closer |

```
bdd_readiness: not-applicable
task_ids: [137]  # prod-audit-p2-hygiene Done; sole-active cleared; next IBL #107 not activated
owning_doc: docs/behavior/prod-audit-p2-hygiene.md
```
