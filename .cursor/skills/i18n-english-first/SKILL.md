---
name: i18n-english-first
description: Internationalization convention for this platform — the system supports multiple languages with English as the primary/base language. Use when adding or changing any user-facing string, error message, API messageKey, or UI label in backend or frontend.
---

# i18n: English-First, Multi-Language Capable

The system is multi-language capable. English is the primary and default language.
Every user-facing string is translatable; nothing is hardcoded.

## Core rules

- Never hardcode display strings in code. Always reference a message key.
- English is the base/default bundle and must always exist for every key.
- Other locale bundles are optional and additive; missing keys fall back to English.
- Keep keys stable and meaningful; do not encode runtime data into keys.

## Backend (API + errors)

- API errors return a stable `error.code`, `error.category`, `error.retryable`, an
  English `error.message`, and an `error.messageKey` for caller-side localization.
- `messageKey` naming: `api.error.<category>.<camelCaseCode>`
  (e.g. `api.error.versionRouting.defaultRouteNotConfigured`).
- The same `error.code` keeps one stable, safe English message; do not vary it per scenario.
- Fidelity warnings carry a stable `warningCode` + `messageKey`.
- Backend message bundles: `backend/src/main/resources/i18n/messages_en.properties` is the base.

## Frontend (Vue 3)

- Base bundle: `frontend/src/i18n/locales/en.ts` (~2500 lines, domain-first namespaces:
  `app`, `login`, `nav`, `templates`, `masters`, `audit`, `identity`, `contentModules`, …).
- `zh-CN.ts` is lazy-loaded and must mirror `en.ts` structure manually; only `api.error.*`
  is test-guarded (`src/i18n/catalogs/apiErrorCatalog.test.ts`).
- API error strings live in `src/i18n/catalogs/apiErrorEn.ts` / `apiErrorZhCn.ts`, merged
  under `api.error.*`.
- UI key convention: domain-first dotted paths (`templates.detail.tabs.overview`,
  `login.validation.usernameRequired`). Tab/config TS files export `*_LABEL_KEYS` maps.
- Components: `const { t } = useI18n()` then `t('key')`; error keys resolved via
  `resolveApiErrorMessageKey(error, 'fallback.key')` from `src/api/errorEnvelope.ts`.
- Add the English key first; components reference keys, never literals.
- Locale switching must not change information architecture, layout, or component structure.
- Locale persistence: localStorage `docgen.app.locale`; registry `src/i18n/localeRegistry.ts`.

## Default workflow when adding a string

```
- [ ] 1. Add the English key to the base bundle (backend properties / frontend en catalog)
- [ ] 2. Reference the key from code (never the literal)
- [ ] 3. Add other locales only if requested; English remains the fallback
```
