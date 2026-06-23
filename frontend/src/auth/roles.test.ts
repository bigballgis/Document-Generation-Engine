import { describe, expect, it } from 'vitest'
import {
  canAccessAuditConsole,
  canAccessMasterManagement,
  canAccessTemplateManagement,
  canReviewMasters,
  canUploadMasters,
  isGroupScopedAuditRole,
  MANAGEMENT_ROLES,
  resolveAuditActorRole,
} from '@/auth/roles'

describe('management roles', () => {
  it('allows master management for authoring and admin roles', () => {
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessMasterManagement([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(true)
    expect(canAccessMasterManagement(['AUDIT_ADMIN'])).toBe(false)
  })

  it('restricts review actions to admin roles', () => {
    expect(canReviewMasters([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canReviewMasters([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(false)
  })

  it('allows upload for master management roles', () => {
    expect(canUploadMasters([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
  })

  it('allows template management for authoring and admin roles', () => {
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(true)
    expect(canAccessTemplateManagement([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe(true)
    expect(canAccessTemplateManagement(['AUDIT_ADMIN'])).toBe(false)
  })

  it('allows audit console for audit and admin roles', () => {
    expect(canAccessAuditConsole([MANAGEMENT_ROLES.AUDIT_ADMIN])).toBe(true)
    expect(canAccessAuditConsole([MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(true)
    expect(canAccessAuditConsole([MANAGEMENT_ROLES.TEMPLATE_AUTHOR])).toBe(false)
  })

  it('resolves audit actor role with audit admin precedence', () => {
    expect(resolveAuditActorRole([MANAGEMENT_ROLES.GROUP_ADMIN])).toBe('GROUP_ADMIN')
    expect(resolveAuditActorRole([MANAGEMENT_ROLES.AUDIT_ADMIN, MANAGEMENT_ROLES.GLOBAL_ADMIN])).toBe(
      'AUDIT_ADMIN',
    )
    expect(isGroupScopedAuditRole('GROUP_ADMIN')).toBe(true)
  })
})
