import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import PublishGateReadOnlyPanel from '@/components/templates/PublishGateReadOnlyPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import type { PublishGateChecklist } from '@/types/template'

vi.mock('@/api/templates', () => ({
  fetchPublishGate: vi.fn(),
  fetchReleasePublishGate: vi.fn(),
}))

const readyChecklist: PublishGateChecklist = {
  templateId: 'tpl-1',
  ready: false,
  blockerCount: 1,
  items: [
    {
      checkCode: 'TEST_RESULTS',
      ready: true,
      blocker: true,
      messageKey: 'templates.publishGate.checkCodes.TEST_RESULTS',
      summary: 'Batch test results',
    },
    {
      checkCode: 'API_POLICY',
      ready: false,
      blocker: true,
      messageKey: 'templates.publishGate.checkCodes.API_POLICY',
      summary: 'API access',
    },
  ],
}

function mountPanel(props: { templateId?: string; releaseVersion?: string } = {}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(PublishGateReadOnlyPanel, {
    props: {
      templateId: props.templateId ?? 'tpl-1',
      releaseVersion: props.releaseVersion ?? '1.0.0',
    },
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('PublishGateReadOnlyPanel', () => {
  beforeEach(() => {
    vi.mocked(templatesApi.fetchPublishGate).mockReset()
    vi.mocked(templatesApi.fetchReleasePublishGate).mockReset()
    vi.mocked(templatesApi.fetchReleasePublishGate).mockResolvedValue(readyChecklist)
  })

  it('P1-3-A: loads release-scoped publish-gate and renders read-only checklist with live-evaluation label', async () => {
    const wrapper = mountPanel({ releaseVersion: '1.0.0' })
    await flushPromises()

    expect(templatesApi.fetchReleasePublishGate).toHaveBeenCalledWith('tpl-1', '1.0.0')
    expect(templatesApi.fetchPublishGate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Current pre-release checks evaluation')
    expect(wrapper.text()).toContain('Live evaluation of this published release version')
    expect(wrapper.text()).toContain('not a historical publish-time snapshot')
    expect(wrapper.text()).toContain('Batch test results')
    expect(wrapper.text()).toContain('API access')
    expect(wrapper.text()).toContain('Ready')
    expect(wrapper.text()).toContain('Pending')
    expect(wrapper.text()).not.toContain('Confirm go-live')
    expect(wrapper.text()).not.toContain('Submit for approval')
  })

  it('P1-3-B: gate load failure shows LoadErrorPanel and retry reloads', async () => {
    vi.mocked(templatesApi.fetchReleasePublishGate)
      .mockRejectedValueOnce(new Error('gate failed'))
      .mockResolvedValueOnce(readyChecklist)
    const wrapper = mountPanel()
    await flushPromises()

    const errorPanel = wrapper.findComponent({ name: 'LoadErrorPanel' })
    expect(errorPanel.exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Batch test results')

    await errorPanel.vm.$emit('retry')
    await flushPromises()

    expect(templatesApi.fetchReleasePublishGate).toHaveBeenCalledTimes(2)
    expect(templatesApi.fetchPublishGate).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Batch test results')
  })

  it('P1-3-C: empty checklist shows honest empty copy without fake green items', async () => {
    vi.mocked(templatesApi.fetchReleasePublishGate).mockResolvedValue({
      templateId: 'tpl-1',
      ready: true,
      blockerCount: 0,
      items: [],
    })
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('No pre-release checks returned')
    expect(wrapper.text()).not.toContain('Ready')
  })

  it('P1-3-D: reloads when releaseVersion changes', async () => {
    const wrapper = mountPanel({ releaseVersion: '1.0.0' })
    await flushPromises()

    await wrapper.setProps({ releaseVersion: '2.0.0' })
    await flushPromises()

    expect(templatesApi.fetchReleasePublishGate).toHaveBeenNthCalledWith(1, 'tpl-1', '1.0.0')
    expect(templatesApi.fetchReleasePublishGate).toHaveBeenNthCalledWith(2, 'tpl-1', '2.0.0')
    expect(templatesApi.fetchPublishGate).not.toHaveBeenCalled()
  })
})