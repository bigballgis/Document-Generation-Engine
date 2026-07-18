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
  - docs/behavior/ibl-a2-format-amount-currency.md
  - docs/behavior/ibl-a3-amount-in-words.md
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/plan/core-excellence-program-2026-07.md
  - docs/plan/intl-bank-letter-readiness-program.md
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
6. **Locale + optional ISO currency:** `FORMAT_DATE` and `FORMAT_AMOUNT` consume
   request `context.locale` for number/date localization; blank/missing/unparseable
   → **`zh-CN`**. `FORMAT_AMOUNT(value)` keeps locale-default currency (fraction
   digits fixed at 2). Optional `FORMAT_AMOUNT(value, currencyCode)` takes an
   **ISO 4217 alphabetic** code (normalized uppercase); currency identity comes from
   that code while grouping/symbol placement still follow `context.locale`. The
   second argument is **not** a locale tag. Null/blank/invalid ISO currency or
   arity ∉ {1,2} → `VARIABLE_COMPUTE_FAILED` (no silent locale-default fallback).
   Binary form uses the ISO currency’s default fraction digits. `SPELL_AMOUNT` is
   **CNY Chinese uppercase only** and does not switch language with locale
   *(superseded for binary form by Amendment 2026-07-18 below; unary remains
   CNY Chinese locale-independent)*.
   Negatives and out-of-range amounts for `SPELL_AMOUNT` fail closed.
7. **Caller compute keys:** Values supplied under compute variable keys are ignored;
   engine results overwrite them.
8. **Failure:** Runtime/preview evaluation failures surface as
   `VARIABLE_COMPUTE_FAILED` (`messageKey=api.error.variable.computeFailed`,
   HTTP 422, `retryable=false`) with observable variable key + expression summary
   (≤128 chars). Author save-time invalid expressions reject with
   `TEMPLATE_VALIDATION_FAILED`.

## Amendment — 2026-07-18 (SPELL_AMOUNT ISO + locale language; IBL-A3)

**Prior decision (CE-K03 / original Decision §6):** `SPELL_AMOUNT` is **CNY Chinese
uppercase only** and does not switch language with locale.

**Amended decision:** `SPELL_AMOUNT` supports unary and optional binary forms:

| Form | Semantics |
| --- | --- |
| `SPELL_AMOUNT(value)` | **Always** CNY Chinese uppercase (`SpellAmountCn` semantics); **locale-independent** (does not follow `context.locale` language). |
| `SPELL_AMOUNT(value, currencyCode)` | Currency identity = **ISO 4217** alphabetic code (normalized uppercase); spelling **language** = primary language of `context.locale` (default/fallback still **`zh-CN`** per Decision §6). Second argument is **not** a locale tag. |

| Field | Value |
| --- | --- |
| **Date** | 2026-07-18 |
| **Rationale** | IBL-A3 / Task Master **#109** — international amount-in-words gap (F3): at least **en + USD** while preserving CNY/`zh` and unary golden compatibility; fail-closed on unsupported pairs (no silent wrong language). |
| **Behavior note** | [ibl-a3-amount-in-words.md](../../behavior/ibl-a3-amount-in-words.md) — `bdd_readiness: ready` (**BDD-IBL-A3-001…012**). |
| **Required pairs (this leaf)** | `(zh, CNY)` — same semantics as unary / `SpellAmountCn`; `(en, USD)` — English USD amount-in-words. |
| **Fail-closed** | Unsupported (language, currency) pair, null/blank/invalid ISO currency, arity ∉ {1,2}, negatives / out-of-range → existing **`VARIABLE_COMPUTE_FAILED`**. No silent fallback to unary Chinese or wrong-language success. |
| **Extensibility** | Spellers dispatch via a registrable (language × currency) table/strategy; new pairs do not change DSL name/arity. Full ISO × language matrix is **out of scope** for this leaf. |
| **Scope of amendment** | `SPELL_AMOUNT` arity + ISO second arg + locale-language selection only. Does **not** change `FORMAT_*`, whitelist set membership, hard bounds, or error code. Does **not** flip checklist **#3b** / **#5a** or claim go-live. |
| **Contract surface** | [openapi-v1.yaml](../../api/openapi-v1.yaml) compute validate/evaluate descriptions; [contract-outline.md](../../api/contract-outline.md); [docs/api/README.md](../../api/README.md). |

**Consequences of this amendment:**

- Unary `SPELL_AMOUNT(${principal})` remains safe under any locale (including `en-US`) — no silent English switch.
- International letters must use binary `SPELL_AMOUNT(..., 'USD')` (or other registered pair) with an explicit `en*` locale when English spelling is required; default `zh-CN` makes binary USD fail closed.
- Extending supported pairs is an implementation registration (+ tests/docs), not a new DSL function name.

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
