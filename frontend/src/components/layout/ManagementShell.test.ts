import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ManagementShell from '@/components/layout/ManagementShell.vue'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ path: '/home/audit' }),
  useRouter: () => ({ push: routerPush }),
}))

describe('ManagementShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
  })

  it('renders navigation from visible routes and user header', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: '10000001',
        displayName: 'Global Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles: ['GLOBAL_ADMIN'],
        authorizedGroupCodes: ['*'],
        defaultRoute: ROUTE_KEYS.globalGovernanceHome,
        visibleRoutes: [
          ROUTE_KEYS.globalGovernanceHome,
          ROUTE_KEYS.auditConsole,
          ROUTE_KEYS.templateAuthoringHome,
        ],
        expiresAt: new Date().toISOString(),
      },
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(ManagementShell, {
      slots: { default: '<div class="content-slot">Page content</div>' },
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Global Admin')
    expect(wrapper.text()).toContain('Audit console')
    expect(wrapper.text()).toContain('Page content')
  })
})
