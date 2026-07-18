import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessage } from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BatchTestHistoryPanel from '@/components/templates/BatchTestHistoryPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { BatchTestRunSummary } from '@/types/template'

vi.mock('@/api/templates', () => ({
  getBatchTestHistory: vi.fn(),
  listTestDataSets: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    },
  }
})

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

const mockHistory: BatchTestRunSummary[] = [
  {
    runId: 'run-100',
    createdAt: '2026-07-03T10:00:00Z',
    createdBy: 'alice',
    createdByDisplayName: 'Alice Author',
    status: 'COMPLETED',
    totalSamples: 5,
    succeededCount: 5,
    failedCount: 0,
    anchorCoveragePct: 90.0,
    variableCoveragePct: 85.0,
    sampleCoveragePct: 100.0,
    gatePassed: true,
    invalidatedAt: null,
    sampleResults: [
      { dataSetExternalId: 'EXT-OK', success: true },
      { dataSetExternalId: 'EXT-FAIL', success: false, errorDetail: 'Sample failed detail' },
    ],
  },
  {
    runId: 'run-099',
    createdAt: '2026-07-02T10:00:00Z',
    createdBy: 'alice',
    status: 'COMPLETED',
    totalSamples: 5,
    succeededCount: 4,
    failedCount: 1,
    anchorCoveragePct: 75.0,
    variableCoveragePct: 80.0,
    sampleCoveragePct: 80.0,
    gatePassed: false,
    invalidatedAt: null,
    sampleResults: [],
  },
  {
    runId: 'run-098',
    createdAt: '2026-07-01T10:00:00Z',
    createdBy: 'alice',
    status: 'COMPLETED',
    totalSamples: 3,
    succeededCount: 3,
    failedCount: 0,
    anchorCoveragePct: 100.0,
    variableCoveragePct: 100.0,
    sampleCoveragePct: 100.0,
    gatePassed: true,
    invalidatedAt: '2026-07-03T08:00:00Z',
    sampleResults: [],
  },
  {
    runId: 'run-running',
    createdAt: '2026-07-03T11:00:00Z',
    createdBy: 'alice',
    status: 'RUNNING',
    totalSamples: 2,
    succeededCount: 0,
    failedCount: 0,
    anchorCoveragePct: null,
    variableCoveragePct: null,
    sampleCoveragePct: null,
    gatePassed: null,
    invalidatedAt: null,
    sampleResults: [],
  },
]

async function mountPanel(history: BatchTestRunSummary[] = mockHistory, props: Record<string, unknown> = {}) {
  vi.mocked(templatesApi.getBatchTestHistory).mockResolvedValue(history)
  const i18n = makeI18n()
  const wrapper = mount(BatchTestHistoryPanel, {
    props: { templateId: 'tpl-1', ...props },
    global: { plugins: [createPinia(), i18n, ElementPlus] },
    attachTo: document.body,
  })
  await flushPromises()
  return wrapper
}

describe('BatchTestHistoryPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(ElMessage.warning).mockReset()
    vi.mocked(ElMessage.error).mockReset()
    vi.mocked(templatesApi.listTestDataSets).mockReset()
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getBatchTestHistory).mockReset()
  })

  it('shows empty state when no history', async () => {
    const wrapper = await mountPanel([])
    expect(document.body.textContent).toContain('No full test runs yet')
    wrapper.unmount()
  })

  it('renders history rows with correct status tags', async () => {
    const wrapper = await mountPanel()
    expect(document.body.textContent).toContain('Readiness checks passed')
    expect(document.body.textContent).toContain('Readiness checks not met')
    wrapper.unmount()
  })

  it('renders createdBy display name when available', async () => {
    const wrapper = await mountPanel()
    expect(document.body.textContent).toContain('Alice Author')
    wrapper.unmount()
  })

  it('shows INVALIDATED tag for invalidated run', async () => {
    const wrapper = await mountPanel()
    expect(document.body.textContent).toContain('Invalidated')
    wrapper.unmount()
  })

  it('shows sample success counts', async () => {
    const wrapper = await mountPanel()
    expect(document.body.textContent).toContain('5 / 5')
    expect(document.body.textContent).toContain('4 / 5')
    wrapper.unmount()
  })

  it('refreshes when refreshToken changes', async () => {
    const wrapper = await mountPanel(mockHistory, { refreshToken: 0 })
    expect(vi.mocked(templatesApi.getBatchTestHistory)).toHaveBeenCalledTimes(1)
    await wrapper.setProps({ refreshToken: 1 })
    await flushPromises()
    expect(vi.mocked(templatesApi.getBatchTestHistory)).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('BDD-CE-U18-BTH-001: expands row and shows sample results', async () => {
    const wrapper = await mountPanel()
    const expandIcon = wrapper.find('.el-table__expand-icon')
    expect(expandIcon.exists()).toBe(true)
    await expandIcon.trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Sample results')
    expect(document.body.textContent).toContain('EXT-OK')
    expect(document.body.textContent).toContain('EXT-FAIL')
    expect(document.body.textContent).toContain('Sample failed detail')
    expect(document.body.textContent).toContain('Succeeded')
    expect(document.body.textContent).toContain('Failed')
    wrapper.unmount()
  })

  it('BDD-CE-U18-BTH-007: RUNNING with empty samples shows in-progress empty state', async () => {
    const wrapper = await mountPanel([mockHistory[3]!])
    const expandIcon = wrapper.find('.el-table__expand-icon')
    await expandIcon.trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Sample results are not available yet')
    wrapper.unmount()
  })

  it('BDD-CE-U18-BTH-002: Open data set emits matched selection', async () => {
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([
      {
        testDataSetId: 'ds-1',
        externalId: 'EXT-FAIL',
        templateId: 'tpl-1',
        name: 'Fail set',
        description: null,
        variables: {},
        required: false,
        scenarioName: null,
        coverageTags: [],
        datasetVersion: 1,
        locked: false,
        derivedFromId: null,
        createdAt: '2026-07-01T00:00:00Z',
        updatedAt: '2026-07-01T00:00:00Z',
      },
    ])

    const wrapper = await mountPanel()
    await wrapper.find('.el-table__expand-icon').trigger('click')
    await flushPromises()

    const openButtons = wrapper.findAll('[data-testid="batch-history-open-data-set"]')
    expect(openButtons.length).toBeGreaterThan(0)
    await openButtons[0]!.trigger('click')
    await flushPromises()

    expect(wrapper.emitted('open-data-set')).toBeTruthy()
    expect(wrapper.emitted('open-data-set')![0]).toEqual([
      { dataSetExternalId: 'EXT-OK', testDataSetId: null, matched: false },
    ])

    await openButtons[1]!.trigger('click')
    await flushPromises()
    expect(wrapper.emitted('open-data-set')![1]).toEqual([
      { dataSetExternalId: 'EXT-FAIL', testDataSetId: 'ds-1', matched: true },
    ])
    wrapper.unmount()
  })

  it('BDD-CE-U18-BTH-003: match miss still emits navigate with matched false', async () => {
    vi.mocked(templatesApi.listTestDataSets).mockResolvedValue([])
    const wrapper = await mountPanel()
    await wrapper.find('.el-table__expand-icon').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="batch-history-open-data-set"]').trigger('click')
    await flushPromises()

    expect(wrapper.emitted('open-data-set')![0]).toEqual([
      { dataSetExternalId: 'EXT-OK', testDataSetId: null, matched: false },
    ])
    wrapper.unmount()
  })

  it('BDD-CE-U18-BTH-009: legacy sample with previewId offers Open preview', async () => {
    const legacyHistory: BatchTestRunSummary[] = [
      {
        ...mockHistory[0]!,
        sampleResults: [
          {
            testDataSetId: 'legacy-ds',
            previewId: 'prev-99',
            status: 'SUCCEEDED',
          },
        ],
      },
    ]
    const wrapper = await mountPanel(legacyHistory)
    await wrapper.find('.el-table__expand-icon').trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('legacy-ds')
    const previewBtn = wrapper.find('[data-testid="batch-history-open-preview"]')
    expect(previewBtn.exists()).toBe(true)
    await previewBtn.trigger('click')
    expect(wrapper.emitted('open-preview')).toEqual([[{ previewId: 'prev-99' }]])
    wrapper.unmount()
  })
})
