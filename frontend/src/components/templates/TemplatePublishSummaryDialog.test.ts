import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it } from 'vitest'
import TemplatePublishSummaryDialog from '@/components/templates/TemplatePublishSummaryDialog.vue'
import en from '@/i18n/locales/en'

describe('TemplatePublishSummaryDialog', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountDialog(
    gateItems: Array<{ key: string; label: string; ready: boolean; informational?: boolean }>,
    options: {
      coverageBelowThreshold?: boolean
      changeDiffHasChanges?: boolean
      previewComparison?: {
        totalDiffCount: number
        blockerCount: number
        warningCount: number
      } | null
    } = {},
  ) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(TemplatePublishSummaryDialog, {
      props: {
        modelValue: true,
        templateName: 'Retail letter',
        releaseVersion: '1.0.1',
        gateItems,
        coverageSummary:
          options.coverageBelowThreshold === undefined
            ? null
            : {
                templateId: 'tpl-1',
                aggregatePercentage: options.coverageBelowThreshold ? 40 : 90,
                belowThreshold: options.coverageBelowThreshold,
                blockerCodes: [],
                dimensions: [],
                appliedThreshold: {
                  scopeType: 'GLOBAL',
                  groupCode: null,
                  minRequiredVariablePct: 80,
                  minRequiredSamplePct: 80,
                  minAnchorBindingPct: 80,
                },
              },
        changeDiffSummary:
          options.changeDiffHasChanges === undefined
            ? null
            : {
                templateId: 'tpl-1',
                baselineReleaseVersion: '1.0.0',
                candidateVersionId: 'ver-2',
                hasChanges: options.changeDiffHasChanges,
                totalChangeCount: options.changeDiffHasChanges ? 3 : 0,
                dimensions: [],
              },
        previewComparison:
          options.previewComparison === undefined
            ? null
            : options.previewComparison === null
              ? null
              : {
                  totalDiffCount: options.previewComparison.totalDiffCount,
                  blockerCount: options.previewComparison.blockerCount,
                  warningCount: options.previewComparison.warningCount,
                  items: [],
                },
      },
      attachTo: document.body,
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
  }

  it('disables confirm when required gate items are pending', async () => {
    const wrapper = mountDialog([
      { key: 'ANCHOR_INTEGRITY', label: 'Layout placeholder check', ready: false },
      { key: 'COVERAGE_THRESHOLDS', label: 'Coverage thresholds', ready: true },
    ])
    await flushPromises()

    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Confirm go-live'))

    expect(confirmButton?.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Resolve pre-release blockers')
  })

  it('shows coverage and change diff summaries when provided', async () => {
    const wrapper = mountDialog(
      [{ key: 'ANCHOR_INTEGRITY', label: 'Layout placeholder check', ready: true }],
      { coverageBelowThreshold: false, changeDiffHasChanges: true },
    )
    await flushPromises()

    expect(wrapper.text()).toContain('Coverage meets thresholds (90%)')
    expect(wrapper.text()).toContain('Change diff reports 3 change(s) from baseline.')
  })

  it('shows preview comparison summary when provided', async () => {
    const wrapper = mountDialog(
      [{ key: 'ANCHOR_INTEGRITY', label: 'Layout placeholder check', ready: true }],
      {
        previewComparison: { totalDiffCount: 2, blockerCount: 0, warningCount: 2 },
      },
    )
    await flushPromises()

    expect(wrapper.text()).toContain('Preview comparison reports 2 difference(s)')
  })

  it('enables confirm when all required gate items are ready', async () => {
    const wrapper = mountDialog([
      { key: 'ANCHOR_INTEGRITY', label: 'Layout placeholder check', ready: true },
      { key: 'COVERAGE_THRESHOLDS', label: 'Coverage thresholds', ready: true },
    ])
    await flushPromises()

    const confirmButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Confirm go-live'))

    expect(confirmButton?.attributes('disabled')).toBeUndefined()
  })
})
