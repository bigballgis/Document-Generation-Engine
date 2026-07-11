import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ManagementShell from '@/components/layout/ManagementShell.vue'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'

const routerPush = vi.fn()
const routeState = {
  path: '/audit',
  fullPath: '/audit',
  query: {} as Record<string, string>,
  hash: '',
}

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/api/auth')

vi.mock('@/api/collaboration', () => ({
  getCollaborationNotificationUnreadCount: vi.fn().mockResolvedValue({ unreadCount: 0 }),
  listCollaborationNotifications: vi.fn().mockResolvedValue([]),
  markCollaborationNotificationRead: vi.fn().mockResolvedValue({ unreadCount: 0 }),
  markAllCollaborationNotificationsRead: vi.fn().mockResolvedValue({ unreadCount: 0 }),
  listCollaborationWorkItems: vi.fn().mockResolvedValue([]),
  getCollaborationTimeoutConfig: vi.fn(),
  upsertCollaborationTimeoutConfig: vi.fn(),
}))

const globalAdminCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: true,
  authorContentModules: true,
  decideContentModuleReviews: true,
  manageContentModuleLifecycle: true,
  manageApiPolicy: true,
  readAudit: true,
}

const testerCapabilities: ManagementCapabilities = {
  ...globalAdminCapabilities,
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: true,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: false,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
}

function mountShell(
  session: NonNullable<ReturnType<typeof useSessionStore>['session']>,
  storeOverrides: Partial<{
    accessTokenExpiresAt: string | null
    sessionAbsoluteDeadline: string | null
  }> = {},
  mountOptions: { attachToDocument?: boolean } = {},
) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session,
    ...storeOverrides,
  })

  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(ManagementShell, {
    slots: { default: '<div class="content-slot">Page content</div>' },
    attachTo: mountOptions.attachToDocument ? document.body : undefined,
    global: {
      plugins: [pinia, i18n, ElementPlus],
    },
  })
}

function globalAdminSession(): NonNullable<ReturnType<typeof useSessionStore>['session']> {
  return {
    username: '10000001',
    displayName: 'Global Admin',
    email: 'admin@example.com',
    authSource: 'LOCAL',
    roles: ['GLOBAL_ADMIN'],
    authorizedGroupCodes: ['*'],
    defaultRoute: ROUTE_KEYS.dashboardHome,
    visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.auditConsole],
    capabilities: globalAdminCapabilities,
    expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
  }
}

describe('ManagementShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    routeState.path = '/audit'
    routeState.fullPath = '/audit'
    routeState.query = {}
    routeState.hash = ''
  })

  it('renders grouped navigation from visible routes and user header', async () => {
    const wrapper = mountShell({
      username: '10000001',
      displayName: 'Global Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [
        ROUTE_KEYS.dashboardHome,
        ROUTE_KEYS.masterManagement,
        ROUTE_KEYS.templateManagement,
        ROUTE_KEYS.apiPolicyManagement,
        ROUTE_KEYS.auditConsole,
        ROUTE_KEYS.identityAdministration,
      ],
      capabilities: globalAdminCapabilities,
      expiresAt: new Date().toISOString(),
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Global Admin')
    expect(wrapper.text()).toContain('Red Bank')
    expect(wrapper.text()).not.toContain('REDBC')
    expect(wrapper.text()).toContain('Document content')
    expect(wrapper.text()).toContain('Letterhead templates')
    expect(wrapper.text()).toContain('Templates')
    expect(wrapper.text()).toContain('Activity log')
    expect(wrapper.text()).toContain('Page content')
    expect(wrapper.find('.shell-page-root').exists()).toBe(true)
  })

  it('does not render myTodos behavior entries in the sidebar', async () => {
    const wrapper = mountShell({
      username: '10000002',
      displayName: 'Template Tester',
      email: 'tester@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
      capabilities: testerCapabilities,
      expiresAt: new Date().toISOString(),
    })

    await flushPromises()

    expect(wrapper.text()).not.toContain('My to-dos')
    expect(wrapper.text()).not.toContain('Waiting on my testing')
    expect(wrapper.text()).not.toContain('Waiting on my approval')
  })

  it('omits myTodos group for audit admin without behavior entries', async () => {
    const wrapper = mountShell({
      username: '10000003',
      displayName: 'Audit Admin',
      email: 'audit@example.com',
      authSource: 'LOCAL',
      roles: ['AUDIT_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.auditConsole,
      visibleRoutes: [ROUTE_KEYS.auditConsole],
      capabilities: { ...globalAdminCapabilities, readAudit: true, decideTests: false },
      expiresAt: new Date().toISOString(),
    })

    await flushPromises()

    expect(wrapper.text()).not.toContain('My to-dos')
    expect(wrapper.text()).not.toContain('Waiting on my testing')
  })

  it('does not render behavior queue shortcuts in the sidebar', async () => {
    const wrapper = mountShell({
      username: '10000002',
      displayName: 'Template Tester',
      email: 'tester@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
      capabilities: testerCapabilities,
      expiresAt: new Date().toISOString(),
    })

    await flushPromises()

    const testingButton = wrapper
      .findAll('button.nav-item')
      .find((button) => button.text() === 'Waiting on my testing')
    expect(testingButton).toBeUndefined()
  })

  it('does not render disabled stubs for hidden behavior entries', async () => {
    const wrapper = mountShell({
      username: '10000002',
      displayName: 'Template Tester',
      email: 'tester@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
      capabilities: testerCapabilities,
      expiresAt: new Date().toISOString(),
    })

    await flushPromises()

    const approvalButton = wrapper
      .findAll('button.nav-item')
      .find((button) => button.text().includes('approval'))
    expect(approvalButton).toBeUndefined()
  })

  it('exposes a skip-link as the first focusable control targeting main content', async () => {
    const wrapper = mountShell(globalAdminSession(), {}, { attachToDocument: true })
    await flushPromises()

    try {
      const skipLink = wrapper.find('a.skip-link')
      expect(skipLink.exists()).toBe(true)
      expect(skipLink.text()).toBe('Skip to main content')
      expect(skipLink.attributes('href')).toBe('#main-content')

      const main = wrapper.find('#main-content')
      expect(main.exists()).toBe(true)
      expect(main.attributes('tabindex')).toBe('-1')

      const focusable = wrapper.findAll('a, button, [tabindex]:not([tabindex="-1"])')
      expect(focusable[0].classes()).toContain('skip-link')

      await skipLink.trigger('click')
      expect(document.activeElement).toBe(main.element)
    } finally {
      wrapper.unmount()
    }
  })

  it('shows the session limit reminder when the absolute deadline is near', async () => {
    const wrapper = mountShell(globalAdminSession(), {
      accessTokenExpiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
      sessionAbsoluteDeadline: new Date(Date.now() + 5 * 60_000).toISOString(),
    })

    await flushPromises()

    expect(wrapper.find('[role="alert"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Session ending soon')
    expect(wrapper.text()).toContain('Sign in again')
  })

  it('hides the session limit reminder while far from the absolute deadline', async () => {
    const wrapper = mountShell(globalAdminSession(), {
      accessTokenExpiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
      sessionAbsoluteDeadline: new Date(Date.now() + 4 * 3_600_000).toISOString(),
    })

    await flushPromises()

    expect(wrapper.text()).not.toContain('Session ending soon')
  })

  it('reminder action signs out and redirects to login preserving the destination', async () => {
    const wrapper = mountShell(globalAdminSession(), {
      accessTokenExpiresAt: new Date(Date.now() + 5 * 60_000).toISOString(),
      sessionAbsoluteDeadline: new Date(Date.now() + 5 * 60_000).toISOString(),
    })

    await flushPromises()

    const actionButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Sign in again'))
    expect(actionButton).toBeDefined()

    await actionButton!.trigger('click')
    await flushPromises()

    expect(useSessionStore().authenticated).toBe(false)
    expect(routerPush).toHaveBeenCalledWith({ name: 'login', query: { redirect: '/audit' } })
  })

  it('shows notification bell when session can view collaboration work items', async () => {
    const wrapper = mountShell(globalAdminSession())
    await flushPromises()
    expect(wrapper.find('[data-testid="notification-bell"]').exists()).toBe(true)
  })

  it('hides notification bell when collaboration capability is absent', async () => {
    const wrapper = mountShell({
      username: '10000004',
      displayName: 'Audit Only',
      email: 'audit@example.com',
      authSource: 'LOCAL',
      roles: ['AUDIT_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.auditConsole,
      visibleRoutes: [ROUTE_KEYS.auditConsole],
      capabilities: {
        ...globalAdminCapabilities,
        viewCollaborationWorkItems: false,
        decideTests: false,
        decideApprovals: false,
        authorTemplates: false,
        maintainCollaborationTimeoutConfig: false,
      },
      expiresAt: new Date().toISOString(),
    })
    await flushPromises()
    expect(wrapper.find('[data-testid="notification-bell"]').exists()).toBe(false)
  })

  it('shows Help menu with replay entry (BDD-LRP-C8-003)', async () => {
    const wrapper = mountShell(globalAdminSession())
    await flushPromises()
    expect(wrapper.find('[data-testid="help-menu"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="help-menu-trigger"]').text()).toContain('Help')
  })
})
