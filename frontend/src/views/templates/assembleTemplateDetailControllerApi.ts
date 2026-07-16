import type { ComputedRef, Ref } from 'vue'
import type { RuntimeEnvironment } from '@/config/environments'
import type { useTemplatesStore } from '@/stores/templates'
import type { useTemplateLifecycleActions } from '@/views/templates/useTemplateLifecycleActions'
import type { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import type { useTemplateDetailVisibility } from '@/views/templates/useTemplateDetailVisibility'
import type { useTemplatePreviewActions } from '@/views/templates/useTemplatePreviewActions'
import type { useTemplateDetailNavigation } from '@/views/templates/useTemplateDetailNavigation'
import type { TemplateDetail } from '@/types/template'
import {
  assembleTemplateDetailJourneySlice,
  assembleTemplateDetailLifecycleSlice,
} from '@/views/templates/assembleTemplateDetailControllerSlices'

type Translate = (key: string, params?: Record<string, unknown>) => string
type HasKey = (key: string) => boolean

export function assembleTemplateDetailControllerApi(options: {
  t: Translate
  te: HasKey
  formatDateTime: (value: string) => string
  templateId: ComputedRef<string>
  devVersionId: ComputedRef<string>
  isDevEditor: ComputedRef<boolean>
  template: ComputedRef<TemplateDetail | null>
  showDetailSkeleton: ComputedRef<boolean>
  authorTemplates: ComputedRef<boolean>
  decideTests: ComputedRef<boolean>
  decideApprovals: ComputedRef<boolean>
  publishTemplates: ComputedRef<boolean>
  reviewMasters: ComputedRef<boolean>
  metadataEditOpen: Ref<boolean>
  selectedContractEnvironment: Ref<RuntimeEnvironment>
  templatesStore: ReturnType<typeof useTemplatesStore>
  lifecycle: ReturnType<typeof useTemplateLifecycleActions>
  policy: ReturnType<typeof useTemplatePolicyCredentials>
  visibility: ReturnType<typeof useTemplateDetailVisibility>
  preview: ReturnType<typeof useTemplatePreviewActions>
  navigation: ReturnType<typeof useTemplateDetailNavigation>
  handleMetadataUpdate: (payload: { name: string; description: string | null }) => Promise<void>
}) {
  const {
    t,
    te,
    formatDateTime,
    templateId,
    devVersionId,
    isDevEditor,
    template,
    showDetailSkeleton,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    metadataEditOpen,
    selectedContractEnvironment,
    templatesStore,
    lifecycle,
    policy,
    visibility,
    preview,
    navigation,
    handleMetadataUpdate,
  } = options

  return {
    t,
    te,
    formatDateTime,
    templateId,
    devVersionId,
    isDevEditor,
    template,
    showDetailSkeleton,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    ...assembleTemplateDetailJourneySlice(navigation),
    ...assembleTemplateDetailLifecycleSlice(lifecycle),
    showTestGenerate: visibility.showTestGenerate,
    showAuthoringSection: visibility.showAuthoringSection,
    canEditContentModuleReferences: visibility.canEditContentModuleReferences,
    showExportActions: visibility.showExportActions,
    showMetadataEdit: visibility.showMetadataEdit,
    showPolicyPanel: policy.showPolicyPanel,
    canPolicy: policy.canPolicy,
    policyLoadFailed: policy.policyLoadFailed,
    apiPolicy: policy.apiPolicy,
    loadingPolicy: policy.loadingPolicy,
    policySubmitting: policy.policySubmitting,
    policyLoadErrorKey: policy.policyLoadErrorKey,
    credentialSecretDialogVisible: policy.credentialSecretDialogVisible,
    credentialSecretValue: policy.credentialSecretValue,
    credentialSecretExternalId: policy.credentialSecretExternalId,
    displayedCredentialSecret: policy.displayedCredentialSecret,
    credentialColumnFilters: policy.credentialColumnFilters,
    credentialsCurrentPage: policy.credentialsCurrentPage,
    paginatedCredentials: policy.paginatedCredentials,
    credentialStatusFilterOptions: policy.credentialStatusFilterOptions,
    totalCredentialRows: policy.totalCredentialRows,
    sortCredentialsByCreatedAt: policy.sortCredentialsByCreatedAt,
    loadPolicyData: policy.loadPolicyData,
    handleCreateCredential: policy.handleCreateCredential,
    handleRotateCredential: policy.handleRotateCredential,
    handleRevokeCredential: policy.handleRevokeCredential,
    lastPreview: preview.lastPreview,
    selectedPreviewId: preview.selectedPreviewId,
    selectedTestDataSetId: preview.selectedTestDataSetId,
    generatingPreview: preview.generatingPreview,
    generatingPreviewId: preview.generatingPreviewId,
    coverageRefreshToken: preview.coverageRefreshToken,
    handleTestGenerate: preview.handleTestGenerate,
    bumpCoverageRefresh: preview.bumpCoverageRefresh,
    handlePreviewSelected: preview.handlePreviewSelected,
    handlePreviewRefreshed: preview.handlePreviewRefreshed,
    metadataEditOpen,
    selectedContractEnvironment,
    templatesStore,
    handleMetadataUpdate,
  }
}
