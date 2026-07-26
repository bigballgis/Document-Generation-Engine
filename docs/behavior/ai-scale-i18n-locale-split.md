---
id: BDD-AI-SCALE-I18N
title: AI-SCALE Leaf 3 — Frontend i18n locale domain split under soft size budgets
status: ready/shipped
date: 2026-07-27
bdd_readiness: ready
task_ids: [168]
placement: MAIN
worktree_path: n/a (REMOVED)
branch: main
slice: ai-scale-peel-i18n
integration_base: main@ce47186a
merge_sha: ce47186a0b949495e4a74dda8711bb910e877dcc
user_confirmation: 2026-07-26 「按你的建议整改吧»; stage-1 handoff confirms structure-only split with stable keys
delivery_lane: light
frontend_ui_in_scope: false
openapi_contract_change: false
runtime_api_semantics_change: false
stages_5_7_10: N/A
kind: structural-peel  # preserve message keys + copy; soft-budget acceptance
---

# AI-SCALE Leaf 3 — Frontend i18n Locale Domain Split

| Field | Value |
| --- | --- |
| **Slice** | `ai-scale-peel-i18n` (program alias `ai-scale-i18n-locale-split`) |
| **bdd_readiness** | **`ready`/shipped** |
| **Recorded** | 2026-07-27 |
| **Task Master** | **#168** (AI-SCALE Leaf 3 / `AI-SCALE-L3`) → **Done** (`ce47186a`) |
| **Program** | [ai-scale-remediation-program-2026-07.md](../plan/ai-scale-remediation-program-2026-07.md) |
| **Formal phase** | **None** (NON-CE AI-SCALE peel; do not invent P24+) |
| **Actor** | Platform engineer / `frontend-engineer` / `code-quality-reviewer` (structure); management-UI users (regression surface via unchanged resolved strings) |
| **delivery_lane** | **`light`** — structure-only catalog modularization with identical keys/values; E1–E5 hold; FE lint/type-check/test/build **still required**; stages **5–7** / **10** **N/A** |
| **Frontend UI journey** | **`frontend_ui_in_scope=false`** — no management-UI journey / visual / IA change |
| **OpenAPI / runtime contract** | **`openapi_contract_change=false`**; **`runtime_api_semantics_change=false`** |
| **Soft budget SoT** | [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) § Complexity and Size · [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) |
| **i18n convention SoT** | [.cursor/skills/i18n-english-first/SKILL.md](../../.cursor/skills/i18n-english-first/SKILL.md) · English-first preserved |

**Completion claim constraints:** This leaf closes soft-budget hotspots on mega locale catalogs by **structural extract only**. It does **not** change product copy meaning, dotted message key paths, locale registry codes, or management-UI journeys. Do **not** flip checklist **#3b** / **#5a**. Do **not** mark umbrella **#53** / **#106** Done. Do **not** activate **#119** / CE-O02. Do **not** fold **#169** mega-test into this leaf.

---

## 0. Batch / slice context

```text
batch_recommendation:
  decision: solo
  rationale: >
    Queue head #168 frontend i18n locale split; #169 mega-test and
    backend peels veto merge (unrelated domains).
  member_task_ids: [168]
  proposed_slice_id: ai-scale-peel-i18n
  shared_acceptance_surface: >
    frontend i18n message catalog modularization; no copy meaning change
  vetoes_applied: [mega-test-split, backend-domain]
  evidence_amortization: >
    FE lint/type-check/test/build; key completeness/parity unit tests;
    E2E/Docker N/A (light lane)
  on_red_split_hint: >
    split by locale (en vs zh-CN) or by domain module if attribution unclear
```

| IN (this leaf) | OUT |
| --- | --- |
| Split oversized `frontend/src/i18n/locales/en.ts` and `zh-CN.ts` into domain modules under soft budgets | Intentional copy / wording / meaning changes |
| Keep stable public import paths (`@/i18n/locales/en`, `@/i18n/locales/zh-CN`) via thin facade re-export/compose | Renaming dotted `t('…')` key paths used by call sites |
| Preserve English-first + `zh-CN` parity / lazy load via `localeRegistry` | Backend message bundles / OpenAPI / Flyway |
| TDD: keep existing i18n unit tests green; add key completeness / value-parity / size tests as needed | Mega-test fixture split (**#169**) |
| Optional: extend existing `catalogs/` peel pattern for non-`api.error` domains | New locales, new product features, Playwright journey changes |

**Path correction (confirmed):** catalogs live under `frontend/src/i18n/locales/` (not `frontend/src/locales/`). TM **#168** description aligned in Stage 3. Residual stale `frontend/src/locales/` wording may remain in closed CRCH program docs — historical; not this leaf’s SoT.

---

## 1. Overview

### 1.1 Hotspot baseline (pre-peel evidence)

Measured in worktree `D:/working/DGE-ai-scale-peel-i18n` @ `757338d8` (PowerShell `Measure-Object -Line` / file size):

| Path | LOC (total) | Bytes | Soft signal |
| --- | --- | --- | --- |
| `frontend/src/i18n/locales/en.ts` | **3604** | ~155 KB | ≫ **500** file soft; ≫ **800** hard-band signal |
| `frontend/src/i18n/locales/zh-CN.ts` | **3536** | ~146 KB | same |
| `frontend/src/i18n/catalogs/apiErrorEn.ts` | already peeled | — | Pattern to extend / leave intact |
| `frontend/src/i18n/catalogs/apiErrorZhCn.ts` | already peeled | — | Pattern to extend / leave intact |

**Observed top-level `en` domains (split candidates):** `app`, `commandPalette`, `onboardingTour`, `login`, `session`, `api`, `forbidden`, `retiredSurface`, `nav`, `brand`, `home`, `dashboard`, `packageCatalog`, `collaboration`, `masters`, `audit`, `documentLocale`, `localeVariantFamily`, `templates`, `table`, `paste`, `common`, `apiPolicy`, `contentModules`, `journey`, `identity`, `legalHold`, `assetLibrary`.

Public loaders remain:

- `frontend/src/i18n/index.ts` → `import en from './locales/en'`
- `frontend/src/i18n/localeRegistry.ts` → dynamic `import('@/i18n/locales/en'|'zh-CN')`
- Many unit tests → `import en from '@/i18n/locales/en'` (and often `zh-CN`)

### 1.2 Product / operability intent (this leaf)

1. Agents and reviewers can edit domain-scoped locale modules that fit soft size budgets without scanning 3k+ line mega-files.
2. Runtime resolved messages for every existing dotted key remain **identical** (same key path → same string value in `en` and `zh-CN`).
3. Call sites continue to use the same `t('domain…')` keys and the same public module paths; facade re-export/compose is the preferred compatibility mechanism.
4. English remains the base/default locale; `zh-CN` stays additive/lazy via `localeRegistry` with existing parity guards.

### 1.3 Confirmed decisions

| ID | Decision | Source |
| --- | --- | --- |
| **I18N-C1** | Structural peel only — **no intentional** change to message key paths, string values/meaning, locale codes, storage key, or fallback behavior. | Handoff + user「按你的建议整改吧」 |
| **I18N-C2** | Soft acceptance targets (review signals; not a new hard CI SoT): domain module files prefer ≤ **500** LOC soft; hard >**800** requires approved further split; thin facade aggregators OK and expected. | quality-gate-threshold-baseline + ADC |
| **I18N-C3** | Public API stability: `default` export shape of `@/i18n/locales/en` and `@/i18n/locales/zh-CN` remains a nested message object consumable by vue-i18n / existing imports. Internal domain file layout is implementer choice (e.g. `locales/domains/*` or sibling modules) as long as facade preserves paths. | Call-site inventory + localeRegistry |
| **I18N-C4** | Existing `api.error` catalogs under `i18n/catalogs/` remain valid; do not regress `apiErrorCatalog.test.ts`. Further peels may mirror that pattern. | Existing catalogs |
| **I18N-C5** | **`delivery_lane: light`** — E1–E5 eligible when keys+values+UI journeys unchanged. Stages **5–7** / **10** recorded **N/A**. FE gates still mandatory. Mid-flight key/value/UI drift → **upgrade to `full`**. | lightweight-delivery-lane |
| **I18N-C6** | TDD: keep `localeRegistry.test.ts` / terminology / apiError tests green; add/extend failing key-set / value-parity / soft-size characterization tests **before** or with the extract as needed. | tdd-bdd constitution |
| **I18N-C7** | English-first preserved: add/edit English base first; `zh-CN` mirrors structure. | i18n-english-first skill |
| **I18N-C8** | Veto merge with **#169** mega-test and backend-domain peels. | Batch Recommendation |
| **I18N-C9** | TM provisional wording “expect full delivery lane” is **superseded** by this BDD when structure-only invariants hold. | Stage-1 classification |

---

## 2. Actor / role

| Actor | Role | Notes |
| --- | --- | --- |
| **frontend-engineer** | Implements domain split + TDD regression | Works only in feature worktree |
| **code-quality-reviewer** | Soft-budget acceptance | Warn/critical bands; no invented harder CI |
| **Management UI user** | Observes unchanged labels/messages | Regression actor — not a journey change |
| **Parent / delivery-orchestrator** | Lane + stage N/A honesty | `light` only while E1–E5 hold |
| **Backend / OpenAPI** | **Out of scope** | No BE bundle or contract change |

---

## 3. Goal

1. After peel, every pre-peel dotted leaf key in `en` still resolves to the **same** English string; every mirrored `zh-CN` key still resolves to the **same** Chinese string (allowlisted parity exceptions unchanged).
2. Public import paths `@/i18n/locales/en` and `@/i18n/locales/zh-CN` continue to work for `index.ts`, `localeRegistry`, and existing tests/call sites with **zero or minimal** import churn.
3. Domain modules (and facades) meet soft budgets in I18N-C2.
4. Existing i18n unit tests stay green; FE lint / type-check / test / build green.
5. No Playwright / Docker evidence required while light eligibility holds.

---

## 4. Preconditions

- AI-SCALE Leaf 1 (**#166**) and Leaf 2 (**#167**) Done; soft budgets + light lane documented.
- Isolated worktree `D:/working/DGE-ai-scale-peel-i18n` on `feat/ai-scale-peel-i18n` @ `757338d8` (or later feat tip).
- Sole-active serial lane available for this leaf (activation by plan-orchestrator).
- Confirmed scope: structure-only; no intentional copy change.

---

## 5. Trigger

1. Deliver activation of TM **#168** / slice `ai-scale-peel-i18n`.
2. Implementer starts TDD Red tests that lock leaf-key set + value parity (and/or soft-size acceptance).
3. Structural extract of `en.ts` / `zh-CN.ts` into domain modules with facade compose.

---

## 6. Primary journey

1. Author/confirm this BDD (`ready`).
2. Plan-orchestrator activates sole-active leaf + detail tasks.
3. Frontend-engineer captures baseline key/value fingerprints; writes failing size/parity characterization tests as needed.
4. Extract domain modules; keep thin facades at `locales/en.ts` and `locales/zh-CN.ts` (or equivalent path-preserving entry).
5. Re-run i18n unit tests + full FE gates (`lint`, `type-check`, `test`, `build`).
6. Record stages **5–7** / **10** as **N/A** with light-lane rationale (or upgrade to `full` if surfaces drifted).
7. Architecture/CQ review as scheduled; merge; MAIN doc-sync.

---

## 7. System responses (success path)

| Step | System response |
| --- | --- |
| Message resolve after peel | Same dotted key → same string for `en` and `zh-CN` |
| Locale switch | `ensureLocaleMessages` / registry still loads `zh-CN` additively with `en` fallback |
| Soft budgets | Domain files ≤500 soft (further peel if >800); facade thin |
| Call sites | Existing `t('…')` keys and `@/i18n/locales/*` imports keep working |
| Gates | FE lint / type-check / test / build **GREEN** |
| Stages 5–7 / 10 | Honest **N/A** under `delivery_lane: light` |

---

## 8. Boundary / exception behavior

| Case | Expected |
| --- | --- |
| Accidental key rename or value drift found by Red tests | **Stop peel**; restore keys/values before Done |
| New domain file >500 soft / approaching >800 | Further split before Done (or approved split plan) |
| Public facade path broken | Fix facade; do not mass-edit call sites unless unavoidable and evidenced |
| Intentional copy/UX change sneaks in | **Out of scope** — revert **or** reclassify leaf to `delivery_lane: full` and run E2E/UIUX/deploy |
| Backend `messages_*.properties` change | **Forbidden** in this leaf |
| Fold #169 / backend peels | **Forbidden** |
| Desire to skip FE gates because light | **Forbidden** — light skips E2E/Docker only |

---

## 9. Observable evidence

| Evidence | Form |
| --- | --- |
| Keys preserved | Unit test: `collectLeafKeys(en)` equals pre-peel baseline (or post-compose equals characterization snapshot) |
| Values preserved | Unit test: for each leaf key, `resolveLeafValue` matches baseline for `en` and `zh-CN` |
| en↔zh-CN parity | Existing `localeRegistry.test.ts` parity (allowlist unchanged) **GREEN** |
| api.error intact | `apiErrorCatalog.test.ts` **GREEN** |
| Soft budgets | LOC measurement on domain modules + facades vs I18N-C2 |
| Call-site stability | Diff shows no mass `t('…')` key renames; facade path retained |
| Gates | `pnpm -C frontend lint` · `type-check` · `test` · `build` **GREEN** |
| Stages 5–7 / 10 | Honest **N/A** + E1–E5 rationale, **or** executed if upgraded to `full` |

---

## 10. Acceptance scenarios (Given / When / Then)

### I18N-01 — Public locale module API unchanged

**Given** call sites and loaders import `@/i18n/locales/en` and `@/i18n/locales/zh-CN`  
**And** the locale catalogs have been split into domain modules under this leaf  
**When** those modules are imported after the peel  
**Then** each still exposes a default nested message object usable by vue-i18n  
**And** `frontend/src/i18n/index.ts` and `localeRegistry.ts` continue to load messages without requiring a new public entry path  
**And** call-site import churn is zero or limited to unavoidable mechanical fixes evidenced in the change set.

### I18N-02 — Dotted leaf key set preserved

**Given** a pre-peel baseline of `collectLeafKeys(en)` (and mirrored `zh-CN` structure subject to existing allowlist)  
**When** the domain split is complete  
**Then** the post-peel English leaf key set equals the baseline  
**And** no existing dotted key is renamed, removed, or nested under a different path  
**And** no new keys are required for this leaf’s Done claim (additive keys for unrelated features are out of scope).

### I18N-03 — String values / meaning unchanged

**Given** the same pre-peel baseline of leaf key → string value maps for `en` and `zh-CN`  
**When** messages are resolved after the peel (direct object resolve and/or `i18n.global.t`)  
**Then** every baseline key yields the identical string value  
**And** there is no intentional copy rewrite, terminology change, or punctuation-only “cleanup” in this leaf.

### I18N-04 — Domain modules under soft size budgets

**Given** the post-peel set of manually maintained locale domain modules and facade files  
**When** agents or `code-quality-reviewer` measure LOC  
**Then** each domain module prefers ≤ **500** LOC soft target  
**And** any file approaching or exceeding the **800** hard-band has an explicit further-split plan before Done  
**And** facade aggregators remain thin compose/re-export modules  
**And** no stricter hard CI gate than the quality-gate baseline is invented.

### I18N-05 — Existing i18n regression suites stay green

**Given** `localeRegistry.test.ts`, `apiErrorCatalog.test.ts`, `sysNormWave8Terminology.test.ts`, `documentAuthorL1Labels.test.ts`, and other tests importing locale catalogs  
**When** the peel is complete  
**Then** those suites remain **GREEN** without weakening assertions  
**And** `zh-CN` parity allowlist behavior is unchanged unless a separately confirmed product decision says otherwise (not this leaf).

### I18N-06 — Frontend quality gates green (light still requires FE gates)

**Given** frontend TypeScript/Vue sources under `frontend/src/i18n/**` changed  
**When** implementers finish the peel  
**Then** `pnpm -C frontend lint`, `type-check`, `test`, and `build` all pass  
**And** light lane is **not** used as a reason to skip those gates.

### I18N-07 — English-first + locale registry behavior preserved

**Given** English is the default/base locale and `zh-CN` is lazy-loaded via `LOCALE_REGISTRY`  
**When** the app initializes and a user switches to `zh-CN` after the peel  
**Then** `DEFAULT_LOCALE` remains `en`, fallback remains English  
**And** `ensureLocaleMessages` still merges the `zh-CN` bundle additively  
**And** locale persistence key `docgen.app.locale` and registry codes (`en` \| `zh-CN`) are unchanged.

### I18N-08 — Light-lane honesty; upgrade on surface creep

**Given** this leaf claims `delivery_lane: light` with `frontend_ui_in_scope=false`  
**When** Done evidence is prepared  
**Then** stages **5–7** and **10** are recorded **N/A** with E1–E5 rationale  
**And** if any user-visible string path, copy meaning, management-UI journey, or runtime API/OpenAPI surface changed, the leaf is upgraded to **`full`** and previously skipped stages are executed before Done  
**And** Batch Recommendation `solo` alone is not cited as light eligibility.

### I18N-09 — Fail-closed scope (no BE / no #169 fold)

**Given** pressure to “also fix” backend bundles, mega-test fixtures, or unrelated UI copy  
**When** scoping this leaf  
**Then** those changes remain **out of scope**  
**And** `#169` stays queued; backend-domain work is not folded into this change set.

---

## 11. Delivery lane classification (E1–E5)

| # | Criterion | This leaf |
| --- | --- | --- |
| **E1** | No management-UI journey / visual acceptance change | **Hold** — `frontend_ui_in_scope=false`; identical keys/values ⇒ no user-visible string-path change |
| **E2** | No runtime / OpenAPI / Flyway / generation acceptance change (or pure docs) | **Hold** — FE catalog structure only; no OpenAPI/Flyway/generation; resolved runtime messages unchanged by invariant |
| **E3** | Stage 1 readiness `ready` or `not-applicable` | **Hold** — `bdd_readiness: ready` |
| **E4** | Handoff records `delivery_lane: light` + E1–E3 rationale | **Hold** — this document + orchestrator handoff |
| **E5** | Worktree still obeyed for multi-file delivery | **Hold** (during delivery) — worktree `DGE-ai-scale-peel-i18n` / `feat/ai-scale-peel-i18n`; **REMOVED** after merge `ce47186a` |

**Verdict:** `delivery_lane: light`. Stages **5–7** / **10** → **N/A**. FE unit/gates → **required**.

---

## 12. Traceability

| Artifact | Role |
| --- | --- |
| This file | Leaf behavior SoT (peel + preservation) |
| [lightweight-delivery-lane.md](./lightweight-delivery-lane.md) | Light eligibility E1–E5 |
| [ai-scale-docs-conventions.md](./ai-scale-docs-conventions.md) | Soft budgets / peel-queue conventions |
| [quality-gate-threshold-baseline.md](../architecture/quality-gate-threshold-baseline.md) | Soft/hard size SoT |
| [i18n-english-first SKILL](../../.cursor/skills/i18n-english-first/SKILL.md) | English-first / key stability / facade vs domain edit paths |
| [module-map.md](../architecture/module-map.md) | Agent retrieval — `frontend/src/i18n` facades + `locales/domains/*` |
| [ai-scale-remediation-program-2026-07.md](../plan/ai-scale-remediation-program-2026-07.md) | Program registry Leaf 3 |
| [detail/ai-scale-i18n-locale-split.md](../plan/detail/ai-scale-i18n-locale-split.md) | Plan detail / task rows |
| `.taskmaster/tasks/tasks.json` **#168** | Task Master queue head |
| Existing tests under `frontend/src/i18n/**` | Characterization / regression harness |

---

## 13. Open questions

**None blocking.** Domain module directory layout (exact folder naming) is an implementer detail under I18N-C3; not an acceptance fork.

---

## 14. Out of scope (explicit)

- Intentional copy / terminology / UX string edits
- Renaming `t('…')` key paths across Vue call sites
- Playwright E2E / UIUX (unless upgraded to `full`)
- Queued Docker deploy evidence (unless upgraded to `full`)
- Backend `messages_*.properties` or API `messageKey` changes
- Mega-test fixture split (**#169**)
- New locales beyond `en` / `zh-CN`
- Claiming umbrella **#53** / **#106** Done; flipping **#3b** / **#5a**; activating **#119** / CE-O02
