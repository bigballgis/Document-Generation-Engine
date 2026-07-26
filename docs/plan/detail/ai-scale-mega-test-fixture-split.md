# AI-SCALE Leaf 4 — Mega-test fixture / helper split

**Program / slice:** `ai-scale-peel-mega-tests` (ad-hoc **NON-CE** AI-SCALE Leaf 4; **not** a formal P-phase)  
**Program registry:** [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)  
**Formal plan phase:** **None**  
**Task Master:** **#169** (`AI-SCALE-L4` / mega-test fixture split) → **In Progress** (sole-active)  
**BDD:** [ai-scale-mega-test-fixture-split.md](../../behavior/ai-scale-mega-test-fixture-split.md) — **`ready`** (2026-07-27; **BDD-AI-SCALE-MTF** MTF-01…10)  
**Placement:** **ISOLATED** · worktree `D:/working/DGE-ai-scale-peel-mega-tests` · branch `feat/ai-scale-peel-mega-tests` · base `b1bea35d`  
**Batch recommendation:** **solo** (`member_task_ids: ["169"]`; `proposed_slice_id: ai-scale-peel-mega-tests`;
`delivery_lane: light`;
vetoes_applied: `unrelated-product-peel`, `checklist-#3b/#5a-GO`, `CE-O02`, `mark-#53-CE-Done`) — **open**

**Prior (Done, do not reopen):** Leaf 1 **#166** → **Done** (`c4f1b0d4`); Leaf 2 **#167** → **Done** (`d02aa414` / `1f55a420`); Leaf 3 **#168** → **Done** (`ce47186a` / `0a5e928e`)

---

## Purpose

Split oversized backend `*Test.java` fixtures/helpers that exceed soft size budgets into focused nested suites, helper classes, and/or shared test support types — **structural extract only**, with **no intentional** product `src/main`, OpenAPI, Flyway, authz, or generation change, and **without** weakening coverage semantics.

---

## Status

| Item | Value |
| --- | --- |
| Leaf status | **In Progress** (sole-active; docs Stage 3 ready for backend-engineer) |
| Formal phase | **None** |
| Host sole-active | TM **#169** / `ai-scale-peel-mega-tests` |
| `bdd_readiness` | **`ready`** |
| `delivery_lane` | **light** (E1–E5; test-infra only) |
| `frontend_ui_in_scope` | **false** |
| `openapi_contract_change` | **false** |
| `runtime_api_semantics_change` | **false** |
| `flyway_change` | **false** |
| `product_main_code_change` | **false** (preferred empty `src/main` diff) |
| Stages 5–7 / 10 | **N/A** (honest light-lane; do not invent greens) |
| Required gate | `mvn -B -ntp -f backend/pom.xml verify` → exit **0** |
| Queued after this leaf | *(none)* — do **not** activate FOS **#177** or fold product peels |
| Do **not** | Flip **#3b/#5a GO**; mark **#53** / **#106** Done; activate **#119** / CE-O02; invent P24+; weaken/delete asserts to shrink LOC |

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

### Pending / non-blocking (implementer sequencing — not behavior blockers)

| ID | Question | Resolution rule |
| --- | --- | --- |
| **MTF-P1** | Exact order among the six baseline hotspots after the two >800 files | Prefer critical (>800) first, then descending LOC; stay within ~25 files |
| **MTF-P2** | Whether additional >500 LOC `*Test.java` files enter this leaf | Only if peel is safe and attribution stays clear; else leave for a follow-on leaf |
| **MTF-P3** | Nested `@Nested` vs extracted top-level suite vs support-class split per hotspot | Choose smallest structural move that preserves discovery + asserts and meets soft budgets |

**Open questions blocking implementation:** **none**.

---

## Hotspot baseline (pre-peel)

Measured in worktree `D:/working/DGE-ai-scale-peel-mega-tests` @ base `b1bea35d` (PowerShell `Measure-Object -Line` = non-empty lines):

| Priority | Path | LOC | Soft signal |
| --- | --- | --- | --- |
| 1 | `backend/src/test/java/com/bank/docgen/template/web/TemplatePlatformSliceTest.java` | **981** | ≫ 500; ≫ 800 |
| 2 | `backend/src/test/java/com/bank/docgen/sharedkernel/document/compute/VariableComputeEngineTest.java` | **882** | ≫ 500; ≫ 800 |
| 3 | `backend/src/test/java/com/bank/docgen/rendering/StructuredContentDocxWriterTest.java` | **775** | > 500; near 800 |
| 4 | `backend/src/test/java/com/bank/docgen/template/service/PublishGateServiceTest.java` | **725** | > 500 |
| 5 | `backend/src/test/java/com/bank/docgen/library/service/LibraryExportServiceTest.java` | **707** | > 500 |
| 6 | `backend/src/test/java/com/bank/docgen/template/service/SysNormPromotionPackTest.java` | **605** | > 500 |

**Batch guidance:** Peel the six listed files first (critical >800, then largest >500). Optionally peel other >500 LOC `*Test.java` files **only** when safe within a practical ~25-file change budget; otherwise defer per `on_red_split_hint`.

Existing test support packages (prefer reuse / extend):

- `backend/src/test/java/com/bank/docgen/demo/support`
- `backend/src/test/java/com/bank/docgen/master/support`
- Colocated `…/<module>/support` beside the suite when domain-local

---

## Peel approach

1. **Baseline** — re-measure LOC on in-scope hotspots; record pre-peel scenario inventory for attribution.
2. **TDD Red** — add/extend SoftBudget-style characterization tests (mirror `TemplateImportSoftBudgetTest`) for in-scope hotspot paths so oversized files fail until peeled.
3. **Extract** — move duplicated setup/builders into support packages; split mega suites via `@Nested` and/or focused top-level test classes; keep assertion bodies intact (move, do not rewrite semantics).
4. **Budgets** — drive each peeled hotspot to ≤500 LOC prefer; leave none >800 without an explicit further-split plan before Done; if a new helper itself exceeds budgets, peel it further.
5. **Verify** — targeted Surefire suites for peeled surfaces, then full `mvn verify`.
6. **Red hygiene** — if verify fails with unclear attribution across many hotspots, split to one hotspot class per sub-leaf (do **not** weaken asserts).
7. **Lane honesty** — keep stages **5–7** / **10** as **N/A** unless mid-flight product/UI/runtime drift forces upgrade to `full`.

---

## Task rows

| ID | Task | Status |
| --- | --- | --- |
| AI-SCALE-L4-T01 | Sole-active activation + TM/plan/ledger wiring for #169 | **Done** (2026-07-27 Stage 2) |
| AI-SCALE-L4-T02 | Doc-keeper: detail plan + program/index cross-links (this document) | **Done** (2026-07-27 Stage 3) |
| AI-SCALE-L4-T03 | TDD Red: SoftBudget-style characterization for in-scope hotspot `*Test.java` | **Not Started** |
| AI-SCALE-L4-T04 | Peel baseline hotspots (and optional safe >500 others) under soft budgets; preserve coverage semantics | **Not Started** |
| AI-SCALE-L4-T05 | `mvn verify` + arch/CQ as scheduled + light-lane N/A evidence + merge + MAIN doc-sync | **Not Started** |

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

1. Prioritized in-scope hotspot `*Test.java` files meet MTF-C2 (≤500 prefer; no unexplained >800).
2. Pre-peel scenarios still execute with equivalent or stronger assertions (MTF-02 / MTF-04).
3. Shared fixtures prefer existing or colocated test support packages (MTF-03).
4. `git diff` on `backend/src/main` empty (preferred) or any unavoidable visibility tweak explicitly documented (MTF-05).
5. `mvn -B -ntp -f backend/pom.xml verify` exit **0** (MTF-06).
6. Stages **5–7** / **10** recorded **N/A** under `delivery_lane: light` (or leaf upgraded to `full` if surfaces appear) (MTF-07).
7. Program / ledger synced at Stage 12; vetoes honored (MTF-10). Leaf status remains **In Progress** until those gates complete — **do not** mark Done from Stage 3 docs alone.

---

## Gate evidence (target)

| Gate | Result (target) |
| --- | --- |
| SoftBudget / LOC measurement on peeled hotspots | ≤500 prefer; no unexplained >800 |
| Scenario / discovery equivalence | Preserved (or stricter) |
| `mvn -B -ntp -f backend/pom.xml verify` | exit **0** |
| architecture-reviewer / code-quality-reviewer | As scheduled (soft-budget focus on test files) |
| Stages 5–7 / 10 | **N/A** (`delivery_lane: light`; E1–E5) |
| FE lint / Playwright / Docker | **N/A** while light eligibility holds |

---

## Traceability

- Behavior SoT: [ai-scale-mega-test-fixture-split.md](../../behavior/ai-scale-mega-test-fixture-split.md) (**MTF-01…10**)
- Program: [ai-scale-remediation-program-2026-07.md](../ai-scale-remediation-program-2026-07.md)
- Soft budgets: [ai-scale-docs-conventions.md](../../behavior/ai-scale-docs-conventions.md) · [quality-gate-threshold-baseline.md](../../architecture/quality-gate-threshold-baseline.md)
- Light lane: [lightweight-delivery-lane.md](../../behavior/lightweight-delivery-lane.md)
- SoftBudget pattern (mirror): `backend/src/test/java/com/bank/docgen/template/service/TemplateImportSoftBudgetTest.java`
- User direction: 2026-07-26「按你的建议整改吧»
