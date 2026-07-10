import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApiPolicyHomeView from '@/views/api/ApiPolicyHomeView.vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import en from '@/i18n/locales/en'

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/api/apiPolicy', () => ({
  fetchAlerts: vi.fn(),
}))

const sampleAlerts = [
  {
    alertKind: 'MISSING_AD_GROUP' as const,
    templateId: 'tpl-1',
    templateName: 'Retail account open',
    templateExternalId: 'RETAIL-ACCOUNT-OPEN',
    groupCode: 'RETAIL',
  },
  {
    alertKind: 'EXPIRING_CREDENTIAL' as const,
    templateId: 'tpl-2',
    templateName: 'Mortgage approval',
    templateExternalId: 'MORTGAGE-APPROVAL',
    groupCode: 'MORTGAGE',
    credentialExternalId: 'EXT-001',
    credentialExpiresAt: '2026-07-20T00:00:00Z',
  },
]

function mountHome() {
  setActivePinia(createPinia())
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(ApiPolicyHomeView, {
    global: {
      plugins: [createPinia(), i18n, ElementPlus],
    },
  })
}

describe('ApiPolicyHomeView', () => {
  beforeEach(() => {
    routerPush.mockReset()
    vi.mocked(apiPolicyApi.fetchAlerts).mockReset()
  })

  it('renders alerts table instead of coming soon placeholder', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).not.toContain('Aggregated alerts coming soon')
    expect(wrapper.text()).toContain('Missing authorized AD group')
    expect(wrapper.text()).toContain('Access key expiring soon')
    expect(wrapper.text()).toContain('RETAIL-ACCOUNT-OPEN')
  })

  it('navigates to hub api access tab on alert row click', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue(sampleAlerts)
    const wrapper = mountHome()
    await flushPromises()

    const table = wrapper.findComponent({ name: 'AppDataTable' })
    await table.vm.$emit('row-click', sampleAlerts[0])

    expect(routerPush).toHaveBeenCalledWith('/templates/tpl-1?tab=apiAccess')
  })

  it('shows LoadErrorPanel instead of empty alerts table when fetch fails', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockRejectedValue({
      response: { status: 500, data: { error: { code: 'INTERNAL_ERROR' } } },
    })
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).not.toContain('No attention items')
    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(true)
  })

  it('offers browse templates when alerts are empty', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue([])
    const wrapper = mountHome()
    await flushPromises()

    expect(wrapper.text()).toContain('Browse templates')
    expect(wrapper.findComponent({ name: 'ElCollapse' }).exists()).toBe(false)
  })

  it('navigates to template catalog from header action', async () => {
    vi.mocked(apiPolicyApi.fetchAlerts).mockResolvedValue([])
    const wrapper = mountHome()
    await flushPromises()

    const browseButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Browse templates'))
    expect(browseButton).toBeDefined()
    await browseButton!.trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/templates')
  })
})
