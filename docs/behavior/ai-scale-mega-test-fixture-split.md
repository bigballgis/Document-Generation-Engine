---
id: BDD-AI-SCALE-MTF
title: AI-SCALE Leaf 4 — Mega-test fixture / helper split under soft size budgets
status: shipped
date: 2026-07-27
bdd_readiness: ready
task_ids: [169]
placement: ISOLATED
worktree_path: REMOVED
branch: feat/ai-scale-peel-mega-tests (deleted)
slice: ai-scale-peel-mega-tests
integration_base: origin/main@1475caebf7170b54997a0c7fde6c819cf38f03c9
merge_sha: 1475caebf7170b54997a0c7fde6c819cf38f03c9
feature_tip: af41410d
user_confirmation: 2026-07-26 「按你的建议整改吧»; stage-1 handoff confirms test-infra peel only; no product behavior change; prefer light + mvn verify; E2E/Docker N/A
delivery_lane: light
frontend_ui_in_scope: false
openapi_contract_change: false
runtime_api_semantics_change: false
flyway_change: false
product_main_code_change: false
stages_5_7_10: N/A
kind: test-infra-structural-peel  # preserve coverage semantics; soft-budget acceptance
---

# AI-SCALE Leaf 4 — Mega-Test Fixture / Helper Split

| Field | Value |
| --- | --- |
| **Slice** | `ai-scale-peel-mega-tests` (program alias `ai-scale-mega-test-fixture-split`) |
| **bdd_readiness** | **`ready`/shipped** |
| **Recorded** | 2026-07-27 |
| **Task Master** | **#169** → **Done** (AI-SCALE Leaf 4 / `AI-SCALE-L4`; merge `1475caeb`) |
| **Program** | [ai-scale-remediation-program-2026-07.md](../plan/ai-scale-remediation-program-2026-07.md) |
| **Formal phase** | **None** (NON-CE AI-SCALE peel; do not invent P24+) |
| **Actor** | Platform engineer / `backend-engineer` / `code-quality-reviewer` (test structure); CI / `mvn verify` as regression surface |
| **delivery_lane** | **`light`** — test-infra structural peel only; E1–E5 hold; `mvn verify` **still required**; stages **5–7** / **10** **N/A** |
| **Frontend UI journey** | **`frontend_ui_in_scope=false`** |
| **OpenAPI / runtime / Flyway / generation** | **`openapi_contract_change=false`**; **`runtime_api_semantics_change=false`**; **`flyway_change=false`**; **`product_main_code_change=false`** (intentional non-goals) |
| **Soft budget SoT** | [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) § Complexity and Size · [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) |
| **SoftBudget pattern** | Extend / mirror [TemplateImportSoftBudgetTest](../../backend/src/test/java/com/bank/docgen/template/service/TemplateImportSoftBudgetTest.java) for peeled test hotspots |

**Completion claim constraints:** This leaf closes soft-budget hotspots on oversized `*Test.java` fixtures/helpers by **structural extract only**. It does **not** weaken assertions, drop scenarios, or change product `src/main` behavior. Do **not** flip checklist **#3b** / **#5a**. Do **not** mark umbrella **#53** / **#106** Done. Do **not** activate **#119** / CE-O02. When this leaf Done closes the AI-SCALE peel queue, plan/doc-sync may mark the program peel queue Done — **not** umbrella **#53**.

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    Last queued AI-SCALE peel #169. Backend/test-only; unrelated to FE i18n
    and product TemplateImport peels (already Done).
  member_task_ids: [169]
  proposed_slice_id: ai-scale-peel-mega-tests
  shared_acceptance_surface: >
    split oversized *Test.java fixtures/helpers without weakening coverage semantics
  vetoes_applied: []
  evidence_amortization: >
    mvn verify; E2E/deploy N/A (test-infra only; light lane)
  on_red_split_hint: >
    peel one hotspot test class per sub-leaf if verify attribution fails
```

| IN (this leaf) | OUT |
| --- | --- |
| Peel oversized backend `*Test.java` hotspots (>500 soft / critical >800) into focused nested suites, helper classes, and/or shared test fixtures | Intentional change to product `src/main` Java, OpenAPI, Flyway, Kafka/Redis runtime, or generation pipelines |
| Prefer shared fixtures under existing test support packages (`…/demo/support`, `…/master/support`, or colocated `…/support` beside the suite) | Weakening / deleting assertions, skipping scenarios, lowering JaCoCo floors, or “simplify” tests to green |
| Keep Surefire discovery + scenario semantics equivalent (same observable asserts for same inputs) | Management UI / Playwright / i18n locale edits |
| Soft-budget characterization tests (LOC) for peeled hotspots | New product features or authz matrix changes |
| `mvn -B -ntp -f backend/pom.xml verify` green | Docker acceptance stack / E2E (honest **N/A**) |
| Update AI-SCALE plan/ledger at Done (Stage 12) | Folding unrelated FE/BE product peels |

---

## 1. Overview

### 1.1 Hotspot baseline (pre-peel evidence)

Measured in worktree `D:/working/DGE-ai-scale-peel-mega-tests` @ base `b1bea35d` (PowerShell `Measure-Object -Line` = non-empty lines; file size KB):

| Priority | Path | LOC | KB | Soft signal |
| --- | --- | --- | --- | --- |
| 1 | `backend/src/test/java/com/bank/docgen/template/web/TemplatePlatformSliceTest.java` | **981** | 61.2 | ≫ **500** soft; ≫ **800** hard-band |
| 2 | `backend/src/test/java/com/bank/docgen/sharedkernel/document/compute/VariableComputeEngineTest.java` | **882** | 37.4 | ≫ **500**; ≫ **800** |
| 3 | `backend/src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterTest.java` | **775** | 38.5 | > **500**; near **800** |
| 4 | `backend/src/test/java/com/bank/docgen/template/service/PublishGateServiceTest.java` | **725** | 41.3 | > **500** |
| 5 | `backend/src/test/java/com/bank/docgen/library/service/LibraryExportServiceTest.java` | **707** | 36.8 | > **500** |
| 6 | `backend/src/test/java/com/bank/docgen/template/service/SysNormPromotionPackTest.java` | **605** | 30.2 | > **500** |

**Batch guidance:** Prefer peeling the critical (>800) and largest >500 hotspots first within a practical ~25-file change budget. Additional >500 LOC `*Test.java` files may be included **only** when peel is safe and attribution stays clear; otherwise leave for a follow-on leaf per `on_red_split_hint`.

Existing test support packages (prefer reuse / extend):

- `backend/src/test/java/com/bank/docgen/demo/support`
- `backend/src/test/java/com/bank/docgen/master/support`
- Colocated `…/<module>/support` beside the suite when domain-local

### 1.2 Operability intent (this leaf)

1. Agents and reviewers can edit focused test modules / fixtures that fit soft size budgets without scanning 700–1000+ line mega-suites.
2. Coverage **semantics** stay equivalent: every pre-peel scenario still runs with the **same** assertions (or strictly stronger), inputs, and fail-closed expectations.
3. Shared builders/fixtures land under existing test support packages where reuse is natural; avoid orphan one-off helpers duplicated across modules.
4. Product `src/main` remains untouched unless an unavoidable compile-only visibility tweak is required for tests — such a tweak is **out of preferred scope** and must be called out; default is **zero** main-code delta.
5. Soft budgets for peeled test files prefer ≤ **500** LOC; no leftover critical > **800** without an explicit further-split plan before Done.

### 1.3 Confirmed decisions

| ID | Decision | Source |
| --- | --- | --- |
| **MTF-C1** | Structural test peel only — **no intentional** product behavior, OpenAPI, Flyway, authz, or generation change. | Handoff + user「按你的建议整改吧」 |
| **MTF-C2** | Soft acceptance targets (review signals; not a new hard CI SoT): peeled `*Test.java` / fixture helpers prefer ≤ **500** LOC; hard-band > **800** requires further split before Done. | quality-gate-threshold-baseline + ADC |
| **MTF-C3** | Coverage semantics preserved: do **not** delete, skip, or weaken assertions; nested `@Nested` suites / extracted helpers OK if discovery + asserts remain equivalent. | Handoff rules |
| **MTF-C4** | Prefer shared fixtures under existing test support packages; colocated support OK when domain-local. | Handoff rules |
| **MTF-C5** | **`delivery_lane: light`** — E1–E5 eligible (test-infra only; no UI/runtime/OpenAPI/Flyway/generation acceptance surface). Stages **5–7** / **10** recorded **N/A**. `mvn verify` still mandatory. Mid-flight product/UI/runtime drift → **upgrade to `full`**. | lightweight-delivery-lane + handoff |
| **MTF-C6** | TDD/refactor green: add/extend failing soft-size characterization tests (mirror `TemplateImportSoftBudgetTest`) and keep scenario suites green via smallest structural move. | tdd-bdd constitution |
| **MTF-C7** | Practical batch: prioritize LOC >800 then largest >500; stay within ~25-file batch when possible; on red attribution failure, peel one hotspot class per sub-leaf. | Batch Recommendation |
| **MTF-C8** | Frontend / Playwright / Docker out of scope while light eligibility holds. | Handoff |
| **MTF-C9** | At Done (Stage 12): update AI-SCALE plan + execution ledger; if Leaf 4 closes the peel queue, mark program peel queue Done — still do **not** flip **#3b/#5a** or umbrella **#53**. | Program exit hygiene |

---

## 2. Actor / role

| Actor | Role | Notes |
| --- | --- | --- |
| **backend-engineer** | Implements fixture/helper peel + soft-budget tests | Works only in feature worktree |
| **code-quality-reviewer** | Soft-budget acceptance on test files | Warn/critical bands; no invented harder CI |
| **CI / mvn verify** | Observable regression surface | Checkstyle/PMD/SpotBugs/JaCoCo still apply to production code; tests must stay green |
| **Parent / delivery-orchestrator** | Lane + stage N/A honesty | `light` only while E1–E5 hold |
| **Management UI / E2E** | **Out of scope** | Stages 5–7 N/A |
| **Product runtime / OpenAPI** | **Out of scope** | No contract or Flyway change |

---

## 3. Goal

1. After peel, prioritized hotspot `*Test.java` files meet MTF-C2 soft budgets (≤500 prefer; no unexplained >800).
2. Pre-peel scenarios remain executable with equivalent (or stronger) assertions — no coverage semantics weakened.
3. Shared fixtures live under existing or colocated test support packages; mega inline setup blocks are extracted rather than duplicated.
4. `mvn -B -ntp -f backend/pom.xml verify` is **GREEN**.
5. No Playwright / Docker evidence required while light eligibility holds.
6. Product `src/main` diff is empty (preferred) or explicitly justified if unavoidable.

---

## 4. Preconditions

- AI-SCALE Leaves **#166–#168** Done; soft budgets + light lane documented.
- Isolated worktree `D:/working/DGE-ai-scale-peel-mega-tests` on `feat/ai-scale-peel-mega-tests` @ base `b1bea35d` (or later feat tip).
- Sole-active serial lane available for this leaf (activation by plan-orchestrator Stage 2).
- Confirmed scope: test-infra structural peel; no intentional product change.

---

## 5. Trigger

1. Deliver activation of TM **#169** / slice `ai-scale-peel-mega-tests`.
2. Implementer starts TDD Red soft-budget characterization for targeted hotspot test files.
3. Structural extract of fixtures/helpers/nested suites until soft budgets hold and verify stays green.

---

## 6. Primary journey

1. Author/confirm this BDD (`ready`).
2. Plan-orchestrator activates sole-active leaf + detail tasks.
3. Backend-engineer captures baseline hotspot LOC; writes failing soft-size characterization tests for in-scope hotspots.
4. Extract fixtures/helpers/`@Nested` suites (prefer existing support packages); keep assertion semantics.
5. Re-run targeted suites then full `mvn verify`.
6. Record stages **5–7** / **10** as **N/A** with light-lane rationale (or upgrade to `full` if product/UI surfaces appear).
7. Architecture/CQ review as scheduled; merge; MAIN doc-sync (AI-SCALE plan/ledger; peel queue Done if Leaf 4 completes the program queue).

---

## 7. System responses (success path)

| Step | System response |
| --- | --- |
| Scenario after peel | Same Given inputs → same Then asserts (pass/fail unchanged) |
| Soft budgets | Peeled hotspot files ≤500 soft; none left >800 without further-split plan |
| Fixtures | Shared builders under test support packages / colocated support |
| Product main | No behavioral `src/main` change |
| Gates | `mvn verify` **GREEN** |
| Stages 5–7 / 10 | Honest **N/A** under `delivery_lane: light` |

---

## 8. Boundary / exception behavior

| Case | Expected |
| --- | --- |
| Soft-budget Red test fails after peel | Continue extract / further split until budgets hold |
| Scenario fails after move (flaky discovery / missing helper) | **Stop Done claim**; restore semantics before Done |
| Desire to delete “redundant” asserts to shrink LOC | **Forbidden** — extract instead |
| Accidental `src/main` product edit | **Revert** or reclassify leaf to `delivery_lane: full` and run previously N/A stages as required by new surface |
| Intentional OpenAPI / Flyway / UI change sneaks in | **Out of scope** — revert **or** upgrade lane |
| Verify red with unclear attribution across many hotspots | Apply `on_red_split_hint`: one hotspot class per sub-leaf |
| New helper file itself >500 / >800 | Peel the helper further before Done |
| Skip `mvn verify` because light | **Forbidden** — light skips E2E/Docker only |
| Flip **#3b/#5a** or mark **#53** Done | **Forbidden** |

---

## 9. Observable evidence

| Evidence | Form |
| --- | --- |
| Soft budgets | LOC measurement (non-empty lines) on peeled hotspot `*Test.java` + extracted helpers vs MTF-C2; optional SoftBudget-style unit test |
| Coverage semantics | Diff review: no removed/weakened asserts; suite names/`@DisplayName` may move but scenarios remain |
| Discovery | Surefire still executes peeled nested/helper-backed tests (count stable or increased, never silently dropped) |
| Product surface | `git diff` on `backend/src/main` empty (preferred) or explicitly documented exception |
| Gates | `mvn -B -ntp -f backend/pom.xml verify` exit **0** |
| Stages 5–7 / 10 | Honest **N/A** + E1–E5 rationale |
| Plan sync (Done) | AI-SCALE program + ledger + detail updated; peel queue Done if applicable |

---

## 10. Acceptance scenarios (Given / When / Then)

### MTF-01 — Soft budgets on prioritized hotspots

**Given** the pre-peel hotspot inventory in §1.1 (at least the six listed files)  
**And** this leaf peels those in-scope mega-test fixtures/helpers  
**When** agents or SoftBudget-style characterization tests measure non-empty LOC after the peel  
**Then** each peeled hotspot `*Test.java` prefers ≤ **500** LOC  
**And** no peeled hotspot remains > **800** LOC without an explicit further-split plan recorded before Done  
**And** no stricter hard CI gate than the quality-gate baseline is invented.

### MTF-02 — Coverage semantics preserved

**Given** the pre-peel scenario set and assertions in an in-scope hotspot suite  
**When** fixtures/helpers/`@Nested` suites are extracted  
**Then** every pre-peel scenario still executes  
**And** assertions are preserved or strictly strengthened  
**And** no scenario is deleted, `@Disabled` without recorded reason, or weakened to pass.

### MTF-03 — Shared test fixtures preferred

**Given** duplicated setup/builders extracted from mega suites  
**When** the peel lands  
**Then** reusable fixtures prefer existing test support packages (`demo/support`, `master/support`, or colocated module `support`)  
**And** helpers are not copy-pasted across unrelated modules without reuse.

### MTF-04 — Surefire discovery remains complete

**Given** Maven Surefire discovers JUnit tests under `backend/src/test/java`  
**When** nested classes or helper-backed tests replace inline mega-suite methods  
**Then** discovery still runs the equivalent scenario set  
**And** test count for the peeled surface is stable or increased (never silently dropped).

### MTF-05 — No product runtime / API / Flyway surface

**Given** this leaf’s intentional non-goals (`product_main_code_change=false`, no OpenAPI/Flyway/generation)  
**When** the change set is reviewed  
**Then** there is no intentional product behavior change in `backend/src/main`  
**And** no OpenAPI, Flyway, permission-matrix, or management-UI file is required for Done  
**And** any unavoidable main-code visibility tweak is explicitly called out (preferred: none).

### MTF-06 — `mvn verify` green

**Given** the peel is complete in the feature worktree  
**When** `mvn -B -ntp -f backend/pom.xml verify` runs  
**Then** the build exits **0**  
**And** Checkstyle / PMD / SpotBugs / JaCoCo gates remain green under existing floors.

### MTF-07 — Light lane E2E/Docker N/A

**Given** BDD proves `frontend_ui_in_scope=false` and no runtime/OpenAPI/Flyway/generation acceptance surface  
**When** delivery evidence is recorded  
**Then** `delivery_lane` is **`light`** with E1–E5 rationale  
**And** stages **5–7** and **10** are recorded **N/A** (not invented greens)  
**And** mid-flight product/UI/runtime drift upgrades the leaf to `full` before Done.

### MTF-08 — Critical >800 hotspots closed or planned

**Given** `TemplatePlatformSliceTest` (981) and `VariableComputeEngineTest` (882) exceed the hard-band signal  
**When** this leaf claims Done for those files (if included in batch)  
**Then** each is ≤ **500** soft **or** ≤800 with an explicit approved further-split plan  
**And** leaving either file >800 without a plan blocks Done for that hotspot.

### MTF-09 — Batch / red-split hygiene

**Given** multiple hotspots may be peeled in one leaf  
**When** `mvn verify` fails with unclear attribution  
**Then** implementers apply `on_red_split_hint`: peel one hotspot test class per sub-leaf  
**And** they do not weaken tests to force green across an oversized batch.

### MTF-10 — Program / governance vetoes honored

**Given** AI-SCALE completion hygiene and host vetoes  
**When** this leaf reaches Done  
**Then** AI-SCALE plan + execution ledger are updated (Stage 12)  
**And** if Leaf 4 completes the peel queue, the program peel queue may be marked Done  
**And** checklist **#3b** / **#5a**, umbrella **#53** / **#106**, and **#119** / CE-O02 remain untouched by this leaf.

---

## 11. Delivery lane classification (E1–E5)

| # | Criterion | Result |
| --- | --- | --- |
| **E1** | No management-UI journey / visual acceptance change | **PASS** — `frontend_ui_in_scope=false`; test Java only |
| **E2** | No runtime / OpenAPI / Flyway / generation acceptance change (or pure docs) | **PASS** — test-infra only; `product_main_code_change=false` |
| **E3** | Stage 1 readiness `ready` or `not-applicable` | **PASS** — `bdd_readiness: ready` |
| **E4** | Handoff records `delivery_lane: light` + E1–E3 rationale | **PASS** — this document + orchestrator handoff |
| **E5** | Worktree still obeyed (light ≠ main-only) | **PASS** — ISOLATED `D:/working/DGE-ai-scale-peel-mega-tests` |

**Lane decision:** **`light`**. Required evidence: `mvn verify`. Stages **5–7** / **10**: **N/A**.

---

## 12. Traceability

| Artifact | Role |
| --- | --- |
| TM **#169** | Delivery task / AI-SCALE-L4 |
| [ai-scale-remediation-program-2026-07.md](../plan/ai-scale-remediation-program-2026-07.md) | Program SoT (Leaf 4) |
| [ai-scale-mega-test-fixture-split.md](../plan/detail/ai-scale-mega-test-fixture-split.md) | Detail plan (hotspots, peel approach, exit criteria) |
| [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) | Soft budgets + peel preference |
| [lightweight-delivery-lane.md](./lightweight-delivery-lane.md) | Lane eligibility |
| [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) | Soft/hard size bands |
| User 2026-07-26「按你的建议整改吧」 | Direction confirmation |

---

## 13. Open questions

None blocking. Hotspot selection within the six listed files (and optional additional >500 suites) is an implementer sequencing choice under MTF-C7 / MTF-09; not a behavior ambiguity.

---

## 14. Handoff fields (Stage 1 → plan-orchestrator)

```text
bdd_readiness: ready
delivery_lane: light
delivery_lane_rationale: >
  E1 frontend_ui_in_scope=false; E2 test-infra only (no OpenAPI/Flyway/runtime/
  generation/product main acceptance); E3 ready; E4 recorded here; E5 ISOLATED
  worktree D:/working/DGE-ai-scale-peel-mega-tests.
behavior_summary: >
  Split oversized backend *Test.java fixtures/helpers into focused modules and
  shared test support types without weakening coverage semantics; no product
  behavior change; soft budgets ≤500 prefer / avoid >800 critical.
acceptance_scenarios: MTF-01…MTF-10
owning_doc: docs/behavior/ai-scale-mega-test-fixture-split.md
task_ids: [169]
evidence_amortization: mvn verify; stages 5–7/10 N/A
open_questions: []
```
