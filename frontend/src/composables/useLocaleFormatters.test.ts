import { defineComponent } from 'vue'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import en from '@/i18n/locales/en'

function runFormatters(locale: string) {
  const i18n = createI18n({
    legacy: false,
    locale,
    messages: { en },
  })
  let formatters: ReturnType<typeof useLocaleFormatters> | undefined

  mount(
    defineComponent({
      setup() {
        formatters = useLocaleFormatters()
        return () => null
      },
    }),
    {
      global: {
        plugins: [i18n],
      },
    },
  )

  return formatters!
}

describe('useLocaleFormatters', () => {
  it('formats dates using the active locale', () => {
    const enFormatters = runFormatters('en')
    const zhFormatters = runFormatters('zh-CN')
    const value = '2026-06-23T10:00:00Z'

    expect(enFormatters.formatDateTime(value)).toContain('2026')
    expect(zhFormatters.formatDateTime(value)).toContain('2026')
    expect(enFormatters.formatNumber(12345.6)).toBe('12,345.6')
  })
})
