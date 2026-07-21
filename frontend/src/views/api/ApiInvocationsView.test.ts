import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import ApiInvocationsView from '@/views/api/ApiInvocationsView.vue'

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => ({ query: {} }),
}))

vi.mock('@/api/apiPolicy', () => ({
  listInvocations: vi.fn(),
  getInvocationDetail: vi.fn(),
}))

vi.mock('@/api/templates', () => ({
  listAllTemplates: vi.fn(),
  listTemplates: vi.fn(),
}))

function mountView() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000000',
      displayName: 'Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.apiPolicyManagement,
      visibleRoutes: [ROUTE_KEYS.apiPolicyManagement],
      expiresAt: new Date().toISOString(),
      capabilities: { manageApiPolicy: true },
    },
  })
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(ApiInvocationsView, {
    global: { plugins: [pinia, i18n, ElementPlus] },
  })
}

describe('ApiInvocationsView (BDD-SYS-NORM-W3-004…007)', () => {
  beforeEach(() => {
    routerPush.mockReset()
    vi.mocked(apiPolicyApi.listInvocations).mockReset()
    vi.mocked(templatesApi.listAllTemplates).mockReset()
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'P1',
          groupCode: 'RETAIL',
          name: 'Retail letter',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'm1',
          updatedBy: 'admin',
          updatedAt: '2026-07-20T00:00:00Z',
        },
      ],
      totalElements: 1,
      truncated: false,
    })
    vi.mocked(apiPolicyApi.listInvocations).mockResolvedValue({
      content: [
        {
          invocationId: 'inv-1',
          invocationKind: 'SINGLE',
          status: 'SUCCEEDED',
          requestId: 'req-1',
          resolvedReleaseVersion: '1.0.0',
          routeType: 'DEFAULT',
          createdAt: '2026-07-20T12:00:00Z',
          accessAccountSummary: 'acct',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
  })

  it('renders a dedicated fluid invocations page with filters', async () => {
    const wrapper = mountView()
    await flushPromises()

    expect(wrapper.find('[data-testid="api-invocations-filters"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="api-invocations-table-card"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Invocation records')
    expect(wrapper.text()).toContain('Retail letter')
    expect(wrapper.text()).not.toContain('variables')
  })

  it('shows honest empty state when no invocations match', async () => {
    vi.mocked(apiPolicyApi.listInvocations).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.text()).toContain('No invocation records')
  })

  it('shows LoadErrorPanel when composition fails', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockRejectedValue(new Error('boom'))
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.findComponent({ name: 'LoadErrorPanel' }).exists()).toBe(true)
  })
})
