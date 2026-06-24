import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import en from '@/i18n/locales/en'
import {
  ensureLocaleMessages,
  LOCALE_REGISTRY,
  resolveAppLocale,
} from '@/i18n/localeRegistry'

describe('locale registry', () => {
  it('registers supported locales', () => {
    expect(LOCALE_REGISTRY.map((entry) => entry.code)).toEqual(['en', 'zh-CN'])
  })

  it('falls back to en for unsupported locale input', () => {
    expect(resolveAppLocale('fr')).toBe('en')
    expect(resolveAppLocale('zh-CN')).toBe('zh-CN')
  })

  it('loads zh-CN bundle additively with en fallback', async () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      fallbackLocale: 'en',
      messages: { en },
    })

    await ensureLocaleMessages(i18n, 'zh-CN')
    ;(i18n.global.locale as unknown as { value: string }).value = 'zh-CN'

    expect(i18n.global.t('app.title')).toBe('文档生成系统')
    expect(i18n.global.t('templates.list.title')).toBe('Templates')
  })
})
