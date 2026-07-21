import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import SurfaceRetiredView from '@/views/governance/SurfaceRetiredView.vue'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

describe('SurfaceRetiredView (BDD-SYS-NORM-D1-006/017/019)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
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
        defaultRoute: ROUTE_KEYS.dashboardHome,
        visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.masterManagement],
        expiresAt: new Date().toISOString(),
      },
    })
  })

  async function mountView(surface: 'document-brands' | 'legal-entities') {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div />' } },
        {
          path: '/governance/retired',
          component: SurfaceRetiredView,
          props: { surface },
        },
        { path: '/masters', name: 'master-list', component: { template: '<div />' } },
        {
          path: '/governance/legal-holds',
          name: 'legal-hold-administration',
          component: { template: '<div />' },
        },
      ],
    })
    await router.push('/governance/retired')
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(SurfaceRetiredView, {
      props: { surface },
      global: {
        plugins: [createPinia(), i18n, ElementPlus, router],
      },
    })
  }

  it('explains document-brands retirement and points to Letterhead (master)', async () => {
    const wrapper = await mountView('document-brands')

    expect(wrapper.find('[data-testid="surface-retired-view"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Document brands catalog retired')
    expect(wrapper.text()).toContain('Letterhead')
    expect(wrapper.find('[data-testid="surface-retired-letterhead-link"]').exists()).toBe(true)
  })

  it('explains legal-entities retirement and keeps Legal holds path visible', async () => {
    const wrapper = await mountView('legal-entities')

    expect(wrapper.text()).toContain('Legal entities catalog retired')
    expect(wrapper.find('[data-testid="surface-retired-legal-holds-link"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Legal holds')
  })
})
