import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BatchTestHistoryPanel from '@/components/template/BatchTestHistoryPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { BatchTestRunSummary } from '@/types/template'

vi.mock('@/api/templates', () => ({
  getBatchTestHistory: vi.fn(),
}))

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

const mockHistory: BatchTestRunSummary[] = [
  {
    runId: 'run-100',
    createdAt: '2026-07-03T10:00:00Z',
    createdBy: 'alice',
    status: 'COMPLETED',
    totalSamples: 5,
    succeededCount: 5,
    failedCount: 0,
    anchorCoveragePct: 90.0,
    variableCoveragePct: 85.0,
    sampleCoveragePct: 100.0,
    gatePassed: true,
    invalidatedAt: null,
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
  },
]

describe('BatchTestHistoryPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getBatchTestHistory).mockReset()
  })

  it('shows empty state when no history', async () => {
    vi.mocked(templatesApi.getBatchTestHistory).mockResolvedValue([])
    const i18n = makeI18n()

    const wrapper = mount(BatchTestHistoryPanel, {
      props: { templateId: 'tpl-1' },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(document.body.textContent).toContain('No full test runs yet')
    wrapper.unmount()
  })

  it('renders history rows with correct status tags', async () => {
    vi.mocked(templatesApi.getBatchTestHistory).mockResolvedValue(mockHistory)
    const i18n = makeI18n()

    const wrapper = mount(BatchTestHistoryPanel, {
      props: { templateId: 'tpl-1' },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    // run-100 should show gate passed
    expect(document.body.textContent).toContain('Readiness checks passed')
    // run-099 readiness not met
    expect(document.body.textContent).toContain('Readiness checks not met')

    wrapper.unmount()
  })

  it('shows INVALIDATED tag for invalidated run', async () => {
    vi.mocked(templatesApi.getBatchTestHistory).mockResolvedValue(mockHistory)
    const i18n = makeI18n()

    const wrapper = mount(BatchTestHistoryPanel, {
      props: { templateId: 'tpl-1' },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(document.body.textContent).toContain('Invalidated')

    wrapper.unmount()
  })

  it('shows sample success counts', async () => {
    vi.mocked(templatesApi.getBatchTestHistory).mockResolvedValue(mockHistory)
    const i18n = makeI18n()

    const wrapper = mount(BatchTestHistoryPanel, {
      props: { templateId: 'tpl-1' },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    // run-100: 5/5 passed
    expect(document.body.textContent).toContain('5 / 5')
    // run-099: 4/5 passed
    expect(document.body.textContent).toContain('4 / 5')

    wrapper.unmount()
  })

  it('refreshes when refreshToken changes', async () => {
    vi.mocked(templatesApi.getBatchTestHistory).mockResolvedValue(mockHistory)
    const i18n = makeI18n()

    const wrapper = mount(BatchTestHistoryPanel, {
      props: { templateId: 'tpl-1', refreshToken: 0 },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(vi.mocked(templatesApi.getBatchTestHistory)).toHaveBeenCalledTimes(1)

    await wrapper.setProps({ refreshToken: 1 })
    await flushPromises()

    expect(vi.mocked(templatesApi.getBatchTestHistory)).toHaveBeenCalledTimes(2)

    wrapper.unmount()
  })
})
