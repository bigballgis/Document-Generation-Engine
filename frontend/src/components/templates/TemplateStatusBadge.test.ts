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

  function mountBadge(
    props: { status: 'PUBLISHED' | 'APPROVAL'; approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' },
  ) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(TemplateStatusBadge, {
      props,
      global: { plugins: [i18n, ElementPlus] },
    })
  }

  it('renders lifecycle status label', () => {
    const wrapper = mountBadge({ status: 'PUBLISHED' })
    expect(wrapper.text()).toContain('Published')
  })

  it('surfaces approval PENDING_SUBMIT substate', () => {
    const wrapper = mountBadge({
      status: 'APPROVAL',
      approvalSubState: 'PENDING_SUBMIT',
    })
    expect(wrapper.text()).toContain('Awaiting submit for approval')
  })

  it('surfaces approval PENDING_DECISION substate', () => {
    const wrapper = mountBadge({
      status: 'APPROVAL',
      approvalSubState: 'PENDING_DECISION',
    })
    expect(wrapper.text()).toContain('Awaiting approval decision')
  })
})
