import { MANAGEMENT_ROLES, resolveCapability, type CapabilityContext } from '@/auth/roleCapabilityCore'

export function canAccessMasterManagement(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.DOCUMENT_AUTHOR,
      ] as string[]
    ).includes(role),
  )
}

export function canReviewMasters(context: CapabilityContext): boolean {
  return resolveCapability(context, 'reviewMasters', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

function canManageMastersByRole(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.DOCUMENT_AUTHOR,
      ] as string[]
    ).includes(role),
  )
}

export function canUploadMasters(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageMasters', canManageMastersByRole)
}
