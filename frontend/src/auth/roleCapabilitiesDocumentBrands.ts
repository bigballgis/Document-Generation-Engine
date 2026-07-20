import { MANAGEMENT_ROLES, type CapabilityContext } from '@/auth/roleCapabilityCore'
import { isGlobalAdmin } from '@/auth/identityRoles'

/**
 * IBL-E4 / ADR-0065 — catalog write is admin-only; no new capability bit.
 * Read APIs remain group-scoped for template authors (selectors / allow-list).
 */
export function canManageDocumentBrandCatalogs(context: CapabilityContext): boolean {
  return (
    isGlobalAdmin(context.roles) || context.roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN)
  )
}
