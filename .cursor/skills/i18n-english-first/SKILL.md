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

- Use the i18n message catalog; `en` is the default locale and the source of truth.
- Add the English key first; components reference keys, never literals.
- Locale switching must not change information architecture, layout, or component structure.

## Default workflow when adding a string

```
- [ ] 1. Add the English key to the base bundle (backend properties / frontend en catalog)
- [ ] 2. Reference the key from code (never the literal)
- [ ] 3. Add other locales only if requested; English remains the fallback
```
