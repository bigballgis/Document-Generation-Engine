import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import en from '@/i18n/locales/en'

function mountBadge(status: 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED') {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(MasterStatusBadge, {
    props: { status },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('MasterStatusBadge', () => {
  it('renders approved status label', () => {
    const wrapper = mountBadge('APPROVED')
    expect(wrapper.text()).toContain('Approved')
  })

  it('renders pending review status label', () => {
    const wrapper = mountBadge('PENDING_REVIEW')
    expect(wrapper.text()).toContain('Pending review')
  })
})
