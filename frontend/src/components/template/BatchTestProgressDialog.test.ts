import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BatchTestProgressDialog from '@/components/template/BatchTestProgressDialog.vue'
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

describe('BatchTestProgressDialog', () => {
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
    expect(streamRequests).toHaveLength(0)
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

    expect(streamRequests).toHaveLength(1)
    expect(streamRequests[0]?.url).toContain('progress-stream')
    expect(streamRequests[0]?.url).not.toContain('token=')
    expect(streamRequests[0]?.token).toBe('test-token')
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

    emitStreamEvent(0, 'sample_done', {
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

    emitStreamEvent(0, 'batch_completed', {
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

    emitStreamEvent(0, 'batch_failed', {
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

    expect(closeMocks[0]).not.toHaveBeenCalled()

    await wrapper.setProps({ modelValue: false })
    await flushPromises()

    expect(closeMocks[0]).toHaveBeenCalled()

    wrapper.unmount()
  })
})
