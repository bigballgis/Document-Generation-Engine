import { describe, expect, it } from 'vitest'
import {
  canAccessAuditConsole,
  canAccessLogicalRoute,
  canAccessMasterManagement,
  canAccessTemplateManagement,
  canViewEscalationQueue,
  canExportTemplates,
  canAccessContentModuleManagement,
  canDecideApprovals,
  canDecideTests,
  canDeleteTemplates,
  canManageApiPolicy,
  canManageReleaseVersionState,
  canReviewMasters,
  canUploadMasters,
  isGroupScopedAuditRole,
  MANAGEMENT_ROLES,
  resolveAuditActorRole,
} from '@/auth/roles'
import type { ManagementCapabilities } from '@/types/session'

const globalAdminCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: true,
  authorContentModules: true,
  decideContentModuleReviews: true,
  manageContentModuleLifecycle: true,
  manageApiPolicy: true,
  readAudit: true,
  manageAssetLibrary: true,
  manageLegalHold: false,
}

const testerCapabilities: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: true,
  decideApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: false,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
  manageAssetLibrary: true,
  manageLegalHold: false,
}

describe('management roles', () => {
  it('allows master management for authoring and admin roles via fallback', () => {
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.AUDIT_ADMIN])).toBe(false)
  })

  it('restricts review actions to admin roles via fallback', () => {
    expect(canReviewMasters({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN] })).toBe(true)
    expect(canReviewMasters({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(false)
  })

  it('prefers capabilities over role fallback when provided', () => {
    expect(canUploadMasters({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR], capabilities: testerCapabilities })).toBe(
      false,
    )
    expect(canDecideTests({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR], capabilities: testerCapabilities })).toBe(
      true,
    )
    expect(
      canManageApiPolicy({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR], capabilities: globalAdminCapabilities }),
    ).toBe(true)
  })

  it('denies upload for template author via fallback (AUD-P01)', () => {
    expect(canUploadMasters({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(false)
  })

  it('allows master designer to manage masters via fallback (permission-matrix §4)', () => {
    expect(canUploadMasters({ roles: [MANAGEMENT_ROLES.MASTER_DESIGNER] })).toBe(true)
    expect(canReviewMasters({ roles: [MANAGEMENT_ROLES.MASTER_DESIGNER] })).toBe(false)
  })

  it('denies upload when manageMasters capability is explicitly false (AUD-P01)', () => {
    expect(
      canUploadMasters({
        roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR],
        capabilities: { ...testerCapabilities, manageMasters: false },
      }),
    ).toBe(false)
  })

  it('allows template export/import for admin and author roles via authorTemplates fallback', () => {
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN] })).toBe(true)
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN] })).toBe(true)
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(true)
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.TEMPLATE_TESTER] })).toBe(false)
  })

  it('denies export when exportTemplates is false even if authorTemplates is true (AUD-P05)', () => {
    expect(
      canExportTemplates({
        roles: [MANAGEMENT_ROLES.TEMPLATE_TESTER],
        capabilities: { ...testerCapabilities, authorTemplates: true, exportTemplates: false },
      }),
    ).toBe(false)
  })

  it('prefers exportTemplates capability when present', () => {
    expect(
      canExportTemplates({
        roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR],
        capabilities: { ...globalAdminCapabilities, exportTemplates: false, authorTemplates: true },
      }),
    ).toBe(false)
  })

  it('allows template management for authoring and admin roles via fallback', () => {
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.AUDIT_ADMIN])).toBe(false)
  })

  it('allows content module management for authoring, approver, and admin roles', () => {
    expect(canAccessContentModuleManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessContentModuleManagement([MANAGEMENT_ROLES.TEMPLATE_APPROVER])).toBe(true)
    expect(canAccessContentModuleManagement([MANAGEMENT_ROLES.MASTER_DESIGNER])).toBe(true)
    expect(canAccessContentModuleManagement([MANAGEMENT_ROLES.TEMPLATE_TESTER])).toBe(false)
  })

  it('maps granular template capabilities from session capabilities', () => {
    expect(canUploadMasters({ roles: [], capabilities: globalAdminCapabilities })).toBe(true)
    expect(canDecideApprovals({ roles: [], capabilities: testerCapabilities })).toBe(false)
  })

  it('restricts release version governance to admin roles via fallback', () => {
    expect(canManageReleaseVersionState({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN] })).toBe(true)
    expect(canManageReleaseVersionState({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(false)
  })

  it('allows audit console for audit and admin roles via fallback', () => {
    expect(canAccessAuditConsole({ roles: [MANAGEMENT_ROLES.AUDIT_ADMIN] })).toBe(true)
    expect(canAccessAuditConsole({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN] })).toBe(true)
    expect(canAccessAuditConsole({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(false)
  })

  it('allows template deletion only for global admins by fallback', () => {
    expect(canDeleteTemplates({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN] })).toBe(true)
    expect(canDeleteTemplates({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN] })).toBe(false)
    expect(canDeleteTemplates({ roles: [], capabilities: globalAdminCapabilities })).toBe(true)
  })

  it('resolves audit actor role with audit admin precedence', () => {
    expect(resolveAuditActorRole([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe('GROUP_ADMIN')
    expect(resolveAuditActorRole([MANAGEMENT_ROLES.AUDIT_ADMIN, MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(
      'AUDIT_ADMIN',
    )
    expect(isGroupScopedAuditRole('GROUP_ADMIN')).toBe(true)
  })

  it('gates escalation queue visibility by admin collaboration roles', () => {
    const testerContext = { roles: [MANAGEMENT_ROLES.TEMPLATE_TESTER], capabilities: testerCapabilities }
    const groupAdminContext = { roles: [MANAGEMENT_ROLES.GROUP_ADMIN], capabilities: globalAdminCapabilities }

    expect(canViewEscalationQueue(testerContext)).toBe(false)
    expect(canViewEscalationQueue(groupAdminContext)).toBe(true)
  })

  it('does not special-case legacy workbench route keys in canAccessLogicalRoute', () => {
    const testerContext = { roles: [MANAGEMENT_ROLES.TEMPLATE_TESTER], capabilities: testerCapabilities }
    const groupAdminContext = { roles: [MANAGEMENT_ROLES.GROUP_ADMIN], capabilities: globalAdminCapabilities }

    expect(canAccessLogicalRoute('route.tester-workbench', testerContext, [])).toBe(false)
    expect(canAccessLogicalRoute('route.escalation-workbench', groupAdminContext, [])).toBe(false)
    expect(
      canAccessLogicalRoute('route.escalation-workbench', groupAdminContext, [
        'route.escalation-workbench',
      ]),
    ).toBe(true)
  })

  it('denies content-module route when not in visibleRoutes even if role would allow (AUD-P02)', () => {
    const authorContext = { roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR], capabilities: globalAdminCapabilities }

    expect(
      canAccessLogicalRoute('route.content-module-management', authorContext, []),
    ).toBe(false)
    expect(
      canAccessLogicalRoute('route.content-module-management', authorContext, [
        'route.content-module-management',
      ]),
    ).toBe(true)
  })

  it('fail-closes capability checks when session capabilities object is present but key missing', () => {
    const partialCapabilities = { authorTemplates: true } as ManagementCapabilities

    expect(canUploadMasters({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN], capabilities: partialCapabilities })).toBe(
      false,
    )
  })
})
