import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TemplateCoveragePanel from '@/components/templates/TemplateCoveragePanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { CoverageSummary, SubmitTestEligibility } from '@/types/template'

vi.mock('@/api/templates', () => ({
  getTemplateCoverage: vi.fn(),
  getSubmitTestEligibility: vi.fn(),
}))

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

const mockCoverage: CoverageSummary = {
  templateId: 'tpl-1',
  aggregatePercentage: 85,
  belowThreshold: false,
  blockerCodes: [],
  dimensions: [
    {
      dimensionCode: 'REQUIRED_VARIABLES',
      totalCount: 20,
      exercisedCount: 17,
      percentage: 85,
      thresholdPercentage: 80,
      belowThreshold: false,
    },
    {
      dimensionCode: 'REQUIRED_SAMPLES',
      totalCount: 5,
      exercisedCount: 5,
      percentage: 100,
      thresholdPercentage: 100,
      belowThreshold: false,
    },
    {
      dimensionCode: 'ANCHOR_BINDINGS',
      totalCount: 3,
      exercisedCount: 3,
      percentage: 100,
      thresholdPercentage: 80,
      belowThreshold: false,
    },
  ],
  appliedThreshold: {
    scopeType: 'GLOBAL',
    groupCode: null,
    minRequiredVariablePct: 80,
    minRequiredSamplePct: 100,
    minAnchorBindingPct: 80,
  },
}

const mockEligibility: SubmitTestEligibility = {
  eligible: false,
  hasValidTestResult: true,
  allSamplesSucceeded: true,
  coverageGatePassed: false,
  failedDataSetNames: [],
  uncoveredAnchors: ['ANCHOR_SEAL'],
  uncoveredVariables: ['customerName', 'accountNumber'],
}

describe('TemplateCoveragePanel (T12 enhancement)', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getTemplateCoverage).mockReset()
    vi.mocked(templatesApi.getSubmitTestEligibility).mockReset()
  })

  it('shows dimension table with threshold columns', async () => {
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue(mockCoverage)
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue({
      ...mockEligibility,
      coverageGatePassed: true,
      uncoveredAnchors: [],
      uncoveredVariables: [],
    })

    const i18n = makeI18n()
    const wrapper = mount(TemplateCoveragePanel, {
      props: { templateId: 'tpl-1', compact: true },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(document.body.textContent).toContain('85')
    expect(document.body.textContent).toContain('80')
    expect(document.body.textContent).toContain('Meets')
    expect(document.body.textContent).toContain('Applied threshold')

    wrapper.unmount()
  })

  it('shows uncovered anchors section when eligibility has uncoveredAnchors', async () => {
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({
      ...mockCoverage,
      belowThreshold: true,
    })
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(mockEligibility)

    const i18n = makeI18n()
    const wrapper = mount(TemplateCoveragePanel, {
      props: { templateId: 'tpl-1', compact: true },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(document.body.textContent).toContain('Uncovered placeholders')
    expect(document.body.textContent).toContain('ANCHOR_SEAL')

    wrapper.unmount()
  })

  it('shows uncovered variables section when eligibility has uncoveredVariables', async () => {
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue({
      ...mockCoverage,
      belowThreshold: true,
    })
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue(mockEligibility)

    const i18n = makeI18n()
    const wrapper = mount(TemplateCoveragePanel, {
      props: { templateId: 'tpl-1', compact: true },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(document.body.textContent).toContain('Uncovered variables')
    expect(document.body.textContent).toContain('customerName')

    wrapper.unmount()
  })

  it('refreshes when refreshToken changes', async () => {
    vi.mocked(templatesApi.getTemplateCoverage).mockResolvedValue(mockCoverage)
    vi.mocked(templatesApi.getSubmitTestEligibility).mockResolvedValue({
      ...mockEligibility,
      eligible: true,
      coverageGatePassed: true,
    })

    const i18n = makeI18n()
    const wrapper = mount(TemplateCoveragePanel, {
      props: { templateId: 'tpl-1', compact: true, refreshToken: 0 },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(vi.mocked(templatesApi.getTemplateCoverage)).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ refreshToken: 1 })
    await flushPromises()

    expect(vi.mocked(templatesApi.getTemplateCoverage)).toHaveBeenCalledTimes(2)

    wrapper.unmount()
  })
})
