import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import {
  GROUPS_CATALOG_PATH,
  USERS_CATALOG_PATH,
  useEntityLinkTargets,
} from '@/composables/useEntityLinkTargets'
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

  it('returns group catalog link when identity administration is visible', () => {
    patchSession([ROUTE_KEYS.identityAdministration])
    const { groupCatalogLink } = useEntityLinkTargets()
    expect(groupCatalogLink('RETAIL')).toEqual({
      path: GROUPS_CATALOG_PATH,
      query: { q: 'RETAIL' },
    })
    expect(groupCatalogLink('*')).toBeUndefined()
    expect(groupCatalogLink('')).toBe(GROUPS_CATALOG_PATH)
  })

  it('omits group catalog link when identity administration is not visible', () => {
    patchSession([ROUTE_KEYS.dashboardHome])
    const { groupCatalogLink } = useEntityLinkTargets()
    expect(groupCatalogLink('RETAIL')).toBeUndefined()
  })

  it('returns user catalog link with q prefill when identity administration is visible', () => {
    patchSession([ROUTE_KEYS.identityAdministration])
    const { userCatalogLink } = useEntityLinkTargets()
    expect(userCatalogLink('10000001')).toEqual({
      path: USERS_CATALOG_PATH,
      query: { q: '10000001' },
    })
    expect(userCatalogLink('')).toBeUndefined()
    expect(userCatalogLink('   ')).toBeUndefined()
    expect(userCatalogLink(null)).toBeUndefined()
  })

  it('omits user catalog link when identity administration is not visible', () => {
    patchSession([ROUTE_KEYS.dashboardHome])
    const { userCatalogLink } = useEntityLinkTargets()
    expect(userCatalogLink('10000001')).toBeUndefined()
  })

  it('returns task entity link gated by source domain (N1)', () => {
    patchSession([ROUTE_KEYS.masterManagement])
    const { taskEntityLink } = useEntityLinkTargets()
    expect(taskEntityLink({ path: '/masters/m-1', source: 'master' })).toBe('/masters/m-1')
    expect(taskEntityLink({ path: '/templates/t-1', source: 'template' })).toBeUndefined()
  })
})
