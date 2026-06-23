type LocaleMessages = Record<string, unknown>
type LocaleAwareI18n = {
  global: {
    mergeLocaleMessage: (locale: string, messages: unknown) => void
  }
}

export const APP_LOCALE_STORAGE_KEY = 'docgen.app.locale'

export interface LocaleRegistryEntry {
  code: 'en' | 'zh-CN'
  labelKey: string
  loadMessages: () => Promise<LocaleMessages>
}

export const LOCALE_REGISTRY = [
  {
    code: 'en',
    labelKey: 'common.locales.en',
    loadMessages: async () => (await import('@/i18n/locales/en')).default as LocaleMessages,
  },
  {
    code: 'zh-CN',
    labelKey: 'common.locales.zhCN',
    loadMessages: async () => (await import('@/i18n/locales/zh-CN')).default as LocaleMessages,
  },
] as const satisfies readonly LocaleRegistryEntry[]

export type AppLocale = (typeof LOCALE_REGISTRY)[number]['code']

export const DEFAULT_LOCALE: AppLocale = 'en'

const LOCALE_CODES = new Set<string>(LOCALE_REGISTRY.map((entry) => entry.code))
const loadedLocales = new Set<AppLocale>(['en'])
const localeLoaders: Record<AppLocale, () => Promise<LocaleMessages>> = {
  en: LOCALE_REGISTRY[0].loadMessages,
  'zh-CN': LOCALE_REGISTRY[1].loadMessages,
}

export function isAppLocale(value: string): value is AppLocale {
  return LOCALE_CODES.has(value)
}

export function resolveAppLocale(value: string | null | undefined): AppLocale {
  if (value && isAppLocale(value)) {
    return value
  }
  return DEFAULT_LOCALE
}

export async function ensureLocaleMessages(
  i18n: LocaleAwareI18n,
  locale: AppLocale,
): Promise<void> {
  if (loadedLocales.has(locale)) {
    return
  }
  const messages = await localeLoaders[locale]()
  i18n.global.mergeLocaleMessage(locale, messages)
  loadedLocales.add(locale)
}
