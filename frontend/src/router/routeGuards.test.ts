import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as securityAuditApi from '@/api/securityAudit'
import router from '@/router/index'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementSession } from '@/types/session'

vi.mock('@/api/securityAudit')

vi.mock('@/views/LoginView.vue', () => ({
  default: { name: 'LoginViewStub', template: '<div />' },
}))
vi.mock('@/views/ForbiddenView.vue', () => ({
  default: { name: 'ForbiddenViewStub', template: '<div />' },
}))
vi.mock('@/views/dashboard/DashboardView.vue', () => ({
  default: { name: 'DashboardViewStub', template: '<div />' },
}))
vi.mock('@/views/api/ApiPolicyHomeView.vue', () => ({
  default: { name: 'ApiPolicyHomeViewStub', template: '<div />' },
}))
vi.mock('@/views/api/ApiInvocationsView.vue', () => ({
  default: { name: 'ApiInvocationsViewStub', template: '<div />' },
}))
vi.mock('@/views/api/ApiPackageSettingsShellView.vue', () => ({
  default: { name: 'ApiPackageSettingsShellViewStub', template: '<div />' },
}))
vi.mock('@/views/audit/AuditConsoleView.vue', () => ({
  default: { name: 'AuditConsoleViewStub', template: '<div />' },
}))
vi.mock('@/views/templates/TemplateListView.vue', () => ({
  default: { name: 'TemplateListViewStub', template: '<div />' },
}))

function buildSession(overrides: Partial<ManagementSession> = {}): ManagementSession {
  return {
    username: '10000003',
    displayName: 'Template Author',
    email: 'author@example.com',
    authSource: 'LOCAL',
    roles: ['DOCUMENT_AUTHOR'],
    authorizedGroupCodes: ['RETAIL'],
    defaultRoute: ROUTE_KEYS.dashboardHome,
    visibleRoutes: [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
    expiresAt: '2099-01-01T00:00:00Z',
    ...overrides,
  }
}

describe('router route guards (OPT-G5)', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.mocked(securityAuditApi.reportRouteAccessDenied).mockReset().mockResolvedValue(undefined)
    await router.push('/login')
  })

  it('redirects unauthenticated users to login with redirect query', async () => {
    await router.push('/templates')

    expect(router.currentRoute.value.name).toBe('login')
    expect(router.currentRoute.value.query.redirect).toBe('/templates')
    expect(securityAuditApi.reportRouteAccessDenied).not.toHaveBeenCalled()
  })

  it('allows deep-link when session role and visibleRoutes both permit', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    await router.push('/templates')

    expect(router.currentRoute.value.name).toBe('template-list')
    expect(securityAuditApi.reportRouteAccessDenied).not.toHaveBeenCalled()
  })

  it('redirects unauthorized deep-link to forbidden with traceId', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    await router.push('/api/policies')

    expect(router.currentRoute.value.name).toBe('forbidden')
    expect(typeof router.currentRoute.value.query.traceId).toBe('string')
    expect(String(router.currentRoute.value.query.traceId).length).toBeGreaterThan(0)
    expect(sessionStore.lastDenyTraceId).toBe(router.currentRoute.value.query.traceId)
    expect(securityAuditApi.reportRouteAccessDenied).toHaveBeenCalledTimes(1)
    expect(securityAuditApi.reportRouteAccessDenied).toHaveBeenCalledWith({
      routeKey: ROUTE_KEYS.apiPolicyManagement,
      traceId: String(router.currentRoute.value.query.traceId),
    })
  })

  it('still navigates to forbidden when route-access-denied report fails', async () => {
    vi.mocked(securityAuditApi.reportRouteAccessDenied).mockRejectedValue(new Error('network down'))

    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    await router.push('/api/policies')

    expect(router.currentRoute.value.name).toBe('forbidden')
    expect(typeof router.currentRoute.value.query.traceId).toBe('string')
    expect(sessionStore.lastDenyTraceId).toBe(router.currentRoute.value.query.traceId)
    expect(securityAuditApi.reportRouteAccessDenied).toHaveBeenCalledTimes(1)
  })

  it('denies api-policy when visibleRoutes is poisoned but role cannot manage policy', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession({
        visibleRoutes: [
          ROUTE_KEYS.dashboardHome,
          ROUTE_KEYS.templateManagement,
          ROUTE_KEYS.apiPolicyManagement,
        ],
      }),
    })

    await router.push('/api/policies')

    expect(router.currentRoute.value.name).toBe('forbidden')
    expect(securityAuditApi.reportRouteAccessDenied).toHaveBeenCalledWith({
      routeKey: ROUTE_KEYS.apiPolicyManagement,
      traceId: String(router.currentRoute.value.query.traceId),
    })
  })

  it('allows audit console for audit admin', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession({
        roles: ['AUDIT_ADMIN'],
        defaultRoute: ROUTE_KEYS.auditConsole,
        visibleRoutes: [ROUTE_KEYS.auditConsole],
      }),
    })

    await router.push('/audit')

    expect(router.currentRoute.value.name).toBe('audit-console')
  })
})
