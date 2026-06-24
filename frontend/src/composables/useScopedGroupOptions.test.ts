import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'
import { ROUTE_KEYS } from '@/routing/routeKeys'

function patchSession(roles: string[], authorizedGroupCodes: string[]) {
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000001',
      displayName: 'Actor',
      email: 'actor@example.com',
      authSource: 'LOCAL',
      roles,
      authorizedGroupCodes,
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes: [ROUTE_KEYS.dashboardHome],
      expiresAt: new Date().toISOString(),
    },
  })
}

describe('useScopedGroupOptions', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('locks a single authorized group for non-global admins', () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    const { isGroupLocked, lockedGroupCode, groupOptions } = useScopedGroupOptions()

    expect(isGroupLocked.value).toBe(true)
    expect(lockedGroupCode.value).toBe('RETAIL')
    expect(groupOptions.value).toEqual([{ value: 'RETAIL', label: 'RETAIL' }])
  })

  it('exposes multiple authorized groups for non-global admins', () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL', 'CORPORATE'])
    const { isGroupLocked, groupOptions } = useScopedGroupOptions()

    expect(isGroupLocked.value).toBe(false)
    expect(groupOptions.value.map((option) => option.value)).toEqual(['RETAIL', 'CORPORATE'])
  })

  it('uses the full group catalog for global admins', () => {
    patchSession(['GLOBAL_ADMIN'], ['*'])
    const identityStore = useIdentityStore()
    identityStore.$patch({
      groups: [
        {
          id: 'g1',
          groupCode: 'RETAIL',
          displayName: 'Retail',
          dimension: 'BUSINESS_LINE',
          enabled: true,
          createdAt: '',
          updatedAt: '',
        },
        {
          id: 'g2',
          groupCode: 'CORPORATE',
          displayName: 'Corporate',
          dimension: 'DEPARTMENT',
          enabled: true,
          createdAt: '',
          updatedAt: '',
        },
      ],
    })

    const { isGroupLocked, groupOptions } = useScopedGroupOptions()

    expect(isGroupLocked.value).toBe(false)
    expect(groupOptions.value.map((option) => option.value)).toEqual(['RETAIL', 'CORPORATE'])
  })

  it('resolveDefaultGroupCode prefers the locked group', () => {
    patchSession(['GROUP_ADMIN'], ['RETAIL'])
    const { resolveDefaultGroupCode } = useScopedGroupOptions()

    expect(resolveDefaultGroupCode('CORPORATE')).toBe('RETAIL')
  })
})
