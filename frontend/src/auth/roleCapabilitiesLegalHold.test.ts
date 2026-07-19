import { describe, expect, it } from 'vitest'
import { canManageLegalHold } from '@/auth/roleCapabilitiesLegalHold'
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

describe('canManageLegalHold', () => {
  it('allows when session capability is true', () => {
    expect(
      canManageLegalHold({
        roles: ['GROUP_ADMIN'],
        capabilities: { ...baseCaps, manageLegalHold: true },
      }),
    ).toBe(true)
  })

  it('denies when session capability is false', () => {
    expect(
      canManageLegalHold({
        roles: ['GLOBAL_ADMIN'],
        capabilities: { ...baseCaps, manageLegalHold: false },
      }),
    ).toBe(false)
  })

  it('falls back to GLOBAL_ADMIN only when capabilities are absent', () => {
    expect(canManageLegalHold({ roles: ['GLOBAL_ADMIN'], capabilities: undefined })).toBe(true)
    expect(canManageLegalHold({ roles: ['GROUP_ADMIN'], capabilities: undefined })).toBe(false)
  })
})
