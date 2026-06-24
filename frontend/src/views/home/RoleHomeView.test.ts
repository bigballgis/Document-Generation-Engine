import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RoleHomeView from '@/views/home/RoleHomeView.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'
import * as templatesApi from '@/api/templates'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn(),
}))

vi.mock('@/api/templates', () => ({
  listTemplates: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

describe('RoleHomeView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    vi.mocked(mastersApi.listMasters).mockReset()
    vi.mocked(templatesApi.listTemplates).mockReset()
  })

  it('renders governance dashboard stats after load', async () => {
    vi.mocked(mastersApi.listMasters).mockResolvedValue([
      {
        id: 'master-1',
        groupCode: 'RETAIL',
        name: 'Retail letterhead',
        status: 'PENDING_REVIEW',
        originalFilename: 'letterhead.docx',
        anchorCount: 2,
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
      },
    ])
    vi.mocked(templatesApi.listTemplates).mockResolvedValue([
      {
        id: 'tpl-1',
        externalId: 'TPL-1',
        groupCode: 'RETAIL',
        name: 'Retail letter',
        lifecycleStatus: 'TESTING',
        releaseVersion: null,
        releaseVersionCount: 0,
        masterId: 'master-1',
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
      },
      {
        id: 'tpl-2',
        externalId: 'TPL-2',
        groupCode: 'RETAIL',
        name: 'Retail notice',
        lifecycleStatus: 'PUBLISHED',
        releaseVersion: '1.0.0',
        releaseVersionCount: 1,
        masterId: 'master-1',
        updatedBy: '10000001',
        updatedAt: '2026-06-23T10:00:00Z',
      },
    ])

    const pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: '10000001',
        displayName: 'Group Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles: ['GROUP_ADMIN'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: ROUTE_KEYS.groupGovernanceHome,
        visibleRoutes: [
          ROUTE_KEYS.groupGovernanceHome,
          ROUTE_KEYS.masterManagement,
          ROUTE_KEYS.templateManagement,
        ],
        expiresAt: new Date().toISOString(),
      },
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(RoleHomeView, {
      props: {
        routeKey: ROUTE_KEYS.groupGovernanceHome,
        titleKey: 'home.groupGovernance.title',
        descriptionKey: 'home.groupGovernance.description',
      },
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Group governance')
    expect(wrapper.text()).toContain('Governance overview')
    expect(wrapper.text()).toContain('Masters pending review')
  })
})
