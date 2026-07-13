import { MANAGEMENT_ROLES, resolveCapability, type CapabilityContext } from '@/auth/roleCapabilityCore'

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
