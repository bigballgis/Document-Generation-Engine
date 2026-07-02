import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { describe, expect, it } from 'vitest'
import BrandLogo from '@/components/branding/BrandLogo.vue'
import en from '@/i18n/locales/en'

function mountLogo(props: { brand: 'REDBC' | 'GREENBC'; showWordmark?: boolean }) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(BrandLogo, {
    props: { showWordmark: true, ...props },
    global: { plugins: [i18n] },
  })
}

describe('BrandLogo', () => {
  it('shows i18n business name instead of internal brand code for REDBC', () => {
    const wrapper = mountLogo({ brand: 'REDBC' })
    expect(wrapper.find('.brand-logo__wordmark').text()).toBe('Red Bank')
    expect(wrapper.text()).not.toContain('REDBC')
  })

  it('shows i18n business name instead of internal brand code for GREENBC', () => {
    const wrapper = mountLogo({ brand: 'GREENBC' })
    expect(wrapper.find('.brand-logo__wordmark').text()).toBe('Green Bank')
    expect(wrapper.text()).not.toContain('GREENBC')
  })
})
