import { MANAGEMENT_ROLES, resolveCapability, type CapabilityContext } from '@/auth/roleCapabilityCore'
import { isGlobalAdmin } from '@/auth/identityRoles'

/** Session capability + GLOBAL_ADMIN-only role fallback (CE-G04). */
export function canManageLegalHold(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageLegalHold', (roles) => isGlobalAdmin(roles))
}

export function canAccessLegalHoldAdministration(roles: string[]): boolean {
  return roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN)
}
