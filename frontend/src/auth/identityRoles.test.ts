import { describe, expect, it } from 'vitest'
import {
  assignableGroupCodes,
  assignableRoles,
  canDeleteUsers,
  canManageGroups,
} from '@/auth/identityRoles'
import { MANAGEMENT_ROLES } from '@/auth/roles'
import type { ManagementSession } from '@/types/session'

function session(partial: Partial<ManagementSession>): ManagementSession {
  return {
    username: '10000001',
    displayName: 'Tester',
    email: 'tester@example.com',
    authSource: 'LOCAL',
    roles: [],
    authorizedGroupCodes: [],
    defaultRoute: 'route.identity-administration',
    visibleRoutes: ['route.identity-administration'],
    expiresAt: new Date().toISOString(),
    ...partial,
  }
}

describe('identityRoles', () => {
  it('only global admins may delete users', () => {
    expect(canDeleteUsers([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(true)
    expect(canDeleteUsers([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(false)
    expect(canDeleteUsers([])).toBe(false)
  })

  it('only global admins may write groups', () => {
    expect(canManageGroups([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(true)
    expect(canManageGroups([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(false)
  })

  it('exposes every role to global admins', () => {
    expect(assignableRoles([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toContain(MANAGEMENT_ROLES.GLOBAL_ADMIN)
    expect(assignableRoles([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toContain(MANAGEMENT_ROLES.AUDIT_ADMIN)
    expect(assignableRoles([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toContain(MANAGEMENT_ROLES.GROUP_ADMIN)
  })

  it('limits group admins to operational roles only', () => {
    const roles = assignableRoles([MANAGEMENT_ROLES.GROUP_ADMIN])
    expect(roles).toEqual([
      MANAGEMENT_ROLES.MASTER_DESIGNER,
      MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
      MANAGEMENT_ROLES.TEMPLATE_TESTER,
      MANAGEMENT_ROLES.TEMPLATE_APPROVER,
      MANAGEMENT_ROLES.LEGAL_REVIEWER,
    ])
    expect(roles).not.toContain(MANAGEMENT_ROLES.GLOBAL_ADMIN)
    expect(roles).not.toContain(MANAGEMENT_ROLES.GROUP_ADMIN)
    expect(roles).not.toContain(MANAGEMENT_ROLES.AUDIT_ADMIN)
  })

  it('gives global admins the full known group catalog as scope options', () => {
    const codes = assignableGroupCodes(
      session({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN], authorizedGroupCodes: ['*'] }),
      ['RETAIL', 'CORPORATE'],
    )
    expect(codes).toEqual(['RETAIL', 'CORPORATE'])
  })

  it('limits group admins to their own authorized scope', () => {
    const codes = assignableGroupCodes(
      session({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN], authorizedGroupCodes: ['RETAIL'] }),
      ['RETAIL', 'CORPORATE'],
    )
    expect(codes).toEqual(['RETAIL'])
  })

  it('drops the wildcard marker from group admin scope', () => {
    const codes = assignableGroupCodes(
      session({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN], authorizedGroupCodes: ['*', 'RETAIL'] }),
      ['RETAIL'],
    )
    expect(codes).toEqual(['RETAIL'])
  })
})
