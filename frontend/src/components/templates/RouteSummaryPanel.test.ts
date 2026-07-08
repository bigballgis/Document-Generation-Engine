import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RouteSummaryPanel from '@/components/templates/RouteSummaryPanel.vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import en from '@/i18n/locales/en'

vi.mock('@/api/apiPolicy', () => ({
  fetchRoutesSummary: vi.fn(),
}))

const sampleSummary = {
  templateId: 'tpl-1',
  externalId: 'RETAIL-ACCOUNT-OPEN',
  defaultPath: '/api/v1/templates/RETAIL-ACCOUNT-OPEN/generate',
  defaultRouteReleaseVersion: '2.1.0',
  defaultRouteStatus: 'PUBLISHED',
  explicitPaths: [
    {
      releaseVersion: '2.1.0',
      explicitVersionUrl: '/api/v1/templates/RETAIL-ACCOUNT-OPEN/versions/2.1.0/generate',
    },
    {
      releaseVersion: '2.0.0',
      explicitVersionUrl: '/api/v1/templates/RETAIL-ACCOUNT-OPEN/versions/2.0.0/generate',
    },
  ],
}

function mountPanel() {
  setActivePinia(createPinia())
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(RouteSummaryPanel, {
    props: { templateId: 'tpl-1' },
    global: {
      plugins: [createPinia(), i18n, ElementPlus],
    },
  })
}

describe('RouteSummaryPanel', () => {
  beforeEach(() => {
    vi.mocked(apiPolicyApi.fetchRoutesSummary).mockReset()
  })

  it('renders external id, default path, and default release badge', async () => {
    vi.mocked(apiPolicyApi.fetchRoutesSummary).mockResolvedValue(sampleSummary)
    const wrapper = mountPanel()
    await flushPromises()

    expect(apiPolicyApi.fetchRoutesSummary).toHaveBeenCalledWith('tpl-1')
    expect(wrapper.text()).toContain('RETAIL-ACCOUNT-OPEN')
    expect(wrapper.text()).toContain('/api/v1/templates/RETAIL-ACCOUNT-OPEN/generate')
    expect(wrapper.text()).toContain('Default release 2.1.0')
  })

  it('renders explicit paths table when provided', async () => {
    vi.mocked(apiPolicyApi.fetchRoutesSummary).mockResolvedValue(sampleSummary)
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Explicit generate paths')
    expect(wrapper.find('.paths-section').exists()).toBe(true)
  })

  it('shows load error with retry', async () => {
    vi.mocked(apiPolicyApi.fetchRoutesSummary).mockRejectedValue(new Error('network'))
    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Unable to load route summary.')
    await wrapper.find('.el-button--primary').trigger('click')
    expect(apiPolicyApi.fetchRoutesSummary).toHaveBeenCalledTimes(2)
  })
})
