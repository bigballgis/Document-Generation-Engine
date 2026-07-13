import { MANAGEMENT_ROLES, resolveCapability, type CapabilityContext } from '@/auth/roleCapabilityCore'

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
