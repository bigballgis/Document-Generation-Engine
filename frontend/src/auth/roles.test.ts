import { describe, expect, it } from 'vitest'
import {
  canAccessAuditConsole,
  canAccessLogicalRoute,
  canAccessMasterManagement,
  canAccessTemplateManagement,
  canAccessCollaborationEscalationWorkbench,
  canAccessTesterWorkbench,
  canAccessApproverWorkbench,
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
  manageApiPolicy: true,
  readAudit: true,
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
  manageApiPolicy: false,
  readAudit: false,
}

describe('management roles', () => {
  it('allows master management for authoring and admin roles via fallback', () => {
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(true)
    expect(canAccessMasterManagement(['AUDIT_ADMIN'])).toBe(false)
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

  it('allows upload for master management roles via fallback', () => {
    expect(canUploadMasters({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(true)
  })

  it('allows template export/import for admin and author roles', () => {
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN] })).toBe(true)
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.GROUP_ADMIN] })).toBe(true)
    expect(canExportTemplates({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(true)
    expect(canExportTemplates({ roles: ['TEMPLATE_TESTER'] })).toBe(false)
  })

  it('allows template management for authoring and admin roles via fallback', () => {
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessTemplateManagement(['AUDIT_ADMIN'])).toBe(false)
  })

  it('allows content module management for authoring, approver, and admin roles', () => {
    expect(canAccessContentModuleManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessContentModuleManagement(['TEMPLATE_APPROVER'])).toBe(true)
    expect(canAccessContentModuleManagement(['MASTER_DESIGNER'])).toBe(true)
    expect(canAccessContentModuleManagement(['TEMPLATE_TESTER'])).toBe(false)
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

  it('gates workbench routes by collaboration role capabilities', () => {
    const testerContext = { roles: ['TEMPLATE_TESTER'], capabilities: testerCapabilities }
    const approverContext = { roles: ['TEMPLATE_APPROVER'], capabilities: { ...testerCapabilities, decideTests: false, decideApprovals: true } }
    const groupAdminContext = { roles: [MANAGEMENT_ROLES.GROUP_ADMIN], capabilities: globalAdminCapabilities }

    expect(canAccessTesterWorkbench(testerContext)).toBe(true)
    expect(canAccessApproverWorkbench(testerContext)).toBe(false)
    expect(canAccessCollaborationEscalationWorkbench(testerContext)).toBe(false)
    expect(canAccessCollaborationEscalationWorkbench(groupAdminContext)).toBe(true)
    expect(canAccessLogicalRoute('route.tester-workbench', testerContext, [])).toBe(true)
    expect(canAccessLogicalRoute('route.approver-workbench', approverContext, [])).toBe(true)
    expect(canAccessLogicalRoute('route.escalation-workbench', groupAdminContext, [])).toBe(true)
  })

  it('allows workbench routes from roles when session capabilities are absent', () => {
    const testerContext = { roles: ['TEMPLATE_TESTER'] }
    const approverContext = { roles: ['TEMPLATE_APPROVER'] }
    const groupAdminContext = { roles: [MANAGEMENT_ROLES.GROUP_ADMIN] }

    expect(canDecideTests(testerContext)).toBe(true)
    expect(canDecideApprovals(approverContext)).toBe(true)
    expect(canAccessTesterWorkbench(testerContext)).toBe(true)
    expect(canAccessApproverWorkbench(approverContext)).toBe(true)
    expect(canAccessCollaborationEscalationWorkbench(groupAdminContext)).toBe(true)
    expect(canAccessLogicalRoute('route.tester-workbench', testerContext, [])).toBe(true)
    expect(canAccessLogicalRoute('route.approver-workbench', approverContext, [])).toBe(true)
    expect(canAccessLogicalRoute('route.escalation-workbench', groupAdminContext, [])).toBe(true)
  })
})
