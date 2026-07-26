---
id: BDD-AI-SCALE-TIP
title: AI-SCALE Leaf 2 — TemplateImport* structural peel under soft size budgets
status: ready
date: 2026-07-27
bdd_readiness: ready
task_ids: [167]
placement: ISOLATED
worktree_path: D:/working/DGE-ai-scale-peel-template-import
branch: feat/ai-scale-peel-template-import
slice: ai-scale-peel-template-import
integration_base: main@f29211c5
user_confirmation: 2026-07-26 「按你的建议整改吧」; continue autonomously after #166 Done
delivery_lane: full
frontend_ui_in_scope: false
openapi_contract_change: false
runtime_api_semantics_change: false
stages_5_7_10: N/A-eligible
kind: structural-peel  # preserve product import semantics; soft-budget acceptance
---

# AI-SCALE Leaf 2 — TemplateImport* Structural Peel

| Field | Value |
| --- | --- |
| **Slice** | `ai-scale-peel-template-import` |
| **bdd_readiness** | **`ready`** |
| **Recorded** | 2026-07-27 |
| **Task Master** | **#167** (AI-SCALE Leaf 2 / `AI-SCALE-L2`) |
| **Program** | [ai-scale-remediation-program-2026-07.md](../plan/ai-scale-remediation-program-2026-07.md) |
| **Detail plan** | [detail/ai-scale-template-import-peel.md](../plan/detail/ai-scale-template-import-peel.md) |
| **Formal phase** | **None** (NON-CE AI-SCALE peel; do not invent P24+) |
| **Actor** | Platform engineer / `backend-engineer` / `code-quality-reviewer` (structure); authorized template import callers (regression surface) |
| **delivery_lane** | **`full`** — backend structural peel with `mvn verify` required; **not** `light` |
| **Frontend UI** | **`frontend_ui_in_scope=false`** — no management-UI journey change |
| **OpenAPI / runtime contract** | **`openapi_contract_change=false`**; **`runtime_api_semantics_change=false`** — intentional non-goals |
| **Stages 5–7 / 10** | **N/A-eligible** when implementer evidence confirms zero FE + zero OpenAPI/runtime contract delta (honest N/A, not silent skip). Lane remains **full** (BE verify + arch review still required). |
| **Soft budget SoT** | [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) § Complexity and Size · [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) |
| **Product import semantics SoT (unchanged)** | [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) · [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) · P14-T03 / [contract-outline.md](../api/contract-outline.md) |

**Completion claim constraints:** This leaf closes soft-budget hotspots on `TemplateImport*` by **structural extract only**. It does **not** amend CE-E01 / Wave 7 / P14 import product decisions. Do **not** flip checklist **#3b** / **#5a**. Do **not** mark umbrella **#53** / **#106** Done. Do **not** activate **#119** / CE-O02. Do **not** fold **#168** / **#169** into this leaf.

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    Queue head #167 backend TemplateImport peel; #168 i18n and #169 mega-test
    veto merge (unrelated domains).
  member_task_ids: [167]
  proposed_slice_id: ai-scale-peel-template-import
  shared_acceptance_surface: >
    backend template import collaborators under soft budgets; no FE UI change
  vetoes_applied: [unrelated-frontend-i18n, mega-test-split]
  evidence_amortization: >
    mvn verify; E2E/deploy N/A if zero UI/runtime contract change confirmed
  on_red_split_hint: >
    split by individual Import* class if verify red attribution unclear
```

| IN (this leaf) | OUT |
| --- | --- |
| Peel oversized `TemplateImportService` / `TemplateImportDependencyPrecheck` (and any related oversized Support created by the peel) toward soft budgets | Intentional change to dry-run / apply / conflict / severity semantics |
| Keep `TemplateImportController` HTTP surface and OpenAPI shapes unchanged | Management UI / Playwright / i18n locale split (**#168**) |
| Update [module-map.md](../architecture/module-map.md) **if** public entry points move/rename | Mega-test fixture split (**#169**) |
| Preserve rendering / lifecycle / authz boundaries | New import features, new dependency types, new messageKeys |
| TDD: characterization / regression Red tests before extract | Claiming FE or OpenAPI “unchanged” without evidence |

---

## 1. Overview

### 1.1 Hotspot baseline (pre-peel evidence)

Measured in worktree `D:/working/DGE-ai-scale-peel-template-import` (LOC = non-empty lines via PowerShell `Measure-Object -Line`):

| Type | Path | LOC | Soft signal |
| --- | --- | --- | --- |
| `@Service` orchestrator | `…/template/service/TemplateImportService.java` | **553** | > **400** Service warn; > **500** file soft target |
| Collaborator | `…/template/service/TemplateImportDependencyPrecheck.java` | **508** | > **500** file soft target |
| Support | `TemplateImportApplySupport.java` | **152** | ≤ **200** Support — already OK |
| Support | `TemplateImportTargetResolutionSupport.java` | **117** | ≤ **200** — already OK |
| Support | `TemplateImportBundleValidator.java` | **71** | ≤ **200** — already OK |

Public HTTP entry remains `TemplateImportController` → `TemplateImportService` under `com.bank.docgen.template`.

### 1.2 Product intent (this leaf)

1. Agents and reviewers can work inside TemplateImport collaborators that fit soft size budgets without changing import outcomes.
2. Dry-run and apply paths keep the same success/failure, report severity aggregates, DRAFT landing, fail-closed authz, and audit event family already confirmed in CE-E01 / Wave 7 / P14.
3. Extracted collaborators stay inside `template` (or documented template-local support packages) — **not** in `rendering`, and **not** owning lifecycle/authz SoT.
4. If the public orchestrator type or package entry point moves, [module-map.md](../architecture/module-map.md) is updated in the **same change set**.

### 1.3 Confirmed decisions

| ID | Decision | Source |
| --- | --- | --- |
| **TIP-C1** | Structural peel only — **no intentional** change to import dry-run/apply semantics, conflict policy, dependency severity/types, HTTP status codes, or OpenAPI schemas. | Handoff + user「按你的建议整改吧」 |
| **TIP-C2** | Soft acceptance targets (review signals; not a new hard CI SoT): `@Service` / orchestrator ≤ **400** LOC warn band; `*Support` helpers prefer ≤ **200** LOC; manually maintained file soft target ≤ **500** LOC. Hard baseline (>800 file / >120 function) still authoritative. | quality-gate-threshold-baseline + ADC |
| **TIP-C3** | Pre-peel Support files already under Support budget may remain; peel focuses on Service + DependencyPrecheck (and any new extract that would otherwise exceed budgets). | Hotspot measurement |
| **TIP-C4** | Existing product BDDs remain SoT for import semantics; this file asserts **preservation**, not new product rules. | CE-E01 / SYS-NORM-PP / P14 |
| **TIP-C5** | `delivery_lane: full` (BE `mvn verify` + architecture review). Stages **5–7** and **10** are **N/A-eligible** only with evidence of zero FE + zero OpenAPI/runtime contract change. Do **not** reclassify as `light`. | Orchestrator handoff |
| **TIP-C6** | TDD: add/extend failing characterization or size/regression tests **before** extract; green via smallest structural move. | tdd-bdd constitution |
| **TIP-C7** | Rendering isolation + lifecycle ownership unchanged: no new `rendering` → template orchestration edges; import must not move lifecycle SoT into rendering. | module-map + tech-stack |
| **TIP-C8** | Module-map update required **iff** public entry points (package / primary orchestrator / controller ownership path agents retrieve) move or rename; pure private-method extract with same public types → map update optional but preferred if specialist routing hints need a TemplateImport row. | module-map-agent-retrieval |
| **TIP-C9** | Veto merge with **#168** i18n and **#169** mega-test. | Batch Recommendation |

---

## 2. Actor / role

| Actor | Role | Notes |
| --- | --- | --- |
| **backend-engineer** | Implements peel + TDD regression | Works only in feature worktree |
| **code-quality-reviewer** | Soft-budget acceptance | Warn/critical bands; no invented harder CI |
| **Authorized import caller** | GLOBAL_ADMIN / GROUP_ADMIN / TEMPLATE_AUTHOR per matrix §5 | Observable regression actor for dry-run/apply |
| **Parent / delivery-orchestrator** | Lane + stage N/A honesty | Must not claim light lane |
| **End-user management UI** | **Out of scope** | `TemplateImportDialog` untouched |

---

## 3. Goal

1. After peel, `POST /api/management/v1/templates/import` with `dryRun=true|false` yields the **same** observable outcomes for equivalent fixtures (status, envelope fields, `dependencyReport` severity aggregates / `readyToCommit`, DRAFT apply summary, fail-closed authz).
2. `TemplateImportDependencyPrecheck` (or its peeled successors) produce the **same** per-item severity / type / blocking semantics for equivalent bundles.
3. Peeled collaborators meet soft budgets in TIP-C2 (Service ≤400 warn; Support ≤200 prefer; file ≤500 soft).
4. Module-map reflects any moved public entry points in the same change set.
5. No FE UI or OpenAPI contract change is claimed without evidence (diff / OpenAPI unchanged / FE untouched).

---

## 4. Preconditions

- AI-SCALE Leaf 1 (**#166**) Done; soft budgets documented.
- Product import semantics already shipped (P14-T03, CE-E01, SYS-NORM Wave 7).
- Isolated worktree `D:/working/DGE-ai-scale-peel-template-import` on `feat/ai-scale-peel-template-import`.
- Sole-active serial lane available for this leaf (activation by plan-orchestrator).

---

## 5. Trigger

1. Deliver activation of TM **#167** / slice `ai-scale-peel-template-import`.
2. Implementer starts TDD Red tests that lock dry-run/apply + precheck report invariants and/or soft-size acceptance.
3. Structural extract of `TemplateImportService` / `TemplateImportDependencyPrecheck` collaborators.

---

## 6. Primary journey

1. Author/confirm this BDD (`ready`).
2. Plan-orchestrator activates sole-active leaf + detail tasks.
3. Backend-engineer writes failing regression/characterization tests against current import/precheck behavior and size acceptance.
4. Extract collaborators (Support / focused components) until soft budgets hold; keep public controller + service facade (or update module-map if facade moves).
5. Re-run targeted TemplateImport tests + full `mvn verify`.
6. Record FE/OpenAPI delta evidence (expected: none) → mark stages 5–7/10 **N/A** honestly.
7. Architecture review for boundaries; merge; MAIN doc-sync.

---

## 7. System responses (success path)

| Step | System response |
| --- | --- |
| Dry-run after peel | Same as before: HTTP **200**, `imported=false`, `dependencyReport` shape/aggregates unchanged for same fixture |
| Apply after peel | Same as before: success → DRAFT landing + summary; blocking deps → **422** `api.error.template.importDependenciesUnsatisfied` + report; no half-import |
| Authz failure | Still **403** fail-closed; no resource existence leak |
| Soft budgets | Service/orchestrator ≤400 LOC; Support ≤200 prefer; files ≤500 soft (or approved split plan if hard >800) |
| Docs | Behavior + module-map (if needed) + plan/ledger updated in same change set at Done |

---

## 8. Boundary / exception behavior

| Case | Expected |
| --- | --- |
| Accidental semantic drift discovered by Red tests | **Stop peel**; restore semantics first; do not “fix product” inside this leaf unless trivial and proven by existing CE-E01/Wave7 scenarios |
| New Support exceeds 200 LOC | Further peel/extract before Done |
| Public entry rename/move | Update module-map same change set; keep rendering isolation |
| FE/OpenAPI file touched unintentionally | Either revert **or** upgrade evidence path (E2E/deploy no longer N/A; re-classify surfaces) |
| Desire to skip `mvn verify` | **Forbidden** — lane is full for BE |
| Fold #168/#169 | **Forbidden** |

---

## 9. Observable evidence

| Evidence | Form |
| --- | --- |
| Semantics preserved | Existing/extended `TemplateImport*Test` / `TemplateImportDependencyPrecheckTest` / controller tests **GREEN** with unchanged assertions for dry-run/apply/report |
| Soft budgets | LOC measurement on peeled types vs TIP-C2 |
| Module-map | Diff shows update **or** explicit “entry points unchanged” note in Done evidence |
| FE / OpenAPI | `git diff` evidence: no intentional `frontend/**` product change; no OpenAPI schema/path change (or documented exception → lose N/A eligibility) |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` **GREEN** |
| Stages 5–7 / 10 | Honest **N/A** with surface flags above, **or** executed if surfaces changed |

---

## 10. Acceptance scenarios (Given / When / Then)

### TIP-01 — Dry-run semantics unchanged after peel

**Given** an authorized actor and a fixture bundle that today yields a known dry-run `dependencyReport` (including `readyToCommit` / counts)  
**And** TemplateImport collaborators have been structurally peeled under this leaf  
**When** `POST /api/management/v1/templates/import` is invoked with `dryRun=true` for that fixture  
**Then** HTTP status, `imported=false`, report item types/severities/codes/aggregates, and zero business persistence match the pre-peel baseline  
**And** no new OpenAPI fields are required for the caller.

### TIP-02 — Apply / commit semantics unchanged after peel

**Given** the same authorized actor and a fixture that is `readyToCommit==true` (or a known blocking fixture)  
**And** the peel is complete  
**When** import is invoked with `dryRun=false` / omitted  
**Then** success path still lands **DRAFT** with the same summary family fields, or blocking path still returns **422** `api.error.template.importDependenciesUnsatisfied` with full report and **no** half-import  
**And** conflict policy behavior remains as in P14 / CE-E01 (unchanged by this leaf).

### TIP-03 — Dependency precheck severity / results unchanged

**Given** characterization fixtures covering MASTER_PIN / CLAUSE / ASSET_KEY / RENDER_PROFILE (and Wave 7 closure cases already covered by existing tests)  
**When** `TemplateImportDependencyPrecheck` (or peeled successors composing the same report) runs after the peel  
**Then** per-item `dependencyType`, `severity`, stable `code`s, and blocking vs non-blocking classification match the pre-peel baseline for those fixtures  
**And** no new dependency type enum values are introduced by this leaf.

### TIP-04 — Peeled collaborators under soft size budgets

**Given** the post-peel set of TemplateImport orchestrator / Support / precheck types touched by this leaf  
**When** agents or `code-quality-reviewer` measure LOC  
**Then** `@Service` / primary orchestrator is ≤ **400** LOC (soft warn band cleared)  
**And** each `*Support` helper prefers ≤ **200** LOC  
**And** manually maintained files are ≤ **500** LOC soft target (or have an explicit approved split plan if approaching hard >800)  
**And** no stricter hard CI gate than the quality-gate baseline is invented.

### TIP-05 — Module-map updated if entry points move

**Given** the peel renames or relocates a public TemplateImport entry point that agents retrieve (package, primary `@Service` facade, or controller ownership path)  
**When** the change set is prepared for Done  
**Then** [module-map.md](../architecture/module-map.md) is updated in the **same change set** so retrieval stays accurate  
**And** if entry points are unchanged, Done evidence states that explicitly (map row optional).

### TIP-06 — No FE / OpenAPI contract change without evidence

**Given** this leaf claims `frontend_ui_in_scope=false` and `openapi_contract_change=false`  
**When** implementers finish the peel  
**Then** evidence shows no intentional management-UI change and no OpenAPI path/schema/enum change  
**And** stages **5–7** and **10** are recorded **N/A** only with that evidence  
**And** if FE or OpenAPI/runtime contract did change, N/A is revoked and the full acceptance path for those surfaces is executed  
**And** the leaf is **not** re-labeled `delivery_lane: light`.

### TIP-07 — Rendering / lifecycle boundaries preserved

**Given** TemplateImport code is reorganized  
**When** architecture review inspects package edges  
**Then** import orchestration remains outside `rendering` ownership  
**And** no new forbidden cross-deps from `rendering` into template import orchestration are introduced  
**And** lifecycle / authz SoT are not moved into Support extracts.

### TIP-08 — Fail-closed authorization unchanged

**Given** an unauthorized caller attempts import dry-run or apply  
**When** the request is processed after the peel  
**Then** the response remains fail-closed **403** without leaking whether the target resource exists  
**And** no new permission codes are introduced.

---

## 11. Traceability

| Artifact | Role |
| --- | --- |
| This file | Leaf behavior SoT (peel + preservation) |
| [ce-e01-export-bundle-v2.md](./ce-e01-export-bundle-v2.md) | Product dry-run / apply / report semantics |
| [sys-norm-promotion-pack.md](./sys-norm-promotion-pack.md) | Wave 7 promotion-closure import extensions |
| [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) | Soft budgets / peel-queue conventions |
| [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) | Soft/hard size SoT |
| [module-map.md](../architecture/module-map.md) / [module-map-agent-retrieval.md](./module-map-agent-retrieval.md) | Entry-point retrieval |
| [ai-scale-remediation-program-2026-07.md](../plan/ai-scale-remediation-program-2026-07.md) | Program registry Leaf 2 |
| `.taskmaster/tasks/tasks.json` **#167** | Task Master queue head |
| Permission matrix §5 | Import/export authz (unchanged) |

---

## 12. Open questions

**None.** Behavior is confirmed from orchestrator handoff + prior user direction; product semantics are already locked in CE-E01 / Wave 7 / P14.

---

## 13. Out of scope (explicit)

- Frontend `TemplateImportDialog` / Playwright E2E / UIUX (unless FE is touched — then N/A revoked)
- i18n locale mega-file split (**#168**)
- Mega-test fixture split (**#169**)
- New import features, new dependency types, OpenAPI additive fields
- Light-lane reclassification
- Formal P-phase invention; CE / IBL / go-live Done claims
