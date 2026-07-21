import { describe, expect, it } from 'vitest'
import {
  canAccessAssetLibraryManagement,
  canDisableAssetLibrary,
  canManageAssetLibrary,
  canUploadAnyLibraryAsset,
  canUploadImageOrOtherAsset,
  canUploadSealAsset,
  isAssetLibraryTesterOnly,
} from '@/auth/roleCapabilitiesAssetLibrary'
import type { ManagementCapabilities } from '@/types/session'

const baseCaps: ManagementCapabilities = {
  manageMasters: false,
  reviewMasters: false,
  authorTemplates: false,
  decideTests: false,
  decideApprovals: false,
  decideLegalApprovals: false,
  publishTemplates: false,
  stopTemplates: false,
  restoreOrDeprecateTemplates: false,
  deleteTemplates: false,
  exportTemplates: false,
  viewCollaborationWorkItems: false,
  maintainCollaborationTimeoutConfig: false,
  authorContentModules: false,
  decideContentModuleReviews: false,
  manageContentModuleLifecycle: false,
  manageApiPolicy: false,
  readAudit: false,
  manageAssetLibrary: false,
  manageLegalHold: false,
}

describe('roleCapabilitiesAssetLibrary', () => {
  it('grants manageAssetLibrary from session capability', () => {
    expect(
      canManageAssetLibrary({
        roles: ['DOCUMENT_AUTHOR'],
        capabilities: { ...baseCaps, manageAssetLibrary: true },
      }),
    ).toBe(true)
    expect(
      canManageAssetLibrary({
        roles: ['DOCUMENT_AUTHOR'],
        capabilities: { ...baseCaps, manageAssetLibrary: false },
      }),
    ).toBe(false)
  })

  it('falls back to role matrix when capabilities are absent', () => {
    expect(canAccessAssetLibraryManagement(['TEMPLATE_TESTER'])).toBe(true)
    expect(canAccessAssetLibraryManagement(['AUDIT_ADMIN'])).toBe(false)
    expect(canManageAssetLibrary({ roles: ['GROUP_ADMIN'] })).toBe(true)
    expect(canManageAssetLibrary({ roles: ['AUDIT_ADMIN'] })).toBe(false)
  })

  it('gates upload and disable actions by role matrix', () => {
    expect(canUploadImageOrOtherAsset({ roles: ['DOCUMENT_AUTHOR'] })).toBe(true)
    expect(canUploadSealAsset({ roles: ['DOCUMENT_AUTHOR'] })).toBe(false)
    expect(canUploadSealAsset({ roles: ['GROUP_ADMIN'] })).toBe(true)
    expect(canUploadAnyLibraryAsset({ roles: ['GROUP_ADMIN'] })).toBe(true)
    expect(canUploadAnyLibraryAsset({ roles: ['TEMPLATE_TESTER'] })).toBe(false)
    expect(canDisableAssetLibrary({ roles: ['GLOBAL_ADMIN'] })).toBe(true)
    expect(canDisableAssetLibrary({ roles: ['DOCUMENT_AUTHOR'] })).toBe(false)
  })

  it('detects ACTIVE-only tester sessions', () => {
    expect(isAssetLibraryTesterOnly({ roles: ['TEMPLATE_TESTER'] })).toBe(true)
    expect(isAssetLibraryTesterOnly({ roles: ['TEMPLATE_TESTER', 'DOCUMENT_AUTHOR'] })).toBe(false)
    expect(isAssetLibraryTesterOnly({ roles: ['DOCUMENT_AUTHOR'] })).toBe(false)
  })
})
