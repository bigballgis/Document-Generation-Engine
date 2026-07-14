import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'

vi.mock('pdfjs-dist', () => ({
  GlobalWorkerOptions: { workerSrc: '' },
  getDocument: vi.fn(),
}))

vi.mock('pdfjs-dist/build/pdf.worker.min.mjs?url', () => ({
  default: 'mock-pdf-worker.js',
}))

import InlinePdfPreviewViewer from '@/components/templates/InlinePdfPreviewViewer.vue'

describe('InlinePdfPreviewViewer', () => {
  afterEach(() => {
    document.body.innerHTML = ''
  })

  it('BDD-CE-U04-IPP-003 shows error when parent reports load failure', () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(InlinePdfPreviewViewer, {
      props: {
        blob: null,
        loading: false,
        errorMessage: 'Unable to load the PDF preview.',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    expect(wrapper.find('[data-testid="inline-pdf-preview-error"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Unable to load the PDF preview.')
  })

  it('shows loading state while parent fetches artifact bytes', () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(InlinePdfPreviewViewer, {
      props: {
        blob: null,
        loading: true,
        errorMessage: null,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    expect(wrapper.find('[data-testid="inline-pdf-preview-loading"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Loading PDF preview')
  })
})
