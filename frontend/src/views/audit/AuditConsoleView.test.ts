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

function mountAuditConsole(options?: {
  roles?: string[]
  username?: string
  authorizedGroupCodes?: string[]
}) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: options?.username ?? '10000004',
      displayName: 'Audit Admin',
      email: 'audit@example.com',
      authSource: 'LOCAL',
      roles: options?.roles ?? ['AUDIT_ADMIN'],
      authorizedGroupCodes: options?.authorizedGroupCodes ?? ['*'],
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

  return mount(AuditConsoleView, {
    global: {
      plugins: [pinia, i18n, ElementPlus],
    },
  })
}

describe('AuditConsoleView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(auditApi.listManagementEvents).mockReset()
    vi.mocked(auditApi.listLifecycleEvents).mockReset()
  })

  it('renders management audit events with business labels after load', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [
        {
          eventAt: '2026-06-23T10:00:00Z',
          eventType: 'COLLABORATION_TIMEOUT_ESCALATION',
          templateId: 'tpl-1',
          changedAreas: ['COLLABORATION'],
          rollback: false,
          warningCodes: [],
          actorSummary: 'admin@example.com',
          statusSummary: 'Overdue reminder sent',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountAuditConsole()
    await flushPromises()

    expect(wrapper.text()).toContain('Overdue reminder sent')
    expect(wrapper.text()).not.toContain('COLLABORATION_TIMEOUT_ESCALATION')
    expect(wrapper.text()).toContain('Activity log')
    expect(wrapper.text()).not.toContain('Audit console')
  })

  it('shows view-only banner in page header for audit admin', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountAuditConsole()
    await flushPromises()

    expect(wrapper.find('#journey-section').exists()).toBe(false)
    expect(wrapper.text()).toContain('View only — no actions')
    expect(wrapper.text()).toContain('Activity log')
  })

  it('formats lifecycle PUBLISH as a business label in the table', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    vi.mocked(auditApi.listLifecycleEvents).mockResolvedValue({
      events: [
        {
          eventAt: '2026-06-24T10:00:00Z',
          eventType: 'PUBLISH',
          templateId: 'tpl-2',
          fromState: 'PENDING_RELEASE',
          toState: 'PUBLISHED',
          summary: 'Template go-live confirmed on behalf of team lead',
          warningCodes: [],
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountAuditConsole()
    await flushPromises()

    const lifecycleTab = wrapper
      .findAll('.el-tabs__item')
      .find((tab) => tab.text().includes('Template workflow activity'))
    expect(lifecycleTab).toBeDefined()
    await lifecycleTab!.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('Template go-live')
    expect(wrapper.text()).not.toMatch(/\bPUBLISH\b/)
    expect(wrapper.text()).toContain('Template go-live confirmed on behalf of team lead')
  })

  it('does not show view-only banner for global admin without AUDIT_ADMIN role', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountAuditConsole({
      roles: ['GLOBAL_ADMIN'],
      username: '10000001',
    })
    await flushPromises()

    expect(wrapper.text()).not.toContain('View only — no actions')
  })

  it('blocks group admin filter apply until template id is provided', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountAuditConsole({
      roles: ['GROUP_ADMIN'],
      username: '10000002',
      authorizedGroupCodes: ['RETAIL'],
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Enter a template ID before querying audit events.')
    expect(auditApi.listManagementEvents).not.toHaveBeenCalled()
  })
})
