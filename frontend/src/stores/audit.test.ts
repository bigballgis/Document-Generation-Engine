import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as auditApi from '@/api/audit'
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
})
