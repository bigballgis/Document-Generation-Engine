/**
 * Common BCP-47 body-language tags for template / content-module locale
 * declaration and catalog filters (IBL-E1 / ADR-0061).
 * Not UI i18n — authored document language.
 */
export const DOCUMENT_LOCALE_OPTIONS = [
  { value: 'zh-CN', labelKey: 'documentLocale.options.zhCN' },
  { value: 'en-US', labelKey: 'documentLocale.options.enUS' },
  { value: 'en-GB', labelKey: 'documentLocale.options.enGB' },
  { value: 'zh-HK', labelKey: 'documentLocale.options.zhHK' },
  { value: 'zh-TW', labelKey: 'documentLocale.options.zhTW' },
] as const

export type DocumentLocaleOption = (typeof DOCUMENT_LOCALE_OPTIONS)[number]

export const DOCUMENT_LOCALE_VALUES = DOCUMENT_LOCALE_OPTIONS.map((option) => option.value)
