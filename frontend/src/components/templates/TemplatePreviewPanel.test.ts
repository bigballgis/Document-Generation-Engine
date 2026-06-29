import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { PreviewRecord } from '@/types/template'

vi.mock('@/api/templates', () => ({
  getPreview: vi.fn(),
}))

describe('TemplatePreviewPanel', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getPreview).mockReset()
  })

  const preview: PreviewRecord = {
    previewId: 'prev-1',
    templateId: 'tpl-1',
    templateVersionId: 'ver-1',
    status: 'SUCCEEDED',
    outputFormat: 'PDF',
    artifactStorageKey: 'artifacts/prev-1.pdf',
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
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('2 difference(s): 1 blocker(s), 1 warning(s)')
    expect(wrapper.text()).toContain('Structured preview comparison')
    expect(wrapper.text()).toContain('header-logo')
    expect(wrapper.text()).toContain('Logo anchor shifted beyond tolerance.')
    expect(wrapper.text()).toContain('rendering.fidelity.layoutShift')
    expect(wrapper.text()).toContain('LAYOUT_SHIFT')
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
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Run test generate to create a preview record.')
  })
})
