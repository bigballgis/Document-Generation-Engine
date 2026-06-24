import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditConsoleView from '@/views/audit/AuditConsoleView.vue'
import en from '@/i18n/locales/en'
import * as auditApi from '@/api/audit'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/audit', () => ({
  listManagementEvents: vi.fn(),
  listLifecycleEvents: vi.fn(),
  exportManagementEvents: vi.fn(),
  exportLifecycleEvents: vi.fn(),
}))

describe('AuditConsoleView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(auditApi.listManagementEvents).mockReset()
    vi.mocked(auditApi.listLifecycleEvents).mockReset()
  })

  it('renders management audit events after load', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [
        {
          eventAt: '2026-06-23T10:00:00Z',
          eventType: 'API_POLICY_UPDATED',
          templateId: 'tpl-1',
          changedAreas: ['POLICY'],
          rollback: false,
          warningCodes: [],
          actorSummary: 'admin@example.com',
          statusSummary: 'Updated',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

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
        defaultRoute: ROUTE_KEYS.auditConsole,
        visibleRoutes: [ROUTE_KEYS.auditConsole],
        expiresAt: new Date().toISOString(),
      },
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(AuditConsoleView, {
      global: { plugins: [pinia, i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('API_POLICY_UPDATED')
    expect(wrapper.text()).toContain('Audit console')
  })
})
