import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BatchTestProgressDialog from '@/components/template/BatchTestProgressDialog.vue'
import en from '@/i18n/locales/en'

class MockEventSource {
  url: string
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

describe('BatchTestProgressDialog', () => {
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
    mount(BatchTestProgressDialog, {
      props: {
        modelValue: false,
        templateId: 'tpl-1',
        runId: 'run-1',
        streamUrl: '/api/management/v1/templates/tpl-1/batch-tests/run-1/progress-stream',
        dataSetCount: 3,
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()
    expect(MockEventSource.instances).toHaveLength(0)
  })

  it('shows progress dialog with sample count when opened', async () => {
    const i18n = makeI18n()
    const wrapper = mount(BatchTestProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        runId: 'run-1',
        streamUrl: '/api/management/v1/templates/tpl-1/batch-tests/run-1/progress-stream',
        dataSetCount: 3,
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    expect(MockEventSource.instances).toHaveLength(1)
    expect(MockEventSource.instances[0].url).toContain('progress-stream')
    expect(MockEventSource.instances[0].url).toContain('token=test-token')
    expect(document.body.textContent).toContain('0 / 3')

    wrapper.unmount()
  })

  it('updates progress list as sample_done events arrive', async () => {
    const i18n = makeI18n()
    const wrapper = mount(BatchTestProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        runId: 'run-1',
        streamUrl: '/api/management/v1/templates/tpl-1/batch-tests/run-1/progress-stream',
        dataSetCount: 3,
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    MockEventSource.instances[0].emit('sample_done', {
      sampleIndex: 1,
      success: true,
      dataSetExternalId: 'TDS-001',
    })
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('TDS-001')
    expect(document.body.textContent).toContain('1 / 3')

    wrapper.unmount()
  })

  it('shows summary and emits completed on batch_completed', async () => {
    const i18n = makeI18n()
    const wrapper = mount(BatchTestProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        runId: 'run-1',
        streamUrl: '/api/management/v1/templates/tpl-1/batch-tests/run-1/progress-stream',
        dataSetCount: 3,
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    MockEventSource.instances[0].emit('batch_completed', {
      runId: 'run-1',
      successCount: 2,
      failedCount: 1,
      anchorCoveragePct: 85.0,
      variableCoveragePct: 90.0,
      sampleCoveragePct: 100.0,
      gatePassed: false,
    })
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('2 succeeded')
    expect(document.body.textContent).toContain('1 failed')
    expect(wrapper.emitted('completed')).toBeTruthy()

    wrapper.unmount()
  })

  it('shows error on batch_failed event', async () => {
    const i18n = makeI18n()
    const wrapper = mount(BatchTestProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        runId: 'run-1',
        streamUrl: '/api/management/v1/templates/tpl-1/batch-tests/run-1/progress-stream',
        dataSetCount: 3,
      },
      global: { plugins: [i18n, ElementPlus] },
      attachTo: document.body,
    })
    await flushPromises()

    MockEventSource.instances[0].emit('batch_failed', {
      error: 'Internal batch error',
    })
    await wrapper.vm.$nextTick()

    expect(document.body.textContent).toContain('Internal batch error')

    wrapper.unmount()
  })

  it('closes SSE when dialog is closed', async () => {
    const i18n = makeI18n()
    const wrapper = mount(BatchTestProgressDialog, {
      props: {
        modelValue: true,
        templateId: 'tpl-1',
        runId: 'run-1',
        streamUrl: '/api/management/v1/templates/tpl-1/batch-tests/run-1/progress-stream',
        dataSetCount: 3,
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
})
