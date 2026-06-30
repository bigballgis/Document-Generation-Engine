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

function resolveCapability(
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

function canManageMastersByRole(roles: string[]): boolean {
  return roles.some((role) =>
    ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
  )
}

export function canUploadMasters(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageMasters', canManageMastersByRole)
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

export function canExportTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'exportTemplates', (roles) => {
    if (!canAccessTemplateManagement(roles)) {
      return false
    }
    return roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
        ] as string[]
      ).includes(role),
    )
  })
}

export function canDecideTests(context: CapabilityContext): boolean {
  return resolveCapability(context, 'decideTests', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.TEMPLATE_TESTER,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canDecideApprovals(context: CapabilityContext): boolean {
  return resolveCapability(context, 'decideApprovals', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.TEMPLATE_APPROVER,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canPublishTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'publishTemplates', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canStopTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'stopTemplates', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.MASTER_DESIGNER,
          MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canRestoreOrDeprecateTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'restoreOrDeprecateTemplates', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canEditTemplateMetadata(context: CapabilityContext): boolean {
  return canPublishTemplates(context) || canRestoreOrDeprecateTemplates(context)
}

export function canManageReleaseVersionState(context: CapabilityContext): boolean {
  return resolveCapability(context, 'restoreOrDeprecateTemplates', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canManageApiPolicy(context: CapabilityContext): boolean {
  return canAccessApiPolicyManagement(context)
}

export function canDeleteTemplates(context: CapabilityContext): boolean {
  return resolveCapability(context, 'deleteTemplates', (roles) =>
    roles.includes(MANAGEMENT_ROLES.GLOBAL_ADMIN),
  )
}

export function canAuthorContentModules(context: CapabilityContext): boolean {
  return resolveCapability(context, 'authorContentModules', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.MASTER_DESIGNER,
          MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canDecideContentModuleReviews(context: CapabilityContext): boolean {
  return resolveCapability(context, 'decideContentModuleReviews', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.TEMPLATE_APPROVER,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canManageContentModuleLifecycle(context: CapabilityContext): boolean {
  return resolveCapability(context, 'manageContentModuleLifecycle', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canAccessContentModuleManagement(roles: string[]): boolean {
  return roles.some((role) =>
    (
      [
        MANAGEMENT_ROLES.GLOBAL_ADMIN,
        MANAGEMENT_ROLES.GROUP_ADMIN,
        MANAGEMENT_ROLES.MASTER_DESIGNER,
        MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
        MANAGEMENT_ROLES.TEMPLATE_APPROVER,
      ] as string[]
    ).includes(role),
  )
}

export function canViewCollaborationWorkItems(context: CapabilityContext): boolean {
  return resolveCapability(context, 'viewCollaborationWorkItems', (roles) =>
    roles.some((role) =>
      (
        [
          MANAGEMENT_ROLES.GLOBAL_ADMIN,
          MANAGEMENT_ROLES.GROUP_ADMIN,
          MANAGEMENT_ROLES.TEMPLATE_AUTHOR,
          MANAGEMENT_ROLES.TEMPLATE_TESTER,
          MANAGEMENT_ROLES.TEMPLATE_APPROVER,
        ] as string[]
      ).includes(role),
    ),
  )
}

export function canMaintainCollaborationTimeoutConfig(context: CapabilityContext): boolean {
  return resolveCapability(context, 'maintainCollaborationTimeoutConfig', (roles) =>
    roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    ),
  )
}

export function canViewEscalationQueue(context: CapabilityContext): boolean {
  return (
    canViewCollaborationWorkItems(context) &&
    context.roles.some((role) =>
      ([MANAGEMENT_ROLES.GLOBAL_ADMIN, MANAGEMENT_ROLES.GROUP_ADMIN] as string[]).includes(role),
    )
  )
}

/** @deprecated Use sessionStore.canAccessRoute for route guards. */
export function canAccessLogicalRoute(
  routeKey: string,
  _context: CapabilityContext,
  visibleRoutes: string[],
): boolean {
  return visibleRoutes.includes(routeKey)
}

/** @deprecated Use granular capability helpers instead. */
export function canManageTemplateLifecycle(context: CapabilityContext): boolean {
  return canAuthorTemplates(context)
}
