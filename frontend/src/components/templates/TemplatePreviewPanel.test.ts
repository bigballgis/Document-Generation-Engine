import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { PreviewRecord } from '@/types/template'

vi.mock('@/api/templates', () => ({
  getPreview: vi.fn(),
  downloadPreviewArtifact: vi.fn(),
}))

vi.mock('@/components/templates/InlinePdfPreviewViewer.vue', () => ({
  default: {
    name: 'InlinePdfPreviewViewer',
    props: ['blob', 'loading', 'errorMessage'],
    template:
      '<div data-testid="inline-pdf-preview-viewer"><span data-testid="inline-pdf-preview-page-label">Page 1 of 1</span><canvas data-testid="inline-pdf-preview-canvas" /></div>',
  },
}))

describe('TemplatePreviewPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getPreview).mockReset()
    vi.mocked(templatesApi.downloadPreviewArtifact).mockReset()
  })

  function createTestRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/templates/:templateId/dev/:devVersionId', component: { template: '<div />' } }],
    })
  }

  const preview: PreviewRecord = {
    previewId: 'prev-1',
    templateId: 'tpl-1',
    templateVersionId: 'ver-1',
    status: 'SUCCEEDED',
    outputFormat: 'PDF',
    artifactStorageKey: 'artifacts/prev-1.pdf',
    pdfArtifactStorageKey: 'artifacts/prev-1.pdf',
    fidelityWarnings: [
      {
        code: 'LAYOUT_SHIFT',
        messageKey: 'rendering.fidelity.layoutShift',
        location: 'page-1',
        artifact: 'artifacts/prev-1.pdf',
        viewed: false,
      },
    ],
    previewComparison: {
      totalDiffCount: 2,
      blockerCount: 1,
      warningCount: 1,
      items: [
        {
          locationType: 'ANCHOR',
          locationRef: 'header-logo',
          severity: 'BLOCKER',
          diffCode: 'LAYOUT_MISMATCH',
          summary: 'Logo anchor shifted beyond tolerance.',
        },
        {
          locationType: 'PAGE',
          locationRef: 'page-1',
          severity: 'WARNING',
          diffCode: 'FONT_SUBSTITUTION',
          summary: 'Fallback font used for body text.',
        },
      ],
    },
    testDataSetId: 'tds-1',
    createdAt: '2026-06-25T00:00:00Z',
  }

  it('renders preview comparison summary and structured diff rows', async () => {
    vi.mocked(templatesApi.downloadPreviewArtifact).mockResolvedValue({
      blob: new Blob(['%PDF'], { type: 'application/pdf' }),
      filename: 'preview.pdf',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplatePreviewPanel, {
      props: {
        templateId: 'tpl-1',
        bindings: [],
        preview,
      },
      global: { plugins: [createPinia(), i18n, ElementPlus, createTestRouter()] },
      attachTo: document.body,
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('2 difference(s): 1 blocker(s), 1 warning(s)')
    expect(wrapper.text()).toContain('Structured preview comparison')
    expect(wrapper.text()).toContain('header-logo')
    expect(wrapper.text()).toContain('Logo anchor shifted beyond tolerance.')
    expect(wrapper.text()).toContain('rendering.fidelity.layoutShift')
    expect(wrapper.find('[data-testid="fidelity-warning-technical-toggle"]').exists()).toBe(true)
    await wrapper.find('[data-testid="fidelity-warning-technical-toggle"]').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('LAYOUT_SHIFT')
    expect(wrapper.find('[data-testid="fidelity-warning-edit-binding"]').exists()).toBe(true)
  })

  it('BDD-CE-U04-IPP-001 shows inline PDF preview section', async () => {
    vi.mocked(templatesApi.downloadPreviewArtifact).mockResolvedValue({
      blob: new Blob(['%PDF'], { type: 'application/pdf' }),
      filename: 'preview.pdf',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplatePreviewPanel, {
      props: {
        templateId: 'tpl-1',
        bindings: [],
        preview,
      },
      global: { plugins: [createPinia(), i18n, ElementPlus, createTestRouter()] },
      attachTo: document.body,
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.find('[data-testid="preview-inline-pdf-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="inline-pdf-preview-viewer"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="inline-pdf-preview-page-label"]').text()).toContain('Page 1 of 1')
  })

  it('hides inline PDF preview when preview run failed', async () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplatePreviewPanel, {
      props: {
        templateId: 'tpl-1',
        bindings: [],
        preview: { ...preview, status: 'FAILED' },
      },
      global: { plugins: [createPinia(), i18n, ElementPlus, createTestRouter()] },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="preview-inline-pdf-section"]').exists()).toBe(false)
  })

  it('shows empty state when no preview record exists', async () => {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplatePreviewPanel, {
      props: {
        templateId: 'tpl-1',
        bindings: [],
        preview: null,
      },
      global: { plugins: [createPinia(), i18n, ElementPlus, createTestRouter()] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Run preview on a test data set to generate downloadable DOCX and PDF artifacts.')
  })
})
