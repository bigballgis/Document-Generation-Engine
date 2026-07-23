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
  - docs/behavior/pqh-f8-format-date-tz.md
  - docs/adr/rendering-authoring/0019-structured-authoring-and-rendering-boundary.md
  - docs/adr/api/0013-api-contract-visibility-audit-and-context.md
  - docs/plan/core-excellence-program-2026-07.md
  - docs/plan/intl-bank-letter-readiness-program.md
  - docs/plan/post-queue-hardening-program-2026-07.md
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
6. **Locale + optional ISO currency + optional date zone:** `FORMAT_DATE` and
   `FORMAT_AMOUNT` consume request `context.locale` for number/date **display**
   localization; blank/missing/unparseable → **`zh-CN`**. `FORMAT_AMOUNT(value)`
   keeps locale-default currency (fraction digits fixed at 2). Optional
   `FORMAT_AMOUNT(value, currencyCode)` takes an **ISO 4217 alphabetic** code
   (normalized uppercase); currency identity comes from that code while
   grouping/symbol placement still follow `context.locale`. The second argument
   is **not** a locale tag. Null/blank/invalid ISO currency or arity ∉ {1,2} →
   `VARIABLE_COMPUTE_FAILED` (no silent locale-default fallback). Binary form
   uses the ISO currency’s default fraction digits. `FORMAT_DATE` unary/binary
   calendar-day conversion (documented UTC default + optional IANA `zoneId`) is
   specified in **Amendment 2026-07-23** below — locale remains display-only and
   is never used as a conversion zone. `SPELL_AMOUNT` is **CNY Chinese uppercase
   only** and does not switch language with locale *(superseded for binary form
   by Amendment 2026-07-18 below; unary remains CNY Chinese locale-independent)*.
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

## Amendment — 2026-07-23 (FORMAT_DATE optional IANA zoneId; PQH-F8)

**Prior decision (CE-K03 / original Decision §6):** `FORMAT_DATE(value)` formats a
calendar date with `context.locale` (`FormatStyle.MEDIUM`); Instant→calendar-day
conversion zone was **undocumented** (implementation used UTC for `java.util.Date`).

**Amended decision:** `FORMAT_DATE` supports unary and optional binary forms.
Locale and timezone are **orthogonal channels**.

| Form | Semantics |
| --- | --- |
| `FORMAT_DATE(value)` | Resolve calendar day, then format with `DateTimeFormatter.ofLocalizedDate(MEDIUM).withLocale(context.locale)` (default/fallback still **`zh-CN`** per Decision §6). |
| `FORMAT_DATE(value, zoneId)` | Second argument = **IANA ZoneId** string (e.g. `'Asia/Shanghai'`, `'UTC'`). **Not** a locale tag. Used only when converting instant-like values to a calendar day; ignored (success) for date-only / `LocalDate` / `LocalDateTime` wall dates. |

| Input class | Unary (zone omitted) | Binary (explicit `zoneId`) |
| --- | --- | --- |
| `LocalDate` / date-only ISO `yyyy-MM-dd` | That calendar day (zone N/A) | Same day; zone unused |
| `LocalDateTime` | `.toLocalDate()` wall date | Same; zone unused |
| `java.util.Date` / `Instant` / ISO datetime with offset or `Z` | Convert with **UTC** → `LocalDate` (**documented default**; Date-compat) | `instant.atZone(zoneId).toLocalDate()` |
| `OffsetDateTime` / `ZonedDateTime` | Embedded offset/zone local date | Convert via `toInstant()` + explicit `zoneId` |

| Field | Value |
| --- | --- |
| **Date** | 2026-07-23 |
| **Rationale** | IBL **F8** / **Q2** residual honesty — remove silent timezone lies; prefer letter as-of as **date-only** variables; optional IANA zone for Instant→local day (A2-style second arg). Delivered under NON-CE [PQH](../../plan/post-queue-hardening-program-2026-07.md) TM **#160** (not an IBL wave reopen). |
| **Behavior note** | [pqh-f8-format-date-tz.md](../../behavior/pqh-f8-format-date-tz.md) — `bdd_readiness: ready` (**BDD-PQH-F8-001…012**). |
| **ISO string honesty** | Date-only `yyyy-MM-dd` → calendar day as written. Datetime with offset/`Z` → parse to instant/offset then UTC or explicit zone — **must not** succeed via `substring(0,10)` prefix truncate. Ambiguous/unparseable → `VARIABLE_COMPUTE_FAILED`. |
| **Fail-closed** | Arity ∉ {1,2}; null value; binary blank/invalid ZoneId (incl. locale tags like `en-US`); unparseable datetime / non-date type → existing **`VARIABLE_COMPUTE_FAILED`**. Binary form does **not** silently fall back to UTC. |
| **Rejected** | Infer conversion zone from `context.locale`; add `context.timeZone` / platform as-of context field (ADR-0013 unchanged); change unary Instant/`Date` default away from UTC without migration. |
| **Scope of amendment** | `FORMAT_DATE` arity + IANA second arg + documented UTC unary default + ISO datetime honesty + carrier matrix. Does **not** change whitelist set membership, hard bounds, `FORMAT_AMOUNT` / `SPELL_AMOUNT`, or error code. Does **not** flip checklist **#3b** / **#5a**, activate **#119** / CE-O02, mark **#53** Done, or claim IBL/CE/go-live Done. |
| **Contract surface** | [openapi-v1.yaml](../../api/openapi-v1.yaml) compute validate/evaluate descriptions; [contract-outline.md](../../api/contract-outline.md); [docs/api/README.md](../../api/README.md). |

**Consequences of this amendment:**

- Unary Instant/`Date`/ISO datetime without zone formats the **UTC** calendar day — honest and backward-compatible with today’s `Date` path; not a silent bank timezone.
- Bank-local calendar days for instants require explicit `FORMAT_DATE(..., 'Asia/Shanghai')` (or other IANA id); locale still only controls MEDIUM display language/pattern.
- Letter “as-of” / signing dates should be supplied as date-only variables so zone is irrelevant.

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
