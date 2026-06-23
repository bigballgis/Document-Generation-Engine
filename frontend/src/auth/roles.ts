import type { ManagementCapabilities, ManagementSession } from '@/types/session'

export const MANAGEMENT_ROLES = {
  GLOBAL_ADMIN: 'GLOBAL_ADMIN',
  GROUP_ADMIN: 'GROUP_ADMIN',
  TEMPLATE_AUTHOR: 'TEMPLATE_AUTHOR',
  AUDIT_ADMIN: 'AUDIT_ADMIN',
} as const

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

function resolveCapability(
  context: CapabilityContext,
  capabilityKey: keyof ManagementCapabilities,
  roleFallback: (roles: string[]) => boolean,
): boolean {
  if (context.capabilities) {
    return context.capabilities[capabilityKey]
  }
  return roleFallback(context.roles)
}

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

export function canReviewMasters(context: CapabilityContext): boolean {
  return resolveCapability(context, 'reviewMasters', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canUploadMasters(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageMasters', canAccessMasterManagement)
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

export function canAccessAuditConsole(context: CapabilityContext): boolean {
  return resolveCapability(context, 'readAudit', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.AUDIT_ADMIN,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canAccessApiPolicyManagement(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageApiPolicy', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
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

export function canAuthorTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'authorTemplates', canAccessTemplateManagement)
}

export function canDecideTests(context: CapabilityContext): boolean {
  return resolveCapability(context, 'decideTests', () => false)
}

export function canDecideApprovals(context: CapabilityContext): boolean {
  return resolveCapability(context, 'decideApprovals', () => false)
}

export function canPublishTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'publishTemplates', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canManageApiPolicy(context: CapabilityContext): boolean {
  return canAccessApiPolicyManagement(context)
}

/** @deprecated Use granular capability helpers instead. */
export function canManageTemplateLifecycle(context: CapabilityContext): boolean {
  return canAuthorTemplates(context)
}
