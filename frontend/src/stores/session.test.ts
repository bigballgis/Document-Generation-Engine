import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

describe('session store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('evaluates route visibility from session summary', () => {
    const store = useSessionStore()
    store.$patch({
      accessToken: 'token',
      session: {
        username: '10000003',
        displayName: 'Template Author',
        email: 'author@example.com',
        authSource: 'LOCAL',
        roles: ['TEMPLATE_AUTHOR'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: ROUTE_KEYS.templateAuthoringHome,
        visibleRoutes: [ROUTE_KEYS.templateAuthoringHome],
        expiresAt: new Date().toISOString(),
      },
    })

    expect(store.canAccessRoute(ROUTE_KEYS.templateAuthoringHome)).toBe(true)
    expect(store.canAccessRoute(ROUTE_KEYS.globalGovernanceHome)).toBe(false)
    expect(store.canAccessRoute(ROUTE_KEYS.masterManagement)).toBe(true)
    expect(store.defaultHomePath()).toBe('/home/template-authoring')
  })

  it('clears persisted token on logout', async () => {
    localStorage.setItem('docgen.accessToken', 'token')
    const store = useSessionStore()
    store.$patch({
      accessToken: 'token',
      session: {
        username: '10000001',
        displayName: 'Global Admin',
        email: 'global.admin@example.com',
        authSource: 'LOCAL',
        roles: ['GLOBAL_ADMIN'],
        authorizedGroupCodes: ['*'],
        defaultRoute: ROUTE_KEYS.globalGovernanceHome,
        visibleRoutes: [ROUTE_KEYS.globalGovernanceHome],
        expiresAt: new Date().toISOString(),
      },
    })

    await store.logout()

    expect(store.authenticated).toBe(false)
    expect(localStorage.getItem('docgen.accessToken')).toBeNull()
  })
})
