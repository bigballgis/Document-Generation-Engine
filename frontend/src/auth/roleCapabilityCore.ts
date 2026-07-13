import { MANAGEMENT_ROLE_VALUES, type ManagementRole } from '@/types/identity'
import type { ManagementCapabilities, ManagementSession } from '@/types/session'

/** Derived from {@link MANAGEMENT_ROLE_VALUES} — single source of truth for role string literals. */
export const MANAGEMENT_ROLES = MANAGEMENT_ROLE_VALUES.reduce(
  (roles, role) => {
    roles[role] = role
    return roles
  },
  {} as Record<ManagementRole, ManagementRole>,
)

export interface CapabilityContext {
  roles: string[]
  capabilities?: ManagementCapabilities
}

export function sessionContext(session: ManagementSession | null): CapabilityContext {
  return {
    roles: session?.roles ?? [],
    capabilities: session?.capabilities,
  }
}

export function resolveCapability(
  context: CapabilityContext,
  capabilityKey: keyof ManagementCapabilities,
  roleFallback: (roles: string[]) => boolean,
): boolean {
  if (context.capabilities) {
    const capability = context.capabilities[capabilityKey]
    if (typeof capability === 'boolean') {
      return capability
    }
    // Fail-closed: missing or non-boolean values deny; no role fallback widening.
    return false
  }
  // Legacy/tests: matrix-aligned role fallback when session capabilities are absent.
  return roleFallback(context.roles)
}
