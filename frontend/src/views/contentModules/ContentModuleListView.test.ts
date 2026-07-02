import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleListView from '@/views/contentModules/ContentModuleListView.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/contentModules', () => ({
  listContentModules: vi.fn(),
  getContentModule: vi.fn(),
  createContentModule: vi.fn(),
}))

vi.mock('@/stores/identity', () => ({
  useIdentityStore: () => ({
    groups: [],
    fetchGroups: vi.fn(),
  }),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

function patchAuthorSession() {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000003',
      displayName: 'Author',
      email: 'author@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: 'route.dashboard-home',
      visibleRoutes: ['route.dashboard-home', 'route.content-module-management'],
      expiresAt: '2099-01-01T00:00:00Z',
    },
  })
}

describe('ContentModuleListView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchAuthorSession()
    routerPush.mockReset()
    vi.mocked(contentModulesApi.listContentModules).mockReset()
  })

  it('renders content modules across authorized groups', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue([
      {
        moduleId: 'MOD-LOAN-DISCLOSURE',
        moduleCode: 'MOD-LOAN-DISCLOSURE',
        groupCode: 'RETAIL',
        name: 'Loan disclosure',
        createdAt: '2026-06-26T10:00:00Z',
        updatedAt: '2026-06-26T10:00:00Z',
      },
    ])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()
    await flushPromises()

    expect(contentModulesApi.listContentModules).toHaveBeenCalled()
    expect(wrapper.text()).toContain('Loan disclosure')
    expect(wrapper.text()).toContain('MOD-LOAN-DISCLOSURE')
    expect(wrapper.text()).toContain('RETAIL')
  })

  it('shows load error with retry when list fails', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockRejectedValue(new Error('network'))

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('Unable to load content modules')
    expect(wrapper.text()).toContain('Retry')
  })

  it('filters rows when a column filter is applied', async () => {
    vi.mocked(contentModulesApi.listContentModules).mockResolvedValue([
      {
        moduleId: 'MOD-LOAN-DISCLOSURE',
        moduleCode: 'MOD-LOAN-DISCLOSURE',
        groupCode: 'RETAIL',
        name: 'Loan disclosure',
        createdAt: '2026-06-26T10:00:00Z',
        updatedAt: '2026-06-26T10:00:00Z',
      },
      {
        moduleId: 'MOD-FEE-SCHEDULE',
        moduleCode: 'MOD-FEE-SCHEDULE',
        groupCode: 'CORPORATE',
        name: 'Fee schedule',
        createdAt: '2026-06-26T10:00:00Z',
        updatedAt: '2026-06-26T10:00:00Z',
      },
    ])

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ContentModuleListView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('Loan disclosure')
    expect(wrapper.text()).toContain('Fee schedule')

    const groupFilter = wrapper.find('.table-column-header__control input')
    await groupFilter.setValue('RETAIL')
    await flushPromises()

    expect(wrapper.text()).toContain('Loan disclosure')
    expect(wrapper.text()).not.toContain('Fee schedule')
  })
})
