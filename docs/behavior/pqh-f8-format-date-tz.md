# BDD behavior spec: PQH-F8 — `FORMAT_DATE` timezone / as-of date semantics

| Field | Value |
| --- | --- |
| **Document status** | `ready` |
| **BDD ID prefix** | `BDD-PQH-F8` |
| **Authored** | 2026-07-23 |
| **Program / queue** | Post-queue hardening · **PQH-F8** (IBL F8 / Q2 follow-on) + **PQH-CHARTER** (same leaf, docs-only) |
| **Slice** | `pqh-f8-format-date-tz` |
| **Branch** | `feat/pqh-f8-format-date-tz` |
| **Worktree** | `D:/working/DGE-pqh-f8-format-date-tz` |
| **Base** | `c5473c69` (MAIN tip handoff) |
| **Placement** | ISOLATED |
| **Task Master** | **#159** PQH-CHARTER + **#160** PQH-F8 (**in-progress** / sole-active); Batch Recommendation **merge** (`member_task_ids: ["159","160"]`) |
| **Formal phase** | **None** (do not invent a sole-active formal P-phase) |
| **Batch recommendation** | **merge** (`proposed_slice_id: pqh-f8-format-date-tz`; vetoes: F8 vs N19–N22 unrelated; do not flip #3b/#5a; do not activate CE-O02 / #119; do not mark #53 Done; do not claim IBL/CE/go-live Done) |
| **Owning docs** | **This file (leaf behavior SoT for PQH-F8)**; finding SoT [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) **F8 / Q2**; upstream DSL [ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md) (K03-C10/C11); locale/currency patterns [ibl-a2-format-amount-currency.md](./ibl-a2-format-amount-currency.md) / [ibl-a3-amount-in-words.md](./ibl-a3-amount-in-words.md) / [ibl-a6-regenerate-locale-replay.md](./ibl-a6-regenerate-locale-replay.md); bounds [ADR-0056](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md); context whitelist [ADR-0013](../adr/api/0013-api-contract-visibility-audit-and-context.md) |
| **Frontend UI** | **`frontend_ui_in_scope=false`** (compute DSL / BE evaluator contract only; E2E/UIUX **N/A**) |

**Completion claim constraints:** This leaf closes IBL finding **F8** / open residual **Q2** for honest `FORMAT_DATE` timezone / calendar-day conversion semantics. **Do not** claim IBL program Done, CE Done, or go-live. **Do not** flip checklist **#3b** / **#5a** GO. **Do not** activate **CE-O02** / **#119**. **Do not** mark umbrella **#53** Done. **Do not** invent Word numbers, fonts, or LDAP.

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: merge
  member_task_ids: ["159", "160"]  # PQH-CHARTER, PQH-F8
  proposed_slice_id: pqh-f8-format-date-tz
  shared_acceptance_surface: >
    FORMAT_DATE timezone/as-of honesty + post-queue-hardening-2026-07 charter docs
  vetoes_applied:
    - f8-vs-n19n22-unrelated-domains
    - checklist-#3b/#5a-GO
    - CE-O02
    - mark-#53-CE-Done
    - activate-#119-Word-host
    - F7-parked-not-in-leaf
    - do-not-claim-IBL-go-live-Done
  evidence_amortization: mvn verify + Stage 10 deploy; E2E N/A (FE out of scope)
  on_red_split_hint: Peel F8 vs charter on red verify
```

| IN (this leaf) | OUT (later / explicitly forbidden) |
| --- | --- |
| Honest `FORMAT_DATE` conversion + locale display contract in compute DSL | Inferring timezone from `context.locale` |
| Optional binary `FORMAT_DATE(value, zoneId)` (IANA) for instant-like values | New `context.timeZone` / bank as-of API field (ADR-0013 expansion) |
| Documented UTC default for unary instant-like conversion (backward-compatible) | Silent calendar-day from ISO datetime prefix truncate |
| Instant / OffsetDateTime / ZonedDateTime / `java.util.Date` path clarity | Word host / Path E / #119 |
| OpenAPI / contract-outline / ADR-0056 note of zone argument + defaults | FE Playwright / OA journey |
| **PQH-CHARTER** docs-only program charter (BDD **not-applicable** for charter prose) | Flip #3b/#5a; claim IBL/CE/go-live Done; schedule F7 here |

### 0.1 PQH-CHARTER (same leaf) — BDD not-applicable

| Item | Value |
| --- | --- |
| **Member** | `PQH-CHARTER` |
| **bdd_readiness** | **`not-applicable`** |
| **Reason** | Docs-only program charter for post-queue hardening (registration / ledger / plan prose). No runtime actor journey, no fail-closed product surface, no Given/When/Then acceptance scenarios. |
| **Do not** | Invent fake G/W/T for charter narrative. |

---

## 1. Overview

### 1.1 Problem (current evidence — implementation input)

| Finding | Evidence |
| --- | --- |
| `FORMAT_DATE` forces UTC when converting `java.util.Date` → `LocalDate` | `ComputeExpressionEvaluator.toLocalDate` → `date.toInstant().atZone(ZoneOffset.UTC)` |
| Conversion zone is **undocumented** → callers may assume bank-local / server-local / locale zone | IBL **F8** / **Q2** |
| `OffsetDateTime` uses embedded offset local date; `Date` uses UTC — inconsistent instant handling | Same `toLocalDate` |
| ISO datetime **strings** take first 10 chars (`yyyy-MM-dd`) without zone conversion — silent wrong calendar day near midnight | `toLocalDate` string branch |
| `Instant` type not handled (falls through to string/`fail`) | Same |
| Locale already drives `FormatStyle.MEDIUM` display | `evalFormatDate` + K03-C11; A6 regenerates locale only |
| No `context.timeZone` on runtime `Context` whitelist | `ContextView` / ADR-0013 |

### 1.2 Behavior domains

| Domain | Summary |
| --- | --- |
| **F8-S1 Calendar-date path** | `LocalDate` and **date-only** ISO `yyyy-MM-dd` → that calendar day; zone N/A |
| **F8-S2 Instant-like conversion** | `Instant` / `java.util.Date` / ISO datetime-with-offset-or-Z → convert to calendar day via zone rules |
| **F8-S3 Offset/Zoned carriers** | Unary: use embedded offset/zone local date; binary: convert via Instant + explicit zone |
| **F8-S4 Optional zone argument** | `FORMAT_DATE(value, zoneId)` — IANA ZoneId; fail-closed if blank/invalid |
| **F8-S5 Documented UTC default** | Unary instant-like without zone → **UTC** (compatible with today’s `Date` path; no silent bank TZ) |
| **F8-S6 Locale display (unchanged channel)** | Display string = `DateTimeFormatter.ofLocalizedDate(MEDIUM).withLocale(context.locale)` |
| **F8-S7 As-of = variable calendar date** | Letter “as-of” / signing dates are **template variables** (prefer date-only); no platform as-of context field |

---

## 2. Actor / Role

| Actor | Role / capability | Notes |
| --- | --- | --- |
| **Template author** | Writes `computeExpression`; optional evaluate API | Must be able to format calendar dates and, when needed, instant→local day with explicit IANA zone |
| **Runtime API caller** | Valid credential; generate / batch; `context.locale` | Supplies supply variables; locale for display; **not** a hidden timezone channel |
| **System (compute)** | `VariableComputeService` → `ComputeExpressionEvaluator` | Parses arity; converts to `LocalDate`; formats with locale |
| **System (author APIs)** | `POST …/compute-expressions/validate\|evaluate` | Binary zone form legal; evaluate previews zone-sensitive results |

---

## 3. Goal

1. Remove **silent** timezone lies: every Instant→calendar-day path is either **documented UTC** or an **explicit IANA zone**.  
2. Prefer bank-letter **as-of** values as **date-only** variables (`yyyy-MM-dd` / `LocalDate`) — zone-independent.  
3. Match IBL-A2 pattern: optional second DSL argument for identity (`zoneId`), while **locale remains** the display channel (`context.locale`).  
4. Keep unary `FORMAT_DATE(value)` working for existing ISO date strings and golden paths.  
5. Fail-closed on null value, illegal arity, blank/invalid zone (binary), and unparseable datetime strings.  
6. Unit + `mvn verify` green; OpenAPI / contract-outline / ADR-0056 document the contract.  
7. Formal phase **None**; no go-live / #3b/#5a / #119 claims.

---

## 4. Confirmed decisions vs non-confirmed

### 4.1 Confirmed decisions this leaf (repo-fact adjudication — no product fork remaining)

| ID | Decision | Basis |
| --- | --- | --- |
| **F8-C1** | **Function forms:** `FORMAT_DATE(value)` **or** `FORMAT_DATE(value, zoneId)`. Arity ∉ {1,2} → `VARIABLE_COMPUTE_FAILED`. | Mirrors A2-C1; parser already supports multi-arg; today’s `requireOne` must widen |
| **F8-C2** | **Second arg = IANA ZoneId string** (e.g. `'Asia/Shanghai'`, `'UTC'`, `'America/New_York'`), **not** a locale tag and **not** a fixed offset alias inventing bank policy. Locale **only** from `context.locale` (CE-K03 / A6). | A2-C2 isomorphism; locale ≠ timezone |
| **F8-C3** | **Zone evaluation:** `zoneId` may be a string literal or an expression evaluating to string (incl. `${path}`). Trim; resolve via `ZoneId.of` (JDK). Unknown/illegal → `VARIABLE_COMPUTE_FAILED`. | Fail-closed; DSL consistency |
| **F8-C4** | **Do not infer zone from locale.** `en-US` / `zh-CN` never select a conversion zone. | Product honesty; CE-C01 locale is language/display |
| **F8-C5** | **No `context.timeZone` / as-of context field in this leaf.** ADR-0013 whitelist unchanged. Zone stays inside compute DSL. Bank letter “as-of” = **caller-supplied calendar date variable**. | Prefer DSL-only; E5 rejects runtime as-of dynamic re-pin; minimize API surface |
| **F8-C6** | **Calendar inputs (zone-independent):** `LocalDate`; string that is **exactly** date-only ISO-8601 calendar date (`yyyy-MM-dd`, optional surrounding whitespace). Result day = that date. Binary `zoneId` **ignored** for these inputs (success; zone unused). | Letter as-of path; authors may still write binary form harmlessly |
| **F8-C7** | **`LocalDateTime`:** use `.toLocalDate()` (wall-clock date already local). Binary `zoneId` **ignored**. | Existing behavior; no inventing offset |
| **F8-C8** | **Instant-like types — unary (zone omitted):** convert with **UTC** → `LocalDate`. Applies to: `java.util.Date`, `java.time.Instant`, and ISO-8601 **datetime** strings that parse to an instant/offset (see F8-C10). **Documented default** = today’s `Date` path; **not** a silent bank TZ. | Backward-compat + honesty |
| **F8-C9** | **Instant-like types — binary:** `instant.atZone(zoneId).toLocalDate()` (or equivalent). Same types as F8-C8. | Explicit bank-local / branch-local day |
| **F8-C10** | **ISO string honesty (closes silent truncate lie):** | F8 root symptom for JSON string variables |
| | • Date-only `yyyy-MM-dd` → F8-C6 | |
| | • Datetime with offset or `Z` (e.g. `2024-01-15T23:30:00Z`) → parse to instant/offset, then F8-C8/C9 — **must not** take substring(0,10) as the calendar day | |
| | • Ambiguous / unparseable datetime → `VARIABLE_COMPUTE_FAILED` | |
| **F8-C11** | **`OffsetDateTime` / `ZonedDateTime`:** | Unify carrier honesty |
| | • **Unary:** local date from the value’s own offset/zone (`.toLocalDate()` / zone-local date) — value already carries zone | |
| | • **Binary:** convert via `toInstant()` + explicit `zoneId` (F8-C9) | |
| **F8-C12** | **Null value:** `FORMAT_DATE` value null → `VARIABLE_COMPUTE_FAILED` (existing). | K03 |
| **F8-C13** | **Binary blank zone:** zone evaluates to null / blank → `VARIABLE_COMPUTE_FAILED` (no silent UTC fallback on binary form). | A2-C7 isomorphism |
| **F8-C14** | **Locale display unchanged:** after `LocalDate` resolved, format with `DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)`; missing/blank/unparseable locale → `zh-CN` per K03-C11. Zone does **not** change MEDIUM pattern language. | A6 / K03; orthogonal channels |
| **F8-C15** | **Regenerate:** no new retention field. Locale replay remains A6. Zone comes from expression / variables only (re-evaluated). | A6-N4 closed by this leaf for compute semantics; retention OOS |
| **F8-C16** | **Error code:** no new top-level code; failures stay `VARIABLE_COMPUTE_FAILED` (HTTP 422; category/retryable per CE-K03). | Shrink scope |
| **F8-C17** | **Docs (mandatory):** OpenAPI compute validate/evaluate descriptions note unary + optional `zoneId`; contract-outline / API README cross-link; **amends ADR-0056** Decision text for zone + UTC default + ISO datetime honesty (decision text, not task Done status). **Docs-first complete (stage 3):** ADR-0056 Amendment 2026-07-23 + OpenAPI/contract-outline/API README; backend keeps code aligned. | A2-C13 pattern |
| **F8-C18** | **FE:** `frontend_ui_in_scope=false`. Optional client comment/sample fix if any pseudo-locale second arg exists; no Playwright. | Delivery scope |
| **F8-C19** | **Gates:** `mvn -B -ntp -f backend/pom.xml verify`; behavior acceptance surface → Stage 5/10 queued Docker deploy evidence; architecture review. E2E/UIUX **N/A**. | Delivery constitution |
| **F8-C20** | **Completion boundary:** F8 Done ≠ IBL program Done ≠ go-live; #3b/#5a stay CONDITIONAL; #119 / CE-O02 / #53 untouched. | Queue policy |

### 4.2 Confirmed upstream (this leaf consumes only)

| ID | Decision | Source |
| --- | --- | --- |
| **F8-U1** | `FORMAT_DATE` whitelist + locale MEDIUM formatting | CE-K03 / ADR-0056 |
| **F8-U2** | `context.locale` retention + regenerate replay | IBL-A6 |
| **F8-U3** | Optional second-arg identity pattern for FORMAT_* | IBL-A2 / IBL-A3 |
| **F8-U4** | Strict `context` whitelist; unknown fields → 400 | ADR-0013 |
| **F8-U5** | Runtime as-of dynamic template re-pin rejected | IBL-E5 |

### 4.3 Non-confirmed / rejected (must not promote)

| ID | Statement | Status |
| --- | --- | --- |
| **F8-N1** | Infer conversion zone from `context.locale` | **Rejected** — F8-C4 |
| **F8-N2** | Add `context.timeZone` / bank as-of context this leaf | **Rejected** — F8-C5 (future ADR-0013 amendment only if product reopens) |
| **F8-N3** | Change unary `Date`/`Instant` default away from UTC without migration | **Rejected** — keep UTC + document (F8-C8) |
| **F8-N4** | Keep ISO datetime string prefix-truncate as success path | **Rejected** — F8-C10 |
| **F8-N5** | FE management copy / Playwright required | **Rejected** — F8-C18 |
| **F8-N6** | Flip #3b/#5a / activate #119 / claim IBL or go-live Done | **Rejected** |

### 4.4 Product questions — resolution record

| Question (handoff) | Resolution | Decision IDs |
| --- | --- | --- |
| Timezone source: locale zone vs explicit arg vs bank as-of? | **Explicit optional IANA `zoneId` arg**; **not** locale; **as-of = date variable** (no context field) | F8-C2…C5 |
| Backward-compat UTC default vs migrate? | **Keep UTC** for unary instant-like; document + offer binary zone | F8-C8, F8-C9 |
| Instant / OffsetDateTime vs LocalDate-only? | Full matrix F8-C6…C11; prefer date-only for letters | F8-C6…C11 |
| Interaction with locale MEDIUM? | Locale = display only; zone = conversion only | F8-C14 |

**Open questions for user:** **none** — remaining items are confirmed defaults above. Reopen only if product later mandates a platform-wide `context.timeZone` (separate ADR-0013 leaf).

---

## 5. Trigger / Preconditions

**Triggers**

- Runtime generate / batch / async assemble evaluates `FORMAT_DATE` compute variables.  
- Author `evaluate` / `validate` compute-expression APIs.  
- Regenerate re-evaluates expressions under A6 locale replay (zone from expression/vars).

**Preconditions**

- Template variable schema permits `FORMAT_DATE` expressions (CE-K03 whitelist).  
- Caller authenticated/authorized per existing runtime/management rules.  
- For binary form, zone string must be a valid IANA ZoneId when the value is instant-like (or any binary call with blank zone fails per F8-C13).

---

## 6. Primary journey

1. Author binds a letter date as **date-only** `signDate=2024-01-15` (recommended).  
2. Expression `FORMAT_DATE(${signDate})` with `context.locale=en-US` (or zh-CN).  
3. Engine resolves calendar day → formats MEDIUM for that locale.  
4. (Optional) Upstream supplies an Instant / ISO datetime; author writes `FORMAT_DATE(${eventAt}, 'Asia/Shanghai')` for bank-local calendar day.  
5. Unary Instant without zone formats the **UTC** calendar day (documented).  
6. Failures surface as `VARIABLE_COMPUTE_FAILED` with variable key + expression summary (existing).

---

## 7. System responses

### 7.1 Success

| Condition | Response |
| --- | --- |
| Date-only / `LocalDate` | MEDIUM-localized string for that calendar day |
| Unary Instant/`Date`/ISO datetime Z | MEDIUM string for **UTC** calendar day |
| Binary Instant + valid zone | MEDIUM string for that zone’s calendar day |
| Unary `OffsetDateTime`/`ZonedDateTime` | MEDIUM string for embedded offset/zone local date |
| Binary offset/zoned + zone | MEDIUM string after Instant + explicit zone |

### 7.2 Fail-closed

| Condition | Behavior |
| --- | --- |
| Arity ∉ {1,2} | `VARIABLE_COMPUTE_FAILED` |
| Null value | same |
| Binary blank/invalid zone | same |
| Unparseable datetime / non-date type | same |
| Authz failure | existing 401/403 — unchanged |

---

## 8. Acceptance scenarios (Given / When / Then)

### BDD-PQH-F8-001 — Date-only ISO is zone-independent

**Given** expression `FORMAT_DATE(${signDate})`, `signDate='2024-01-15'`  
**And** `context.locale=en-US`  
**When** compute evaluates  
**Then** result is the MEDIUM en-US formatting of calendar day **2024-01-15**  
**And** result does not depend on any hidden server/bank timezone

### BDD-PQH-F8-002 — Locale still changes display, not the calendar day

**Given** same `signDate='2024-01-15'` and `FORMAT_DATE(${signDate})`  
**When** evaluated with `zh-CN` and with `en-US`  
**Then** both succeed and the two display strings are **distinguishable** (CE-K03 / A6 regression)  
**And** both represent the same calendar day 2024-01-15

### BDD-PQH-F8-003 — Unary `java.util.Date` / Instant uses documented UTC

**Given** an Instant (or `java.util.Date`) equivalent to `2024-01-15T23:30:00Z`  
**And** expression `FORMAT_DATE(${eventAt})` (unary)  
**When** evaluate  
**Then** calendar day used for formatting is **2024-01-15** (UTC)  
**And** the day is **not** silently shifted to a bank-local zone

### BDD-PQH-F8-004 — Binary zone shifts Instant calendar day

**Given** Instant `2024-01-15T23:30:00Z`  
**And** expression `FORMAT_DATE(${eventAt}, 'Asia/Shanghai')`  
**When** evaluate  
**Then** calendar day is **2024-01-16** (UTC+8)  
**And** MEDIUM formatting uses `context.locale` (zone does not replace locale)

### BDD-PQH-F8-005 — ISO datetime string must not prefix-truncate

**Given** `eventAt='2024-01-15T23:30:00Z'` (string)  
**And** `FORMAT_DATE(${eventAt}, 'Asia/Shanghai')`  
**When** evaluate  
**Then** calendar day is **2024-01-16**  
**And** behavior must **not** equal treating the value as date-only `2024-01-15` via `substring(0,10)`

### BDD-PQH-F8-006 — Unary OffsetDateTime uses embedded offset local date

**Given** `OffsetDateTime` `2024-01-15T23:30:00+08:00`  
**And** unary `FORMAT_DATE(${eventAt})`  
**When** evaluate  
**Then** calendar day is **2024-01-15** (offset-local)

### BDD-PQH-F8-007 — Binary OffsetDateTime converts via Instant + zone

**Given** `OffsetDateTime` `2024-01-15T23:30:00+08:00` (instant = `2024-01-15T15:30:00Z`)  
**And** `FORMAT_DATE(${eventAt}, 'UTC')`  
**When** evaluate  
**Then** calendar day is **2024-01-15** (UTC)

### BDD-PQH-F8-008 — Binary blank / invalid zone fail-closed

**Given** `FORMAT_DATE(${eventAt}, ${tz})` with Instant value  
**And** `tz` is null, `''`, or `'Not/AZone'`  
**When** evaluate  
**Then** `VARIABLE_COMPUTE_FAILED`  
**And** no successful formatted string

### BDD-PQH-F8-009 — Illegal arity fail-closed

**Given** `FORMAT_DATE()` or `FORMAT_DATE(${d}, 'UTC', 'extra')`  
**When** evaluate  
**Then** `VARIABLE_COMPUTE_FAILED`

### BDD-PQH-F8-010 — Zone arg is not a locale tag

**Given** Instant near a zone boundary and `FORMAT_DATE(${eventAt}, 'en-US')`  
**When** evaluate  
**Then** `VARIABLE_COMPUTE_FAILED` (illegal ZoneId)  
**And** locale remains solely from `context.locale` on other successful calls

### BDD-PQH-F8-011 — Date-only + unused zone still succeeds

**Given** `signDate='2024-01-15'` and `FORMAT_DATE(${signDate}, 'Asia/Shanghai')`  
**When** evaluate  
**Then** success with calendar day **2024-01-15** (zone ignored per F8-C6)

### BDD-PQH-F8-012 — Contract docs record zone + UTC default

**Given** this leaf’s implementation set  
**When** reviewing OpenAPI compute descriptions, contract-outline / API README, and ADR-0056  
**Then** docs state: `FORMAT_DATE(value)` and `FORMAT_DATE(value, zoneId)`; unary instant-like → **UTC**; binary → IANA zone; date-only preferred for letter as-of; locale still `context.locale` for MEDIUM display  
**And** examples do **not** describe the second argument as a locale tag

---

## 9. Boundary / exception

| Scenario | Behavior |
| --- | --- |
| `LocalDateTime` with binary zone | Ignore zone; use wall `.toLocalDate()` |
| `ZonedDateTime` unary | Zone-local date from the value |
| Nested `FORMAT_DATE(COALESCE(...), 'UTC')` | Legal if args evaluate correctly |
| Golden `04-compute-variables` date-only paths | Must remain green |
| Authorization failure | Existing fail-closed; unchanged |
| Server JVM default timezone | **Must not** affect Instant→LocalDate (use UTC or explicit zone only) |

---

## 10. Observable evidence

| Evidence | Notes |
| --- | --- |
| Unit matrix | Cover BDD-PQH-F8-001…011 (012 = doc review) |
| API evaluate (optional) | `success=true` + `result` string |
| Golden | date-only `FORMAT_DATE` paths remain GREEN |
| Contract | OpenAPI + ADR-0056 amendment + contract-outline |
| Gates | `mvn verify` GREEN; queued Docker deploy evidence |
| Trace | Existing `metadata.traceId` retained |

---

## 11. Traceability

| Item | Reference |
| --- | --- |
| Finding | IBL **F8** — [intl-bank-letter-readiness-program.md](../plan/intl-bank-letter-readiness-program.md) §1 |
| Open residual | IBL **Q2** — same doc §9 (answered by scheduling this leaf; plan row update at doc-sync) |
| Upstream DSL | [ce-k03-variable-compute-engine.md](./ce-k03-variable-compute-engine.md) K03-C10/C11 |
| Pattern peers | IBL-A2 / A3 / A6 behavior specs |
| ADR | [ADR-0056](../adr/rendering-authoring/0056-whitelist-variable-compute-dsl-bounds.md) (Amendment 2026-07-23 — docs-first; code pending) |
| Context whitelist | [ADR-0013](../adr/api/0013-api-contract-visibility-audit-and-context.md) — **no** timeZone add |
| Code locus | `backend/.../sharedkernel/document/compute/ComputeExpressionEvaluator.java` (`evalFormatDate` / `toLocalDate`) |
| Task ids | `PQH-F8`, `PQH-CHARTER` (TM registration at stage 2) |

---

## 12. Implementation handoff (stage 4)

| Item | Value |
| --- | --- |
| **Recommended engineer** | **`backend-engineer`** |
| **Package rationale** | Behavior lives in `com.bank.docgen.sharedkernel.document.compute` (`ComputeExpressionEvaluator` / `VariableComputeEngine`) — whitelist DSL evaluation, not DOCX/PDF writer, LibreOffice, or rendering package layout. No Word/font/LO deps. |
| **Not** | `rendering-engineer` (no pagination/writer/PDF surface) |
| **TDD Red first** | Unit tests for BDD-PQH-F8-001…011 before production edits |
| **Docs with code** | ADR-0056 Amendment + OpenAPI/contract-outline/API README **already updated** (doc-keeper); keep implementation aligned — do not re-litigate confirmed F8-C* |
| **PQH-CHARTER** | Docs-only; may be authored by `doc-keeper` / plan-orchestrator in same leaf — **no** G/W/T |

---

## 13. Ready checklist

- [x] Actor / goal / trigger / preconditions  
- [x] Journey + system responses  
- [x] Acceptance scenarios BDD-PQH-F8-001…012  
- [x] Boundary / fail-closed  
- [x] Observable evidence  
- [x] Traceability to IBL F8 / Q2  
- [x] Product questions resolved as confirmed defaults  
- [x] `frontend_ui_in_scope=false`  
- [x] PQH-CHARTER marked BDD **not-applicable**  
- [x] No invented bank TZ policy beyond documented UTC + optional IANA zone  
