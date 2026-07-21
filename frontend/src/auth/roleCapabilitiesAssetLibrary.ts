import { MANAGEMENT_ROLES, resolveCapability, type CapabilityContext } from '@/auth/roleCapabilityCore'

const ASSET_LIBRARY_ROUTE_ROLES = [
  MANAGEMENT_ROLES.GLOBAL_ADMIN,
  MANAGEMENT_ROLES.GROUP_ADMIN,
  MANAGEMENT_ROLES.DOCUMENT_AUTHOR,
  MANAGEMENT_ROLES.TEMPLATE_TESTER,
] as const

const IMAGE_OR_OTHER_UPLOAD_ROLES = [
  MANAGEMENT_ROLES.GLOBAL_ADMIN,
  MANAGEMENT_ROLES.GROUP_ADMIN,
  MANAGEMENT_ROLES.DOCUMENT_AUTHOR,
] as const

/** SEAL upload — admins only (former TEMPLATE_APPROVER absorbed into GROUP_ADMIN). */
const SEAL_UPLOAD_ROLES = [MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as const

const DISABLE_ROLES = [MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as const

function hasAnyRole(roles: string[], allowed: readonly string[]): boolean {
  return roles.some((role) => allowed.includes(role))
}

/** Session capability + role fallback for route visibility (CE-E02). */
export function canManageAssetLibrary(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageAssetLibrary', (roles) =>
    hasAnyRole(roles, ASSET_LIBRARY_ROUTE_ROLES),
  )
}

export function canAccessAssetLibraryManagement(roles: string[]): boolean {
  return hasAnyRole(roles, ASSET_LIBRARY_ROUTE_ROLES)
}

/** Fine-grained: IMAGE / OTHER upload (role matrix; not a session capability). */
export function canUploadImageOrOtherAsset(context: CapabilityContext): boolean {
  return hasAnyRole(context.roles, IMAGE_OR_OTHER_UPLOAD_ROLES)
}

/** Fine-grained: SEAL upload — admin only. */
export function canUploadSealAsset(context: CapabilityContext): boolean {
  return hasAnyRole(context.roles, SEAL_UPLOAD_ROLES)
}

export function canUploadAnyLibraryAsset(context: CapabilityContext): boolean {
  return canUploadImageOrOtherAsset(context) || canUploadSealAsset(context)
}

/** Fine-grained: disable — admin only. */
export function canDisableAssetLibrary(context: CapabilityContext): boolean {
  return hasAnyRole(context.roles, DISABLE_ROLES)
}

/**
 * TEMPLATE_TESTER without elevated roles is ACTIVE-only on the list API.
 * UI hides DISABLED/ALL status filters for the same actors.
 */
export function isAssetLibraryTesterOnly(context: CapabilityContext): boolean {
  return (
    context.roles.includes(MANAGEMENT_ROLES.TEMPLATE_TESTER) &&
    !hasAnyRole(context.roles, [
      MANAGEMENT_ROLES.GLOBAL_ADMIN,
      MANAGEMENT_ROLES.GROUP_ADMIN,
      MANAGEMENT_ROLES.DOCUMENT_AUTHOR,
    ])
  )
}
