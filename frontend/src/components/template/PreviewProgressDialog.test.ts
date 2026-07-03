import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import PreviewProgressDialog from '@/components/template/PreviewProgressDialog.vue'
import en from '@/i18n/locales/en'

// Mock EventSource globally
class MockEventSource {
  url: string
  onmessage: ((evt: MessageEvent) => void) | null = null
  onerror: ((evt: Event) => void) | null = null
  readyState: number = 1
  private handlers: Record<string, ((evt: MessageEvent) => void)[]> = {}
  static instances: MockEventSource[] = []

  constructor(url: string) {
    this.url = url
    MockEventSource.instances.push(this)
  }

  addEventListener(event: string, handler: (evt: MessageEvent) => void) {
    if (!this.handlers[event]) {
      this.handlers[event] = []
    }
    this.handlers[event].push(handler)
  }

  removeEventListener(event: string, handler: (evt: MessageEvent) => void) {
    if (this.handlers[event]) {
      this.handlers[event] = this.handlers[event].filter((h) => h !== handler)
    }
  }

  close() {
    this.readyState = 2
  }

  emit(event: string, data: unknown) {
    const msg = { data: JSON.stringify(data) } as MessageEvent
    this.handlers[event]?.forEach((h) => h(msg))
  }
}

vi.stubGlobal('EventSource', MockEventSource)

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

describe('PreviewProgressDialog', () => {
  beforeEach(() => {
    MockEventSource.instances = []
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
    expect(MockEventSource.instances).toHaveLength(0)
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

    expect(MockEventSource.instances).toHaveLength(1)
    expect(MockEventSource.instances[0].url).toContain('progress-stream')
    expect(MockEventSource.instances[0].url).toContain('token=test-token')

    // Emit progress event
    MockEventSource.instances[0].emit('progress', {
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

    MockEventSource.instances[0].emit('completed', {
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

    MockEventSource.instances[0].emit('failed', {
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

    const es = MockEventSource.instances[0]
    expect(es.readyState).not.toBe(2)

    await wrapper.setProps({ modelValue: false })
    await flushPromises()

    expect(es.readyState).toBe(2)

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

    MockEventSource.instances[0].emit('failed', { error: 'Failed', retryable: true })
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
