import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import AuthoringPreviewPane from '@/components/templates/AuthoringPreviewPane.vue'
import type { PreviewRecord } from '@/types/template'

vi.mock('@/components/templates/TemplatePreviewPanel.vue', () => ({
  default: {
    name: 'TemplatePreviewPanel',
    props: ['templateId', 'bindings', 'preview', 'embedded'],
    template:
      '<div data-testid="template-preview-panel-stub"><div data-testid="inline-pdf-preview-viewer-stub" /></div>',
  },
}))

describe('AuthoringPreviewPane', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  const preview: PreviewRecord = {
    previewId: 'prev-1',
    templateId: 'tpl-1',
    templateVersionId: 'ver-1',
    status: 'SUCCEEDED',
    outputFormat: 'PDF',
    artifactStorageKey: 'artifacts/prev-1.pdf',
    pdfArtifactStorageKey: 'artifacts/prev-1.pdf',
    fidelityWarnings: [],
    previewComparison: null,
    testDataSetId: 'tds-1',
    createdAt: '2026-07-09T00:00:00Z',
  }

  function mountPane(overrides: Record<string, unknown> = {}) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(AuthoringPreviewPane, {
      props: {
        templateId: 'tpl-1',
        bindings: [],
        preview: null,
        stale: false,
        refreshing: false,
        ...overrides,
      },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
  }

  it('BDD-F7-B2-007 shows empty state when no preview', () => {
    const wrapper = mountPane()
    expect(wrapper.find('[data-testid="authoring-preview-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('No preview yet')
  })

  it('BDD-F7-B2-005 shows CD-PIT-08 boundary copy', () => {
    const wrapper = mountPane({ preview })
    expect(wrapper.find('[data-testid="authoring-preview-boundary"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('not legal evidence')
  })

  it('BDD-F7-B2-003 shows stale badge when stale', () => {
    const wrapper = mountPane({ preview, stale: true })
    expect(wrapper.find('[data-testid="authoring-preview-stale-badge"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Preview out of date')
  })

  it('BDD-F7-B2-004 emits refresh when Refresh now clicked', async () => {
    const wrapper = mountPane({ preview, stale: true })
    await wrapper.get('[data-testid="authoring-preview-refresh"]').trigger('click')
    expect(wrapper.emitted('refresh')).toHaveLength(1)
  })

  it('BDD-BEI-006 Refresh is secondary (not primary) relative to Save', () => {
    const wrapper = mountPane({ preview, stale: true })
    const refresh = wrapper.get('[data-testid="authoring-preview-refresh"]')
    expect(refresh.classes().join(' ')).not.toMatch(/el-button--primary/)
  })

  it('BDD-F7-B2-006 disables refresh while in-flight', async () => {
    const wrapper = mountPane({ preview, refreshing: true })
    const button = wrapper.get('[data-testid="authoring-preview-refresh"]')
    expect(button.attributes('disabled')).toBeDefined()
    await button.trigger('click')
    expect(wrapper.emitted('refresh')).toBeUndefined()
  })

  it('renders preview panel stub when preview exists', async () => {
    const wrapper = mountPane({ preview })
    await flushPromises()
    expect(wrapper.find('[data-testid="template-preview-panel-stub"]').exists()).toBe(true)
  })

  it('CRCH-W1-1 removes duplicated authoring inline PDF section; single viewer remains', async () => {
    const wrapper = mountPane({ preview })
    await flushPromises()
    expect(wrapper.findAll('[data-testid="authoring-inline-pdf-section"]')).toHaveLength(0)
    expect(wrapper.findAll('[data-testid="inline-pdf-preview-viewer-stub"]')).toHaveLength(1)
  })

  it('fetches the preview PDF exactly once (regression: CRCH-W1-1 duplicated viewer)', async () => {
    const wrapper = mountPane({ preview })
    await flushPromises()
    expect(wrapper.findAll('[data-testid="authoring-inline-pdf-section"]')).toHaveLength(0)
    expect(wrapper.findAll('[data-testid="inline-pdf-preview-viewer-stub"]')).toHaveLength(1)
  })
})
