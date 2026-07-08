import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { TemplateDetail } from '@/types/template'

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn(),
}))

describe('TemplateDetailOverviewTab', () => {
  const template: TemplateDetail = {
    id: 'tpl-1',
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Loan agreement',
    description: 'Retail loan pack',
    masterId: 'master-1',
    lifecycleStatus: 'DRAFT',
    releaseVersion: null,
    devVersionId: 'dev-1',
    devVersionNumber: 1,
    createdAt: '2026-06-23T09:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
    variables: [],
    bindings: [],
    rules: [],
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Loan Offer Base',
      description: null,
      status: 'APPROVED',
      originalFilename: 'master.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: 'admin',
      updatedBy: 'admin',
      createdAt: '2026-06-23T08:00:00Z',
      updatedAt: '2026-06-23T09:00:00Z',
    })
  })

  function mountTab() {
    const pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: 'admin',
        displayName: 'Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles: ['GLOBAL_ADMIN'],
        authorizedGroupCodes: ['*'],
        defaultRoute: ROUTE_KEYS.templateManagement,
        visibleRoutes: [ROUTE_KEYS.templateManagement, ROUTE_KEYS.masterManagement],
        expiresAt: new Date().toISOString(),
      },
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TemplateDetailOverviewTab, {
      props: {
        template,
        formatDateTime: (value: string) => value,
      },
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a class="router-link-stub"><slot /></a>',
          },
        },
      },
    })
  }

  it('renders template summary fields', async () => {
    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.text()).toContain('TPL-001')
    expect(wrapper.text()).toContain('Loan Offer Base')
    expect(wrapper.text()).not.toContain('master-1')
    expect(wrapper.text()).toContain('Retail loan pack')
    expect(mastersApi.getMaster).toHaveBeenCalledWith('master-1')
  })

  it('falls back to master id when master lookup fails', async () => {
    vi.mocked(mastersApi.getMaster).mockRejectedValue(new Error('not found'))

    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.text()).toContain('master-1')
  })
})
