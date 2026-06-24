import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as auditApi from '@/api/audit'
import { axiosEnvelopeError } from '@/test/axiosEnvelopeError'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useAuditStore } from '@/stores/audit'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/audit', () => ({
  listManagementEvents: vi.fn(),
  listLifecycleEvents: vi.fn(),
  exportManagementEvents: vi.fn(),
}))

describe('audit store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(auditApi.listManagementEvents).mockReset()
  })

  it('loads management events using resolved actor role', async () => {
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

    vi.mocked(auditApi.listManagementEvents).mockResolvedValue({
      events: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const store = useAuditStore()
    store.initializeFiltersFromSession()
    await store.fetchManagementEvents()

    expect(auditApi.listManagementEvents).toHaveBeenCalledWith(
      expect.objectContaining({ actorRole: 'GLOBAL_ADMIN' }),
    )
  })

  it('stores api error message key when group-scoped fetch fails with scope envelope', async () => {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: '10000002',
        displayName: 'Group Admin',
        email: 'group.admin@example.com',
        authSource: 'LOCAL',
        roles: ['GROUP_ADMIN'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: ROUTE_KEYS.auditConsole,
        visibleRoutes: [ROUTE_KEYS.auditConsole],
        expiresAt: new Date().toISOString(),
      },
    })

    vi.mocked(auditApi.listManagementEvents).mockRejectedValue(
      axiosEnvelopeError(422, 'api.error.audit.scopeRequired', {
        code: 'AUDIT_SCOPE_REQUIRED',
        category: 'VALIDATION',
        message: 'Group scope and template identifier are required for group-scoped audit queries.',
      }),
    )

    const store = useAuditStore()
    store.initializeFiltersFromSession()
    store.filters.groupScope = 'RETAIL'

    await expect(store.fetchManagementEvents()).rejects.toBeTruthy()

    expect(store.lastErrorMessageKey).toBe('api.error.audit.scopeRequired')
  })
})
