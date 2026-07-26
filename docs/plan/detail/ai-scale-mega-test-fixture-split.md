# AI-SCALE Leaf 4 — Mega-test fixture / helper split

**Program / slice:** `ai-scale-peel-mega-tests` (ad-hoc **NON-CE** AI-SCALE Leaf 4; **not** a formal P-phase)  
**Program registry:** [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)  
**Formal plan phase:** **None**  
**Task Master:** **#169** (`AI-SCALE-L4` / mega-test fixture split) → **Done**  
**BDD:** [ai-scale-mega-test-fixture-split.md](../../behavior/ai-scale-mega-test-fixture-split.md) — **`ready`/shipped** (2026-07-27; **BDD-AI-SCALE-MTF** MTF-01…10)  
**Placement:** **ISOLATED** (closed) · worktree **REMOVED** · branch `feat/ai-scale-peel-mega-tests` **deleted** · merge `1475caeb` · feat tip `af41410d`  
**Batch recommendation:** **solo** (`member_task_ids: ["169"]`; `proposed_slice_id: ai-scale-peel-mega-tests`;
`delivery_lane: light`;
vetoes_applied: `unrelated-product-peel`, `checklist-#3b/#5a-GO`, `CE-O02`, `mark-#53-CE-Done`) — **closed**

**Prior (Done, do not reopen):** Leaf 1 **#166** → **Done** (`c4f1b0d4`); Leaf 2 **#167** → **Done** (`d02aa414` / `1f55a420`); Leaf 3 **#168** → **Done** (`ce47186a` / `0a5e928e`)

---

## Purpose

Split oversized backend `*Test.java` fixtures/helpers that exceed soft size budgets into focused nested suites, helper classes, and/or shared test support types — **structural extract only**, with **no intentional** product `src/main`, OpenAPI, Flyway, authz, or generation change, and **without** weakening coverage semantics.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **Done** (2026-07-27; sole-active cleared) |
| Formal phase | **None** |
| Host sole-active | **none** (cleared after **#169**) |
| `bdd_readiness` | **`ready`/shipped** |
| `delivery_lane` | **light** (E1–E5; test-infra only) |
| `frontend_ui_in_scope` | **false** |
| `openapi_contract_change` | **false** |
| `runtime_api_semantics_change` | **false** |
| `flyway_change` | **false** |
| `product_main_code_change` | **false** (no product `src/main` changes) |
| Stages 5–7 / 10 | **N/A** (honest light-lane) |
| Required gate | `mvn -B -ntp -f backend/pom.xml verify` → exit **0** |
| Merge | `1475caebf7170b54997a0c7fde6c819cf38f03c9` (feat tip `af41410d`) |
| Queued after this leaf | *(none)* — peel queue **Done**; residual >500 = optional backlog (Not Started) |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119** / CE-O02; invent P24+; invent new AI-SCALE In Progress leaf |

---

## Confirmed facts vs pending questions

### Confirmed (implement against these)

| ID | Fact | Source |
| --- | --- | --- |
| **MTF-C1** | Structural test peel only — no intentional product / OpenAPI / Flyway / authz / generation change | BDD + handoff |
| **MTF-C2** | Soft budgets (review signals): peeled `*Test.java` / fixture helpers prefer ≤ **500** LOC; hard-band > **800** requires further split (or explicit further-split plan) before Done | quality-gate baseline + ADC |
| **MTF-C3** | Coverage semantics preserved — no delete / skip / weaken of assertions | BDD MTF-02 |
| **MTF-C4** | Prefer shared fixtures under `demo/support`, `master/support`, or colocated `…/support` | BDD MTF-03 |
| **MTF-C5** | `delivery_lane: light` while E1–E5 hold; stages **5–7** / **10** **N/A**; `mvn verify` still mandatory | BDD §11 |
| **MTF-C6** | Soft-budget characterization tests (mirror `TemplateImportSoftBudgetTest`) + green scenario suites | BDD MTF-C6 |
| **MTF-C7** | Prioritize LOC >800 then largest >500; prefer ~**25**-file batch; on unclear verify red → one hotspot class per sub-leaf | Batch Recommendation |
| **MTF-C8** | FE / Playwright / Docker out of scope while light eligibility holds | BDD MTF-07 |
| **MTF-C9** | At Done (Stage 12): update AI-SCALE plan + ledger; peel queue may close — still do **not** flip **#3b/#5a** or umbrella **#53** | Program vetoes |

### Pending / non-blocking (closed for this leaf)

| ID | Question | Resolution |
| --- | --- | --- |
| **MTF-P1** | Exact order among the six baseline hotspots after the two >800 files | Delivered all six in one leaf (critical >800 first pattern honored) |
| **MTF-P2** | Whether additional >500 LOC `*Test.java` files enter this leaf | Deferred — residual >500 listed as optional backlog only |
| **MTF-P3** | Nested `@Nested` vs extracted top-level suite vs support-class split per hotspot | Focused top-level suites + colocated fixtures used |

**Open questions blocking implementation:** **none**.

---

## Hotspot baseline (pre-peel) → after peel

Measured pre-peel in worktree @ base `b1bea35d` (PowerShell `Measure-Object -Line` = non-empty lines):

| Priority | Hotspot | Before | After |
| --- | --- | --- | --- |
| 1 | `TemplatePlatformSliceTest` | **981** | Lifecycle **298** · Runtime **297** · DatasetContract **169** · Fixtures **276** |
| 2 | `VariableComputeEngineTest` | **882** | CoreDsl **302** · FormatLocale **381** · SpellAmountSuite **228** |
| 3 | `StructuredContentDocxWriterTest` | **775** | Core **439** · ModuleMedia **268** · Fixtures **98** |
| 4 | `PublishGateServiceTest` | **725** | Core **247** · ContentModule **282** · Fixtures **228** |
| 5 | `LibraryExportServiceTest` | **707** | ZipArtifact **350** · AccessFilter **161** · Fixtures **239** |
| 6 | `SysNormPromotionPackTest` | **605** | Suite **327** · Fixtures **300** |

**Optional residual backlog (>500, Not Started — not In Progress):** `DocxAssemblerTest` ~564; `TemplateBindingConfigurationServiceTest` ~531; `ManagementInvocationQueryServiceTest` ~531; `ContentModuleServiceTest` ~511; `InvocationRegenerationServiceTest` ~510.

---

## Peel approach (executed)

1. SoftBudget-style characterization + peel of six baseline hotspots.
2. Extract fixtures / focused suites; preserve assertion semantics.
3. Drive peeled surfaces under soft budgets (no unexplained >800).
4. Full `mvn verify` green; Arch/CQ **APPROVE_WITH_NOTES**.
5. Stages **5–7** / **10** recorded **N/A**.

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| AI-SCALE-L4-T01 | Sole-active activation + TM/plan/ledger wiring for #169 | **Done** (2026-07-27 Stage 2) |
| AI-SCALE-L4-T02 | Doc-keeper: detail plan + program/index cross-links (this document) | **Done** (2026-07-27 Stage 3) |
| AI-SCALE-L4-T03 | TDD Red: SoftBudget-style characterization for in-scope hotspot `*Test.java` | **Done** (2026-07-27; `MegaTestFixtureSoftBudgetTest`) |
| AI-SCALE-L4-T04 | Peel baseline hotspots (and optional safe >500 others) under soft budgets; preserve coverage semantics | **Done** (2026-07-27; six baseline hotspots peeled; residual >500 deferred) |
| AI-SCALE-L4-T05 | `mvn verify` + arch/CQ as scheduled + light-lane N/A evidence + merge + MAIN doc-sync | **Done** (2026-07-27; merge `1475caeb`; Stage 12 this sync) |

---

## Scope (IN / OUT)

| IN | OUT |
| --- | --- |
| Structural peel of oversized backend `*Test.java` fixtures/helpers | Intentional product `src/main` behavior change |
| Shared / colocated test support extract | OpenAPI / Flyway / Kafka-Redis runtime / generation pipeline changes |
| Soft-budget characterization tests | Weakening / deleting / `@Disabled` of scenarios to shrink LOC |
| Surefire discovery + assertion equivalence | Management UI / Playwright / i18n locale edits |
| `mvn verify` green | Docker acceptance stack / E2E (honest **N/A**) |
| AI-SCALE plan/ledger update at Done (Stage 12) | Flip **#3b/#5a**; mark **#53** / **#106** Done; activate **#119** / CE-O02 |

---

## Exit criteria (before Done)

1. Prioritized in-scope hotspot `*Test.java` files meet MTF-C2 (≤500 prefer; no unexplained >800). ✅
2. Pre-peel scenarios still execute with equivalent or stronger assertions (MTF-02 / MTF-04). ✅
3. Shared fixtures prefer existing or colocated test support packages (MTF-03). ✅
4. `git diff` on `backend/src/main` empty (preferred) or any unavoidable visibility tweak explicitly documented (MTF-05). ✅ (no product main changes)
5. `mvn -B -ntp -f backend/pom.xml verify` exit **0** (MTF-06). ✅
6. Stages **5–7** / **10** recorded **N/A** under `delivery_lane: light` (MTF-07). ✅
7. Program / ledger synced at Stage 12; vetoes honored (MTF-10). ✅

---

## Gate evidence (recorded)

| Gate | Result |
| --- | --- |
| SoftBudget / LOC measurement on peeled hotspots | Six baseline hotspots peeled; none left as mega >800; see after-peel table |
| Scenario / discovery equivalence | Preserved (focused suites + fixtures) |
| `mvn -B -ntp -f backend/pom.xml verify` | exit **0** (feature worktree after rebase; LINE/BRANCH above floors) |
| architecture-reviewer | **APPROVE_WITH_NOTES** |
| code-quality-reviewer | **APPROVE_WITH_NOTES** |
| Stages 5–7 / 10 | **N/A** (`delivery_lane: light`; E1–E5) |
| FE lint / Playwright / Docker | **N/A** |
| Merge SHA | `1475caebf7170b54997a0c7fde6c819cf38f03c9` (feat tip `af41410d`) |

---

## Traceability

- Behavior SoT: [ai-scale-mega-test-fixture-split.md](../../behavior/ai-scale-mega-test-fixture-split.md) (**MTF-01…10**)
- Program: [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)
- Soft budgets: [ai-scale-docs-conventions.md](../../behavior/ai-scale-docs-conventions.md) · [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md)
- Light lane: [lightweight-delivery-lane.md](../../behavior/lightweight-delivery-lane.md)
- SoftBudget pattern (mirror): `backend/src/test/java/com/bank/docgen/template/service/TemplateImportSoftBudgetTest.java`
- SoftBudget leaf test: `backend/src/test/java/com/bank/docgen/sharedkernel/testsupport/MegaTestFixtureSoftBudgetTest.java`
- User direction: 2026-07-26「按你的建议整改吧»
