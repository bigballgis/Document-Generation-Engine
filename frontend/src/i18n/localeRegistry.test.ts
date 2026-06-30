import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import { collectLeafKeys, resolveLeafValue } from '@/i18n/collectLeafKeys'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'
import {
  ensureLocaleMessages,
  LOCALE_REGISTRY,
  resolveAppLocale,
} from '@/i18n/localeRegistry'

/** Keys intentionally English-only (L3 contract surfaces). Prefer zero entries. */
const ZH_CN_PARITY_ALLOWLIST = new Set<string>([])

describe('locale registry', () => {
  it('registers supported locales', () => {
    expect(LOCALE_REGISTRY.map((entry) => entry.code)).toEqual(['en', 'zh-CN'])
  })

  it('falls back to en for unsupported locale input', () => {
    expect(resolveAppLocale('fr')).toBe('en')
    expect(resolveAppLocale('zh-CN')).toBe('zh-CN')
  })

  it('collectLeafKeys returns dotted paths for string leaves', () => {
    const keys = collectLeafKeys({
      app: { title: 'App' },
      nested: { child: { label: 'Child' } },
      skip: 42,
    })

    expect(keys).toEqual(['app.title', 'nested.child.label'])
  })

  it('zh-CN contains every en leaf key with a non-empty string value', () => {
    const enKeys = collectLeafKeys(en as Record<string, unknown>)
    const missing: string[] = []
    const empty: string[] = []

    for (const key of enKeys) {
      if (ZH_CN_PARITY_ALLOWLIST.has(key)) {
        continue
      }

      const value = resolveLeafValue(zhCN as Record<string, unknown>, key)

      if (typeof value !== 'string') {
        missing.push(key)
        continue
      }

      if (value.trim().length === 0) {
        empty.push(key)
      }
    }

    expect(
      { missing, empty },
      [
        missing.length > 0 ? `Missing zh-CN keys (${missing.length}):\n${missing.join('\n')}` : '',
        empty.length > 0 ? `Empty zh-CN values (${empty.length}):\n${empty.join('\n')}` : '',
      ]
        .filter(Boolean)
        .join('\n\n'),
    ).toEqual({ missing: [], empty: [] })
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
    expect(i18n.global.t('templates.list.title')).toBe('模板')
  })
})
