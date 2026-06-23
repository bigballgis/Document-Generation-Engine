import { createI18n } from 'vue-i18n'
import en from './locales/en'
import { DEFAULT_LOCALE, type AppLocale } from './localeRegistry'

const initialLocale: AppLocale = DEFAULT_LOCALE

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale as string,
  fallbackLocale: 'en' as string,
  messages: {
    en,
  },
})
