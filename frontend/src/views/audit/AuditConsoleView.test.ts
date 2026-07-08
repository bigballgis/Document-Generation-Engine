import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuditConsoleView from '@/views/audit/AuditConsoleView.vue'
import en from '@/i18n/locales/en'
import * as auditApi from '@/api/audit'
import * as templatesApi from '@/api/templates'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import { useAuditStore } from '@/stores/audit'

const { routeState } = vi.hoisted(() => ({
  routeState: {
    query: {} as Record<string, string | string[] | undefined>,
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
}))

vi.mock('@/api/audit', () => ({
  listManagementEvents: vi.fn(),
  listLifecycleEvents: vi.fn(),
  exportManagementEvents: vi.fn(),
  exportLifecycleEvents: vi.fn(),
}))

vi.mock('@/api/templates', () => ({
  listTemplates: vi.fn(),
}))

function mountAuditConsole(options?: {
  roles?: string[]
  username?: string
  authorizedGroupCodes?: string[]
  visibleRoutes?: string[]
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
      visibleRoutes: options?.visibleRoutes ?? [ROUTE_KEYS.auditConsole],
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
      stubs: {
        RouterLink: {
          template: '<a class="router-link-stub"><slot /></a>',
        },
      },
    },
  })
}

describe('AuditConsoleView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routeState.query = {}
    vi.mocked(auditApi.listManagementEvents).mockReset()
    vi.mocked(auditApi.listLifecycleEvents).mockReset()
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
  })

  it('renders management audit events with business labels after load', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [
        {
          eventAt: '2026-06-23T10:00:00Z',
          eventType: 'COLLABORATION_TIMEOUT_ESCALATION',
          templateId: 'tpl-1',
          templateDisplayName: 'Loan agreement',
          templateExternalId: 'TPL-001',
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

    const wrapper = mountAuditConsole({
      roles: ['GLOBAL_ADMIN'],
      visibleRoutes: [ROUTE_KEYS.auditConsole, ROUTE_KEYS.templateManagement],
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Overdue reminder sent')
    expect(wrapper.text()).not.toContain('COLLABORATION_TIMEOUT_ESCALATION')
    expect(wrapper.text()).toContain('Activity log')
    expect(wrapper.text()).toContain('Loan agreement')
    expect(wrapper.text()).toContain('TPL-001')
    expect(wrapper.find('.router-link-stub').exists()).toBe(true)
  })

  it('renders template names as plain text when user cannot access template routes', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [
        {
          eventAt: '2026-06-23T10:00:00Z',
          eventType: 'COLLABORATION_TIMEOUT_ESCALATION',
          templateId: 'tpl-1',
          templateDisplayName: 'Loan agreement',
          templateExternalId: 'TPL-001',
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

    const wrapper = mountAuditConsole({ roles: ['AUDIT_ADMIN'] })
    await flushPromises()

    expect(wrapper.text()).toContain('Loan agreement')
    expect(wrapper.find('.router-link-stub').exists()).toBe(false)
    expect(wrapper.find('.entity-link-cell__text').exists()).toBe(true)
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
          templateDisplayName: 'Retail pack',
          fromState: 'PENDING_RELEASE',
          toState: 'PUBLISHED',
          actorDisplayName: 'Jane Doe',
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
    expect(wrapper.text()).toContain('Jane Doe')
    expect(wrapper.text()).not.toContain('tpl-2')
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

    expect(wrapper.text()).toContain('Select a template before querying audit events.')
    expect(auditApi.listManagementEvents).not.toHaveBeenCalled()
  })

  it('uses searchable selects for event type and template filters', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'TPL-001',
          groupCode: 'RETAIL',
          name: 'Loan agreement',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0',
          releaseVersionCount: 1,
          masterId: 'master-1',
          updatedBy: 'admin',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountAuditConsole({
      roles: ['GROUP_ADMIN'],
      username: '10000002',
      authorizedGroupCodes: ['RETAIL'],
    })
    await flushPromises()

    const selects = wrapper.findAll('.el-select')
    expect(selects.length).toBeGreaterThanOrEqual(2)
    expect(wrapper.text()).toContain('Filter by event type')
    expect(wrapper.text()).toContain('Search by name or external ID')
    expect(templatesApi.listTemplates).toHaveBeenCalled()
  })

  it('uses fluid page layout without max width constraint', async () => {
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const wrapper = mountAuditConsole()
    await flushPromises()

    const layout = wrapper.find('.app-page-layout')
    const style = layout.attributes('style') ?? ''
    expect(style).not.toContain('max-width')
  })

  it('prefills requestId filter from route query on mount', async () => {
    routeState.query = { requestId: 'req-deep-link-001' }
    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    mountAuditConsole()
    await flushPromises()

    const auditStore = useAuditStore()
    expect(auditStore.filters.requestId).toBe('req-deep-link-001')
    expect(auditApi.listManagementEvents).toHaveBeenCalledWith(
      expect.objectContaining({ requestId: 'req-deep-link-001' }),
      expect.any(Object),
    )
  })
})
