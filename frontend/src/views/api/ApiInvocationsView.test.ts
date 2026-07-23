import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'
import TableEditMoreActions from '@/components/common/TableEditMoreActions.vue'
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

describe('ApiInvocationsView (BDD-SYS-NORM-W3-004…007)', () => {
  let activeWrapper: VueWrapper | null = null

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
    activeWrapper = mount(ApiInvocationsView, {
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
    return activeWrapper
  }

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

  afterEach(() => {
    activeWrapper?.unmount()
    activeWrapper = null
    document.body.querySelectorAll('.el-popper, .el-overlay').forEach((node) => node.remove())
  })

  it('renders a dedicated fluid invocations page with filters', { timeout: 30_000 }, async () => {
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

  describe('PQH N22 catalog row actions (BDD-PQH-N22-009/010/012)', () => {
    it('BDD-PQH-N22-009/012 — primary Open detail via TableEditMoreActions', async () => {
      const wrapper = mountView()
      await flushPromises()

      const actions = wrapper.find('[data-testid="table-edit-more-actions"]')
      expect(actions.exists()).toBe(true)
      const edit = actions.find('.table-edit-more-actions__edit')
      expect(edit.exists()).toBe(true)
      expect(edit.text()).toContain(en.apiPolicy.invocationsPage.openDetail)

      await edit.trigger('click')
      await flushPromises()

      expect(wrapper.findComponent({ name: 'InvocationSummaryDrawer' }).exists()).toBe(true)
    })

    it('BDD-PQH-N22-010 — Open settings under More navigates to package settings', async () => {
      const wrapper = mountView()
      await flushPromises()

      const actions = wrapper.find('[data-testid="table-edit-more-actions"]')
      const moreButton = actions.findAll('button').find((button) => button.text().includes('More'))
      expect(moreButton).toBeDefined()
      await moreButton!.trigger('click')
      await flushPromises()
      expect(document.body.querySelector('[data-testid="api-invocations-open-settings"]')).toBeTruthy()

      const dropdown = wrapper.findComponent(TableEditMoreActions).findComponent({ name: 'ElDropdown' })
      await dropdown.vm.$emit('command', 'openSettings')
      await flushPromises()

      expect(routerPush).toHaveBeenCalledWith('/api/packages/tpl-1/settings')
    })
  })
})
