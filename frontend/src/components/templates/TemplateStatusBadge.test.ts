import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import en from '@/i18n/locales/en'

describe('TemplateStatusBadge', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders lifecycle status label', () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplateStatusBadge, {
      props: { status: 'PUBLISHED' },
      global: { plugins: [i18n, ElementPlus] },
    })

    expect(wrapper.text()).toContain('Published')
  })
})
