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
vi.mock('@/views/governance/SurfaceRetiredView.vue', () => ({
  default: {
    name: 'SurfaceRetiredViewStub',
    template: '<div data-testid="surface-retired-view" />',
  },
}))
vi.mock('@/views/dashboard/DashboardView.vue', () => ({
  default: { name: 'DashboardViewStub', template: '<div />' },
}))
vi.mock('@/views/legalHold/LegalHoldListView.vue', () => ({
  default: {
    name: 'LegalHoldListViewStub',
    template: '<div data-testid="legal-hold-list-view" />',
  },
}))
vi.mock('@/views/masters/MasterListView.vue', () => ({
  default: { name: 'MasterListViewStub', template: '<div />' },
}))

function buildSession(overrides: Partial<ManagementSession> = {}): ManagementSession {
  return {
    username: '10000001',
    displayName: 'Global Admin',
    email: 'admin@example.com',
    authSource: 'LOCAL',
    roles: ['GLOBAL_ADMIN'],
    authorizedGroupCodes: ['*'],
    defaultRoute: ROUTE_KEYS.dashboardHome,
    visibleRoutes: [
      ROUTE_KEYS.dashboardHome,
      ROUTE_KEYS.legalHoldAdministration,
      ROUTE_KEYS.masterManagement,
    ],
    expiresAt: '2099-01-01T00:00:00Z',
    capabilities: {
      manageLegalHold: true,
    } as never,
    ...overrides,
  }
}

describe('SYS-NORM D1 brand/entity route hard retirement (BDD-SYS-NORM-D1-006/016)', () => {
  beforeEach(async () => {
    setActivePinia(createPinia())
    vi.mocked(securityAuditApi.reportRouteAccessDenied).mockReset().mockResolvedValue(undefined)
    await router.push('/login')
  })

  it('serves honest gone page for legacy document-brands bookmark (not catalog)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    await router.push('/governance/document-brands')

    expect(router.currentRoute.value.name).toBe('document-brand-surface-retired')
    expect(router.currentRoute.value.meta.logicalRoute).toBeUndefined()
    expect(securityAuditApi.reportRouteAccessDenied).not.toHaveBeenCalled()
  })

  it('serves honest gone page for legacy legal-entities bookmark (not catalog)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    await router.push('/governance/legal-entities')

    expect(router.currentRoute.value.name).toBe('legal-entity-surface-retired')
    expect(router.currentRoute.value.meta.logicalRoute).toBeUndefined()
    expect(securityAuditApi.reportRouteAccessDenied).not.toHaveBeenCalled()
  })

  it('keeps Legal holds product route available (BDD-SYS-NORM-D1-016)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: buildSession(),
    })

    await router.push('/governance/legal-holds')

    expect(router.currentRoute.value.name).toBe('legal-hold-administration')
    expect(router.currentRoute.value.meta.logicalRoute).toBe(ROUTE_KEYS.legalHoldAdministration)
  })
})
