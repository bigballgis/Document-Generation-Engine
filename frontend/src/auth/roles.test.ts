import { describe, expect, it } from 'vitest'
import {
  canAccessAuditConsole,
  canAccessMasterManagement,
  canAccessTemplateManagement,
  canDecideApprovals,
  canDecideTests,
  canManageApiPolicy,
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

  it('allows template management for authoring and admin roles via fallback', () => {
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessTemplateManagement(['AUDIT_ADMIN'])).toBe(false)
  })

  it('maps granular template capabilities from session capabilities', () => {
    expect(canUploadMasters({ roles: [], capabilities: globalAdminCapabilities })).toBe(true)
    expect(canDecideApprovals({ roles: [], capabilities: testerCapabilities })).toBe(false)
  })

  it('allows audit console for audit and admin roles via fallback', () => {
    expect(canAccessAuditConsole({ roles: [MANAGEMENT_ROLES.AUDIT_ADMIN] })).toBe(true)
    expect(canAccessAuditConsole({ roles: [MANAGEMENT_ROLES.GLOBAL_ADMIN] })).toBe(true)
    expect(canAccessAuditConsole({ roles: [MANAGEMENT_ROLES.TEMPLATE_AUTHOR] })).toBe(false)
  })

  it('resolves audit actor role with audit admin precedence', () => {
    expect(resolveAuditActorRole([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe('GROUP_ADMIN')
    expect(resolveAuditActorRole([MANAGEMENT_ROLES.AUDIT_ADMIN, MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(
      'AUDIT_ADMIN',
    )
    expect(isGroupScopedAuditRole('GROUP_ADMIN')).toBe(true)
  })
})
