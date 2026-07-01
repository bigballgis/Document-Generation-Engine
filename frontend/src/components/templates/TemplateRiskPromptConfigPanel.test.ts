import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateRiskPromptConfigPanel from '@/components/templates/TemplateRiskPromptConfigPanel.vue'
import en from '@/i18n/locales/en'

vi.mock('@/api/riskPromptConfig', () => ({
  getGlobalRiskPromptConfig: vi.fn().mockResolvedValue({
    scopeType: 'GLOBAL',
    groupCode: null,
    reasonCategories: ['BINDING_ISSUE', 'FIDELITY_WARNING'],
    riskPromptCopy: {},
    updatedAt: '2026-07-02T00:00:00Z',
  }),
}))

describe('TemplateRiskPromptConfigPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function mountPanel(props: Record<string, unknown> = {}) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(TemplateRiskPromptConfigPanel, {
      props: {
        createMode: true,
        showSave: false,
        ...props,
      },
      global: { plugins: [i18n, ElementPlus] },
    })
  }

  it('shows inherit hint when not customizing', async () => {
    const wrapper = mountPanel()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Using system default')
    })
  })

  it('does not expose group scope controls', async () => {
    const wrapper = mountPanel()
    await vi.waitFor(() => {
      expect(wrapper.text()).toContain('Test and approval return reasons')
    })
    expect(wrapper.text()).not.toContain('Group override')
  })
})
