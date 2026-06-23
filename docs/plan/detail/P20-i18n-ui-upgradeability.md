# P20 — i18n Multi-Locale Readiness & UI Upgradeability (Detailed Plan)

**Phase status:** Not Started | **Depends on:** P1, E06 (management shell)
**Confirmed:** 2026-06-23 (deep-review gap G6 — i18n constitution + UXF1/UXF4/UXF5)

> Single-active-phase invariant: P13 completed **Done** (2026-06-23); there is currently no
> single active phase. P20 stays `Not Started` until selected as the next active phase.

## Source-of-truth & traceability

- `.cursor/rules/i18n-english-first-constitution.mdc` — English-first, additive locales,
  fallback to `en`, locale switching must not change information architecture/layout.
- requirements-plan / PRD §6.1 (双品牌主题 REDBC/GREENBC、品牌资产槽位、英文优先).
- PRD §11 (`/api/{environment}/v1` 环境前缀) — environment is a first-class concept.
- Backlog origin: `ux-upgradeability-optimization-plan.md` UXF1 / UXF4 / UXF5 / findings X1, X4, X5.

## Gap evidence

- `i18n/index.ts` registers **only `en`**; no locale switcher, no `html lang` wiring, no
  additive-bundle fallback; several hardcoded strings remain (X1 / UXE2).
- Brand presets live in `theme/tokens.ts` (code-bound); adding a third brand needs code (X4).
- Environment is hardcoded `"dev"` in the caller-contract panel; no environment selector (X5).

## Behavior goal

Make the platform cheap to extend along locale, brand, and environment axes without code
churn, honoring the English-first constitution and the unchanged visual baseline (white
background, classic-OA desktop-first layout, dual-brand theming):

- A locale registry supports additive bundles with `en` as the source-of-truth fallback;
  a locale switcher in the shell wires `html lang` and locale-aware date/number formatting;
  adding a locale = drop in a bundle, no component edits.
- Brand themes (incl. logo asset slots) are config/data-driven; adding a brand = config entry
  + approved assets, not code.
- Environment is selected from an allowed list (not hardcoded) for contract/runtime views and
  future export/import.

### Acceptance scenarios

- **Given** the `en` base bundle, **When** a new locale bundle is added (missing some keys),
  **Then** present keys render in the new locale and missing keys fall back to `en`; no layout
  or information-architecture change on switch.
- **Given** the shell, **When** the locale is switched, **Then** `html lang` updates and
  dates/numbers format per locale; all user-facing strings come from keys (no literals).
- **Given** a third brand config entry + approved assets, **When** the brand is selected,
  **Then** theme tokens + logo switch with no code change; aspect ratio preserved, white-bg
  placement honored.
- **Given** the contract/runtime view, **When** an environment is chosen from the allowed
  list, **Then** the view targets that environment; no hardcoded `dev`.

## Tasks

| ID | Task | Status |
| --- | --- | --- |
| P20-T01 | Locale registry + additive bundle loader + `en` fallback strategy | Not Started |
| P20-T02 | Locale switcher in shell + `html lang` wiring + locale-aware date/number formatting | Not Started |
| P20-T03 | Sweep + i18n-ize remaining hardcoded strings (login, shell aria, export filenames, status enums) | Not Started |
| P20-T04 | Config/data-driven brand theming (tokens + logo asset slots) — add-a-brand without code | Not Started |
| P20-T05 | Environment selector from an allowed list for contract/runtime views (remove hardcoded `dev`) | Not Started |
| P20-T06 | Frontend message catalog aligned with backend `api.error.*` messageKeys + fallback | Not Started |
| P20-T07 | Tests: fallback behavior, switcher, brand config, environment selection; frontend coverage gate | Not Started |

## Exit criteria (phase)

- Adding a locale = drop-in bundle with `en` fallback; switcher + `html lang` + locale-aware
  formatting; no hardcoded user-facing strings.
- Brand theming + logo slots config-driven; environment selectable from an allowed list.
- Visual baseline and information architecture unchanged across locale/brand switches.
- Green gates: `pnpm -C frontend lint && type-check && test && build` (+ backend `mvn verify`
  if backend message keys touched).
- i18n constitution + PRD §6.1 cross-links verified; ledger evidence recorded.

---

## Task-level behavior specs & test-first plan (2026-06-23)

> Anchored to current code. Mostly frontend; activation requires P13 Done +
> plan-orchestrator selection (single-active-phase). Each task: failing test first
> (Vitest) → smallest implementation → refactor → doc sync.

### Current-state anchors (verified)

- `frontend/src/i18n/index.ts` — registers **only `en`** (`locale:'en'`,
  `fallbackLocale:'en'`); no locale registry, no additive-bundle loader, no switcher, no
  `html lang` wiring beyond default.
- `frontend/src/theme/tokens.ts` — `BrandPreset` is a hardcoded union
  `'REDBC' | 'GREENBC'`; `BRAND_THEMES` is a hardcoded `Record`; `applyBrandTheme` sets CSS
  vars. Adding a brand = editing the union + record (code). `logoSlotLabel` is a label, not a
  wired asset slot.
- Environment hardcoded `dev`: backend `ApiManagementController` `@RequestParam(defaultValue
  = "dev")` + frontend caller-contract panel.

### P20-T01 — Locale registry + additive bundle loader + `en` fallback

- **Behavior:** A registry lists available locales; bundles load additively; `en` is the
  source-of-truth fallback for any missing key. Adding a locale = register + drop a bundle, no
  component edits.
- **Failing tests first** (`i18n.registry.test.ts`):
  1. `missingKeyInNewLocale_fallsBackToEn`
  2. `presentKeyInNewLocale_rendersLocaleValue`
  3. `registry_listsAllAvailableLocales`
- **Impl:** registry module + loader; refactor `i18n/index.ts` to consume it.

### P20-T02 — Locale switcher + `html lang` + locale-aware date/number

- **Behavior:** Shell locale switcher changes active locale, updates `document.documentElement.lang`,
  and date/number formatting follows the active locale. Switching does not change layout or
  information architecture.
- **Failing tests first**:
  1. `switchLocale_updatesHtmlLang`
  2. `switchLocale_doesNotChangeNavStructure`
  3. `dates_formatPerActiveLocale`
- **Impl:** switcher component in `ManagementShell`; `lang` wiring; locale-aware formatters.

### P20-T03 — i18n-ize remaining hardcoded strings

- **Behavior:** Sweep `LoginView` placeholder/aria, `ManagementShell` aria, export filenames,
  raw status enums → message keys; status enums mapped to localized labels.
- **Failing tests first**: assert no literal user-facing strings in the swept components
  (key-based render); status enum → localized label mapping test.
- **Impl:** add keys to `en.ts`; replace literals.

### P20-T04 — Config/data-driven brand theming + logo asset slots

- **Behavior:** Brands defined in config/data (not a hardcoded union); adding a brand = config
  entry + approved logo assets wired to a brand asset slot; aspect ratio preserved, white-bg
  placement honored; theme + logo switch together; no layout change.
- **Failing tests first** (`theme.brandRegistry.test.ts`):
  1. `addBrand_viaConfig_appliesTokens_withoutCodeUnionEdit`
  2. `switchBrand_swapsLogoAsset`
  3. `unknownBrand_fallsBackToDefault`
- **Impl:** brand registry (config-driven) replacing the hardcoded union/record; logo slot
  binding; keep REDBC/GREENBC as seed config.

### P20-T05 — Environment selector from an allowed list

- **Behavior:** Environment chosen from an allowed list (not hardcoded `dev`) for
  contract/runtime views and future export/import; selection drives the contract request.
- **Failing tests first**:
  1. `contractPanel_usesSelectedEnvironment_notHardcodedDev`
  2. `environmentList_comesFromAllowedConfig`
- **Impl:** environment selector component + allowed-list source; thread through contract API
  calls; (backend `defaultValue="dev"` remains a safe default but UI passes explicit env).

### P20-T06 — Frontend catalog aligned with backend `api.error.*`

- **Behavior:** Frontend message catalog covers backend `api.error.*` messageKeys with a
  documented fallback strategy (unknown key → safe generic English).
- **Failing tests first**:
  1. `knownApiErrorKey_resolvesToLocalizedMessage`
  2. `unknownApiErrorKey_fallsBackToGeneric`
- **Impl:** extend `en.ts` `api.error.*`; central resolver.

### P20-T07 — Tests + frontend coverage gate

- **Behavior:** Add/extend tests for fallback, switcher, brand config, environment selection;
  ensure the frontend coverage gate (optimization-plan B3) covers new modules.
- **Failing tests first:** the above suites must be red before implementation.
- **Impl:** wire Vitest coverage thresholds if not yet present (coordinate with OPT-B3).

### Migration & sequencing

- No DB migration (frontend-centric). If backend message keys are touched (T06), run
  `mvn verify` too.
- Suggested order: T01 → T02 → T03 → T06 → T04 → T05 → T07.
- Visual baseline (white bg, classic-OA desktop-first, dual-brand) and information
  architecture must remain unchanged across locale/brand switches (PRD §6.1).
