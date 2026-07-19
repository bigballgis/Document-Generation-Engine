import { MANAGEMENT_ROLE_VALUES, type ManagementRole } from '@/types/identity'
import type { ManagementSession } from '@/types/session'
import { MANAGEMENT_ROLES } from '@/auth/roles'

const OPERATIONAL_ROLES: ManagementRole[] = [
  MANAGEMENT_ROLES.MASTER_DESIGNER,
  MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
  MANAGEMENT_ROLES.TEMPLATE_TESTER,
  MANAGEMENT_ROLES.TEMPLATE_APPROVER,
  MANAGEMENT_ROLES.LEGAL_REVIEWER,
]

export function isGlobalAdmin(roles: string[]): boolean {
  return roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN)
}

export function canDeleteUsers(roles: string[]): boolean {
  return isGlobalAdmin(roles)
}

export function canManageGroups(roles: string[]): boolean {
  return isGlobalAdmin(roles)
}

/** Fail-closed: only global/group admins may create or edit users. */
export function canManageUsers(roles: string[]): boolean {
  return (
    isGlobalAdmin(roles) || roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN)
  )
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
