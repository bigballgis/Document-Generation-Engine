import { computed } from 'vue'
import {
  canAuthorContentModules,
  canAuthorTemplates,
  canConfigureContentModuleSharedGroups,
  canDecideApprovals,
  canDecideContentModuleReviews,
  canDecideTests,
  canDeleteTemplates,
  canDisableAssetLibrary,
  canEditTemplateMetadata,
  canExportTemplates,
  canManageApiPolicy,
  canManageAssetLibrary,
  canManageContentModuleLifecycle,
  canPublishTemplates,
  canManageReleaseVersionState,
  canRestoreOrDeprecateTemplates,
  canReviewMasters,
  canStopTemplates,
  canUploadAnyLibraryAsset,
  canUploadImageOrOtherAsset,
  canUploadMasters,
  canUploadSealAsset,
  isAssetLibraryTesterOnly,
  sessionContext,
} from '@/auth/roles'
import { useSessionStore } from '@/stores/session'

export function useCapabilities() {
  const sessionStore = useSessionStore()

  const context = computed(() => sessionContext(sessionStore.session))

  const manageMasters = computed(() => canUploadMasters(context.value))
  const reviewMasters = computed(() => canReviewMasters(context.value))
  const authorTemplates = computed(() => canAuthorTemplates(context.value))
  const exportTemplates = computed(() => canExportTemplates(context.value))
  const decideTests = computed(() => canDecideTests(context.value))
  const decideApprovals = computed(() => canDecideApprovals(context.value))
  const publishTemplates = computed(() => canPublishTemplates(context.value))
  const stopTemplates = computed(() => canStopTemplates(context.value))
  const restoreOrDeprecateTemplates = computed(() => canRestoreOrDeprecateTemplates(context.value))
  const manageReleaseVersionState = computed(() => canManageReleaseVersionState(context.value))
  const manageApiPolicy = computed(() => canManageApiPolicy(context.value))
  const deleteTemplates = computed(() => canDeleteTemplates(context.value))
  const editTemplateMetadata = computed(() => canEditTemplateMetadata(context.value))
  const authorContentModules = computed(() => canAuthorContentModules(context.value))
  const decideContentModuleReviews = computed(() => canDecideContentModuleReviews(context.value))
  const manageContentModuleLifecycle = computed(() => canManageContentModuleLifecycle(context.value))
  const configureContentModuleSharedGroups = computed(() =>
    canConfigureContentModuleSharedGroups(context.value),
  )
  const manageAssetLibrary = computed(() => canManageAssetLibrary(context.value))
  const uploadAnyLibraryAsset = computed(() => canUploadAnyLibraryAsset(context.value))
  const uploadImageOrOtherAsset = computed(() => canUploadImageOrOtherAsset(context.value))
  const uploadSealAsset = computed(() => canUploadSealAsset(context.value))
  const disableAssetLibrary = computed(() => canDisableAssetLibrary(context.value))
  const assetLibraryTesterOnly = computed(() => isAssetLibraryTesterOnly(context.value))

  return {
    context,
    manageMasters,
    reviewMasters,
    authorTemplates,
    exportTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    stopTemplates,
    restoreOrDeprecateTemplates,
    manageReleaseVersionState,
    manageApiPolicy,
    deleteTemplates,
    editTemplateMetadata,
    authorContentModules,
    decideContentModuleReviews,
    manageContentModuleLifecycle,
    configureContentModuleSharedGroups,
    manageAssetLibrary,
    uploadAnyLibraryAsset,
    uploadImageOrOtherAsset,
    uploadSealAsset,
    disableAssetLibrary,
    assetLibraryTesterOnly,
  }
}
