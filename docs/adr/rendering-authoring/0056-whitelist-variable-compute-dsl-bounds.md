---
id: ADR-0056
type: ADR
status: Accepted
sourceOfTruth: true
owners:
  - template-authoring
  - rendering
adrNumber: "0056"
topic: rendering-authoring
related:
  - docs/behavior/ce-k03-variable-compute-engine.md
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/plan/core-excellence-program-2026-07.md
---

# ADR-0056 — Whitelist variable-compute DSL bounds (no script engines)

## Status

Accepted

## Context

Template variables can declare `computeExpression` / `COMPUTED` derived values. Banking
letter generation must support declarative aggregation and amount/date formatting
without exposing an arbitrary code-execution surface to template authors or API callers.

## Decision

1. **Engine form:** Platform-owned **whitelist DSL** parser/evaluator only. Literals,
   `${path}` references, and whitelist function calls are the only executable constructs.
2. **Forbidden engines:** Do **not** introduce Groovy, JavaScript, full SpEL, or any
   other general-purpose script/eval engine as the compute implementation.
3. **Whitelist functions (exact, case-sensitive UPPER_SNAKE):**
   `COALESCE`, `SUM`, `COUNT`, `AVG`, `FILTER`, `FORMAT_AMOUNT`, `FORMAT_DATE`,
   `SPELL_AMOUNT`. Unknown names fail closed.
4. **FILTER form:** `FILTER(collection, fieldPath, op, literal?)` with
   `op ∈ {EQ, NE, GT, GE, LT, LE, IS_NULL, IS_NOT_NULL}`. Matching rows project the
   filtered field values so `SUM`/`AVG`/`COUNT` compose naturally.
5. **Hard bounds:** expression length ≤ 2048; function nesting depth ≤ 8;
   `${path}` segments ≤ 16; compute→compute dependency depth ≤ 8; collection size
   ≤ 10_000 for `SUM`/`AVG`/`COUNT`/`FILTER`.
6. **Locale:** `FORMAT_AMOUNT` / `FORMAT_DATE` consume request `context.locale`;
   blank/missing/unparseable → **`zh-CN`**. `SPELL_AMOUNT` is **CNY Chinese uppercase
   only** and does not switch language with locale. Negatives and out-of-range amounts
   fail closed.
7. **Caller compute keys:** Values supplied under compute variable keys are ignored;
   engine results overwrite them.
8. **Failure:** Runtime/preview evaluation failures surface as
   `VARIABLE_COMPUTE_FAILED` (`messageKey=api.error.variable.computeFailed`,
   HTTP 422, `retryable=false`) with observable variable key + expression summary
   (≤128 chars). Author save-time invalid expressions reject with
   `TEMPLATE_VALIDATION_FAILED`.

## Consequences

- Authors get declarative compute without a script sandbox risk surface.
- Golden packages `04-compute-variables` and `05-chinese-uppercase-amount` exercise
  the same engine path used by runtime/preview assembly.
- Extending the function set requires an ADR amendment; ad-hoc script plugins are out
  of scope.

## Alternatives considered

- **Groovy / JS / SpEL eval** — rejected: arbitrary code execution risk and
  non-deterministic authoring surface.
- **Caller-preformatted amounts only** — rejected: duplicates formatting logic and
  blocks bank-grade RMB uppercase amount requirements.
