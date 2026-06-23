import { describe, expect, it } from 'vitest'
import {
  assignableGroupCodes,
  assignableRoles,
  canDeleteUsers,
  canManageGroups,
} from '@/auth/identityRoles'
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
    expect(canDeleteUsers(['GLOBAL_ADMIN'])).toBe(true)
    expect(canDeleteUsers(['GROUP_ADMIN'])).toBe(false)
    expect(canDeleteUsers([])).toBe(false)
  })

  it('only global admins may write groups', () => {
    expect(canManageGroups(['GLOBAL_ADMIN'])).toBe(true)
    expect(canManageGroups(['GROUP_ADMIN'])).toBe(false)
  })

  it('exposes every role to global admins', () => {
    expect(assignableRoles(['GLOBAL_ADMIN'])).toContain('GLOBAL_ADMIN')
    expect(assignableRoles(['GLOBAL_ADMIN'])).toContain('AUDIT_ADMIN')
    expect(assignableRoles(['GLOBAL_ADMIN'])).toContain('GROUP_ADMIN')
  })

  it('limits group admins to operational roles only', () => {
    const roles = assignableRoles(['GROUP_ADMIN'])
    expect(roles).toEqual([
      'MASTER_DESIGNER',
      'TEMPLATE_AUTHOR',
      'TEMPLATE_TESTER',
      'TEMPLATE_APPROVER',
    ])
    expect(roles).not.toContain('GLOBAL_ADMIN')
    expect(roles).not.toContain('GROUP_ADMIN')
    expect(roles).not.toContain('AUDIT_ADMIN')
  })

  it('gives global admins the full known group catalog as scope options', () => {
    const codes = assignableGroupCodes(session({ roles: ['GLOBAL_ADMIN'], authorizedGroupCodes: ['*'] }), [
      'RETAIL',
      'CORPORATE',
    ])
    expect(codes).toEqual(['RETAIL', 'CORPORATE'])
  })

  it('limits group admins to their own authorized scope', () => {
    const codes = assignableGroupCodes(
      session({ roles: ['GROUP_ADMIN'], authorizedGroupCodes: ['RETAIL'] }),
      ['RETAIL', 'CORPORATE'],
    )
    expect(codes).toEqual(['RETAIL'])
  })

  it('drops the wildcard marker from group admin scope', () => {
    const codes = assignableGroupCodes(
      session({ roles: ['GROUP_ADMIN'], authorizedGroupCodes: ['*', 'RETAIL'] }),
      ['RETAIL'],
    )
    expect(codes).toEqual(['RETAIL'])
  })
})
