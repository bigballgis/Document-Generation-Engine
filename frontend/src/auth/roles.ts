/**
 * Role / capability helpers facade.
 * Domain checks live in sibling modules; existing `@/auth/roles` imports stay valid.
 */
export {
  MANAGEMENT_ROLES,
  sessionContext,
  type CapabilityContext,
} from '@/auth/roleCapabilityCore'

export {
  canAccessMasterManagement,
  canReviewMasters,
  canUploadMasters,
} from '@/auth/roleCapabilitiesMasters'

export {
  canAccessTemplateManagement,
  canAccessApiPolicyManagement,
  canAccessAuditConsole,
  resolveAuditActorRole,
  isGroupScopedAuditRole,
  canAuthorTemplates,
  canExportTemplates,
  canDecideTests,
  canDecideApprovals,
  canDecideLegalApprovals,
  canPublishTemplates,
  canStopTemplates,
  canRestoreOrDeprecateTemplates,
  canEditTemplateMetadata,
  canManageReleaseVersionState,
  canManageApiPolicy,
  canDeleteTemplates,
} from '@/auth/roleCapabilitiesTemplates'

export {
  canAuthorContentModules,
  canDecideContentModuleReviews,
  canManageContentModuleLifecycle,
  canConfigureContentModuleSharedGroups,
  canAccessContentModuleManagement,
  canViewCollaborationWorkItems,
  canMaintainCollaborationTimeoutConfig,
  canViewEscalationQueue,
} from '@/auth/roleCapabilitiesContentCollab'

export {
  canManageAssetLibrary,
  canUploadImageOrOtherAsset,
  canUploadSealAsset,
  canUploadAnyLibraryAsset,
  canDisableAssetLibrary,
  isAssetLibraryTesterOnly,
} from '@/auth/roleCapabilitiesAssetLibrary'

export { canManageLegalHold } from '@/auth/roleCapabilitiesLegalHold'

import type { CapabilityContext } from '@/auth/roleCapabilityCore'

/** @deprecated Use sessionStore.canAccessRoute for route guards. */
export function canAccessLogicalRoute(
  routeKey: string,
  _context: CapabilityContext,
  visibleRoutes: string[],
): boolean {
  return visibleRoutes.includes(routeKey)
}
