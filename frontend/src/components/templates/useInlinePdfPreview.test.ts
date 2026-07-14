import { defineComponent, h } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import { useInlinePdfPreview } from '@/components/templates/useInlinePdfPreview'
import type { PreviewRecord } from '@/types/template'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/api/templates')>()
  return {
    ...actual,
    downloadPreviewArtifact: vi.fn(),
  }
})

describe('useInlinePdfPreview', () => {
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
    createdAt: '2026-07-14T00:00:00Z',
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.downloadPreviewArtifact).mockReset()
    vi.mocked(templatesApi.downloadPreviewArtifact).mockResolvedValue({
      blob: new Blob(['%PDF'], { type: 'application/pdf' }),
      filename: 'preview.pdf',
    })
  })

  async function runHook(previewRecord: PreviewRecord | null) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let state: ReturnType<typeof useInlinePdfPreview> | null = null
    const Host = defineComponent({
      props: {
        previewRecord: {
          type: Object as () => PreviewRecord | null,
          required: true,
        },
      },
      setup(props) {
        state = useInlinePdfPreview({
          templateId: 'tpl-1',
          preview: () => props.previewRecord,
        })
        return () => h('div')
      },
    })
    mount(Host, {
      props: { previewRecord },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()
    return state!
  }

  it('loads PDF blob when preview succeeded', async () => {
    const state = await runHook(preview)

    expect(state.canShowInlinePdf.value).toBe(true)
    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalledWith('tpl-1', 'prev-1', 'pdf')
    expect(state.pdfBlob.value).toBeInstanceOf(Blob)
  })

  it('allows inline preview when only generic artifact key is present', async () => {
    const state = await runHook({
      ...preview,
      pdfArtifactStorageKey: null,
      artifactStorageKey: 'artifacts/prev-1.pdf',
    })

    expect(state.canShowInlinePdf.value).toBe(true)
    await flushPromises()
    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalled()
  })

  it('skips load when preview not succeeded', async () => {
    const state = await runHook({ ...preview, status: 'FAILED' })

    expect(state.canShowInlinePdf.value).toBe(false)
    expect(templatesApi.downloadPreviewArtifact).not.toHaveBeenCalled()
  })

  it('surfaces load error when artifact download fails', async () => {
    vi.mocked(templatesApi.downloadPreviewArtifact).mockRejectedValue(new Error('network'))
    const state = await runHook(preview)

    expect(state.errorMessage.value).toBe('Unable to load the PDF preview.')
    expect(state.pdfBlob.value).toBeNull()
  })

  it('does not download when preview id is missing', async () => {
    const state = await runHook({ ...preview, previewId: '' })
    await flushPromises()
    expect(templatesApi.downloadPreviewArtifact).not.toHaveBeenCalled()
    expect(state.pdfBlob.value).toBeNull()
  })

  it('drops inline preview when status changes away from succeeded', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let state: ReturnType<typeof useInlinePdfPreview> | null = null
    const Host = defineComponent({
      props: {
        previewRecord: {
          type: Object as () => PreviewRecord | null,
          required: true,
        },
      },
      setup(props) {
        state = useInlinePdfPreview({
          templateId: 'tpl-1',
          preview: () => props.previewRecord,
        })
        return () => h('div')
      },
    })
    const wrapper = mount(Host, {
      props: { previewRecord: preview },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()
    expect(state!.canShowInlinePdf.value).toBe(true)

    await wrapper.setProps({ previewRecord: { ...preview, status: 'FAILED' } })
    await flushPromises()
    expect(state!.canShowInlinePdf.value).toBe(false)
    expect(state!.pdfBlob.value).toBeNull()
  })

  it('clears blob when preview record is removed', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let state: ReturnType<typeof useInlinePdfPreview> | null = null
    const Host = defineComponent({
      props: {
        previewRecord: {
          type: Object as () => PreviewRecord | null,
          required: true,
        },
      },
      setup(props) {
        state = useInlinePdfPreview({
          templateId: 'tpl-1',
          preview: () => props.previewRecord,
        })
        return () => h('div')
      },
    })
    const wrapper = mount(Host, {
      props: { previewRecord: preview },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()
    await wrapper.setProps({ previewRecord: null })
    await flushPromises()
    expect(state!.canShowInlinePdf.value).toBe(false)
    expect(state!.pdfBlob.value).toBeNull()
  })

  it('allows inline preview when only pdf artifact key is present', async () => {
    const state = await runHook({
      ...preview,
      artifactStorageKey: null,
      pdfArtifactStorageKey: 'artifacts/prev-1.pdf',
    })

    expect(state.canShowInlinePdf.value).toBe(true)
  })

  it('disables inline preview without artifact keys', async () => {
    const state = await runHook({
      ...preview,
      artifactStorageKey: null,
      pdfArtifactStorageKey: null,
    })

    expect(state.canShowInlinePdf.value).toBe(false)
    await flushPromises()
    expect(templatesApi.downloadPreviewArtifact).not.toHaveBeenCalled()
  })

  it('exposes reloadPdf to refetch artifact bytes', async () => {
    const state = await runHook(preview)
    await flushPromises()
    vi.mocked(templatesApi.downloadPreviewArtifact).mockClear()

    await state.reloadPdf()

    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalledWith('tpl-1', 'prev-1', 'pdf')
  })

  it('discards in-flight download on unmount', async () => {
    let resolveSlow: (value: { blob: Blob; filename: string }) => void
    const slowPromise = new Promise<{ blob: Blob; filename: string }>((resolve) => {
      resolveSlow = resolve
    })
    vi.mocked(templatesApi.downloadPreviewArtifact).mockImplementationOnce(() => slowPromise)

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let state: ReturnType<typeof useInlinePdfPreview> | null = null
    const Host = defineComponent({
      props: {
        previewRecord: {
          type: Object as () => PreviewRecord | null,
          required: true,
        },
      },
      setup(props) {
        state = useInlinePdfPreview({
          templateId: 'tpl-1',
          preview: () => props.previewRecord,
        })
        return () => h('div')
      },
    })
    const wrapper = mount(Host, {
      props: { previewRecord: preview },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()
    expect(state!.loading.value).toBe(true)

    wrapper.unmount()
    resolveSlow!({
      blob: new Blob(['%PDF-stale'], { type: 'application/pdf' }),
      filename: 'preview-stale.pdf',
    })
    await flushPromises()

    expect(state!.pdfBlob.value).toBeNull()
  })

  it('ignores stale download when preview id changes mid-fetch', async () => {
    let resolveSlow: (value: { blob: Blob; filename: string }) => void
    const slowPromise = new Promise<{ blob: Blob; filename: string }>((resolve) => {
      resolveSlow = resolve
    })
    vi.mocked(templatesApi.downloadPreviewArtifact)
      .mockImplementationOnce(() => slowPromise)
      .mockResolvedValueOnce({
        blob: new Blob(['%PDF-new'], { type: 'application/pdf' }),
        filename: 'preview-new.pdf',
      })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    let state: ReturnType<typeof useInlinePdfPreview> | null = null
    const Host = defineComponent({
      props: {
        previewRecord: {
          type: Object as () => PreviewRecord | null,
          required: true,
        },
      },
      setup(props) {
        state = useInlinePdfPreview({
          templateId: 'tpl-1',
          preview: () => props.previewRecord,
        })
        return () => h('div')
      },
    })
    const wrapper = mount(Host, {
      props: { previewRecord: preview },
      global: { plugins: [createPinia(), i18n, ElementPlus] },
    })
    await flushPromises()

    await wrapper.setProps({ previewRecord: { ...preview, previewId: 'prev-2' } })
    await flushPromises()
    expect(templatesApi.downloadPreviewArtifact).toHaveBeenCalledTimes(2)
    expect(state!.pdfBlob.value).toBeInstanceOf(Blob)

    resolveSlow!({
      blob: new Blob(['%PDF-stale'], { type: 'application/pdf' }),
      filename: 'preview-stale.pdf',
    })
    await flushPromises()

    expect(state!.pdfBlob.value).toBeInstanceOf(Blob)
    expect(state!.pdfBlob.value!.size).toBeGreaterThan(0)
  })
})
