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
const routeState = { path: '/audit', query: {} as Record<string, string>, hash: '' }

vi.mock('vue-router', () => ({
  useRoute: () => routeState,
  useRouter: () => ({ push: routerPush }),
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

function mountShell(session: NonNullable<ReturnType<typeof useSessionStore>['session']>) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session,
  })

  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(ManagementShell, {
    slots: { default: '<div class="content-slot">Page content</div>' },
    global: {
      plugins: [pinia, i18n, ElementPlus],
    },
  })
}

describe('ManagementShell', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    routerPush.mockReset()
    routeState.path = '/audit'
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
    expect(wrapper.text()).toContain('Document content')
    expect(wrapper.text()).toContain('Letterhead templates')
    expect(wrapper.text()).toContain('Templates')
    expect(wrapper.text()).toContain('Activity log')
    expect(wrapper.text()).toContain('Page content')
  })

  it('renders myTodos behavior group for tester with only testing entry', async () => {
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

    expect(wrapper.text()).toContain('My to-dos')
    expect(wrapper.text()).toContain('Waiting on my testing')
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

  it('navigates to dashboard with queue query and hash when behavior entry is clicked', async () => {
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
    expect(testingButton).toBeDefined()
    await testingButton!.trigger('click')

    expect(routerPush).toHaveBeenCalledWith({
      path: '/dashboard',
      query: { queue: 'TEST' },
      hash: '#tasks-section',
    })
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
})
