import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TemplateCoveragePanel from '@/components/templates/TemplateCoveragePanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  getTemplateCoverage: vi.fn(),
}))

describe('TemplateCoveragePanel', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getTemplateCoverage).mockReset()
  })

  it('renders coverage dimensions from the API summary', async () => {
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({
      templateId: 'tpl-1',
      aggregatePercentage: 85,
      belowThreshold: false,
      blockerCodes: [],
      dimensions: [
        {
          dimensionCode: 'REQUIRED_VARIABLES',
          totalCount: 10,
          exercisedCount: 9,
          percentage: 90,
          thresholdPercentage: 80,
          belowThreshold: false,
        },
      ],
      appliedThreshold: {
        scopeType: 'GLOBAL',
        groupCode: null,
        minRequiredVariablePct: 80,
        minRequiredSamplePct: 80,
        minAnchorBindingPct: 80,
      },
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplateCoveragePanel, {
      props: { templateId: 'tpl-1' },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(templatesApi.getTemplateCoverage).toHaveBeenCalledWith('tpl-1')
    expect(wrapper.text()).toContain('Required variables')
    expect(wrapper.text()).toContain('9 / 10')
    expect(wrapper.text()).toContain('Aggregate coverage meets thresholds (85%)')
  })
})
