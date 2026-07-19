import { flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { usePreviewRunPdfBlob } from '@/components/templates/usePreviewRunPdfBlob'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  downloadPreviewArtifact: vi.fn(),
}))

describe('usePreviewRunPdfBlob', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.downloadPreviewArtifact).mockResolvedValue({
      blob: new Blob(['%PDF'], { type: 'application/pdf' }),
      filename: 'preview.pdf',
    })
  })

  afterEach(() => {
    vi.mocked(templatesApi.downloadPreviewArtifact).mockReset()
  })

  function mountComposable(previewId: string | null, enabled: boolean) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const previewIdRef = ref(previewId)
    const enabledRef = ref(enabled)
    let api: ReturnType<typeof usePreviewRunPdfBlob> | null = null

    const Host = defineComponent({
      setup() {
        api = usePreviewRunPdfBlob({
          templateId: 'tpl-1',
          previewId: previewIdRef,
          enabled: enabledRef,
        })
        return () => h('div')
      },
    })

    mount(Host, { global: { plugins: [createPinia(), i18n] } })
    return { api: api!, previewIdRef, enabledRef }
  }

  it('loads PDF when enabled (BDD-IBL-C2-001)', async () => {
    const { api } = mountComposable('prev-1', true)
    await flushPromises()
    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalledWith('tpl-1', 'prev-1', 'pdf')
    expect(api.pdfBlob.value).toBeInstanceOf(Blob)
    expect(api.errorMessage.value).toBeNull()
  })

  it('skips download when disabled', async () => {
    mountComposable('prev-1', false)
    await flushPromises()
    expect(templatesApi.downloadPreviewArtifact).not.toHaveBeenCalled()
  })

  it('surfaces load failure without clearing the other pane contract (BDD-IBL-C2-004)', async () => {
    vi.mocked(templatesApi.downloadPreviewArtifact).mockRejectedValue(new Error('404'))
    const { api } = mountComposable('prev-1', true)
    await flushPromises()
    await nextTick()
    expect(api.pdfBlob.value).toBeNull()
    expect(api.errorMessage.value).toBe('Unable to load the PDF preview.')
  })
})
