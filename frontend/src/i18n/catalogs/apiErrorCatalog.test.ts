import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import { apiErrorEn } from '@/i18n/catalogs/apiErrorEn'
import { apiErrorZhCn } from '@/i18n/catalogs/apiErrorZhCn'
import en from '@/i18n/locales/en'
import zhCN from '@/i18n/locales/zh-CN'
import { ensureLocaleMessages } from '@/i18n/localeRegistry'

function collectLeafPaths(obj: Record<string, unknown>, prefix = ''): string[] {
  const paths: string[] = []
  for (const [key, value] of Object.entries(obj)) {
    const path = prefix ? `${prefix}.${key}` : key
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      paths.push(...collectLeafPaths(value as Record<string, unknown>, path))
    } else {
      paths.push(path)
    }
  }
  return paths
}

const apiErrorLeafPaths = collectLeafPaths(apiErrorEn as Record<string, unknown>)

describe('api.error catalog', () => {
  it('mirrors en and zh-CN catalog structure', () => {
    const enPaths = collectLeafPaths(apiErrorEn as Record<string, unknown>)
    const zhPaths = collectLeafPaths(apiErrorZhCn as Record<string, unknown>)
    expect(zhPaths).toEqual(enPaths)
  })

  it('resolves every catalog key in en and zh-CN locales', async () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      fallbackLocale: 'en',
      messages: { en },
    })
    await ensureLocaleMessages(i18n, 'zh-CN')

    const localeRef = i18n.global.locale as unknown as { value: string }

    for (const path of apiErrorLeafPaths) {
      const key = `api.error.${path}`
      localeRef.value = 'en'
      const enMessage = i18n.global.t(key)
      localeRef.value = 'zh-CN'
      const zhMessage = i18n.global.t(key)

      expect(enMessage).not.toBe(key)
      expect(zhMessage).not.toBe(key)
      expect(enMessage.length).toBeGreaterThan(0)
      expect(zhMessage.length).toBeGreaterThan(0)
      expect(zhMessage).not.toBe(enMessage)
    }
  })

  it('zh-CN bundle includes merged api.error catalog', () => {
    expect(zhCN.api?.error).toBe(apiErrorZhCn)
  })
})
