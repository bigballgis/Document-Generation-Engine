import { MANAGEMENT_ROLE_VALUES, type ManagementRole } from '@/types/identity'
import type { ManagementSession } from '@/types/session'

const GLOBAL_ADMIN = 'GLOBAL_ADMIN'

const OPERATIONAL_ROLES: ManagementRole[] = [
  'MASTER_DESIGNER',
  'TEMPLATE_AUTHOR',
  'TEMPLATE_TESTER',
  'TEMPLATE_APPROVER',
]

export function isGlobalAdmin(roles: string[]): boolean {
  return roles.includes(GLOBAL_ADMIN)
}

export function canDeleteUsers(roles: string[]): boolean {
  return isGlobalAdmin(roles)
}

export function canManageGroups(roles: string[]): boolean {
  return isGlobalAdmin(roles)
}

export function assignableRoles(actorRoles: string[]): ManagementRole[] {
  if (isGlobalAdmin(actorRoles)) {
    return [...MANAGEMENT_ROLE_VALUES]
  }
  return [...OPERATIONAL_ROLES]
}

export function assignableGroupCodes(
  session: ManagementSession | null,
  catalog: string[],
): string[] {
  if (!session) {
    return []
  }
  if (isGlobalAdmin(session.roles)) {
    return [...catalog]
  }
  const authorized = session.authorizedGroupCodes.filter((code) => code !== '*')
  return catalog.filter((code) => authorized.includes(code))
}
