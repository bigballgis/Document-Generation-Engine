import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import RenderedComparePanel from '@/components/templates/RenderedComparePanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { PreviewRunSummary } from '@/types/template'

vi.mock('@/api/templates', () => ({
  downloadPreviewArtifact: vi.fn(),
}))

vi.mock('@/components/templates/InlinePdfPreviewViewer.vue', () => ({
  default: {
    name: 'InlinePdfPreviewViewer',
    props: ['blob', 'loading', 'errorMessage'],
    template: `
      <div data-testid="inline-pdf-preview-viewer">
        <div v-if="loading" data-testid="inline-pdf-preview-loading">loading</div>
        <div v-else-if="errorMessage" data-testid="inline-pdf-preview-error">{{ errorMessage }}</div>
        <canvas v-else-if="blob" data-testid="inline-pdf-preview-canvas" />
      </div>
    `,
  },
}))

function run(overrides: Partial<PreviewRunSummary> = {}): PreviewRunSummary {
  return {
    previewId: 'prev-a',
    templateVersionId: 'ver-1',
    status: 'SUCCEEDED',
    testDataSetId: 'tds-1',
    createdAt: '2026-07-19T00:00:00Z',
    createdBy: 'author-1',
    fidelityWarningCount: 0,
    comparisonBlockerCount: 0,
    comparisonWarningCount: 0,
    docxAvailable: true,
    pdfAvailable: true,
    ...overrides,
  }
}

describe('RenderedComparePanel (BDD-IBL-C2-001/002/004/006)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.downloadPreviewArtifact).mockImplementation(
      async (_templateId, previewId) => ({
        blob: new Blob([`%PDF-${previewId}`], { type: 'application/pdf' }),
        filename: `${previewId}.pdf`,
      }),
    )
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.downloadPreviewArtifact).mockReset()
  })

  function mountPanel(runA: PreviewRunSummary, runB: PreviewRunSummary) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(RenderedComparePanel, {
      props: {
        templateId: 'tpl-1',
        runA,
        runB,
      },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
  }

  it('renders side-by-side panes with distinct previewIds and PDF viewers (BDD-IBL-C2-001)', async () => {
    const wrapper = mountPanel(
      run({ previewId: 'prev-a' }),
      run({ previewId: 'prev-b', testDataSetId: 'tds-2' }),
    )
    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="rendered-compare-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="rendered-compare-pane-a"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="rendered-compare-pane-b"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="rendered-compare-pane-a"]').text()).toContain('prev-a')
    expect(wrapper.find('[data-testid="rendered-compare-pane-b"]').text()).toContain('prev-b')
    expect(wrapper.findAll('[data-testid="inline-pdf-preview-viewer"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-testid="inline-pdf-preview-canvas"]')).toHaveLength(2)
    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalledWith('tpl-1', 'prev-a', 'pdf')
    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalledWith('tpl-1', 'prev-b', 'pdf')
  })

  it('does not treat semantic/warning panels as the compare surface (BDD-IBL-C2-002)', async () => {
    const wrapper = mountPanel(run({ previewId: 'prev-a' }), run({ previewId: 'prev-b' }))
    await flushPromises()

    expect(wrapper.find('[data-testid="rendered-compare-panel"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Structured preview comparison')
    expect(wrapper.find('[data-testid="template-change-diff-panel"]').exists()).toBe(false)
  })

  it('shows English-first title copy (BDD-IBL-C2-006)', async () => {
    const wrapper = mountPanel(run({ previewId: 'prev-a' }), run({ previewId: 'prev-b' }))
    await flushPromises()

    expect(wrapper.text()).toContain('Compare rendered outputs')
    expect(wrapper.text()).not.toMatch(/对比渲染|并排对比/)
  })

  it('keeps the successful pane visible when the other side fails (BDD-IBL-C2-004)', async () => {
    vi.mocked(templatesApi.downloadPreviewArtifact).mockImplementation(async (_t, previewId) => {
      if (previewId === 'prev-b') {
        throw new Error('expired')
      }
      return {
        blob: new Blob(['%PDF-a'], { type: 'application/pdf' }),
        filename: 'prev-a.pdf',
      }
    })

    const wrapper = mountPanel(run({ previewId: 'prev-a' }), run({ previewId: 'prev-b' }))
    await flushPromises()
    await flushPromises()

    const paneA = wrapper.find('[data-testid="rendered-compare-pane-a"]')
    const paneB = wrapper.find('[data-testid="rendered-compare-pane-b"]')
    expect(paneA.find('[data-testid="inline-pdf-preview-canvas"]').exists()).toBe(true)
    expect(paneB.find('[data-testid="inline-pdf-preview-error"]').exists()).toBe(true)
    expect(paneB.text()).toMatch(/Unable to load|failed/i)
  })
})
