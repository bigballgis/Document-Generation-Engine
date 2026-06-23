export const MANAGEMENT_ROLES = {
  GLOBAL_ADMIN: 'GLOBAL_ADMIN',
  GROUP_ADMIN: 'GROUP_ADMIN',
  TEMPLATE_AUTHOR: 'TEMPLATE_AUTHOR',
  AUDIT_ADMIN: 'AUDIT_ADMIN',
  API_ADMIN: 'API_ADMIN',
} as const

export function canAccessMasterManagement(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
      ] as string[]
    ).includes(role),
  )
}

export function canReviewMasters(roles: string[]): boolean {
  return roles.some((role) =>
    ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
  )
}

export function canUploadMasters(roles: string[]): boolean {
  return canAccessMasterManagement(roles)
}

export function canAccessTemplateManagement(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
      ] as string[]
    ).includes(role),
  )
}

export function canAccessAuditConsole(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.AUDIT_ADMIN,
      ] as string[]
    ).includes(role),
  )
}

export function canAccessApiPolicyManagement(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.API_ADMIN,
      ] as string[]
    ).includes(role),
  )
}

export function resolveAuditActorRole(roles: string[]): 'GLOBAL_ADMIN' | 'GROUP_ADMIN' | 'AUDIT_ADMIN' | null {
  if (roles.includes(MANAGEMENT_ROLES.AUDIT_ADMIN)) {
    return 'AUDIT_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN)) {
    return 'GLOBAL_ADMIN'
  }
  if (roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN)) {
    return 'GROUP_ADMIN'
  }
  return null
}

export function isGroupScopedAuditRole(actorRole: string | null): boolean {
  return actorRole === MANAGEMENT_ROLES.GROUP_ADMIN
}

export function canManageTemplateLifecycle(roles: string[]): boolean {
  return canAccessTemplateManagement(roles)
}

export function canManageApiPolicy(roles: string[]): boolean {
  return canAccessApiPolicyManagement(roles)
}
