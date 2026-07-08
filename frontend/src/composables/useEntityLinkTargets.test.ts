import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { useSessionStore } from '@/stores/session'
import type { ManagementSession } from '@/types/session'

function patchSession(visibleRoutes: string[]) {
  const sessionStore = useSessionStore()
  const session: ManagementSession = {
    username: '10000000',
    displayName: 'Admin',
    email: 'admin@example.com',
    authSource: 'LOCAL',
    roles: ['GLOBAL_ADMIN'],
    authorizedGroupCodes: ['*'],
    defaultRoute: ROUTE_KEYS.dashboardHome,
    visibleRoutes,
    expiresAt: new Date().toISOString(),
  }
  sessionStore.$patch({ accessToken: 'token', session })
}

describe('useEntityLinkTargets', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('returns template detail link when route is visible', () => {
    patchSession([ROUTE_KEYS.templateManagement])
    const { templateDetailLink } = useEntityLinkTargets()
    expect(templateDetailLink('tpl-1')).toBe('/templates/tpl-1')
  })

  it('omits template detail link when route is not visible', () => {
    patchSession([ROUTE_KEYS.dashboardHome])
    const { templateDetailLink } = useEntityLinkTargets()
    expect(templateDetailLink('tpl-1')).toBeUndefined()
  })

  it('returns master and content module links when routes are visible', () => {
    patchSession([ROUTE_KEYS.masterManagement, ROUTE_KEYS.contentModuleManagement])
    const { masterDetailLink, contentModuleDetailLink } = useEntityLinkTargets()
    expect(masterDetailLink('master-1')).toBe('/masters/master-1')
    expect(contentModuleDetailLink('mod-1')).toBe('/content-modules/mod-1')
  })
})
