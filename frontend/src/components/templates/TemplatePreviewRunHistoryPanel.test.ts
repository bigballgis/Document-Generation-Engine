import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TemplatePreviewRunHistoryPanel from '@/components/templates/TemplatePreviewRunHistoryPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { PreviewRunSummary } from '@/types/template'

vi.mock('@/api/templates', () => ({
  listPreviewRuns: vi.fn(),
  downloadPreviewArtifact: vi.fn(),
}))

vi.mock('@/components/templates/InlinePdfPreviewViewer.vue', () => ({
  default: {
    name: 'InlinePdfPreviewViewer',
    props: ['blob', 'loading', 'errorMessage'],
    template:
      '<div data-testid="inline-pdf-preview-viewer"><canvas data-testid="inline-pdf-preview-canvas" /></div>',
  },
}))

function run(overrides: Partial<PreviewRunSummary> = {}): PreviewRunSummary {
  return {
    previewId: 'prev-1',
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

describe('TemplatePreviewRunHistoryPanel rendered compare (BDD-IBL-C2-003/005/006)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.listPreviewRuns).mockResolvedValue([
      run({ previewId: 'prev-a' }),
      run({ previewId: 'prev-b', createdAt: '2026-07-19T01:00:00Z' }),
      run({ previewId: 'prev-failed', status: 'FAILED', pdfAvailable: false }),
    ])
    vi.mocked(templatesApi.downloadPreviewArtifact).mockResolvedValue({
      blob: new Blob(['%PDF'], { type: 'application/pdf' }),
      filename: 'preview.pdf',
    })
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.listPreviewRuns).mockReset()
    vi.mocked(templatesApi.downloadPreviewArtifact).mockReset()
  })

  function mountPanel() {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(TemplatePreviewRunHistoryPanel, {
      props: {
        templateId: 'tpl-1',
        selectedPreviewId: null,
      },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
      attachTo: document.body,
    })
  }

  it('disables Compare rendered outputs until exactly two SUCCEEDED+PDF runs are selected', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const compareButton = wrapper.find('[data-testid="compare-rendered-outputs"]')
    expect(compareButton.exists()).toBe(true)
    expect(compareButton.text()).toBe('Compare rendered outputs')
    expect((compareButton.element as HTMLButtonElement).disabled).toBe(true)
    expect(wrapper.find('[data-testid="compare-rendered-outputs-hint"]').text()).toContain(
      'Select exactly two',
    )

    const table = wrapper.findComponent({ name: 'ElTable' })
    await table.vm.$emit('selection-change', [run({ previewId: 'prev-a' })])
    await flushPromises()
    expect((wrapper.find('[data-testid="compare-rendered-outputs"]').element as HTMLButtonElement).disabled).toBe(
      true,
    )

    await table.vm.$emit('selection-change', [
      run({ previewId: 'prev-a' }),
      run({ previewId: 'prev-b' }),
    ])
    await flushPromises()
    expect((wrapper.find('[data-testid="compare-rendered-outputs"]').element as HTMLButtonElement).disabled).toBe(
      false,
    )
  })

  it('keeps Compare disabled when a selected run lacks PDF (BDD-IBL-C2-003)', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const table = wrapper.findComponent({ name: 'ElTable' })
    await table.vm.$emit('selection-change', [
      run({ previewId: 'prev-a' }),
      run({ previewId: 'prev-docx-only', pdfAvailable: false }),
    ])
    await flushPromises()

    expect((wrapper.find('[data-testid="compare-rendered-outputs"]').element as HTMLButtonElement).disabled).toBe(
      true,
    )
    expect(wrapper.find('[data-testid="compare-rendered-outputs-hint"]').text()).toMatch(/PDF/i)
  })

  it('opens the rendered compare panel with dual panes when Compare is clicked (BDD-IBL-C2-001)', async () => {
    const wrapper = mountPanel()
    await flushPromises()

    const table = wrapper.findComponent({ name: 'ElTable' })
    await table.vm.$emit('selection-change', [
      run({ previewId: 'prev-a' }),
      run({ previewId: 'prev-b' }),
    ])
    await flushPromises()

    await wrapper.find('[data-testid="compare-rendered-outputs"]').trigger('click')
    await flushPromises()
    await flushPromises()

    // el-dialog teleports to body by default
    expect(document.body.querySelector('[data-testid="rendered-compare-panel"]')).toBeTruthy()
    expect(document.body.querySelector('[data-testid="rendered-compare-pane-a"]')).toBeTruthy()
    expect(document.body.querySelector('[data-testid="rendered-compare-pane-b"]')).toBeTruthy()
  })
})
