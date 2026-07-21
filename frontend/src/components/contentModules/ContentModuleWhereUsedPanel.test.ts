import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleWhereUsedPanel from '@/components/contentModules/ContentModuleWhereUsedPanel.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/contentModules', () => ({
  listContentModuleWhereUsed: vi.fn(),
}))

describe('ContentModuleWhereUsedPanel', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.mocked(contentModulesApi.listContentModuleWhereUsed).mockReset()
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: 'author',
        displayName: 'Author',
        email: 'author@example.com',
        authSource: 'LOCAL',
        roles: ['DOCUMENT_AUTHOR'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: ROUTE_KEYS.dashboardHome,
        visibleRoutes: [ROUTE_KEYS.templateManagement, ROUTE_KEYS.contentModuleManagement],
        expiresAt: '2099-01-01T00:00:00Z',
      },
    })
  })

  function mountPanel() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(ContentModuleWhereUsedPanel, {
      props: { moduleId: 'MOD-1' },
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

  it('renders where-used templates with entity links', async () => {
    vi.mocked(contentModulesApi.listContentModuleWhereUsed).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'TPL-1',
          name: 'Loan Notice',
          groupCode: 'RETAIL',
          lifecycleStatus: 'PUBLISHED',
          pinnedSemanticVersion: '1.0.0',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(contentModulesApi.listContentModuleWhereUsed).toHaveBeenCalledWith('MOD-1', 0, 20)
    expect(wrapper.find('[data-testid="content-module-where-used-table"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Loan Notice')
    expect(wrapper.text()).toContain('1.0.0')
  })

  it('shows empty state when no templates reference the module', async () => {
    vi.mocked(contentModulesApi.listContentModuleWhereUsed).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="content-module-where-used-empty"]').exists()).toBe(true)
  })
})
