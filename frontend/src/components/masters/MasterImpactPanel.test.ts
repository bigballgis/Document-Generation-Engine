import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import MasterImpactPanel from '@/components/masters/MasterImpactPanel.vue'
import en from '@/i18n/locales/en'
import type { MasterImpactAnalysis } from '@/types/master'

function mountPanel(impact: MasterImpactAnalysis | null) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })
  return mount(MasterImpactPanel, {
    props: { impact },
    global: {
      plugins: [i18n, ElementPlus],
      stubs: {
        'router-link': {
          props: ['to'],
          template: '<a :href="typeof to === \'string\' ? to : to.path || to" data-testid="master-impact-template-link"><slot /></a>',
        },
      },
    },
  })
}

describe('MasterImpactPanel', () => {
  it('MIR-009 — renders name links and hides empty state when referencedTemplates present', async () => {
    const wrapper = mountPanel({
      masterId: 'master-1',
      referencedTemplateIds: ['tpl-1', 'tpl-2'],
      referencedTemplates: [
        { templateId: 'tpl-1', name: 'Loan Contract', lifecycleStatus: 'DRAFT' },
        { templateId: 'tpl-2', name: 'Credit Notice', lifecycleStatus: 'PUBLISHED' },
      ],
      retestRequired: true,
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="master-impact-empty"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Loan Contract')
    expect(wrapper.text()).toContain('Credit Notice')
    expect(wrapper.text()).not.toMatch(/No templates currently reference/i)
    const links = wrapper.findAll('[data-testid="master-impact-template-link"]')
    expect(links).toHaveLength(2)
    expect(links[0].text()).toBe('Loan Contract')
    expect(links[0].attributes('href')).toContain('/templates/tpl-1')
  })

  it('shows honest empty state only when API returns no references', async () => {
    const wrapper = mountPanel({
      masterId: 'master-1',
      referencedTemplateIds: [],
      referencedTemplates: [],
      retestRequired: false,
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="master-impact-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toMatch(/No templates currently reference/i)
  })
})
