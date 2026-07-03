import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PreviewProgressDialog from '@/components/template/PreviewProgressDialog.vue'
import { connectAuthorizedEventStream } from '@/utils/authorizedEventStream'
import en from '@/i18n/locales/en'

vi.mock('@/utils/authorizedEventStream', () => ({
  connectAuthorizedEventStream: vi.fn(),
}))

interface MockStreamRequest {
  url: string
  token?: string | null
  onMessage: (event: { type: string; data: string }) => void
}

const streamRequests: MockStreamRequest[] = []
const closeMocks: Array<ReturnType<typeof vi.fn>> = []

function emitStreamEvent(index: number, type: string, payload: unknown) {
  streamRequests[index]?.onMessage({
    type,
    data: JSON.stringify(payload),
  })
}

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

describe('PreviewProgressDialog', () => {
  beforeEach(() => {
    streamRequests.length = 0
    closeMocks.length = 0
    vi.mocked(connectAuthorizedEventStream).mockImplementation(async ({ url, token, onMessage }) => {
      const close = vi.fn()
      streamRequests.push({ url, token, onMessage })
      closeMocks.push(close)
      return {
        close,
        done: Promise.resolve(),
      }
    })
    localStorage.setItem('docgen.accessToken', 'test-token')
  })

  afterEach(() => {
    document.body.innerHTML = ''
    localStorage.clear()
  })

  it('does not connect SSE when dialog is closed', async () => {
    const i18n = makeI18n()
    mount(PreviewProgressDialog, {
      props: {
        modelValue: false,
        templateId: 'tpl-1',
        previewId: 'prev-1',
        streamUrl: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
        dataSetName: 'DS-001',
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()
    expect(streamRequests).toHaveLength(0)
  })

  it('connects SSE and shows progress stage when dialog opens', async () => {
    const i18n = makeI18n()
    const wrapper = mount(PreviewProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        previewId: 'prev-1',
        streamUrl: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
        dataSetName: 'DS-001',
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(streamRequests).toHaveLength(1)
    expect(streamRequests[0]?.url).toContain('progress-stream')
    expect(streamRequests[0]?.url).not.toContain('token=')
    expect(streamRequests[0]?.token).toBe('test-token')

    // Emit progress event
    emitStreamEvent(0, 'progress', {
      stage: 'GENERATING_DOCX',
      percent: 40,
      message: 'Generating document',
    })
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('40')

    wrapper.unmount()
  })

  it('shows download buttons and countdown on completed event', async () => {
    const i18n = makeI18n()
    const futureExpiry = new Date(Date.now() + 23 * 60 * 60 * 1000).toISOString()

    const wrapper = mount(PreviewProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        previewId: 'prev-1',
        streamUrl: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
        dataSetName: 'DS-001',
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    emitStreamEvent(0, 'completed', {
      previewId: 'prev-1',
      docxDownloadUrl: 'http://example.com/docx',
      pdfDownloadUrl: 'http://example.com/pdf',
      expiresAt: futureExpiry,
    })
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('Download DOCX')
    expect(document.body.textContent).toContain('Download PDF')

    wrapper.unmount()
  })

  it('shows error message and retry button on failed event', async () => {
    const i18n = makeI18n()
    const wrapper = mount(PreviewProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        previewId: 'prev-1',
        streamUrl: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
        dataSetName: 'DS-001',
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    emitStreamEvent(0, 'failed', {
      error: 'Render engine failed',
      retryable: true,
    })
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('Render engine failed')
    expect(document.body.textContent).toContain('Retry')

    wrapper.unmount()
  })

  it('closes SSE on dialog close', async () => {
    const i18n = makeI18n()
    const wrapper = mount(PreviewProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        previewId: 'prev-1',
        streamUrl: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
        dataSetName: 'DS-001',
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(closeMocks[0]).not.toHaveBeenCalled()

    await wrapper.setProps({ modelValue: false })
    await flushPromises()

    expect(closeMocks[0]).toHaveBeenCalled()

    wrapper.unmount()
  })

  it('emits retry event when retry button clicked', async () => {
    const i18n = makeI18n()
    const wrapper = mount(PreviewProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        previewId: 'prev-1',
        streamUrl: '/api/management/v1/templates/tpl-1/previews/prev-1/progress-stream',
        dataSetName: 'DS-001',
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    emitStreamEvent(0, 'failed', { error: 'Failed', retryable: true })
    await wrapper.vm.$nextTick()

    const retryBtn = document.querySelector('[data-testid="retry-btn"]')
    if (retryBtn) {
      ;(retryBtn as HTMLElement).click()
    }
    await flushPromises()

    expect(wrapper.emitted('retry')).toBeTruthy()

    wrapper.unmount()
  })
})
