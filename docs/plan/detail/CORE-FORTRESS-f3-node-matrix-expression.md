# CORE-FORTRESS F3 — Node Matrix + Expression Engine (Detailed Plan)

**Program ID:** `CORE-FORTRESS`  
**Phase ID:** `CORE-FORTRESS-F3-NODE-MATRIX-EXPRESSION`  
**Phase status:** **Done** (2026-07-09)  
**Depends on:** CORE-FORTRESS F1 (**Done**), F2 (**Done**)  
**BDD:** `docs/behavior/core-fortress-f3-node-matrix-expression.md` — **ready** (`BDD-CORE-FORTRESS-F3-001`)

> **Single-active-phase invariant:** F3 closed. **F4** sole formal `In Progress`.

---

## 1. North star

**Template authors can express real business conditions safely** — `${customerName} != null`, numeric thresholds, and boolean logic — with **one shared evaluator** used at validation time (fail-closed, publish-blocking) and render time (fail-safe false). No SpEL, no silent content emission on bad expressions, full backward compatibility with `${flag} == true`.

---

## 2. Scope (in) / out (out)

| In scope (F3) | Out of scope (later phases) |
| --- | --- |
| `ConditionExpressionEvaluator` — parse, validate, extract refs, evaluate | Template rule **runtime** branch execution changes |
| `StructuredContentDocxWriter` integration | SpEL / scripting (forbidden) |
| `NodeMatrixValidationService` — condition + loop + nested walk | Frontend expression editor (F7) |
| `TemplateRuleValidationService` — syntax validation | LO pool / fonts / pagination (F4) |
| `FidelityWarningCode.INVALID_CONDITION_EXPRESSION` + i18n | Async durability (F5) |
| `TableComponentService` loopRow `loopVariable` schema check (F3-Q4 default) | E2E/UIUX (backend-only phase) |
| Backward compat `${showNotice} == true` | |

---

## 3. Exit criteria

1. **Shared evaluator** exists; writer + validation services delegate to it (no `SIMPLE_CONDITION_PATTERN` duplicate).
2. **Rich conditionals** work at render time: `!= null`, numeric compare, `&&` / `||` / `!`, parentheses.
3. **Validation fail-closed**: malformed `conditionBlock` expressions → `INVALID_CONDITION_EXPRESSION` blocker → binding invalid → publish gate blocked.
4. **Loop variable validation**: undeclared `loopBlock.loopVariable` (and table `loopRow`) → `UNRESOLVED_VARIABLE` blocker.
5. **Template rules**: malformed `conditionExpression` → `MALFORMED_RULE` → `RULE_BOUNDS` publish gate blocked.
6. **Runtime fail-safe**: malformed expression at render → condition false, debug log only (F3-C4).
7. **Backward compatible**: existing boolean equality tests green.
8. **Green gates:** `mvn -B -ntp -f backend/pom.xml verify`.
9. **Doc sync:** master-plan, ledger, program roadmap link, behavior spec indexed.

---

## 4. Task breakdown

| ID | Owner | Task | Depends on | Status |
| --- | --- | --- | --- | --- |
| **F3-T01** | behavior-spec-author | **BDD behavior spec** — `core-fortress-f3-node-matrix-expression.md` + this plan | — | **Done** (2026-07-09; readiness `ready`) |
| **F3-T02** | backend-engineer | **Expression engine core** — `ConditionExpressionEvaluator` (validateSyntax, extractVariableReferences, evaluate); `ConditionExpressionEvaluatorTest` TDD red→green | F3-T01 | **Done** (2026-07-09) |
| **F3-T03** | backend-engineer | **Writer integration** — replace `evaluateSimpleCondition` in `StructuredContentDocxWriter`; extend writer tests (BDD-F3-E2-*) | F3-T02 | **Done** (2026-07-09) |
| **F3-T04** | backend-engineer | **Node matrix hardening** — extend `NodeMatrixValidationService` for conditionBlock + loopBlock + nested walk; `FidelityWarningCode.INVALID_CONDITION_EXPRESSION`; i18n key | F3-T02 | **Done** (2026-07-09) |
| **F3-T05** | backend-engineer | **Rule validation** — `TemplateRuleValidationService` calls `validateSyntax`; malformed → `MALFORMED_RULE` | F3-T02 | **Done** (2026-07-09) |
| **F3-T06** | backend-engineer | **Table loopRow** — `TableComponentService` validates `loopVariable` against schema (F3-Q4); tests | F3-T04 | **Done** (2026-07-09) |
| **F3-T07** | backend-engineer | **Publish gate regression** — binding + rule gate paths with invalid expressions; `PublishGateServiceTest` / binding tests | F3-T04, F3-T05 | **Done** (2026-07-09) |
| **F3-T08** | architecture-reviewer | **Boundary + security review** — no SpEL; no sensitive data in errors; package placement (authoring vs sharedkernel) | F3-T03–T07 | **Done** (2026-07-09; **PASS**, no critical findings) |
| **F3-T09** | post-task-doc-sync | **Plan + ledger closeout** — mark F3 Done when exit criteria met | F3-T08 + green gates | **Done** (2026-07-09) |

### Recommended wave order

```text
Wave 0 — BDD + plan (Done)
  F3-T01

Wave 1 — Expression engine (TDD mandatory first)
  F3-T02

Wave 2 — Integrations (T03–T06 can parallel after T02 green)
  F3-T03 (writer)
  F3-T04 (node matrix)
  F3-T05 (rules) — parallel OK
  F3-T06 (table loopRow) — after T04

Wave 3 — Gate regression + review + closeout
  F3-T07 → F3-T08 → F3-T09
```

---

## 5. Implementation notes

### Package placement (F3-C1 / F3-Q3)

- **Preferred:** `com.bank.docgen.authoring.structured.expression.ConditionExpressionEvaluator`
- **Fallback:** `com.bank.docgen.sharedkernel.expression` if architecture review finds rendering→authoring dependency violation
- **Forbidden:** duplicate regex in `rendering` and `template` packages

### Error code mapping

| Context | Code / status | messageKey |
| --- | --- | --- |
| Structured content validation blocker | `FidelityWarningCode.INVALID_CONDITION_EXPRESSION` | `generation.warning.fidelity.invalidConditionExpression` |
| Undeclared `${var}` in expression | `FidelityWarningCode.UNRESOLVED_VARIABLE` | `generation.warning.fidelity.unresolvedVariable` |
| Template rule syntax error | `RuleValidationStatus.MALFORMED_RULE` | existing rule validation envelope |
| Runtime render (malformed) | *(none — fail-safe false)* | debug log only |

### Key files to touch

| File | Change |
| --- | --- |
| `StructuredContentDocxWriter.java` | Remove `SIMPLE_CONDITION_PATTERN`; inject/use evaluator |
| `NodeMatrixValidationService.java` | conditionBlock + loopBlock branches |
| `TemplateRuleValidationService.java` | `validateSyntax` before variable extraction |
| `TableComponentService.java` | loopRow `loopVariable` schema check |
| `FidelityWarningCode.java` | `INVALID_CONDITION_EXPRESSION` |
| `messages_en.properties` | new fidelity message key |

---

## 6. Gate commands

| Context | Command |
| --- | --- |
| TDD inner loop (expression) | `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=ConditionExpressionEvaluatorTest` |
| TDD inner loop (integration) | `mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=StructuredContentDocxWriterTest,NodeMatrixValidationServiceTest,TemplateRuleValidationServiceLogicTest` |
| Full backend gate | `mvn -B -ntp -f backend/pom.xml verify` |

**Gate evidence (closeout):** `mvn -B -ntp -f backend/pom.xml verify` BUILD SUCCESS (2026-07-09) — **1121** Surefire, **0** failures, **3** skipped.

---

## 7. Acceptance scenario index (BDD)

| Group | IDs | Count |
| --- | --- | --- |
| F3-E1 Shared engine | BDD-F3-E1-001 … 007 | 7 |
| F3-E2 Runtime render | BDD-F3-E2-001 … 005 | 5 |
| F3-E3 Node matrix | BDD-F3-E3-001 … 007 | 7 |
| F3-E4 Template rules | BDD-F3-E4-001 … 004 | 4 |
| F3-E5 Safety / i18n | BDD-F3-E5-001 … 002 | 2 |
| **Total** | | **25** |

---

## 8. CORE-FORTRESS program roadmap (context)

| Phase | Name | Status |
| --- | --- | --- |
| F1 | Rendering core correctness | **Done** |
| F2 | Runtime lightweight | **Done** |
| **F3** | Node matrix + expression engine | **Done** (2026-07-09) |
| **F4** | Production rendering hardening | **In Progress** |
| F5 | Async durability + security | Not Started |
| F6 | Frontend kernel refactor | Not Started |
| F7 | Authoring UX | Not Started |
| F8 | Observability + evidence | Not Started |

---

## 9. Traceability

- Behavior: [core-fortress-f3-node-matrix-expression.md](../../behavior/core-fortress-f3-node-matrix-expression.md)
- Prior phases: [CORE-FORTRESS-f1-rendering-correctness.md](./CORE-FORTRESS-f1-rendering-correctness.md), [CORE-FORTRESS-f2-runtime-lightweight.md](./CORE-FORTRESS-f2-runtime-lightweight.md)
- Program roadmap: [CORE-FORTRESS-program-roadmap.md](./CORE-FORTRESS-program-roadmap.md)
- Master plan: [master-plan.md](../master-plan.md) § CORE-FORTRESS
- Ledger: [execution-sync-ledger.md](../execution-sync-ledger.md)
