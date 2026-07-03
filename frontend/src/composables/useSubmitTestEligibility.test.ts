import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { defineComponent } from 'vue'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { SubmitTestEligibility } from '@/types/template'
import { useSubmitTestEligibility } from '@/composables/useSubmitTestEligibility'

vi.mock('@/api/templates', () => ({
  getSubmitTestEligibility: vi.fn(),
}))

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

function makeWrapperWithData(templateId: string) {
  const Comp = defineComponent({
    setup() {
      const elig = useSubmitTestEligibility(templateId)
      return { elig }
    },
    template: '<div></div>',
  })

  return mount(Comp, {
    global: { plugins: [makeI18n(), ElementPlus] },
  })
}

describe('useSubmitTestEligibility', () => {
  beforeEach(() => {
    vi.mocked(templatesApi.getSubmitTestEligibility).mockReset()
  })

  afterEach(() => {
    vi.mocked(templatesApi.getSubmitTestEligibility).mockReset()
  })

  it('returns eligible=true and empty tooltip when API returns eligible', async () => {
    const eligibleData: SubmitTestEligibility = {
      eligible: true,
      hasValidTestResult: true,
      allSamplesSucceeded: true,
      coverageGatePassed: true,
      failedDataSetNames: [],
      uncoveredAnchors: [],
      uncoveredVariables: [],
    }
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(eligibleData)

    const wrapper = makeWrapperWithData('tpl-1')
    const { elig } = wrapper.vm as unknown as { elig: ReturnType<typeof useSubmitTestEligibility> }

    await elig.refresh()
    await flushPromises()

    expect(elig.isEligible.value).toBe(true)
    expect(elig.tooltipContent.value).toBe('')
    wrapper.unmount()
  })

  it('tooltip contains no-valid-test-result message when hasValidTestResult=false', async () => {
    const data: SubmitTestEligibility = {
      eligible: false,
      hasValidTestResult: false,
      allSamplesSucceeded: false,
      coverageGatePassed: false,
      failedDataSetNames: [],
      uncoveredAnchors: [],
      uncoveredVariables: [],
    }
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(data)

    const wrapper = makeWrapperWithData('tpl-1')
    const { elig } = wrapper.vm as unknown as { elig: ReturnType<typeof useSubmitTestEligibility> }

    await elig.refresh()
    await flushPromises()

    expect(elig.isEligible.value).toBe(false)
    expect(elig.tooltipContent.value).toContain('No valid full test result')
    wrapper.unmount()
  })

  it('tooltip contains failed data set names', async () => {
    const data: SubmitTestEligibility = {
      eligible: false,
      hasValidTestResult: true,
      allSamplesSucceeded: false,
      coverageGatePassed: true,
      failedDataSetNames: ['DS-002', 'DS-005'],
      uncoveredAnchors: [],
      uncoveredVariables: [],
    }
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(data)

    const wrapper = makeWrapperWithData('tpl-1')
    const { elig } = wrapper.vm as unknown as { elig: ReturnType<typeof useSubmitTestEligibility> }

    await elig.refresh()
    await flushPromises()

    expect(elig.tooltipContent.value).toContain('DS-002')
    expect(elig.tooltipContent.value).toContain('DS-005')
    wrapper.unmount()
  })

  it('truncates long anchor lists to 5 items with remaining count', async () => {
    const data: SubmitTestEligibility = {
      eligible: false,
      hasValidTestResult: true,
      allSamplesSucceeded: true,
      coverageGatePassed: false,
      failedDataSetNames: [],
      uncoveredAnchors: ['A1', 'A2', 'A3', 'A4', 'A5', 'A6', 'A7'],
      uncoveredVariables: [],
    }
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(data)

    const wrapper = makeWrapperWithData('tpl-1')
    const { elig } = wrapper.vm as unknown as { elig: ReturnType<typeof useSubmitTestEligibility> }

    await elig.refresh()
    await flushPromises()

    const content = elig.tooltipContent.value
    expect(content).toContain('A1')
    expect(content).toContain('A5')
    expect(content).toContain('2')
    wrapper.unmount()
  })

  it('shows uncoveredVariables in tooltip when coverage gate fails', async () => {
    const data: SubmitTestEligibility = {
      eligible: false,
      hasValidTestResult: true,
      allSamplesSucceeded: true,
      coverageGatePassed: false,
      failedDataSetNames: [],
      uncoveredAnchors: [],
      uncoveredVariables: ['customerName', 'accountNumber'],
    }
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(data)

    const wrapper = makeWrapperWithData('tpl-1')
    const { elig } = wrapper.vm as unknown as { elig: ReturnType<typeof useSubmitTestEligibility> }

    await elig.refresh()
    await flushPromises()

    const content = elig.tooltipContent.value
    expect(content).toContain('customerName')
    expect(content).toContain('accountNumber')
    wrapper.unmount()
  })
})
